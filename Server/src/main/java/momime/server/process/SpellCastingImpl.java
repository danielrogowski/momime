package momime.server.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
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
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.TargetSpellResult;
import momime.server.MomSessionVariables;
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
	
	/** Damage processor */
	private DamageProcessor damageProcessor;
	
	/** City processing methods */
	private CityProcessing cityProcessing;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
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
		
		// List out all the Unit IDs that this spell can summon
		final List<UnitEx> possibleUnits = getServerUnitCalculations ().listUnitsSpellMightSummon (spell, player, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getServerDB ());

		// Pick one at random
		if (possibleUnits.size () > 0)
		{
			final UnitEx summonedUnit = possibleUnits.get (getRandomUtils ().nextInt (possibleUnits.size ()));

			log.debug ("Player " + player.getPlayerDescription ().getPlayerName () + " had " + possibleUnits.size () + " possible units to summon from spell " +
				spell.getSpellID () + ", randomly picked unit ID " + summonedUnit.getUnitID ());

			// Check if the summon location has space for the unit
			final UnitAddLocation addLocation = getUnitServerUtils ().findNearestLocationWhereUnitCanBeAdded
				(summonLocation, summonedUnit.getUnitID (), player.getPlayerDescription ().getPlayerID (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

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

					getFogOfWarMidTurnChanges ().updateUnitStatusToAliveOnServerAndClients (newUnit, addLocation.getUnitLocation (), player, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription (), mom.getServerDB ());
				}
				else
					// For non-heroes, create a new unit
					newUnit = getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (mom.getGeneralServerKnowledge (),
						summonedUnit.getUnitID (), addLocation.getUnitLocation (), null, null, null,
						player, UnitStatusID.ALIVE, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
				
				// Let it move this turn
				newUnit.setDoubleOverlandMovesLeft (2 * getExpandUnitDetails ().expandUnitDetails (newUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()).getMovementSpeed ());
			}

			// Show on new turn messages for the player who summoned it
			if ((sendNewTurnMessage) && (player.getPlayerDescription ().isHuman ()))
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
	 * @param players List of players in the session
	 * @param spells List of known spells
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void sendOverlandCastingInfo (final String ourSpellID, final int onlyOnePlayerID, final List<PlayerServerDetails> players,
		final List<MemoryMaintainedSpell> spells) throws JAXBException, XMLStreamException
	{
		// Don't bother to build the message until we find at least one human player who needs it
		OverlandCastingInfoMessage msg = null;

		for (final PlayerServerDetails sendToPlayer : players)
			if (((onlyOnePlayerID == 0) || (onlyOnePlayerID == sendToPlayer.getPlayerDescription ().getPlayerID ())) &&
				(sendToPlayer.getPlayerDescription ().isHuman ()) &&
				((ourSpellID.equals (CommonDatabaseConstants.SPELL_ID_SPELL_BLAST)) ||
					(getMemoryMaintainedSpellUtils ().findMaintainedSpell (spells, sendToPlayer.getPlayerDescription ().getPlayerID (), ourSpellID, null, null, null, null) != null)))
			{
				// Need to build message or already done?
				if (msg == null)
				{
					msg = new OverlandCastingInfoMessage ();
					msg.setOurSpellID (ourSpellID);
					
					for (final PlayerServerDetails player : players)
					{
						final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
						if ((PlayerKnowledgeUtils.isWizard (pub.getWizardID ())) && (pub.getWizardState () == WizardState.ACTIVE))
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
	 * @param castingPlayer Player who cast the attack spell
	 * @param spell Which attack spell they cast
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them
	 * @param targetLocations Location(s) where the spell is aimed
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void castOverlandAttackSpell (final PlayerServerDetails castingPlayer, final Spell spell, final Integer variableDamage,
		final List<MapCoordinates3DEx> targetLocations, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) castingPlayer.getPersistentPlayerPrivateKnowledge ();
		
		// Break up all the target units into separate lists for each player
		final Map<Integer, List<ResolveAttackTarget>> targetUnitsForEachPlayer = new HashMap<Integer, List<ResolveAttackTarget>> ();
		
		for (final MemoryUnit tu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
			if ((targetLocations.contains (tu.getUnitLocation ())) && (tu.getStatus () == UnitStatusID.ALIVE))
			{
				final ExpandedUnitDetails thisTarget = getExpandUnitDetails ().expandUnitDetails (tu, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
				
				if (getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell (spell, SpellBookSectionID.ATTACK_SPELLS, null,
					castingPlayer.getPlayerDescription ().getPlayerID (), null, null, thisTarget, false, mom.getGeneralServerKnowledge ().getTrueMap (),
					priv.getFogOfWar (), mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
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
		
		for (final Entry<Integer, List<ResolveAttackTarget>> entry : targetUnitsForEachPlayer.entrySet ())
		{
			final PlayerServerDetails defendingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), entry.getKey (), "castOverlandAttackSpell");
			
			getDamageProcessor ().resolveAttack (null, entry.getValue (), castingPlayer, defendingPlayer,
				null, null, null, null, spell, variableDamage, castingPlayer, null, mom);
		}
	}
	
	/**
	 * Rolls when a spell has a certain % chance of destroying each building in a city.  Used for Earthquake and Chaos Rift.
	 * 
	 * @param spellID The spell that is destroying the buildings
	 * @param castingPlayerID Who cast the spell
	 * @param percentageChance The % chance of each building being destroyed
	 * @param targetLocations The city(s) being targeted
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void rollChanceOfEachBuildingBeingDestroyed (final String spellID, final int castingPlayerID, final int percentageChance,
		final List<MapCoordinates3DEx> targetLocations, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		final List<MemoryBuilding> destroyedBuildings = new ArrayList<MemoryBuilding> ();
		for (final MemoryBuilding thisBuilding : mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ())
			if ((targetLocations.contains (thisBuilding.getCityLocation ())) &&
				(!thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS)) &&
				(!thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)) &&
				(getRandomUtils ().nextInt (100) < percentageChance))
				
				destroyedBuildings.add (thisBuilding);

		// Have to do this even if 0 buildings got destroyed, as for Earthquake this is how the client knows to show the animation and clean up the NTM
		getCityProcessing ().destroyBuildings (mom.getGeneralServerKnowledge ().getTrueMap (),
			mom.getPlayers (), destroyedBuildings, spellID, castingPlayerID,
			(targetLocations.size () == 1) ? targetLocations.get (0) : null,
			mom.getSessionDescription (), mom.getServerDB ());
	}
	
	/**
	 * @param targetLocation Tile to corrupt
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void corruptTile (final MapCoordinates3DEx targetLocation, final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final MomSessionDescription sd, final CommonDatabase db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		final OverlandMapTerrainData terrainData = trueMap.getMap ().getPlane ().get
			(targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).getTerrainData ();
		terrainData.setCorrupted (5);
			
		getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (trueMap.getMap (),
			players, targetLocation, sd.getFogOfWarSetting ().getTerrainAndNodeAuras ());
			
		getCityProcessing ().recheckCurrentConstructionIsStillValid (targetLocation,
			trueMap, players, sd, db);
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
}