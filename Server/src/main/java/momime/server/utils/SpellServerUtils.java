package momime.server.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.SwitchResearch;
import momime.common.database.v0_9_4.SummonedUnit;
import momime.common.messages.MemoryBuildingUtils;
import momime.common.messages.MemoryMaintainedSpellUtils;
import momime.common.messages.ResourceValueUtils;
import momime.common.messages.SpellUtils;
import momime.common.messages.servertoclient.v0_9_4.RemoveQueuedSpellMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateManaSpentOnCastingCurrentSpellMessage;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.NewTurnMessageData;
import momime.common.messages.v0_9_4.NewTurnMessageTypeID;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.Spell;
import momime.server.database.v0_9_4.Unit;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Server side methods dealing with researching and casting spells
 */
public final class SpellServerUtils
{
	/**
	 * @param player Player who wants to switch research
	 * @param spellID Spell that we want to research
	 * @param switchResearch Switch research option from session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return null if choice is acceptable; message to send back to client if choice isn't acceptable
	 * @throws RecordNotFoundException If either the spell we want to research now, or the spell previously being researched, can't be found
	 */
	public final static String validateResearch (final PlayerServerDetails player, final String spellID,
		final SwitchResearch switchResearch, final ServerDatabaseLookup db, final Logger debugLogger) throws RecordNotFoundException
	{
		debugLogger.entering (SpellServerUtils.class.getName (), "validateResearch", new String [] {player.getPlayerDescription ().getPlayerID ().toString (), spellID});

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Find the spell that we want to research
		final SpellResearchStatus spellWeWantToResearch = SpellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), spellID, debugLogger);

		// Find the spell that was previously being researched
		final Spell spellPreviouslyBeingResearched;
		final SpellResearchStatus spellPreviouslyBeingResearchedStatus;

		if (priv.getSpellIDBeingResearched () == null)
		{
			spellPreviouslyBeingResearched = null;
			spellPreviouslyBeingResearchedStatus = null;
		}
		else
		{
			spellPreviouslyBeingResearched = db.findSpell (priv.getSpellIDBeingResearched (), "validateResearch");
			spellPreviouslyBeingResearchedStatus = SpellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), priv.getSpellIDBeingResearched (), debugLogger);
		}

		// If we can't research it then its obviously disallowed regardless of the status of the previous research
		final String msg;
		if (spellWeWantToResearch.getStatus () != SpellResearchStatusID.RESEARCHABLE_NOW)
			msg = "The spell you've requested is currently not available for you to research.";

		// Picking research when we've got no current research, or switching to what we're already researching is always fine
		else if ((priv.getSpellIDBeingResearched () == null) || (priv.getSpellIDBeingResearched ().equals (spellID)))
			msg = null;

		// Check game option
		else if (switchResearch == SwitchResearch.DISALLOWED)
			msg = "You can't start researching a different spell until you've finished your current research.";

		else if ((spellPreviouslyBeingResearchedStatus.getRemainingResearchCost () < spellPreviouslyBeingResearched.getResearchCost ()) && (switchResearch == SwitchResearch.ONLY_IF_NOT_STARTED))
			msg = "You can't start researching a different spell until you've finished your current research.";

		else
			msg = null;

		debugLogger.exiting (SpellServerUtils.class.getName (), "validateResearch", msg);
		return msg;
	}

	/**
	 * Handles casting an overland spell, i.e. when we've finished channeling sufficient mana in to actually complete the casting
	 *
	 * @param gsk Server knowledge structure
	 * @param player Player who is casting the spell
	 * @param spell Which spell is being cast
	 * @param players List of players in this session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	private static void castOverlandNow (final MomGeneralServerKnowledge gsk, final PlayerServerDetails player, final Spell spell,
		final List<PlayerServerDetails> players, final ServerDatabaseLookup db, final MomSessionDescription sd, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		debugLogger.entering (SpellServerUtils.class.getName (), "castOverlandNow", new String [] {player.getPlayerDescription ().getPlayerID ().toString (), spell.getSpellID ()});

		// Modifying this by section is really only a safeguard to protect against casting spells which we don't have researched yet
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final SpellResearchStatus researchStatus = SpellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), spell.getSpellID (), debugLogger);
		final String sectionID = SpellUtils.getModifiedSectionID (spell, researchStatus, true, debugLogger);

		// Overland enchantments
		if (sectionID.equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_OVERLAND_ENCHANTMENTS))
		{
			// Check if the player already has this overland enchantment cast
			// If they do, they can't have it twice so nothing to do, they just lose the cast
			if (MemoryMaintainedSpellUtils.findMaintainedSpell (gsk.getTrueMap ().getMaintainedSpell (), player.getPlayerDescription ().getPlayerID (), spell.getSpellID (), null, null, null, null, debugLogger) == null)
			{
				// Show message, everybody can see overland enchantment casts
				final NewTurnMessageData overlandMessage = new NewTurnMessageData ();
				overlandMessage.setMsgType (NewTurnMessageTypeID.OVERLAND_ENCHANTMENT);
				overlandMessage.setOtherPlayerID (player.getPlayerDescription ().getPlayerID ());
				overlandMessage.setSpellID (spell.getSpellID ());

				for (final PlayerServerDetails messagePlayer : players)
					if (messagePlayer.getPlayerDescription ().isHuman ())
						((MomTransientPlayerPrivateKnowledge) messagePlayer.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (overlandMessage);

				// Add it on server and anyone who can see it (which, because its an overland enchantment, will be everyone)
				FogOfWarMidTurnChanges.addMaintainedSpellOnServerAndClients (gsk, player.getPlayerDescription ().getPlayerID (), spell.getSpellID (),
					null, null, false, null, null, players, db, sd, debugLogger);
			}
		}

		// Summoning
		else if (sectionID.equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_SUMMONING))
		{
			// Find the location of the wizards' summoning circle 'building'
			final OverlandMapCoordinates summoningCircleLocation = MemoryBuildingUtils.findCityWithBuilding (player.getPlayerDescription ().getPlayerID (),
				CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getBuilding (), debugLogger);

			if (summoningCircleLocation != null)
			{
				// List out all the Unit IDs that this spell can summon
				final List<String> possibleUnitIDs = new ArrayList<String> ();
				for (final SummonedUnit possibleSummonedUnit : spell.getSummonedUnit ())
				{
					// Check whether we can summon this unit If its a hero, this depends on whether we've summoned the hero before, or if he's dead
					final Unit possibleUnit = db.findUnit (possibleSummonedUnit.getSummonedUnitID (), "castOverlandNow");
					final boolean addToList;
					if (possibleUnit.getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
					{
						final MemoryUnit hero = UnitServerUtils.findUnitWithPlayerAndID (gsk.getTrueMap ().getUnit (),
							player.getPlayerDescription ().getPlayerID (), possibleSummonedUnit.getSummonedUnitID (), debugLogger);

						if (hero == null)
							addToList = false;
						else
							addToList = ((hero.getStatus () == UnitStatusID.NOT_GENERATED) || (hero.getStatus () == UnitStatusID.GENERATED));
					}
					else
						addToList = true;

					if (addToList)
						possibleUnitIDs.add (possibleSummonedUnit.getSummonedUnitID ());
				}

				// Pick one at random
				if (possibleUnitIDs.size () > 0)
				{
					final String summonedUnitID = possibleUnitIDs.get (RandomUtils.getGenerator ().nextInt (possibleUnitIDs.size ()));

					debugLogger.finest ("Player " + player.getPlayerDescription ().getPlayerName () + " had " + possibleUnitIDs.size () + " possible units to summon from spell " +
						spell.getSpellID () + ", randomly picked unit ID " + summonedUnitID);

					// Check if the city with the summoning circle has space for the unit
					final UnitAddLocation addLocation = UnitServerUtils.findNearestLocationWhereUnitCanBeAdded
						(summoningCircleLocation, summonedUnitID, player.getPlayerDescription ().getPlayerID (), gsk.getTrueMap (), sd, db, debugLogger);

					final MemoryUnit newUnit;
					if (addLocation.getUnitLocation () == null)
						newUnit = null;
					else
					{
						// Add the unit
						if (db.findUnit (summonedUnitID, "castOverlandNow").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
						{
							// The unit object already exists for heroes
							newUnit = UnitServerUtils.findUnitWithPlayerAndID (gsk.getTrueMap ().getUnit (), player.getPlayerDescription ().getPlayerID (), summonedUnitID, debugLogger);

							if (newUnit.getStatus () == UnitStatusID.NOT_GENERATED)
								UnitServerUtils.generateHeroNameAndRandomSkills (newUnit, db, debugLogger);

							FogOfWarMidTurnChanges.updateUnitStatusToAliveOnServerAndClients (newUnit, addLocation.getUnitLocation (), player, players, gsk.getTrueMap (), sd, db, debugLogger);
						}
						else
							// For non-heroes, create a new unit
							newUnit = FogOfWarMidTurnChanges.addUnitOnServerAndClients (gsk, summonedUnitID, addLocation.getUnitLocation (), summoningCircleLocation,
								null, player, UnitStatusID.ALIVE, players, sd, db, debugLogger);
					}

					// Show on new turn messages for the player who summoned it
					if (player.getPlayerDescription ().isHuman ())
					{
						final NewTurnMessageData summoningSpell = new NewTurnMessageData ();
						summoningSpell.setMsgType (NewTurnMessageTypeID.SUMMONED_UNIT);
						summoningSpell.setSpellID (spell.getSpellID ());
						summoningSpell.setBuildingOrUnitID (summonedUnitID);
						summoningSpell.setLocation (addLocation.getUnitLocation ());
						summoningSpell.setUnitAddBumpType (addLocation.getBumpType ());

						if (newUnit != null)
							summoningSpell.setUnitURN (newUnit.getUnitURN ());

						((MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (summoningSpell);
					}
				}
			}
		}

		// City or Unit enchantments
		else if ((sectionID.equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_CITY_ENCHANTMENTS)) ||
			(sectionID.equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_UNIT_ENCHANTMENTS)) ||
			(sectionID.equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_CITY_CURSES)) ||
			(sectionID.equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_UNIT_CURSES)))
		{
			// Add it on server - note we add it without a target chosen
			final MemoryMaintainedSpell trueSpell = new MemoryMaintainedSpell ();
			trueSpell.setCastingPlayerID (player.getPlayerDescription ().getPlayerID ());
			trueSpell.setSpellID (spell.getSpellID ());
			gsk.getTrueMap ().getMaintainedSpell ().add (trueSpell);

			// Tell client to pick a target for this spell
			final NewTurnMessageData targetSpell = new NewTurnMessageData ();
			targetSpell.setMsgType (NewTurnMessageTypeID.TARGET_SPELL);
			targetSpell.setSpellID (spell.getSpellID ());
			((MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (targetSpell);

			// We don't tell clients about this new maintained spell until the player confirms a target for it, since they might just hit cancel
		}

		else
			throw new MomException ("Completed casting an overland spell with a section ID that there is no code to deal with yet: " + sectionID);

		debugLogger.exiting (SpellServerUtils.class.getName (), "castOverlandNow");
	}

	/**
	 * Spends any skill/mana the player has left towards casting queued spells
	 *
	 * @param gsk Server knowledge structure
	 * @param player Player whose casting to progress
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return True if we cast at least one spell
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public static boolean progressOverlandCasting (final MomGeneralServerKnowledge gsk, final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final ServerDatabaseLookup db, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		debugLogger.entering (SpellServerUtils.class.getName (), "progressOverlandCasting", player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();

		// Keep going while this player has spells queued, free mana and free skill
		boolean anySpellsCast = false;
		int manaRemaining = ResourceValueUtils.findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, debugLogger);
		while ((priv.getQueuedSpellID ().size () > 0) && (trans.getOverlandCastingSkillRemainingThisTurn () > 0) && (manaRemaining > 0))
		{
			// How much to put towards this spell?
			final Spell spell = db.findSpell (priv.getQueuedSpellID ().get (0), "progressOverlandCasting");
			final int reducedCastingCost = SpellUtils.getReducedOverlandCastingCost (spell, pub.getPick (), sd.getSpellSetting (), db, debugLogger);
			final int leftToCast = Math.max (0, reducedCastingCost - priv.getManaSpentOnCastingCurrentSpell ());
			final int manaAmount = Math.min (Math.min (trans.getOverlandCastingSkillRemainingThisTurn (), manaRemaining), leftToCast);

			// Put this amount towards the spell
			trans.setOverlandCastingSkillRemainingThisTurn (trans.getOverlandCastingSkillRemainingThisTurn () - manaAmount);
			priv.setManaSpentOnCastingCurrentSpell (priv.getManaSpentOnCastingCurrentSpell () + manaAmount);
			ResourceValueUtils.addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, -manaAmount, debugLogger);
			manaRemaining = manaRemaining - manaAmount;

			// Finished?
			if (priv.getManaSpentOnCastingCurrentSpell () >= reducedCastingCost)
			{
				// Remove queued spell on server
				priv.getQueuedSpellID ().remove (0);
				priv.setManaSpentOnCastingCurrentSpell (0);

				// Remove queued spell on client
				final RemoveQueuedSpellMessage msg = new RemoveQueuedSpellMessage ();
				msg.setQueuedSpellIndex (0);
				player.getConnection ().sendMessageToClient (msg);

				// Cast it
				castOverlandNow (gsk, player, spell, players, db, sd, debugLogger);
				anySpellsCast = true;
			}

			// Update mana spent so far on client (or set to 0 if finished)
			final UpdateManaSpentOnCastingCurrentSpellMessage msg = new UpdateManaSpentOnCastingCurrentSpellMessage ();
			msg.setManaSpentOnCastingCurrentSpell (priv.getManaSpentOnCastingCurrentSpell ());
			player.getConnection ().sendMessageToClient (msg);

			// No need to tell client how much skill they've got left or mana stored since this is the end of the turn and both will be sent next start phase
		}

		debugLogger.exiting (SpellServerUtils.class.getName (), "progressOverlandCasting", anySpellsCast);
		return anySpellsCast;
	}

	/**
	 * Prevent instantiation
	 */
	private SpellServerUtils ()
	{
	}
}
