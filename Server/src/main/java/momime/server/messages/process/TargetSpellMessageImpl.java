package momime.server.messages.process;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.random.RandomUtils;

import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.clienttoserver.TargetSpellMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.process.SpellProcessing;

/**
 * Client sends this to specify where they want to cast a spell they've completed casting overland.
 * (combat spells' targets are sent in the original requestCastSpellMessage).
 */
public final class TargetSpellMessageImpl extends TargetSpellMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (TargetSpellMessageImpl.class);

	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Spell utils */
	private SpellUtils spellUtils;

	/** Unit utils */
	private UnitUtils unitUtils;

	/** Random number generator */
	private RandomUtils randomUtils;

	/** Spell processing methods */
	private SpellProcessing spellProcessing;
	
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
		final Spell spell = mom.getServerDB ().findSpell (getSpellID (), "TargetSpellMessageImpl");
		
		// Do all the checks
		final String error;
		String citySpellEffectID = null;
		String unitSkillID = null;
		MemoryUnit unit = null;
		ExpandedUnitDetails xu = null;
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
					(getOverlandTargetLocation ().getZ () >= mom.getServerDB ().getPlane ().size ()))
					
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
						xu = getUnitUtils ().expandUnitDetails (unit, null, null, spell.getSpellRealm (),
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
						(getOverlandTargetLocation ().getZ () >= mom.getServerDB ().getPlane ().size ()))
						
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
			getSpellProcessing ().targetOverlandSpell (spell, maintainedSpell, (MapCoordinates3DEx) getOverlandTargetLocation (), unit, citySpellEffectID, unitSkillID, mom);
		
		log.trace ("Exiting process");
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
}