package momime.server.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.utils.random.RandomUtils;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.MapFeatureEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellValidTileTypeTarget;
import momime.common.database.UnitEx;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageSummonUnit;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.OverlandCastingInfo;
import momime.common.messages.servertoclient.OverlandCastingInfoMessage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.SpellTargetingUtils;
import momime.common.utils.TargetSpellResult;
import momime.server.MomSessionVariables;
import momime.server.ai.RelationAI;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.utils.UnitAddLocation;
import momime.server.utils.UnitServerUtils;

/**
 * Methods for casting specific types of spells
 */
public final class SpellCastingImpl implements SpellCasting
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (SpellCastingImpl.class);
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Server-only unit calculations */
	private ServerUnitCalculations serverUnitCalculations;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Methods that determine whether something is a valid target for a spell */
	private SpellTargetingUtils spellTargetingUtils;
	
	/** Damage processor */
	private DamageProcessor damageProcessor;
	
	/** City processing methods */
	private CityProcessing cityProcessing;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** For calculating relation scores between two wizards */
	private RelationAI relationAI;
	
	/**
	 * Processes casting a summoning spell overland, finding where there is space for the unit to go and adding it
	 * 
	 * @param spell Summoning spell
	 * @param player Player who is casting it
	 * @param summonLocation Location where the unit will appear (or try to)
	 * @param sendNewTurnMessage Notify player about the summoned unit on NTM scroll?
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void castOverlandSummoningSpell (final Spell spell, final PlayerServerDetails player, final MapCoordinates3DEx summonLocation,
		final boolean sendNewTurnMessage, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
		
		final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails
			(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), player.getPlayerDescription ().getPlayerID (), "castOverlandSummoningSpell");
		
		// List out all the Unit IDs that this spell can summon
		final List<UnitEx> possibleUnits = getServerUnitCalculations ().listUnitsSpellMightSummon (spell, wizardDetails,
			mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getServerDB ());

		// Pick one at random
		if (possibleUnits.size () > 0)
		{
			final UnitEx summonedUnit = possibleUnits.get (getRandomUtils ().nextInt (possibleUnits.size ()));

			log.debug ("Player " + player.getPlayerDescription ().getPlayerName () + " had " + possibleUnits.size () + " possible units to summon from spell " +
				spell.getSpellID () + ", randomly picked unit ID " + summonedUnit.getUnitID ());

			// Check if the summon location has space for the unit
			final UnitAddLocation addLocation = getUnitServerUtils ().findNearestLocationWhereUnitCanBeAdded
				(summonLocation, summonedUnit.getUnitID (), player.getPlayerDescription ().getPlayerID (), mom);

			final MemoryUnit newUnit;
			if (addLocation.getUnitLocation () == null)
				newUnit = null;
			else
			{
				// Add the unit
				if (summonedUnit.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
				{
					// The unit object already exists for heroes
					newUnit = getUnitServerUtils ().findUnitWithPlayerAndID (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), player.getPlayerDescription ().getPlayerID (), summonedUnit.getUnitID ());

					if (newUnit.getStatus () == UnitStatusID.NOT_GENERATED)
						getUnitServerUtils ().generateHeroNameAndRandomSkills (newUnit, mom.getServerDB ());

					getFogOfWarMidTurnChanges ().updateUnitStatusToAliveOnServerAndClients (newUnit, addLocation.getUnitLocation (), player, true, true, mom);
				}
				else
					// For non-heroes, create a new unit
					newUnit = getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (summonedUnit.getUnitID (), addLocation.getUnitLocation (), null, null, null,
						player, UnitStatusID.ALIVE, true, true, mom);
			}

			// Show on new turn messages for the player who summoned it
			if ((sendNewTurnMessage) && (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN))
			{
				final NewTurnMessageSummonUnit summoningSpell = new NewTurnMessageSummonUnit ();
				summoningSpell.setMsgType (NewTurnMessageTypeID.SUMMONED_UNIT);
				summoningSpell.setSpellID (spell.getSpellID ());
				summoningSpell.setUnitID (summonedUnit.getUnitID ());
				summoningSpell.setCityLocation (addLocation.getUnitLocation ());
				summoningSpell.setUnitAddBumpType (addLocation.getBumpType ());

				if (newUnit != null)
					summoningSpell.setUnitURN (newUnit.getUnitURN ());

				trans.getNewTurnMessage ().add (summoningSpell);
			}
		}
	}

	/**
	 * Normally the spells being cast by other wizards are private, but we get to see this info if we have Detect Magic or Spell Blast cast.
	 * 
	 * @param ourSpellID Which spell allows us to see the info - Detect Magic or Spell Blast
	 * @param onlyOnePlayerID If zero, will send to all players who have Detect Magic cast; if specified will send only to the specified player
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If one of the wizard isn't found in the list
	 */
	@Override
	public final void sendOverlandCastingInfo (final String ourSpellID, final int onlyOnePlayerID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException
	{
		// Don't bother to build the message until we find at least one human player who needs it
		OverlandCastingInfoMessage msg = null;

		for (final PlayerServerDetails sendToPlayer : mom.getPlayers ())
			if (((onlyOnePlayerID == 0) || (onlyOnePlayerID == sendToPlayer.getPlayerDescription ().getPlayerID ())) &&
				(sendToPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN) &&
				((ourSpellID.equals (CommonDatabaseConstants.SPELL_ID_SPELL_BLAST)) ||
					(getMemoryMaintainedSpellUtils ().findMaintainedSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
						sendToPlayer.getPlayerDescription ().getPlayerID (), ourSpellID, null, null, null, null) != null)))
			{
				// Need to build message or already done?
				if (msg == null)
				{
					final MomPersistentPlayerPrivateKnowledge sendToPriv = (MomPersistentPlayerPrivateKnowledge) sendToPlayer.getPersistentPlayerPrivateKnowledge ();

					msg = new OverlandCastingInfoMessage ();
					msg.setOurSpellID (ourSpellID);
					
					for (final PlayerServerDetails player : mom.getPlayers ())
					{
						final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails
							(sendToPriv.getFogOfWarMemory ().getWizardDetails (), player.getPlayerDescription ().getPlayerID ());
						
						if ((wizardDetails != null) && (getPlayerKnowledgeUtils ().isWizard (wizardDetails.getWizardID ())) && (wizardDetails.getWizardState () == WizardState.ACTIVE))
							msg.getOverlandCastingInfo ().add (createOverlandCastingInfo (player, ourSpellID));
					}
				}
				
				sendToPlayer.getConnection ().sendMessageToClient (msg);
			}
	}
	
	/**
	 * @param player Player to create casting info for
	 * @param ourSpellID Which spell allows us to see the info - Detect Magic or Spell Blast
	 * @return Summary details about what the wizard is casting overland
	 */
	@Override
	public final OverlandCastingInfo createOverlandCastingInfo (final PlayerServerDetails player, final String ourSpellID)
	{
		final OverlandCastingInfo info = new OverlandCastingInfo ();
		info.setPlayerID (player.getPlayerDescription ().getPlayerID ());
		
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		if (priv.getQueuedSpell ().size () > 0)
		{
			info.setSpellID (priv.getQueuedSpell ().get (0).getQueuedSpellID ());
			
			if (ourSpellID.equals (CommonDatabaseConstants.SPELL_ID_SPELL_BLAST))
				info.setManaSpentOnCasting (priv.getManaSpentOnCastingCurrentSpell ());
		}
		
		return info;
	}
	
	/**
	 * Processes casting an attack spell overland that hits all units in a stack (that are valid targets), or all units in multiple stacks.
	 * If multiple targetLocations are specified then the units may not all belong to the same player.
	 * 
	 * @param castingPlayer Player who cast the attack spell; can be null if not being cast by a player
	 * @param eventID The event that caused an attack, if it wasn't initiated by a player
	 * @param spell Which attack spell they cast
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them
	 * @param targetLocations Location(s) where the spell is aimed
	 * @param penaltyToVisibleRelation How mad each wizard will get who has units affected by the attack
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Number of units that were killed
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final int castOverlandAttackSpell (final PlayerServerDetails castingPlayer, final String eventID, final Spell spell, final Integer variableDamage,
		final List<MapCoordinates3DEx> targetLocations, final int penaltyToVisibleRelation, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		// Pass in FOW area, though it doesn't really have any effect as we're passing isTargeting = false which turns off the check that we must be able to see the target units
		final MomPersistentPlayerPrivateKnowledge priv = (castingPlayer == null) ? null :
			(MomPersistentPlayerPrivateKnowledge) castingPlayer.getPersistentPlayerPrivateKnowledge ();
		
		// Break up all the target units into separate lists for each player
		final Map<Integer, List<ResolveAttackTarget>> targetUnitsForEachPlayer = new HashMap<Integer, List<ResolveAttackTarget>> ();
		
		for (final MemoryUnit tu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
			if ((targetLocations.contains (tu.getUnitLocation ())) && (tu.getStatus () == UnitStatusID.ALIVE))
			{
				final ExpandedUnitDetails thisTarget = getExpandUnitDetails ().expandUnitDetails (tu, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
				
				if (getSpellTargetingUtils ().isUnitValidTargetForSpell (spell, SpellBookSectionID.ATTACK_SPELLS, null, null,
					(castingPlayer == null) ? 0 : castingPlayer.getPlayerDescription ().getPlayerID (),
						null, null, thisTarget, false, mom.getGeneralServerKnowledge ().getTrueMap (),
						(priv == null) ? null : priv.getFogOfWar (), mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
				{
					// Does a list exist for this player yet?
					List<ResolveAttackTarget> targetUnits = targetUnitsForEachPlayer.get (thisTarget.getOwningPlayerID ());
					if (targetUnits == null)
					{
						targetUnits = new ArrayList<ResolveAttackTarget> ();
						targetUnitsForEachPlayer.put (thisTarget.getOwningPlayerID (), targetUnits);
					}
					
					targetUnits.add (new ResolveAttackTarget (tu));
				}
			}
		
		int unitsKilled = 0;
		for (final Entry<Integer, List<ResolveAttackTarget>> entry : targetUnitsForEachPlayer.entrySet ())
		{
			final PlayerServerDetails defendingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), entry.getKey (), "castOverlandAttackSpell");
			
			final ResolveAttackResult result = getDamageProcessor ().resolveAttack (null, entry.getValue (), castingPlayer, defendingPlayer, eventID,
				null, null, null, null, spell, variableDamage, castingPlayer, null, false, mom);
			
			unitsKilled = unitsKilled + result.getAttackingPlayerUnitsKilled () + result.getDefendingPlayerUnitsKilled ();
			
			// Player gets mad about their units being attacked
			if ((penaltyToVisibleRelation > 0) && (defendingPlayer != castingPlayer))
			{
				final KnownWizardDetails defendingWizard = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), entry.getKey (), "castOverlandAttackSpell");
				if (getPlayerKnowledgeUtils ().isWizard (defendingWizard.getWizardID ()))
				{
					final MomPersistentPlayerPrivateKnowledge defendingPriv = (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
					final KnownWizardDetails defendingWizardOpinionOfCaster = getKnownWizardUtils ().findKnownWizardDetails (defendingPriv.getFogOfWarMemory ().getWizardDetails (), castingPlayer.getPlayerDescription ().getPlayerID ());
					if (defendingWizardOpinionOfCaster != null)
						getRelationAI ().penaltyToVisibleRelation ((DiplomacyWizardDetails) defendingWizardOpinionOfCaster, penaltyToVisibleRelation);
				}
			}
		}
		
		return unitsKilled;
	}
	
	/**
	 * Rolls when a spell has a certain % chance of destroying each building in a city.  Used for Earthquake and Chaos Rift.
	 * 
	 * @param spellID The spell that is destroying the buildings
	 * @param castingPlayerID Who cast the spell; null if not from a spell 
	 * @param percentageChance The % chance of each building being destroyed
	 * @param targetLocations The city(s) being targeted
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Number of buildings that were destroyed
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final int rollChanceOfEachBuildingBeingDestroyed (final String spellID, final Integer castingPlayerID, final int percentageChance,
		final List<MapCoordinates3DEx> targetLocations, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final List<MemoryBuilding> destroyedBuildings = new ArrayList<MemoryBuilding> ();
		for (final MemoryBuilding thisBuilding : mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ())
			if ((targetLocations.contains (thisBuilding.getCityLocation ())) &&
				(!thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS)) &&
				(!thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)) &&
				(getRandomUtils ().nextInt (100) < percentageChance))
				
				destroyedBuildings.add (thisBuilding);

		// Have to do this even if 0 buildings got destroyed, as for Earthquake this is how the client knows to show the animation and clean up the NTM
		getCityProcessing ().destroyBuildings (destroyedBuildings, spellID, castingPlayerID,
			(targetLocations.size () == 1) ? targetLocations.get (0) : null, mom);
		
		return destroyedBuildings.size ();
	}
	
	/**
	 * @param targetLocation Tile to corrupt
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void corruptTile (final MapCoordinates3DEx targetLocation, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).getTerrainData ();
		terrainData.setCorrupted (5);
			
		getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
			mom.getPlayers (), targetLocation, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
			
		getCityProcessing ().recheckCurrentConstructionIsStillValid (targetLocation, mom);
	}

	/**
	 * Change a change tile type kind of spell, like Change Terrain or Raise Volcano
	 * 
	 * @param spell Which spell was cast
	 * @param targetLocation Tile to change
	 * @param castingPlayerID Player who cast the spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void changeTileType (final Spell spell, final MapCoordinates3DEx targetLocation, final int castingPlayerID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		// Change Terrain or Raise Volcano
		final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).getTerrainData ();
		
		final Iterator<SpellValidTileTypeTarget> iter = spell.getSpellValidTileTypeTarget ().iterator ();				
		boolean found = false;
		while ((!found) && (iter.hasNext ()))
		{
			final SpellValidTileTypeTarget thisTileType = iter.next ();
			if (thisTileType.getTileTypeID ().equals (terrainData.getTileTypeID ()))
			{
				if (thisTileType.getChangeToTileTypeID () == null)
					throw new MomException ("Spell " + spell.getSpellID () + " is a change terrain type spell but has no tile type defined to change from " + thisTileType.getTileTypeID ());
				
				terrainData.setTileTypeID (thisTileType.getChangeToTileTypeID ());
				
				if (thisTileType.getChangeToTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_RAISE_VOLCANO))
					terrainData.setVolcanoOwnerID (castingPlayerID);
				else
					terrainData.setVolcanoOwnerID (null);
				
				if ((thisTileType.isMineralDestroyed () != null) && (thisTileType.isMineralDestroyed ()) && (terrainData.getMapFeatureID () != null))
				{
					// Minerals are destroyed, but not lairs
					final MapFeatureEx mapFeature = mom.getServerDB ().findMapFeature (terrainData.getMapFeatureID (), "changeTileType");
					if (mapFeature.getMapFeatureMagicRealm ().size () == 0)
						terrainData.setMapFeatureID (null);
				}
				
				if (thisTileType.getBuildingsDestroyedChance () != null)
				{
					// Every building here has a chance of being destroyed
					final List<MemoryBuilding> destroyedBuildings = new ArrayList<MemoryBuilding> ();
					for (final MemoryBuilding thisBuilding : mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ())
						if ((thisBuilding.getCityLocation ().equals (targetLocation)) &&
							(!thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS)) &&
							(!thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)) &&
							(getRandomUtils ().nextInt (100) < thisTileType.getBuildingsDestroyedChance ()))
							
							destroyedBuildings.add (thisBuilding);
					
					if (destroyedBuildings.size () > 0)
						getCityProcessing ().destroyBuildings (destroyedBuildings, spell.getSpellID (), castingPlayerID, null, mom);
				}
				
				found = true;
			}
		}
		
		if (found)
		{
			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getPlayers (), targetLocation, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());

			getCityProcessing ().recheckCurrentConstructionIsStillValid (targetLocation, mom);
		}
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
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtils)
	{
		memoryMaintainedSpellUtils = spellUtils;
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
	 * @return City processing methods
	 */
	public final CityProcessing getCityProcessing ()
	{
		return cityProcessing;
	}

	/**
	 * @param obj City processing methods
	 */
	public final void setCityProcessing (final CityProcessing obj)
	{
		cityProcessing = obj;
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
	 * @return For calculating relation scores between two wizards
	 */
	public final RelationAI getRelationAI ()
	{
		return relationAI;
	}

	/**
	 * @param ai For calculating relation scores between two wizards
	 */
	public final void setRelationAI (final RelationAI ai)
	{
		relationAI = ai;
	}
}