package momime.server.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;
import com.ndg.random.WeightedChoicesImpl;

import momime.common.MomException;
import momime.common.calculations.SpellCalculations;
import momime.common.database.AttackSpellCombatTargetID;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitCanCast;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatPlayers;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.process.SpellProcessing;
import momime.server.process.SpellQueueing;
import momime.server.utils.UnitServerUtils;

/**
 * Methods for AI players making decisions about spells
 */
public final class SpellAIImpl implements SpellAI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (SpellAIImpl.class);
	
	/** Spell utils */
	private SpellUtils spellUtils;

	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** AI spell calculations */
	private AISpellCalculations aiSpellCalculations;

	/** Spell queueing methods */
	private SpellQueueing spellQueueing;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** AI decisions about cities */
	private CityAI cityAI;
	
	/** Spell processing methods */
	private SpellProcessing spellProcessing;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;

	/** AI decisions about units */
	private UnitAI unitAI;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Methods that the AI uses to calculate stats about types of units and rating how good units are */
	private AIUnitCalculations aiUnitCalculations;
	
	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/** Spell calculations */
	private SpellCalculations spellCalculations;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/**
	 * Common routine between picking free spells at the start of the game and picking the next spell to research - it picks a spell from the supplied list
	 * @param spells List of possible spells to choose from
	 * @return ID of chosen spell to research
	 * @throws MomException If the list was empty
	 */
	final Spell chooseSpellToResearchAI (final List<Spell> spells)
		throws MomException
	{
		String debugLogMessage = null;

		// Check each spell in the list to find the the best research order, 1 being the best, 9 being the worst, and make a list of spells with this research order
		int bestResearchOrder = Integer.MAX_VALUE;
		final List<Spell> spellsWithBestResearchOrder = new ArrayList<Spell> ();

		for (final Spell spell : spells)
		{
			if (spell.getAiResearchOrder () != null)
			{
				// List possible choices in debug message
				if (debugLogMessage == null)
					debugLogMessage = spell.getSpellName () + " (" + spell.getAiResearchOrder () + ")";
				else
					debugLogMessage = debugLogMessage + ", " + spell.getSpellName () + " (" + spell.getAiResearchOrder () + ")";

				if (spell.getAiResearchOrder () < bestResearchOrder)
				{
					bestResearchOrder = spell.getAiResearchOrder ();
					spellsWithBestResearchOrder.clear ();
				}

				if (spell.getAiResearchOrder () == bestResearchOrder)
					spellsWithBestResearchOrder.add (spell);
			}
		}

		// Check we found one (this error should only happen if the list was totally empty)
		if ((bestResearchOrder == Integer.MAX_VALUE) || (spellsWithBestResearchOrder.size () == 0))
			throw new MomException ("chooseSpellToResearchAI: No appropriate spells to pick from list of " + spells.size ());

		// Pick one at random
		final Spell chosenSpell = spellsWithBestResearchOrder.get (getRandomUtils ().nextInt (spellsWithBestResearchOrder.size ()));
		return chosenSpell;
	}

	/**
	 * @param player AI player who needs to choose what to research
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 * @throws MomException If there is an error in the logic
	 */
	@Override
	public final void decideWhatToResearch (final PlayerServerDetails player, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		final List<Spell> researchableSpells = getSpellUtils ().getSpellsForStatus
			(priv.getSpellResearchStatus (), SpellResearchStatusID.RESEARCHABLE_NOW, db);

		if (!researchableSpells.isEmpty ())
		{
			final Spell chosenSpell = chooseSpellToResearchAI (researchableSpells);
			priv.setSpellIDBeingResearched (chosenSpell.getSpellID ());
		}
	}

	/**
	 * AI player at the start of the game chooses any spell of the specific magic realm & rank and researches it for free
	 * @param spells Pre-locked list of the player's spell
	 * @param magicRealmID Magic Realm (e.g. chaos) to pick a spell from
	 * @param spellRankID Spell rank (e.g. uncommon) to pick a spell of
	 * @param db Lookup lists built over the XML database
	 * @return Spell AI chose to learn for free
	 * @throws MomException If no eligible spells are available (e.g. player has them all researched already)
	 * @throws RecordNotFoundException If the spell chosen couldn't be found in the player's spell list
	 */
	@Override
	public final SpellResearchStatus chooseFreeSpellAI (final List<SpellResearchStatus> spells, final String magicRealmID, final String spellRankID, final CommonDatabase db)
		throws MomException, RecordNotFoundException
	{
		// Get candidate spells
		final List<Spell> spellList = getSpellUtils ().getSpellsNotInBookForRealmAndRank (spells, magicRealmID, spellRankID, db);

		// Choose a spell
		final Spell chosenSpell = chooseSpellToResearchAI (spellList);

		// Return spell research status; calling routine sets it to available
		final SpellResearchStatus chosenSpellStatus = getSpellUtils ().findSpellResearchStatus (spells, chosenSpell.getSpellID ());
		return chosenSpellStatus;
	}
	
	/**
	 * If AI player is not currently casting any spells overland, then look through all of them and consider if we should cast any.
	 * 
	 * @param player AI player who needs to choose what to cast
	 * @param constructableUnits List of everything we can construct everywhere or summon
	 * @param wantedUnitTypesOnEachPlane Map of which unit types we need to construct or summon on each plane
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws RecordNotFoundException If we find the spell they're trying to cast, or other expected game elements
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Override
	public final void decideWhatToCastOverland (final PlayerServerDetails player, final List<AIConstructableUnit> constructableUnits,
		final Map<Integer, List<AIUnitType>> wantedUnitTypesOnEachPlane, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		// Exit if we're already in the middle of casting something
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		if (priv.getQueuedSpell ().size () == 0)
		{
			// Find our summoning circle - we have to have one, or we wouldn't be able to cast spells and be in this method
			final MemoryBuilding summoningCircleLocation = getMemoryBuildingUtils ().findCityWithBuilding (player.getPlayerDescription ().getPlayerID (),
				CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
			
			final int summoningCirclePlane = summoningCircleLocation.getCityLocation ().getZ ();
			final int oppositeSummoningCirclePlane = 1 - summoningCirclePlane;
			
			// Do we need a magic/guardian spirit in order to capture a node on the same plane as our summoning circle?
			// If so then that's important enough to just do it, with no randomness.
			final WeightedChoicesImpl<Spell> considerSpells = new WeightedChoicesImpl<Spell> ();
			considerSpells.setRandomUtils (getRandomUtils ());
			
			if (wantedUnitTypesOnEachPlane.get (summoningCirclePlane).contains (AIUnitType.MELD_WITH_NODE))
			{
				// What's the best spirit summoning unit we can get?  So summon Guardian Spirit if we have it, otherwise Magic Spirit
				final List<AIConstructableUnit> spirits = constructableUnits.stream ().filter
					(u -> (u.getAiUnitType () == AIUnitType.MELD_WITH_NODE) && (u.getSpell () != null)).sorted ().collect (Collectors.toList ());
				
				log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " has a guarded node to capture on same plane as their summoning circle " +
					summoningCircleLocation.getCityLocation () + " so summonining spirit with " + spirits.get (0).getSpell ().getSpellID ());
				considerSpells.add (1, spirits.get (0).getSpell ());
			}

			// Do we need a magic/guardian spirit in order to capture a node on the opposite plane as our summoning circle AND have a city there we can move our summoning circle to?
			else if ((wantedUnitTypesOnEachPlane.get (oppositeSummoningCirclePlane).contains (AIUnitType.MELD_WITH_NODE)) &&
				(getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), CommonDatabaseConstants.SPELL_ID_SUMMONING_CIRCLE).getStatus () == SpellResearchStatusID.AVAILABLE) &&
				(constructableUnits.stream ().anyMatch (u -> (u.getCityLocation () != null) && (u.getCityLocation ().getZ () == oppositeSummoningCirclePlane))))
			{
				log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " has a guarded node on the opposite plane to their summoning circle, " +
					" so casting summoning circle spell to move our summoning circle onto that plane, so after we can summon a spirit there");
				considerSpells.add (1, mom.getServerDB ().findSpell (CommonDatabaseConstants.SPELL_ID_SUMMONING_CIRCLE, "decideWhatToCastOverland"));
			}
			
			else
			{
				// Unit types classify magic/guardian spirits as non-combat units, so just look at all other kinds of summoned units we can get.
				// Also we want to split them up by magic realm, as if we can get very rare creatures in multiple realms, we shouldn't just summon lots of the one type.
				// Note this list includes units we cannot afford maintenance of which is what we want.  They'll get filtered out below when we check each spell.
				int maxSummonCost = 0;
				final Map<String, List<AIConstructableUnit>> summonableCombatUnits = new HashMap<String, List<AIConstructableUnit>> ();
				for (final AIConstructableUnit summonUnit : constructableUnits)
					if ((summonUnit.getAiUnitType () == AIUnitType.COMBAT_UNIT) && (summonUnit.getSpell () != null))
					{
						// Put into correct list
						List<AIConstructableUnit> summonableUnitsForThisMagicRealm = summonableCombatUnits.get (summonUnit.getSpell ().getSpellRealm ());
						if (summonableUnitsForThisMagicRealm == null)
						{
							summonableUnitsForThisMagicRealm = new ArrayList<AIConstructableUnit> ();
							summonableCombatUnits.put (summonUnit.getSpell ().getSpellRealm (), summonableUnitsForThisMagicRealm);
						}
						summonableUnitsForThisMagicRealm.add (summonUnit);
						
						// Find most expensive summonable unit across all magic realms
						if (summonUnit.getSpell ().getOverlandCastingCost () > maxSummonCost)
							maxSummonCost = summonUnit.getSpell ().getOverlandCastingCost ();
					}
				
				// Don't summon anything 25% cheaper than maximum
				final int minSummonCost = (maxSummonCost * 3) / 4;
				
				// Sort them all, within each magic realm, most expensive first
				for (final List<AIConstructableUnit> summonableUnitsForThisMagicRealm : summonableCombatUnits.values ())
					summonableUnitsForThisMagicRealm.sort ((s1, s2) -> s2.getSpell ().getOverlandCastingCost () - s1.getSpell ().getOverlandCastingCost ());
				
				// Consider every possible spell we could cast overland and can afford maintainence of
				for (final Spell spell : mom.getServerDB ().getSpell ())
					if ((spell.getSpellBookSectionID () != null) && (getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.OVERLAND)) &&
						(getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spell.getSpellID ()).getStatus () == SpellResearchStatusID.AVAILABLE) &&
						(getAiSpellCalculations ().canAffordSpellMaintenance (player, mom.getPlayers (), spell, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
							mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ())))
						
						switch (spell.getSpellBookSectionID ())
						{
							// Consider casting any overland enchantment that we don't already have
							case OVERLAND_ENCHANTMENTS:
								if (getMemoryMaintainedSpellUtils ().findMaintainedSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
									player.getPlayerDescription ().getPlayerID (), spell.getSpellID (), null, null, null, null) == null)
								{
									log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering casting overland enchantment " + spell.getSpellID ());
									considerSpells.add (2, spell);
								}
								break;
								
							// Consider summoning combat units that are the best we can get in that realm, and over minimum summoning cost
							case SUMMONING:
								if ((spell.getHeroItemBonusMaximumCraftingCost () == null) && (spell.getOverlandCastingCost () >= minSummonCost) &&
									(summonableCombatUnits.containsKey (spell.getSpellRealm ())) && (summonableCombatUnits.get (spell.getSpellRealm ()).get (0).getSpell () == spell))
								{
									log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering casting summoning spell " + spell.getSpellID ());
									considerSpells.add (3, spell);
								}
								break;
								
							// City enchantments and curses - we don't pick the target until its finished casting, but must prove that there is a valid target to pick
							case CITY_ENCHANTMENTS:
							case CITY_CURSES:
								// Ignore Spell of Return, Summoning Circle & Move Fortress or the AI will just keep wasting mana moving them around
								if (spell.getBuildingID () == null)
								{
									boolean validTargetFound = false;
									int z = 0;
									while ((!validTargetFound) && (z < mom.getSessionDescription ().getOverlandMapSize ().getDepth ()))
									{
										int y = 0;
										while ((!validTargetFound) && (y < mom.getSessionDescription ().getOverlandMapSize ().getHeight ()))
										{
											int x = 0;
											while ((!validTargetFound) && (x < mom.getSessionDescription ().getOverlandMapSize ().getWidth ()))
											{
												final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);
												
												// Routine checks everything, even down to whether there is even a city there or not, or whether the city already has that spell cast on it, so just let it handle it
												if (getMemoryMaintainedSpellUtils ().isCityValidTargetForSpell (priv.getFogOfWarMemory ().getMaintainedSpell (), spell,
													player.getPlayerDescription ().getPlayerID (), cityLocation, priv.getFogOfWarMemory ().getMap (), priv.getFogOfWar (),
													priv.getFogOfWarMemory ().getBuilding ()) == TargetSpellResult.VALID_TARGET)
													
													validTargetFound = true;
		
												x++;
											}
											y++;
										}
										z++;
									}
									if (validTargetFound)
									{
										log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering casting city enchantment/curse spell " + spell.getSpellID ());
										considerSpells.add (1, spell);
									}
								}
								break;
								
							// Unit enchantments - again don't pick target until its finished casting
							case UNIT_ENCHANTMENTS:
								boolean validTargetFound = false;
								final Iterator<MemoryUnit> iter = priv.getFogOfWarMemory ().getUnit ().iterator ();
								while ((!validTargetFound) && (iter.hasNext ()))
								{
									final MemoryUnit mu = iter.next ();
									final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (mu, null, null, null, mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ());
									if ((getAiUnitCalculations ().determineAIUnitType (xu) == AIUnitType.COMBAT_UNIT) &&
										(getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell (spell, null, player.getPlayerDescription ().getPlayerID (), null, xu,
											priv.getFogOfWarMemory (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET))
										
										validTargetFound = true;
								}

								if (validTargetFound)
								{
									log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering unit enchantment spell " + spell.getSpellID ());
									considerSpells.add (1, spell);
								}
								
								break;
								
							// This is fine, the AI doesn't cast every type of spell yet
							default:
						}
			}
						
			// If we found any, then pick one randomly
			final Spell spell = considerSpells.nextWeightedValue ();
			if (spell != null)
			{
				log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " casting overland spell " + spell.getSpellID ());
				getSpellQueueing ().requestCastSpell (player, null, null, null, spell.getSpellID (), null, null, null, null, null, mom);
			}
		}
	}
	
	/**
	 * Overland spells are cast first (probably taking several turns) and a target is only chosen after casting is completed.  So after the AI finishes
	 * casting an overland spell that requires a target, this method tries to pick a good target for the spell.  This won't even get called for
	 * types of spell that don't require targets (e.g. overland enchantments or summoning spells), see method castOverlandNow.
	 * 
	 * @param player AI player who needs to choose a spell target
	 * @param spell Definition for the spell to target
	 * @param maintainedSpell Spell being targetted in server's true memory - at the time this is called, this is the only copy of the spell that exists,
	 * 	so its the only thing we need to clean up
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void decideSpellTarget (final PlayerServerDetails player, final Spell spell, final MemoryMaintainedSpell maintainedSpell, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		MapCoordinates3DEx targetLocation = null;
		MemoryUnit targetUnit = null;
		String citySpellEffectID = null;
		String unitSkillID = null;
		
		switch (spell.getSpellBookSectionID ())
		{
			// City enchantments and curses, including Spell of Return
			case CITY_ENCHANTMENTS:
			case CITY_CURSES:
				if (spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SUMMONING_CIRCLE))
				{
					final List<MapCoordinates3DEx> nodeLocations = getUnitAI ().listNodesWeDontOwnOnPlane (player.getPlayerDescription ().getPlayerID (), null, priv.getFogOfWarMemory (),
						mom.getSessionDescription ().getOverlandMapSize (), mom.getServerDB ());
					
					// Find the city nearest any listed node
					Integer shortestDistance = null;
					for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
						for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
							for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
							{
								final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
								if ((mc != null) && (mc.getCityData () != null) && (mc.getCityData ().getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))
								{
									final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);
									
									for (final MapCoordinates3DEx nodeLocation : nodeLocations)
										if (nodeLocation.getZ () == z)
										{
											final int thisDistance = getCoordinateSystemUtils ().determineStep2DDistanceBetween (mom.getSessionDescription ().getOverlandMapSize (),
												nodeLocation, cityLocation);
											
											if ((shortestDistance == null) || (thisDistance < shortestDistance))
											{
												shortestDistance = thisDistance;
												targetLocation = cityLocation;
											}
										}
								}
							}
				}
				else
				{
					int bestCityQuality = -1;
					for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
						for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
							for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
							{
								final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);
								
								// Routine checks everything, even down to whether there is even a city there or not, so just let it handle it
								if (getMemoryMaintainedSpellUtils ().isCityValidTargetForSpell (priv.getFogOfWarMemory ().getMaintainedSpell (), spell,
									player.getPlayerDescription ().getPlayerID (), cityLocation, priv.getFogOfWarMemory ().getMap (), priv.getFogOfWar (),
									priv.getFogOfWarMemory ().getBuilding ()) == TargetSpellResult.VALID_TARGET)
								{
									final int thisCityQuality = getCityAI ().evaluateCityQuality (cityLocation, false, false, priv.getFogOfWarMemory ().getMap (), mom.getSessionDescription (), mom.getServerDB ());
									if ((targetLocation == null) || (thisCityQuality > bestCityQuality))
									{
										targetLocation = cityLocation;
										bestCityQuality = thisCityQuality;
									}
								}
							}
				}
				break;
				
			// Unit enchantments
			case UNIT_ENCHANTMENTS:
				int bestUnitRating = -1;
				for (final MemoryUnit mu : priv.getFogOfWarMemory ().getUnit ())
				{
					final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (mu, null, null, null, mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ());
					if ((getAiUnitCalculations ().determineAIUnitType (xu) == AIUnitType.COMBAT_UNIT) &&
						(getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell (spell, null, player.getPlayerDescription ().getPlayerID (), null, xu,
							priv.getFogOfWarMemory (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET))
					{
						int thisUnitRating = getAiUnitCalculations ().calculateUnitAverageRating (xu.getUnit (), xu, mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ());
						
						// Give priority to buffing heroes
						if (xu.isHero ())
							thisUnitRating = thisUnitRating + 10;
						
						if ((targetUnit == null) || (thisUnitRating > bestUnitRating))
						{
							targetUnit = mu;
							bestUnitRating = thisUnitRating;
						}
					}
				}
				break;
				
			default:
				throw new MomException ("AI decideSpellTarget does not know how to decide a target for spell " + spell.getSpellID () + " in section " + spell.getSpellBookSectionID ());
		}
		
		// If we got a location then target the spell; if we didn't then cancel the spell
		if ((targetLocation == null) && (targetUnit == null))
			getSpellProcessing ().cancelTargetOverlandSpell (maintainedSpell, mom);
		else
		{
			// Pick a specific spell effect if we need to
			switch (spell.getSpellBookSectionID ())
			{
				case CITY_ENCHANTMENTS:
				case CITY_CURSES:
					final List<String> citySpellEffectIDs = getMemoryMaintainedSpellUtils ().listCitySpellEffectsNotYetCastAtLocation
						(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), spell, player.getPlayerDescription ().getPlayerID (), targetLocation);
					if ((citySpellEffectIDs != null) && (citySpellEffectIDs.size () > 0))
						citySpellEffectID = citySpellEffectIDs.get (getRandomUtils ().nextInt (citySpellEffectIDs.size ()));
					break;
					
				case UNIT_ENCHANTMENTS:
					final List<String> unitSkillIDs = getMemoryMaintainedSpellUtils ().listUnitSpellEffectsNotYetCastOnUnit
						(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), spell, player.getPlayerDescription ().getPlayerID (), targetUnit.getUnitURN ());
					if ((unitSkillIDs != null) && (unitSkillIDs.size () > 0))
						unitSkillID = unitSkillIDs.get (getRandomUtils ().nextInt (unitSkillIDs.size ()));
					break;
			
				// This is fine, only need to do this for certain types of spells
				default:
			}
			
			// Target it
			getSpellProcessing ().targetOverlandSpell (spell, maintainedSpell, targetLocation, targetUnit, citySpellEffectID, unitSkillID, mom);
		}
	}

	/**
	 * AI player decides whether to cast a spell in combat
	 * 
	 * @param player AI player who needs to choose what to cast
	 * @param combatCastingUnit Unit who is casting the spell; null means its the wizard casting, rather than a specific unit
	 * @param combatLocation Location of the combat where this spell is being cast
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether a spell was cast or not
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws RecordNotFoundException If we find the spell they're trying to cast, or other expected game elements
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Override
	public final CombatAIMovementResult decideWhatToCastCombat (final PlayerServerDetails player, final ExpandedUnitDetails combatCastingUnit, final MapCoordinates3DEx combatLocation,
		final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		final ServerGridCellEx gc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
		
		// Units with the caster skill (Archangels, Efreets and Djinns) cast spells from their magic realm, totally ignoring whatever spells their controlling wizard knows.
		// Using getModifiedUnitMagicRealmLifeformTypeID makes this account for them casting Death spells instead if you get an undead Archangel or similar.
		String overridePickID = null;
		if ((combatCastingUnit != null) && (combatCastingUnit.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT)))
		{
			overridePickID = combatCastingUnit.getModifiedUnitMagicRealmLifeformType ().getCastSpellsFromPickID ();
			if (overridePickID == null)
				overridePickID = combatCastingUnit.getModifiedUnitMagicRealmLifeformType ().getPickID ();
		}
		
		// Now we can go through every spell in the book to test which we can consider casting
		final WeightedChoicesImpl<CombatAISpellChoice> choices = new WeightedChoicesImpl<CombatAISpellChoice> ();
		choices.setRandomUtils (getRandomUtils ());
		
		for (final Spell spell : mom.getServerDB ().getSpell ())
			if ((spell.getSpellBookSectionID () != null) && (getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.COMBAT)) &&
					
				// Ignore "recall" spells then the AI would then have to understand its likelehood of losing a combat, and there's nothing like this yet
				((spell.getSpellBookSectionID () != SpellBookSectionID.SPECIAL_UNIT_SPELLS) || (spell.getCombatBaseDamage () != null)))
			{
				// A lot of this is lifted from the same validation that requestCastSpell does
				boolean knowSpell;
				if (overridePickID != null)
					knowSpell = overridePickID.equals (spell.getSpellRealm ());
				else
				{
					final SpellResearchStatusID researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spell.getSpellID ()).getStatus ();
					knowSpell = (researchStatus == SpellResearchStatusID.AVAILABLE);
					
					// Some heroes have their own list of spells they know even if their owning wizard does not
					if ((!knowSpell) && (combatCastingUnit != null))
					{
						final Iterator<UnitCanCast> knownSpellsIter = combatCastingUnit.getUnitDefinition ().getUnitCanCast ().iterator ();
						while ((!knowSpell) && (knownSpellsIter.hasNext ()))
						{
							final UnitCanCast thisKnownSpell = knownSpellsIter.next ();
							if ((thisKnownSpell.getUnitSpellID ().equals (spell.getSpellID ())) && (thisKnownSpell.getNumberOfTimes () == null))
								knowSpell = true;
						}
					}
				}
				
				if (knowSpell)
				{
					// Now validate we have enough remaining MP and casting skill to cast it
					final boolean canAffordSpell;
					final int unmodifiedCombatCastingCost = spell.getCombatCastingCost ();
					if (combatCastingUnit == null)
					{
						// Wizards get range penalty depending where the combat is in relation to their Fortress; units participating in the combat don't get this
						final int reducedCombatCastingCost = getSpellUtils ().getReducedCastingCost
							(spell, unmodifiedCombatCastingCost, pub.getPick (), mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ());

						final Integer doubleRangePenalty = getSpellCalculations ().calculateDoubleCombatCastingRangePenalty (player, combatLocation,
							getMemoryGridCellUtils ().isTerrainTowerOfWizardry (gc.getTerrainData ()),
							mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (),
							mom.getSessionDescription ().getOverlandMapSize ());
					
						final int multipliedManaCost = (doubleRangePenalty == null) ? Integer.MAX_VALUE :
							(reducedCombatCastingCost * doubleRangePenalty + 1) / 2;
						
						// Wizards have limited casting skill they can use in each combat
						final CombatPlayers combatPlayers = getCombatMapUtils ().determinePlayersInCombatFromLocation
							(combatLocation, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getPlayers ());

						final int ourSkill;
						if (player == combatPlayers.getAttackingPlayer ())
							ourSkill = gc.getCombatAttackerCastingSkillRemaining ();
						else if (player == combatPlayers.getDefendingPlayer ())
							ourSkill = gc.getCombatDefenderCastingSkillRemaining ();
						else
							ourSkill = 0;

						canAffordSpell = (reducedCombatCastingCost <= ourSkill) &&
							(multipliedManaCost <= getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA));
					}
					else
					{
						// Validation for units casting is much simpler, as reductions for number of spell books or certain retorts and the range penalty all don't apply
						canAffordSpell = (unmodifiedCombatCastingCost <= combatCastingUnit.getManaRemaining ());
					}

					if (canAffordSpell)
					{
						// Validation for certain spell book sections
						boolean valid = true;
						switch (spell.getSpellBookSectionID ())
						{
							// Only add combat enchantments that aren't already there
							case COMBAT_ENCHANTMENTS:
								if (getMemoryCombatAreaEffectUtils ().listCombatEffectsNotYetCastAtLocation (mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (),
									spell, player.getPlayerDescription ().getPlayerID (), combatLocation).size () == 0)
									
									valid = false;
								break;
								
							// Can't summon (including raise dead-type spells) if already max number of units
							case SUMMONING:
								if ((!mom.getSessionDescription ().getUnitSetting ().isCanExceedMaximumUnitsDuringCombat ()) &&
									(getCombatMapUtils ().countPlayersAliveUnitsAtCombatLocation (player.getPlayerDescription ().getPlayerID (), combatLocation,
										mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ()) >= mom.getSessionDescription ().getUnitSetting ().getUnitsPerMapCell ()))
									
									valid = false;
								break;
								
							default:
						}
						
						if (valid)
						{
							// Do we need to pick a target?  Or if spell hits multiple targets, prove there is at least one?
							if ((spell.getSpellBookSectionID () == SpellBookSectionID.COMBAT_ENCHANTMENTS) ||
								((spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING) && (spell.getSummonedUnit ().size () > 0)))
							{
								log.debug ("AI player " + player.getPlayerDescription ().getPlayerID () + " considering casting spell " + spell.getSpellID () + " (" + spell.getSpellName () + ") in combat which requires no unit checks");
								final CombatAISpellChoice choice = new CombatAISpellChoice (spell, null, null);
								choices.add (choice.getWeighting (), choice);
							}
							else
							{
								// Every other kind of spell requires either to be targetted on a specific unit, or at least for spells that hit all units (e.g. Flame Strike or Mass Healing)
								// that there are some appropriate targets for the spell to act on.  First figure out which of those situations it is.
								Integer targetCount;
								if ((spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) || (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES) ||
									(spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING) ||
									(((spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS) || (spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_UNIT_SPELLS) ||
										(spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS)) && (spell.getAttackSpellCombatTarget () == AttackSpellCombatTargetID.SINGLE_UNIT)))
									
									targetCount = null;		// Targetted at a specific unit, so do not keep a count of targets
								else
									targetCount = 0;		// Targetted at all units, so count how many
	
								for (final MemoryUnit targetUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
								{
									final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (targetUnit, null, null, spell.getSpellRealm (),
										mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
									
									if (getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell (spell, combatLocation, player.getPlayerDescription ().getPlayerID (),
										null, xu, mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
									{
										if (targetCount == null)
										{
											log.debug ("AI player " + player.getPlayerDescription ().getPlayerID () + " considering casting spell " + spell.getSpellID () + " (" + spell.getSpellName () + ") in combat at Unit URN " + targetUnit.getUnitURN ());
											final CombatAISpellChoice choice = new CombatAISpellChoice (spell, xu, null);
											choices.add (choice.getWeighting (), choice);
										}
										else
											targetCount++;
									}
								}
								
								// Is it a spell that hits all units, and found some targets?
								if ((targetCount != null) && (targetCount > 0))
								{
									log.debug ("AI player " + player.getPlayerDescription ().getPlayerID () + " considering casting spell " + spell.getSpellID () + " (" + spell.getSpellName () + ") in combat which will hit " + targetCount + " target(s)");
									final CombatAISpellChoice choice = new CombatAISpellChoice (spell, null, targetCount);
									choices.add (choice.getWeighting (), choice);
								}
							}
						}
					}
				}
			}
		
		final CombatAISpellChoice choice = choices.nextWeightedValue ();
		final CombatAIMovementResult result;
		if (choice == null)
			result = CombatAIMovementResult.NOTHING;
		else
		{
			log.debug ("AI player " + player.getPlayerDescription ().getPlayerID () + " decided to cast combat spell " + choice.getSpell ().getSpellID () + " (" +
				choice.getSpell ().getSpellName () + ")");
			
			// If a summoning spell, pick a location for the unit
			final MapCoordinates2DEx summoningLocation = (choice.getSpell ().getSpellBookSectionID () != SpellBookSectionID.SUMMONING) ? null :
				getUnitServerUtils ().findFreeCombatPositionClosestTo (combatLocation, gc.getCombatMap (),
					new MapCoordinates2DEx (mom.getSessionDescription ().getCombatMapSize ().getWidth () / 2, mom.getSessionDescription ().getCombatMapSize ().getHeight () / 2),
					mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getSessionDescription ().getCombatMapSize (), mom.getServerDB ());
			
			if (getSpellQueueing ().requestCastSpell (player, (combatCastingUnit == null) ? null : combatCastingUnit.getUnitURN (), null, null,
				choice.getSpell ().getSpellID (), null, combatLocation, summoningLocation, (choice.getTargetUnit () == null) ? null : choice.getTargetUnit ().getUnitURN (), null, mom))
				
				result = CombatAIMovementResult.ENDED_COMBAT;
			else
				result = CombatAIMovementResult.MOVED_OR_ATTACKED;
		}

		return result;
	}
	
	/**
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtil MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtil)
	{
		memoryMaintainedSpellUtils = spellUtil;
	}
	
	/**
	 * @return AI spell calculations
	 */
	public final AISpellCalculations getAiSpellCalculations ()
	{
		return aiSpellCalculations;
	}

	/**
	 * @param calc AI spell calculations
	 */
	public final void setAiSpellCalculations (final AISpellCalculations calc)
	{
		aiSpellCalculations = calc;
	}

	/**
	 * @return Spell queueing methods
	 */
	public final SpellQueueing getSpellQueueing ()
	{
		return spellQueueing;
	}

	/**
	 * @param obj Spell queueing methods
	 */
	public final void setSpellQueueing (final SpellQueueing obj)
	{
		spellQueueing = obj;
	}
	
	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}

	/**
	 * @return AI decisions about cities
	 */
	public final CityAI getCityAI ()
	{
		return cityAI;
	}

	/**
	 * @param ai AI decisions about cities
	 */
	public final void setCityAI (final CityAI ai)
	{
		cityAI = ai;
	}

	/**
	 * @return Spell processing methods
	 */
	public final SpellProcessing getSpellProcessing ()
	{
		return spellProcessing;
	}

	/**
	 * @param obj Spell processing methods
	 */
	public final void setSpellProcessing (final SpellProcessing obj)
	{
		spellProcessing = obj;
	}

	/**
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}

	/**
	 * @return AI decisions about units
	 */
	public final UnitAI getUnitAI ()
	{
		return unitAI;
	}

	/**
	 * @param ai AI decisions about units
	 */
	public final void setUnitAI (final UnitAI ai)
	{
		unitAI = ai;
	}

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param csu Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils csu)
	{
		coordinateSystemUtils = csu;
	}

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}

	/**
	 * @return Methods that the AI uses to calculate stats about types of units and rating how good units are
	 */
	public final AIUnitCalculations getAiUnitCalculations ()
	{
		return aiUnitCalculations;
	}

	/**
	 * @param calc Methods that the AI uses to calculate stats about types of units and rating how good units are
	 */
	public final void setAiUnitCalculations (final AIUnitCalculations calc)
	{
		aiUnitCalculations = calc;
	}

	/**
	 * @return Combat map utils
	 */
	public final CombatMapUtils getCombatMapUtils ()
	{
		return combatMapUtils;
	}
	
	/**
	 * @param utils Combat map utils
	 */
	public final void setCombatMapUtils (final CombatMapUtils utils)
	{
		combatMapUtils = utils;
	}

	/**
	 * @return Spell calculations
	 */
	public final SpellCalculations getSpellCalculations ()
	{
		return spellCalculations;
	}

	/**
	 * @param calc Spell calculations
	 */
	public final void setSpellCalculations (final SpellCalculations calc)
	{
		spellCalculations = calc;
	}

	/**
	 * @return MemoryGridCell utils
	 */
	public final MemoryGridCellUtils getMemoryGridCellUtils ()
	{
		return memoryGridCellUtils;
	}

	/**
	 * @param utils MemoryGridCell utils
	 */
	public final void setMemoryGridCellUtils (final MemoryGridCellUtils utils)
	{
		memoryGridCellUtils = utils;
	}

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils utils)
	{
		resourceValueUtils = utils;
	}

	/**
	 * @return Server-only unit utils
	 */
	public final UnitServerUtils getUnitServerUtils ()
	{
		return unitServerUtils;
	}

	/**
	 * @param utils Server-only unit utils
	 */
	public final void setUnitServerUtils (final UnitServerUtils utils)
	{
		unitServerUtils = utils;
	}

	/**
	 * @return Memory CAE utils
	 */
	public final MemoryCombatAreaEffectUtils getMemoryCombatAreaEffectUtils ()
	{
		return memoryCombatAreaEffectUtils;
	}

	/**
	 * @param utils Memory CAE utils
	 */
	public final void setMemoryCombatAreaEffectUtils (final MemoryCombatAreaEffectUtils utils)
	{
		memoryCombatAreaEffectUtils = utils;
	}
}