package momime.server.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageSpell;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.UnitStatusID;
import momime.common.movement.MovementUtils;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KindOfSpell;
import momime.common.utils.KindOfSpellUtils;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.knowledge.CombatDetails;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.utils.CombatMapServerUtils;
import momime.server.utils.KnownWizardServerUtils;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.utils.UnitServerUtils;

/**
 * Tests the SpellProcessingImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestSpellProcessingImpl extends ServerTestData
{
	/**
	 * Tests the castOverlandNow spell casting a spell that we haven't researched yet.
	 * This test is a bit misleading - what's really happening is because the research status is not AVAILABLE, kindOfSpell ends up as null, and so the
	 * exception that gets thrown is actually just saying it doesn't know how to deal with casting a spell of kind "null" - there isn't an actual real check on research status.
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_Unavailable () throws Exception
	{
		// Maintained spell list
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setPlayerType (PlayerType.HUMAN);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);

		// Wizard casting the spell
		final KnownWizardDetails wizard3 = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), pd3.getPlayerID (), "castOverlandNow")).thenReturn (wizard3);
		
		// Spell to cast
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		
		// Isn't researched yet
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		
		final SpellUtils utils = mock (SpellUtils.class);
		when (utils.findSpellResearchStatus (priv3.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		when (utils.getModifiedSectionID (spell, researchStatus.getStatus (), true)).thenReturn (SpellBookSectionID.RESEARCHABLE_NOW);		// Can research, but don't know it yet
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up test object
		final SpellProcessingImpl proc = new SpellProcessingImpl ();
		proc.setSpellUtils (utils);
		proc.setKindOfSpellUtils (kindOfSpellUtils);
		proc.setKnownWizardUtils (knownWizardUtils);

		// Run test
		assertThrows (MomException.class, () ->
		{
			proc.castOverlandNow (player3, spell, null, null, mom);
		});
	}

	/**
	 * Tests the castOverlandNow spell casting an overland enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_OverlandEnchantment () throws Exception
	{
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();

		// Maintained spell list
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Human player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (5);
		pd1.setPlayerType (PlayerType.HUMAN);
		
		final MomTransientPlayerPrivateKnowledge trans1 = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, null, null, trans1);
		players.add (player1);

		// AI player
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (-1);
		pd2.setPlayerType (PlayerType.AI);

		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, null, null, null);
		players.add (player2);

		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setPlayerType (PlayerType.HUMAN);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, trans3);
		players.add (player3);
		
		// Wizard casting the spell
		final KnownWizardDetails wizard3 = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), pd3.getPlayerID (), "castOverlandNow")).thenReturn (wizard3);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);

		// Spell to cast
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setOverlandCastingCost (22);
		
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		
		// It grants one of 5 possible effects
		for (int n = 1; n <= 5; n++)
			spell.getSpellHasCombatEffect ().add ("CSE00" + n);
		
		// Pick the 4th effect
		final RandomUtils randomUtils = mock (RandomUtils.class);
		when (randomUtils.nextInt (5)).thenReturn (3);

		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils utils = mock (SpellUtils.class);
		when (utils.findSpellResearchStatus (priv3.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		when (utils.getModifiedSectionID (spell, researchStatus.getStatus (), true)).thenReturn (SpellBookSectionID.OVERLAND_ENCHANTMENTS);
		
		// Set up test object
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		final KnownWizardServerUtils knownWizardServerUtils = mock (KnownWizardServerUtils.class);
		
		final SpellProcessingImpl proc = new SpellProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setSpellUtils (utils);
		proc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		proc.setRandomUtils (randomUtils);
		proc.setKindOfSpellUtils (kindOfSpellUtils);
		proc.setKnownWizardServerUtils (knownWizardServerUtils);
		proc.setKnownWizardUtils (knownWizardUtils);

		// Run test
		proc.castOverlandNow (player3, spell, null, null, mom);
		
		// Mocked method handles adding the spell to the true map, player's memories and sending the network msgs, so don't need to worry about any of that
		verify (midTurn).addMaintainedSpellOnServerAndClients (pd3.getPlayerID (), "SP001", null, null, false, null, null, null, false, true, mom);
		
		// CAE should get added also
		verify (midTurn).addCombatAreaEffectOnServerAndClients (gsk, "CSE004", "SP001", pd3.getPlayerID (), 22, null, players, sd);
		
		// Human players won't get any NTMs about it
		assertEquals (0, trans1.getNewTurnMessage ().size ());
		assertEquals (0, trans3.getNewTurnMessage ().size ());
		
		verifyNoMoreInteractions (midTurn);
	}

	/**
	 * Tests the castOverlandNow spell casting an overland enchantment that we already have
	 * This isn't an error - just nothing happens
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_OverlandEnchantment_AlreadyExists () throws Exception
	{
		// Maintained spell list
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Human player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (5);
		pd1.setPlayerType (PlayerType.HUMAN);
		
		final MomTransientPlayerPrivateKnowledge trans1 = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, null, null, trans1);
		players.add (player1);

		// AI player
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (-1);
		pd2.setPlayerType (PlayerType.AI);

		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, null, null, null);
		players.add (player2);

		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setPlayerType (PlayerType.HUMAN);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, trans3);
		players.add (player3);

		// Wizard casting the spell
		final KnownWizardDetails wizard3 = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), pd3.getPlayerID (), "castOverlandNow")).thenReturn (wizard3);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Spell to cast
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setOverlandCastingCost (22);
		
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils utils = mock (SpellUtils.class);
		when (utils.findSpellResearchStatus (priv3.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		when (utils.getModifiedSectionID (spell, researchStatus.getStatus (), true)).thenReturn (SpellBookSectionID.OVERLAND_ENCHANTMENTS);

		// We've already cast it
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		when (memoryMaintainedSpellUtils.findMaintainedSpell (trueMap.getMaintainedSpell (), 7, "SP001", null, null, null, null)).thenReturn (new MemoryMaintainedSpell ());
		
		// Set up test object
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		
		final SpellProcessingImpl proc = new SpellProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setSpellUtils (utils);
		proc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		proc.setKindOfSpellUtils (kindOfSpellUtils);
		proc.setKnownWizardUtils (knownWizardUtils);

		// Run test
		proc.castOverlandNow (player3, spell, null, null, mom);
		
		// Human players won't get any NTMs about it
		assertEquals (0, trans1.getNewTurnMessage ().size ());
		assertEquals (0, trans3.getNewTurnMessage ().size ());
		
		verifyNoMoreInteractions (midTurn);
	}

	/**
	 * Tests the castOverlandNow spell casting a unit enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_UnitEnchantment () throws Exception
	{
		// Maintained spell list
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Human player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setPlayerType (PlayerType.HUMAN);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, trans3);
		players.add (player3);

		// Wizard casting the spell
		final KnownWizardDetails wizard3 = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), pd3.getPlayerID (), "castOverlandNow")).thenReturn (wizard3);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Spell to cast
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils utils = mock (SpellUtils.class);
		when (utils.findSpellResearchStatus (priv3.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		when (utils.getModifiedSectionID (spell, researchStatus.getStatus (), true)).thenReturn (SpellBookSectionID.UNIT_ENCHANTMENTS);
		
		// Set up test object
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class); 
		
		final SpellProcessingImpl proc = new SpellProcessingImpl ();
		proc.setSpellUtils (utils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setKindOfSpellUtils (kindOfSpellUtils);
		proc.setKnownWizardUtils (knownWizardUtils);

		// Run test
		proc.castOverlandNow (player3, spell, null, null, mom);
		
		// Check we told human player to pick a target
		assertEquals (1, trans3.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.TARGET_SPELL, trans3.getNewTurnMessage ().get (0).getMsgType ());
		final NewTurnMessageSpell ntm = (NewTurnMessageSpell) trans3.getNewTurnMessage ().get (0);
		assertEquals ("SP001", ntm.getSpellID ());

		// Check that we recorded targetless spell on server.
		// NB. players (arg just before 'db') intentionally null so that spell only added on server.
		verify (midTurn).addMaintainedSpellOnServerAndClients (pd3.getPlayerID ().intValue (), "SP001", null, null, false, null, null, null, false, false, mom);
		
		verifyNoMoreInteractions (midTurn);
	}
	
	/**
	 * Tests trying to cast a spell into a combat we aren't participating in
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastCombatNow_NotParticipating () throws Exception
	{
		// Server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Spell to cast
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (15, 25, 1);

		// Players involved
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (null, null, null, null, null); 
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (null, null, null, null, null);
		
		final PlayerDescription castingPd = new PlayerDescription ();
		castingPd.setPlayerType (PlayerType.HUMAN);
		castingPd.setPlayerID (7);
		
		final PlayerServerDetails castingPlayer = new PlayerServerDetails (castingPd, null, null, null, null); 

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		
		// Set up test object
		final SpellProcessingImpl proc = new SpellProcessingImpl ();

		// Run test
		assertThrows (MomException.class, () ->
		{
			proc.castCombatNow (castingPlayer, null, null, null, spell, 10, 20, null, combatLocation, defendingPlayer, attackingPlayer, null, null, false, mom);
		});
	}
	
	/**
	 * Tests the castCombatNow method on a combat enchantment, like prayer
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastCombatNow_CombatEnchantment () throws Exception
	{
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// Server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Spell to cast
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.COMBAT_ENCHANTMENTS);
		spell.setCombatCastingCost (22);
		
		// It grants one of 5 possible effects
		final List<String> combatAreaEffectIDs = new ArrayList<String> ();
		for (int n = 1; n <= 5; n++)
			combatAreaEffectIDs.add ("CSE00" + n);
		
		final MemoryCombatAreaEffectUtils caeUtils = mock (MemoryCombatAreaEffectUtils.class);
		when (caeUtils.listCombatEffectsNotYetCastAtLocation (trueMap.getCombatAreaEffect (), spell,
			7, new MapCoordinates3DEx (15, 25, 1))).thenReturn (combatAreaEffectIDs);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.COMBAT_ENCHANTMENTS);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (15, 25, 1);
		
		// Players involved
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (7);

		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge (); 
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		players.add (attackingPlayer);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (null, null, null, null, null);
		players.add (defendingPlayer);
		
		final PlayerServerDetails castingPlayer = attackingPlayer;
		
		// Wizard
		final KnownWizardDetails attackingWizard = new KnownWizardDetails ();

		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), attackingPd.getPlayerID (), "castCombatNow")).thenReturn (attackingWizard);
		
		// Combat details
		final CombatMapServerUtils combatMapServerUtils = mock (CombatMapServerUtils.class);
		final List<CombatDetails> combatList = new ArrayList<CombatDetails> ();
		
		final CombatDetails combatDetails = new CombatDetails (1, new MapCoordinates3DEx (combatLocation), null, 1, 2, null, null, 0, 0, 0, 0);
		when (combatMapServerUtils.findCombatByLocation (combatList, new MapCoordinates3DEx (combatLocation), "castCombatNow")).thenReturn (combatDetails);
		combatDetails.setAttackerCastingSkillRemaining (45);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getCombatDetails ()).thenReturn (combatList);
		
		// Pick the 4th effect
		final RandomUtils randomUtils = mock (RandomUtils.class);
		when (randomUtils.nextInt (5)).thenReturn (3);
		
		// Counter magic
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.getUnmodifiedCombatCastingCost (spell, null, attackingWizard.getPick ())).thenReturn (15);
		
		final SpellDispelling spellDispelling = mock (SpellDispelling.class);
		when (spellDispelling.processCountering (castingPlayer, spell, 15, combatLocation, defendingPlayer, attackingPlayer,
			null, null, mom)).thenReturn (true);
		
		// Set up test object
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		final ServerResourceCalculations serverResourceCalc = mock (ServerResourceCalculations.class);
		
		final SpellProcessingImpl proc = new SpellProcessingImpl ();
		proc.setRandomUtils (randomUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setServerResourceCalculations (serverResourceCalc);
		proc.setMemoryCombatAreaEffectUtils (caeUtils);
		proc.setSpellUtils (spellUtils);
		proc.setSpellDispelling (spellDispelling);
		proc.setKindOfSpellUtils (kindOfSpellUtils);
		proc.setKnownWizardUtils (knownWizardUtils);
		proc.setCombatMapServerUtils (combatMapServerUtils);

		// Run test
		proc.castCombatNow (castingPlayer, null, null, null, spell, 10, 20, null, combatLocation, defendingPlayer, attackingPlayer, null, null, false, mom);
		
		// Prove right effect was added
		verify (midTurn).addCombatAreaEffectOnServerAndClients (gsk, "CSE004", "SP001", attackingPd.getPlayerID (), 22, combatLocation, players, sd);
		
		// We were charged MP for it
		verify (resourceValueUtils).addToAmountStored (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -20);
		
		// We were charged skill for it
		assertEquals (35, combatDetails.getAttackerCastingSkillRemaining ());
		verify (serverResourceCalc).sendGlobalProductionValues (attackingPlayer, 35, true);
		
		// Can't cast another
		assertTrue (combatDetails.isSpellCastThisCombatTurn ());
		
		verifyNoMoreInteractions (midTurn);
		verifyNoMoreInteractions (resourceValueUtils);
		verifyNoMoreInteractions (serverResourceCalc);
	}
	
	/**
	 * Tests the castCombatNow method on a unit enchantment, like holy weapon
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastCombatNow_UnitEnchantment () throws Exception
	{
		// Database, session description and so on
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Unit we're casting it on
		final MemoryUnit targetUnit = new MemoryUnit ();
		targetUnit.setUnitURN (101);
		targetUnit.setCombatPosition (new MapCoordinates2DEx (3, 4));
		
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		// Spell to cast
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		
		// It grants one of 5 possible effects
		final List<UnitSpellEffect> effects = new ArrayList<UnitSpellEffect> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSpellEffect effect = new UnitSpellEffect ();
			effect.setUnitSkillID ("CSE00" + n);
			effects.add (effect);
		}
		
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		when (memoryMaintainedSpellUtils.listUnitSpellEffectsNotYetCastOnUnit (trueMap.getMaintainedSpell (), spell, 7, targetUnit.getUnitURN ())).thenReturn (effects);

		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_ENCHANTMENTS);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (15, 25, 1);
		
		// Combat map
		final MapAreaOfCombatTiles combatMap = createCombatMap ();

		// Players involved
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (7);

		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge (); 
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		players.add (attackingPlayer);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (null, null, null, null, null);
		players.add (defendingPlayer);
		
		final PlayerServerDetails castingPlayer = attackingPlayer;

		// Wizard
		final KnownWizardDetails attackingWizard = new KnownWizardDetails ();

		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), attackingPd.getPlayerID (), "castCombatNow")).thenReturn (attackingWizard);
		
		// Combat details
		final CombatMapServerUtils combatMapServerUtils = mock (CombatMapServerUtils.class);
		final List<CombatDetails> combatList = new ArrayList<CombatDetails> ();
		
		final CombatDetails combatDetails = new CombatDetails (1, new MapCoordinates3DEx (combatLocation), combatMap, 1, 2, null, null, 0, 0, 0, 0);
		when (combatMapServerUtils.findCombatByLocation (combatList, new MapCoordinates3DEx (combatLocation), "castCombatNow")).thenReturn (combatDetails);
		combatDetails.setAttackerCastingSkillRemaining (45);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getCombatDetails ()).thenReturn (combatList);
		
		// Pick the 4th effect
		final RandomUtils randomUtils = mock (RandomUtils.class);
		when (randomUtils.nextInt (5)).thenReturn (3);
		
		// Counter magic
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.getUnmodifiedCombatCastingCost (spell, null, attackingWizard.getPick ())).thenReturn (15);
		
		final SpellDispelling spellDispelling = mock (SpellDispelling.class);
		when (spellDispelling.processCountering (castingPlayer, spell, 15, combatLocation, defendingPlayer, attackingPlayer,
			null, null, mom)).thenReturn (true);
		
		// All cells are passable
		final MovementUtils movementUtils = mock (MovementUtils.class);
		
		// Set up test object
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		final ServerResourceCalculations serverResourceCalc = mock (ServerResourceCalculations.class);
		
		final SpellProcessingImpl proc = new SpellProcessingImpl ();
		proc.setRandomUtils (randomUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setServerResourceCalculations (serverResourceCalc);
		proc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		proc.setSpellUtils (spellUtils);
		proc.setSpellDispelling (spellDispelling);
		proc.setKindOfSpellUtils (kindOfSpellUtils);
		proc.setExpandUnitDetails (expand);
		proc.setMovementUtils (movementUtils);
		proc.setKnownWizardUtils (knownWizardUtils);
		proc.setCombatMapServerUtils (combatMapServerUtils);

		// Run test
		proc.castCombatNow (castingPlayer, null, null, null, spell, 10, 20, null, combatLocation, defendingPlayer, attackingPlayer, targetUnit, null, false, mom);
		
		// Prove right effect was added
		verify (midTurn).addMaintainedSpellOnServerAndClients (attackingPd.getPlayerID (), "SP001", targetUnit.getUnitURN (),
			"CSE004", true, null, null, null, false, true, mom);
		
		// We were charged MP for it
		verify (resourceValueUtils).addToAmountStored (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -20);
		
		// We were charged skill for it
		assertEquals (35, combatDetails.getAttackerCastingSkillRemaining ());
		verify (serverResourceCalc).sendGlobalProductionValues (attackingPlayer, 35, true);
		
		// Can't cast another
		assertTrue (combatDetails.isSpellCastThisCombatTurn ());
		
		verifyNoMoreInteractions (midTurn);
		verifyNoMoreInteractions (resourceValueUtils);
		verifyNoMoreInteractions (serverResourceCalc);
	}
	
	/**
	 * Tests the castCombatNow method summoning a unit in combat, like phantom warriors
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastCombatNow_Summoning () throws Exception
	{
		// Empty mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final CombatMapSize combatMapSize = createCombatMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setCombatMapSize (combatMapSize);
		
		// Server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Spell to cast
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		
		// It summons one of 5 possible units
		for (int n = 1; n <= 5; n++)
			spell.getSummonedUnit ().add ("UN00" + n);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.SUMMONING);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (15, 25, 1);
		
		// Combat map
		final MapAreaOfCombatTiles combatMap = createCombatMap ();

		// Players involved
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (7);

		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge (); 
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		players.add (attackingPlayer);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (null, null, null, null, null);
		players.add (defendingPlayer);
		
		final PlayerServerDetails castingPlayer = attackingPlayer;

		// Wizard
		final KnownWizardDetails attackingWizard = new KnownWizardDetails ();

		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), attackingPd.getPlayerID (), "castCombatNow")).thenReturn (attackingWizard);
		
		// Combat details
		final CombatMapServerUtils combatMapServerUtils = mock (CombatMapServerUtils.class);
		final List<CombatDetails> combatList = new ArrayList<CombatDetails> ();
		
		final CombatDetails combatDetails = new CombatDetails (1, new MapCoordinates3DEx (combatLocation), combatMap, 1, 2, null, null, 0, 0, 0, 0);
		when (combatMapServerUtils.findCombatByLocation (combatList, new MapCoordinates3DEx (combatLocation), "castCombatNow")).thenReturn (combatDetails);
		combatDetails.setAttackerCastingSkillRemaining (45);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getCombatDetails ()).thenReturn (combatList);
		
		// Pick the 4th unit
		final RandomUtils randomUtils = mock (RandomUtils.class);
		when (randomUtils.nextInt (5)).thenReturn (3);
		
		// Attacker is attacking from 1 cell away
		final MapCoordinates3DEx attackingFrom = new MapCoordinates3DEx (16, 25, 1);

		final OverlandMapServerUtils overlandMapServerUtils = mock (OverlandMapServerUtils.class);
		when (overlandMapServerUtils.findMapLocationOfUnitsInCombat (combatLocation, UnitCombatSideID.ATTACKER, trueMap.getUnit ())).thenReturn (attackingFrom);
		
		// Position on the combat field where we clicked to summon the unit
		final MapCoordinates2DEx targetLocation = new MapCoordinates2DEx (9, 7);
		
		// Mock the creation of the unit
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final MemoryUnit summonedUnit = new MemoryUnit ();
		summonedUnit.setUnitID ("UN004");
		
		when (midTurn.addUnitOnServerAndClients ("UN004", attackingFrom, null, null,
			combatLocation, attackingPlayer, UnitStatusID.ALIVE, true, mom)).thenReturn (summonedUnit);
		
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (summonedUnit, null, null, null, players, trueMap, db)).thenReturn (xu);
		
		// Mock unit speed
		when (xu.getMovementSpeed ()).thenReturn (49);
		
		// Position where its actually going to appear
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		final MapCoordinates2DEx adjustedTargetLocation = new MapCoordinates2DEx (10, 7);
		
		when (unitServerUtils.findFreeCombatPositionAvoidingInvisibleClosestTo (xu, combatLocation, combatMap, targetLocation,
			trueMap.getUnit (), combatMapSize, db)).thenReturn (adjustedTargetLocation);
		
		// Counter magic
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.getUnmodifiedCombatCastingCost (spell, null, attackingWizard.getPick ())).thenReturn (15);
		
		final SpellDispelling spellDispelling = mock (SpellDispelling.class);
		when (spellDispelling.processCountering (castingPlayer, spell, 15, combatLocation, defendingPlayer, attackingPlayer,
			null, null, mom)).thenReturn (true);
		
		// Set up test object
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		final ServerResourceCalculations serverResourceCalc = mock (ServerResourceCalculations.class);
		final CombatProcessing combatProcessing = mock (CombatProcessing.class);
		
		final SpellProcessingImpl proc = new SpellProcessingImpl ();
		proc.setRandomUtils (randomUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setResourceValueUtils (resourceValueUtils);
		proc.setServerResourceCalculations (serverResourceCalc);
		proc.setOverlandMapServerUtils (overlandMapServerUtils);
		proc.setCombatProcessing (combatProcessing);
		proc.setExpandUnitDetails (expand);
		proc.setSpellUtils (spellUtils);
		proc.setSpellDispelling (spellDispelling);
		proc.setUnitServerUtils (unitServerUtils);
		proc.setKindOfSpellUtils (kindOfSpellUtils);
		proc.setKnownWizardUtils (knownWizardUtils);
		proc.setCombatMapServerUtils (combatMapServerUtils);
		
		// Run test
		proc.castCombatNow (castingPlayer, null, null, null, spell, 10, 20, null, combatLocation, defendingPlayer, attackingPlayer, null, targetLocation, false, mom);
		
		// Prove unit was summoned
		verify (combatProcessing).setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, summonedUnit, combatLocation, combatLocation,
			adjustedTargetLocation, 8, UnitCombatSideID.ATTACKER, "SP001", mom);
		
		// We were charged MP for it
		verify (resourceValueUtils).addToAmountStored (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -20);
		
		// We were charged skill for it
		assertEquals (35, combatDetails.getAttackerCastingSkillRemaining ());
		verify (serverResourceCalc).sendGlobalProductionValues (attackingPlayer, 35, true);
		
		// Can't cast another
		assertTrue (combatDetails.isSpellCastThisCombatTurn ());
		
		// Check values on unit
		assertEquals (98, summonedUnit.getDoubleCombatMovesLeft ().intValue ());
		assertTrue (summonedUnit.isWasSummonedInCombat ());

		verifyNoMoreInteractions (combatProcessing);
		verifyNoMoreInteractions (resourceValueUtils);
		verifyNoMoreInteractions (serverResourceCalc);
	}
}