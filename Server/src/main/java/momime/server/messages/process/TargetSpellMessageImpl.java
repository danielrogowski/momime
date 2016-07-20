package momime.server.messages.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.random.RandomUtils;

import momime.common.calculations.CityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageConstructBuilding;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.clienttoserver.TargetSpellMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.database.BuildingSvr;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.SpellSvr;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.process.PlayerMessageProcessing;
import momime.server.utils.CityServerUtils;

/**
 * Client sends this to specify where they want to cast a spell they've completed casting overland.
 * (combat spells' targets are sent in the original requestCastSpellMessage).
 */
public final class TargetSpellMessageImpl extends TargetSpellMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (TargetSpellMessageImpl.class);

	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Spell utils */
	private SpellUtils spellUtils;

	/** Unit utils */
	private UnitUtils unitUtils;

	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;

	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnMultiChanges fogOfWarMidTurnMultiChanges;
	
	/** Main FOW update routine */
	private FogOfWarProcessing fogOfWarProcessing;
	
	/** Random number generator */
	private RandomUtils randomUtils;

	/** Server-only city utils */
	private CityServerUtils cityServerUtils;

	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;

	/** Methods for dealing with player msgs */
	private PlayerMessageProcessing playerMessageProcessing;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the client
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the client
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering process: Player ID " + sender.getPlayerDescription ().getPlayerID () + ", " + getSpellID ());
		
		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		// Spell should already exist, but not targetted
		final MemoryMaintainedSpell maintainedSpell = getMemoryMaintainedSpellUtils ().findMaintainedSpell
			(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
				sender.getPlayerDescription ().getPlayerID (), getSpellID (), null, null, null, null);
		
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
		final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), getSpellID ());
		final SpellSvr spell = mom.getServerDB ().findSpell (getSpellID (), "TargetSpellMessageImpl");
		
		// Do all the checks
		final String error;
		String citySpellEffectID = null;
		String unitSkillID = null;
		MemoryUnit unit = null;
		if (maintainedSpell == null)
			error = "Can't find an instance of this spell awaiting targetting";
		
		else if (researchStatus == null)
			error = "Couldn't find the spell you''re trying to target in your spell book";
		
		else
		{
			if ((spell.getSpellBookSectionID () == SpellBookSectionID.CITY_ENCHANTMENTS) ||
				(spell.getSpellBookSectionID () == SpellBookSectionID.CITY_CURSES))
			{
				// Find the city we're aiming at
				if (getOverlandTargetUnitURN () != null)
					error = "You chose a unit as the target for a city enchantment or curse";
				
				else if (getOverlandTargetLocation () == null)
					error = "You didn't provide a target for a city enchantment or curse";
				
				else if ((getOverlandTargetLocation ().getX () < 0) || (getOverlandTargetLocation ().getY () < 0) || (getOverlandTargetLocation ().getZ () < 0) ||
					(getOverlandTargetLocation ().getX () >= mom.getSessionDescription ().getOverlandMapSize ().getWidth ()) ||
					(getOverlandTargetLocation ().getY () >= mom.getSessionDescription ().getOverlandMapSize ().getHeight ()) ||
					(getOverlandTargetLocation ().getZ () >= mom.getServerDB ().getPlanes ().size ()))
					
					error = "The coordinates you are trying to aim a city spell at are off the edge of the map";
				else
				{
					// Common routine used by both the client and server does the guts of the validation work
					final TargetSpellResult reason = getMemoryMaintainedSpellUtils ().isCityValidTargetForSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
						spell, sender.getPlayerDescription ().getPlayerID (), (MapCoordinates3DEx) getOverlandTargetLocation (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), priv.getFogOfWar (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
					if (reason == TargetSpellResult.VALID_TARGET)
					{
						// Looks ok but weird if at this point we can't find a free skill ID
						final List<String> citySpellEffectIDs = getMemoryMaintainedSpellUtils ().listCitySpellEffectsNotYetCastAtLocation
							(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), spell, sender.getPlayerDescription ().getPlayerID (), (MapCoordinates3DEx) getOverlandTargetLocation ());
						if ((spell.getBuildingID () == null) && ((citySpellEffectIDs == null) || (citySpellEffectIDs.size () == 0)))
							error = "City is supposedly a valid target, yet couldn't find any citySpellEffectIDs to use or a building to create";
						else
						{
							// Must be a valid target
							// Choose an effect at random, unless this is a spell that creates a building
							// In future this will need to be made choosable for Spell Ward
							error = null;
							if ((citySpellEffectIDs != null) && (citySpellEffectIDs.size () > 0))
								citySpellEffectID = citySpellEffectIDs.get (getRandomUtils ().nextInt (citySpellEffectIDs.size ()));
						}
					}
					else
						// Using the enum name isn't that great, but the client will already have performed this validation so should never see any message generated here anyway
						error = "This city is not a valid target for this spell for reason " + reason;
				}				
			}
			
			else if ((spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) ||
				(spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_UNIT_SPELLS))
			{
				// Find the unit we're aiming at
				if (getOverlandTargetLocation () != null)
					error = "You chose a location as the target for a unit spell";
				
				else if (getOverlandTargetUnitURN () == null)
					error = "You didn't provide a target for a unit spell";
				
				else
				{
					unit = getUnitUtils ().findUnitURN (getOverlandTargetUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
					if (unit == null)
						error = "Could not find the unit you're trying to target the spell on";
					else
					{
						// Common routine used by both the client and server does the guts of the validation work
						final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (unit, null, null, spell.getSpellRealm (),
							mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
						
						final TargetSpellResult reason = getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell
							(spell, null, sender.getPlayerDescription ().getPlayerID (), null, xu,
							mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
						
						if (reason == TargetSpellResult.VALID_TARGET)
						{
							// If its a unit enchantment, now pick which skill ID we'll actually get
							if (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS)
							{
								final List<String> unitSkillIDs = getMemoryMaintainedSpellUtils ().listUnitSpellEffectsNotYetCastOnUnit
									(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), spell, sender.getPlayerDescription ().getPlayerID (), getOverlandTargetUnitURN ());
								if ((unitSkillIDs == null) || (unitSkillIDs.size () == 0))
									error = "Unit is supposedly a valid target, yet couldn't find any unitSkillIDs to use";
								else
								{
									// Yay
									error = null;
									unitSkillID = unitSkillIDs.get (getRandomUtils ().nextInt (unitSkillIDs.size ()));
								}
							}
							else
								// Special unit spells don't need to pick a skill ID, so they're just OK
								error = null;
						}
						else
							// Using the enum name isn't that great, but the client will already have performed this validation so should never see any message generated here anyway
							error = "This unit is not a valid target for this spell for reason " + reason;
					}
				}
			}
			
			else if (spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_OVERLAND_SPELLS)
			{
				// Just validate that we got a location
				if (getOverlandTargetUnitURN () != null)
					error = "You chose a unit as the target for a special overland spell";
				
				else if (getOverlandTargetLocation () == null)
					error = "You didn't provide a target for a special overland spell";
			
				else if ((getOverlandTargetLocation ().getX () < 0) || (getOverlandTargetLocation ().getY () < 0) || (getOverlandTargetLocation ().getZ () < 0) ||
						(getOverlandTargetLocation ().getX () >= mom.getSessionDescription ().getOverlandMapSize ().getWidth ()) ||
						(getOverlandTargetLocation ().getY () >= mom.getSessionDescription ().getOverlandMapSize ().getHeight ()) ||
						(getOverlandTargetLocation ().getZ () >= mom.getServerDB ().getPlanes ().size ()))
						
						error = "The coordinates you are trying to aim a special overland spell at are off the edge of the map";
				
				else
				{
					// Common routine used by both the client and server does the guts of the validation work
					final TargetSpellResult reason = getMemoryMaintainedSpellUtils ().isOverlandLocationValidTargetForSpell (spell, (MapCoordinates3DEx) getOverlandTargetLocation (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), priv.getFogOfWar (), mom.getServerDB ());
					if (reason == TargetSpellResult.VALID_TARGET)
						error = null;
					else
						// Using the enum name isn't that great, but the client will already have performed this validation so should never see any message generated here anyway
						error = "This city is not a valid target for this spell for reason " + reason;
				}
			}
			
			else
				error = "Don't know how to target spells from this spell book section";
		}

		// All ok?
		if (error != null)
		{
			// Return error
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			if ((spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_UNIT_SPELLS) || (spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_OVERLAND_SPELLS))
			{
				// Transient spell that performs some immediate action, then the temporary untargetted spell on the server gets removed
				// So the spell never does get added to any clients
				// Set values on server - it'll be removed below, but we need to set these to make the visibility checks in sendTransientSpellToClients () work correctly 
				maintainedSpell.setUnitURN (getOverlandTargetUnitURN ());
				maintainedSpell.setCityLocation (getOverlandTargetLocation ());
				maintainedSpell.setUnitSkillID (unitSkillID);
				maintainedSpell.setCitySpellEffectID (citySpellEffectID);
				
				// Just remove it - don't even bother to check if any clients can see it
				getMemoryMaintainedSpellUtils ().removeSpellURN (maintainedSpell.getSpellURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ());
				
				// Tell the client to stop asking about targetting the spell, and show an animation for it - need to send this to all players that can see it!
				getFogOfWarMidTurnChanges ().sendTransientSpellToClients (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), maintainedSpell, mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());

				if (spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_UNIT_SPELLS)
				{
					// Recall spells - first we need the location of the wizards' summoning circle 'building' to know where we're recalling them to
					final MemoryBuilding summoningCircleLocation = getMemoryBuildingUtils ().findCityWithBuilding (sender.getPlayerDescription ().getPlayerID (),
						CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
					
					if (summoningCircleLocation != null)
					{
						final List<MemoryUnit> targetUnits = new ArrayList<MemoryUnit> ();
						targetUnits.add (unit);
						
						getFogOfWarMidTurnMultiChanges ().moveUnitStackOneCellOnServerAndClients (targetUnits, sender, (MapCoordinates3DEx) unit.getUnitLocation (),
							(MapCoordinates3DEx) summoningCircleLocation.getCityLocation (),
							mom.getPlayers (), mom.getGeneralServerKnowledge (), mom.getSessionDescription (), mom.getServerDB ());
					}
				}
				
				else if (spell.getSpellScoutingRange () == null)
				{
					// Corruption
					final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(getOverlandTargetLocation ().getZ ()).getRow ().get (getOverlandTargetLocation ().getY ()).getCell ().get (getOverlandTargetLocation ().getX ()).getTerrainData ();
					terrainData.setCorrupted (5);
					
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getPlayers (), (MapCoordinates3DEx) getOverlandTargetLocation (), mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
					
					// Is the corrupted tile within range of a city?
					final MapCoordinates3DEx cityLocation = getCityServerUtils ().findCityWithinRadius ((MapCoordinates3DEx) getOverlandTargetLocation (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getSessionDescription ().getOverlandMapSize ());
					if (cityLocation != null)
					{
						// City probably isn't owned by the person who cast the spell
						final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
							(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
						if (cityData.getCurrentlyConstructingBuildingID () != null)
						{
							final BuildingSvr buildingDef = mom.getServerDB ().findBuilding (cityData.getCurrentlyConstructingBuildingID (), "targetCorruption");
							if (!getCityCalculations ().buildingPassesTileTypeRequirements (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), cityLocation,
								buildingDef, mom.getSessionDescription ().getOverlandMapSize ()))
							{
								// City can no longer proceed with their current construction project
								cityData.setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);
								getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
									mom.getPlayers (), cityLocation, mom.getSessionDescription ().getFogOfWarSetting ());

								// If it is a human player then we need to let them know that this has happened
								final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), cityData.getCityOwnerID (), "targetCorruption");
								if (cityOwner.getPlayerDescription ().isHuman ())
								{
									final NewTurnMessageConstructBuilding abortConstruction = new NewTurnMessageConstructBuilding ();
									abortConstruction.setMsgType (NewTurnMessageTypeID.ABORT_BUILDING);
									abortConstruction.setBuildingID (buildingDef.getBuildingID ());
									abortConstruction.setCityLocation (cityLocation);
									((MomTransientPlayerPrivateKnowledge) cityOwner.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (abortConstruction);
									
									getPlayerMessageProcessing ().sendNewTurnMessages (mom.getGeneralPublicKnowledge (), mom.getPlayers (), null);
								}
							}
						}
					}
				}
				
				else
				{
					// Earth lore
					getFogOfWarProcessing ().canSeeRadius (priv.getFogOfWar (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getSessionDescription ().getOverlandMapSize (), getOverlandTargetLocation ().getX (), getOverlandTargetLocation ().getY (),
						getOverlandTargetLocation ().getZ (), spell.getSpellScoutingRange ());
					
					getFogOfWarProcessing ().updateAndSendFogOfWar (mom.getGeneralServerKnowledge ().getTrueMap (), sender, mom.getPlayers (),
						"earthLore", mom.getSessionDescription (), mom.getServerDB ());
				}
			}
			else if (spell.getBuildingID () == null)
			{
				// Enchantment or curse spell that generates some city or unit effect
				// Set values on server
				maintainedSpell.setUnitURN (getOverlandTargetUnitURN ());
				maintainedSpell.setCityLocation (getOverlandTargetLocation ());
				maintainedSpell.setUnitSkillID (unitSkillID);
				maintainedSpell.setCitySpellEffectID (citySpellEffectID);
				
				// Add spell on clients (they don't have a blank version of it before now)
				getFogOfWarMidTurnChanges ().addExistingTrueMaintainedSpellToClients (mom.getGeneralServerKnowledge (), maintainedSpell,
					mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
				
				// If its a unit enchantment, does it grant any secondary permanent effects? (Black Channels making units Undead)
				if (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS)
					for (final UnitSpellEffect effect : spell.getUnitSpellEffect ())
						if ((effect.isPermanent () != null) && (effect.isPermanent ()) &&
							(getUnitUtils ().getBasicSkillValue (unit.getUnitHasSkill (), effect.getUnitSkillID ()) < 0))
						{
							final UnitSkillAndValue permanentEffect = new UnitSkillAndValue ();
							permanentEffect.setUnitSkillID (effect.getUnitSkillID ());
							unit.getUnitHasSkill ().add (permanentEffect);
							
							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (unit, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
								mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
						}
			}
			else
			{
				// Spell that creates a building instead of an effect, like "Wall of Stone" or "Move Fortress"
				// Is it a type of building where we only ever have one of them, and need to remove the existing one?
				String secondBuildingID = null;
				if ((spell.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)) ||
					(spell.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS)))
				{
					// Find & remove the main building for this spell
					final MemoryBuilding destroyBuildingLocation = getMemoryBuildingUtils ().findCityWithBuilding
						(sender.getPlayerDescription ().getPlayerID (), spell.getBuildingID (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
							mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
					
					if (destroyBuildingLocation != null)
					{
						getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (),
							mom.getPlayers (), destroyBuildingLocation.getBuildingURN (), false, mom.getSessionDescription (), mom.getServerDB ());
						
						// Move summoning circle as well if its in the same place as the wizard's fortress
						if (spell.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS))
						{
							final MemoryBuilding summoningCircleLocation = getMemoryBuildingUtils ().findCityWithBuilding
								(sender.getPlayerDescription ().getPlayerID (), CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE,
									mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
							
							if (summoningCircleLocation != null)
							{
								getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (),
									mom.getPlayers (), summoningCircleLocation.getBuildingURN (),
									false, mom.getSessionDescription (), mom.getServerDB ());
								
								secondBuildingID = CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE;
							}
						}
					}
				}

				// Is the building that the spell is adding the same as what was being constructed?  If so then reset construction.
				// (Casting Wall of Stone in a city that's building City Walls).
				final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(getOverlandTargetLocation ().getZ ()).getRow ().get (getOverlandTargetLocation ().getY ()).getCell ().get (getOverlandTargetLocation ().getX ()).getCityData ();
							
				if ((cityData != null) && (spell.getBuildingID ().equals (cityData.getCurrentlyConstructingBuildingID ())))
				{
					cityData.setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getPlayers (), (MapCoordinates3DEx) getOverlandTargetLocation (), mom.getSessionDescription ().getFogOfWarSetting ());
				}
				
				// First create the building(s) on the server
				getFogOfWarMidTurnChanges ().addBuildingOnServerAndClients (mom.getGeneralServerKnowledge (),
					mom.getPlayers (), (MapCoordinates3DEx) getOverlandTargetLocation (), spell.getBuildingID (), secondBuildingID, getSpellID (), sender.getPlayerDescription ().getPlayerID (),
					mom.getSessionDescription (), mom.getServerDB ());
				
				// Remove the maintained spell on the server (clients would never have gotten it to begin with)
				mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().remove (maintainedSpell);
			}
			
			// New spell will probably use up some mana maintenance
			getServerResourceCalculations ().recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom);
		}
		
		log.trace ("Exiting process");
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
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnMultiChanges getFogOfWarMidTurnMultiChanges ()
	{
		return fogOfWarMidTurnMultiChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnMultiChanges (final FogOfWarMidTurnMultiChanges obj)
	{
		fogOfWarMidTurnMultiChanges = obj;
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
	 * @return Server-only city utils
	 */
	public final CityServerUtils getCityServerUtils ()
	{
		return cityServerUtils;
	}

	/**
	 * @param utils Server-only city utils
	 */
	public final void setCityServerUtils (final CityServerUtils utils)
	{
		cityServerUtils = utils;
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