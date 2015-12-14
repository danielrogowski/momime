package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellHasCombatEffect;
import momime.common.database.SummonedUnit;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageSpell;
import momime.common.messages.NewTurnMessageSummonUnit;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.UnitAddBumpTypeID;
import momime.common.messages.UnitStatusID;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.UnitSkillUtils;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.SpellSvr;
import momime.server.database.UnitSvr;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.utils.UnitAddLocation;
import momime.server.utils.UnitServerUtils;

/**
 * Tests the SpellProcessingImpl class
 */
public final class TestSpellProcessingImpl
{
	/**
	 * Tests the castOverlandNow spell casting a spell that we haven't researched yet
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testCastOverlandNow_Unavailable () throws Exception
	{
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);

		// Spell to cast
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellID ("SP001");
		
		// Isn't researched yet
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		
		final SpellUtils utils = mock (SpellUtils.class);
		when (utils.findSpellResearchStatus (priv3.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		when (utils.getModifiedSectionID (spell, researchStatus.getStatus (), true)).thenReturn (SpellBookSectionID.RESEARCHABLE_NOW);		// Can research, but don't know it yet
		
		// Set up test object
		final SpellProcessingImpl proc = new SpellProcessingImpl ();
		proc.setSpellUtils (utils);

		// Run test
		proc.castOverlandNow (null, player3, spell, null, null, null, null);
	}

	/**
	 * Tests the castOverlandNow spell casting an overland enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_OverlandEnchantment () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();

		// Maintained spell list
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);

		// Human player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (5);
		pd1.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge trans1 = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, null, null, trans1);
		players.add (player1);

		// AI player
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (-1);
		pd2.setHuman (false);

		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, null, null, null);
		players.add (player2);

		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, trans3);
		players.add (player3);

		// Spell to cast
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellID ("SP001");
		
		// It grants one of 5 possible effects
		for (int n = 1; n <= 5; n++)
		{
			final SpellHasCombatEffect effect = new SpellHasCombatEffect ();
			effect.setCombatAreaEffectID ("CSE00" + n);
			spell.getSpellHasCombatEffect ().add (effect);
		}
		
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
		
		final SpellProcessingImpl proc = new SpellProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setSpellUtils (utils);
		proc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		proc.setRandomUtils (randomUtils);

		// Run test
		proc.castOverlandNow (gsk, player3, spell, null, players, db, sd);
		
		// Mocked method handles adding the spell to the true map, player's memories and sending the network msgs, so don't need to worry about any of that
		verify (midTurn, times (1)).addMaintainedSpellOnServerAndClients (gsk, pd3.getPlayerID (), "SP001", null, null, false, null, null, players, db, sd);
		
		// CAE should get added also
		verify (midTurn, times (1)).addCombatAreaEffectOnServerAndClients (gsk, "CSE004", "SP001", pd3.getPlayerID (), null, players, db, sd);
		
		// Human players won't get any NTMs about it
		assertEquals (0, trans1.getNewTurnMessage ().size ());
		assertEquals (0, trans3.getNewTurnMessage ().size ());
	}

	/**
	 * Tests the castOverlandNow spell casting an overland enchantment that we already have
	 * This isn't an error - just nothing happens
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_OverlandEnchantment_AlreadyExists () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();

		// Maintained spell list
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);

		// Human player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (5);
		pd1.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge trans1 = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, null, null, trans1);
		players.add (player1);

		// AI player
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (-1);
		pd2.setHuman (false);

		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, null, null, null);
		players.add (player2);

		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, trans3);
		players.add (player3);

		// Spell to cast
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellID ("SP001");
		
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

		// Run test
		proc.castOverlandNow (gsk, player3, spell, null, players, db, sd);
		
		// So this shouldn't happen
		verify (midTurn, times (0)).addMaintainedSpellOnServerAndClients (gsk, pd3.getPlayerID (), "SP158", null, null, false, null, null, players, db, sd);
		
		// CAE shouldn't be added either
		verify (midTurn, times (0)).addCombatAreaEffectOnServerAndClients (gsk, "CSE158", null, pd3.getPlayerID (), null, players, db, sd);
		
		// Human players won't get any NTMs about it
		assertEquals (0, trans1.getNewTurnMessage ().size ());
		assertEquals (0, trans3.getNewTurnMessage ().size ());
	}

	/**
	 * Tests the castOverlandNow spell casting a creature summoning spell overland
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_Summon_Creature () throws Exception
	{
		// Mock database
		final UnitSvr unitDef = new UnitSvr ();
		unitDef.setUnitMagicRealm ("LT01");
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnit ("UN001", "castOverlandNow")).thenReturn (unitDef);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();

		// General server knowledge
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);

		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, trans3);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player3);
		
		// Summoning circle location
		final MapCoordinates3DEx summoningCircleLocation = new MapCoordinates3DEx (15, 25, 0);
		
		final MemoryBuilding summoningCircle = new MemoryBuilding ();
		summoningCircle.setCityLocation (summoningCircleLocation);
		
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findCityWithBuilding (7, CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE,
			trueMap.getMap (), trueMap.getBuilding ())).thenReturn (summoningCircle);

		// Spell to cast
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellID ("SP001");
		
		// It only summons 1 kind of unit
		final SummonedUnit summonedUnit = new SummonedUnit ();
		summonedUnit.setSummonedUnitID ("UN001");
		spell.getSummonedUnit ().add (summonedUnit);
		
		// Fix random results
		final RandomUtils randomUtils = mock (RandomUtils.class);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils utils = mock (SpellUtils.class);
		when (utils.findSpellResearchStatus (priv3.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		when (utils.getModifiedSectionID (spell, researchStatus.getStatus (), true)).thenReturn (SpellBookSectionID.SUMMONING);
		
		// Will the unit fit in the city?
		final UnitAddLocation addLocation = new UnitAddLocation (summoningCircleLocation, UnitAddBumpTypeID.CITY);
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.findNearestLocationWhereUnitCanBeAdded (summoningCircleLocation, "UN001", 7, trueMap, sd, db)).thenReturn (addLocation);
		
		// Mock creation of the unit
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		when (midTurn.addUnitOnServerAndClients (gsk, "UN001", summoningCircleLocation, summoningCircleLocation, null,
			player3, UnitStatusID.ALIVE, players, sd, db)).thenReturn (new MemoryUnit ());
		
		// Set up test object		
		final SpellProcessingImpl proc = new SpellProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setSpellUtils (utils);
		proc.setMemoryBuildingUtils (memoryBuildingUtils);
		proc.setRandomUtils (randomUtils);
		proc.setUnitServerUtils (unitServerUtils);
		proc.setUnitSkillUtils (mock (UnitSkillUtils.class));

		// Run test
		proc.castOverlandNow (gsk, player3, spell, null, players, db, sd);
		
		// Prove that unit got added
		verify (midTurn, times (1)).addUnitOnServerAndClients (gsk, "UN001", summoningCircleLocation, summoningCircleLocation, null, player3,
			UnitStatusID.ALIVE, players, sd, db);
		
		// Casting player gets the "You have summoned Hell Hounds!" new turn message popup
		assertEquals (1, trans3.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.SUMMONED_UNIT, trans3.getNewTurnMessage ().get (0).getMsgType ());
		final NewTurnMessageSummonUnit ntm = (NewTurnMessageSummonUnit) trans3.getNewTurnMessage ().get (0);
		assertEquals ("SP001", ntm.getSpellID ());
		assertEquals ("UN001", ntm.getUnitID ());
		assertEquals (summoningCircleLocation, ntm.getCityLocation ());
		assertEquals (UnitAddBumpTypeID.CITY, ntm.getUnitAddBumpType ());
	}

	/**
	 * Tests the castOverlandNow spell casting a hero summoning spell overland
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_Summon_Hero () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();

		// General server knowledge
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);

		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, trans3);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player3);
		
		// Summoning circle location
		final MapCoordinates3DEx summoningCircleLocation = new MapCoordinates3DEx (15, 25, 0);
		
		final MemoryBuilding summoningCircle = new MemoryBuilding ();
		summoningCircle.setCityLocation (summoningCircleLocation);
		
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findCityWithBuilding (7, CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE,
			trueMap.getMap (), trueMap.getBuilding ())).thenReturn (summoningCircle);

		// Spell to cast
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellID ("SP001");
		
		// Lets say there's 9 possible heroes we could get
		for (int n = 1; n <= 9; n++)
		{
			final UnitSvr unitDef = new UnitSvr ();
			unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
			when (db.findUnit ("UN00" + n, "castOverlandNow")).thenReturn (unitDef);
			
			final SummonedUnit summonedUnit = new SummonedUnit ();
			summonedUnit.setSummonedUnitID ("UN00" + n);
			spell.getSummonedUnit ().add (summonedUnit);
		}
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils utils = mock (SpellUtils.class);
		when (utils.findSpellResearchStatus (priv3.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		when (utils.getModifiedSectionID (spell, researchStatus.getStatus (), true)).thenReturn (SpellBookSectionID.SUMMONING);
		
		// Will the unit fit in the city?
		final UnitAddLocation addLocation = new UnitAddLocation (summoningCircleLocation, UnitAddBumpTypeID.CITY);
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.findNearestLocationWhereUnitCanBeAdded (summoningCircleLocation, "UN008", 7, trueMap, sd, db)).thenReturn (addLocation);
		
		// Heroes already exist in the units list, but lets say 1 we've already summoned, and another we've already summoned and got them killed
		MemoryUnit theHero = null;
		for (int n = 1; n <= 9; n++)
		{
			final MemoryUnit hero = new MemoryUnit ();
			if (n == 3)
				hero.setStatus (UnitStatusID.ALIVE);
			else if (n == 6)
				hero.setStatus (UnitStatusID.DEAD);
			else
				hero.setStatus (UnitStatusID.GENERATED);
			
			if (n == 8)
				theHero = hero;		// The one we'll actually summon
			
			when (unitServerUtils.findUnitWithPlayerAndID (trueMap.getUnit (), 7, "UN00" + n)).thenReturn (hero);				
		}

		// Fix random results
		final RandomUtils randomUtils = mock (RandomUtils.class);
		when (randomUtils.nextInt (7)).thenReturn (5);
		
		// Set up test object
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		
		final SpellProcessingImpl proc = new SpellProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setSpellUtils (utils);
		proc.setMemoryBuildingUtils (memoryBuildingUtils);
		proc.setRandomUtils (randomUtils);
		proc.setUnitServerUtils (unitServerUtils);
		proc.setUnitSkillUtils (mock (UnitSkillUtils.class));

		// Run test
		proc.castOverlandNow (gsk, player3, spell, null, players, db, sd);
		
		// Prove that unit got updated, not added
		verify (midTurn, times (0)).addUnitOnServerAndClients (gsk, "UN008", summoningCircleLocation, summoningCircleLocation, null, player3,
			UnitStatusID.ALIVE, players, sd, db);
		verify (midTurn, times (1)).updateUnitStatusToAliveOnServerAndClients (theHero, summoningCircleLocation, player3, players, trueMap, sd, db);
		
		// Casting player gets the "You have summoned Hell Hounds!" new turn message popup
		assertEquals (1, trans3.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.SUMMONED_UNIT, trans3.getNewTurnMessage ().get (0).getMsgType ());
		final NewTurnMessageSummonUnit ntm = (NewTurnMessageSummonUnit) trans3.getNewTurnMessage ().get (0);
		assertEquals ("SP001", ntm.getSpellID ());
		assertEquals ("UN008", ntm.getUnitID ());
		assertEquals (summoningCircleLocation, ntm.getCityLocation ());
		assertEquals (UnitAddBumpTypeID.CITY, ntm.getUnitAddBumpType ());
	}
	
	/**
	 * Tests the castOverlandNow spell casting a summoning spell overland, except that we're banished and have no summoning circle
	 * This isn't an error - just nothing happens
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_Summoning_NoCircle () throws Exception
	{
		// Mock database
		final UnitSvr unitDef = new UnitSvr ();
		unitDef.setUnitMagicRealm ("LT01");
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnit ("UN001", "castOverlandNow")).thenReturn (unitDef);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();

		// General server knowledge
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);

		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, trans3);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player3);
		
		// Summoning circle location
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findCityWithBuilding (7, CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE,
			trueMap.getMap (), trueMap.getBuilding ())).thenReturn (null);		// <---

		// Spell to cast
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellID ("SP001");
		
		// It only summons 1 kind of unit
		final SummonedUnit summonedUnit = new SummonedUnit ();
		summonedUnit.setSummonedUnitID ("UN001");
		spell.getSummonedUnit ().add (summonedUnit);
		
		// We know the spell
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils utils = mock (SpellUtils.class);
		when (utils.findSpellResearchStatus (priv3.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		when (utils.getModifiedSectionID (spell, researchStatus.getStatus (), true)).thenReturn (SpellBookSectionID.SUMMONING);
		
		// Set up test object
		final SpellProcessingImpl proc = new SpellProcessingImpl ();
		proc.setSpellUtils (utils);
		proc.setMemoryBuildingUtils (memoryBuildingUtils);

		// Run test
		proc.castOverlandNow (gsk, player3, spell, null, players, db, sd);
		
		// Casting player gets no message
		assertEquals (0, trans3.getNewTurnMessage ().size ());
	}

	/**
	 * Tests the castOverlandNow spell casting a unit enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandNow_UnitEnchantment () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();

		// General server knowledge
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);

		// Human player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, trans3);
		players.add (player3);

		// Spell to cast
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellID ("SP001");
		
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

		// Run test
		proc.castOverlandNow (gsk, player3, spell, null, players, db, sd);
		
		// Check we told human player to pick a target
		assertEquals (1, trans3.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.TARGET_SPELL, trans3.getNewTurnMessage ().get (0).getMsgType ());
		final NewTurnMessageSpell ntm = (NewTurnMessageSpell) trans3.getNewTurnMessage ().get (0);
		assertEquals ("SP001", ntm.getSpellID ());

		// Check that we recorded targetless spell on server.
		// NB. players (arg just before 'db') intentionally null so that spell only added on server.
		verify (midTurn).addMaintainedSpellOnServerAndClients (gsk, pd3.getPlayerID ().intValue (), "SP001", null, null, false, null, null, null, db, sd);
	}
	
	/**
	 * Tests trying to cast a spell into a combat we aren't participating in
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testCastCombatNow_NotParticipating () throws Exception
	{
		// Spell to cast
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellID ("SP001");
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (15, 25, 1);

		// Players involved
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (null, null, null, null, null); 
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (null, null, null, null, null);
		
		final PlayerDescription castingPd = new PlayerDescription ();
		castingPd.setHuman (true);
		castingPd.setPlayerID (7);
		
		final PlayerServerDetails castingPlayer = new PlayerServerDetails (castingPd, null, null, null, null); 
		
		// Set up test object
		final SpellProcessingImpl proc = new SpellProcessingImpl ();

		// Run test
		proc.castCombatNow (castingPlayer, null, null, null, spell, 10, 20, null, combatLocation, defendingPlayer, attackingPlayer, null, null, null);
	}
	
	/**
	 * Tests the castCombatNow method on a combat enchantment, like prayer
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastCombatNow_CombatEnchantment () throws Exception
	{
		// Database, session description and so on
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// Server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		// Spell to cast
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.COMBAT_ENCHANTMENTS);
		
		// It grants one of 5 possible effects
		final List<String> combatAreaEffectIDs = new ArrayList<String> ();
		for (int n = 1; n <= 5; n++)
			combatAreaEffectIDs.add ("CSE00" + n);
		
		final MemoryCombatAreaEffectUtils caeUtils = mock (MemoryCombatAreaEffectUtils.class);
		when (caeUtils.listCombatEffectsNotYetCastAtLocation (trueMap.getCombatAreaEffect (), spell,
			7, new MapCoordinates3DEx (15, 25, 1))).thenReturn (combatAreaEffectIDs);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (15, 25, 1);
		
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (25).getCell ().get (15);
		gc.setCombatAttackerCastingSkillRemaining (45);

		// Players involved
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (7);

		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge (); 
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		players.add (attackingPlayer);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (null, null, null, null, null);
		players.add (defendingPlayer);
		
		final PlayerServerDetails castingPlayer = attackingPlayer;

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
		// Pick the 4th effect
		final RandomUtils randomUtils = mock (RandomUtils.class);
		when (randomUtils.nextInt (5)).thenReturn (3);
		
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

		// Run test
		proc.castCombatNow (castingPlayer, null, null, null, spell, 10, 20, null, combatLocation, defendingPlayer, attackingPlayer, null, null, mom);
		
		// Prove right effect was added
		verify (midTurn, times (1)).addCombatAreaEffectOnServerAndClients (gsk, "CSE004", "SP001", attackingPd.getPlayerID (), combatLocation, players, db, sd);
		
		// We were charged MP for it
		verify (resourceValueUtils, times (1)).addToAmountStored (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -20);
		
		// We were charged skill for it
		assertEquals (35, gc.getCombatAttackerCastingSkillRemaining ().intValue ());
		verify (serverResourceCalc, times (1)).sendGlobalProductionValues (attackingPlayer, 35);
		
		// Can't cast another
		assertTrue (gc.isSpellCastThisCombatTurn ());
	}
	
	/**
	 * Tests the castCombatNow method on a unit enchantment, like holy weapon
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastCombatNow_UnitEnchantment () throws Exception
	{
		// Database, session description and so on
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// Server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		// Unit we're casting it on
		final MemoryUnit targetUnit = new MemoryUnit ();
		targetUnit.setUnitURN (101);
		
		// Spell to cast
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		
		// It grants one of 5 possible effects
		final List<String> effects = new ArrayList<String> ();
		for (int n = 1; n <= 5; n++)
			effects.add ("CSE00" + n);
		
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		when (memoryMaintainedSpellUtils.listUnitSpellEffectsNotYetCastOnUnit (trueMap.getMaintainedSpell (), spell, 7, targetUnit.getUnitURN ())).thenReturn (effects);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (15, 25, 1);
		
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (25).getCell ().get (15);
		gc.setCombatAttackerCastingSkillRemaining (45);

		// Players involved
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (7);

		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge (); 
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		players.add (attackingPlayer);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (null, null, null, null, null);
		players.add (defendingPlayer);
		
		final PlayerServerDetails castingPlayer = attackingPlayer;

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
		// Pick the 4th effect
		final RandomUtils randomUtils = mock (RandomUtils.class);
		when (randomUtils.nextInt (5)).thenReturn (3);
		
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

		// Run test
		proc.castCombatNow (castingPlayer, null, null, null, spell, 10, 20, null, combatLocation, defendingPlayer, attackingPlayer, targetUnit, null, mom);
		
		// Prove right effect was added
		verify (midTurn, times (1)).addMaintainedSpellOnServerAndClients (gsk, attackingPd.getPlayerID (), "SP001", targetUnit.getUnitURN (),
			"CSE004", true, null, null, players, db, sd);
		
		// We were charged MP for it
		verify (resourceValueUtils, times (1)).addToAmountStored (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -20);
		
		// We were charged skill for it
		assertEquals (35, gc.getCombatAttackerCastingSkillRemaining ().intValue ());
		verify (serverResourceCalc, times (1)).sendGlobalProductionValues (attackingPlayer, 35);
		
		// Can't cast another
		assertTrue (gc.isSpellCastThisCombatTurn ());
	}
	
	/**
	 * Tests the castCombatNow method summoning a unit in combat, like phantom warriors
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastCombatNow_Summoning () throws Exception
	{
		// Empty mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// Server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);
		
		// Spell to cast
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		
		// It summons one of 5 possible units
		for (int n = 1; n <= 5; n++)
		{
			final SummonedUnit summons = new SummonedUnit ();
			summons.setSummonedUnitID ("UN00" + n);
			spell.getSummonedUnit ().add (summons);
		}
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (15, 25, 1);
		
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (25).getCell ().get (15);
		gc.setCombatAttackerCastingSkillRemaining (45);

		// Players involved
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (7);

		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge (); 
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		players.add (attackingPlayer);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (null, null, null, null, null);
		players.add (defendingPlayer);
		
		final PlayerServerDetails castingPlayer = attackingPlayer;

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
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
		
		when (midTurn.addUnitOnServerAndClients (gsk, "UN004", attackingFrom, attackingFrom,
			combatLocation, attackingPlayer, UnitStatusID.ALIVE, players, sd, db)).thenReturn (summonedUnit);
		
		// Mock unit speed
		final UnitSkillUtils unitSkillUtils = mock (UnitSkillUtils.class);
		when (unitSkillUtils.getModifiedSkillValue (summonedUnit, summonedUnit.getUnitHasSkill (),
			CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
			players, trueMap, db)).thenReturn (49);
		
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
		proc.setUnitSkillUtils (unitSkillUtils);
		
		// Run test
		proc.castCombatNow (castingPlayer, null, null, null, spell, 10, 20, null, combatLocation, defendingPlayer, attackingPlayer, null, targetLocation, mom);
		
		// Prove unit was summoned
		verify (combatProcessing, times (1)).setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, summonedUnit, combatLocation, combatLocation,
			targetLocation, 8, UnitCombatSideID.ATTACKER, "SP001", db);
		
		// We were charged MP for it
		verify (resourceValueUtils, times (1)).addToAmountStored (attackingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -20);
		
		// We were charged skill for it
		assertEquals (35, gc.getCombatAttackerCastingSkillRemaining ().intValue ());
		verify (serverResourceCalc, times (1)).sendGlobalProductionValues (attackingPlayer, 35);
		
		// Can't cast another
		assertTrue (gc.isSpellCastThisCombatTurn ());
		
		// Check values on unit
		assertEquals (98, summonedUnit.getDoubleCombatMovesLeft ().intValue ());
		assertTrue (summonedUnit.isWasSummonedInCombat ());
	}
	
	/**
	 * Tests the switchOffSpell method on a spell book section with no special processing (a unit enchantment)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSwitchOffSpell_UnitEnchantment () throws Exception
	{
		// Mock database
		final SpellSvr spell = new SpellSvr ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findSpell ("SP001", "switchOffSpell")).thenReturn (spell);
		
		// Other setup
		final MomSessionDescription sd = new MomSessionDescription ();
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		// Players
		final PlayerDescription castingPd = new PlayerDescription ();
		castingPd.setHuman (true);
		castingPd.setPlayerID (7);

		final MomPersistentPlayerPrivateKnowledge castingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails castingPlayer = new PlayerServerDetails (castingPd, null, castingPriv, null, null);

		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (castingPlayer);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, castingPd.getPlayerID (), "switchOffSpell")).thenReturn (castingPlayer);
		
		// Research status
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (castingPriv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		when (spellUtils.getModifiedSectionID (spell, researchStatus.getStatus (), true)).thenReturn (SpellBookSectionID.UNIT_ENCHANTMENTS);
		
		// The spell being switched off
		final MemoryMaintainedSpell trueSpell = new MemoryMaintainedSpell ();
		trueSpell.setSpellID ("SP001");
		trueSpell.setSpellURN (3);
		trueSpell.setCastingPlayerID (castingPd.getPlayerID ());
		
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		when (memoryMaintainedSpellUtils.findSpellURN (trueSpell.getSpellURN (), trueMap.getMaintainedSpell (), "switchOffSpell")).thenReturn (trueSpell);
		
		// Set up test object
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		
		final SpellProcessingImpl proc = new SpellProcessingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run test
		proc.switchOffSpell (trueMap, trueSpell.getSpellURN (), players, db, sd);
		
		// Check spell was switched off
		verify (midTurn, times (1)).switchOffMaintainedSpellOnServerAndClients (trueMap, trueSpell.getSpellURN (), players, db, sd);
	}

	/**
	 * Tests the switchOffSpell method to turn off an overland enchantment, where we also have to turn off the CAE
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSwitchOffSpell_OverlandEnchantment () throws Exception
	{
		// Mock database - spell grants 5 effects
		final SpellSvr spell = new SpellSvr ();
		for (int n = 1; n <= 5; n++)
		{
			final SpellHasCombatEffect effect = new SpellHasCombatEffect ();
			effect.setCombatAreaEffectID ("CSE00" + n);
			spell.getSpellHasCombatEffect ().add (effect);
		}
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findSpell ("SP001", "switchOffSpell")).thenReturn (spell);
		
		// Other setup
		final MomSessionDescription sd = new MomSessionDescription ();
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		// Players
		final PlayerDescription castingPd = new PlayerDescription ();
		castingPd.setHuman (true);
		castingPd.setPlayerID (7);

		final MomPersistentPlayerPrivateKnowledge castingPriv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails castingPlayer = new PlayerServerDetails (castingPd, null, castingPriv, null, null);

		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (castingPlayer);

		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, castingPd.getPlayerID (), "switchOffSpell")).thenReturn (castingPlayer);
		
		// Research status
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (castingPriv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		when (spellUtils.getModifiedSectionID (spell, researchStatus.getStatus (), true)).thenReturn (SpellBookSectionID.OVERLAND_ENCHANTMENTS);
		
		// One of the effects is actually cast
		final MemoryCombatAreaEffect cae = new MemoryCombatAreaEffect ();
		cae.setCombatAreaEffectURN (4);
		
		final MemoryCombatAreaEffectUtils caeUtils = mock (MemoryCombatAreaEffectUtils.class);
		when (caeUtils.findCombatAreaEffect (trueMap.getCombatAreaEffect (), null, "CSE04", 7)).thenReturn (cae);

		// The spell being switched off
		final MemoryMaintainedSpell trueSpell = new MemoryMaintainedSpell ();
		trueSpell.setSpellID ("SP001");
		trueSpell.setSpellURN (3);
		trueSpell.setCastingPlayerID (castingPd.getPlayerID ());
		
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		when (memoryMaintainedSpellUtils.findSpellURN (trueSpell.getSpellURN (), trueMap.getMaintainedSpell (), "switchOffSpell")).thenReturn (trueSpell);
		
		// Set up test object
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		
		final SpellProcessingImpl proc = new SpellProcessingImpl ();
		proc.setSpellUtils (spellUtils);
		proc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setMemoryCombatAreaEffectUtils (caeUtils);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run test
		proc.switchOffSpell (trueMap, trueSpell.getSpellURN (), players, db, sd);
		
		// Check spell was switched off
		verify (midTurn, times (1)).switchOffMaintainedSpellOnServerAndClients (trueMap, trueSpell.getSpellURN (), players, db, sd);
		verify (midTurn, times (0)).removeCombatAreaEffectFromServerAndClients (trueMap, cae.getCombatAreaEffectURN (), players, db, sd);
	}
}