package momime.server.fogofwar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.calculations.UnitMovement;
import momime.common.calculations.UnitStack;
import momime.common.database.CitySpellEffect;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.Pick;
import momime.common.database.Plane;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.PendingMovement;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.FogOfWarVisibleAreaChangedMessage;
import momime.common.messages.servertoclient.MoveUnitStackOverlandMessage;
import momime.common.messages.servertoclient.PendingMovementMessage;
import momime.common.messages.servertoclient.SelectNextUnitToMoveOverlandMessage;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.FogOfWarCalculations;
import momime.server.calculations.ServerCityCalculations;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.process.CombatStartAndEnd;
import momime.server.process.OneCellPendingMovement;
import momime.server.process.PlayerMessageProcessing;
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
	private static final Log log = LogFactory.getLog (FogOfWarMidTurnMultiChangesImpl.class);
	
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
	
	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param combatLocation Location of combat that just ended
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void switchOffMaintainedSpellsCastInCombatLocation_OnServerAndClients (final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final MapCoordinates3DEx combatLocation, final CommonDatabase db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		// Copy the list, since we'll be removing spells from it as we go
		final List<MemoryMaintainedSpell> copyOfTrueSpells = new ArrayList<MemoryMaintainedSpell> ();
		copyOfTrueSpells.addAll (trueMap.getMaintainedSpell ());
		
		for (final MemoryMaintainedSpell trueSpell : copyOfTrueSpells)
			if ((trueSpell.isCastInCombat ()) && (combatLocation.equals (trueSpell.getCityLocation ())))
				getFogOfWarMidTurnChanges ().switchOffMaintainedSpellOnServerAndClients (trueMap, trueSpell.getSpellURN (), players, db, sd);
	}
	
	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param combatLocation Location of combat that just ended
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final MapCoordinates3DEx combatLocation, final CommonDatabase db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		// Copy the list, since we'll be removing spells from it as we go
		final List<MemoryMaintainedSpell> copyOfTrueSpells = new ArrayList<MemoryMaintainedSpell> ();
		copyOfTrueSpells.addAll (trueMap.getMaintainedSpell ());
		
		for (final MemoryMaintainedSpell trueSpell : copyOfTrueSpells)
			if ((trueSpell.isCastInCombat ()) && (trueSpell.getUnitURN () != null))
			{
				// Find the unit that the spell is cast on, to see whether they're in this particular combat
				final MemoryUnit thisUnit = getUnitUtils ().findUnitURN (trueSpell.getUnitURN (), trueMap.getUnit (), "switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients");
				if (combatLocation.equals (thisUnit.getCombatLocation ()))
					getFogOfWarMidTurnChanges ().switchOffMaintainedSpellOnServerAndClients (trueMap, trueSpell.getSpellURN (), players, db, sd);
			}
	}
	
	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param cityLocation Location to turn spells off from
	 * @param castingPlayerID Which player's spells to turn off; 0 = everybodys 
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void switchOffMaintainedSpellsInLocationOnServerAndClients (final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final MapCoordinates3DEx cityLocation, final int castingPlayerID,
		final CommonDatabase db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		// Copy the list, since we'll be removing spells from it as we go
		final List<MemoryMaintainedSpell> copyOfTrueSpells = new ArrayList<MemoryMaintainedSpell> ();
		copyOfTrueSpells.addAll (trueMap.getMaintainedSpell ());
		
		for (final MemoryMaintainedSpell trueSpell : copyOfTrueSpells)
			if ((cityLocation.equals (trueSpell.getCityLocation ())) &&
				((castingPlayerID == 0) || (trueSpell.getCastingPlayerID () == castingPlayerID)))

				getFogOfWarMidTurnChanges ().switchOffMaintainedSpellOnServerAndClients (trueMap, trueSpell.getSpellURN (), players, db, sd);
	}
	
	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param mapLocation Indicates which city the CAE is cast on; null for CAEs not cast on cities
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
	public final void removeCombatAreaEffectsFromLocalisedSpells (final FogOfWarMemory trueMap, final MapCoordinates3DEx mapLocation,
		final List<PlayerServerDetails> players, final CommonDatabase db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		// Make a list of all CAEs caused by permanent city spells at this location, to make sure we don't remove them, e.g. Heavenly Light and Cloud of Darkness
		final List<MemoryCombatAreaEffect> keepCAEs = new ArrayList<MemoryCombatAreaEffect> (); 
		for (final MemoryMaintainedSpell trueSpell : trueMap.getMaintainedSpell ())
			if ((mapLocation.equals (trueSpell.getCityLocation ())) && (trueSpell.getCitySpellEffectID () != null))
			{
				final CitySpellEffect citySpellEffect = db.findCitySpellEffect (trueSpell.getCitySpellEffectID (), "removeCombatAreaEffectsFromLocalisedSpells");
				if (citySpellEffect.getCombatAreaEffectID () != null)
				{
					final MemoryCombatAreaEffect trueCAE = getMemoryCombatAreaEffectUtils ().findCombatAreaEffect
						(trueMap.getCombatAreaEffect (), (MapCoordinates3DEx) trueSpell.getCityLocation (), citySpellEffect.getCombatAreaEffectID (), trueSpell.getCastingPlayerID ());
						
					if (trueCAE != null)
						keepCAEs.add (trueCAE);
				}
			}
		
		// Better copy the list of CAEs, since we'll be removing them as we go along
		final List<MemoryCombatAreaEffect> copyOftrueCAEs = new ArrayList<MemoryCombatAreaEffect> ();
		copyOftrueCAEs.addAll (trueMap.getCombatAreaEffect ());
	
		// CAE must be localised at this combat location (so we don't remove global enchantments like Crusade) and must be owned by a player (so we don't remove node auras)
		for (final MemoryCombatAreaEffect trueCAE : copyOftrueCAEs)
			if ((!keepCAEs.contains (trueCAE)) && (mapLocation.equals (trueCAE.getMapLocation ())) && (trueCAE.getCastingPlayerID () != null))
				getFogOfWarMidTurnChanges ().removeCombatAreaEffectFromServerAndClients (trueMap, trueCAE.getCombatAreaEffectURN (), players, sd);
	}

	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param cityLocation Location of the city to remove the building from
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void destroyAllBuildingsInLocationOnServerAndClients (final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final MapCoordinates3DEx cityLocation,
		final MomSessionDescription sd, final CommonDatabase db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		// Better copy the list of buildings, since we'll be removing them as we go along
		final List<MemoryBuilding> copyOfBuildingsList = new ArrayList<MemoryBuilding> ();
		copyOfBuildingsList.addAll (trueMap.getBuilding ());
		
		for (final MemoryBuilding trueBuilding : copyOfBuildingsList)
			if (cityLocation.equals (trueBuilding.getCityLocation ()))
				getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (trueMap, players, trueBuilding.getBuildingURN (), false, sd, db);
	}
	
	/**
	 * @param trueUnits True list of units to heal/gain experience
	 * @param onlyOnePlayerID If zero, will heal/exp units belonging to all players; if specified will heal/exp only units belonging to the specified player
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of War settings from session description
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found, or the player should be able to see the unit but it isn't in their list
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	@Override
	public final void healUnitsAndGainExperience (final List<MemoryUnit> trueUnits, final int onlyOnePlayerID, final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final CommonDatabase db, final FogOfWarSetting fogOfWarSettings)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// This can generate a lot of data - a unit update for every single one of our own units plus all units we can see (except summoned ones) - so collate the client messages
		final Map<Integer, FogOfWarVisibleAreaChangedMessage> fowMessages = new HashMap<Integer, FogOfWarVisibleAreaChangedMessage> ();
		
		// Now process all units
		for (final MemoryUnit thisUnit : trueUnits)
			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && ((onlyOnePlayerID == 0) || (onlyOnePlayerID == thisUnit.getOwningPlayerID ())))
			{
				final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (thisUnit, null, null, null, players, trueMap, db);
				final Pick magicRealm = xu.getModifiedUnitMagicRealmLifeformType ();
				
				boolean sendMsg = false;

				// Heal?
				if ((magicRealm.isHealEachTurn ()) && (thisUnit.getUnitDamage ().size () > 0))
				{
					getUnitServerUtils ().healDamage (thisUnit.getUnitDamage (), 1, true);
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
				}

				// Inform any clients who know about this unit
				if (sendMsg)
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (thisUnit, trueMap.getMap (), players, db, fogOfWarSettings, fowMessages);
			}
		
		// Send out client updates
		for (final Entry<Integer, FogOfWarVisibleAreaChangedMessage> entry : fowMessages.entrySet ())
		{
			final PlayerServerDetails player = getMultiplayerSessionServerUtils ().findPlayerWithID (players, entry.getKey (), "healUnitsAndGainExperience");
			player.getConnection ().sendMessageToClient (entry.getValue ());
		}
	}
	
	/**
	 * When a unit dies in combat, all the units on the opposing side gain 1 exp. 
	 * 
	 * @param combatLocation The location where the combat is taking place
	 * @param combatSide Which side is to gain 1 exp
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of War settings from session description
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found, or the player should be able to see the unit but it isn't in their list
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	@Override
	public final void grantExperienceToUnitsInCombat (final MapCoordinates3DEx combatLocation, final UnitCombatSideID combatSide,
		final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final CommonDatabase db, final FogOfWarSetting fogOfWarSettings)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// If 9 units gain experience, don't send out 9 separate messages
		final Map<Integer, FogOfWarVisibleAreaChangedMessage> fowMessages = new HashMap<Integer, FogOfWarVisibleAreaChangedMessage> ();
		
		// Find the units who are in combat on the side that earned the kill
		for (final MemoryUnit trueUnit : trueMap.getUnit ())
			if ((trueUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (trueUnit.getCombatLocation ())) &&
				(trueUnit.getCombatSide () == combatSide) && (trueUnit.getCombatPosition () != null) && (trueUnit.getCombatHeading () != null))
			{
				final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (trueUnit, null, null, null, players, trueMap, db);
				final Pick magicRealm = xu.getModifiedUnitMagicRealmLifeformType ();

				int exp = getUnitSkillDirectAccess ().getDirectSkillValue (trueUnit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
				if ((magicRealm.isGainExperienceEachTurn ()) && (exp >= 0))
				{
					exp++;
					getUnitSkillDirectAccess ().setDirectSkillValue (trueUnit, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, exp);
					
					// Note we don't do this directly on xu as that will not reflect the updated experience, and don't want to make a whole new ExpandedUnitDetails just to do a simple check
					getUnitServerUtils ().checkIfHeroGainedALevel (xu.getUnitURN (), xu.getUnitType (), (PlayerServerDetails) xu.getOwningPlayer (), exp);
					
					// This updates both the player memories on the server, and sends messages out to the clients, as needed
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (trueUnit, trueMap.getMap (), players, db, fogOfWarSettings, fowMessages);
				}				
			}
		
		// Send out client updates
		for (final Entry<Integer, FogOfWarVisibleAreaChangedMessage> entry : fowMessages.entrySet ())
		{
			final PlayerServerDetails player = getMultiplayerSessionServerUtils ().findPlayerWithID (players, entry.getKey (), "grantExperienceToUnitsInCombat");
			player.getConnection ().sendMessageToClient (entry.getValue ());
		}
		
		getPlayerMessageProcessing ().sendNewTurnMessages (null, players, null);
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
	 * @param players List of players in the session
	 * @param gsk Server knowledge structure
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void moveUnitStackOneCellOnServerAndClients (final List<MemoryUnit> unitStack, final PlayerServerDetails unitStackOwner,
		final MapCoordinates3DEx moveFrom, final MapCoordinates3DEx moveTo, final List<PlayerServerDetails> players,
		final MomGeneralServerKnowledge gsk, final MomSessionDescription sd, final CommonDatabase db)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException
	{
		// We need a list of the unit URNs
		final List<Integer> unitURNList = new ArrayList<Integer> ();
		for (final MemoryUnit tu : unitStack)
			unitURNList.add (tu.getUnitURN ());
		
		// Check each player in turn
		for (final PlayerServerDetails thisPlayer : players)
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
					(moveFrom, sd.getFogOfWarSetting ().getUnits (), gsk.getTrueMap ().getMap (), priv.getFogOfWar (), db);
				canSeeAfterMove = getFogOfWarCalculations ().canSeeMidTurnOnAnyPlaneIfTower
					(moveTo, sd.getFogOfWarSetting ().getUnits (), gsk.getTrueMap ().getMap (), priv.getFogOfWar (), db);

				// Deal with clients who could not see this unit stack before this move, but now can
				if ((!couldSeeBeforeMove) && (canSeeAfterMove))
				{
					// The unit stack doesn't exist yet in the player's memory or on the client, so before they can move, we have to send all the unit details
					getFogOfWarMidTurnChanges ().addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient (unitStack, gsk.getTrueMap ().getMaintainedSpell (), thisPlayer);
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
				if (thisPlayer.getPlayerDescription ().isHuman ())
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
		getFogOfWarProcessing ().updateAndSendFogOfWar (gsk.getTrueMap (), unitStackOwner, players, "moveUnitStackOneCellOnServerAndClients", sd, db);

		// If we moved out of or into a city, then need to recalc rebels, production, because the units may now be (or may now no longer be) helping ease unrest.
		// Note this doesn't deal with capturing cities - attacking even an empty city is treated as a combat, so we can pick Capture/Raze.
		final MapCoordinates3DEx [] cityLocations = new MapCoordinates3DEx [] {moveFrom, moveTo};
		for (final MapCoordinates3DEx cityLocation : cityLocations)
		{
			final OverlandMapCityData cityData = gsk.getTrueMap ().getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
			if (cityData != null)
			{
				final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (players, cityData.getCityOwnerID (), "moveUnitStackOneCellOnServerAndClients");
				final MomPersistentPlayerPrivateKnowledge cityOwnerPriv = (MomPersistentPlayerPrivateKnowledge) cityOwner.getPersistentPlayerPrivateKnowledge ();

				cityData.setNumberOfRebels (getCityCalculations ().calculateCityRebels (players, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getUnit (), gsk.getTrueMap ().getBuilding (),
					cityLocation, cityOwnerPriv.getTaxRateID (), db).getFinalTotal ());

				getServerCityCalculations ().ensureNotTooManyOptionalFarmers (cityData);

				getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (gsk.getTrueMap ().getMap (), players, cityLocation, sd.getFogOfWarSetting ());
			}
		}
		
		// Record the current tile type and map feature, in case we captured a node/lair/tower and have treasure to award.
		// In case there's a prisoner, we need lairs to have been removed so that the prisoner doesn't get bumped to an adjacent cell unnecessarily,
		// but the rollTreasureReward routine still needs to know what the type of lair that was removed was, since it affects the type of spell books that can be obtained.
		final ServerGridCellEx tc = (ServerGridCellEx) gsk.getTrueMap ().getMap ().getPlane ().get (moveTo.getZ ()).getRow ().get (moveTo.getY ()).getCell ().get (moveTo.getX ());
		final String tileTypeID = tc.getTerrainData ().getTileTypeID ();
		final String mapFeatureID = tc.getTerrainData ().getMapFeatureID ();
		
		// If we captured a monster lair, temple, etc. then remove it from the map (don't remove nodes or towers of course).
		// This is now part of movement, rather than part of cleaning up after a combat, because capturing empty lairs no longer even initiates a combat.
		// If the monsters in a lair are killed in a combat, then the attackers advance after the combat using this same routine, so that also works.
		if ((tc.getTerrainData ().getMapFeatureID () != null) &&
			(db.findMapFeature (tc.getTerrainData ().getMapFeatureID (), "moveUnitStackOneCellOnServerAndClients").getMapFeatureMagicRealm ().size () > 0) &&
			(!getMemoryGridCellUtils ().isTerrainTowerOfWizardry (tc.getTerrainData ())))
		{
			log.debug ("Removing lair at " + moveTo);
			tc.getTerrainData ().setMapFeatureID (null);
			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (gsk.getTrueMap ().getMap (), players, moveTo, sd.getFogOfWarSetting ().getTerrainAndNodeAuras ());
		}					
		
		// If we captured a tower of wizardry, then turn the light on
		else if (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY.equals (tc.getTerrainData ().getMapFeatureID ()))
		{
			for (final Plane plane : db.getPlane ())
			{
				final MapCoordinates3DEx towerCoords = new MapCoordinates3DEx (moveTo.getX (), moveTo.getY (), plane.getPlaneNumber ());
				log.debug ("Turning light on in tower at " + towerCoords);
				
				gsk.getTrueMap ().getMap ().getPlane ().get (towerCoords.getZ ()).getRow ().get (towerCoords.getY ()).getCell ().get
					(towerCoords.getX ()).getTerrainData ().setMapFeatureID (CommonDatabaseConstants.FEATURE_CLEARED_TOWER_OF_WIZARDRY);
				getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (gsk.getTrueMap ().getMap (), players, towerCoords, sd.getFogOfWarSetting ().getTerrainAndNodeAuras ());
			}
		}

		// If we captured a node/lair/tower then award the treasure.
		if (tc.getTreasureValue () != null)
		{
			getTreasureUtils ().sendTreasureReward
				(getTreasureUtils ().rollTreasureReward (tc.getTreasureValue (), unitStackOwner, moveTo, tileTypeID, mapFeatureID, players, gsk, sd, db),
					unitStackOwner, players, db);
			tc.setTreasureValue (null);
		}
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
	 * @param moveTo Location to move to
	 * 		Note about moveTo.getPlane () - the same comment as moveUnitStackOneCellOnServerAndClients *doesn't apply*, moveTo.getPlane ()
	 *			will be whatever the player clicked on - if they click on a tower on Myrror, moveTo.getPlane () will be set to 1; the routine
	 *			sorts the correct destination plane out for each cell that the unit stack moves
	 *
	 * @param forceAsPendingMovement If true, forces all generated moves to be added as pending movements rather than occurring immediately (used for simultaneous turns games)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the move resulted in a combat being started in a one-player-at-a-time game (and thus the player's turn should halt while the combat is played out)
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final boolean moveUnitStack (final List<ExpandedUnitDetails> selectedUnits, final PlayerServerDetails unitStackOwner, final boolean processCombats,
		final MapCoordinates3DEx originalMoveFrom, final MapCoordinates3DEx moveTo,
		final boolean forceAsPendingMovement, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException
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
		int doubleMovementRemaining = 0;
		int [] [] [] movementDirections = null;

		MapCoordinates3DEx moveFrom = originalMoveFrom;
		boolean combatInitiated = false;

		while (keepGoing)
		{
			// What's the lowest movement remaining of any unit in the stack
			doubleMovementRemaining = Integer.MAX_VALUE;
			for (final ExpandedUnitDetails thisUnit : movingUnits)
				if (thisUnit.getDoubleOverlandMovesLeft () < doubleMovementRemaining)
					doubleMovementRemaining = thisUnit.getDoubleOverlandMovesLeft ();

			// Find distances and route from our start point to every location on the map
			final int [] [] [] doubleMovementDistances			= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
			movementDirections											= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
			final boolean [] [] [] canMoveToInOneTurn			= new boolean [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
			final boolean [] [] [] movingHereResultsInAttack	= new boolean [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];

			getUnitMovement ().calculateOverlandMovementDistances (moveFrom.getX (), moveFrom.getY (), moveFrom.getZ (), unitStackOwner.getPlayerDescription ().getPlayerID (),
				priv.getFogOfWarMemory (), unitStack, doubleMovementRemaining,
				doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

			// Is there a route to where we want to go?
			validMoveFound = (doubleMovementDistances [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()] >= 0);

			// Make 1 move as long as there is a valid move, and we're not allocating movement in a simultaneous turns game
			if ((validMoveFound) && (!forceAsPendingMovement))
			{
				// Get the direction to make our 1 move in
				final int movementDirection = getFogOfWarMidTurnChanges ().determineMovementDirection (moveFrom, moveTo, movementDirections, mom.getSessionDescription ().getOverlandMapSize ());

				// Work out where this moves us to
				final MapCoordinates3DEx oneStep = new MapCoordinates3DEx (moveFrom);
				getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), oneStep, movementDirection);
				
				MemoryGridCell oneStepTrueTile = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(oneStep.getZ ()).getRow ().get (oneStep.getY ()).getCell ().get (oneStep.getX ());

				// Adjust move to plane if moving onto or off of a tower
				if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (oneStepTrueTile.getTerrainData ()))
				{
					combatInitiated = movingHereResultsInAttack [oneStep.getZ ()] [oneStep.getY ()] [oneStep.getX ()];
					oneStep.setZ (0);
				}
				else
				{
					oneStep.setZ (moveTo.getZ ());
					combatInitiated = movingHereResultsInAttack [oneStep.getZ ()] [oneStep.getY ()] [oneStep.getX ()];
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
						getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (thisUnit.getMemoryUnit (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
							mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting (), null);
	
						if (thisUnit.getDoubleOverlandMovesLeft () < doubleMovementRemaining)
							doubleMovementRemaining = thisUnit.getDoubleOverlandMovesLeft ();
					}
				}

				// Make our 1 movement?
				if (!combatInitiated)
				{
					// Actually move the units
					moveUnitStackOneCellOnServerAndClients (allUnitsMem, unitStackOwner, moveFrom, oneStep,
						mom.getPlayers (), mom.getGeneralServerKnowledge (), mom.getSessionDescription (), mom.getServerDB ());

					// Prepare for next loop
					moveFrom = oneStep;
				}
			}

			// Check whether to loop again
			keepGoing = (!forceAsPendingMovement) && (validMoveFound) && (!combatInitiated) && (doubleMovementRemaining > 0) &&
				((moveFrom.getX () != moveTo.getX ()) || (moveFrom.getY () != moveTo.getY ()));
		}

		// If the unit stack failed to reach its destination this turn, create a pending movement object so they'll continue their movement next turn
		if ((!combatInitiated) && ((moveFrom.getX () != moveTo.getX ()) || (moveFrom.getY () != moveTo.getY ())))
		{
			// Unless ForceAsPendingMovement is on, we'll have made at least one move so should recalc the
			// best path again based on what else we learned about the terrain in our last move
			if (!forceAsPendingMovement)
			{
				final int [] [] [] doubleMovementDistances			= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
				movementDirections											= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
				final boolean [] [] [] canMoveToInOneTurn			= new boolean [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
				final boolean [] [] [] movingHereResultsInAttack	= new boolean [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];

				getUnitMovement ().calculateOverlandMovementDistances (moveFrom.getX (), moveFrom.getY (), moveFrom.getZ (), unitStackOwner.getPlayerDescription ().getPlayerID (),
					priv.getFogOfWarMemory (), unitStack, doubleMovementRemaining,
					doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

				validMoveFound = (doubleMovementDistances [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()] >= 0);
			}

			if (validMoveFound)
			{
				final PendingMovement pending = new PendingMovement ();
				pending.setMoveFrom (moveFrom);
				pending.setMoveTo (moveTo);

				for (final ExpandedUnitDetails thisUnit : selectedUnits)
					pending.getUnitURN ().add (thisUnit.getUnitURN ());

				// Record the movement path
				final MapCoordinates3DEx coords = new MapCoordinates3DEx (moveTo);
				while ((coords.getX () != moveFrom.getX () || (coords.getY () != moveFrom.getY ())))
				{
					final int direction = movementDirections [coords.getZ ()] [coords.getY ()] [coords.getX ()];

					pending.getPath ().add (direction);

					if (!getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, getCoordinateSystemUtils ().normalizeDirection
						(mom.getSessionDescription ().getOverlandMapSize ().getCoordinateSystemType (), direction + 4)))
						
						throw new MomException ("moveUnitStack: Server map tracing moved to a cell off the map");
				}

				priv.getPendingMovement ().add (pending);

				// Send the pending movement to the client
				if (unitStackOwner.getPlayerDescription ().isHuman ())
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
			if (unitStackOwner.getPlayerDescription ().isHuman ())
				unitStackOwner.getConnection ().sendMessageToClient (new SelectNextUnitToMoveOverlandMessage ());
		}
		else if (!processCombats)
			log.debug ("Would have started combat, but processCombats is false");
		else
		{
			// What plane will the monsters/defenders be on?
			final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(moveTo.getZ ()).getRow ().get (moveTo.getY ()).getCell ().get (moveTo.getX ());
			
			final int towerPlane;
			if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (tc.getTerrainData ()))
				towerPlane = 0;
			else
				towerPlane = moveTo.getZ ();
			
			// Scheduled the combat or start it immediately
			final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (moveTo.getX (), moveTo.getY (), towerPlane);

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
		final int [] [] [] doubleMovementDistances			= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
		final int [] [] [] movementDirections					= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
		final boolean [] [] [] canMoveToInOneTurn			= new boolean [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
		final boolean [] [] [] movingHereResultsInAttack	= new boolean [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];

		getUnitMovement ().calculateOverlandMovementDistances (moveFrom.getX (), moveFrom.getY (), moveFrom.getZ (), unitStackOwner.getPlayerDescription ().getPlayerID (),
			priv.getFogOfWarMemory (), unitStack, doubleMovementRemaining,
			doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

		// Is there a route to where we want to go?
		final OneCellPendingMovement result;
		if (doubleMovementDistances [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()] < 0)
			result = null;
		else
		{
			// Get the direction to make our 1 move in
			final int movementDirection = getFogOfWarMidTurnChanges ().determineMovementDirection (moveFrom, moveTo, movementDirections, mom.getSessionDescription ().getOverlandMapSize ());

			// Work out where this moves us to
			// If we are moving ON to a tower from Myrror then plane still must be 1, or moveUnitStack thinks we can't click there, and it will adjust the plane when we actually move
			// If we are moving OFF of a tower onto Myrror, then plane must be 1 or we'll get off on the wrong plane
			// So plane must always be set from moveTo
			final MapCoordinates3DEx oneStep = new MapCoordinates3DEx (moveFrom.getX (), moveFrom.getY (), moveTo.getZ ());
			getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), oneStep, movementDirection);

			// Does this initiate a combat?
			final boolean combatInitiated = movingHereResultsInAttack [oneStep.getZ ()] [oneStep.getY ()] [oneStep.getX ()];

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
	public final List<Integer> determineMovementPath (final List<ExpandedUnitDetails> selectedUnits, final PlayerServerDetails unitStackOwner,
		final MapCoordinates3DEx moveFrom, final MapCoordinates3DEx moveTo, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, PlayerNotFoundException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) unitStackOwner.getPersistentPlayerPrivateKnowledge ();
		
		final UnitStack unitStack = getUnitCalculations ().createUnitStack (selectedUnits, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
		
		// We do this at the end of simultaneous turns movement, when all units stacks have used up all movement, so we know this
		final int doubleMovementRemaining = 0;
		
		// Find distances and route from our start point to every location on the map
		final int [] [] [] doubleMovementDistances			= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
		final int [] [] [] movementDirections					= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
		final boolean [] [] [] canMoveToInOneTurn			= new boolean [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
		final boolean [] [] [] movingHereResultsInAttack	= new boolean [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];

		getUnitMovement ().calculateOverlandMovementDistances (moveFrom.getX (), moveFrom.getY (), moveFrom.getZ (), unitStackOwner.getPlayerDescription ().getPlayerID (),
			priv.getFogOfWarMemory (), unitStack, doubleMovementRemaining,
			doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

		// Is there a route to where we want to go?
		final List<Integer> result;
		if (doubleMovementDistances [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()] < 0)
			result = null;
		else
		{
			// Record the movement path
			result = new ArrayList<Integer> ();

			final MapCoordinates3DEx coords = new MapCoordinates3DEx (moveTo);
			while ((coords.getX () != moveFrom.getX () || (coords.getY () != moveFrom.getY ())))
			{
				final int direction = movementDirections [coords.getZ ()] [coords.getY ()] [coords.getX ()];
				result.add (direction);
	
				if (!getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, getCoordinateSystemUtils ().normalizeDirection
					(mom.getSessionDescription ().getOverlandMapSize ().getCoordinateSystemType (), direction + 4)))
					
					throw new MomException ("determineMovementPath: Server map tracing moved to a cell off the map");
			}
		}
		
		return result;
	}

	/**
	 * Gives all units full movement back again overland
	 *
	 * @param onlyOnePlayerID If zero, will reset movmenet for units belonging to all players; if specified will reset movement only for units belonging to the specified player
	 * @param players Players list
	 * @param trueMap True terrain, list of units and so on
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	@Override
	public final void resetUnitOverlandMovement (final int onlyOnePlayerID, final List<PlayerServerDetails> players,
		final FogOfWarMemory trueMap, final FogOfWarSetting fogOfWarSettings, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		// This can generate a lot of data - a unit update for every single one of our own units plus all units we can see - so collate the client messages
		final Map<Integer, FogOfWarVisibleAreaChangedMessage> fowMessages = new HashMap<Integer, FogOfWarVisibleAreaChangedMessage> ();

		// Check every unit
		for (final MemoryUnit thisUnit : trueMap.getUnit ())
			if ((onlyOnePlayerID == 0) || (onlyOnePlayerID == thisUnit.getOwningPlayerID ()))
			{
				thisUnit.setDoubleOverlandMovesLeft (2 * getUnitUtils ().expandUnitDetails (thisUnit, null, null, null, players, trueMap, db).getModifiedSkillValue
					(CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED));

				getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (thisUnit, trueMap.getMap (), players, db, fogOfWarSettings, fowMessages);
			}
		
		// Send out client updates
		for (final Entry<Integer, FogOfWarVisibleAreaChangedMessage> entry : fowMessages.entrySet ())
		{
			final PlayerServerDetails player = getMultiplayerSessionServerUtils ().findPlayerWithID (players, entry.getKey (), "healUnitsAndGainExperience");
			player.getConnection ().sendMessageToClient (entry.getValue ());
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
}