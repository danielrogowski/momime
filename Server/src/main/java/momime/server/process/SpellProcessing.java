package momime.server.process;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.SpellHasCombatEffect;
import momime.common.database.v0_9_4.SummonedUnit;
import momime.common.messages.MemoryBuildingUtils;
import momime.common.messages.MemoryCombatAreaEffectUtils;
import momime.common.messages.MemoryMaintainedSpellUtils;
import momime.common.messages.ResourceValueUtils;
import momime.common.messages.SpellUtils;
import momime.common.messages.servertoclient.v0_9_4.RemoveQueuedSpellMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateManaSpentOnCastingCurrentSpellMessage;
import momime.common.messages.v0_9_4.FogOfWarMemory;
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
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.Spell;
import momime.server.database.v0_9_4.Unit;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;
import momime.server.utils.RandomUtils;
import momime.server.utils.UnitAddLocation;
import momime.server.utils.UnitServerUtils;

import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Methods for any significant message processing to do with spells that isn't done in the message implementations
 */
public final class SpellProcessing
{
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
		debugLogger.entering (SpellProcessing.class.getName (), "castOverlandNow", new String [] {player.getPlayerDescription ().getPlayerID ().toString (), spell.getSpellID ()});

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

				// Does this overland enchantment give a global combat area effect? (Not all do)
				if (spell.getSpellHasCombatEffect ().size () > 0)
				{
					// Pick one at random
					final String combatAreaEffectID = spell.getSpellHasCombatEffect ().get (RandomUtils.getGenerator ().nextInt (spell.getSpellHasCombatEffect ().size ())).getCombatAreaEffectID ();
					FogOfWarMidTurnChanges.addCombatAreaEffectOnServerAndClients (gsk, combatAreaEffectID, player.getPlayerDescription ().getPlayerID (), null, players, db, sd, debugLogger);
				}
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

		debugLogger.exiting (SpellProcessing.class.getName (), "castOverlandNow");
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
		debugLogger.entering (SpellProcessing.class.getName (), "progressOverlandCasting", player.getPlayerDescription ().getPlayerID ());

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

		debugLogger.exiting (SpellProcessing.class.getName (), "progressOverlandCasting", anySpellsCast);
		return anySpellsCast;
	}

	/**
	 * The method in the FOW class physically removed spells from the server and players' memory; this method
	 * deals with all the knock on effects of spells being switched off, which isn't really much since spells don't grant money or anything when sold
	 * so this is mostly here for consistency with the building and unit methods
	 *
	 *  Does not recalc global production (which will now be reduced from not having to pay the maintenance of the cancelled spell),
	 *  this has to be done by the calling routine
	 *
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param castingPlayerID Player who cast the spell
	 * @param spellID Which spell it is
	 * @param unitURN Indicates which unit the spell is cast on; null for spells not cast on units
	 * @param unitSkillID If a spell cast on a unit, indicates the specific skill that this spell grants the unit
	 * @param castInCombat Whether this spell was cast in combat or not
	 * @param cityLocation Indicates which city the spell is cast on; null for spells not cast on cities
	 * @param citySpellEffectID If a spell cast on a city, indicates the specific effect that this spell grants the city
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public final static void switchOffSpell (final FogOfWarMemory trueMap,
		final int castingPlayerID, final String spellID, final Integer unitURN, final String unitSkillID,
		final boolean castInCombat, final OverlandMapCoordinates cityLocation, final String citySpellEffectID, final List<PlayerServerDetails> players,
		final ServerDatabaseLookup db, final MomSessionDescription sd, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		debugLogger.entering (SpellProcessing.class.getName (), "switchOffSpell",
			new String [] {new Integer (castingPlayerID).toString (), spellID});

		// Any secondary effects we also need to switch off?
		final PlayerServerDetails player = MultiplayerSessionServerUtils.findPlayerWithID (players, castingPlayerID, "switchOffSpell");
		final Spell spell = db.findSpell (spellID, "switchOffSpell");
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final SpellResearchStatus researchStatus = SpellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), spellID, debugLogger);
		final String sectionID = SpellUtils.getModifiedSectionID (spell, researchStatus, true, debugLogger);

		// Overland enchantments
		if (sectionID.equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_OVERLAND_ENCHANTMENTS))
		{
			// Check each combat area effect that this overland enchantment gives to see if we have any of them in effect - if so cancel them
			for (final SpellHasCombatEffect effect : spell.getSpellHasCombatEffect ())
				if (MemoryCombatAreaEffectUtils.findCombatAreaEffect (trueMap.getCombatAreaEffect (), null, effect.getCombatAreaEffectID (), castingPlayerID, debugLogger))
					FogOfWarMidTurnChanges.removeCombatAreaEffectFromServerAndClients (trueMap, effect.getCombatAreaEffectID (), castingPlayerID, null, players, db, sd, debugLogger);
		}

		// Remove spell itself
		FogOfWarMidTurnChanges.switchOffMaintainedSpellOnServerAndClients (trueMap, castingPlayerID, spellID, unitURN, unitSkillID, castInCombat, cityLocation, citySpellEffectID, players, db, sd, debugLogger);

		debugLogger.exiting (SpellProcessing.class.getName (), "switchOffSpell");
	}

	/**
	 * Prevent instantiation
	 */
	private SpellProcessing ()
	{
	}
}
