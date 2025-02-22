package momime.server.fogofwar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerType;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PendingMovement;
import momime.common.messages.PendingMovementStep;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.FogOfWarVisibleAreaChangedMessage;
import momime.common.messages.servertoclient.MoveUnitStackOverlandMessage;
import momime.common.messages.servertoclient.PendingMovementMessage;
import momime.common.messages.servertoclient.PlaneShiftUnitStackMessage;
import momime.common.messages.servertoclient.SelectNextUnitToMoveOverlandMessage;
import momime.common.movement.OverlandMovementCell;
import momime.common.movement.UnitMovement;
import momime.common.movement.UnitStack;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.FogOfWarCalculations;
import momime.server.calculations.ServerCityCalculations;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.process.CombatStartAndEnd;
import momime.server.process.OneCellPendingMovement;
import momime.server.process.PlayerMessageProcessing;
import momime.server.utils.KnownWizardServerUtils;
import momime.server.utils.TreasureUtils;
import momime.server.utils.UnitServerUtils;
import momime.server.utils.UnitSkillDirectAccess;

/**
 * This contains methods for updating multiple mid turn changes at once, e.g. remove all spells in a location.
 * Movement methods are also here, since movement paths are calculated by repeatedly calling the other methods.
 * Separating this from the single changes mean the single changes can be mocked out in the unit tests for the multi change methods.
 */
public final class FogOfWarMidTurnMultiChangesImpl implements FogOfWarMidTurnMultiChanges
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (FogOfWarMidTurnMultiChangesImpl.class);
	
	/** Single cell FOW calculations */
	private FogOfWarCalculations fogOfWarCalculations;
	
	/** Main FOW update routine */
	private FogOfWarProcessing fogOfWarProcessing;
	
	/** FOW single changes */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** City calculations */
	private CityCalculations cityCalculations;

	/** Server-only city calculations */
	private ServerCityCalculations serverCityCalculations;
	
	/** Methods dealing with unit movement */
	private UnitMovement unitMovement;
	
	/** Starting and ending combats */
	private CombatStartAndEnd combatStartAndEnd;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Treasure awarding utils */
	private TreasureUtils treasureUtils;
	
	/** Unit skill values direct access */
	private UnitSkillDirectAccess unitSkillDirectAccess;
	
	/** Methods for dealing with player msgs */
	private PlayerMessageProcessing playerMessageProcessing;
	
	/** Server-only unit calculations */
	private ServerUnitCalculations serverUnitCalculations;

	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Process for making sure one wizard has met another wizard */
	private KnownWizardServerUtils knownWizardServerUtils;
	
	/**
	 * @param combatLocation Location of combat that just ended
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void switchOffSpellsCastInCombatAtLocation (final MapCoordinates3DEx combatLocation, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		for (final MemoryMaintainedSpell trueSpell : mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ())

			// Spells cast at the location
			if ((trueSpell.isCastInCombat ()) && (combatLocation.equals (trueSpell.getCityLocation ())))
				mom.getWorldUpdates ().switchOffSpell (trueSpell.getSpellURN (), false);

			// Spells cast on units
			else if ((trueSpell.isCastInCombat ()) && (trueSpell.getUnitURN () != null))
			{
				final MemoryUnit thisUnit = getUnitUtils ().findUnitURN (trueSpell.getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "switchOffSpellsCastInCombatAtLocation");
				if (combatLocation.equals (thisUnit.getCombatLocation ()))
					mom.getWorldUpdates ().switchOffSpell (trueSpell.getSpellURN (), false);
			}

		mom.getWorldUpdates ().process (mom);
	}
	
	/**
	 * @param unitURN Unit being taken out of combat
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void switchOffSpellsCastInCombatOnUnit (final int unitURN, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		for (final MemoryMaintainedSpell trueSpell : mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ())

			// Spells cast on units
			if ((trueSpell.isCastInCombat ()) && (trueSpell.getUnitURN () != null) && (trueSpell.getUnitURN () == unitURN))
				mom.getWorldUpdates ().switchOffSpell (trueSpell.getSpellURN (), false);
	}
	
	/**
	 * @param cityLocation Location to turn spells off from
	 * @param castingPlayerID Which player's spells to turn off; 0 = everybodys 
	 * @param processChanges Whether to process the generated world updates or leave this up to the caller
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void switchOffSpellsInLocationOnServerAndClients (final MapCoordinates3DEx cityLocation, final int castingPlayerID,
		final boolean processChanges, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		for (final MemoryMaintainedSpell trueSpell : mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ())
			if ((cityLocation.equals (trueSpell.getCityLocation ())) &&
				((castingPlayerID == 0) || (trueSpell.getCastingPlayerID () == castingPlayerID)))
				
				mom.getWorldUpdates ().switchOffSpell (trueSpell.getSpellURN (), false);
		
		if (processChanges)
			mom.getWorldUpdates ().process (mom);
	}
	
	/**
	 * Note this only lists out the world updates, it doesn't call process.
	 * 
	 * @param mapLocation Location the combat is taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void removeCombatAreaEffectsFromLocalisedSpells (final MapCoordinates3DEx mapLocation, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		// Other method finds the relevant CAEs for us, so we only need to remove them
		final List<MemoryCombatAreaEffect> CAEsToRemove = getMemoryCombatAreaEffectUtils ().listCombatAreaEffectsFromLocalisedSpells
			(mom.getGeneralServerKnowledge ().getTrueMap (), mapLocation, mom.getServerDB ());
		
		if (!CAEsToRemove.isEmpty ())
		{
			for (final MemoryCombatAreaEffect trueCAE : CAEsToRemove)
				mom.getWorldUpdates ().removeCombatAreaEffect (trueCAE.getCombatAreaEffectURN ());
			
			mom.getWorldUpdates ().process (mom);
		}
	}

	/**
	 * @param cityLocation Location of the city to remove the building from
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void destroyAllBuildingsInLocationOnServerAndClients (final MapCoordinates3DEx cityLocation, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		final List<Integer> buildingURNs = mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ().stream ().filter
			(b -> cityLocation.equals (b.getCityLocation ())).map (b -> b.getBuildingURN ()).collect (Collectors.toList ());

		if (buildingURNs.size () > 0)
			getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (buildingURNs, false, null, null, null, mom);
	}
	
	/**
	 * @param onlyOnePlayerID If zero, will heal/exp units belonging to all players; if specified will heal/exp only units belonging to the specified player
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void healUnitsAndGainExperience (final int onlyOnePlayerID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		// Find the best Armsmaster at each map cell
		final Map<MapCoordinates3DEx, Integer> armsmasters = new HashMap<MapCoordinates3DEx, Integer> ();
		
		// Find all the healers at each map cell; this is in 1/60ths so
		// Default healing rate 3 = 5%
		// Units with healer skill typically have value 12 = 20% (25% total)
		// Being in a city adds +3 = 5% (10% total)
		// Animsts' Guilds add +4 = 6.66% (16.66% total)
		// Stream of Life adds +60 = 100%
		final Map<MapCoordinates3DEx, Integer> healers = new HashMap<MapCoordinates3DEx, Integer> ();
		
		for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && ((onlyOnePlayerID == 0) || (onlyOnePlayerID == thisUnit.getOwningPlayerID ())))
			{
				final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
				if (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_ARMSMASTER))
				{
					final int expLevel = xu.getModifiedExperienceLevel ().getLevelNumber ();
					final int heroSkillValue = ((xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_ARMSMASTER) + 1) * 2 * (expLevel+1)) / 2;
					
					Integer oldValue = armsmasters.get (xu.getUnitLocation ());
					if ((oldValue == null) || (oldValue < heroSkillValue))
						armsmasters.put (xu.getUnitLocation (), heroSkillValue);
				}
				
				if (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_HEALER))
				{
					final int value = xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_HEALER);
					Integer oldValue = healers.get (xu.getUnitLocation ());
					if ((oldValue == null) || (oldValue < value))
						healers.put (xu.getUnitLocation (), value);
				}
			}
		
		// Add +3 heal at every city
		for (int plane = 0; plane < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); plane++)
			for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
				{
					final MemoryGridCell mc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x);
					final OverlandMapCityData cityData = mc.getCityData ();
					
					if ((cityData != null) && (cityData.getCityPopulation () > 0))
					{
						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, plane);
						final Integer oldValue = healers.get (cityLocation);
						healers.put (cityLocation, ((oldValue == null ? 0 : oldValue) + 3));
					}
				}
		
		// Add +4 heal at every Animists' Guild
		final Map<String, Integer> healingBuildings = mom.getServerDB ().getBuilding ().stream ().filter
			(b -> b.getHealingRateBonus () != null).collect (Collectors.toMap (b -> b.getBuildingID (), b -> b.getHealingRateBonus ()));
		
		mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ().forEach (b ->
		{
			final Integer healingValue = healingBuildings.get (b.getBuildingID ());
			if (healingValue != null)
			{
				final Integer oldValue = healers.get (b.getCityLocation ());
				healers.put ((MapCoordinates3DEx) b.getCityLocation (), ((oldValue == null ? 0 : oldValue) + healingValue));
			}
		});
		
		// Stream of Life fully heals
		final Map<String, Integer> healingCitySpellEffects = mom.getServerDB ().getCitySpellEffect ().stream ().filter
			(e -> e.getCitySpellEffectHealingRateBonus () != null).collect (Collectors.toMap (e -> e.getCitySpellEffectID (), e -> e.getCitySpellEffectHealingRateBonus ()));
		
		mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().forEach (s ->
		{
			if ((s.getCitySpellEffectID () != null) && (s.getCityLocation () != null))
			{
				final Integer healingValue = healingCitySpellEffects.get (s.getCitySpellEffectID ());
				if (healingValue != null)
				{
					final Integer oldValue = healers.get (s.getCityLocation ());
					healers.put ((MapCoordinates3DEx) s.getCityLocation (), ((oldValue == null ? 0 : oldValue) + healingValue));
				}
			}
		});
		
		// Find who has Herb Mastery cast
		final Set<Integer> herbMasteryPlayerIDs = mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().stream ().filter
			(s -> s.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_HERB_MASTERY)).map (s -> s.getCastingPlayerID ()).collect (Collectors.toSet ());
		
		// This can generate a lot of data - a unit update for every single one of our own units plus all units we can see (except summoned ones) - so collate the client messages
		final Map<Integer, FogOfWarVisibleAreaChangedMessage> fowMessages = new HashMap<Integer, FogOfWarVisibleAreaChangedMessage> ();

		// Now process all units
		for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && ((onlyOnePlayerID == 0) || (onlyOnePlayerID == thisUnit.getOwningPlayerID ())))
			{
				final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
				final Pick magicRealm = xu.getModifiedUnitMagicRealmLifeformType ();
				
				boolean sendMsg = false;

				// Units with regeneration can heal even if they wouldn't normally be allowed to (undead trolls)
				boolean regeneration = false;
				for (final String regenerationSkillID : CommonDatabaseConstants.UNIT_SKILL_IDS_REGENERATION)
					if (xu.hasModifiedSkill (regenerationSkillID))
						regeneration = true;
				
				if ((regeneration) && (thisUnit.getUnitDamage ().size () > 0))
				{
					getUnitServerUtils ().healDamage (thisUnit.getUnitDamage (), 1000, false);
					sendMsg = true;
				}
				
				// Heal?
				else if ((magicRealm.isHealEachTurn ()) && (thisUnit.getUnitDamage ().size () > 0))
				{
					// Work out healing rate
					int healingRate = 3;
					
					final Integer healingRateBonus = healers.get (xu.getUnitLocation ());
					if (healingRateBonus != null)
						healingRate = healingRate + healingRateBonus;
					
					// Check Herb Mastery here - Regeneration can heal undead but Herb Mastery can't
					if (herbMasteryPlayerIDs.contains (thisUnit.getOwningPlayerID ()))
						healingRate = healingRate + 60;
					
					// How much is actually healed?
					final int totalHealth = xu.getFullFigureCount () * xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS);
					final int healValue = ((totalHealth * healingRate) + 59) / 60;
					
					getUnitServerUtils ().healDamage (thisUnit.getUnitDamage (), healValue, false);
					sendMsg = true;
				}

				// Experience?
				int exp = getUnitSkillDirectAccess ().getDirectSkillValue (thisUnit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
				if ((magicRealm.isGainExperienceEachTurn ()) && (exp >= 0))
				{
					exp++;
					getUnitSkillDirectAccess ().setDirectSkillValue (thisUnit, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, exp);
					
					// Note we don't do this directly on xu as that will not reflect the updated experience, and don't want to make a whole new ExpandedUnitDetails just to do a simple check
					getUnitServerUtils ().checkIfHeroGainedALevel (xu.getUnitURN (), xu.getUnitType (), (PlayerServerDetails) xu.getOwningPlayer (), exp);
					sendMsg = true;
					
					// Any armsmaster here?  Block heroes from getting this too - can't do this via modified magic realm as heroes can become
					// chaos channeled and so can normal units, but one gains exp from armsmaster and one does not
					if (!xu.getUnitDefinition ().getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
					{
						final Integer heroSkillValue = armsmasters.get (xu.getUnitLocation ());
						if (heroSkillValue != null)
							getUnitSkillDirectAccess ().setDirectSkillValue (thisUnit, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, exp + heroSkillValue);
					}
					
					// Switch off Heroism if the unit now has 120 exp naturally
					getUnitServerUtils ().checkIfNaturallyElite (thisUnit, mom);
				}

				// Inform any clients who know about this unit
				if (sendMsg)
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (thisUnit, mom, fowMessages);
			}
		
		// Send out client updates
		for (final Entry<Integer, FogOfWarVisibleAreaChangedMessage> entry : fowMessages.entrySet ())
		{
			final PlayerServerDetails player = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), entry.getKey (), "healUnitsAndGainExperience");
			player.getConnection ().sendMessageToClient (entry.getValue ());
		}
	}
	
	/**
	 * When a unit dies in combat, all the units on the opposing side gain +2 exp. 
	 * 
	 * @param combatLocation The location where the combat is taking place
	 * @param combatSide Which side is to gain 1 exp
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found, or the player should be able to see the unit but it isn't in their list
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	@Override
	public final void grantExperienceToUnitsInCombat (final MapCoordinates3DEx combatLocation, final UnitCombatSideID combatSide, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// If 9 units gain experience, don't send out 9 separate messages
		final Map<Integer, FogOfWarVisibleAreaChangedMessage> fowMessages = new HashMap<Integer, FogOfWarVisibleAreaChangedMessage> ();
		
		// Find the units who are in combat on the side that earned the kill
		for (final MemoryUnit trueUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
			if ((trueUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (trueUnit.getCombatLocation ())) &&
				(trueUnit.getCombatSide () == combatSide) && (trueUnit.getCombatPosition () != null) && (trueUnit.getCombatHeading () != null))
			{
				final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (trueUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
				final Pick magicRealm = xu.getModifiedUnitMagicRealmLifeformType ();

				int exp = getUnitSkillDirectAccess ().getDirectSkillValue (trueUnit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
				if ((magicRealm.isGainExperienceEachTurn ()) && (exp >= 0))
				{
					for (int gainExp = 0; gainExp < 2; gainExp++)
					{
						exp++;
						getUnitSkillDirectAccess ().setDirectSkillValue (trueUnit, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, exp);
						
						// Note we don't do this directly on xu as that will not reflect the updated experience, and don't want to make a whole new ExpandedUnitDetails just to do a simple check
						getUnitServerUtils ().checkIfHeroGainedALevel (xu.getUnitURN (), xu.getUnitType (), (PlayerServerDetails) xu.getOwningPlayer (), exp);
					}
					
					// This updates both the player memories on the server, and sends messages out to the clients, as needed
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (trueUnit, mom, fowMessages);
				}				
			}
		
		// Send out client updates
		for (final Entry<Integer, FogOfWarVisibleAreaChangedMessage> entry : fowMessages.entrySet ())
		{
			final PlayerServerDetails player = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), entry.getKey (), "grantExperienceToUnitsInCombat");
			player.getConnection ().sendMessageToClient (entry.getValue ());
		}
		
		getPlayerMessageProcessing ().sendNewTurnMessages (null, mom.getPlayers (), null);
	}
	
	/**
	 * Moves a unit stack from one location to another; the two locations are assumed to be adjacent map cells.
	 * It deals with all the resulting knock on effects, namely:
	 * 1) Checking if the units come into view for any players, if so adds the units into the player's memory and sends them to the client
	 * 2) Checking if the units go out of sight for any players, if so removes the units from the player's memory and removes them from the client
	 * 3) Checking what the units can see from their new location
	 * 4) Updating any cities the units are moving out of or into - normal units calm rebels in cities, so by moving the number of rebels may change
	 *
	 * @param unitStack The units we want to move (true unit versions)
	 * @param unitStackOwner The player who owns the units
	 * @param moveFrom Location to move from
	 *
	 * @param moveTo Location to move to
	 * 		moveTo.getPlane () needs some special discussion.  The calling routine must have set moveTo.getPlane () correctly, i.e. so if we're on Myrror
	 *			moving onto a tower, moveTo.getPlane () = 0 - you can't just assume moveTo.getPlane () = moveFrom.getPlane ().
	 *			Also moveTo.getPlane () cannot be calculated simply from checking if the map cell at moveTo is a tower - we might be
	 *			in a tower (on plane 0) moving to a map cell on Myrror - in this case the only way to know the correct value
	 *			of moveTo.getPlane () is by what map cell the player clicked on in the UI.
	 *
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether anything interesting changed as a result of the move (a unit, city, building, terrain, anything new or changed we didn't know about before, besides just the visible area changing)
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final boolean moveUnitStackOneCellOnServerAndClients (final List<MemoryUnit> unitStack, final PlayerServerDetails unitStackOwner,
		final MapCoordinates3DEx moveFrom, final MapCoordinates3DEx moveTo, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException
	{
		// We need a list of the unit URNs
		final List<Integer> unitURNList = new ArrayList<Integer> ();
		for (final MemoryUnit tu : unitStack)
			unitURNList.add (tu.getUnitURN ());
		
		// Check each player in turn
		for (final PlayerServerDetails thisPlayer : mom.getPlayers ())
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final boolean thisPlayerCanSee;
			final boolean canSeeAfterMove;

			// If this is the player who owns the units, then obviously he can see them!
			if (thisPlayer == unitStackOwner)
			{
				thisPlayerCanSee = true;
				canSeeAfterMove = true;
			}
			else
			{
				// It isn't enough to check whether the unit URNs moving are in the player's memory - they may be
				// remembering a location that they previously saw the units at, but can't see where they're moving from/to now
				final boolean couldSeeBeforeMove = getFogOfWarCalculations ().canSeeMidTurnOnAnyPlaneIfTower
					(moveFrom, mom.getSessionDescription ().getFogOfWarSetting ().getUnits (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), priv.getFogOfWar (), mom.getServerDB ());
				canSeeAfterMove = getFogOfWarCalculations ().canSeeMidTurnOnAnyPlaneIfTower
					(moveTo, mom.getSessionDescription ().getFogOfWarSetting ().getUnits (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), priv.getFogOfWar (), mom.getServerDB ());

				// Deal with clients who could not see this unit stack before this move, but now can
				if ((!couldSeeBeforeMove) && (canSeeAfterMove))
				{
					// The unit stack doesn't exist yet in the player's memory or on the client, so before they can move, we have to send all the unit details
					getKnownWizardServerUtils ().meetWizard (unitStackOwner.getPlayerDescription ().getPlayerID (), thisPlayer.getPlayerDescription ().getPlayerID (), true, mom);
					getFogOfWarMidTurnChanges ().addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient
						(unitStack, mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), thisPlayer);
					thisPlayerCanSee = true;
				}

				// Can this player see the units in their current location?
				else if (couldSeeBeforeMove)
				{
					thisPlayerCanSee = true;

					// If we're losing sight of the unit stack, then we need to forget the units and any spells they have cast on them in the player's memory on the server
					// Unlike the add above, we *don't* have to do this on the client, it does it itself via the freeAfterMoving flag after it finishes displaying the animation
					if (!canSeeAfterMove)
						getFogOfWarMidTurnChanges ().freeUnitStackIncludingSpellsFromServerPlayerMemoryOnly (unitURNList, thisPlayer);
				}

				// This player can't see the units before, during or after their move
				else
					thisPlayerCanSee = false;
			}

			// Any updates to make for this player?
			if (thisPlayerCanSee)
			{
				// Move units in player's memory on server; N/A if we can't see them after the move - they'd have been freed above already
				if (canSeeAfterMove)
					for (final MemoryUnit thisUnit : unitStack)
					{
						final MemoryUnit fowUnit = getUnitUtils ().findUnitURN (thisUnit.getUnitURN (), priv.getFogOfWarMemory ().getUnit ());
						if (fowUnit != null)
							fowUnit.setUnitLocation (new MapCoordinates3DEx (moveTo));
					}
				
				// Move units on client
				if (thisPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
				{
					// Create a new message each time; reusing the same message messes up unit tests because the FreeAfterMoving flag changes each time 
					final MoveUnitStackOverlandMessage movementUnitMessage = new MoveUnitStackOverlandMessage ();
					movementUnitMessage.setMoveFrom (moveFrom);
					movementUnitMessage.setMoveTo (moveTo);
					movementUnitMessage.setFreeAfterMoving (!canSeeAfterMove);

					for (final MemoryUnit tu : unitStack)
						movementUnitMessage.getUnitURN ().add (tu.getUnitURN ());
					
					thisPlayer.getConnection ().sendMessageToClient (movementUnitMessage);
				}
			}
		}

		// Move units on true map - this has to be done after updating the players' memories above so that any calls to
		// addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient that might take place add the units at their old location, THEN show them moving to the new location
		for (final MemoryUnit thisUnit : unitStack)
			thisUnit.setUnitLocation (new MapCoordinates3DEx (moveTo));

		// See what the units can see from their new location
		boolean somethingInterestingChanged = getFogOfWarProcessing ().updateAndSendFogOfWar (unitStackOwner, "moveUnitStackOneCellOnServerAndClients", mom);

		// If we moved out of or into a city, then need to recalc rebels, production, because the units may now be (or may now no longer be) helping ease unrest.
		// Note this doesn't deal with capturing cities - attacking even an empty city is treated as a combat, so we can pick Capture/Raze.
		final MapCoordinates3DEx [] cityLocations = new MapCoordinates3DEx [] {moveFrom, moveTo};
		for (final MapCoordinates3DEx cityLocation : cityLocations)
		{
			final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
			if (cityData != null)
			{
				final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), cityData.getCityOwnerID (), "moveUnitStackOneCellOnServerAndClients");
				final MomPersistentPlayerPrivateKnowledge cityOwnerPriv = (MomPersistentPlayerPrivateKnowledge) cityOwner.getPersistentPlayerPrivateKnowledge ();

				cityData.setNumberOfRebels (getCityCalculations ().calculateCityRebels (mom.getGeneralServerKnowledge ().getTrueMap (),
					cityLocation, cityOwnerPriv.getTaxRateID (), mom.getServerDB ()).getFinalTotal ());

				getServerCityCalculations ().ensureNotTooManyOptionalFarmers (cityData);

				getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (),
					cityLocation, mom.getSessionDescription ().getFogOfWarSetting ());
				
				somethingInterestingChanged = true;
			}
		}
		
		// Record the current tile type and map feature, in case we captured a node/lair/tower and have treasure to award.
		// In case there's a prisoner, we need lairs to have been removed so that the prisoner doesn't get bumped to an adjacent cell unnecessarily,
		// but the rollTreasureReward routine still needs to know what the type of lair that was removed was, since it affects the type of spell books that can be obtained.
		final ServerGridCellEx tc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(moveTo.getZ ()).getRow ().get (moveTo.getY ()).getCell ().get (moveTo.getX ());
		final String tileTypeID = tc.getTerrainData ().getTileTypeID ();
		final String mapFeatureID = tc.getTerrainData ().getMapFeatureID ();
		
		// If we captured a monster lair, temple, etc. then remove it from the map (don't remove nodes or towers of course).
		// This is now part of movement, rather than part of cleaning up after a combat, because capturing empty lairs no longer even initiates a combat.
		// If the monsters in a lair are killed in a combat, then the attackers advance after the combat using this same routine, so that also works.
		if ((tc.getTerrainData ().getMapFeatureID () != null) &&
			(mom.getServerDB ().findMapFeature (tc.getTerrainData ().getMapFeatureID (), "moveUnitStackOneCellOnServerAndClients").getMapFeatureMagicRealm ().size () > 0) &&
			(!getMemoryGridCellUtils ().isTerrainTowerOfWizardry (tc.getTerrainData ())))
		{
			log.debug ("Removing lair at " + moveTo);
			tc.getTerrainData ().setMapFeatureID (null);
			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (),
				moveTo, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
			
			somethingInterestingChanged = true;
		}					
		
		// If we captured a tower of wizardry, then turn the light on
		else if (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY.equals (tc.getTerrainData ().getMapFeatureID ()))
		{
			for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
			{
				final MapCoordinates3DEx towerCoords = new MapCoordinates3DEx (moveTo.getX (), moveTo.getY (), z);
				log.debug ("Turning light on in tower at " + towerCoords);
				
				mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (towerCoords.getZ ()).getRow ().get (towerCoords.getY ()).getCell ().get
					(towerCoords.getX ()).getTerrainData ().setMapFeatureID (CommonDatabaseConstants.FEATURE_CLEARED_TOWER_OF_WIZARDRY);
				getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (),
					towerCoords, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
			}
			
			somethingInterestingChanged = true;
		}

		// If we captured a node/lair/tower then award the treasure.
		if (tc.getTreasureValue () != null)
		{
			getTreasureUtils ().sendTreasureReward
				(getTreasureUtils ().rollTreasureReward (tc.getTreasureValue (), unitStackOwner, moveTo, tileTypeID, mapFeatureID, mom),
					unitStackOwner, mom);
			tc.setTreasureValue (null);
			
			somethingInterestingChanged = true;
		}
		
		else if (tc.getGoldInRuin () != null)
		{
			getTreasureUtils ().sendTreasureReward (getTreasureUtils ().giveGoldInRuin (tc.getGoldInRuin (), unitStackOwner, moveTo, tileTypeID, mapFeatureID),
				unitStackOwner, mom);
			tc.setGoldInRuin (null);
			
			somethingInterestingChanged = true;
		}
		
		return somethingInterestingChanged;
	}

	/**
	 * Client has requested that we try move a stack of their units to a certain location - that location may be on the other
	 * end of the map, and we may not have seen it or the intervening terrain yet, so we basically move one tile at a time
	 * and re-evaluate *everthing* based on the knowledge we learn of the terrain from our new location before we make the next move
	 *
	 * @param selectedUnits The units we want to move (true unit versions)
	 * @param unitStackOwner The player who owns the units
	 * @param processCombats If true will allow combats to be started; if false any move that would initiate a combat will be cancelled and ignored.
	 *		This is used when executing pending movements at the start of one-player-at-a-time game turns.
	 * @param originalMoveFrom Location to move from
	 *
	 * @param originalMoveTo Location to move to
	 * 		Note about moveTo.getPlane () - the same comment as moveUnitStackOneCellOnServerAndClients *doesn't apply*, moveTo.getPlane ()
	 *			will be whatever the player clicked on - if they click on a tower on Myrror, moveTo.getPlane () will be set to 1; the routine
	 *			sorts the correct destination plane out for each cell that the unit stack moves
	 *
	 * @param forceAsPendingMovement If true, forces all generated moves to be added as pending movements rather than occurring immediately (used for simultaneous turns games)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the move resulted in a combat being started in a one-player-at-a-time game (and thus the player's turn should halt while the combat is played out)
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final boolean moveUnitStack (final List<ExpandedUnitDetails> selectedUnits, final PlayerServerDetails unitStackOwner, final boolean processCombats,
		final MapCoordinates3DEx originalMoveFrom, final MapCoordinates3DEx originalMoveTo,
		final boolean forceAsPendingMovement, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) unitStackOwner.getPersistentPlayerPrivateKnowledge ();
		final Set<String> unitStackSkills = getUnitCalculations ().listAllSkillsInUnitStack (selectedUnits);

		final UnitStack unitStack = getUnitCalculations ().createUnitStack (selectedUnits, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
		
		// Get the list of units who are actually moving
		final List<ExpandedUnitDetails> movingUnits = (unitStack.getTransports ().size () > 0) ? unitStack.getTransports () : unitStack.getUnits ();
		
		final List<ExpandedUnitDetails> allUnits = new ArrayList<ExpandedUnitDetails> ();
		allUnits.addAll (unitStack.getTransports ());
		allUnits.addAll (unitStack.getUnits ());
		
		final List<MemoryUnit> allUnitsMem = new ArrayList<MemoryUnit> ();
		for (final ExpandedUnitDetails xu : allUnits)
			allUnitsMem.add (xu.getMemoryUnit ());
		
		// Have to define a lot of these out here so they can be used after the loop
		boolean keepGoing = true;
		boolean validMoveFound = false;
		boolean somethingInterestingChanged = true;
		int doubleMovementRemaining = 0;
		OverlandMovementCell [] [] [] moves = null;

		MapCoordinates3DEx moveFrom = originalMoveFrom;
		final MapCoordinates3DEx moveTo = new MapCoordinates3DEx (originalMoveTo);
		boolean combatInitiated = false;

		while (keepGoing)
		{
			// What's the lowest movement remaining of any unit in the stack
			doubleMovementRemaining = Integer.MAX_VALUE;
			for (final ExpandedUnitDetails thisUnit : movingUnits)
				if (thisUnit.getDoubleOverlandMovesLeft () < doubleMovementRemaining)
					doubleMovementRemaining = thisUnit.getDoubleOverlandMovesLeft ();

			// Find distances and route from our start point to every location on the map
			if (somethingInterestingChanged)
			{
				moves = getUnitMovement ().calculateOverlandMovementDistances (moveFrom, unitStackOwner.getPlayerDescription ().getPlayerID (),
					unitStack, doubleMovementRemaining, mom.getPlayers (), mom.getSessionDescription ().getOverlandMapSize (), priv.getFogOfWarMemory (), mom.getServerDB ());
				
				somethingInterestingChanged = false;
			}

			// Is there a route to where we want to go?
			validMoveFound = (moves [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()] != null);

			// Make 1 move as long as there is a valid move, and we're not allocating movement in a simultaneous turns game
			if ((validMoveFound) && (!forceAsPendingMovement))
			{
				// Get where our 1 move will take us to
				final MapCoordinates3DEx oneStep = getFogOfWarMidTurnChanges ().determineMovementDirection (moveFrom, moveTo, moves);

				MemoryGridCell oneStepTrueTile = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(oneStep.getZ ()).getRow ().get (oneStep.getY ()).getCell ().get (oneStep.getX ());

				combatInitiated = getUnitCalculations ().willMovingHereResultInAnAttack (oneStep.getX (), oneStep.getY (), oneStep.getZ (), unitStackOwner.getPlayerDescription ().getPlayerID (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
				
				// If we ran into some invisible units prior to our destination, then adjust destination
				if ((combatInitiated) && (!oneStep.equals (moveTo)))
				{
					moveTo.setX (oneStep.getX ());
					moveTo.setY (oneStep.getY ());
					moveTo.setZ (oneStep.getZ ());
					
					somethingInterestingChanged = true;
				}
				
				oneStepTrueTile = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(oneStep.getZ ()).getRow ().get (oneStep.getY ()).getCell ().get (oneStep.getX ());

				// Update the movement remaining for each unit
				if (!combatInitiated)
					getFogOfWarMidTurnChanges ().reduceMovementRemaining (movingUnits, unitStackSkills,
						(oneStepTrueTile.getTerrainData ().getRoadTileTypeID () != null) ? oneStepTrueTile.getTerrainData ().getRoadTileTypeID () : oneStepTrueTile.getTerrainData ().getTileTypeID (),
						 mom.getServerDB ());
				else if (processCombats)
				{
					// Attacking uses up all movement
					for (final ExpandedUnitDetails thisUnit : allUnits)
						thisUnit.setDoubleOverlandMovesLeft (0);
				}
				
				// Tell the client how much movement each unit has left, while we're at it recheck the lowest movement remaining of anyone in the stack
				if ((!combatInitiated) || (processCombats))
				{
					doubleMovementRemaining = Integer.MAX_VALUE;
	
					// If entering a combat, ALL units have their movement zeroed, even ones sitting in transports; for regular movement only the transports' movementRemaining is updated
					for (final ExpandedUnitDetails thisUnit : (combatInitiated ? allUnits : movingUnits))
					{
						getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (thisUnit.getMemoryUnit (), mom, null);
	
						if (thisUnit.getDoubleOverlandMovesLeft () < doubleMovementRemaining)
							doubleMovementRemaining = thisUnit.getDoubleOverlandMovesLeft ();
					}
				}

				// Make our 1 movement?
				if (!combatInitiated)
				{
					// Actually move the units
					if (moveUnitStackOneCellOnServerAndClients (allUnitsMem, unitStackOwner, moveFrom, oneStep, mom))
						somethingInterestingChanged = true;

					// Prepare for next loop
					moveFrom = oneStep;
				}
			}

			// Check whether to loop again
			keepGoing = (!forceAsPendingMovement) && (validMoveFound) && (!combatInitiated) && (doubleMovementRemaining > 0) && (!moveFrom.equals (moveTo));
		}

		// If the unit stack failed to reach its destination this turn, create a pending movement object so they'll continue their movement next turn
		if ((!combatInitiated) && (!moveFrom.equals (moveTo)))
		{
			// Unless ForceAsPendingMovement is on, we'll have made at least one move so should recalc the
			// best path again based on what else we learned about the terrain in our last move
			if (!forceAsPendingMovement)
			{
				moves = getUnitMovement ().calculateOverlandMovementDistances (moveFrom, unitStackOwner.getPlayerDescription ().getPlayerID (),
					unitStack, doubleMovementRemaining, mom.getPlayers (), mom.getSessionDescription ().getOverlandMapSize (), priv.getFogOfWarMemory (), mom.getServerDB ());

				validMoveFound = (moves [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()] != null);
			}

			if (validMoveFound)
			{
				final PendingMovement pending = new PendingMovement ();
				pending.setMoveFrom (moveFrom);
				pending.setMoveTo (moveTo);

				for (final ExpandedUnitDetails thisUnit : selectedUnits)
					pending.getUnitURN ().add (thisUnit.getUnitURN ());

				// Record the movement path
				MapCoordinates3DEx coords = moveTo;
				while (!coords.equals (moveFrom))
				{
					final OverlandMovementCell cell = moves [coords.getZ ()] [coords.getY ()] [coords.getX ()];
					
					final PendingMovementStep step = new PendingMovementStep ();
					step.setMoveTo (new MapCoordinates3DEx (coords));
					if (cell.getDirection () > 0)
						step.setDirection (cell.getDirection ());

					step.setMoveFrom (cell.getMovedFrom ());
					pending.getPath ().add (0, step);
					
					coords = cell.getMovedFrom ();
				}

				priv.getPendingMovement ().add (pending);

				// Send the pending movement to the client
				if (unitStackOwner.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
				{
					final PendingMovementMessage pendingMsg = new PendingMovementMessage ();
					pendingMsg.setPendingMovement (pending);
					unitStackOwner.getConnection ().sendMessageToClient (pendingMsg);
				}
			}
		}

		// Deal with any combat initiated
		boolean combatStarted = false;
		if (!combatInitiated)
		{
			// No combat, so tell the client to ask for the next unit to move
			if (unitStackOwner.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
				unitStackOwner.getConnection ().sendMessageToClient (new SelectNextUnitToMoveOverlandMessage ());
		}
		else if (!processCombats)
			log.debug ("Would have started combat, but processCombats is false");
		else
		{
			// Scheduled the combat or start it immediately
			final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (moveTo.getX (), moveTo.getY (), moveTo.getZ ());

			final List<Integer> attackingUnitURNs = new ArrayList<Integer> ();
			for (final ExpandedUnitDetails tu : allUnits)
				attackingUnitURNs.add (tu.getUnitURN ());
			
			if (mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS)
				throw new MomException ("moveUnitStack found a combat in a simultaneous turns game, which should be handled outside of here");
			
			// Start a one-player-at-a-time combat
			combatStarted = true;
			getCombatStartAndEnd ().startCombat (defendingLocation, moveFrom, attackingUnitURNs, null, null, null, mom);
		}

		return combatStarted;
	}
	
	/**
	 * This follows the same logic as moveUnitStack, except that it only works out what the first cell of movement will be,
	 * and doesn't actually perform the movement.
	 * 
	 * @param selectedUnits The units we want to move (true unit versions)
	 * @param unitStackOwner The player who owns the units
	 * @param pendingMovement The pending move we're determining one step of
	 * @param doubleMovementRemaining The lowest movement remaining of any unit in the stack
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws PlayerNotFoundException If we cannot find the player who a unit in the same location as our transports
	 * @throws MomException If there is a problem with any of the calculations
	 * @return null if the location is unreachable; otherwise object holding the details of the move, the one step we'll take first, and whether it initiates a combat
	 */
	@Override
	public final OneCellPendingMovement determineOneCellPendingMovement (final List<ExpandedUnitDetails> selectedUnits, final PlayerServerDetails unitStackOwner,
		final PendingMovement pendingMovement,  final int doubleMovementRemaining, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, PlayerNotFoundException
	{
		final MapCoordinates3DEx moveFrom = (MapCoordinates3DEx) pendingMovement.getMoveFrom ();
		final MapCoordinates3DEx moveTo = (MapCoordinates3DEx) pendingMovement.getMoveTo ();
		
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) unitStackOwner.getPersistentPlayerPrivateKnowledge ();

		final UnitStack unitStack = getUnitCalculations ().createUnitStack (selectedUnits, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
		
		// Find distances and route from our start point to every location on the map
		final OverlandMovementCell [] [] [] moves = getUnitMovement ().calculateOverlandMovementDistances (moveFrom, unitStackOwner.getPlayerDescription ().getPlayerID (),
			unitStack, doubleMovementRemaining, mom.getPlayers (), mom.getSessionDescription ().getOverlandMapSize (), priv.getFogOfWarMemory (), mom.getServerDB ());

		// Is there a route to where we want to go?
		final OneCellPendingMovement result;
		if (moves [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()] == null)
			result = null;
		else
		{
			final MapCoordinates3DEx oneStep = getFogOfWarMidTurnChanges ().determineMovementDirection (moveFrom, moveTo, moves);

			// Does this initiate a combat?
			final boolean combatInitiated = getUnitCalculations ().willMovingHereResultInAnAttack (oneStep.getX (), oneStep.getY (), oneStep.getZ (),
				unitStackOwner.getPlayerDescription ().getPlayerID (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
			
			// Set up result
			result = new OneCellPendingMovement (unitStackOwner, pendingMovement, oneStep, combatInitiated);
		}
		
		return result;
	}
	
	/**
	 * This follows the same logic as moveUnitStack, except that it only works out what the movement path will be,
	 * and doesn't actually perform the movement.
	 * 
	 * @param selectedUnits The units we want to move (true unit versions)
	 * @param unitStackOwner The player who owns the units
	 * @param moveFrom Location to move from
	 * @param moveTo Location to move to
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If there is a problem with any of the calculations
	 * @return null if the location is unreachable; otherwise object holding the details of the move, the one step we'll take first, and whether it initiates a combat
	 */
	@Override
	public final List<PendingMovementStep> determineMovementPath (final List<ExpandedUnitDetails> selectedUnits, final PlayerServerDetails unitStackOwner,
		final MapCoordinates3DEx moveFrom, final MapCoordinates3DEx moveTo, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, PlayerNotFoundException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) unitStackOwner.getPersistentPlayerPrivateKnowledge ();
		
		final UnitStack unitStack = getUnitCalculations ().createUnitStack (selectedUnits, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
		
		// We do this at the end of simultaneous turns movement, when all units stacks have used up all movement, so we know this
		final int doubleMovementRemaining = 0;
		
		// Find distances and route from our start point to every location on the map
		final OverlandMovementCell [] [] [] moves = getUnitMovement ().calculateOverlandMovementDistances (moveFrom, unitStackOwner.getPlayerDescription ().getPlayerID (),
			unitStack, doubleMovementRemaining, mom.getPlayers (), mom.getSessionDescription ().getOverlandMapSize (), priv.getFogOfWarMemory (), mom.getServerDB ());

		// Is there a route to where we want to go?
		final List<PendingMovementStep> result;
		if (moves [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()] == null)
			result = null;
		else
		{
			// Record the movement path
			result = new ArrayList<PendingMovementStep> ();

			MapCoordinates3DEx coords = moveTo;
			while (!coords.equals (moveFrom))
			{
				final OverlandMovementCell cell = moves [coords.getZ ()] [coords.getY ()] [coords.getX ()];
				
				final PendingMovementStep step = new PendingMovementStep ();
				step.setMoveTo (new MapCoordinates3DEx (coords));
				if (cell.getDirection () > 0)
					step.setDirection (cell.getDirection ());

				step.setMoveFrom (cell.getMovedFrom ());
				result.add (0, step);
				
				coords = cell.getMovedFrom ();
			}
		}
		
		return result;
	}

	/**
	 * Gives all units full movement back again overland
	 *
	 * @param onlyOnePlayerID If zero, will reset movmenet for units belonging to all players; if specified will reset movement only for units belonging to the specified player
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	@Override
	public final void resetUnitOverlandMovement (final int onlyOnePlayerID, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		// This can generate a lot of data - a unit update for every single one of our own units plus all units we can see - so collate the client messages
		final Map<Integer, FogOfWarVisibleAreaChangedMessage> fowMessages = new HashMap<Integer, FogOfWarVisibleAreaChangedMessage> ();

		// Check every unit
		for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
			if ((onlyOnePlayerID == 0) || (onlyOnePlayerID == thisUnit.getOwningPlayerID ()))
			{
				final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
				
				boolean stasis = false;
				for (final String stasisSkillID : CommonDatabaseConstants.UNIT_SKILL_IDS_STASIS)
					if (xu.hasModifiedSkill (stasisSkillID))
						stasis = true;
				
				thisUnit.setDoubleOverlandMovesLeft (stasis ? 0 : (2 * xu.getMovementSpeed ()));
				
				getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (thisUnit, mom, fowMessages);
			}
		
		// Send out client updates
		for (final Entry<Integer, FogOfWarVisibleAreaChangedMessage> entry : fowMessages.entrySet ())
		{
			final PlayerServerDetails player = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), entry.getKey (), "resetUnitOverlandMovement");
			player.getConnection ().sendMessageToClient (entry.getValue ());
		}
	}
	
	/**
	 * @param selectedUnits List of units who want to jump to the other plane
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void planeShiftUnitStack (final List<ExpandedUnitDetails> selectedUnits, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final int playerID = selectedUnits.get (0).getOwningPlayerID ();
		final MapCoordinates3DEx moveFrom = selectedUnits.get (0).getUnitLocation ();
		
		final MapCoordinates3DEx moveTo = new MapCoordinates3DEx (moveFrom);
		moveTo.setZ (1 - moveTo.getZ ());
		
		final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(moveTo.getZ ()).getRow ().get (moveTo.getY ()).getCell ().get (moveTo.getX ());
		final OverlandMapTerrainData terrainData = tc.getTerrainData ();
		
		// Check for units already in the target cell (we need a list of them below anyway which is why am not using the methods like findFirstAliveEnemyAtLocation)
		final List<MemoryUnit> unitsInTargetCell = new ArrayList<MemoryUnit> ();
		boolean anyEnemyHere = false;
		
		for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getUnitLocation () != null) && (thisUnit.getUnitLocation ().equals (moveTo)))
			{
				if (thisUnit.getOwningPlayerID () != playerID)
					anyEnemyHere = true;
				else
					unitsInTargetCell.add (thisUnit);
			}		
		
		// Any number of reasons it may fail
		boolean success = true;
		if (anyEnemyHere)
			success = false;
		
		else if (unitsInTargetCell.size () > CommonDatabaseConstants.MAX_UNITS_PER_MAP_CELL)
			success = false;

		else if (getMemoryGridCellUtils ().isNodeLairTower (terrainData, mom.getServerDB ()))
			success = false;

		// We can plane shift into our own cities, but not someone else's, even if its empty
		else if ((tc.getCityData () != null) && (tc.getCityData ().getCityPopulation () > 0) && (tc.getCityData ().getCityOwnerID () != playerID))
			success = false;
		
		else
		{
			// Now need to start checking each individual unit.  Need to combine the units who are plane shifting together with any of our own
			// units in the destination map cell and make sure nobody ends up on impassable terrain.  This is so you could for example have
			// some spearmen in a boat on one plane, and if there's a boat waiting on the other plane to hold them, you can plane shift them and its fine
			// even though the ocean tile is impassable to them.  This is all based on how recheckTransportCapacity works.
			final List<ExpandedUnitDetails> unitStack = new ArrayList<ExpandedUnitDetails> ();
			unitStack.addAll (selectedUnits);
			
			for (final MemoryUnit tu : unitsInTargetCell)
				unitStack.add (getExpandUnitDetails ().expandUnitDetails (tu, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()));
			
			// Get a list of the unit stack skills
			final Set<String> unitStackSkills = getUnitCalculations ().listAllSkillsInUnitStack (unitStack);
			
			// Now check each unit in the stack
			int spaceRequired = 0;
			
			final Iterator<ExpandedUnitDetails> iter = unitStack.iterator ();
			while ((success) && (iter.hasNext ()))
			{
				final ExpandedUnitDetails tu = iter.next ();
				
				final boolean impassable = getUnitCalculations ().isTileTypeImpassable (tu, unitStackSkills,
					getMemoryGridCellUtils ().convertNullTileTypeToFOW (terrainData, false), mom.getServerDB ());
					
				// Count space granted by transports
				final Integer unitTransportCapacity = tu.getUnitDefinition ().getTransportCapacity ();
				if ((unitTransportCapacity != null) && (unitTransportCapacity > 0))
				{
					// Transports on impassable terrain are just invalid, since they can't get inside any other unit to transport them
					if (impassable)
						success = false;
					else
						spaceRequired = spaceRequired - unitTransportCapacity;
				}
				else if (impassable)
					spaceRequired++;
			}
			
			// If there's enough space to transport any units for who the terrain is impassable, then we're fine
			if (spaceRequired > 0)
				success = false;
		}

		// Did it work?
		final PlayerServerDetails player = (PlayerServerDetails) selectedUnits.get (0).getOwningPlayer ();
		if (success)
		{
			// Move units
			final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
			for (final ExpandedUnitDetails tu : selectedUnits)
				unitStack.add (tu.getMemoryUnit ());

			// This method does all the proper handling for units moving from one cell to another - it doesn't really
			// matter whether that is a conventional move or the cells are on different planes
			moveUnitStackOneCellOnServerAndClients (unitStack, player, moveFrom, moveTo, mom);
			
			// Recheck the units left behind - maybe a ship plane shifted and all the units who used to be inside it are now swimming
			mom.getWorldUpdates ().recheckTransportCapacity (moveFrom);
			mom.getWorldUpdates ().process (mom);
		}
		else
		{
			// Stay where we are
			moveTo.setZ (1 - moveTo.getZ ());
		}
		
		// Inform client
		// For now, keeping this as informing the owner of the units, and not displaying an animation for it.  The client only does something with
		// this message if the plane shift failed.  If we try to display an animation for anyone other than the unit owner then it gets messy, as they
		// may be able to see the start/end point but not the other, or so on.
		if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final PlaneShiftUnitStackMessage msg = new PlaneShiftUnitStackMessage ();
			msg.setMoveFrom (moveFrom);
			msg.setMoveTo (moveTo);
			
			for (final ExpandedUnitDetails tu : selectedUnits)
				msg.getUnitURN ().add (tu.getUnitURN ());
			
			player.getConnection ().sendMessageToClient (msg);
		}
	}
	
	/**
	 * Units stuck in webs in combat hack/burn some of the HP off the web trying to free themselves.
	 * 
	 * @param webbedUnits List of units stuck in web
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 */
	@Override
	public final void processWebbedUnits (final List<ExpandedUnitDetails> webbedUnits, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException
	{
		for (final ExpandedUnitDetails xu : webbedUnits)
		{
			final int meleeStrength = xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK) ?
				xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK) : 0;
			
			final int rangedStrength = ((xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)) &&
				(xu.getRangedAttackType ().getMagicRealmID () != null)) ? 
					xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK) : 0;
			
			final int attackStrength = Math.max (meleeStrength, rangedStrength);
			if (attackStrength > 0)
				
				// Process ALL web spells, there could me multiple so we can't just use findMaintainedSpell to find the first one for us
				for (final MemoryMaintainedSpell webSpell : mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ())
					if ((webSpell.getUnitSkillID () != null) && (webSpell.getUnitURN () != null) &&
						(webSpell.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_WEB)) && (webSpell.getUnitURN () == xu.getUnitURN ()))
					{
						final Integer webHP = webSpell.getVariableDamage ();
						if ((webHP != null) && (webHP > 0))
						{
							Integer newHP = webHP - attackStrength;
							if (newHP <= 0)
								newHP = null;
							
							// Update its remaining HP
							webSpell.setVariableDamage (newHP);
							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfSpell (webSpell, mom);
						}
					}
		}
	}
	
	/**
	 * @return Single cell FOW calculations
	 */
	public final FogOfWarCalculations getFogOfWarCalculations ()
	{
		return fogOfWarCalculations;
	}

	/**
	 * @param calc Single cell FOW calculations
	 */
	public final void setFogOfWarCalculations (final FogOfWarCalculations calc)
	{
		fogOfWarCalculations = calc;
	}

	/**
	 * @return Main FOW update routine
	 */
	public final FogOfWarProcessing getFogOfWarProcessing ()
	{
		return fogOfWarProcessing;
	}

	/**
	 * @param obj Main FOW update routine
	 */
	public final void setFogOfWarProcessing (final FogOfWarProcessing obj)
	{
		fogOfWarProcessing = obj;
	}

	/**
	 * @return FOW single changes
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param single FOW single changes
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges single)
	{
		fogOfWarMidTurnChanges = single;
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
	 * @return City calculations
	 */
	public final CityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final CityCalculations calc)
	{
		cityCalculations = calc;
	}

	/**
	 * @return Server-only city calculations
	 */
	public final ServerCityCalculations getServerCityCalculations ()
	{
		return serverCityCalculations;
	}

	/**
	 * @param calc Server-only city calculations
	 */
	public final void setServerCityCalculations (final ServerCityCalculations calc)
	{
		serverCityCalculations = calc;
	}

	/**
	 * @return Methods dealing with unit movement
	 */
	public final UnitMovement getUnitMovement ()
	{
		return unitMovement;
	}

	/**
	 * @param u Methods dealing with unit movement
	 */
	public final void setUnitMovement (final UnitMovement u)
	{
		unitMovement = u;
	}

	/**
	 * @return Starting and ending combats
	 */
	public final CombatStartAndEnd getCombatStartAndEnd ()
	{
		return combatStartAndEnd;
	}

	/**
	 * @param cse Starting and ending combats
	 */
	public final void setCombatStartAndEnd (final CombatStartAndEnd cse)
	{
		combatStartAndEnd = cse;
	}

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
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
	 * @return Treasure awarding utils
	 */
	public final TreasureUtils getTreasureUtils ()
	{
		return treasureUtils;
	}

	/**
	 * @param util Treasure awarding utils
	 */
	public final void setTreasureUtils (final TreasureUtils util)
	{
		treasureUtils = util;
	}

	/** 
	 * @return Unit skill values direct access
	 */
	public final UnitSkillDirectAccess getUnitSkillDirectAccess ()
	{
		return unitSkillDirectAccess;
	}

	/**
	 * @param direct Unit skill values direct access
	 */
	public final void setUnitSkillDirectAccess (final UnitSkillDirectAccess direct)
	{
		unitSkillDirectAccess = direct;
	}

	/**
	 * @return Methods for dealing with player msgs
	 */
	public PlayerMessageProcessing getPlayerMessageProcessing ()
	{
		return playerMessageProcessing;
	}

	/**
	 * @param obj Methods for dealing with player msgs
	 */
	public final void setPlayerMessageProcessing (final PlayerMessageProcessing obj)
	{
		playerMessageProcessing = obj;
	}

	/**
	 * @return Server-only unit calculations
	 */
	public final ServerUnitCalculations getServerUnitCalculations ()
	{
		return serverUnitCalculations;
	}

	/**
	 * @param calc Server-only unit calculations
	 */
	public final void setServerUnitCalculations (final ServerUnitCalculations calc)
	{
		serverUnitCalculations = calc;
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
	 * @return Process for making sure one wizard has met another wizard
	 */
	public final KnownWizardServerUtils getKnownWizardServerUtils ()
	{
		return knownWizardServerUtils;
	}

	/**
	 * @param k Process for making sure one wizard has met another wizard
	 */
	public final void setKnownWizardServerUtils (final KnownWizardServerUtils k)
	{
		knownWizardServerUtils = k;
	}
}