package momime.server.messages.process;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.clienttoserver.v0_9_4.TargetSpellMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.utils.TargetUnitSpellResult;
import momime.server.MomSessionVariables;
import momime.server.database.v0_9_4.Spell;

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
	private final Logger log = Logger.getLogger (TargetSpellMessageImpl.class.getName ());

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
		log.entering (TargetSpellMessageImpl.class.getName (), "process",
			new String [] {sender.getPlayerDescription ().getPlayerID ().toString (), getSpellID ()});
		
		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		// Spell should already exist, but not targetted
		final MemoryMaintainedSpell maintainedSpell = mom.getMemoryMaintainedSpellUtils ().findMaintainedSpell
			(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
				sender.getPlayerDescription ().getPlayerID (), getSpellID (), null, null, null, null);
		
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
		final SpellResearchStatus researchStatus = mom.getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), getSpellID ());
		final Spell spell = mom.getServerDB ().findSpell (getSpellID (), "TargetSpellMessageImpl");
		
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
			if ((spell.getSpellBookSectionID ().equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_CITY_ENCHANTMENTS)) ||
				(spell.getSpellBookSectionID ().equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_CITY_CURSES)))
			{
				// Find the city we're aiming at
				if (getUnitURN () != null)
					error = "You chose a unit as the target for a city enchantment";
				
				else if (getCityLocation () == null)
					error = "You didn't provide a target for a city enchantment";
				
				else if ((getCityLocation ().getX () < 0) || (getCityLocation ().getY () < 0) || (getCityLocation ().getPlane () < 0) ||
					(getCityLocation ().getX () >= mom.getSessionDescription ().getMapSize ().getWidth ()) ||
					(getCityLocation ().getY () >= mom.getSessionDescription ().getMapSize ().getHeight ()) ||
					(getCityLocation ().getPlane () >= mom.getServerDB ().getPlane ().size ()))
					
					error = "The coordinates you are trying to aim a city spell at are off the edge of the map";
				else
				{
					// Get the city we're casting the spell on
					final OverlandMapCityData city = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(getCityLocation ().getPlane ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();
					
					// Get the list of possible citySpellEffectIDs that this spell might cast
					final List<String> citySpellEffectIDs = mom.getMemoryMaintainedSpellUtils ().listCitySpellEffectsNotYetCastAtLocation
						(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), spell, sender.getPlayerDescription ().getPlayerID (), (OverlandMapCoordinatesEx) getCityLocation ());
					
					if ((spell.getBuildingID () == null) && (citySpellEffectIDs == null))
						error = "Spell does not specify any city effects or a building ID so code doesn't know what to do with it";
					
					else if (city == null)
						error = "There is no city at this location";
					
					else if ((city.getCityPopulation () == null) || (city.getCityPopulation () <= 0))
						error = "The city you are trying to cast a spell on has been razed and no longer exists";
					
					else if ((spell.getSpellBookSectionID ().equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_CITY_ENCHANTMENTS)) &&
						(city.getCityOwnerID () != sender.getPlayerDescription ().getPlayerID ()))
						error = "This spell can only be targetted at cities that you own";
					
					else if ((spell.getSpellBookSectionID ().equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_CITY_CURSES)) &&
						(city.getCityOwnerID () == sender.getPlayerDescription ().getPlayerID ()))
						error = "This spell can only be targetted at enemy cities";
					
					else if ((citySpellEffectIDs != null) && (citySpellEffectIDs.size () == 0))
						error = "This city already has this enchantment cast on it";
					
					else if ((citySpellEffectIDs == null) && (mom.getMemoryBuildingUtils ().findBuilding
						(mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), (OverlandMapCoordinatesEx) getCityLocation (), spell.getBuildingID ())))
						error = "This city already has the type of building that this spell creates";
					
					else
					{
						// Must be a valid target
						// Choose an effect at random, unless this is a spell that creates a building
						// In future this will need to be made choosable for Spell Ward
						error = null;
						if (citySpellEffectIDs != null)
							citySpellEffectID = citySpellEffectIDs.get (getRandomUtils ().nextInt (citySpellEffectIDs.size ()));
					}					
				}				
			}
			
			else if (spell.getSpellBookSectionID ().equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_UNIT_ENCHANTMENTS))
			{
				// Find the unit we're aiming at
				final MemoryUnit unit = mom.getUnitUtils ().findUnitURN (getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
				if (unit == null)
					error = "Could not find the unit you're trying to target the spell on";
				else
				{
					// Common routine used by both the client and server does the guts of the validation work
					final TargetUnitSpellResult reason = mom.getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
						spell, sender.getPlayerDescription ().getPlayerID (), unit, mom.getServerDB ());
					if (reason == TargetUnitSpellResult.VALID_TARGET)
					{
						// Looks ok but weird if at this point we can't find a free skill ID
						final List<String> unitSkillIDs = mom.getMemoryMaintainedSpellUtils ().listUnitSpellEffectsNotYetCastOnUnit
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
			log.warning (TargetSpellMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

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
				mom.getFogOfWarMidTurnChanges ().addExistingTrueMaintainedSpellToClients (maintainedSpell,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), null, null, null, mom.getServerDB (), mom.getSessionDescription ());
			}
			else
			{
				// Spell that creates a building instead of an effect, like "Wall of Stone" or "Move Fortress"
				// Is it a type of building where we only ever have one of them, and need to remove the existing one?
				String secondBuildingID = null;
				if ((spell.getBuildingID ().equals (CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE)) ||
					(spell.getBuildingID ().equals (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS)))
				{
					// Find & remove the main building for this spell
					final OverlandMapCoordinatesEx destroyBuildingLocation = mom.getMemoryBuildingUtils ().findCityWithBuilding
						(sender.getPlayerDescription ().getPlayerID (), spell.getBuildingID (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
							mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
					
					if (destroyBuildingLocation != null)
					{
						mom.getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (),
							mom.getPlayers (), destroyBuildingLocation, spell.getBuildingID (), false, mom.getSessionDescription (), mom.getServerDB ());
						
						// Move summoning circle as well if its in the same place as the wizard's fortress
						if (spell.getBuildingID ().equals (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS))
						{
							final OverlandMapCoordinatesEx summoningCircleLocation = mom.getMemoryBuildingUtils ().findCityWithBuilding
								(sender.getPlayerDescription ().getPlayerID (), CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE,
									mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
							
							if (summoningCircleLocation != null)
							{
								mom.getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (),
									mom.getPlayers (), summoningCircleLocation, CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE,
									false, mom.getSessionDescription (), mom.getServerDB ());
								
								secondBuildingID = CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE;
							}
						}
					}
				}
				
				// First create the building(s) on the server
				mom.getFogOfWarMidTurnChanges ().addBuildingOnServerAndClients (mom.getGeneralServerKnowledge (),
					mom.getPlayers (), (OverlandMapCoordinatesEx) getCityLocation (), spell.getBuildingID (), secondBuildingID, getSpellID (), sender.getPlayerDescription ().getPlayerID (),
					mom.getSessionDescription (), mom.getServerDB ());
				
				// Remove the maintained spell on the server (clients would never have gotten it to begin with)
				mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().remove (maintainedSpell);
			}
			
			// New spell will probably use up some mana maintenance
			mom.getServerResourceCalculations ().recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom);
		}
		
		log.exiting (TargetSpellMessageImpl.class.getName (), "process");
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
