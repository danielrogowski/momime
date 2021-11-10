package momime.server.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.calculations.SpellCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.OverlandMapSize;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellSetting;
import momime.common.database.UnitSetting;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.QueuedSpell;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.OverlandCastQueuedMessage;
import momime.common.messages.servertoclient.RemoveQueuedSpellMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.messages.servertoclient.UpdateManaSpentOnCastingCurrentSpellMessage;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatPlayers;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.utils.CombatMapServerUtils;

/**
 * Tests the SpellQueueingImpl class
 */
public final class TestSpellQueueingImpl extends ServerTestData
{
	/**
	 * Tests trying to cast a spell that we don't have researched
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_Unavailable () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		pub.setWizardState (WizardState.ACTIVE);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd3, pub, priv, null, null);

		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player.setConnection (msgs3);
		
		// Isn't researched yet
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);

		// It can be cast overland
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.OVERLAND)).thenReturn (true);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);

		// Run test
		proc.requestCastSpell (player, null, null, null, "SP001", null, null, null, null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, msgs3.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) msgs3.getMessages ().get (0);
		assertEquals ("You don't have that spell researched and/or available so can't cast it.", msg.getText ());
	}

	/**
	 * Tests trying to cast a combat-only spell overland
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_CastCombatOnlySpellOverland () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd3, null, priv, null, null);

		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player.setConnection (msgs3);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can't be cast overland
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.OVERLAND)).thenReturn (false);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);

		// Run test
		proc.requestCastSpell (player, null, null, null, "SP001", null, null, null, null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, msgs3.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) msgs3.getMessages ().get (0);
		assertEquals ("That spell cannot be cast overland.", msg.getText ());
	}

	/**
	 * Tests trying to cast an overland-only spell in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_CastOverlandOnlySpellInCombat () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd3, null, priv, null, null);

		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player.setConnection (msgs3);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can't be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (false);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);

		// Run test
		proc.requestCastSpell (player, null, null, null, "SP001", null, combatLocation, null, null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, msgs3.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) msgs3.getMessages ().get (0);
		assertEquals ("That spell cannot be cast in combat.", msg.getText ());
	}

	/**
	 * Tests trying to target an overland spell (it doesn't work like this - overland spells we first cast, then send a TargetSpellMessage, then specify the target, hence why its an error)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_TargetOverlandSpell () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd3, null, priv, null, null);

		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player.setConnection (msgs3);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast overland
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.OVERLAND)).thenReturn (true);
		
		// Cell to target in combat
		final MapCoordinates2DEx combatTargetLocation = new MapCoordinates2DEx (10, 5);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);

		// Test providing a cell target
		proc.requestCastSpell (player, null, null, null, "SP001", null, null, combatTargetLocation, null, null, mom);
		
		assertEquals (1, msgs3.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) msgs3.getMessages ().get (0);
		assertEquals ("Cannot specify a target when casting an overland spell.", msg.getText ());
		
		// Test providing a unit target
		msgs3.getMessages ().clear ();
		proc.requestCastSpell (player, null, null, null, "SP001", null, null, null, 1, null, mom);
		
		assertEquals (1, msgs3.getMessages ().size ());
		final TextPopupMessage msg2 = (TextPopupMessage) msgs3.getMessages ().get (0);
		assertEquals ("Cannot specify a target when casting an overland spell.", msg2.getText ());
	}

	/**
	 * Tests trying to cast a unit enchantment in combat without specifying a target unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_CastUnitEnchantmentInCombatWithoutTarget () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd3, null, priv, null, null);

		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player.setConnection (msgs3);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);

		// Run test
		proc.requestCastSpell (player, null, null, null, "SP001", null, combatLocation, null, null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, msgs3.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) msgs3.getMessages ().get (0);
		assertEquals ("You must specify a unit target when casting this spell in combat.", msg.getText ());
	}

	/**
	 * Tests trying to cast a summon in combat without specifying a target location
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_CastSummontInCombatWithoutTarget () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd3, null, priv, null, null);

		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player.setConnection (msgs3);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);

		// Run test
		proc.requestCastSpell (player, null, null, null, "SP001", null, combatLocation, null, null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, msgs3.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) msgs3.getMessages ().get (0);
		assertEquals ("You must specify a target location when casting summoning spells in combat.", msg.getText ());
	}

	/**
	 * Tests casting an overland spell that we don't have enough skill/MP to cast this turn, so it gets queued up
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_QueueOverlandSpell () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		pub.setWizardState (WizardState.ACTIVE);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd3, pub, priv, null, trans);

		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player.setConnection (msgs3);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast overland
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.OVERLAND)).thenReturn (true);
		
		// We've got loads of MP, but not enough casting skill
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (spellUtils.getReducedOverlandCastingCost (spell, null, null, pub.getPick (), settings, db)).thenReturn (20);
		when (resourceValueUtils.findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (1000);
		trans.setOverlandCastingSkillRemainingThisTurn (15);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setResourceValueUtils (resourceValueUtils);

		// Call method
		proc.requestCastSpell (player, null, null, null, "SP001", null, null, null, null, null, mom);
		
		// Check results
		assertEquals (1, priv.getQueuedSpell ().size ());
		assertEquals ("SP001", priv.getQueuedSpell ().get (0).getQueuedSpellID ());
		
		assertEquals (1, msgs3.getMessages ().size ());
		final OverlandCastQueuedMessage reply = (OverlandCastQueuedMessage) msgs3.getMessages ().get (0);
		assertEquals ("SP001", reply.getSpellID ());
	}
	
	/**
	 * Tests casting an overland spell that we have enough skill/MP to cast it instantly
	 * @throws Exception If there is a problem
	 */	
	@Test
	public final void testRequestCastSpell_CastOverlandSpellInstantly () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		// Session variables
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		pub.setWizardState (WizardState.ACTIVE);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd3, pub, priv, null, trans);

		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		player.setConnection (msgs3);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player);
		when (mom.getPlayers ()).thenReturn (players);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast overland
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.OVERLAND)).thenReturn (true);
		
		// We've got enough MP and skill
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (spellUtils.getReducedOverlandCastingCost (spell, null, null, pub.getPick (), settings, db)).thenReturn (20);
		when (resourceValueUtils.findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (1000);
		trans.setOverlandCastingSkillRemainingThisTurn (25);	// <---
		
		// Set up test object
		final SpellProcessing spellProcessing = mock (SpellProcessing.class);
		final PlayerMessageProcessing msgProc = mock (PlayerMessageProcessing.class);
		final ServerResourceCalculations serverResourceCalculations = mock (ServerResourceCalculations.class);
		
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setSpellProcessing (spellProcessing);
		proc.setPlayerMessageProcessing (msgProc);
		proc.setServerResourceCalculations (serverResourceCalculations);

		// Call method
		proc.requestCastSpell (player, null, null, null, "SP001", null, null, null, null, null, mom);
		
		// Check it was cast
		verify (spellProcessing, times (1)).castOverlandNow (player, spell, null, null, mom);
		verify (msgProc, times (1)).sendNewTurnMessages (null, players, null);
		
		// Check we were charged skill and mana
		assertEquals (5, trans.getOverlandCastingSkillRemainingThisTurn ());
		verify (resourceValueUtils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -20);
		verify (serverResourceCalculations, times (1)).recalculateGlobalProductionValues (pd3.getPlayerID (), false, mom);
		
		// Check nothing was queued
		assertEquals (0, priv.getQueuedSpell ().size ());
		assertEquals (0, msgs3.getMessages ().size ());
	}

	/**
	 * Tests trying to cast a combat spell when one side is already wiped out
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_CastCombatSpellButWipedOut () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.COMBAT_ENCHANTMENTS);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		pub.setWizardState (WizardState.ACTIVE);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (pd3, pub, priv, null, null);

		final DummyServerToClientConnection msgs3 = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (msgs3);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		when (mom.getPlayers ()).thenReturn (players);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Casting cost
		spell.setCombatCastingCost (20);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);
		
		// One side is wiped out already
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, null);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players, db)).thenReturn (combatPlayers);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setCombatMapUtils (combatMapUtils);

		// Run test
		proc.requestCastSpell (attackingPlayer, null, null, null, "SP001", null, combatLocation, null, null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, msgs3.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) msgs3.getMessages ().get (0);
		assertEquals ("You cannot cast combat spells if one side has been wiped out in the combat.", msg.getText ());
	}

	/**
	 * Tests trying to cast a 2nd combat spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_CastAnotherCombatSpell () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.COMBAT_ENCHANTMENTS);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		// Defending human player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (5);
		defendingPd.setHuman (true);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Attacking human player, who is also the one casting the spell
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerID (7);
		attackingPd.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardState (WizardState.ACTIVE);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, attackingPub, attackingPriv, null, null);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (defendingPlayer);
		players.add (attackingPlayer);
		when (mom.getPlayers ()).thenReturn (players);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (attackingPriv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Casting cost
		spell.setCombatCastingCost (20);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);
		
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		gc.setCombatCurrentPlayerID (7);
		
		// Two sides in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players, db)).thenReturn (combatPlayers);
		
		// We already cast a spell
		gc.setSpellCastThisCombatTurn (true);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setCombatMapUtils (combatMapUtils);

		// Run test
		proc.requestCastSpell (attackingPlayer, null, null, null, "SP001", null, combatLocation, null, null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, attackingMsgs.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) attackingMsgs.getMessages ().get (0);
		assertEquals ("You have already cast a spell this combat turn.", msg.getText ());
	}

	/**
	 * Tests trying to cast spell into a combat that we aren't invovled in
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_CastCombatWeArentInvolvedIn () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.COMBAT_ENCHANTMENTS);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		// Defending human player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (5);
		defendingPd.setHuman (true);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Attacking human player, who is also the one casting the spell
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerID (7);
		attackingPd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardState (WizardState.ACTIVE);
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, attackingPub, attackingPriv, null, null);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (defendingPlayer);
		players.add (attackingPlayer);
		when (mom.getPlayers ()).thenReturn (players);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (attackingPriv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		gc.setCombatCurrentPlayerID (7);
		
		// Two sides in combat (ok this makes the variable names a little misleading, because attackingPlayer isn't really attacking, but it suits the test)
		final CombatPlayers combatPlayers = new CombatPlayers (new PlayerServerDetails (null, null, null, null, null), defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players, db)).thenReturn (combatPlayers);
		
		// Casting cost
		spell.setCombatCastingCost (20);
		when (spellUtils.getReducedCombatCastingCost (spell, null, attackingPub.getPick (), settings, db)).thenReturn (20);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setCombatMapUtils (combatMapUtils);

		// Run test
		proc.requestCastSpell (attackingPlayer, null, null, null, "SP001", null, combatLocation, null, null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, attackingMsgs.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) attackingMsgs.getMessages ().get (0);
		assertEquals ("You tried to cast a combat spell in a combat that you are not participating in.", msg.getText ());
	}

	/**
	 * Tests trying to cast a combat spell while we're banished (capital has been taken)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_CastCombatWhileBanished () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.COMBAT_ENCHANTMENTS);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		// General server knowledge
		final OverlandMapSize sys = createOverlandMapSize ();
		sd.setOverlandMapSize (sys);
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		// Defending human player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (5);
		defendingPd.setHuman (true);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Attacking human player, who is also the one casting the spell
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerID (7);
		attackingPd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, attackingPub, attackingPriv, null, null);
		attackingPub.setWizardState (WizardState.ACTIVE);		// Being sneaky here, allowing the easy check to pass so it has to be trapped by the more in depth check

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (defendingPlayer);
		players.add (attackingPlayer);
		when (mom.getPlayers ()).thenReturn (players);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (attackingPriv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		gc.setCombatCurrentPlayerID (7);
		
		// Two sides in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players, db)).thenReturn (combatPlayers);
		
		// Casting cost
		spell.setCombatCastingCost (20);
		when (spellUtils.getReducedCombatCastingCost (spell, null, attackingPub.getPick (), settings, db)).thenReturn (20);
		gc.setCombatAttackerCastingSkillRemaining (15);
		
		// Isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (gc.getTerrainData ())).thenReturn (false);
		
		// Range multiplier
		final SpellCalculations spellCalc = mock (SpellCalculations.class);
		when (spellCalc.calculateDoubleCombatCastingRangePenalty (attackingPlayer, combatLocation, false, trueTerrain, trueMap.getBuilding (), sys)).thenReturn (null);	// <--- banished
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setCombatMapUtils (combatMapUtils);
		proc.setSpellCalculations (spellCalc);
		proc.setMemoryGridCellUtils (memoryGridCellUtils);

		// Run test
		proc.requestCastSpell (attackingPlayer, null, null, null, "SP001", null, combatLocation, null, null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, attackingMsgs.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) attackingMsgs.getMessages ().get (0);
		assertEquals ("You cannot cast combat spells while you are banished.", msg.getText ());
	}

	/**
	 * Tests trying to cast a combat spell when we have insufficient casting skill remaining for this combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_CastCombatInsufficientCastingSkill () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.COMBAT_ENCHANTMENTS);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		// General server knowledge
		final OverlandMapSize sys = createOverlandMapSize ();
		sd.setOverlandMapSize (sys);
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		// Defending human player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (5);
		defendingPd.setHuman (true);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Attacking human player, who is also the one casting the spell
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerID (7);
		attackingPd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, attackingPub, attackingPriv, null, null);
		attackingPub.setWizardState (WizardState.ACTIVE);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (defendingPlayer);
		players.add (attackingPlayer);
		when (mom.getPlayers ()).thenReturn (players);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (attackingPriv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		gc.setCombatCurrentPlayerID (7);
		
		// Two sides in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players, db)).thenReturn (combatPlayers);
		
		// Casting cost
		spell.setCombatCastingCost (20);
		when (spellUtils.getReducedCombatCastingCost (spell, null, attackingPub.getPick (), settings, db)).thenReturn (20);
		gc.setCombatAttackerCastingSkillRemaining (15);

		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (1000);
		
		// Isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (gc.getTerrainData ())).thenReturn (false);
		
		// Range multiplier
		final SpellCalculations spellCalc = mock (SpellCalculations.class);
		when (spellCalc.calculateDoubleCombatCastingRangePenalty (attackingPlayer, combatLocation, false, trueTerrain, trueMap.getBuilding (), sys)).thenReturn (3);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setCombatMapUtils (combatMapUtils);
		proc.setSpellCalculations (spellCalc);
		proc.setMemoryGridCellUtils (memoryGridCellUtils);
		proc.setResourceValueUtils (resourceValueUtils);

		// Run test
		proc.requestCastSpell (attackingPlayer, null, null, null, "SP001", null, combatLocation, null, null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, attackingMsgs.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) attackingMsgs.getMessages ().get (0);
		assertEquals ("You don't have enough casting skill remaining to cast that spell in combat.", msg.getText ());
	}

	/**
	 * Tests trying to cast a combat spell when we have insufficient MP to cast the spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_CastCombatInsufficientMana () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.COMBAT_ENCHANTMENTS);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		// General server knowledge
		final OverlandMapSize sys = createOverlandMapSize ();
		sd.setOverlandMapSize (sys);
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		// Defending human player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (5);
		defendingPd.setHuman (true);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Attacking human player, who is also the one casting the spell
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerID (7);
		attackingPd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, attackingPub, attackingPriv, null, null);
		attackingPub.setWizardState (WizardState.ACTIVE);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (defendingPlayer);
		players.add (attackingPlayer);
		when (mom.getPlayers ()).thenReturn (players);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (attackingPriv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		gc.setCombatCurrentPlayerID (7);
		
		// Two sides in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players, db)).thenReturn (combatPlayers);
		
		// Casting cost
		spell.setCombatCastingCost (20);
		when (spellUtils.getReducedCombatCastingCost (spell, null, attackingPub.getPick (), settings, db)).thenReturn (20);
		gc.setCombatAttackerCastingSkillRemaining (21);

		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (28);
		
		// Isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (gc.getTerrainData ())).thenReturn (false);
		
		// Range multiplier
		final SpellCalculations spellCalc = mock (SpellCalculations.class);
		when (spellCalc.calculateDoubleCombatCastingRangePenalty (attackingPlayer, combatLocation, false, trueTerrain, trueMap.getBuilding (), sys)).thenReturn (3);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setCombatMapUtils (combatMapUtils);
		proc.setSpellCalculations (spellCalc);
		proc.setMemoryGridCellUtils (memoryGridCellUtils);
		proc.setResourceValueUtils (resourceValueUtils);

		// Run test
		proc.requestCastSpell (attackingPlayer, null, null, null, "SP001", null, combatLocation, null, null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, attackingMsgs.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) attackingMsgs.getMessages ().get (0);
		assertEquals ("You don't have enough mana remaining to cast that spell in combat at this range.", msg.getText ());
	}

	/**
	 * Tests successfully casting a combat enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_CombatEnchantment () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.COMBAT_ENCHANTMENTS);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		// General server knowledge
		final OverlandMapSize sys = createOverlandMapSize ();
		sd.setOverlandMapSize (sys);
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		// Defending human player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (5);
		defendingPd.setHuman (true);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Attacking human player, who is also the one casting the spell
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerID (7);
		attackingPd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, attackingPub, attackingPriv, null, null);
		attackingPub.setWizardState (WizardState.ACTIVE);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (defendingPlayer);
		players.add (attackingPlayer);
		when (mom.getPlayers ()).thenReturn (players);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (attackingPriv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		gc.setCombatCurrentPlayerID (7);
		
		// Two sides in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players, db)).thenReturn (combatPlayers);
		
		// Casting cost
		spell.setCombatCastingCost (20);
		when (spellUtils.getReducedCombatCastingCost (spell, null, attackingPub.getPick (), settings, db)).thenReturn (20);
		gc.setCombatAttackerCastingSkillRemaining (21);

		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (35);
		
		// Isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (gc.getTerrainData ())).thenReturn (false);
		
		// Range multiplier
		final SpellCalculations spellCalc = mock (SpellCalculations.class);
		when (spellCalc.calculateDoubleCombatCastingRangePenalty (attackingPlayer, combatLocation, false, trueTerrain, trueMap.getBuilding (), sys)).thenReturn (3);
		
		// Possible effects
		final MemoryCombatAreaEffectUtils caeUtils = mock (MemoryCombatAreaEffectUtils.class);
		final List<String> possibleEffectIDs = new ArrayList<String> ();
		possibleEffectIDs.add (null);
		when (caeUtils.listCombatEffectsNotYetCastAtLocation (trueMap.getCombatAreaEffect (), spell, attackingPlayer.getPlayerDescription ().getPlayerID (), combatLocation)).thenReturn (possibleEffectIDs);
		
		// Set up test object
		final SpellProcessing spellProcessing = mock (SpellProcessing.class);
		
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setCombatMapUtils (combatMapUtils);
		proc.setSpellCalculations (spellCalc);
		proc.setMemoryGridCellUtils (memoryGridCellUtils);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setSpellProcessing (spellProcessing);
		proc.setMemoryCombatAreaEffectUtils (caeUtils);

		// Run test
		proc.requestCastSpell (attackingPlayer, null, null, null, "SP001", null, combatLocation, null, null, null, mom);
		
		// Check results
		verify (spellProcessing, times (1)).castCombatNow (attackingPlayer, null, null, null, spell, 20, 30, null, combatLocation, defendingPlayer, attackingPlayer, null, null, mom);
		assertEquals (0, attackingMsgs.getMessages ().size ());
	}

	/**
	 * Tests trying to cast a unit enchantment in combat, but the target unit doesn't exist
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_UnitEnchantmentInCombat_TargetNotFound () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		// General server knowledge
		final OverlandMapSize sys = createOverlandMapSize ();
		sd.setOverlandMapSize (sys);
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		// Defending human player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (5);
		defendingPd.setHuman (true);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Attacking human player, who is also the one casting the spell
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerID (7);
		attackingPd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, attackingPub, attackingPriv, null, null);
		attackingPub.setWizardState (WizardState.ACTIVE);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (defendingPlayer);
		players.add (attackingPlayer);
		when (mom.getPlayers ()).thenReturn (players);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (attackingPriv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		gc.setCombatCurrentPlayerID (7);
		
		// Two sides in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players, db)).thenReturn (combatPlayers);
		
		// Casting cost
		spell.setCombatCastingCost (20);
		when (spellUtils.getReducedCombatCastingCost (spell, null, attackingPub.getPick (), settings, db)).thenReturn (20);
		gc.setCombatAttackerCastingSkillRemaining (21);

		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (35);
		
		// Isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (gc.getTerrainData ())).thenReturn (false);
		
		// Range multiplier
		final SpellCalculations spellCalc = mock (SpellCalculations.class);
		when (spellCalc.calculateDoubleCombatCastingRangePenalty (attackingPlayer, combatLocation, false, trueTerrain, trueMap.getBuilding (), sys)).thenReturn (3);
		
		// Unit doesn't exist
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (101, trueMap.getUnit ())).thenReturn (null);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setCombatMapUtils (combatMapUtils);
		proc.setSpellCalculations (spellCalc);
		proc.setMemoryGridCellUtils (memoryGridCellUtils);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setUnitUtils (unitUtils);

		// Run test
		proc.requestCastSpell (attackingPlayer, null, null, null, "SP001", null, combatLocation, null, 101, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, attackingMsgs.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) attackingMsgs.getMessages ().get (0);
		assertEquals ("Cannot find the unit you are trying to target the spell on.", msg.getText ());
	}

	/**
	 * Tests trying to cast a unit enchantment in combat on an invalid target, e.g. trying to cast an enchantment like holy weapon on an enemy unit 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_UnitEnchantmentInCombat_InvalidTarget () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellRealm ("MB01");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		// General server knowledge
		final OverlandMapSize sys = createOverlandMapSize ();
		sd.setOverlandMapSize (sys);
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		// Defending human player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (5);
		defendingPd.setHuman (true);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Attacking human player, who is also the one casting the spell
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerID (7);
		attackingPd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, attackingPub, attackingPriv, null, null);
		attackingPub.setWizardState (WizardState.ACTIVE);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (defendingPlayer);
		players.add (attackingPlayer);
		when (mom.getPlayers ()).thenReturn (players);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (attackingPriv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		gc.setCombatCurrentPlayerID (7);
		
		// Two sides in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players, db)).thenReturn (combatPlayers);
		
		// Casting cost
		spell.setCombatCastingCost (20);
		when (spellUtils.getReducedCombatCastingCost (spell, null, attackingPub.getPick (), settings, db)).thenReturn (20);
		gc.setCombatAttackerCastingSkillRemaining (21);

		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (35);
		
		// Isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (gc.getTerrainData ())).thenReturn (false);
		
		// Range multiplier
		final SpellCalculations spellCalc = mock (SpellCalculations.class);
		when (spellCalc.calculateDoubleCombatCastingRangePenalty (attackingPlayer, combatLocation, false, trueTerrain, trueMap.getBuilding (), sys)).thenReturn (3);
		
		// Target unit
		final MemoryUnit targetUnit = new MemoryUnit ();
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (101, trueMap.getUnit ())).thenReturn (targetUnit);
		
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (targetUnit, null, null, spell.getSpellRealm (), players, trueMap, db)).thenReturn (xu);
		
		// Invalid target
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		when (memoryMaintainedSpellUtils.isUnitValidTargetForSpell (spell, null, combatLocation, attackingPd.getPlayerID (), null, null, xu, true,
			trueMap, attackingPriv.getFogOfWar (), players, db)).thenReturn (TargetSpellResult.ENCHANTING_OR_HEALING_ENEMY);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setCombatMapUtils (combatMapUtils);
		proc.setSpellCalculations (spellCalc);
		proc.setMemoryGridCellUtils (memoryGridCellUtils);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setUnitUtils (unitUtils);
		proc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		proc.setExpandUnitDetails (expand);

		// Run test
		proc.requestCastSpell (attackingPlayer, null, null, null, "SP001", null, combatLocation, null, 101, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, attackingMsgs.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) attackingMsgs.getMessages ().get (0);
		assertEquals ("This unit is not a valid target for this spell for reason ENCHANTING_OR_HEALING_ENEMY", msg.getText ());
	}

	/**
	 * Tests successfully casting a unit enchantment in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_UnitEnchantmentInCombat () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellRealm ("MB01");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		// General server knowledge
		final OverlandMapSize sys = createOverlandMapSize ();
		sd.setOverlandMapSize (sys);
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		// Defending human player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (5);
		defendingPd.setHuman (true);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Attacking human player, who is also the one casting the spell
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerID (7);
		attackingPd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, attackingPub, attackingPriv, null, null);
		attackingPub.setWizardState (WizardState.ACTIVE);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (defendingPlayer);
		players.add (attackingPlayer);
		when (mom.getPlayers ()).thenReturn (players);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (attackingPriv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		gc.setCombatCurrentPlayerID (7);
		
		// Two sides in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players, db)).thenReturn (combatPlayers);
		
		// Casting cost
		spell.setCombatCastingCost (20);
		when (spellUtils.getReducedCombatCastingCost (spell, null, attackingPub.getPick (), settings, db)).thenReturn (20);
		gc.setCombatAttackerCastingSkillRemaining (21);

		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (35);
		
		// Isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (gc.getTerrainData ())).thenReturn (false);
		
		// Range multiplier
		final SpellCalculations spellCalc = mock (SpellCalculations.class);
		when (spellCalc.calculateDoubleCombatCastingRangePenalty (attackingPlayer, combatLocation, false, trueTerrain, trueMap.getBuilding (), sys)).thenReturn (3);
		
		// Target unit
		final MemoryUnit targetUnit = new MemoryUnit ();
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (101, trueMap.getUnit ())).thenReturn (targetUnit);
		
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (targetUnit, null, null, spell.getSpellRealm (), players, trueMap, db)).thenReturn (xu);
		
		// Invalid target
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		when (memoryMaintainedSpellUtils.isUnitValidTargetForSpell (spell, null, combatLocation, attackingPd.getPlayerID (), null, null, xu, true,
			trueMap, attackingPriv.getFogOfWar (), players, db)).thenReturn (TargetSpellResult.VALID_TARGET);
		
		// Set up test object
		final SpellProcessing spellProcessing = mock (SpellProcessing.class);
		
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setCombatMapUtils (combatMapUtils);
		proc.setSpellCalculations (spellCalc);
		proc.setMemoryGridCellUtils (memoryGridCellUtils);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setUnitUtils (unitUtils);
		proc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		proc.setSpellProcessing (spellProcessing);
		proc.setExpandUnitDetails (expand);

		// Run test
		proc.requestCastSpell (attackingPlayer, null, null, null, "SP001", null, combatLocation, null, 101, null, mom);
		
		// Check results
		verify (spellProcessing, times (1)).castCombatNow (attackingPlayer, null, null, null, spell, 20, 30, null, combatLocation, defendingPlayer, attackingPlayer, targetUnit, null, mom);
		assertEquals (0, attackingMsgs.getMessages ().size ());
	}

	/**
	 * Tests trying to summon a unit in combat onto a space already occupied by another unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_CombatSummon_OnTopOfOtherUnit () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		sd.setCombatMapSize (createCombatMapSize ());
		
		// General server knowledge
		final OverlandMapSize sys = createOverlandMapSize ();
		sd.setOverlandMapSize (sys);
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		// Defending human player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (5);
		defendingPd.setHuman (true);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Attacking human player, who is also the one casting the spell
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerID (7);
		attackingPd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, attackingPub, attackingPriv, null, null);
		attackingPub.setWizardState (WizardState.ACTIVE);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (defendingPlayer);
		players.add (attackingPlayer);
		when (mom.getPlayers ()).thenReturn (players);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (attackingPriv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		gc.setCombatCurrentPlayerID (7);
		
		// Two sides in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players, db)).thenReturn (combatPlayers);
		
		// Casting cost
		spell.setCombatCastingCost (20);
		when (spellUtils.getReducedCombatCastingCost (spell, null, attackingPub.getPick (), settings, db)).thenReturn (20);
		gc.setCombatAttackerCastingSkillRemaining (21);

		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (35);
		
		// Isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (gc.getTerrainData ())).thenReturn (false);
		
		// Range multiplier
		final SpellCalculations spellCalc = mock (SpellCalculations.class);
		when (spellCalc.calculateDoubleCombatCastingRangePenalty (attackingPlayer, combatLocation, false, trueTerrain, trueMap.getBuilding (), sys)).thenReturn (3);

		// Cell to target in combat
		final MapCoordinates2DEx combatTargetLocation = new MapCoordinates2DEx (9, 7);
		
		// Specifics about cell being target
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findAliveUnitInCombatWeCanSeeAt (combatLocation, combatTargetLocation, 7, players,
			trueMap, db, sd.getCombatMapSize (), true)).thenReturn (mock (ExpandedUnitDetails.class));
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setCombatMapUtils (combatMapUtils);
		proc.setSpellCalculations (spellCalc);
		proc.setMemoryGridCellUtils (memoryGridCellUtils);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setUnitUtils (unitUtils);

		// Run test
		proc.requestCastSpell (attackingPlayer, null, null, null, "SP001", null, combatLocation, combatTargetLocation, null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, attackingMsgs.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) attackingMsgs.getMessages ().get (0);
		assertEquals ("There is already a unit in the chosen location so you cannot summon there.", msg.getText ());
	}

	/**
	 * Tests trying to summon a unit in combat when we're already at the maximum number of units
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_CombatSummon_MaxUnits () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		final UnitSetting unitSettings = new UnitSetting ();
		unitSettings.setCanExceedMaximumUnitsDuringCombat (false);
		sd.setUnitSetting (unitSettings);
		
		// General server knowledge
		final OverlandMapSize sys = createOverlandMapSize ();
		sd.setOverlandMapSize (sys);
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		// Defending human player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (5);
		defendingPd.setHuman (true);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Attacking human player, who is also the one casting the spell
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerID (7);
		attackingPd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, attackingPub, attackingPriv, null, null);
		attackingPub.setWizardState (WizardState.ACTIVE);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (defendingPlayer);
		players.add (attackingPlayer);
		when (mom.getPlayers ()).thenReturn (players);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (attackingPriv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		gc.setCombatCurrentPlayerID (7);
		
		// Two sides in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players, db)).thenReturn (combatPlayers);
		
		// Casting cost
		spell.setCombatCastingCost (20);
		when (spellUtils.getReducedCombatCastingCost (spell, null, attackingPub.getPick (), settings, db)).thenReturn (20);
		gc.setCombatAttackerCastingSkillRemaining (21);

		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (35);
		
		// Isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (gc.getTerrainData ())).thenReturn (false);
		
		// Range multiplier
		final SpellCalculations spellCalc = mock (SpellCalculations.class);
		when (spellCalc.calculateDoubleCombatCastingRangePenalty (attackingPlayer, combatLocation, false, trueTerrain, trueMap.getBuilding (), sys)).thenReturn (3);

		// Cell to target in combat
		final MapCoordinates2DEx combatTargetLocation = new MapCoordinates2DEx (9, 7);
		
		// Specifics about cell being target
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findAliveUnitInCombatAt (trueMap.getUnit (), combatLocation, combatTargetLocation, db, true)).thenReturn (null);
		
		// Number of units already here
		final CombatMapServerUtils combatMapServerUtils = mock (CombatMapServerUtils.class);
		when (combatMapServerUtils.countPlayersAliveUnitsAtCombatLocation (attackingPd.getPlayerID (), combatLocation, trueMap.getUnit (), db)).thenReturn (9);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setCombatMapUtils (combatMapUtils);
		proc.setSpellCalculations (spellCalc);
		proc.setMemoryGridCellUtils (memoryGridCellUtils);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setUnitUtils (unitUtils);
		proc.setCombatMapServerUtils (combatMapServerUtils);

		// Run test
		proc.requestCastSpell (attackingPlayer, null, null, null, "SP001", null, combatLocation, combatTargetLocation, null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, attackingMsgs.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) attackingMsgs.getMessages ().get (0);
		assertEquals ("You already have the maximum number of units in combat so cannot summon any more.", msg.getText ());
	}
	
	/**
	 * Tests trying to summon a unit in combat onto impassable terrain
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_CombatSummon_Impassable () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		final UnitSetting unitSettings = new UnitSetting ();
		unitSettings.setCanExceedMaximumUnitsDuringCombat (false);
		sd.setUnitSetting (unitSettings);
		
		// General server knowledge
		final OverlandMapSize sys = createOverlandMapSize ();
		sd.setOverlandMapSize (sys);
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		// Defending human player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (5);
		defendingPd.setHuman (true);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Attacking human player, who is also the one casting the spell
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerID (7);
		attackingPd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, attackingPub, attackingPriv, null, null);
		attackingPub.setWizardState (WizardState.ACTIVE);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (defendingPlayer);
		players.add (attackingPlayer);
		when (mom.getPlayers ()).thenReturn (players);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (attackingPriv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);

		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		gc.setCombatMap (createCombatMap ());
		gc.setCombatCurrentPlayerID (7);
		
		// Two sides in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players, db)).thenReturn (combatPlayers);
		
		// Casting cost
		spell.setCombatCastingCost (20);
		when (spellUtils.getReducedCombatCastingCost (spell, null, attackingPub.getPick (), settings, db)).thenReturn (20);
		gc.setCombatAttackerCastingSkillRemaining (21);

		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (35);
		
		// Isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (gc.getTerrainData ())).thenReturn (false);
		
		// Range multiplier
		final SpellCalculations spellCalc = mock (SpellCalculations.class);
		when (spellCalc.calculateDoubleCombatCastingRangePenalty (attackingPlayer, combatLocation, false, trueTerrain, trueMap.getBuilding (), sys)).thenReturn (3);

		// Cell to target in combat
		final MapCoordinates2DEx combatTargetLocation = new MapCoordinates2DEx (9, 7);
		
		// Specifics about cell being target
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findAliveUnitInCombatAt (trueMap.getUnit (), combatLocation, combatTargetLocation, db, true)).thenReturn (null);
		
		// Number of units already here
		final CombatMapServerUtils combatMapServerUtils = mock (CombatMapServerUtils.class);
		when (combatMapServerUtils.countPlayersAliveUnitsAtCombatLocation (attackingPd.getPlayerID (), combatLocation, trueMap.getUnit (), db)).thenReturn (8);
		
		// Combat terrain cell
		final UnitCalculations unitCalc = mock (UnitCalculations.class);
		when (unitCalc.calculateDoubleMovementToEnterCombatTile (gc.getCombatMap ().getRow ().get (7).getCell ().get (9), db)).thenReturn (-1);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setCombatMapUtils (combatMapUtils);
		proc.setSpellCalculations (spellCalc);
		proc.setMemoryGridCellUtils (memoryGridCellUtils);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setUnitUtils (unitUtils);
		proc.setUnitCalculations (unitCalc);
		proc.setCombatMapServerUtils (combatMapServerUtils);

		// Run test
		proc.requestCastSpell (attackingPlayer, null, null, null, "SP001", null, combatLocation, combatTargetLocation, null, null, mom);
		
		// Check player got send the right error message
		assertEquals (1, attackingMsgs.getMessages ().size ());
		final TextPopupMessage msg = (TextPopupMessage) attackingMsgs.getMessages ().get (0);
		assertEquals ("The terrain at your chosen location is impassable so you cannot summon a unit there.", msg.getText ());
	}

	/**
	 * Tests successfully summoning a unit in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRequestCastSpell_CombatSummon () throws Exception
	{
		// Mock database
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "requestCastSpell")).thenReturn (spell);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		final UnitSetting unitSettings = new UnitSetting ();
		unitSettings.setCanExceedMaximumUnitsDuringCombat (true); 		// <---
		sd.setUnitSetting (unitSettings);
		
		// General server knowledge
		final OverlandMapSize sys = createOverlandMapSize ();
		sd.setOverlandMapSize (sys);
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);

		// Defending human player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerID (5);
		defendingPd.setHuman (true);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		// Attacking human player, who is also the one casting the spell
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerID (7);
		attackingPd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, attackingPub, attackingPriv, null, null);
		attackingPub.setWizardState (WizardState.ACTIVE);

		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (defendingPlayer);
		players.add (attackingPlayer);
		when (mom.getPlayers ()).thenReturn (players);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (attackingPriv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// It can be cast in combat
		when (spellUtils.spellCanBeCastIn (spell, SpellCastType.COMBAT)).thenReturn (true);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);

		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		gc.setCombatMap (createCombatMap ());
		gc.setCombatCurrentPlayerID (7);
		
		// Two sides in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players, db)).thenReturn (combatPlayers);
		
		// Casting cost
		spell.setCombatCastingCost (20);
		when (spellUtils.getReducedCombatCastingCost (spell, null, attackingPub.getPick (), settings, db)).thenReturn (20);
		gc.setCombatAttackerCastingSkillRemaining (21);

		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (35);
		
		// Isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (gc.getTerrainData ())).thenReturn (false);
		
		// Range multiplier
		final SpellCalculations spellCalc = mock (SpellCalculations.class);
		when (spellCalc.calculateDoubleCombatCastingRangePenalty (attackingPlayer, combatLocation, false, trueTerrain, trueMap.getBuilding (), sys)).thenReturn (3);

		// Cell to target in combat
		final MapCoordinates2DEx combatTargetLocation = new MapCoordinates2DEx (9, 7);
		combatTargetLocation.setX (9);
		combatTargetLocation.setY (7);
		
		// Specifics about cell being target
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findAliveUnitInCombatAt (trueMap.getUnit (), combatLocation, combatTargetLocation, db, true)).thenReturn (null);
		
		// Number of units already here
		final CombatMapServerUtils combatMapServerUtils = mock (CombatMapServerUtils.class);
		when (combatMapServerUtils.countPlayersAliveUnitsAtCombatLocation (attackingPd.getPlayerID (), combatLocation, trueMap.getUnit (), db)).thenReturn (9);
		
		// Combat terrain cell
		final UnitCalculations unitCalc = mock (UnitCalculations.class);
		when (unitCalc.calculateDoubleMovementToEnterCombatTile (gc.getCombatMap ().getRow ().get (7).getCell ().get (9), db)).thenReturn (1);
		
		// Set up test object
		final SpellProcessing spellProcessing = mock (SpellProcessing.class);
		
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setCombatMapUtils (combatMapUtils);
		proc.setSpellCalculations (spellCalc);
		proc.setMemoryGridCellUtils (memoryGridCellUtils);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setUnitUtils (unitUtils);
		proc.setUnitCalculations (unitCalc);
		proc.setSpellProcessing (spellProcessing);
		proc.setCombatMapServerUtils (combatMapServerUtils);

		// Run test
		proc.requestCastSpell (attackingPlayer, null, null, null, "SP001", null, combatLocation, combatTargetLocation, null, null, mom);
		
		// Check results
		verify (spellProcessing, times (1)).castCombatNow (attackingPlayer, null, null, null, spell, 20, 30, null, combatLocation, defendingPlayer, attackingPlayer, null, combatTargetLocation, mom);
		assertEquals (0, attackingMsgs.getMessages ().size ());
	}

	/**
	 * Tests the progressOverlandCasting method, when we have no queued spells, so nothing to do
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProgressOverlandCasting_NothingQueued () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// General server knowledge
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge (); 
		
		// Players list
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (7);
		pd.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		player.setConnection (msgs);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player);
		
		// How much spare MP we have
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (30);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setResourceValueUtils (resourceValueUtils);
		
		// Run method
		assertFalse (proc.progressOverlandCasting (player, mom));
		
		// Check results
		assertEquals (0, msgs.getMessages ().size ());
	}
	
	/**
	 * Tests the progressOverlandCasting method, when we still haven't finished, so we put some MP towards it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProgressOverlandCasting_Progress () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		// General server knowledge
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge (); 
		
		// Players list
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (7);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge (); 
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, trans);

		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		player.setConnection (msgs);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player);
		
		// How much spare MP we have
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (1000);
		
		// Queued spell
		final Spell spell = new Spell ();
		when (db.findSpell ("SP001", "progressOverlandCasting")).thenReturn (spell);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		
		final QueuedSpell queued = new QueuedSpell ();
		queued.setQueuedSpellID ("SP001");
		
		priv.getQueuedSpell ().add (queued);
		trans.setOverlandCastingSkillRemainingThisTurn (12);
		
		when (spellUtils.getReducedOverlandCastingCost (spell, null, null, pub.getPick (), settings, db)).thenReturn (100);
		priv.setManaSpentOnCastingCurrentSpell (15);		
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
		// Set up test object
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setSpellUtils (spellUtils);
		
		// Run method
		assertFalse (proc.progressOverlandCasting (player, mom));

		// Check results
		assertEquals (0, trans.getOverlandCastingSkillRemainingThisTurn ());
		assertEquals (15+12, priv.getManaSpentOnCastingCurrentSpell ());
		assertEquals (1, priv.getQueuedSpell ().size ());
		
		verify (resourceValueUtils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -12);
		
		// Messages sent to client
		assertEquals (1, msgs.getMessages ().size ());
		assertEquals (UpdateManaSpentOnCastingCurrentSpellMessage.class.getName (), msgs.getMessages ().get (0).getClass ().getName ());
		final UpdateManaSpentOnCastingCurrentSpellMessage msg = (UpdateManaSpentOnCastingCurrentSpellMessage) msgs.getMessages ().get (0);
		assertEquals (15+12, msg.getManaSpentOnCastingCurrentSpell ());
	}

	/**
	 * Tests the progressOverlandCasting method, when we complete casting a single spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProgressOverlandCasting_CastOne () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		// General server knowledge
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge (); 
		
		// Players list
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (7);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge (); 
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, trans);

		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		player.setConnection (msgs);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player);
		
		// How much spare MP we have
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (1000);
		
		// Queued spell
		final Spell spell = new Spell ();
		when (db.findSpell ("SP001", "progressOverlandCasting")).thenReturn (spell);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		
		final QueuedSpell queued = new QueuedSpell ();
		queued.setQueuedSpellID ("SP001");

		priv.getQueuedSpell ().add (queued);
		trans.setOverlandCastingSkillRemainingThisTurn (12);
		
		when (spellUtils.getReducedOverlandCastingCost (spell, null, null, pub.getPick (), settings, db)).thenReturn (25);		// <---
		priv.setManaSpentOnCastingCurrentSpell (15);		
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
		// Set up test object
		final SpellProcessing spellProcessing = mock (SpellProcessing.class);
		
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setSpellUtils (spellUtils);
		proc.setSpellProcessing (spellProcessing);
		
		// Run method
		assertTrue (proc.progressOverlandCasting (player, mom));

		// Check results
		assertEquals (2, trans.getOverlandCastingSkillRemainingThisTurn ());
		assertEquals (0, priv.getManaSpentOnCastingCurrentSpell ());
		assertEquals (0, priv.getQueuedSpell ().size ());
		
		verify (resourceValueUtils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -10);
		verify (spellProcessing, times (1)).castOverlandNow (player, spell, null, null, mom);
		
		// Messages sent to client
		assertEquals (2, msgs.getMessages ().size ());
		assertEquals (RemoveQueuedSpellMessage.class.getName (), msgs.getMessages ().get (0).getClass ().getName ());
		final RemoveQueuedSpellMessage msg1 = (RemoveQueuedSpellMessage) msgs.getMessages ().get (0);
		assertEquals (0, msg1.getQueuedSpellIndex ());		
		
		assertEquals (UpdateManaSpentOnCastingCurrentSpellMessage.class.getName (), msgs.getMessages ().get (1).getClass ().getName ());
		final UpdateManaSpentOnCastingCurrentSpellMessage msg2 = (UpdateManaSpentOnCastingCurrentSpellMessage) msgs.getMessages ().get (1);
		assertEquals (0, msg2.getManaSpentOnCastingCurrentSpell ());
	}
	
	/**
	 * Tests the progressOverlandCasting method, when we complete casting multiple cheap spells
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProgressOverlandCasting_CastMultiple () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final SpellSetting settings = new SpellSetting (); 
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (settings);
		
		// General server knowledge
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge (); 
		
		// Players list
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (7);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge (); 
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, trans);

		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		player.setConnection (msgs);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player);
		
		// How much spare MP we have
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (1000);
		
		// Queued spell
		final Spell spell = new Spell ();
		when (db.findSpell ("SP001", "progressOverlandCasting")).thenReturn (spell);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		for (int n = 0; n < 4; n++)
		{
			final QueuedSpell queued = new QueuedSpell ();
			queued.setQueuedSpellID ("SP001");
			
			priv.getQueuedSpell ().add (queued);
		}
		
		trans.setOverlandCastingSkillRemainingThisTurn (14);
		
		when (spellUtils.getReducedOverlandCastingCost (spell, null, null, pub.getPick (), settings, db)).thenReturn (5);		// <---
		priv.setManaSpentOnCastingCurrentSpell (2);		// So we should finish this one, fully cast 2 more, and spend 1 point toward casting the 4th
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
		// Set up test object
		final SpellProcessing spellProcessing = mock (SpellProcessing.class);
		
		final SpellQueueingImpl proc = new SpellQueueingImpl ();
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setSpellUtils (spellUtils);
		proc.setSpellProcessing (spellProcessing);
		
		// Run method
		assertTrue (proc.progressOverlandCasting (player, mom));

		// Check results
		assertEquals (0, trans.getOverlandCastingSkillRemainingThisTurn ());
		assertEquals (1, priv.getManaSpentOnCastingCurrentSpell ());
		assertEquals (1, priv.getQueuedSpell ().size ());
		
		verify (resourceValueUtils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -3);
		verify (resourceValueUtils, times (2)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -5);
		verify (resourceValueUtils, times (1)).addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -1);
		verify (spellProcessing, times (3)).castOverlandNow (player, spell, null, null, mom);
		
		// Messages sent to client
		// #0, #2, #4 will be removing queued spells
		// #1, #3, #5, #6 will be updating mana remaining 
		assertEquals (7, msgs.getMessages ().size ());
		for (int n = 0; n <= 4; n=n+2)
		{
			assertEquals (RemoveQueuedSpellMessage.class.getName (), msgs.getMessages ().get (n).getClass ().getName ());
			final RemoveQueuedSpellMessage msg1 = (RemoveQueuedSpellMessage) msgs.getMessages ().get (n);
			assertEquals (0, msg1.getQueuedSpellIndex ());
		
			assertEquals (UpdateManaSpentOnCastingCurrentSpellMessage.class.getName (), msgs.getMessages ().get (n+1).getClass ().getName ());
			final UpdateManaSpentOnCastingCurrentSpellMessage msg2 = (UpdateManaSpentOnCastingCurrentSpellMessage) msgs.getMessages ().get (n+1);
			assertEquals (0, msg2.getManaSpentOnCastingCurrentSpell ());
		}

		assertEquals (UpdateManaSpentOnCastingCurrentSpellMessage.class.getName (), msgs.getMessages ().get (6).getClass ().getName ());
		final UpdateManaSpentOnCastingCurrentSpellMessage msg2 = (UpdateManaSpentOnCastingCurrentSpellMessage) msgs.getMessages ().get (6);
		assertEquals (1, msg2.getManaSpentOnCastingCurrentSpell ());
	}
}