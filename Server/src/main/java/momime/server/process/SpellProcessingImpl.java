package momime.server.process;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.AttackSpellCombatTargetID;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.HeroItem;
import momime.common.database.PickAndQuantity;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellHasCombatEffect;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.SummonedUnit;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageCreateArtifact;
import momime.common.messages.NewTurnMessageSpell;
import momime.common.messages.NewTurnMessageSummonUnit;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.AddUnassignedHeroItemMessage;
import momime.common.messages.servertoclient.UpdateCombatMapMessage;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitSkillUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.SpellSvr;
import momime.server.database.UnitSvr;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.mapgenerator.CombatMapGenerator;
import momime.server.utils.HeroItemServerUtils;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.utils.UnitAddLocation;
import momime.server.utils.UnitServerUtils;

/**
 * Methods for processing the effects of spells that have completed casting
 */
public final class SpellProcessingImpl implements SpellProcessing
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SpellProcessingImpl.class);

	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit skill utils */
	private UnitSkillUtils unitSkillUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;

	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Combat processing */
	private CombatProcessing combatProcessing;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;

	/** Server-only overland map utils */
	private OverlandMapServerUtils overlandMapServerUtils;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Damage processor */
	private DamageProcessor damageProcessor;
	
	/** Map generator */
	private CombatMapGenerator combatMapGenerator;
	
	/** Methods dealing with hero items */
	private HeroItemServerUtils heroItemServerUtils;

	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/**
	 * Handles casting an overland spell, i.e. when we've finished channeling sufficient mana in to actually complete the casting
	 *
	 * @param gsk Server knowledge structure
	 * @param player Player who is casting the spell
	 * @param spell Which spell is being cast
	 * @param heroItem The item being created; null for spells other than Enchant Item or Create Artifact
	 * @param players List of players in this session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void castOverlandNow (final MomGeneralServerKnowledgeEx gsk, final PlayerServerDetails player, final SpellSvr spell, final HeroItem heroItem,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.trace ("Entering castOverlandNow: Player ID " + player.getPlayerDescription ().getPlayerID () + ", " + spell.getSpellID ());

		// Modifying this by section is really only a safeguard to protect against casting spells which we don't have researched yet
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spell.getSpellID ());
		final SpellBookSectionID sectionID = getSpellUtils ().getModifiedSectionID (spell, researchStatus.getStatus (), true);

		// Overland enchantments
		if (sectionID == SpellBookSectionID.OVERLAND_ENCHANTMENTS)
		{
			// Check if the player already has this overland enchantment cast
			// If they do, they can't have it twice so nothing to do, they just lose the cast
			if (getMemoryMaintainedSpellUtils ().findMaintainedSpell (gsk.getTrueMap ().getMaintainedSpell (), player.getPlayerDescription ().getPlayerID (), spell.getSpellID (), null, null, null, null) == null)
			{
				// Add it on server and anyone who can see it (which, because its an overland enchantment, will be everyone)
				getFogOfWarMidTurnChanges ().addMaintainedSpellOnServerAndClients (gsk, player.getPlayerDescription ().getPlayerID (), spell.getSpellID (),
					null, null, false, null, null, players, db, sd);

				// Does this overland enchantment give a global combat area effect? (Not all do)
				if (spell.getSpellHasCombatEffect ().size () > 0)
				{
					// Pick one at random
					final String combatAreaEffectID = spell.getSpellHasCombatEffect ().get (getRandomUtils ().nextInt (spell.getSpellHasCombatEffect ().size ())).getCombatAreaEffectID ();
					getFogOfWarMidTurnChanges ().addCombatAreaEffectOnServerAndClients (gsk, combatAreaEffectID, spell.getSpellID (),
						player.getPlayerDescription ().getPlayerID (), null, players, db, sd);
				}
			}
		}
		
		// Enchant item / Create artifact
		else if ((sectionID == SpellBookSectionID.SUMMONING) && (heroItem != null))
		{
			// Put new item in players' bank on the server
			final NumberedHeroItem numberedHeroItem = getHeroItemServerUtils ().createNumberedHeroItem (heroItem, gsk);
			priv.getUnassignedHeroItem ().add (numberedHeroItem);

			// Put new item in players' bank on the client
			if (player.getPlayerDescription ().isHuman ())
			{
				final AddUnassignedHeroItemMessage addItemMsg = new AddUnassignedHeroItemMessage ();
				addItemMsg.setHeroItem (numberedHeroItem);
				player.getConnection ().sendMessageToClient (addItemMsg);
			
				// Show on new turn messages for the player who summoned it
				final NewTurnMessageCreateArtifact createArtifactSpell = new NewTurnMessageCreateArtifact ();
				createArtifactSpell.setMsgType (NewTurnMessageTypeID.CREATE_ARTIFACT);
				createArtifactSpell.setSpellID (spell.getSpellID ());
				createArtifactSpell.setHeroItemName (heroItem.getHeroItemName ());

				((MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (createArtifactSpell);
			}
		}

		// Summoning
		else if (sectionID == SpellBookSectionID.SUMMONING)
		{
			// Find the location of the wizards' summoning circle 'building'
			final MemoryBuilding summoningCircleLocation = getMemoryBuildingUtils ().findCityWithBuilding (player.getPlayerDescription ().getPlayerID (),
				CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getBuilding ());

			if (summoningCircleLocation != null)
			{
				// List out all the Unit IDs that this spell can summon
				final List<String> possibleUnitIDs = new ArrayList<String> ();
				for (final SummonedUnit possibleSummonedUnit : spell.getSummonedUnit ())
				{
					// Check whether we can summon this unit If its a hero, this depends on whether we've summoned the hero before, or if he's dead
					final UnitSvr possibleUnit = db.findUnit (possibleSummonedUnit.getSummonedUnitID (), "castOverlandNow");
					boolean addToList;
					if (possibleUnit.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
					{
						final MemoryUnit hero = getUnitServerUtils ().findUnitWithPlayerAndID (gsk.getTrueMap ().getUnit (),
							player.getPlayerDescription ().getPlayerID (), possibleSummonedUnit.getSummonedUnitID ());

						if (hero == null)
							addToList = false;
						else
							addToList = ((hero.getStatus () == UnitStatusID.NOT_GENERATED) || (hero.getStatus () == UnitStatusID.GENERATED));
					}
					else
						addToList = true;
					
					// Check for units that require particular picks to summon
					final Iterator<PickAndQuantity> iter = possibleUnit.getUnitPickPrerequisite ().iterator ();
					while ((addToList) && (iter.hasNext ()))
					{
						final PickAndQuantity prereq = iter.next ();
						if (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), prereq.getPickID ()) < prereq.getQuantity ())
							addToList = false;
					}

					if (addToList)
						possibleUnitIDs.add (possibleSummonedUnit.getSummonedUnitID ());
				}

				// Pick one at random
				if (possibleUnitIDs.size () > 0)
				{
					final String summonedUnitID = possibleUnitIDs.get (getRandomUtils ().nextInt (possibleUnitIDs.size ()));

					log.debug ("Player " + player.getPlayerDescription ().getPlayerName () + " had " + possibleUnitIDs.size () + " possible units to summon from spell " +
						spell.getSpellID () + ", randomly picked unit ID " + summonedUnitID);

					// Check if the city with the summoning circle has space for the unit
					final MapCoordinates3DEx cityLocation = (MapCoordinates3DEx) summoningCircleLocation.getCityLocation ();
					final UnitAddLocation addLocation = getUnitServerUtils ().findNearestLocationWhereUnitCanBeAdded
						(cityLocation, summonedUnitID, player.getPlayerDescription ().getPlayerID (), gsk.getTrueMap (), sd, db);

					final MemoryUnit newUnit;
					if (addLocation.getUnitLocation () == null)
						newUnit = null;
					else
					{
						// Add the unit
						if (db.findUnit (summonedUnitID, "castOverlandNow").getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
						{
							// The unit object already exists for heroes
							newUnit = getUnitServerUtils ().findUnitWithPlayerAndID (gsk.getTrueMap ().getUnit (), player.getPlayerDescription ().getPlayerID (), summonedUnitID);

							if (newUnit.getStatus () == UnitStatusID.NOT_GENERATED)
								getUnitServerUtils ().generateHeroNameAndRandomSkills (newUnit, db);

							getFogOfWarMidTurnChanges ().updateUnitStatusToAliveOnServerAndClients (newUnit, addLocation.getUnitLocation (), player, players, gsk.getTrueMap (), sd, db);
						}
						else
							// For non-heroes, create a new unit
							newUnit = getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (gsk, summonedUnitID, addLocation.getUnitLocation (), null,
								null, player, UnitStatusID.ALIVE, players, sd, db);
						
						// Let it move this turn
						newUnit.setDoubleOverlandMovesLeft (2 * getUnitSkillUtils ().getModifiedSkillValue (newUnit, newUnit.getUnitHasSkill (),
							CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
							null, null, players, gsk.getTrueMap (), db));
					}

					// Show on new turn messages for the player who summoned it
					if (player.getPlayerDescription ().isHuman ())
					{
						final NewTurnMessageSummonUnit summoningSpell = new NewTurnMessageSummonUnit ();
						summoningSpell.setMsgType (NewTurnMessageTypeID.SUMMONED_UNIT);
						summoningSpell.setSpellID (spell.getSpellID ());
						summoningSpell.setUnitID (summonedUnitID);
						summoningSpell.setCityLocation (addLocation.getUnitLocation ());
						summoningSpell.setUnitAddBumpType (addLocation.getBumpType ());

						if (newUnit != null)
							summoningSpell.setUnitURN (newUnit.getUnitURN ());

						((MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (summoningSpell);
					}
				}
			}
		}

		// City or Unit enchantments
		else if ((sectionID == SpellBookSectionID.CITY_ENCHANTMENTS) || (sectionID == SpellBookSectionID.UNIT_ENCHANTMENTS) ||
			(sectionID == SpellBookSectionID.CITY_CURSES) || (sectionID == SpellBookSectionID.UNIT_CURSES))
		{
			// Add it on server - note we add it without a target chosen and without adding it on any
			// clients - clients don't know about spells until the target has been chosen, since they might hit cancel or have no appropriate target.
			getFogOfWarMidTurnChanges ().addMaintainedSpellOnServerAndClients (gsk, player.getPlayerDescription ().getPlayerID (), spell.getSpellID (),
				null, null, false, null, null, null, db, sd);

			// Tell client to pick a target for this spell
			final NewTurnMessageSpell targetSpell = new NewTurnMessageSpell ();
			targetSpell.setMsgType (NewTurnMessageTypeID.TARGET_SPELL);
			targetSpell.setSpellID (spell.getSpellID ());
			((MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (targetSpell);
		}

		else
			throw new MomException ("Completed casting an overland spell with a section ID that there is no code to deal with yet: " + sectionID);

		log.trace ("Exiting castOverlandNow");
	}
	
	/**
	 * Handles casting a spell in combat, after all validation has passed.
	 * If its a spell where we need to choose a target (like Doom Bolt or Phantom Warriors), additional mana (like Counter Magic)
	 * or both (like Firebolt), then the client will already have done all this and supplied us with the chosen values.
	 * 
	 * @param castingPlayer Player who is casting the spell
	 * @param combatCastingUnit Unit who is casting the spell; null means its the wizard casting, rather than a specific unit
	 * @param combatCastingFixedSpellNumber For casting fixed spells the unit knows (e.g. Giant Spiders casting web), indicates the spell number; for other types of casting this is null
	 * @param combatCastingSlotNumber For casting spells imbued into hero items, this is the number of the slot (0, 1 or 2); for other types of casting this is null
	 * @param spell Which spell they want to cast
	 * @param reducedCombatCastingCost Skill cost of the spell, reduced by any book or retort bonuses the player may have
	 * @param multipliedManaCost MP cost of the spell, reduced as above, then multiplied up according to the distance the combat is from the wizard's fortress
	 * @param variableDamage Chosen damage selected for the spell, for spells like fire bolt where a varying amount of mana can be channeled into the spell
	 * @param combatLocation Location of the combat where this spell is being cast; null = being cast overland
	 * @param defendingPlayer Defending player in the combat
	 * @param attackingPlayer Attacking player in the combat
	 * @param targetUnit Unit to target the spell on, if appropriate for spell book section, otherwise null
	 * @param targetLocation Location to target the spell at, if appropriate for spell book section, otherwise null
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void castCombatNow (final PlayerServerDetails castingPlayer, final MemoryUnit combatCastingUnit, final Integer combatCastingFixedSpellNumber,
		final Integer combatCastingSlotNumber, final SpellSvr spell, final int reducedCombatCastingCost, final int multipliedManaCost,
		final Integer variableDamage, final MapCoordinates3DEx combatLocation, final PlayerServerDetails defendingPlayer, final PlayerServerDetails attackingPlayer,
		final MemoryUnit targetUnit, final MapCoordinates2DEx targetLocation, final MomSessionVariables mom)
		throws MomException, JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException
	{
		log.trace ("Entering castCombatNow: Player ID " +
			castingPlayer.getPlayerDescription ().getPlayerID () + ", " + spell.getSpellID () + ", " + spell.getSpellBookSectionID () + ", " + combatLocation);

		// Which side is casting the spell
		final UnitCombatSideID castingSide;
		if (castingPlayer == attackingPlayer)
			castingSide = UnitCombatSideID.ATTACKER;
		else if (castingPlayer == defendingPlayer)
			castingSide = UnitCombatSideID.DEFENDER;
		else
			throw new MomException ("castCombatNow: Casting player is neither the attacker nor defender");
		
		// Combat enchantments
		if (spell.getSpellBookSectionID () == SpellBookSectionID.COMBAT_ENCHANTMENTS)
		{
			// What effects doesn't the combat already have
			final List<String> combatAreaEffectIDs = getMemoryCombatAreaEffectUtils ().listCombatEffectsNotYetCastAtLocation
				(mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (),
				spell, castingPlayer.getPlayerDescription ().getPlayerID (), combatLocation);

			if ((combatAreaEffectIDs == null) || (combatAreaEffectIDs.size () == 0))
				throw new MomException ("castCombatNow was called for casting spell " + spell.getSpellID () + " in combat " + combatLocation +
					" but combatAreaEffectIDs list came back empty");
			
			final String combatAreaEffectID = combatAreaEffectIDs.get (getRandomUtils ().nextInt (combatAreaEffectIDs.size ()));
			log.debug ("castCombatNow chose CAE " + combatAreaEffectID + " as effect for spell " + spell.getSpellID ());
				
			getFogOfWarMidTurnChanges ().addCombatAreaEffectOnServerAndClients (mom.getGeneralServerKnowledge (), combatAreaEffectID,
				spell.getSpellID (), castingPlayer.getPlayerDescription ().getPlayerID (), combatLocation, mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
		}
		
		// Unit enchantments or curses
		else if ((spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) ||
			(spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES))
		{
			// What effects doesn't the unit already have - can cast Warp Creature multiple times
			final List<String> unitSpellEffectIDs = getMemoryMaintainedSpellUtils ().listUnitSpellEffectsNotYetCastOnUnit
				(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
				spell, castingPlayer.getPlayerDescription ().getPlayerID (), targetUnit.getUnitURN ());
			
			if ((unitSpellEffectIDs == null) || (unitSpellEffectIDs.size () == 0))
				throw new MomException ("castCombatNow was called for casting spell " + spell.getSpellID () + " on unit URN " + targetUnit.getUnitURN () +
					" but unitSpellEffectIDs list came back empty");
			
			// Pick an actual effect at random
			final String unitSpellEffectID = unitSpellEffectIDs.get (getRandomUtils ().nextInt (unitSpellEffectIDs.size ()));
			getFogOfWarMidTurnChanges ().addMaintainedSpellOnServerAndClients (mom.getGeneralServerKnowledge (),
				castingPlayer.getPlayerDescription ().getPlayerID (), spell.getSpellID (), targetUnit.getUnitURN (), unitSpellEffectID,
				true, null, null, mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
		}
		
		else if ((spell.getSpellBookSectionID () == SpellBookSectionID.CITY_ENCHANTMENTS) || (spell.getSpellBookSectionID () == SpellBookSectionID.CITY_CURSES))
		{
			// What effects doesn't the city already have
			final List<String> citySpellEffectIDs = getMemoryMaintainedSpellUtils ().listCitySpellEffectsNotYetCastAtLocation
				(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
				spell, castingPlayer.getPlayerDescription ().getPlayerID (), combatLocation);
			
			if ((citySpellEffectIDs == null) || (citySpellEffectIDs.size () == 0))
				throw new MomException ("castCombatNow was called for casting spell " + spell.getSpellID () + " at location " + combatLocation +
					" but citySpellEffectIDs list came back empty");
			
			// Pick an actual effect at random
			final String citySpellEffectID = citySpellEffectIDs.get (getRandomUtils ().nextInt (citySpellEffectIDs.size ()));
			getFogOfWarMidTurnChanges ().addMaintainedSpellOnServerAndClients (mom.getGeneralServerKnowledge (),
				castingPlayer.getPlayerDescription ().getPlayerID (), spell.getSpellID (), null, null,
				true, combatLocation, citySpellEffectID, mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
			
			// The new enchantment presumably requires the combat map to be regenerated so we can see it
			final ServerGridCellEx mc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
			
			getCombatMapGenerator ().regenerateCombatTileBorders (mc.getCombatMap (), mom.getServerDB (), mom.getGeneralServerKnowledge ().getTrueMap (), combatLocation);
			
			// Send the updated map
			final UpdateCombatMapMessage msg = new UpdateCombatMapMessage ();
			msg.setCombatLocation (combatLocation);
			msg.setCombatTerrain (mc.getCombatMap ());
			
			if (attackingPlayer.getPlayerDescription ().isHuman ())
				attackingPlayer.getConnection ().sendMessageToClient (msg);

			if (defendingPlayer.getPlayerDescription ().isHuman ())
				defendingPlayer.getConnection ().sendMessageToClient (msg);
		}
		
		// Raise dead
		else if ((spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING) && (targetUnit != null))
		{
			// Even though we're summoning the unit into a combat, the location of the unit might not be
			// the same location as the combat - if its the attacker summoning a unit, it needs to go in the
			// cell they're attacking from, not the actual defending/combat cell
			final MapCoordinates3DEx summonLocation = getOverlandMapServerUtils ().findMapLocationOfUnitsInCombat
				(combatLocation, castingSide, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
			
			// Heal it
			targetUnit.getUnitDamage ().clear ();
			targetUnit.setOwningPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
			
			if (spell.getResurrectedHealthPercentage () < 100)
			{
				final int totalHP = getUnitCalculations ().calculateHitPointsRemaining (targetUnit, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
				final int hp = (totalHP * spell.getResurrectedHealthPercentage ()) / 100;
				final int dmg = totalHP - hp;
				if (dmg > 0)
				{
					final UnitDamage dmgTaken = new UnitDamage ();
					dmgTaken.setDamageTaken (dmg);
					dmgTaken.setDamageType (StoredDamageTypeID.HEALABLE);
					targetUnit.getUnitDamage ().add (dmgTaken);
				}
			}
			
			// Does it become undead?
			if (spell.getResurrectingAddsSkillID () != null)
			{
				final UnitSkillAndValue undead = new UnitSkillAndValue ();
				undead.setUnitSkillID (spell.getResurrectingAddsSkillID ());
				targetUnit.getUnitHasSkill ().add (undead);
			}
			
			// Set it back to alive; this also sends the updates from above
			getFogOfWarMidTurnChanges ().updateUnitStatusToAliveOnServerAndClients (targetUnit, summonLocation, castingPlayer,
				mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription (), mom.getServerDB ());

			// Show the "summoning" animation for it
			final int combatHeading = (castingPlayer == attackingPlayer) ? 8 : 4;

			getCombatProcessing ().setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer,
				mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), targetUnit,
				combatLocation, combatLocation, targetLocation, combatHeading, castingSide, spell.getSpellID (), mom.getServerDB ());

			// Allow it to be moved this combat turn
			targetUnit.setDoubleCombatMovesLeft (2 * getUnitSkillUtils ().getModifiedSkillValue (targetUnit, targetUnit.getUnitHasSkill (),
				CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
				null, null, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()));
		}
		
		// Combat summons
		else if (spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING)
		{
			// Pick an actual unit at random
			if (spell.getSummonedUnit ().size () > 0)
			{
				final String unitID = spell.getSummonedUnit ().get (getRandomUtils ().nextInt (spell.getSummonedUnit ().size ())).getSummonedUnitID ();
				log.debug ("castCombatNow chose Unit ID " + unitID + " as unit to summon from spell " + spell.getSpellID ());
				
				// Even though we're summoning the unit into a combat, the location of the unit might not be
				// the same location as the combat - if its the attacker summoning a unit, it needs to go in the
				// cell they're attacking from, not the actual defending/combat cell
				final MapCoordinates3DEx summonLocation = getOverlandMapServerUtils ().findMapLocationOfUnitsInCombat
					(combatLocation, castingSide, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
				
				// Now can add it
				final MemoryUnit tu = getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (mom.getGeneralServerKnowledge (),
					unitID, summonLocation, summonLocation, combatLocation, castingPlayer, UnitStatusID.ALIVE, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
				
				// What direction should the unit face?
				final int combatHeading = (castingPlayer == attackingPlayer) ? 8 : 4;
				
				// Set it immediately into combat
				getCombatProcessing ().setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer,
					mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), tu,
					combatLocation, combatLocation, targetLocation, combatHeading, castingSide, spell.getSpellID (), mom.getServerDB ());
				
				// Allow it to be moved this combat turn
				tu.setDoubleCombatMovesLeft (2 * getUnitSkillUtils ().getModifiedSkillValue (tu, tu.getUnitHasSkill (),
					CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
					null, null, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()));
				
				// Make sure we remove it after combat
				tu.setWasSummonedInCombat (true);
			}
		}
		
		// Attack or Healing spells
		else if ((spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS) || (spell.getSpellBookSectionID () == SpellBookSectionID.HEALING_SPELLS))
		{
			// Does the spell attack a specific unit or ALL enemy units? e.g. Flame Strike or Death Spell
			final List<MemoryUnit> targetUnits = new ArrayList<MemoryUnit> ();
			if (spell.getAttackSpellCombatTarget () == AttackSpellCombatTargetID.SINGLE_UNIT)
				targetUnits.add (targetUnit);
			else
				for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if (getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell (spell, combatLocation, castingPlayer.getPlayerDescription ().getPlayerID (),
						variableDamage, thisUnit, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
						
						targetUnits.add (thisUnit);
			
			if (targetUnits.size () > 0)
			{
				if (spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS)
					getDamageProcessor ().resolveAttack (null, targetUnits, attackingPlayer, defendingPlayer,
						null, null, spell, variableDamage, castingPlayer, combatLocation, mom);
				else
				{
					// Healing spells work by sending ApplyDamage - this is basically just updating the client as to the damage taken by a bunch of combat units,
					// and handles showing the animation for us, so its convenient to reuse it for this.  Effectively we're just applying negative damage...
					for (final MemoryUnit tu : targetUnits)
					{
						final int dmg = getUnitUtils ().getHealableDamageTaken (tu.getUnitDamage ());
						final int heal = Math.min (dmg, spell.getCombatBaseDamage ());
						if (heal > 0)
							getUnitServerUtils ().healDamage (tu.getUnitDamage (), heal, false);
					}
					
					getFogOfWarMidTurnChanges ().sendCombatDamageToClients (null, castingPlayer.getPlayerDescription ().getPlayerID (),
						targetUnits, null, spell.getSpellID (), new ArrayList<DamageResolutionTypeID> (), mom.getPlayers (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
				}
			}
		}
		
		else
			throw new MomException ("Cast a combat spell with a section ID that there is no code to deal with yet: " + spell.getSpellBookSectionID ());
		
		// Who is casting the spell?
		if (combatCastingUnit == null)
		{
			// Wizard casting - so charge them the mana cost
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) castingPlayer.getPersistentPlayerPrivateKnowledge ();
			getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -multipliedManaCost);
			
			// Charge skill
			final ServerGridCellEx gc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
			final int sendSkillValue;
			if (castingPlayer == defendingPlayer)
			{
				gc.setCombatDefenderCastingSkillRemaining (gc.getCombatDefenderCastingSkillRemaining () - reducedCombatCastingCost);
				sendSkillValue = gc.getCombatDefenderCastingSkillRemaining ();
			}
			else if (castingPlayer == attackingPlayer)
			{
				gc.setCombatAttackerCastingSkillRemaining (gc.getCombatAttackerCastingSkillRemaining () - reducedCombatCastingCost);
				sendSkillValue = gc.getCombatAttackerCastingSkillRemaining ();
			}
			else
				throw new MomException ("Trying to charge combat casting cost to kill but the caster appears to be neither attacker nor defender");
			
			// Send both values to client
			getServerResourceCalculations ().sendGlobalProductionValues (castingPlayer, sendSkillValue);
			
			// Only allow casting one spell each combat turn
			gc.setSpellCastThisCombatTurn (true);
		}
		else if (combatCastingFixedSpellNumber != null)
		{
			// Casting a fixed spell that's part of the unit definition
			combatCastingUnit.setDoubleCombatMovesLeft (0);

			combatCastingUnit.getFixedSpellsRemaining ().set (combatCastingFixedSpellNumber,
				combatCastingUnit.getFixedSpellsRemaining ().get (combatCastingFixedSpellNumber) - 1);

			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (combatCastingUnit, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
		}
		else if (combatCastingSlotNumber != null)
		{
			// Casting a spell imbued into a hero item
			combatCastingUnit.setDoubleCombatMovesLeft (0);

			combatCastingUnit.getHeroItemSpellChargesRemaining ().set (combatCastingSlotNumber,
				combatCastingUnit.getHeroItemSpellChargesRemaining ().get (combatCastingSlotNumber) - 1);

			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (combatCastingUnit, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
		}
		else
		{
			// Unit or hero casting - so charge them the mana cost and zero their movement
			combatCastingUnit.setManaRemaining (combatCastingUnit.getManaRemaining () - multipliedManaCost);
			combatCastingUnit.setDoubleCombatMovesLeft (0);
			
			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (combatCastingUnit, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
		}

		log.trace ("Exiting castCombatNow");
	}
	
	/**
	 * The method in the FOW class physically removed spells from the server and players' memory; this method
	 * deals with all the knock on effects of spells being switched off, which isn't really much since spells don't grant money or anything when sold
	 * so this is mostly here for consistency with the building and unit methods
	 *
	 * Does not recalc global production (which will now be reduced from not having to pay the maintenance of the cancelled spell),
	 * this has to be done by the calling routine
	 * 
	 * NB. Delphi method was called OkToSwitchOffMaintainedSpell
	 *
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param spellURN Which spell it is
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void switchOffSpell (final FogOfWarMemory trueMap, final int spellURN,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering switchOffSpell: Spell URN " + spellURN);

		// First find the spell
		final MemoryMaintainedSpell trueSpell = getMemoryMaintainedSpellUtils ().findSpellURN (spellURN, trueMap.getMaintainedSpell (), "switchOffSpell");
		
		// Any secondary effects we also need to switch off?
		final PlayerServerDetails player = getMultiplayerSessionServerUtils ().findPlayerWithID (players, trueSpell.getCastingPlayerID (), "switchOffSpell");
		final SpellSvr spell = db.findSpell (trueSpell.getSpellID (), "switchOffSpell");
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), trueSpell.getSpellID ());
		final SpellBookSectionID sectionID = getSpellUtils ().getModifiedSectionID (spell, researchStatus.getStatus (), true);

		// Overland enchantments
		if (sectionID == SpellBookSectionID.OVERLAND_ENCHANTMENTS)
		{
			// Check each combat area effect that this overland enchantment gives to see if we have any of them in effect - if so cancel them
			for (final SpellHasCombatEffect effect : spell.getSpellHasCombatEffect ())
			{
				final MemoryCombatAreaEffect cae = getMemoryCombatAreaEffectUtils ().findCombatAreaEffect
					(trueMap.getCombatAreaEffect (), null, effect.getCombatAreaEffectID (), trueSpell.getCastingPlayerID ());
				
				if (cae != null)
					getFogOfWarMidTurnChanges ().removeCombatAreaEffectFromServerAndClients (trueMap, cae.getCombatAreaEffectURN (), players, db, sd);
			}
		}

		// Remove spell itself
		getFogOfWarMidTurnChanges ().switchOffMaintainedSpellOnServerAndClients (trueMap, trueSpell.getSpellURN (), players, db, sd);

		log.trace ("Exiting switchOffSpell");
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
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param utils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils utils)
	{
		memoryMaintainedSpellUtils = utils;
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
	 * @return Unit skill utils
	 */
	public final UnitSkillUtils getUnitSkillUtils ()
	{
		return unitSkillUtils;
	}

	/**
	 * @param utils Unit skill utils
	 */
	public final void setUnitSkillUtils (final UnitSkillUtils utils)
	{
		unitSkillUtils = utils;
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
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
	}
	
	/**
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}

	/**
	 * @return Combat processing
	 */
	public final CombatProcessing getCombatProcessing ()
	{
		return combatProcessing;
	}

	/**
	 * @param proc Combat processing
	 */
	public final void setCombatProcessing (final CombatProcessing proc)
	{
		combatProcessing = proc;
	}

	/**
	 * @return Resource calculations
	 */
	public final ServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final ServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}

	/**
	 * @return Server-only overland map utils
	 */
	public final OverlandMapServerUtils getOverlandMapServerUtils ()
	{
		return overlandMapServerUtils;
	}
	
	/**
	 * @param utils Server-only overland map utils
	 */
	public final void setOverlandMapServerUtils (final OverlandMapServerUtils utils)
	{
		overlandMapServerUtils = utils;
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
	 * @return Server only helper methods for dealing with players in a session
	 */
	public final MultiplayerSessionServerUtils getMultiplayerSessionServerUtils ()
	{
		return multiplayerSessionServerUtils;
	}

	/**
	 * @param obj Server only helper methods for dealing with players in a session
	 */
	public final void setMultiplayerSessionServerUtils (final MultiplayerSessionServerUtils obj)
	{
		multiplayerSessionServerUtils = obj;
	}

	/**
	 * @return Damage processor
	 */
	public final DamageProcessor getDamageProcessor ()
	{
		return damageProcessor;
	}

	/**
	 * @param proc Damage processor
	 */
	public final void setDamageProcessor (final DamageProcessor proc)
	{
		damageProcessor = proc;
	}

	/**
	 * @return Map generator
	 */
	public final CombatMapGenerator getCombatMapGenerator ()
	{
		return combatMapGenerator;
	}

	/**
	 * @param gen Map generator
	 */
	public final void setCombatMapGenerator (final CombatMapGenerator gen)
	{
		combatMapGenerator = gen;
	}

	/**
	 * @return Methods dealing with hero items
	 */
	public final HeroItemServerUtils getHeroItemServerUtils ()
	{
		return heroItemServerUtils;
	}

	/**
	 * @param util Methods dealing with hero items
	 */
	public final void setHeroItemServerUtils (final HeroItemServerUtils util)
	{
		heroItemServerUtils = util;
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