package momime.server.ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.utils.random.RandomUtils;
import com.ndg.utils.random.WeightedChoicesImpl;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.calculations.SpellCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.MapFeatureEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellValidMapFeatureTarget;
import momime.common.database.UnitCanCast;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.OverlandCastingInfo;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatPlayers;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KindOfSpell;
import momime.common.utils.KindOfSpellUtils;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellTargetingUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.server.MomSessionVariables;
import momime.server.knowledge.CombatDetails;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.process.SpellCasting;
import momime.server.process.SpellDispelling;
import momime.server.process.SpellProcessing;
import momime.server.process.SpellQueueing;
import momime.server.utils.SpellServerUtils;

/**
 * Methods for AI players making decisions about spells
 */
public final class SpellAIImpl implements SpellAI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (SpellAIImpl.class);
	
	/** Tile types around or cities we want to get rid of */
	private final static List<String> BAD_TILE_TYPES = Arrays.asList (CommonDatabaseConstants.TILE_TYPE_SWAMP, CommonDatabaseConstants.TILE_TYPE_DESERT);

	/** How far from cities the AI wants to put enchanted roads */
	private final static int ENCHANT_ROAD_DISTANCE = 8;
	
	/** Spell utils */
	private SpellUtils spellUtils;

	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Methods that determine whether something is a valid target for a spell */
	private SpellTargetingUtils spellTargetingUtils;
	
	/** AI spell calculations */
	private AISpellCalculations aiSpellCalculations;

	/** Spell queueing methods */
	private SpellQueueing spellQueueing;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** AI city calculations */
	private AICityCalculations aiCityCalculations;
	
	/** Spell processing methods */
	private SpellProcessing spellProcessing;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;

	/** AI decisions about units */
	private UnitAI unitAI;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
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
	
	/** Methods relating to casting spells in combat */
	private CombatSpellAI combatSpellAI;
	
	/** Kind of spell utils */
	private KindOfSpellUtils kindOfSpellUtils;
	
	/** Dispel magic processing */
	private SpellDispelling spellDispelling;
	
	/** Server-side only spell utils */
	private SpellServerUtils spellServerUtils;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Casting for each type of spell */
	private SpellCasting spellCasting;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
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
	 * @param wizardDetails AI wizard who needs to choose what to cast
	 * @param constructableUnits List of everything we can construct everywhere or summon
	 * @param wantedUnitTypesOnEachPlane Map of which unit types we need to construct or summon on each plane
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void decideWhatToCastOverland (final PlayerServerDetails player, final KnownWizardDetails wizardDetails, final List<AIConstructableUnit> constructableUnits,
		final Map<Integer, List<AIUnitType>> wantedUnitTypesOnEachPlane, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
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
				
				// Never use Disenchant Area if we have Disenchant Area True
				Spell bestDisenchantArea = null;
				Spell bestDisjunction = null;
				for (final Spell spell : mom.getServerDB ().getSpell ())
					if ((spell.getSpellBookSectionID () != null) && (getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.OVERLAND)) &&
						(getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spell.getSpellID ()).getStatus () == SpellResearchStatusID.AVAILABLE))
					{
						final KindOfSpell kind = getKindOfSpellUtils ().determineKindOfSpell (spell, null);
						if (kind == KindOfSpell.DISPEL_UNIT_CITY_COMBAT_SPELLS)
						{
							if ((bestDisenchantArea == null) || (spell.getOverlandBaseDamage () > bestDisenchantArea.getOverlandBaseDamage ()))
								bestDisenchantArea = spell;
						}
						else if (kind == KindOfSpell.DISPEL_OVERLAND_ENCHANTMENTS)
						{
							if ((bestDisjunction == null) || (spell.getOverlandBaseDamage () > bestDisjunction.getOverlandBaseDamage ()))
								bestDisjunction = spell;
						}
					}
				
				// Get a list of cities and warped nodes
				final Set<MapCoordinates3DEx> ourCities = new HashSet<MapCoordinates3DEx> ();
				final Set<MapCoordinates3DEx> enemyCities = new HashSet<MapCoordinates3DEx> ();
				final Set<MapCoordinates3DEx> ourWarpedNodes = new HashSet<MapCoordinates3DEx> ();
				
				for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
					for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
						for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
						{
							final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
							if ((mc != null) && (mc.getCityData () != null))
							{
								if (mc.getCityData ().getCityOwnerID () == player.getPlayerDescription ().getPlayerID ())
									ourCities.add (new MapCoordinates3DEx (x, y, z));
								else
									enemyCities.add (new MapCoordinates3DEx (x, y, z));
							}
							
							if ((mc != null) && (mc.getTerrainData () != null) && (mc.getTerrainData ().getNodeOwnerID () != null) &&
								(mc.getTerrainData ().getNodeOwnerID () == player.getPlayerDescription ().getPlayerID ()) &&
								(mc.getTerrainData ().isWarped () != null) && (mc.getTerrainData ().isWarped ()))
							{
								// Has to be actual node, not just an aura tile
								if (mom.getServerDB ().findTileType (mc.getTerrainData ().getTileTypeID (), "decideWhatToCastOverland").getMagicRealmID () != null)
									ourWarpedNodes.add (new MapCoordinates3DEx (x, y, z));
							}
						}
				
				// Consider every possible spell we could cast overland and can afford maintainence of
				for (final Spell spell : mom.getServerDB ().getSpell ())
					if ((spell.getSpellBookSectionID () != null) && (getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.OVERLAND)) &&
						(getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spell.getSpellID ()).getStatus () == SpellResearchStatusID.AVAILABLE))
					{
						if (!getAiSpellCalculations ().canAffordSpellMaintenance (player, wizardDetails, mom.getPlayers (), spell, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
							mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ()))
							
							log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " won't try to cast " + spell.getSpellID () + " because it can't afford the maintenance"); 
						else
						{
							final KindOfSpell kind = getKindOfSpellUtils ().determineKindOfSpell (spell, null);
							
							switch (kind)
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
									if ((spell.getOverlandCastingCost () >= minSummonCost) &&
										(summonableCombatUnits.containsKey (spell.getSpellRealm ())) && (summonableCombatUnits.get (spell.getSpellRealm ()).get (0).getSpell () == spell))
									{
										log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering casting summoning spell " + spell.getSpellID ());
										considerSpells.add (3, spell);
									}
									break;
									
								// City enchantments and curses - we don't pick the target until its finished casting, but must prove that there is a valid target to pick
								case CITY_ENCHANTMENTS:
								case CITY_CURSES:
								case ATTACK_UNITS_AND_BUILDINGS:
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
													if (getSpellTargetingUtils ().isCityValidTargetForSpell (priv.getFogOfWarMemory ().getMaintainedSpell (), spell,
														player.getPlayerDescription ().getPlayerID (), cityLocation, priv.getFogOfWarMemory ().getMap (), priv.getFogOfWar (),
														priv.getFogOfWarMemory ().getBuilding (), mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (),
														mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
														
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
									
								// Disenchant area - similar to above, prove there is a vaild target to pick
								case DISPEL_UNIT_CITY_COMBAT_SPELLS:
									if (spell == bestDisenchantArea)
									{
										// isOverlandLocationValidTargetForSpell will look for spells cast on cities, spells cast on units, and warped nodes.
										// Problem is, it will look for ALL warped nodes, not just our own, and it returns no info about WHY the choice is valid.
										// So really we have to repeat everything here.
										final int weighting;
										
										// First look for curses on our cities and any nodes we own that are warped
										if ((!ourWarpedNodes.isEmpty ()) || (priv.getFogOfWarMemory ().getMaintainedSpell ().stream ().anyMatch
											(s -> (s.getCastingPlayerID () != player.getPlayerDescription ().getPlayerID ()) && (ourCities.contains (s.getCityLocation ())))))
											
											weighting = 3;
										
										// This routine looks for anything we might possibly want to disenchant area, but we already know there's no curses on our cities or nodes from above
										else if (getSpellDispelling ().chooseDisenchantAreaTarget (player, mom) != null)
											weighting = 1;
											
										else
											weighting = 0;
										
										if (weighting > 0)
										{
											log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering casting Disenchant Area spell " + spell.getSpellID () + " with weighting " + weighting);
											considerSpells.add (weighting, spell);
										}
									}
									break;
									
								// Disjunction - weight according to worst overland enchantment any enemy wizard has
								case DISPEL_OVERLAND_ENCHANTMENTS:
									if (spell == bestDisjunction)
									{
										int weighting = 0;
										for (final MemoryMaintainedSpell enemySpell : priv.getFogOfWarMemory ().getMaintainedSpell ())
											if (enemySpell.getCastingPlayerID () != player.getPlayerDescription ().getPlayerID ())
											{
												final Spell spellDef = mom.getServerDB ().findSpell (enemySpell.getSpellID (), "decideWhatToCastOverland");
												if ((spellDef.getSpellBookSectionID () == SpellBookSectionID.OVERLAND_ENCHANTMENTS) && (spellDef.getDispelWeighting () > weighting))
													weighting = spellDef.getDispelWeighting ();
											}
										
										if (weighting > 0)
										{
											log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering casting Disjunction spell " + spell.getSpellID () + " with weighting " + weighting);
											considerSpells.add (weighting, spell);
										}
									}
									break;
									
								// Spell Binding - look for any overland enchantments that we don't know
								case SPELL_BINDING:
								{
									boolean found = false;
									final Iterator<MemoryMaintainedSpell> iter = priv.getFogOfWarMemory ().getMaintainedSpell ().iterator ();
									while ((!found) && (iter.hasNext ()))
									{
										final MemoryMaintainedSpell enemySpell = iter.next ();
										final Spell spellDef = mom.getServerDB ().findSpell (enemySpell.getSpellID (), "decideWhatToCastOverland");
										if ((spellDef.getSpellBookSectionID () == SpellBookSectionID.OVERLAND_ENCHANTMENTS) &&
											(getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), enemySpell.getSpellID ()).getStatus () != SpellResearchStatusID.AVAILABLE))
											
											found = true;
									}
									if (found)
									{
										log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering casting Spell Binding");
										considerSpells.add (4, spell);
									}
									break;
								}
								
								// Warp node
								case WARP_NODE:
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
												final MapCoordinates3DEx nodeLocation = new MapCoordinates3DEx (x, y, z);
												
												// Routine checks everything, looking for visible enemy owned unwarped nodes
												if (getSpellTargetingUtils ().isOverlandLocationValidTargetForSpell (spell,
													player.getPlayerDescription ().getPlayerID (), nodeLocation, priv.getFogOfWarMemory (), priv.getFogOfWar (),
													mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
													
													validTargetFound = true;
		
												x++;
											}
											y++;
										}
										z++;
									}
									if (validTargetFound)
									{
										log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering casting Warp Node");
										considerSpells.add (3, spell);
									}
									break;
								}
									
								// Unit enchantments - again don't pick target until its finished casting
								case UNIT_ENCHANTMENTS:
								case CHANGE_UNIT_ID:
								{
									boolean validTargetFound = false;
									final Iterator<MemoryUnit> iter = priv.getFogOfWarMemory ().getUnit ().iterator ();
									while ((!validTargetFound) && (iter.hasNext ()))
									{
										final MemoryUnit mu = iter.next ();
										final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (mu, null, null, null,
											mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ());
										if ((getAiUnitCalculations ().determineAIUnitType (xu) == AIUnitType.COMBAT_UNIT) &&
											(getSpellTargetingUtils ().isUnitValidTargetForSpell (spell, null, null, null, player.getPlayerDescription ().getPlayerID (), null, null, xu, true,
												priv.getFogOfWarMemory (), priv.getFogOfWar (), mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET))
											
											validTargetFound = true;
									}
	
									if (validTargetFound)
									{
										log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering unit enchantment spell " + spell.getSpellID ());
										considerSpells.add (1, spell);
									}
									
									break;
								}
									
								// Special spells with no target to choose (especially spell of mastery)
								case SPECIAL_SPELLS:
									if (spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SPELL_OF_MASTERY))
										considerSpells.add (10, spell);
									else
									{
										// Global attack spell like Great Unsummoning or Death Wish; so see how many of our units we might hit vs how many of everyone else's
										final List<MemoryUnit> targetUnits = getSpellServerUtils ().listGlobalAttackTargets (spell, player, true, mom);
										final long ourTargetUnits = targetUnits.stream ().filter (u -> u.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()).count ();
										final long enemyTargetUnits = targetUnits.stream ().filter (u -> u.getOwningPlayerID () != player.getPlayerDescription ().getPlayerID ()).count ();
										if ((ourTargetUnits < 5) && (enemyTargetUnits > 15))
											considerSpells.add (2, spell);
									}
									break;
									
								// Spells that directly target wizards - just make sure there's a non-banished wizard
								case ENEMY_WIZARD_SPELLS:
								{
									boolean validPlayerFound = false;
									final Iterator<PlayerServerDetails> iter = mom.getPlayers ().iterator ();
									while ((!validPlayerFound) && (iter.hasNext ()))
									{
										final PlayerServerDetails targetPlayer = iter.next ();
										
										// Don't need OverlandCastingInfo since this isn't Spell Blast
										if (getSpellTargetingUtils ().isWizardValidTargetForSpell (spell, player.getPlayerDescription ().getPlayerID (),
											priv, targetPlayer.getPlayerDescription ().getPlayerID (), null) == TargetSpellResult.VALID_TARGET)
											
											validPlayerFound = true;
									}
									
									if (validPlayerFound)
									{
										log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering enemy wizard spell " + spell.getSpellID ());
										considerSpells.add (2, spell);
									}
									
									break;
								}
								
								// Spell Blast
								case SPELL_BLAST:
									// We have to have Detect Magic cast, or we don't know if there's anything nasty being cast that's worth blasting
									if (getMemoryMaintainedSpellUtils ().findMaintainedSpell (priv.getFogOfWarMemory ().getMaintainedSpell (), player.getPlayerDescription ().getPlayerID (),
										CommonDatabaseConstants.SPELL_ID_DETECT_MAGIC, null, null, null, null) != null)
									{
										int weighting = 0;
										for (final PlayerServerDetails targetPlayer : mom.getPlayers ())
											if (targetPlayer != player)
											{
												final KnownWizardDetails targetWizard = getKnownWizardUtils ().findKnownWizardDetails
													(priv.getFogOfWarMemory ().getWizardDetails (), targetPlayer.getPlayerDescription ().getPlayerID ());
												
												if ((targetWizard != null) && (getPlayerKnowledgeUtils ().isWizard (targetWizard.getWizardID ())) && (targetWizard.getWizardState () == WizardState.ACTIVE))
												{
													final OverlandCastingInfo castingInfo = getSpellCasting ().createOverlandCastingInfo (targetPlayer, CommonDatabaseConstants.SPELL_ID_DETECT_MAGIC);
													if (castingInfo.getSpellID () != null)
													{
														final Spell targetSpell = mom.getServerDB ().findSpell (castingInfo.getSpellID (), "decideWhatToCastOverland");
														if ((targetSpell.getSpellBlastWeighting () != null) && (targetSpell.getSpellBlastWeighting () > weighting))
															weighting = targetSpell.getSpellBlastWeighting ();
													}
												}
											}
										
										if (weighting > 0)
										{
											log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering Spell Blast " + spell.getSpellID () + " with weighting " + weighting);
											considerSpells.add (weighting, spell);
										}
									}
									break;
									
								// Corruption, Raise Volcano and Change Terrain
								case CORRUPTION:
								case CHANGE_TILE_TYPE:
									
									// Corruption and Raise Volcano
									if ((kind == KindOfSpell.CORRUPTION) || (spell.getSpellValidTileTypeTarget ().get (0).getChangeToTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_RAISE_VOLCANO)))
									{
										// Look for a land tile near an enemy city, that isn't also near one of our cities
										final Set<MapCoordinates3DEx> nearOurCities = new HashSet<MapCoordinates3DEx> ();
										for (final MapCoordinates3DEx ourCityLocation : ourCities)
										{
											final MapCoordinates3DEx coords = new MapCoordinates3DEx (ourCityLocation);
											for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
												if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
													nearOurCities.add (new MapCoordinates3DEx (coords));
										}

										boolean found = false;
										final Iterator<MapCoordinates3DEx> enemyCitiesIter = enemyCities.iterator ();
										while ((!found) && (enemyCitiesIter.hasNext ()))
										{
											final MapCoordinates3DEx enemyCityLocation = enemyCitiesIter.next ();
											final MapCoordinates3DEx coords = new MapCoordinates3DEx (enemyCityLocation);
											for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
												if ((getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ())) &&
													(!nearOurCities.contains (coords)) && (getSpellTargetingUtils ().isOverlandLocationValidTargetForSpell
														(spell, player.getPlayerDescription ().getPlayerID (), coords, priv.getFogOfWarMemory (), priv.getFogOfWar (),
															mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET))
													
													found = true;
										}
										
										if (found)
										{
											log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering Corruption/Volcano spell " + spell.getSpellID ());
											considerSpells.add (1, spell);
										}
									}
									else
									{
										// Change Terrain - look for any tile near an enemy city that isn't grass 
										int weighting = 0;
										final Set<MapCoordinates3DEx> nearEnemyCities = new HashSet<MapCoordinates3DEx> ();
										for (final MapCoordinates3DEx enemyCityLocation : enemyCities) 
										{
											final MapCoordinates3DEx coords = new MapCoordinates3DEx (enemyCityLocation);
											for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
												if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
												{
													nearEnemyCities.add (new MapCoordinates3DEx (coords));
													
													final OverlandMapTerrainData terrainData = priv.getFogOfWarMemory ().getMap ().getPlane ().get
														(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
													
													if ((getSpellTargetingUtils ().isOverlandLocationValidTargetForSpell
														(spell, player.getPlayerDescription ().getPlayerID (), coords, priv.getFogOfWarMemory (), priv.getFogOfWar (),
															mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET) &&
														(terrainData != null) && (terrainData.getTileTypeID () != null) && (!terrainData.getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_GRASS)))	
													
														weighting = 1;
												}
										}
										
										// Also look for any of our cities that have deserts, swamps, or don't have at least one forest tile
										final Iterator<MapCoordinates3DEx> ourCitiesIter = ourCities.iterator ();
										while ((weighting < 3) && (ourCitiesIter.hasNext ()))
										{
											final MapCoordinates3DEx ourCityLocation = ourCitiesIter.next ();
											final MapCoordinates3DEx coords = new MapCoordinates3DEx (ourCityLocation);
											
											boolean forestTileFound = false;
											boolean grassTileNotNearEnemyCityFound = false;
											for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
												if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
												{
													final OverlandMapTerrainData terrainData = priv.getFogOfWarMemory ().getMap ().getPlane ().get
														(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
													
													if ((terrainData != null) && (terrainData.getTileTypeID () != null))
													{
														if (terrainData.getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_FOREST))
															forestTileFound = true;
														else if ((terrainData.getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_GRASS)) && (!nearEnemyCities.contains (coords)))
															grassTileNotNearEnemyCityFound = true;
														
														else if ((BAD_TILE_TYPES.contains (terrainData.getTileTypeID ())) &&
															(getSpellTargetingUtils ().isOverlandLocationValidTargetForSpell
																(spell, player.getPlayerDescription ().getPlayerID (), coords, priv.getFogOfWarMemory (), priv.getFogOfWar (),
																	mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET))
															weighting = 3;														
													}
												}
											
											if ((grassTileNotNearEnemyCityFound) && (!forestTileFound))
												weighting = 3;
										}

										if (weighting > 0)
										{
											log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering Change Terrain " + spell.getSpellID () + " with weighting " + weighting);
											considerSpells.add (weighting, spell);
										}
									}
									break;
									
								// Transmute
								case CHANGE_MAP_FEATURE:
								{
									boolean found = false;
									
									// Look for beneficial map feature near an enemy city that we can change into a worse one
									final Set<MapCoordinates3DEx> nearEnemyCities = new HashSet<MapCoordinates3DEx> ();
									
									final Iterator<MapCoordinates3DEx> enemyCitiesIter = enemyCities.iterator ();
									while ((!found) && (enemyCitiesIter.hasNext ()))
									{
										final MapCoordinates3DEx enemyCityLocation = enemyCitiesIter.next ();
										final MapCoordinates3DEx coords = new MapCoordinates3DEx (enemyCityLocation);
										
										for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
											if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
											{
												nearEnemyCities.add (new MapCoordinates3DEx (coords));
												
												final OverlandMapTerrainData terrainData = priv.getFogOfWarMemory ().getMap ().getPlane ().get
													(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
												
												if ((getSpellTargetingUtils ().isOverlandLocationValidTargetForSpell
													(spell, player.getPlayerDescription ().getPlayerID (), coords, priv.getFogOfWarMemory (), priv.getFogOfWar (),
														mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET) &&
													(terrainData != null) && (terrainData.getMapFeatureID () != null))
												{
													final SpellValidMapFeatureTarget targetMapFeature = spell.getSpellValidMapFeatureTarget ().stream ().filter
														(t -> t.getMapFeatureID ().equals (terrainData.getMapFeatureID ())).findAny ().orElse (null);
													if ((targetMapFeature != null) && (!targetMapFeature.isChangeBeneficial ()))
														found = true;
												}
											}
									}
									
									// Look for poor map feature near our city that we can change into a better one
									final Iterator<MapCoordinates3DEx> ourCitiesIter = ourCities.iterator ();
									while ((!found) && (ourCitiesIter.hasNext ()))
									{
										final MapCoordinates3DEx ourCityLocation = ourCitiesIter.next ();
										final MapCoordinates3DEx coords = new MapCoordinates3DEx (ourCityLocation);
										
										for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
											if ((getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ())) &&
												(!nearEnemyCities.contains (coords)))
											{
												final OverlandMapTerrainData terrainData = priv.getFogOfWarMemory ().getMap ().getPlane ().get
													(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
												
												if ((getSpellTargetingUtils ().isOverlandLocationValidTargetForSpell
													(spell, player.getPlayerDescription ().getPlayerID (), coords, priv.getFogOfWarMemory (), priv.getFogOfWar (),
														mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET) &&
													(terrainData != null) && (terrainData.getMapFeatureID () != null))
												{
													final SpellValidMapFeatureTarget targetMapFeature = spell.getSpellValidMapFeatureTarget ().stream ().filter
														(t -> t.getMapFeatureID ().equals (terrainData.getMapFeatureID ())).findAny ().orElse (null);
													if ((targetMapFeature != null) && (targetMapFeature.isChangeBeneficial ()))
														found = true;
												}
											}
									}
									
									if (found)
									{
										log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering Transmute spell " + spell.getSpellID ());
										considerSpells.add (3, spell);
									}
									break;
								}
								
								// Enchant Road - look for any normal road tiles with n squares of our cities
								case ENCHANT_ROAD:
								{
									boolean validTargetFound = false;
									final int z = 0;
									int y = 0;
									while ((!validTargetFound) && (y < mom.getSessionDescription ().getOverlandMapSize ().getHeight ()))
									{
										int x = 0;
										while ((!validTargetFound) && (x < mom.getSessionDescription ().getOverlandMapSize ().getWidth ()))
										{
											final OverlandMapTerrainData terrainData = priv.getFogOfWarMemory ().getMap ().getPlane ().get
												(z).getRow ().get (y).getCell ().get (x).getTerrainData ();
											if ((terrainData != null) && (CommonDatabaseConstants.TILE_TYPE_NORMAL_ROAD.equals (terrainData.getRoadTileTypeID ())))
											{
												int shortestDistance = Integer.MAX_VALUE;
												for (final MapCoordinates3DEx ourCityLocation : ourCities)
													shortestDistance = Math.min (shortestDistance, getCoordinateSystemUtils ().determineStep2DDistanceBetween
														(mom.getSessionDescription ().getOverlandMapSize (), x, y, ourCityLocation.getX (), ourCityLocation.getY ()));
												
												if (shortestDistance <= ENCHANT_ROAD_DISTANCE)
													validTargetFound = true;
											}
												
											x++;
										}
										y++;
									}
									
									if (validTargetFound)
									{
										log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering casting Enchant Road");
										considerSpells.add (1, spell);
									}
									break;
								}
								
								// This is fine, the AI doesn't cast every type of spell yet
								default:
							}
						}
					}
			}
						
			// If we found any, then pick one randomly
			final Spell spell = considerSpells.nextWeightedValue ();
			if (spell == null)
				log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " decided not to cast anything overland");
			else
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
	 * @param wizardDetails AI wizard who needs to choose what to cast
	 * @param spell Definition for the spell to target
	 * @param maintainedSpell Spell being targetted in server's true memory - at the time this is called, this is the only copy of the spell that exists,
	 * 	so its the only thing we need to clean up
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void decideOverlandSpellTarget (final PlayerServerDetails player, final KnownWizardDetails wizardDetails,
		final Spell spell, final MemoryMaintainedSpell maintainedSpell, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		MapCoordinates3DEx targetLocation = null;
		MemoryUnit targetUnit = null;
		MemoryMaintainedSpell targetSpell = null;
		Integer targetPlayerID = null;
		String citySpellEffectID = null;
		String unitSkillID = null;
		
		final KindOfSpell kind = getKindOfSpellUtils ().determineKindOfSpell (spell, null);
		
		switch (kind)
		{
			// City enchantments and curses, including Spell of Return
			case CITY_ENCHANTMENTS:
			case CITY_CURSES:
			case ATTACK_UNITS_AND_BUILDINGS:
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
								if (getSpellTargetingUtils ().isCityValidTargetForSpell (priv.getFogOfWarMemory ().getMaintainedSpell (), spell,
									player.getPlayerDescription ().getPlayerID (), cityLocation, priv.getFogOfWarMemory ().getMap (), priv.getFogOfWar (),
									priv.getFogOfWarMemory ().getBuilding (), mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (),
									mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
								{
									final int thisCityQuality = getAiCityCalculations ().evaluateCityQuality (cityLocation, false, false, priv.getFogOfWarMemory (), mom);
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
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (mu, null, null, null,
						mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ());
					if ((getAiUnitCalculations ().determineAIUnitType (xu) == AIUnitType.COMBAT_UNIT) &&
						(getSpellTargetingUtils ().isUnitValidTargetForSpell (spell, null, null, null, player.getPlayerDescription ().getPlayerID (), null, null, xu, true,
							priv.getFogOfWarMemory (), priv.getFogOfWar (), mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET))
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
				
			// Lycanthrophy - prefer casting on the worst unit possible, rather than the best:
			case CHANGE_UNIT_ID:
				int worstUnitRating = Integer.MAX_VALUE;
				for (final MemoryUnit mu : priv.getFogOfWarMemory ().getUnit ())
				{
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (mu, null, null, null,
						mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ());
					if ((getAiUnitCalculations ().determineAIUnitType (xu) == AIUnitType.COMBAT_UNIT) &&
						(getSpellTargetingUtils ().isUnitValidTargetForSpell (spell, null, null, null, player.getPlayerDescription ().getPlayerID (), null, null, xu, true,
							priv.getFogOfWarMemory (), priv.getFogOfWar (), mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET))
					{
						int thisUnitRating = getAiUnitCalculations ().calculateUnitAverageRating (xu.getUnit (), xu, mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ());
						if ((targetUnit == null) || (thisUnitRating < worstUnitRating))
						{
							targetUnit = mu;
							worstUnitRating = thisUnitRating;
						}
					}
				}
				break;
				
			// Disenchant area
			case DISPEL_UNIT_CITY_COMBAT_SPELLS:
				targetLocation = getSpellDispelling ().chooseDisenchantAreaTarget (player, mom);
				break;
				
			// Disjunction
			case DISPEL_OVERLAND_ENCHANTMENTS:
			{
				int bestWeighting = 0;
				for (final MemoryMaintainedSpell enemySpell : priv.getFogOfWarMemory ().getMaintainedSpell ())
					if (enemySpell.getCastingPlayerID () != player.getPlayerDescription ().getPlayerID ())
					{
						final Spell spellDef = mom.getServerDB ().findSpell (enemySpell.getSpellID (), "decideOverlandSpellTarget");
						if ((spellDef.getSpellBookSectionID () == SpellBookSectionID.OVERLAND_ENCHANTMENTS) &&
							((targetSpell == null) || (spellDef.getDispelWeighting () > bestWeighting)))
						{
							targetSpell = enemySpell;
							bestWeighting = spellDef.getDispelWeighting ();
						}
					}				
				break;
			}

			// Spell Binding - look for any overland enchantments that we don't know
			case SPELL_BINDING:
			{
				final List<MemoryMaintainedSpell> primaryTargets = new ArrayList<MemoryMaintainedSpell> ();
				final List<MemoryMaintainedSpell> secondaryTargets = new ArrayList<MemoryMaintainedSpell> ();
				
				for (final MemoryMaintainedSpell enemySpell : priv.getFogOfWarMemory ().getMaintainedSpell ())
					if (enemySpell.getCastingPlayerID () != player.getPlayerDescription ().getPlayerID ())
					{
						final Spell spellDef = mom.getServerDB ().findSpell (enemySpell.getSpellID (), "decideOverlandSpellTarget");
						if (spellDef.getSpellBookSectionID () == SpellBookSectionID.OVERLAND_ENCHANTMENTS)
						{
							if (getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), enemySpell.getSpellID ()).getStatus () != SpellResearchStatusID.AVAILABLE)
								primaryTargets.add (enemySpell);
							else
								secondaryTargets.add (enemySpell);
						}
					}
				
				if (!primaryTargets.isEmpty ())
					targetSpell = primaryTargets.get (getRandomUtils ().nextInt (primaryTargets.size ()));
				else if (!secondaryTargets.isEmpty ())
					targetSpell = secondaryTargets.get (getRandomUtils ().nextInt (secondaryTargets.size ()));
				
				break;
			}
				
			// Warp node
			case WARP_NODE:
			{
				final List<MapCoordinates3DEx> warpedNodes = new ArrayList<MapCoordinates3DEx> ();
				
				for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
					for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
						for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
						{
							final MapCoordinates3DEx nodeLocation = new MapCoordinates3DEx (x, y, z);
							
							// Routine checks everything, looking for visible enemy owned unwarped nodes
							if (getSpellTargetingUtils ().isOverlandLocationValidTargetForSpell (spell,
								player.getPlayerDescription ().getPlayerID (), nodeLocation, priv.getFogOfWarMemory (), priv.getFogOfWar (),
								mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
								
								warpedNodes.add (nodeLocation);

						}
				
				if (!warpedNodes.isEmpty ())
					targetLocation = warpedNodes.get (getRandomUtils ().nextInt (warpedNodes.size ()));
				
				break;				
			}
				
			// Spells that directly target wizards - prefer targeting strong wizards, same as beneficial random events do
			case ENEMY_WIZARD_SPELLS:
			{
				final WeightedChoicesImpl<Integer> playerChoices = new WeightedChoicesImpl<Integer> ();
				playerChoices.setRandomUtils (getRandomUtils ());

				for (final PlayerServerDetails targetPlayer : mom.getPlayers ())
					if (getSpellTargetingUtils ().isWizardValidTargetForSpell (spell, player.getPlayerDescription ().getPlayerID (),
						priv, targetPlayer.getPlayerDescription ().getPlayerID (), null) == TargetSpellResult.VALID_TARGET)
					{
						final MomPersistentPlayerPrivateKnowledge targetPriv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
						final int thisPowerBase = getResourceValueUtils ().findAmountPerTurnForProductionType (targetPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
						playerChoices.add (10 + thisPowerBase, targetPlayer.getPlayerDescription ().getPlayerID ());
					}
				
				targetPlayerID = playerChoices.nextWeightedValue ();
				break;
			}

			// Spell Blast
			case SPELL_BLAST:
			{
				// What's the maximum MP we couild generate with alchemy?
				final boolean alchemyRetort = (getPlayerPickUtils ().getQuantityOfPick (wizardDetails.getPick (), CommonDatabaseConstants.RETORT_ID_ALCHEMY) > 0);
				
				final int manaStored = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
				final int goldStored = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
				final int maximumMana = manaStored + (alchemyRetort ? goldStored : (goldStored/2));
				
				// No randomness, just blast the nastiest thing that's being cast
				int bestWeighting = 0;
				int bestBlastingCost = 0;
				for (final PlayerServerDetails targetPlayer : mom.getPlayers ())
					if (targetPlayer != player)
					{
						final KnownWizardDetails targetWizard = getKnownWizardUtils ().findKnownWizardDetails
							(priv.getFogOfWarMemory ().getWizardDetails (), targetPlayer.getPlayerDescription ().getPlayerID ());
						
						if ((targetWizard != null) && (getPlayerKnowledgeUtils ().isWizard (targetWizard.getWizardID ())) && (targetWizard.getWizardState () == WizardState.ACTIVE))
						{
							final OverlandCastingInfo castingInfo = getSpellCasting ().createOverlandCastingInfo (targetPlayer, spell.getSpellID ());
							if (castingInfo.getSpellID () != null)
							{
								final Spell targetSpellDef = mom.getServerDB ().findSpell (castingInfo.getSpellID (), "decideWhatToCastOverland");
								if ((targetSpellDef.getSpellBlastWeighting () != null) && (targetSpellDef.getSpellBlastWeighting () > bestWeighting))
								{
									// Found one we want to blast, but have we got enough MP?
									final int blastingCost = castingInfo.getManaSpentOnCasting ();
									if (maximumMana >= blastingCost)
									{
										bestWeighting = targetSpellDef.getSpellBlastWeighting ();
										bestBlastingCost = blastingCost;
										targetPlayerID = targetPlayer.getPlayerDescription ().getPlayerID ();
									}
								}
							}
						}
					}
				
				// Use alchemy to generate enough MP
				if ((bestBlastingCost > 0) && (bestBlastingCost > manaStored))
				{
					final int manaToConvert = bestBlastingCost - manaStored;
					final int goldToConvert = alchemyRetort ? manaToConvert : (manaToConvert * 2);
					
					log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " converting " + goldToConvert + " GP into " + manaToConvert + " MP so it can afford to Spell Blast");
					
					getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, -goldToConvert);
					getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, manaToConvert);
				}
				
				break;
			}
			
			// Corruption, Raise Volcano and Change Terrain
			case CORRUPTION:
			case CHANGE_TILE_TYPE:
			{
				final List<MapCoordinates3DEx> primaryTargets = new ArrayList<MapCoordinates3DEx> ();
				final List<MapCoordinates3DEx> secondaryTargets = new ArrayList<MapCoordinates3DEx> ();

				// Get a list of cities
				final Set<MapCoordinates3DEx> ourCities = new HashSet<MapCoordinates3DEx> ();
				final Set<MapCoordinates3DEx> enemyCities = new HashSet<MapCoordinates3DEx> ();
				
				for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
					for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
						for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
						{
							final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
							if ((mc != null) && (mc.getCityData () != null))
							{
								if (mc.getCityData ().getCityOwnerID () == player.getPlayerDescription ().getPlayerID ())
									ourCities.add (new MapCoordinates3DEx (x, y, z));
								else
									enemyCities.add (new MapCoordinates3DEx (x, y, z));
							}
						}
				
				// Corruption and Raise Volcano
				if ((kind == KindOfSpell.CORRUPTION) || (spell.getSpellValidTileTypeTarget ().get (0).getChangeToTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_RAISE_VOLCANO)))
				{
					// Look for a land tile near an enemy city, that isn't also near one of our cities
					final Set<MapCoordinates3DEx> nearOurCities = new HashSet<MapCoordinates3DEx> ();
					for (final MapCoordinates3DEx ourCityLocation : ourCities)
					{
						final MapCoordinates3DEx coords = new MapCoordinates3DEx (ourCityLocation);
						for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
							if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
								nearOurCities.add (new MapCoordinates3DEx (coords));
					}
	
					for (final MapCoordinates3DEx enemyCityLocation : enemyCities)
					{
						final MapCoordinates3DEx coords = new MapCoordinates3DEx (enemyCityLocation);
						for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
							if ((getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ())) &&
								(!nearOurCities.contains (coords)) && (getSpellTargetingUtils ().isOverlandLocationValidTargetForSpell
									(spell, player.getPlayerDescription ().getPlayerID (), coords, priv.getFogOfWarMemory (), priv.getFogOfWar (),
										mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET))
							{
								// Aim for minerals first
								final OverlandMapTerrainData terrainData = priv.getFogOfWarMemory ().getMap ().getPlane ().get
									(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
								if (terrainData.getMapFeatureID () != null)
								{
									final MapFeatureEx mapFeature = mom.getServerDB ().findMapFeature (terrainData.getMapFeatureID (), "decideOverlandSpellTarget");
									if (mapFeature.getMapFeatureMagicRealm ().isEmpty ())
										primaryTargets.add (new MapCoordinates3DEx (coords));
									else
										secondaryTargets.add (new MapCoordinates3DEx (coords));
								}
							}
					}
				}
				else
				{
					// Change Terrain - look for any tile near an enemy city that isn't grass 
					final Set<MapCoordinates3DEx> nearEnemyCities = new HashSet<MapCoordinates3DEx> ();
					for (final MapCoordinates3DEx enemyCityLocation : enemyCities) 
					{
						final MapCoordinates3DEx coords = new MapCoordinates3DEx (enemyCityLocation);
						for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
							if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
							{
								nearEnemyCities.add (new MapCoordinates3DEx (coords));
								
								final OverlandMapTerrainData terrainData = priv.getFogOfWarMemory ().getMap ().getPlane ().get
									(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
								
								if ((getSpellTargetingUtils ().isOverlandLocationValidTargetForSpell
									(spell, player.getPlayerDescription ().getPlayerID (), coords, priv.getFogOfWarMemory (), priv.getFogOfWar (),
										mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET) &&
									(terrainData != null) && (terrainData.getTileTypeID () != null) && (!terrainData.getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_GRASS)))	
								
									secondaryTargets.add (new MapCoordinates3DEx (coords));
							}
					}
					
					// Also look for any of our cities that have deserts, swamps, or don't have at least one forest tile
					for (final MapCoordinates3DEx ourCityLocation : ourCities)
					{
						final MapCoordinates3DEx coords = new MapCoordinates3DEx (ourCityLocation);
						
						boolean forestTileFound = false;
						final Set<MapCoordinates3DEx> grassTilesNotNearEnemyCity = new HashSet<MapCoordinates3DEx> ();
						
						for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
							if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
							{
								final OverlandMapTerrainData terrainData = priv.getFogOfWarMemory ().getMap ().getPlane ().get
									(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
								
								if ((terrainData != null) && (terrainData.getTileTypeID () != null))
								{
									if (terrainData.getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_FOREST))
										forestTileFound = true;
									else if ((terrainData.getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_GRASS)) && (!nearEnemyCities.contains (coords)))
										grassTilesNotNearEnemyCity.add (new MapCoordinates3DEx (coords));
									
									else if ((BAD_TILE_TYPES.contains (terrainData.getTileTypeID ())) &&
										(getSpellTargetingUtils ().isOverlandLocationValidTargetForSpell
											(spell, player.getPlayerDescription ().getPlayerID (), coords, priv.getFogOfWarMemory (), priv.getFogOfWar (),
												mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET))
										primaryTargets.add (new MapCoordinates3DEx (coords));
								}
							}
						
						if ((!grassTilesNotNearEnemyCity.isEmpty ()) && (!forestTileFound))
							primaryTargets.addAll (grassTilesNotNearEnemyCity);
					}
				}
				
				if (!primaryTargets.isEmpty ())
					targetLocation = primaryTargets.get (getRandomUtils ().nextInt (primaryTargets.size ()));
				else if (!secondaryTargets.isEmpty ())
					targetLocation = secondaryTargets.get (getRandomUtils ().nextInt (secondaryTargets.size ()));
				break;
			}
			
			// Transmute
			case CHANGE_MAP_FEATURE:
			{
				// Get a list of cities
				final Set<MapCoordinates3DEx> ourCities = new HashSet<MapCoordinates3DEx> ();
				final Set<MapCoordinates3DEx> enemyCities = new HashSet<MapCoordinates3DEx> ();
				
				for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
					for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
						for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
						{
							final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
							if ((mc != null) && (mc.getCityData () != null))
							{
								if (mc.getCityData ().getCityOwnerID () == player.getPlayerDescription ().getPlayerID ())
									ourCities.add (new MapCoordinates3DEx (x, y, z));
								else
									enemyCities.add (new MapCoordinates3DEx (x, y, z));
							}
						}

				// Look for beneficial map feature near an enemy city that we can change into a worse one
				final List<MapCoordinates3DEx> targetLocations = new ArrayList<MapCoordinates3DEx> ();
				final Set<MapCoordinates3DEx> nearEnemyCities = new HashSet<MapCoordinates3DEx> ();
				
				for (final MapCoordinates3DEx enemyCityLocation : enemyCities)
				{
					final MapCoordinates3DEx coords = new MapCoordinates3DEx (enemyCityLocation);
					
					for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
						if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
						{
							nearEnemyCities.add (new MapCoordinates3DEx (coords));
							
							final OverlandMapTerrainData terrainData = priv.getFogOfWarMemory ().getMap ().getPlane ().get
								(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
							
							if ((getSpellTargetingUtils ().isOverlandLocationValidTargetForSpell
								(spell, player.getPlayerDescription ().getPlayerID (), coords, priv.getFogOfWarMemory (), priv.getFogOfWar (),
									mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET) &&
								(terrainData != null) && (terrainData.getMapFeatureID () != null))
							{
								final SpellValidMapFeatureTarget targetMapFeature = spell.getSpellValidMapFeatureTarget ().stream ().filter
									(t -> t.getMapFeatureID ().equals (terrainData.getMapFeatureID ())).findAny ().orElse (null);
								if ((targetMapFeature != null) && (!targetMapFeature.isChangeBeneficial ()))
									targetLocations.add (new MapCoordinates3DEx (coords));
							}
						}
				}
				
				// Look for poor map feature near our city that we can change into a better one
				for (final MapCoordinates3DEx ourCityLocation : ourCities)
				{
					final MapCoordinates3DEx coords = new MapCoordinates3DEx (ourCityLocation);
					
					for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
						if ((getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ())) &&
							(!nearEnemyCities.contains (coords)))
						{
							final OverlandMapTerrainData terrainData = priv.getFogOfWarMemory ().getMap ().getPlane ().get
								(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
							
							if ((getSpellTargetingUtils ().isOverlandLocationValidTargetForSpell
								(spell, player.getPlayerDescription ().getPlayerID (), coords, priv.getFogOfWarMemory (), priv.getFogOfWar (),
									mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET) &&
								(terrainData != null) && (terrainData.getMapFeatureID () != null))
							{
								final SpellValidMapFeatureTarget targetMapFeature = spell.getSpellValidMapFeatureTarget ().stream ().filter
									(t -> t.getMapFeatureID ().equals (terrainData.getMapFeatureID ())).findAny ().orElse (null);
								if ((targetMapFeature != null) && (targetMapFeature.isChangeBeneficial ()))
									targetLocations.add (new MapCoordinates3DEx (coords));
							}
						}
				}
				
				if (!targetLocations.isEmpty ())
					targetLocation = targetLocations.get (getRandomUtils ().nextInt (targetLocations.size ()));
				
				break;
			}
			
			// Enchant Road - look for any normal road tiles with n squares of our cities
			case ENCHANT_ROAD:
			{
				// Get a list of cities
				final Set<MapCoordinates3DEx> ourCities = new HashSet<MapCoordinates3DEx> ();
				
				for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
					for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
						for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
						{
							final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
							if ((mc != null) && (mc.getCityData () != null) && (mc.getCityData ().getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))
								ourCities.add (new MapCoordinates3DEx (x, y, z));
						}
				
				final List<MapCoordinates3DEx> targetLocations = new ArrayList<MapCoordinates3DEx> ();
				final int z = 0;
				for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					{
						final OverlandMapTerrainData terrainData = priv.getFogOfWarMemory ().getMap ().getPlane ().get
							(z).getRow ().get (y).getCell ().get (x).getTerrainData ();
						if ((terrainData != null) && (CommonDatabaseConstants.TILE_TYPE_NORMAL_ROAD.equals (terrainData.getRoadTileTypeID ())))
						{
							int shortestDistance = Integer.MAX_VALUE;
							for (final MapCoordinates3DEx ourCityLocation : ourCities)
								shortestDistance = Math.min (shortestDistance, getCoordinateSystemUtils ().determineStep2DDistanceBetween
									(mom.getSessionDescription ().getOverlandMapSize (), x, y, ourCityLocation.getX (), ourCityLocation.getY ()));
							
							if (shortestDistance <= ENCHANT_ROAD_DISTANCE)
								targetLocations.add (new MapCoordinates3DEx (x, y, z));
						}
					}
				
				if (!targetLocations.isEmpty ())
					targetLocation = targetLocations.get (getRandomUtils ().nextInt (targetLocations.size ()));
				
				break;
			}
				
			default:
				throw new MomException ("AI decideSpellTarget does not know how to decide a target for spell " + spell.getSpellID () + " in section " + spell.getSpellBookSectionID ());
		}
		
		// If we got a location then target the spell; if we didn't then cancel the spell
		if ((targetLocation == null) && (targetUnit == null) && (targetSpell == null) && (targetPlayerID == null))
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
					final List<UnitSpellEffect> unitSpellEffects = getMemoryMaintainedSpellUtils ().listUnitSpellEffectsNotYetCastOnUnit
						(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), spell, player.getPlayerDescription ().getPlayerID (), targetUnit.getUnitURN ());
					if ((unitSpellEffects != null) && (unitSpellEffects.size () > 0))
						unitSkillID = unitSpellEffects.get (getRandomUtils ().nextInt (unitSpellEffects.size ())).getUnitSkillID ();
					break;
			
				// This is fine, only need to do this for certain types of spells
				default:
			}
			
			// Target it
			getSpellProcessing ().targetOverlandSpell (spell, maintainedSpell, targetPlayerID, targetLocation, targetUnit, targetSpell, citySpellEffectID, unitSkillID, mom);
		}
	}

	/**
	 * AI player decides whether to cast a spell in combat
	 * 
	 * @param player AI player who needs to choose what to cast
	 * @param wizardDetails AI wizard who needs to choose what to cast
	 * @param combatCastingUnit Unit who is casting the spell; null means its the wizard casting, rather than a specific unit
	 * @param combatDetails Details about the combat taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether a spell was cast or not
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final CombatAIMovementResult decideWhatToCastCombat (final PlayerServerDetails player, final KnownWizardDetails wizardDetails,
		final ExpandedUnitDetails combatCastingUnit, final CombatDetails combatDetails, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Units with the caster skill (Archangels, Efreets and Djinns) cast spells from their magic realm, totally ignoring whatever spells their controlling wizard knows.
		// Using getModifiedUnitMagicRealmLifeformTypeID makes this account for them casting Death spells instead if you get an undead Archangel or similar.
		String overridePickID = null;
		if ((combatCastingUnit != null) && (combatCastingUnit.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT)) && (!combatCastingUnit.isHero ()))
		{
			overridePickID = combatCastingUnit.getModifiedUnitMagicRealmLifeformType ().getCastSpellsFromPickID ();
			if (overridePickID == null)
				overridePickID = combatCastingUnit.getModifiedUnitMagicRealmLifeformType ().getPickID ();
		}
		
		// Now we can go through every spell in the book to test which we can consider casting
		final WeightedChoicesImpl<CombatAISpellChoice> choices = new WeightedChoicesImpl<CombatAISpellChoice> ();
		choices.setRandomUtils (getRandomUtils ());
		
		// Are we completely blocked from casting spells of any particular magic realms?
		final Set<String> blockedMagicRealms = getMemoryMaintainedSpellUtils ().listMagicRealmsBlockedAsCombatSpells
			(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), player.getPlayerDescription ().getPlayerID (), combatDetails.getCombatLocation (), mom.getServerDB ());
		
		// Ignore "recall" spells then the AI would then have to understand its likelehood of losing a combat, and there's nothing like this yet
		for (final Spell spell : mom.getServerDB ().getSpell ())
			if ((spell.getSpellBookSectionID () != null) && (getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.COMBAT)) &&
				(getKindOfSpellUtils ().determineKindOfSpell (spell, null) != KindOfSpell.RECALL) && (!blockedMagicRealms.contains (spell.getSpellRealm ())))
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
					final Integer variableDamage = null;		// Until AI is clever enough to set this properly
					if (combatCastingUnit == null)
					{
						// Wizards get range penalty depending where the combat is in relation to their Fortress; units participating in the combat don't get this
						final ServerGridCellEx gc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
							(combatDetails.getCombatLocation ().getZ ()).getRow ().get (combatDetails.getCombatLocation ().getY ()).getCell ().get (combatDetails.getCombatLocation ().getX ());

						final int reducedCombatCastingCost = getSpellUtils ().getReducedCombatCastingCost
							(spell, variableDamage, wizardDetails.getPick (), priv.getFogOfWarMemory ().getMaintainedSpell (),
								mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ());

						final Integer doubleRangePenalty = getSpellCalculations ().calculateDoubleCombatCastingRangePenalty (wizardDetails, combatDetails.getCombatLocation (),
							getMemoryGridCellUtils ().isTerrainTowerOfWizardry (gc.getTerrainData ()),
							mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (),
							mom.getSessionDescription ().getOverlandMapSize ());
					
						final int multipliedManaCost = (doubleRangePenalty == null) ? Integer.MAX_VALUE :
							(reducedCombatCastingCost * doubleRangePenalty + 1) / 2;
						
						// Wizards have limited casting skill they can use in each combat
						final CombatPlayers combatPlayers = getCombatMapUtils ().determinePlayersInCombatFromLocation
							(combatDetails.getCombatLocation (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getPlayers (), mom.getServerDB ());

						final int ourSkill;
						if (player == combatPlayers.getAttackingPlayer ())
							ourSkill = combatDetails.getAttackerCastingSkillRemaining ();
						else if (player == combatPlayers.getDefendingPlayer ())
							ourSkill = combatDetails.getDefenderCastingSkillRemaining ();
						else
							ourSkill = 0;

						canAffordSpell = (reducedCombatCastingCost <= ourSkill) &&
							(multipliedManaCost <= getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA));
					}
					else
					{
						// Validation for units casting is much simpler, as reductions for number of spell books or certain retorts and the range penalty all don't apply
						final int unmodifiedCombatCastingCost = getSpellUtils ().getUnmodifiedCombatCastingCost (spell, variableDamage, wizardDetails.getPick ());
						canAffordSpell = (unmodifiedCombatCastingCost <= combatCastingUnit.getManaRemaining ());
					}

					if (canAffordSpell)
						getCombatSpellAI ().listChoicesForSpell (player, spell, combatDetails, combatCastingUnit, null, null, mom, choices);
				}
			}
		
		return getCombatSpellAI ().makeCastingChoice (player, combatDetails, choices, combatCastingUnit, mom);
	}
	
	/**
	 * AI player decides whether to use a fixed spell a unit can cast in combat, e.g. Giant Spiders' web
	 * 
	 * @param player AI player who needs to choose what to cast
	 * @param combatCastingUnit Unit who is casting the spell
	 * @param combatDetails Details about the combat taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether a spell was cast or not
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final CombatAIMovementResult decideWhetherToCastFixedSpellInCombat (final PlayerServerDetails player, final ExpandedUnitDetails combatCastingUnit,
		final CombatDetails combatDetails, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final WeightedChoicesImpl<CombatAISpellChoice> choices = new WeightedChoicesImpl<CombatAISpellChoice> ();
		choices.setRandomUtils (getRandomUtils ());
		
		for (int fixedSpellNumber = 0; fixedSpellNumber < combatCastingUnit.getMemoryUnit ().getFixedSpellsRemaining ().size (); fixedSpellNumber++)
		{
			final Integer fixedSpellsRemaining = combatCastingUnit.getMemoryUnit ().getFixedSpellsRemaining ().get (fixedSpellNumber);
			if ((fixedSpellsRemaining != null) && (fixedSpellsRemaining > 0))
			{
				final Spell spell = mom.getServerDB ().findSpell (combatCastingUnit.getUnitDefinition ().getUnitCanCast ().get (fixedSpellNumber).getUnitSpellID (), "decideWhetherToCastFixedSpellInCombat");
				if (!getMemoryMaintainedSpellUtils ().isBlockedCastingCombatSpellsOfRealm (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
					player.getPlayerDescription ().getPlayerID (), combatDetails.getCombatLocation (), spell.getSpellRealm (), mom.getServerDB ()))
					
					getCombatSpellAI ().listChoicesForSpell (player, spell, combatDetails, combatCastingUnit, fixedSpellNumber, null, mom, choices);
			}
		}
		
		return getCombatSpellAI ().makeCastingChoice (player, combatDetails, choices, combatCastingUnit, mom);
	}
	
	/**
	 * AI player decides whether to use the spell imbued in a hero item
	 * 
	 * @param player AI player who needs to choose what to cast
	 * @param combatCastingUnit Unit who is casting the spell
	 * @param combatDetails Details about the combat taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether a spell was cast or not
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final CombatAIMovementResult decideWhetherToCastSpellImbuedInHeroItem (final PlayerServerDetails player, final ExpandedUnitDetails combatCastingUnit,
		final CombatDetails combatDetails, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final WeightedChoicesImpl<CombatAISpellChoice> choices = new WeightedChoicesImpl<CombatAISpellChoice> ();
		choices.setRandomUtils (getRandomUtils ());

		for (int slotNumber = 0; slotNumber < combatCastingUnit.getMemoryUnit ().getHeroItemSpellChargesRemaining ().size (); slotNumber++)
		{
			final Integer charges = combatCastingUnit.getMemoryUnit ().getHeroItemSpellChargesRemaining ().get (slotNumber);
			if ((charges != null) && (charges > 0))
			{
				final Spell spell = mom.getServerDB ().findSpell (combatCastingUnit.getMemoryUnit ().getHeroItemSlot ().get
					(slotNumber).getHeroItem ().getSpellID (), "decideWhetherToCastSpellImbuedInHeroItem");
				
				if (!getMemoryMaintainedSpellUtils ().isBlockedCastingCombatSpellsOfRealm (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
					player.getPlayerDescription ().getPlayerID (), combatDetails.getCombatLocation (), spell.getSpellRealm (), mom.getServerDB ()))
				
					getCombatSpellAI ().listChoicesForSpell (player, spell, combatDetails, combatCastingUnit, null, slotNumber, mom, choices);
			}
		}
		
		return getCombatSpellAI ().makeCastingChoice (player, combatDetails, choices, combatCastingUnit, mom);
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
	 * @return Methods that determine whether something is a valid target for a spell
	 */
	public final SpellTargetingUtils getSpellTargetingUtils ()
	{
		return spellTargetingUtils;
	}

	/**
	 * @param s Methods that determine whether something is a valid target for a spell
	 */
	public final void setSpellTargetingUtils (final SpellTargetingUtils s)
	{
		spellTargetingUtils = s;
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
	 * @return AI city calculations
	 */
	public final AICityCalculations getAiCityCalculations ()
	{
		return aiCityCalculations;
	}

	/**
	 * @param c AI city calculations
	 */
	public final void setAiCityCalculations (final AICityCalculations c)
	{
		aiCityCalculations = c;
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
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
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
	 * @return Methods relating to casting spells in combat
	 */
	public final CombatSpellAI getCombatSpellAI ()
	{
		return combatSpellAI;
	}
	
	/**
	 * @param ai Methods relating to casting spells in combat
	 */
	public final void setCombatSpellAI (final CombatSpellAI ai)
	{
		combatSpellAI = ai;
	}

	/**
	 * @return Kind of spell utils
	 */
	public final KindOfSpellUtils getKindOfSpellUtils ()
	{
		return kindOfSpellUtils;
	}

	/**
	 * @param k Kind of spell utils
	 */
	public final void setKindOfSpellUtils (final KindOfSpellUtils k)
	{
		kindOfSpellUtils = k;
	}

	/**
	 * @return Dispel magic processing
	 */
	public final SpellDispelling getSpellDispelling ()
	{
		return spellDispelling;
	}

	/**
	 * @param p Dispel magic processing
	 */
	public final void setSpellDispelling (final SpellDispelling p)
	{
		spellDispelling = p;
	}

	/**
	 * @return Server-side only spell utils
	 */
	public final SpellServerUtils getSpellServerUtils ()
	{
		return spellServerUtils;
	}

	/**
	 * @param utils Server-side only spell utils
	 */
	public final void setSpellServerUtils (final SpellServerUtils utils)
	{
		spellServerUtils = utils;
	}

	/**
	 * @return Methods for working with wizardIDs
	 */
	public final PlayerKnowledgeUtils getPlayerKnowledgeUtils ()
	{
		return playerKnowledgeUtils;
	}

	/**
	 * @param k Methods for working with wizardIDs
	 */
	public final void setPlayerKnowledgeUtils (final PlayerKnowledgeUtils k)
	{
		playerKnowledgeUtils = k;
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}

	/**
	 * @return Casting for each type of spell
	 */
	public final SpellCasting getSpellCasting ()
	{
		return spellCasting;
	}

	/**
	 * @param c Casting for each type of spell
	 */
	public final void setSpellCasting (final SpellCasting c)
	{
		spellCasting = c;
	}

	/**
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}
}