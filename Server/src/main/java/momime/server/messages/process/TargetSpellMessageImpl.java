package momime.server.messages.process;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.clienttoserver.TargetSpellMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.SpellSvr;
import momime.server.fogofwar.FogOfWarMidTurnChanges;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.random.RandomUtils;

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
	
	/** Random number generator */
	private RandomUtils randomUtils;

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
				if (getUnitURN () != null)
					error = "You chose a unit as the target for a city enchantment";
				
				else if (getCityLocation () == null)
					error = "You didn't provide a target for a city enchantment";
				
				else if ((getCityLocation ().getX () < 0) || (getCityLocation ().getY () < 0) || (getCityLocation ().getZ () < 0) ||
					(getCityLocation ().getX () >= mom.getSessionDescription ().getMapSize ().getWidth ()) ||
					(getCityLocation ().getY () >= mom.getSessionDescription ().getMapSize ().getHeight ()) ||
					(getCityLocation ().getZ () >= mom.getServerDB ().getPlanes ().size ()))
					
					error = "The coordinates you are trying to aim a city spell at are off the edge of the map";
				else
				{
					// Common routine used by both the client and server does the guts of the validation work
					final TargetSpellResult reason = getMemoryMaintainedSpellUtils ().isCityValidTargetForSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
						spell, sender.getPlayerDescription ().getPlayerID (), (MapCoordinates3DEx) getCityLocation (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), mom.getServerDB ());
					if (reason == TargetSpellResult.VALID_TARGET)
					{
						// Looks ok but weird if at this point we can't find a free skill ID
						final List<String> citySpellEffectIDs = getMemoryMaintainedSpellUtils ().listCitySpellEffectsNotYetCastAtLocation
							(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), spell, sender.getPlayerDescription ().getPlayerID (), (MapCoordinates3DEx) getCityLocation ());
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
			
			else if (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS)
			{
				// Find the unit we're aiming at
				final MemoryUnit unit = getUnitUtils ().findUnitURN (getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
				if (unit == null)
					error = "Could not find the unit you're trying to target the spell on";
				else
				{
					// Common routine used by both the client and server does the guts of the validation work
					final TargetSpellResult reason = getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell
						(spell, null, sender.getPlayerDescription ().getPlayerID (), null, unit,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
					
					if (reason == TargetSpellResult.VALID_TARGET)
					{
						// Looks ok but weird if at this point we can't find a free skill ID
						final List<String> unitSkillIDs = getMemoryMaintainedSpellUtils ().listUnitSpellEffectsNotYetCastOnUnit
							(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), spell, sender.getPlayerDescription ().getPlayerID (), getUnitURN ());
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
						// Using the enum name isn't that great, but the client will already have performed this validation so should never see any message generated here anyway
						error = "This unit is not a valid target for this spell for reason " + reason;
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
			// Create a building or a spell?
			if (spell.getBuildingID () == null)
			{
				// Normal spell that generates some city or unit effect
				// Set values on server
				maintainedSpell.setUnitURN (getUnitURN ());
				maintainedSpell.setCityLocation (getCityLocation ());
				maintainedSpell.setUnitSkillID (unitSkillID);
				maintainedSpell.setCitySpellEffectID (citySpellEffectID);
				
				// Add spell on clients (they don't have a blank version of it before now)
				getFogOfWarMidTurnChanges ().addExistingTrueMaintainedSpellToClients (mom.getGeneralServerKnowledge (), maintainedSpell,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB (), mom.getSessionDescription ());
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
					(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
							
				if ((cityData != null) && (spell.getBuildingID ().equals (cityData.getCurrentlyConstructingBuildingID ())))
				{
					cityData.setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getPlayers (), (MapCoordinates3DEx) cityLocation, mom.getSessionDescription ().getFogOfWarSetting (), false);
				}
				
				// First create the building(s) on the server
				getFogOfWarMidTurnChanges ().addBuildingOnServerAndClients (mom.getGeneralServerKnowledge (),
					mom.getPlayers (), (MapCoordinates3DEx) getCityLocation (), spell.getBuildingID (), secondBuildingID, getSpellID (), sender.getPlayerDescription ().getPlayerID (),
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
}