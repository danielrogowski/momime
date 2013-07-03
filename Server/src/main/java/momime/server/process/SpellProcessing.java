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
import momime.common.messages.CombatMapCoordinatesEx;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.servertoclient.v0_9_4.OverlandCastQueuedMessage;
import momime.common.messages.servertoclient.v0_9_4.RemoveQueuedSpellMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
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
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.common.player.MomSpellCastType;
import momime.common.utils.IMemoryBuildingUtils;
import momime.common.utils.IMemoryCombatAreaEffectUtils;
import momime.common.utils.IMemoryMaintainedSpellUtils;
import momime.common.utils.IResourceValueUtils;
import momime.common.utils.ISpellUtils;
import momime.server.IMomSessionVariables;
import momime.server.calculations.IMomServerResourceCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Spell;
import momime.server.database.v0_9_4.Unit;
import momime.server.fogofwar.IFogOfWarMidTurnChanges;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;
import momime.server.utils.IUnitServerUtils;
import momime.server.utils.RandomUtils;
import momime.server.utils.UnitAddLocation;

import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Methods for any significant message processing to do with spells that isn't done in the message implementations
 */
public final class SpellProcessing implements ISpellProcessing
{
	/** Class logger */
	private final Logger log = Logger.getLogger (SpellProcessing.class.getName ());

	/** Spell utils */
	private ISpellUtils spellUtils;
	
	/** Memory building utils */
	private IMemoryBuildingUtils memoryBuildingUtils;
	
	/** MemoryMaintainedSpell utils */
	private IMemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Memory CAE utils */
	private IMemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** Resource value utils */
	private IResourceValueUtils resourceValueUtils;
	
	/** Server-only unit utils */
	private IUnitServerUtils unitServerUtils;
	
	/** Methods for updating true map + players' memory */
	private IFogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Methods for dealing with player msgs */
	private IPlayerMessageProcessing playerMessageProcessing;
	
	/** Resource calculations */
	private IMomServerResourceCalculations serverResourceCalculations;
	
	/**
	 * Handles casting an overland spell, i.e. when we've finished channeling sufficient mana in to actually complete the casting
	 *
	 * @param gsk Server knowledge structure
	 * @param player Player who is casting the spell
	 * @param spell Which spell is being cast
	 * @param players List of players in this session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	void castOverlandNow (final MomGeneralServerKnowledge gsk, final PlayerServerDetails player, final Spell spell,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.entering (SpellProcessing.class.getName (), "castOverlandNow", new String [] {player.getPlayerDescription ().getPlayerID ().toString (), spell.getSpellID ()});

		// Modifying this by section is really only a safeguard to protect against casting spells which we don't have researched yet
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spell.getSpellID ());
		final String sectionID = getSpellUtils ().getModifiedSectionID (spell, researchStatus, true);

		// Overland enchantments
		if (sectionID.equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_OVERLAND_ENCHANTMENTS))
		{
			// Check if the player already has this overland enchantment cast
			// If they do, they can't have it twice so nothing to do, they just lose the cast
			if (getMemoryMaintainedSpellUtils ().findMaintainedSpell (gsk.getTrueMap ().getMaintainedSpell (), player.getPlayerDescription ().getPlayerID (), spell.getSpellID (), null, null, null, null) == null)
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
				getFogOfWarMidTurnChanges ().addMaintainedSpellOnServerAndClients (gsk, player.getPlayerDescription ().getPlayerID (), spell.getSpellID (),
					null, null, false, null, null, players, null, null, null, db, sd);

				// Does this overland enchantment give a global combat area effect? (Not all do)
				if (spell.getSpellHasCombatEffect ().size () > 0)
				{
					// Pick one at random
					final String combatAreaEffectID = spell.getSpellHasCombatEffect ().get (RandomUtils.getGenerator ().nextInt (spell.getSpellHasCombatEffect ().size ())).getCombatAreaEffectID ();
					getFogOfWarMidTurnChanges ().addCombatAreaEffectOnServerAndClients (gsk, combatAreaEffectID, player.getPlayerDescription ().getPlayerID (), null, players, db, sd);
				}
			}
		}

		// Summoning
		else if (sectionID.equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_SUMMONING))
		{
			// Find the location of the wizards' summoning circle 'building'
			final OverlandMapCoordinatesEx summoningCircleLocation = getMemoryBuildingUtils ().findCityWithBuilding (player.getPlayerDescription ().getPlayerID (),
				CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getBuilding ());

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
						final MemoryUnit hero = getUnitServerUtils ().findUnitWithPlayerAndID (gsk.getTrueMap ().getUnit (),
							player.getPlayerDescription ().getPlayerID (), possibleSummonedUnit.getSummonedUnitID ());

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

					log.finest ("Player " + player.getPlayerDescription ().getPlayerName () + " had " + possibleUnitIDs.size () + " possible units to summon from spell " +
						spell.getSpellID () + ", randomly picked unit ID " + summonedUnitID);

					// Check if the city with the summoning circle has space for the unit
					final UnitAddLocation addLocation = getUnitServerUtils ().findNearestLocationWhereUnitCanBeAdded
						(summoningCircleLocation, summonedUnitID, player.getPlayerDescription ().getPlayerID (), gsk.getTrueMap (), sd, db);

					final MemoryUnit newUnit;
					if (addLocation.getUnitLocation () == null)
						newUnit = null;
					else
					{
						// Add the unit
						if (db.findUnit (summonedUnitID, "castOverlandNow").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
						{
							// The unit object already exists for heroes
							newUnit = getUnitServerUtils ().findUnitWithPlayerAndID (gsk.getTrueMap ().getUnit (), player.getPlayerDescription ().getPlayerID (), summonedUnitID);

							if (newUnit.getStatus () == UnitStatusID.NOT_GENERATED)
								getUnitServerUtils ().generateHeroNameAndRandomSkills (newUnit, db);

							getFogOfWarMidTurnChanges ().updateUnitStatusToAliveOnServerAndClients (newUnit, addLocation.getUnitLocation (), player, players, gsk.getTrueMap (), sd, db);
						}
						else
							// For non-heroes, create a new unit
							newUnit = getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (gsk, summonedUnitID, addLocation.getUnitLocation (), summoningCircleLocation,
								null, player, UnitStatusID.ALIVE, players, sd, db);
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

		log.exiting (SpellProcessing.class.getName (), "castOverlandNow");
	}
	
	/**
	 * Client wants to cast a spell, either overland or in combat
	 * We may not actually be able to cast it yet - big overland spells take a number of turns to channel, so this
	 * routine does all the checks to see if it can be instantly cast or needs to be queued up and cast over multiple turns
	 * 
	 * @param player Player who is casting the spell
	 * @param spellID Which spell they want to cast
	 * @param combatLocation Location of the combat where this spell is being cast; null = being cast overland
	 * @param combatTargetLocation Which specific tile of the combat map the spell is being cast at, for cell-targetted spells like combat summons
	 * @param combatTargetUnitURN Which specific unit within combat the spell is being cast at, for unit-targetted spells like Fire Bolt
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws RecordNotFoundException If we find the spell they're trying to cast, or other expected game elements
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Override
	public final void requestCastSpell (final PlayerServerDetails player, final String spellID,
		final OverlandMapCoordinatesEx combatLocation, final CombatMapCoordinatesEx combatTargetLocation, final Integer combatTargetUnitURN,
		final IMomSessionVariables mom)
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException
	{
		log.entering (SpellProcessing.class.getName (), "requestCastSpell", new String [] {player.getPlayerDescription ().getPlayerID ().toString (), spellID});
		
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
		
		// Find the spell in the player's search list
		final Spell spell = mom.getServerDB ().findSpell (spellID, "requestCastSpell");
		final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spellID);
		
		// Do validation checks
		final String msg;
		if (researchStatus.getStatus () != SpellResearchStatusID.AVAILABLE)
			msg = "You don't have that spell researched and/or available so can't cast it.";
		
		else if ((combatLocation == null) && (!getSpellUtils ().spellCanBeCastIn (spell, MomSpellCastType.OVERLAND)))
			msg = "That spell cannot be cast overland.";
		
		else if ((combatLocation != null) && (!getSpellUtils ().spellCanBeCastIn (spell, MomSpellCastType.COMBAT)))
			msg = "That spell cannot be cast in combat.";

		else if ((combatLocation == null) && (combatTargetUnitURN != null))
			msg = "Cannot specify a unit target when casting an overland spell.";

		else if ((combatLocation != null) && (combatTargetUnitURN == null) &&
			((spell.getSpellBookSectionID ().equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_UNIT_ENCHANTMENTS)) ||
			 (spell.getSpellBookSectionID ().equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_UNIT_CURSES))))
			msg = "You must specify a unit target when casting unit enchantments or curses in combat.";

		else if ((combatLocation != null) && (combatTargetLocation == null) &&
				(spell.getSpellBookSectionID ().equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_SUMMONING)))
				msg = "You must specify a target location when casting summoning spells in combat.";
		
		else
			msg = null;
		
		if ((msg == null) && (combatLocation != null))
			throw new UnsupportedOperationException ("requestCastSpell is missing a lot validation regarding casting spells in combat");
		
		// Ok to go ahead and cast (or queue) it?
		if (msg != null)
		{
			log.warning (player.getPlayerDescription ().getPlayerName () + " disallowed from casting spell " + spellID + ": " + msg);
			
			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (msg);
			player.getConnection ().sendMessageToClient (reply);
		}
		else if (combatLocation != null)
		{
			// Cast combat spell
			throw new UnsupportedOperationException ("requestCastSpell is missing code for casting combat spells");
		}
		else
		{
			// Overland spell - need to see if we can instant cast it or need to queue it up
			final int reducedCastingCost = getSpellUtils ().getReducedOverlandCastingCost (spell, pub.getPick (),
			mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ());
			
			if ((priv.getQueuedSpellID ().size () == 0) && (Math.min (trans.getOverlandCastingSkillRemainingThisTurn (),
				getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (),
					CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA)) >= reducedCastingCost))
			{
				// Cast instantly, and show the casting message instantly too
				castOverlandNow (mom.getGeneralServerKnowledge (), player, spell, mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
				getPlayerMessageProcessing ().sendNewTurnMessages (null, mom.getPlayers (), null);
				
				// Charge player the skill/mana
				trans.setOverlandCastingSkillRemainingThisTurn (trans.getOverlandCastingSkillRemainingThisTurn () - reducedCastingCost);
				getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA,
					-reducedCastingCost);
				
				// Recalc their production values, to send them their reduced skill/mana, but also the spell just cast might have had some maintenance
				getServerResourceCalculations ().recalculateGlobalProductionValues
					(player.getPlayerDescription ().getPlayerID (), false, mom);
			}
			else
			{
				// Queue it on server
				priv.getQueuedSpellID ().add (spellID);
				
				// Queue it on client
				final OverlandCastQueuedMessage reply = new OverlandCastQueuedMessage ();
				reply.setSpellID (spellID);
				player.getConnection ().sendMessageToClient (reply);
			}
		}
		
		log.exiting (SpellProcessing.class.getName (), "requestCastSpell");
	}

	/**
	 * Spends any skill/mana the player has left towards casting queued spells
	 *
	 * @param gsk Server knowledge structure
	 * @param player Player whose casting to progress
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return True if we cast at least one spell
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final boolean progressOverlandCasting (final MomGeneralServerKnowledge gsk, final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.entering (SpellProcessing.class.getName (), "progressOverlandCasting", player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();

		// Keep going while this player has spells queued, free mana and free skill
		boolean anySpellsCast = false;
		int manaRemaining = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA);
		while ((priv.getQueuedSpellID ().size () > 0) && (trans.getOverlandCastingSkillRemainingThisTurn () > 0) && (manaRemaining > 0))
		{
			// How much to put towards this spell?
			final Spell spell = db.findSpell (priv.getQueuedSpellID ().get (0), "progressOverlandCasting");
			final int reducedCastingCost = getSpellUtils ().getReducedOverlandCastingCost (spell, pub.getPick (), sd.getSpellSetting (), db);
			final int leftToCast = Math.max (0, reducedCastingCost - priv.getManaSpentOnCastingCurrentSpell ());
			final int manaAmount = Math.min (Math.min (trans.getOverlandCastingSkillRemainingThisTurn (), manaRemaining), leftToCast);

			// Put this amount towards the spell
			trans.setOverlandCastingSkillRemainingThisTurn (trans.getOverlandCastingSkillRemainingThisTurn () - manaAmount);
			priv.setManaSpentOnCastingCurrentSpell (priv.getManaSpentOnCastingCurrentSpell () + manaAmount);
			getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, -manaAmount);
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
				castOverlandNow (gsk, player, spell, players, db, sd);
				anySpellsCast = true;
			}

			// Update mana spent so far on client (or set to 0 if finished)
			final UpdateManaSpentOnCastingCurrentSpellMessage msg = new UpdateManaSpentOnCastingCurrentSpellMessage ();
			msg.setManaSpentOnCastingCurrentSpell (priv.getManaSpentOnCastingCurrentSpell ());
			player.getConnection ().sendMessageToClient (msg);

			// No need to tell client how much skill they've got left or mana stored since this is the end of the turn and both will be sent next start phase
		}

		log.exiting (SpellProcessing.class.getName (), "progressOverlandCasting", anySpellsCast);
		return anySpellsCast;
	}

	/**
	 * The method in the FOW class physically removed spells from the server and players' memory; this method
	 * deals with all the knock on effects of spells being switched off, which isn't really much since spells don't grant money or anything when sold
	 * so this is mostly here for consistency with the building and unit methods
	 *
	 * Does not recalc global production (which will now be reduced from not having to pay the maintenance of the cancelled spell),
	 * this has to be done by the calling routine
	 * 
	 * NB. Delphi method was called OkToSwitchOffMaintainedSpell
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
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void switchOffSpell (final FogOfWarMemory trueMap,
		final int castingPlayerID, final String spellID, final Integer unitURN, final String unitSkillID,
		final boolean castInCombat, final OverlandMapCoordinatesEx cityLocation, final String citySpellEffectID, final List<PlayerServerDetails> players,
		final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		log.entering (SpellProcessing.class.getName (), "switchOffSpell",
			new String [] {new Integer (castingPlayerID).toString (), spellID});

		// Any secondary effects we also need to switch off?
		final PlayerServerDetails player = MultiplayerSessionServerUtils.findPlayerWithID (players, castingPlayerID, "switchOffSpell");
		final Spell spell = db.findSpell (spellID, "switchOffSpell");
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spellID);
		final String sectionID = getSpellUtils ().getModifiedSectionID (spell, researchStatus, true);

		// Overland enchantments
		if (sectionID.equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_OVERLAND_ENCHANTMENTS))
		{
			// Check each combat area effect that this overland enchantment gives to see if we have any of them in effect - if so cancel them
			for (final SpellHasCombatEffect effect : spell.getSpellHasCombatEffect ())
				if (getMemoryCombatAreaEffectUtils ().findCombatAreaEffect (trueMap.getCombatAreaEffect (), null, effect.getCombatAreaEffectID (), castingPlayerID))
					getFogOfWarMidTurnChanges ().removeCombatAreaEffectFromServerAndClients (trueMap, effect.getCombatAreaEffectID (), castingPlayerID, null, players, db, sd);
		}

		// Remove spell itself
		getFogOfWarMidTurnChanges ().switchOffMaintainedSpellOnServerAndClients (trueMap, castingPlayerID, spellID, unitURN, unitSkillID, castInCombat,
			cityLocation, citySpellEffectID, players, null, null, null, db, sd);

		log.exiting (SpellProcessing.class.getName (), "switchOffSpell");
	}

	/**
	 * @return Spell utils
	 */
	public final ISpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final ISpellUtils utils)
	{
		spellUtils = utils;
	}

	/**
	 * @return Memory building utils
	 */
	public final IMemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final IMemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}
	
	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final IMemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param utils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final IMemoryMaintainedSpellUtils utils)
	{
		memoryMaintainedSpellUtils = utils;
	}

	/**
	 * @return Memory CAE utils
	 */
	public final IMemoryCombatAreaEffectUtils getMemoryCombatAreaEffectUtils ()
	{
		return memoryCombatAreaEffectUtils;
	}

	/**
	 * @param utils Memory CAE utils
	 */
	public final void setMemoryCombatAreaEffectUtils (final IMemoryCombatAreaEffectUtils utils)
	{
		memoryCombatAreaEffectUtils = utils;
	}
	
	/**
	 * @return Resource value utils
	 */
	public final IResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final IResourceValueUtils utils)
	{
		resourceValueUtils = utils;
	}
	
	/**
	 * @return Server-only unit utils
	 */
	public final IUnitServerUtils getUnitServerUtils ()
	{
		return unitServerUtils;
	}

	/**
	 * @param utils Server-only unit utils
	 */
	public final void setUnitServerUtils (final IUnitServerUtils utils)
	{
		unitServerUtils = utils;
	}
	
	/**
	 * @return Methods for updating true map + players' memory
	 */
	public final IFogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final IFogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}

	/**
	 * @return Methods for dealing with player msgs
	 */
	public IPlayerMessageProcessing getPlayerMessageProcessing ()
	{
		return playerMessageProcessing;
	}

	/**
	 * @param obj Methods for dealing with player msgs
	 */
	public final void setPlayerMessageProcessing (final IPlayerMessageProcessing obj)
	{
		playerMessageProcessing = obj;
	}

	/**
	 * @return Resource calculations
	 */
	public final IMomServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final IMomServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}
}
