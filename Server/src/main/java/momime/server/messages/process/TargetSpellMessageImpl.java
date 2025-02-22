package momime.server.messages.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.utils.random.RandomUtils;

import jakarta.xml.bind.JAXBException;
import momime.common.database.AttackSpellTargetID;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.TargetSpellMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KindOfSpell;
import momime.common.utils.KindOfSpellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.SpellTargetingUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.process.SpellCasting;
import momime.server.process.SpellProcessing;

/**
 * Client sends this to specify where they want to cast a spell they've completed casting overland.
 * (combat spells' targets are sent in the original requestCastSpellMessage).
 */
public final class TargetSpellMessageImpl extends TargetSpellMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (TargetSpellMessageImpl.class);

	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Methods that determine whether something is a valid target for a spell */
	private SpellTargetingUtils spellTargetingUtils;
	
	/** Spell utils */
	private SpellUtils spellUtils;

	/** Unit utils */
	private UnitUtils unitUtils;

	/** Random number generator */
	private RandomUtils randomUtils;

	/** Spell processing methods */
	private SpellProcessing spellProcessing;
	
	/** Kind of spell utils */
	private KindOfSpellUtils kindOfSpellUtils;
	
	/** Casting for each type of spell */
	private SpellCasting spellCasting;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
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
		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		// Spell should already exist, but not targetted
		final MemoryMaintainedSpell maintainedSpell = getMemoryMaintainedSpellUtils ().findMaintainedSpell
			(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
				sender.getPlayerDescription ().getPlayerID (), getSpellID (), null, null, null, null);
		
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
		final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), getSpellID ());
		final Spell spell = mom.getServerDB ().findSpell (getSpellID (), "TargetSpellMessageImpl");
		final KindOfSpell kind = getKindOfSpellUtils ().determineKindOfSpell (spell, null);
		
		// Do all the checks
		final String error;
		String citySpellEffectID = null;
		String unitSkillID = null;
		MemoryUnit unit = null;
		ExpandedUnitDetails xu = null;
		MemoryMaintainedSpell targetSpell = null;
		boolean sendInfo = false;
		
		if (maintainedSpell == null)
			error = "Can't find an instance of this spell awaiting targetting";
		
		else if (researchStatus == null)
			error = "Couldn't find the spell you're trying to target in your spell book";
		
		// In some situations its valid to send no target, in which case the client is requesting more info so they can show a list of valid targets
		else if ((getOverlandTargetUnitURN () == null) && (getOverlandTargetSpellURN () == null) &&
			(getOverlandTargetPlayerID () == null) && (getOverlandTargetLocation () == null) &&
			(kind == KindOfSpell.SPELL_BLAST))
		{
			error = null;
			sendInfo = true;
		}
		
		else if ((spell.getSpellBookSectionID () == SpellBookSectionID.CITY_ENCHANTMENTS) ||
			(spell.getSpellBookSectionID () == SpellBookSectionID.CITY_CURSES) || (kind == KindOfSpell.ATTACK_UNITS_AND_BUILDINGS))
		{
			// Find the city we're aiming at
			if (getOverlandTargetUnitURN () != null)
				error = "You chose a unit as the target for a city enchantment or curse";
			
			else if (getOverlandTargetSpellURN () != null)
				error = "You chose a spell as the target for a city enchantment or curse";
			
			else if (getOverlandTargetPlayerID () != null)
				error = "You chose a wizard as the target for a city enchantment or curse";
			
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
				final TargetSpellResult reason = getSpellTargetingUtils ().isCityValidTargetForSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
					spell, sender.getPlayerDescription ().getPlayerID (), (MapCoordinates3DEx) getOverlandTargetLocation (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), priv.getFogOfWar (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), mom.getServerDB ());
				if (reason == TargetSpellResult.VALID_TARGET)
				{
					// Do we need to pick a citySpellEffectID?
					if (kind == KindOfSpell.ATTACK_UNITS_AND_BUILDINGS)
						error = null;
					else
					{
						final List<String> citySpellEffectIDs = getMemoryMaintainedSpellUtils ().listCitySpellEffectsNotYetCastAtLocation
							(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), spell, sender.getPlayerDescription ().getPlayerID (), (MapCoordinates3DEx) getOverlandTargetLocation ());
						if ((spell.getBuildingID () == null) && ((citySpellEffectIDs == null) || (citySpellEffectIDs.size () == 0)))
							error = "City is supposedly a valid target, yet couldn't find any citySpellEffectIDs to use or a building to create";
						else
						{
							if ((citySpellEffectIDs != null) && (citySpellEffectIDs.size () > 0))
							{
								// Did the player request a specific city spell effect? (Spell Ward)
								if (getChosenCitySpellEffectID () != null)
								{
									if (citySpellEffectIDs.contains (getChosenCitySpellEffectID ()))
									{
										error = null;
										citySpellEffectID = getChosenCitySpellEffectID ();
									}
									else
										error = "You requested an invalid city spell effect";
								}
								else
								{
									// Normal behaviour, where we just pick one at random (because really there's only ever going to be 1 to pick from)
									error = null;
									citySpellEffectID = citySpellEffectIDs.get (getRandomUtils ().nextInt (citySpellEffectIDs.size ()));
								}
							}
							else
								// Creates a building, not a city spell effect
								error = null;
						}
					}
				}
				else
					// Using the enum name isn't that great, but the client will already have performed this validation so should never see any message generated here anyway
					error = "This city is not a valid target for this spell for reason " + reason;
			}				
		}
		
		else if ((spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) || (kind == KindOfSpell.ATTACK_UNITS) ||
			(spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_UNIT_SPELLS) || (kind == KindOfSpell.RAISE_DEAD) ||
			(spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES))
		{
			if ((spell.getAttackSpellOverlandTarget () != null) && (spell.getAttackSpellOverlandTarget () == AttackSpellTargetID.ALL_UNITS))
			{
				// Target entire stack
				if (getOverlandTargetUnitURN () != null)
					error = "You chose a unit as the target for a unit stack spell";
				
				else if (getOverlandTargetSpellURN () != null)
					error = "You chose a spell as the target for a unit stack spell";

				else if (getOverlandTargetPlayerID () != null)
					error = "You chose a wizard as the target for a unit stack spell";
				
				else if (getOverlandTargetLocation () == null)
					error = "You didn't provide a target for a unit stack spell";

				else if ((getOverlandTargetLocation ().getX () < 0) || (getOverlandTargetLocation ().getY () < 0) || (getOverlandTargetLocation ().getZ () < 0) ||
					(getOverlandTargetLocation ().getX () >= mom.getSessionDescription ().getOverlandMapSize ().getWidth ()) ||
					(getOverlandTargetLocation ().getY () >= mom.getSessionDescription ().getOverlandMapSize ().getHeight ()) ||
					(getOverlandTargetLocation ().getZ () >= mom.getServerDB ().getPlane ().size ()))
						
					error = "The coordinates you are trying to aim a unit stack spell at are off the edge of the map";
				
				else
				{
					final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
					final List<ExpandedUnitDetails> validUnits = new ArrayList<ExpandedUnitDetails> ();
					
					for (final MemoryUnit mu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
						if ((getOverlandTargetLocation ().equals (mu.getUnitLocation ())) && (mu.getStatus () == UnitStatusID.ALIVE))
						{
							units.add (mu);
							
							final ExpandedUnitDetails thisTarget = getExpandUnitDetails ().expandUnitDetails (mu, null, null, null,
								mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
							
							if (getSpellTargetingUtils ().isUnitValidTargetForSpell (spell, null, null, null,
								sender.getPlayerDescription ().getPlayerID (), null, null, thisTarget, true, mom.getGeneralServerKnowledge ().getTrueMap (),
								priv.getFogOfWar (), mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)												
								
								validUnits.add (thisTarget);
						}
					
					if (units.size () == 0)
						error = "You have no units at this location to target the spell on";
					
					else if (validUnits.size () == 0)
						error = "None of the units here are suitable targets for this spell";
					
					else if (spell.getSpellBookSectionID () != SpellBookSectionID.UNIT_CURSES)
						error = null;
					
					// For curses on a whole stack, assume there's only going to be 1 effect
					else if (spell.getUnitSpellEffect ().size () == 0)
						error = "Casting unit curse on entire stack, but no effects are defined";

					else if (spell.getUnitSpellEffect ().size () > 1)
						error = "Casting unit curse on entire stack, but multiple effects are defined";
					
					else
					{
						error = null;
						unitSkillID = spell.getUnitSpellEffect ().get (0).getUnitSkillID ();
					}
				}
			}
			else
			{
				// Target individual unit
				if (getOverlandTargetLocation () != null)
					error = "You chose a location as the target for a unit spell";

				else if (getOverlandTargetSpellURN () != null)
					error = "You chose a spell as the target for a unit spell";
				
				else if (getOverlandTargetPlayerID () != null)
					error = "You chose a wizard as the target for a unit spell";
				
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
						xu = getExpandUnitDetails ().expandUnitDetails (unit, null, null, spell.getSpellRealm (),
							mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
						
						final TargetSpellResult reason = getSpellTargetingUtils ().isUnitValidTargetForSpell
							(spell, null, null, null, sender.getPlayerDescription ().getPlayerID (), null, null, xu, true,
							mom.getGeneralServerKnowledge ().getTrueMap (), priv.getFogOfWar (), mom.getPlayers (), mom.getServerDB ());
						
						if (reason == TargetSpellResult.VALID_TARGET)
						{
							// If its a unit enchantment, now pick which skill ID we'll actually get
							if (kind == KindOfSpell.UNIT_ENCHANTMENTS)
							{
								final List<UnitSpellEffect> unitSpellEffects = getMemoryMaintainedSpellUtils ().listUnitSpellEffectsNotYetCastOnUnit
									(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), spell, sender.getPlayerDescription ().getPlayerID (), getOverlandTargetUnitURN ());
								if ((unitSpellEffects == null) || (unitSpellEffects.size () == 0))
									error = "Unit is supposedly a valid target, yet couldn't find any unitSkillIDs to use";
								else
								{
									// Yay
									error = null;
									unitSkillID = unitSpellEffects.get (getRandomUtils ().nextInt (unitSpellEffects.size ())).getUnitSkillID ();
								}
							}
							else
								// Special unit spells and Lycanthrophy don't need to pick a skill ID, so they're just OK
								error = null;
						}
						else
							// Using the enum name isn't that great, but the client will already have performed this validation so should never see any message generated here anyway
							error = "This unit is not a valid target for this spell for reason " + reason;
					}
				}
			}
		}

		else if ((spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS) && (spell.getAttackSpellCombatTarget () == null))
		{
			// Find the spell we're aiming at
			if (getOverlandTargetUnitURN () != null)
				error = "You chose a unit as the target for a disjunction spell";

			else if (getOverlandTargetLocation () != null)
				error = "You chose a location as the target for a disjunction spell";
			
			else if (getOverlandTargetPlayerID () != null)
				error = "You chose a wizard as the target for a disjunction spell";
			
			else if (getOverlandTargetSpellURN () == null)
				error = "You didn't provide a target for a disjunction spell";
			
			else
			{
				targetSpell = getMemoryMaintainedSpellUtils ().findSpellURN (getOverlandTargetSpellURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ());
				if (targetSpell == null)
					error = "Could not find the spell you're trying to disjunct";
				
				else
				{
					// Common routine used by both the client and server does the guts of the validation work
					final TargetSpellResult reason = getSpellTargetingUtils ().isSpellValidTargetForSpell
						(sender.getPlayerDescription ().getPlayerID (), targetSpell, mom.getServerDB ());
					if (reason == TargetSpellResult.VALID_TARGET)
						error = null;
					else
						error = "This spell is not a valid target for disjunction for reason " + reason; 
				}
			}
		}
		
		else if ((spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_OVERLAND_SPELLS) ||
			(spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS) || (spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING))
		{
			// Just validate that we got a location
			if (getOverlandTargetUnitURN () != null)
				error = "You chose a unit as the target for a special overland spell";
			
			else if (getOverlandTargetSpellURN () != null)
				error = "You chose a spell as the target for a special overland spell";
			
			else if (getOverlandTargetPlayerID () != null)
				error = "You chose a wizard as the target for a special overland spell";
			
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
				final TargetSpellResult reason = getSpellTargetingUtils ().isOverlandLocationValidTargetForSpell (spell, sender.getPlayerDescription ().getPlayerID (), 
					(MapCoordinates3DEx) getOverlandTargetLocation (), mom.getGeneralServerKnowledge ().getTrueMap (),
					priv.getFogOfWar (), mom.getPlayers (), mom.getServerDB ());
				
				if (reason == TargetSpellResult.VALID_TARGET)
					error = null;
				else
					// Using the enum name isn't that great, but the client will already have performed this validation so should never see any message generated here anyway
					error = "This location is not a valid target for this spell for reason " + reason;
			}
		}
		
		else if (spell.getSpellBookSectionID () == SpellBookSectionID.ENEMY_WIZARD_SPELLS)
		{
			if (getOverlandTargetUnitURN () != null)
				error = "You chose a unit as the target for an enemy wizard spell";
			
			else if (getOverlandTargetSpellURN () != null)
				error = "You chose a spell as the target for an enemy wizard spell";
			
			else if (getOverlandTargetLocation () != null)
				error = "You chose a location as the target for an enemy wizard spell";

			else if (getOverlandTargetPlayerID () == null)
				error = "You didn't choose a wizard as the target for an enemy wizard spell";
			
			else
			{
				// Common routine used by both the client and server does the guts of the validation work
				final PlayerServerDetails targetPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID
					(mom.getPlayers (), getOverlandTargetPlayerID ());
				
				if (targetPlayer == null)
					error = "Could not find the wizard you're trying to target the spell on";
				else
				{
					final TargetSpellResult reason = getSpellTargetingUtils ().isWizardValidTargetForSpell (spell, sender.getPlayerDescription ().getPlayerID (), priv,
						getOverlandTargetPlayerID (), getSpellCasting ().createOverlandCastingInfo (targetPlayer, spell.getSpellID ()));

					if (reason == TargetSpellResult.VALID_TARGET)
						error = null;
					else
						// Using the enum name isn't that great, but the client will already have performed this validation so should never see any message generated here anyway
						error = "This wizard is not a valid target for this spell for reason " + reason;
				}
			}				
		}
		
		else
			error = "Don't know how to target spells from spell book section " + spell.getSpellBookSectionID ();

		// All ok?
		if (error != null)
		{
			// Return error
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		
		else if (sendInfo)
			getSpellCasting ().sendOverlandCastingInfo (spell.getSpellID (), sender.getPlayerDescription ().getPlayerID (), mom);
		
		else
			getSpellProcessing ().targetOverlandSpell (spell, maintainedSpell, getOverlandTargetPlayerID (),
				(MapCoordinates3DEx) getOverlandTargetLocation (), unit, targetSpell, citySpellEffectID, unitSkillID, mom);
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
}