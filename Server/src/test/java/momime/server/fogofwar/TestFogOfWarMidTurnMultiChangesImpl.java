package momime.server.fogofwar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.FogOfWarValue;
import momime.common.database.MapFeatureEx;
import momime.common.database.MapFeatureMagicRealm;
import momime.common.database.OverlandMapSize;
import momime.common.database.Pick;
import momime.common.database.Plane;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.MoveUnitStackOverlandMessage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.FogOfWarCalculations;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.process.PlayerMessageProcessing;
import momime.server.utils.UnitServerUtils;
import momime.server.utils.UnitSkillDirectAccess;
import momime.server.worldupdates.WorldUpdates;

/**
 * Tests the FogOfWarMidTurnMultiChangesImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestFogOfWarMidTurnMultiChangesImpl extends ServerTestData
{
	/**
	 * Tests the switchOffSpellsCastInCombat method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSwitchOffSpellsCastInCombat () throws Exception
	{
		// 3 location spells, one combat spell in the right location, one combat spell in the wrong location, one overland spell in the right location		
		final MemoryMaintainedSpell spellOne = new MemoryMaintainedSpell ();
		spellOne.setSpellURN (1);
		spellOne.setCastInCombat (true);
		spellOne.setCityLocation (new MapCoordinates3DEx (20, 11, 1));

		final MemoryMaintainedSpell spellTwo = new MemoryMaintainedSpell ();
		spellTwo.setSpellURN (2);
		spellTwo.setCastInCombat (false);
		spellTwo.setCityLocation (new MapCoordinates3DEx (20, 10, 1));

		final MemoryMaintainedSpell spellThree = new MemoryMaintainedSpell ();
		spellThree.setSpellURN (3);
		spellThree.setCastInCombat (true);
		spellThree.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.getMaintainedSpell ().add (spellOne);
		trueMap.getMaintainedSpell ().add (spellTwo);
		trueMap.getMaintainedSpell ().add (spellThree);

		// 3 unit spells, one combat spell cast on a unit in the right location, one combat spell cast on a unit in the wrong location,
		// and oneoverland spell cast on a unit in the right location,
		final MemoryMaintainedSpell spellFour = new MemoryMaintainedSpell ();
		spellFour.setSpellURN (4);
		spellFour.setCastInCombat (true);
		spellFour.setUnitURN (101);
		
		final MemoryUnit unitFour = new MemoryUnit ();
		unitFour.setUnitURN (101);
		unitFour.setCombatLocation (new MapCoordinates3DEx (20, 11, 1));

		final MemoryMaintainedSpell spellFive = new MemoryMaintainedSpell ();
		spellFive.setSpellURN (5);
		spellFive.setCastInCombat (false);
		spellFive.setUnitURN (102);

		final MemoryUnit unitFive = new MemoryUnit ();
		unitFive.setUnitURN (102);
		unitFive.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		
		final MemoryMaintainedSpell spellSix = new MemoryMaintainedSpell ();
		spellSix.setSpellURN (6);
		spellSix.setCastInCombat (true);
		spellSix.setUnitURN (103);

		final MemoryUnit unitSix = new MemoryUnit ();
		unitSix.setUnitURN (103);
		unitSix.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		
		trueMap.getMaintainedSpell ().add (spellFour);
		trueMap.getMaintainedSpell ().add (spellFive);
		trueMap.getMaintainedSpell ().add (spellSix);

		// Units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (unitFour.getUnitURN (), trueMap.getUnit (), "switchOffSpellsCastInCombat")).thenReturn (unitFour);
		when (unitUtils.findUnitURN (unitSix.getUnitURN (), trueMap.getUnit (), "switchOffSpellsCastInCombat")).thenReturn (unitSix);
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setUnitUtils (unitUtils);
		
		// Run method
		multi.switchOffSpellsCastInCombat (new MapCoordinates3DEx (20, 10, 1), mom);
		
		// Check results
		verify (wu).switchOffSpell (3);
		verify (wu).switchOffSpell (6);
		verify (wu).process (mom);
		
		verifyNoMoreInteractions (wu);
	}
	
	/**
	 * Tests the switchOffSpellsInLocationOnServerAndClients method, switching off spells for one player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSwitchOffSpellsInLocationOnServerAndClients_OnePlayer () throws Exception
	{
		// 3 spells, one in the right location for the right player, one in the right location for the wrong player, one in the wrong location for the right player
		final MemoryMaintainedSpell spellOne = new MemoryMaintainedSpell ();
		spellOne.setSpellURN (1);
		spellOne.setCastingPlayerID (4);
		spellOne.setCityLocation (new MapCoordinates3DEx (20, 10, 1));

		final MemoryMaintainedSpell spellTwo = new MemoryMaintainedSpell ();
		spellTwo.setSpellURN (2);
		spellTwo.setCastingPlayerID (3);
		spellTwo.setCityLocation (new MapCoordinates3DEx (20, 11, 1));

		final MemoryMaintainedSpell spellThree = new MemoryMaintainedSpell ();
		spellThree.setSpellURN (3);
		spellThree.setCastingPlayerID (3);
		spellThree.setCityLocation (new MapCoordinates3DEx (20, 10, 1));

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.getMaintainedSpell ().add (spellOne);
		trueMap.getMaintainedSpell ().add (spellTwo);
		trueMap.getMaintainedSpell ().add (spellThree);
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		
		// Run method
		multi.switchOffSpellsInLocationOnServerAndClients (new MapCoordinates3DEx (20, 10, 1), 3, true, mom);

		// Check results
		verify (wu).switchOffSpell (3);
		verify (wu).process (mom);
		
		verifyNoMoreInteractions (wu);
	}
	
	/**
	 * Tests the switchOffSpellsInLocationOnServerAndClients method, switching off spells for all players
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSwitchOffSpellsInLocationOnServerAndClients_AllPlayers () throws Exception
	{
		// 3 spells, one in the right location for the right player, one in the right location for the wrong player, one in the wrong location for the right player
		final MemoryMaintainedSpell spellOne = new MemoryMaintainedSpell ();
		spellOne.setSpellURN (1);
		spellOne.setCastingPlayerID (4);
		spellOne.setCityLocation (new MapCoordinates3DEx (20, 10, 1));

		final MemoryMaintainedSpell spellTwo = new MemoryMaintainedSpell ();
		spellTwo.setSpellURN (2);
		spellTwo.setCastingPlayerID (3);
		spellTwo.setCityLocation (new MapCoordinates3DEx (20, 11, 1));

		final MemoryMaintainedSpell spellThree = new MemoryMaintainedSpell ();
		spellThree.setSpellURN (3);
		spellThree.setCastingPlayerID (3);
		spellThree.setCityLocation (new MapCoordinates3DEx (20, 10, 1));

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.getMaintainedSpell ().add (spellOne);
		trueMap.getMaintainedSpell ().add (spellTwo);
		trueMap.getMaintainedSpell ().add (spellThree);
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Set up object to test
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		
		// Run method
		multi.switchOffSpellsInLocationOnServerAndClients (new MapCoordinates3DEx (20, 10, 1), 0, true, mom);

		// Check results
		verify (wu).switchOffSpell (1);		// <--
		verify (wu).switchOffSpell (3);
		verify (wu).process (mom);
		
		verifyNoMoreInteractions (wu);
	}
	
	/**
	 * Tests the destroyAllBuildingsInLocationOnServerAndClients method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDestroyAllBuildingsInLocationOnServerAndClients () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// 2 buildings, one in the right location, one in the wrong location
		final MemoryBuilding buildingOne = new MemoryBuilding ();
		buildingOne.setBuildingURN (1);
		buildingOne.setCityLocation (new MapCoordinates3DEx (20, 11, 1));

		final MemoryBuilding buildingTwo = new MemoryBuilding ();
		buildingTwo.setBuildingURN (2);
		buildingTwo.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.getBuilding ().add (buildingOne);
		trueMap.getBuilding ().add (buildingTwo);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Set up object to test
		final FogOfWarMidTurnChanges single = mock (FogOfWarMidTurnChanges.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setFogOfWarMidTurnChanges (single);
		
		// Run method
		multi.destroyAllBuildingsInLocationOnServerAndClients (trueMap, players, new MapCoordinates3DEx (20, 10, 1), sd, db);
		
		// Check results
		verify (single).destroyBuildingOnServerAndClients (trueMap, players, Arrays.asList (2), false, null, null, null, sd, db);
		
		verifyNoMoreInteractions (single);
	}
	
	/**
	 * Tests the healUnitsAndGainExperience method for all players
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHealUnitsAndGainExperience_AllPlayers () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("LTN");
		
		final Pick magicRealm = new Pick ();
		magicRealm.setHealEachTurn (true);
		magicRealm.setGainExperienceEachTurn (true);

		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		final FogOfWarSetting fogOfWarSettings = new FogOfWarSetting ();	
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		sd.setFogOfWarSetting (fogOfWarSettings);
		
		// Server memory
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		// Other lists and objects just needed for mocks
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		// Normal unit that has taken 2 kinds of damage
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final UnitSkillDirectAccess direct = mock (UnitSkillDirectAccess.class);
		
		final UnitDamage unit1dmg1 = new UnitDamage ();
		unit1dmg1.setDamageType (StoredDamageTypeID.PERMANENT);
		unit1dmg1.setDamageTaken (3);

		final UnitDamage unit1dmg2 = new UnitDamage ();
		unit1dmg2.setDamageType (StoredDamageTypeID.HEALABLE);
		unit1dmg2.setDamageTaken (2);
		
		final MemoryUnit unit1 = new MemoryUnit ();
		unit1.setUnitID ("UN001");
		unit1.setStatus (UnitStatusID.ALIVE);
		unit1.setOwningPlayerID (1);
		unit1.getUnitDamage ().add (unit1dmg1);
		unit1.getUnitDamage ().add (unit1dmg2);
		unit1.getUnitHasSkill ().add (null);	// Just to make lists unique for mocks
		when (direct.getDirectSkillValue (unit1.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)).thenReturn (10);

		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unit1, null, null, null, players, trueMap, db)).thenReturn (xu1);
		when (xu1.getModifiedUnitMagicRealmLifeformType ()).thenReturn (magicRealm);
		when (xu1.getUnitDefinition ()).thenReturn (unitDef);
		when (xu1.getFullFigureCount ()).thenReturn (6);
		when (xu1.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (4);
		
		// Summoned unit that has taken 1 kind of damage
		final UnitDamage unit2dmg1 = new UnitDamage ();
		unit2dmg1.setDamageType (StoredDamageTypeID.PERMANENT);
		unit2dmg1.setDamageTaken (3);

		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setUnitID ("UN001");
		unit2.setStatus (UnitStatusID.ALIVE);
		unit2.setOwningPlayerID (1);
		unit2.getUnitDamage ().add (unit2dmg1);
		for (int n = 0; n < 2; n++)
			unit2.getUnitHasSkill ().add (null);	// Just to make lists unique for mocks
		when (direct.getDirectSkillValue (unit2.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)).thenReturn (-1);

		final ExpandedUnitDetails xu2 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unit2, null, null, null, players, trueMap, db)).thenReturn (xu2);
		when (xu2.getModifiedUnitMagicRealmLifeformType ()).thenReturn (magicRealm);
		when (xu2.getFullFigureCount ()).thenReturn (2);
		when (xu2.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (8);
		
		// Hero that has taken no damage
		final MemoryUnit unit3 = new MemoryUnit ();
		unit3.setUnitID ("UN001");
		unit3.setStatus (UnitStatusID.ALIVE);
		unit3.setOwningPlayerID (1);
		for (int n = 0; n < 3; n++)
			unit3.getUnitHasSkill ().add (null);	// Just to make lists unique for mocks
		when (direct.getDirectSkillValue (unit3.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)).thenReturn (10);

		final ExpandedUnitDetails xu3 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unit3, null, null, null, players, trueMap, db)).thenReturn (xu3);
		when (xu3.getModifiedUnitMagicRealmLifeformType ()).thenReturn (magicRealm);
		when (xu3.getUnitDefinition ()).thenReturn (unitDef);
		
		// Summoned unit that has taken no damage
		final MemoryUnit unit4 = new MemoryUnit ();
		unit4.setUnitID ("UN001");
		unit4.setStatus (UnitStatusID.ALIVE);
		unit4.setOwningPlayerID (1);
		for (int n = 0; n < 4; n++)
			unit4.getUnitHasSkill ().add (null);	// Just to make lists unique for mocks
		when (direct.getDirectSkillValue (unit4.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)).thenReturn (-1);

		final ExpandedUnitDetails xu4 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unit4, null, null, null, players, trueMap, db)).thenReturn (xu4);
		when (xu4.getModifiedUnitMagicRealmLifeformType ()).thenReturn (magicRealm);
		
		// Dead hero
		final UnitDamage unit5dmg1 = new UnitDamage ();
		unit5dmg1.setDamageType (StoredDamageTypeID.HEALABLE);
		unit5dmg1.setDamageTaken (2);
		
		final MemoryUnit unit5 = new MemoryUnit ();
		unit5.setUnitID ("UN001");
		unit5.setStatus (UnitStatusID.DEAD);
		unit5.setOwningPlayerID (1);
		unit5.getUnitDamage ().add (unit5dmg1);
		for (int n = 0; n < 5; n++)
			unit5.getUnitHasSkill ().add (null);	// Just to make lists unique for mocks

		// Units list
		trueMap.getUnit ().add (unit1);
		trueMap.getUnit ().add (unit2);
		trueMap.getUnit ().add (unit3);
		trueMap.getUnit ().add (unit4);
		trueMap.getUnit ().add (unit5);

		// Session variables
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		// The damage list and exp lookups are more awkward to mock than if we just let it use the real methods
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setUnitServerUtils (unitServerUtils);
		multi.setUnitSkillDirectAccess (direct);
		multi.setFogOfWarMidTurnChanges (midTurn);
		multi.setExpandUnitDetails (expand);
		
		// Run method
		multi.healUnitsAndGainExperience (0, mom);
		
		// Check results
		verify (unitServerUtils).healDamage (unit1.getUnitDamage (), 2, false);		// 5% of 24 is 1.2, then round up
		verify (direct).setDirectSkillValue (unit1, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, 11);
		verify (midTurn).updatePlayerMemoryOfUnit (eq (unit1), eq (trueTerrain), eq (players), eq (db), eq (fogOfWarSettings), anyMap ());

		verify (unitServerUtils).healDamage (unit2.getUnitDamage (), 1, false);		// 5% of 16 is 0.8, then round up
		verify (midTurn).updatePlayerMemoryOfUnit (eq (unit2), eq (trueTerrain), eq (players), eq (db), eq (fogOfWarSettings), anyMap ());

		verify (direct).setDirectSkillValue (unit3, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, 11);
		verify (midTurn).updatePlayerMemoryOfUnit (eq (unit3), eq (trueTerrain), eq (players), eq (db), eq (fogOfWarSettings), anyMap ());
	
		verifyNoMoreInteractions (direct);
		verifyNoMoreInteractions (midTurn);
	}
	
	/**
	 * Tests the grantExperienceToUnitsInCombat method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGrantExperienceToUnitsInCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Pick normalUnit = new Pick ();
		normalUnit.setGainExperienceEachTurn (true);
		
		final Pick summonedUnit = new Pick ();
		summonedUnit.setGainExperienceEachTurn (false);
		
		// Other lists and objects needed for mocks
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final FogOfWarSetting fogOfWarSettings = new FogOfWarSetting ();
		
		// Server memory
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		
		// Normal unit, in combat, on the correct side
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final UnitSkillDirectAccess direct = mock (UnitSkillDirectAccess.class);
		
		final MemoryUnit unit1 = new MemoryUnit ();
		unit1.setStatus (UnitStatusID.ALIVE);
		unit1.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		unit1.setCombatSide (UnitCombatSideID.ATTACKER);
		unit1.setCombatPosition (new MapCoordinates2DEx (5, 6));
		unit1.setCombatHeading (1);
		trueMap.getUnit ().add (unit1);
		when (direct.getDirectSkillValue (unit1.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)).thenReturn (11);

		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unit1, null, null, null, players, trueMap, db)).thenReturn (xu1);
		when (xu1.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnit);
		
		// Normal unit, in combat, on the wrong side
		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setStatus (UnitStatusID.ALIVE);
		unit2.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		unit2.setCombatSide (UnitCombatSideID.DEFENDER);
		unit2.setCombatPosition (new MapCoordinates2DEx (5, 6));
		unit2.setCombatHeading (1);
		trueMap.getUnit ().add (unit2);
		when (direct.getDirectSkillValue (unit2.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)).thenReturn (11);
		
		// Normal unit, not in combat, on the correct side
		final MemoryUnit unit3 = new MemoryUnit ();
		unit3.setStatus (UnitStatusID.ALIVE);
		trueMap.getUnit ().add (unit3);
		when (direct.getDirectSkillValue (unit3.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)).thenReturn (11);
		
		// Summoned unit, in combat, on the correct side
		final MemoryUnit unit4 = new MemoryUnit ();
		unit4.setStatus (UnitStatusID.ALIVE);
		unit4.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		unit4.setCombatSide (UnitCombatSideID.ATTACKER);
		unit4.setCombatPosition (new MapCoordinates2DEx (5, 6));
		unit4.setCombatHeading (1);
		trueMap.getUnit ().add (unit4);
		when (direct.getDirectSkillValue (unit4.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)).thenReturn (11);

		final ExpandedUnitDetails xu4 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unit4, null, null, null, players, trueMap, db)).thenReturn (xu4);
		when (xu4.getModifiedUnitMagicRealmLifeformType ()).thenReturn (summonedUnit);
		
		// Dead unit, in combat, on the correct side
		final MemoryUnit unit5 = new MemoryUnit ();
		unit5.setStatus (UnitStatusID.DEAD);
		unit5.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		unit5.setCombatSide (UnitCombatSideID.ATTACKER);
		unit5.setCombatPosition (new MapCoordinates2DEx (5, 6));
		unit5.setCombatHeading (1);
		trueMap.getUnit ().add (unit5);
		when (direct.getDirectSkillValue (unit5.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)).thenReturn (11);
		
		// Normal unit, in combat, on the correct side
		final MemoryUnit unit6 = new MemoryUnit ();
		unit6.setStatus (UnitStatusID.ALIVE);
		unit6.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		unit6.setCombatSide (UnitCombatSideID.ATTACKER);
		unit6.setCombatPosition (new MapCoordinates2DEx (5, 7));
		unit6.setCombatHeading (1);
		trueMap.getUnit ().add (unit6);
		when (direct.getDirectSkillValue (unit6.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)).thenReturn (11);

		final ExpandedUnitDetails xu6 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unit6, null, null, null, players, trueMap, db)).thenReturn (xu6);
		when (xu6.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnit);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		final PlayerMessageProcessing playerMessageProcessing = mock (PlayerMessageProcessing.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setExpandUnitDetails (expand);
		multi.setFogOfWarMidTurnChanges (midTurn);
		multi.setUnitSkillDirectAccess (direct);
		multi.setUnitServerUtils (unitServerUtils);
		multi.setPlayerMessageProcessing (playerMessageProcessing);
		
		// Run method
		multi.grantExperienceToUnitsInCombat (new MapCoordinates3DEx (20, 10, 1), UnitCombatSideID.ATTACKER, trueMap, players, db, fogOfWarSettings);
		
		// Check results
		verify (direct).setDirectSkillValue (unit1, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, 12);
		verify (direct).setDirectSkillValue (unit1, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, 13);
		verify (midTurn).updatePlayerMemoryOfUnit (eq (unit1), eq (trueMap.getMap ()), eq (players), eq (db), eq (fogOfWarSettings), anyMap ());

		verify (direct).setDirectSkillValue (unit6, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, 12);
		verify (direct).setDirectSkillValue (unit6, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, 13);
		verify (midTurn).updatePlayerMemoryOfUnit (eq (unit6), eq (trueMap.getMap ()), eq (players), eq (db), eq (fogOfWarSettings), anyMap ());
		
		verifyNoMoreInteractions (direct);
		verifyNoMoreInteractions (midTurn);
	}
	
	/**
	 * Tests the moveUnitStackOneCellOnServerAndClients method, when there's no cities, lairs, nodes, towers invovled
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMoveUnitStackOneCellOnServerAndClients () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Map feature is something irrelevant, like gems
		final MapFeatureEx mapFeature = new MapFeatureEx ();
		when (db.findMapFeature ("MF01", "moveUnitStackOneCellOnServerAndClients")).thenReturn (mapFeature);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setUnits (FogOfWarValue.REMEMBER_AS_LAST_SEEN);	// Value used is pretty much irrelevant since anything to do with it is mocked out
		fowSettings.setTerrainAndNodeAuras (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		fowSettings.setCitiesSpellsAndCombatAreaEffects (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fowSettings);
		sd.setOverlandMapSize (overlandMapSize);
		
		// True map
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData moveToCell = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (0).getRow ().get (11).getCell ().get (20).setTerrainData (moveToCell);
		moveToCell.setMapFeatureID ("MF01");
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Lets say we're moving onto a tower, so plane on moveTo changes to 0
		final MapCoordinates3DEx moveFrom = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx moveTo = new MapCoordinates3DEx (20, 11, 0);

		// Mock what each player can see
		final FogOfWarCalculations fowCalc = mock (FogOfWarCalculations.class);
		
		// Player owning the units
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		fow1.getUnit ().add (new MemoryUnit ());
		final MapVolumeOfFogOfWarStates fowArea1 = new MapVolumeOfFogOfWarStates ();
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (fowArea1);
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);
		
		// Player who can see the start of their move, but not the end
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (5);
		pd2.setHuman (true);

		final FogOfWarMemory fow2 = new FogOfWarMemory ();
		fow2.getUnit ().add (new MemoryUnit ());
		final MapVolumeOfFogOfWarStates fowArea2 = new MapVolumeOfFogOfWarStates ();
		
		final MomPersistentPlayerPrivateKnowledge priv2 = new MomPersistentPlayerPrivateKnowledge ();
		priv2.setFogOfWar (fowArea2);
		priv2.setFogOfWarMemory (fow2);
		
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, priv2, null, null);

		final DummyServerToClientConnection conn2 = new DummyServerToClientConnection ();
		player2.setConnection (conn2);
		
		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveFrom, fowSettings.getUnits (), trueTerrain, fowArea2, db)).thenReturn (true);
		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveTo, fowSettings.getUnits (), trueTerrain, fowArea2, db)).thenReturn (false);
		
		// AI player who can see the end of their move, but not the start
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (-1);
		pd3.setHuman (false);

		final FogOfWarMemory fow3 = new FogOfWarMemory ();
		fow3.getUnit ().add (new MemoryUnit ());
		final MapVolumeOfFogOfWarStates fowArea3 = new MapVolumeOfFogOfWarStates ();

		final MomPersistentPlayerPrivateKnowledge priv3 = new MomPersistentPlayerPrivateKnowledge ();
		priv3.setFogOfWar (fowArea3);
		priv3.setFogOfWarMemory (fow3);
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, priv3, null, null);

		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveFrom, fowSettings.getUnits (), trueTerrain, fowArea3, db)).thenReturn (false);
		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveTo, fowSettings.getUnits (), trueTerrain, fowArea3, db)).thenReturn (true);
		
		// Player whose seen the units at some point in the past, but can't see them now
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (6);
		pd4.setHuman (true);
		
		final FogOfWarMemory fow4 = new FogOfWarMemory ();
		fow4.getUnit ().add (new MemoryUnit ());
		final MapVolumeOfFogOfWarStates fowArea4 = new MapVolumeOfFogOfWarStates ();
		
		final MomPersistentPlayerPrivateKnowledge priv4 = new MomPersistentPlayerPrivateKnowledge ();
		priv4.setFogOfWar (fowArea4);
		priv4.setFogOfWarMemory (fow4);
		
		final PlayerServerDetails player4 = new PlayerServerDetails (pd4, null, priv4, null, null);
		
		final DummyServerToClientConnection conn4 = new DummyServerToClientConnection ();
		player4.setConnection (conn4);

		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveFrom, fowSettings.getUnits (), trueTerrain, fowArea4, db)).thenReturn (false);
		when (fowCalc.canSeeMidTurnOnAnyPlaneIfTower (moveTo, fowSettings.getUnits (), trueTerrain, fowArea4, db)).thenReturn (false);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);
		players.add (player3);
		players.add (player4);
		
		// Units being moved
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
		final List<Integer> unitURNList = new ArrayList<Integer> ();

		for (int n = 1; n <= 3; n++)
		{
			unitURNList.add (n);
			
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			unitStack.add (tu);
			
			// Players 1, 2 & 4 can see the units before they move; player 3 can't see them yet but they get added partway through by a mocked call so better to add it now
			final MemoryUnit mu1 = new MemoryUnit ();
			mu1.setUnitURN (n);
			mu1.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			when (unitUtils.findUnitURN (n, fow1.getUnit (), "moveUnitStackOneCellOnServerAndClients")).thenReturn (mu1);
			when (unitUtils.findUnitURN (n, fow1.getUnit ())).thenReturn (mu1);

			final MemoryUnit mu2 = new MemoryUnit ();
			mu2.setUnitURN (n);
			mu2.setUnitLocation (new MapCoordinates3DEx (moveFrom));

			final MemoryUnit mu3 = new MemoryUnit ();
			mu3.setUnitURN (n);
			mu3.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			when (unitUtils.findUnitURN (n, fow3.getUnit (), "moveUnitStackOneCellOnServerAndClients")).thenReturn (mu3);
			when (unitUtils.findUnitURN (n, fow3.getUnit ())).thenReturn (mu3);
			
			final MemoryUnit mu4 = new MemoryUnit ();
			mu4.setUnitURN (n);
			mu4.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			when (unitUtils.findUnitURN (n, fow4.getUnit (), "moveUnitStackOneCellOnServerAndClients")).thenReturn (mu4);
		}
		
		// Set up object to test
		final FogOfWarProcessing fowProc = mock (FogOfWarProcessing.class);
		final FogOfWarMidTurnChanges single = mock (FogOfWarMidTurnChanges.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setFogOfWarCalculations (fowCalc);
		multi.setFogOfWarProcessing (fowProc);
		multi.setFogOfWarMidTurnChanges (single);
		multi.setUnitUtils (unitUtils);

		// Run method
		multi.moveUnitStackOneCellOnServerAndClients (unitStack, player1, moveFrom, moveTo, players, gsk, sd, db);
		
		// Check player 1
		for (int n = 1; n <= 3; n++)
			assertEquals (moveTo, unitUtils.findUnitURN (n, fow1.getUnit (), "moveUnitStackOneCellOnServerAndClients").getUnitLocation ());

		assertEquals (1, conn1.getMessages ().size ());
		final MoveUnitStackOverlandMessage msg1 = (MoveUnitStackOverlandMessage) conn1.getMessages ().get (0);
		assertFalse (msg1.isFreeAfterMoving ());
		assertEquals (moveFrom, msg1.getMoveFrom ());
		assertEquals (moveTo, msg1.getMoveTo ());
		assertEquals (3, msg1.getUnitURN ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals (n, msg1.getUnitURN ().get (n-1).intValue ());

		// Check player 2
		verify (single).freeUnitStackIncludingSpellsFromServerPlayerMemoryOnly (unitURNList, player2);

		assertEquals (1, conn2.getMessages ().size ());
		final MoveUnitStackOverlandMessage msg2 = (MoveUnitStackOverlandMessage) conn2.getMessages ().get (0);
		assertTrue (msg2.isFreeAfterMoving ());
		assertEquals (moveFrom, msg2.getMoveFrom ());
		assertEquals (moveTo, msg2.getMoveTo ());
		assertEquals (3, msg2.getUnitURN ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals (n, msg2.getUnitURN ().get (n-1).intValue ());

		// Check player 3
		verify (single).addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient (unitStack, trueMap.getMaintainedSpell (), player3);

		for (int n = 1; n <= 3; n++)
			assertEquals (moveTo, unitUtils.findUnitURN (n, fow3.getUnit (), "moveUnitStackOneCellOnServerAndClients").getUnitLocation ());

		// Check player 4
		for (int n = 1; n <= 3; n++)		// Player still knows about the units, but at their old location, we didn't see them move
			assertEquals (moveFrom, unitUtils.findUnitURN (n, fow4.getUnit (), "moveUnitStackOneCellOnServerAndClients").getUnitLocation ());

		assertEquals (0, conn4.getMessages ().size ());
		
		// The gems are still there
		assertEquals ("MF01", moveToCell.getMapFeatureID ());
		
		verifyNoMoreInteractions (single);
	}

	/**
	 * Tests the moveUnitStackOneCellOnServerAndClients method, moving onto an empty lair.
	 * Note from this method's point of view, its irrelevant whether the lair had monsters in it - we could be coming here from the movement
	 * routine just directly moving onto an empty lair, or could be coming here from the combat routine after successfully clearing out the lair. 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMoveUnitStackOneCellOnServerAndClients_MoveToLair () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Map feature is one that may contain monsters
		final MapFeatureEx mapFeature = new MapFeatureEx ();
		mapFeature.getMapFeatureMagicRealm ().add (new MapFeatureMagicRealm ());
		when (db.findMapFeature ("MF01", "moveUnitStackOneCellOnServerAndClients")).thenReturn (mapFeature);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setUnits (FogOfWarValue.REMEMBER_AS_LAST_SEEN);	// Value used is pretty much irrelevant since anything to do with it is mocked out
		fowSettings.setTerrainAndNodeAuras (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		fowSettings.setCitiesSpellsAndCombatAreaEffects (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fowSettings);
		sd.setOverlandMapSize (overlandMapSize);
		
		// True map
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData moveToCell = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (0).getRow ().get (11).getCell ().get (20).setTerrainData (moveToCell);
		moveToCell.setMapFeatureID ("MF01");
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Lets say we're moving onto a tower, so plane on moveTo changes to 0
		final MapCoordinates3DEx moveFrom = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx moveTo = new MapCoordinates3DEx (20, 11, 0);

		// Mock what each player can see
		final FogOfWarCalculations fowCalc = mock (FogOfWarCalculations.class);
		
		// Player owning the units
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final MapVolumeOfMemoryGridCells terrain1 = createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		fow1.setMap (terrain1);
		
		final MapVolumeOfFogOfWarStates fowArea1 = createFogOfWarArea (overlandMapSize);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (fowArea1);
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);

		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		
		// Units being moved
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
		final List<Integer> unitURNList = new ArrayList<Integer> ();

		for (int n = 1; n <= 3; n++)
		{
			unitURNList.add (n);
			
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			unitStack.add (tu);
			
			// Player can see the units before they move
			final MemoryUnit mu1 = new MemoryUnit ();
			mu1.setUnitURN (n);
			mu1.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			when (unitUtils.findUnitURN (n, fow1.getUnit (), "moveUnitStackOneCellOnServerAndClients")).thenReturn (mu1);
			when (unitUtils.findUnitURN (n, fow1.getUnit ())).thenReturn (mu1);
		}

		// Its a lair, not a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (moveToCell)).thenReturn (false);
		
		// Set up object to test
		final FogOfWarProcessing fowProc = mock (FogOfWarProcessing.class);
		final FogOfWarMidTurnChanges single = mock (FogOfWarMidTurnChanges.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setFogOfWarCalculations (fowCalc);
		multi.setFogOfWarProcessing (fowProc);
		multi.setFogOfWarMidTurnChanges (single);
		multi.setUnitUtils (unitUtils);
		multi.setMemoryGridCellUtils (memoryGridCellUtils);
		
		// Run method
		multi.moveUnitStackOneCellOnServerAndClients (unitStack, player1, moveFrom, moveTo, players, gsk, sd, db);
		
		// Check player 1
		for (int n = 1; n <= 3; n++)
			assertEquals (moveTo, unitUtils.findUnitURN (n, fow1.getUnit (), "moveUnitStackOneCellOnServerAndClients").getUnitLocation ());

		assertEquals (1, conn1.getMessages ().size ());
		final MoveUnitStackOverlandMessage msg1 = (MoveUnitStackOverlandMessage) conn1.getMessages ().get (0);
		assertFalse (msg1.isFreeAfterMoving ());
		assertEquals (moveFrom, msg1.getMoveFrom ());
		assertEquals (moveTo, msg1.getMoveTo ());
		assertEquals (3, msg1.getUnitURN ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals (n, msg1.getUnitURN ().get (n-1).intValue ());

		// The lair is gone
		assertNull (moveToCell.getMapFeatureID ());
	}

	/**
	 * Tests the moveUnitStackOneCellOnServerAndClients method, moving onto a Tower.
	 * Note from this method's point of view, its irrelevant whether the tower had monsters in it - we could be coming here from the movement
	 * routine just directly moving onto an empty tower, or could be coming here from the combat routine after successfully clearing out the tower. 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMoveUnitStackOneCellOnServerAndClients_MoveToTower () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPlaneNumber (1);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		planes.add (arcanus);
		planes.add (myrror);

		when (db.getPlane ()).thenReturn (planes);
		
		// Map feature is one that may contain monsters
		final MapFeatureEx mapFeature = new MapFeatureEx ();
		mapFeature.getMapFeatureMagicRealm ().add (new MapFeatureMagicRealm ());
		when (db.findMapFeature (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY, "moveUnitStackOneCellOnServerAndClients")).thenReturn (mapFeature);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setUnits (FogOfWarValue.REMEMBER_AS_LAST_SEEN);	// Value used is pretty much irrelevant since anything to do with it is mocked out
		fowSettings.setTerrainAndNodeAuras (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		fowSettings.setCitiesSpellsAndCombatAreaEffects (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fowSettings);
		sd.setOverlandMapSize (overlandMapSize);
		
		// True map
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData moveToCell = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (0).getRow ().get (11).getCell ().get (20).setTerrainData (moveToCell);
		moveToCell.setMapFeatureID (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);
		
		final OverlandMapTerrainData moveToCellOtherPlane = new OverlandMapTerrainData ();
		trueTerrain.getPlane ().get (1).getRow ().get (11).getCell ().get (20).setTerrainData (moveToCellOtherPlane);
		moveToCellOtherPlane.setMapFeatureID (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Lets say we're moving onto a tower, so plane on moveTo changes to 0
		final MapCoordinates3DEx moveFrom = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx moveTo = new MapCoordinates3DEx (20, 11, 0);

		// Mock what each player can see
		final FogOfWarCalculations fowCalc = mock (FogOfWarCalculations.class);
		
		// Player owning the units
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final MapVolumeOfMemoryGridCells terrain1 = createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory fow1 = new FogOfWarMemory ();
		fow1.setMap (terrain1);
		
		final MapVolumeOfFogOfWarStates fowArea1 = createFogOfWarArea (overlandMapSize);
		
		final MomPersistentPlayerPrivateKnowledge priv1 = new MomPersistentPlayerPrivateKnowledge ();
		priv1.setFogOfWar (fowArea1);
		priv1.setFogOfWarMemory (fow1);
		
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, priv1, null, null);
		
		final DummyServerToClientConnection conn1 = new DummyServerToClientConnection ();
		player1.setConnection (conn1);

		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		
		// Units being moved
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
		final List<Integer> unitURNList = new ArrayList<Integer> ();

		for (int n = 1; n <= 3; n++)
		{
			unitURNList.add (n);
			
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			unitStack.add (tu);
			
			// Player can see the units before they move
			final MemoryUnit mu1 = new MemoryUnit ();
			mu1.setUnitURN (n);
			mu1.setUnitLocation (new MapCoordinates3DEx (moveFrom));
			when (unitUtils.findUnitURN (n, fow1.getUnit (), "moveUnitStackOneCellOnServerAndClients")).thenReturn (mu1);
			when (unitUtils.findUnitURN (n, fow1.getUnit ())).thenReturn (mu1);
		}

		// Its a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (moveToCell)).thenReturn (true);
		
		// Set up object to test
		final FogOfWarProcessing fowProc = mock (FogOfWarProcessing.class);
		final FogOfWarMidTurnChanges single = mock (FogOfWarMidTurnChanges.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setFogOfWarCalculations (fowCalc);
		multi.setFogOfWarProcessing (fowProc);
		multi.setFogOfWarMidTurnChanges (single);
		multi.setUnitUtils (unitUtils);
		multi.setMemoryGridCellUtils (memoryGridCellUtils);
		
		// Run method
		multi.moveUnitStackOneCellOnServerAndClients (unitStack, player1, moveFrom, moveTo, players, gsk, sd, db);
		
		// Check player 1
		for (int n = 1; n <= 3; n++)
			assertEquals (moveTo, unitUtils.findUnitURN (n, fow1.getUnit (), "moveUnitStackOneCellOnServerAndClients").getUnitLocation ());

		assertEquals (1, conn1.getMessages ().size ());
		final MoveUnitStackOverlandMessage msg1 = (MoveUnitStackOverlandMessage) conn1.getMessages ().get (0);
		assertFalse (msg1.isFreeAfterMoving ());
		assertEquals (moveFrom, msg1.getMoveFrom ());
		assertEquals (moveTo, msg1.getMoveTo ());
		assertEquals (3, msg1.getUnitURN ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals (n, msg1.getUnitURN ().get (n-1).intValue ());

		// The tower is now cleared, on both planes
		assertEquals (CommonDatabaseConstants.FEATURE_CLEARED_TOWER_OF_WIZARDRY, moveToCell.getMapFeatureID ());
		assertEquals (CommonDatabaseConstants.FEATURE_CLEARED_TOWER_OF_WIZARDRY, moveToCellOtherPlane.getMapFeatureID ());
	}

	/**
	 * Tests the resetUnitOverlandMovement method for all players
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testResetUnitOverlandMovement_AllPlayers () throws Exception
	{
		// Empty mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final FogOfWarSetting fogOfWarSettings = new FogOfWarSetting ();
		
		// Other lists
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Create units owned by 3 players
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		for (int playerID = 1; playerID <= 3; playerID++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setOwningPlayerID (playerID);
			fow.getUnit ().add (spearmen);
			
			final ExpandedUnitDetails xuSpearmen = mock (ExpandedUnitDetails.class);
			when (expand.expandUnitDetails (spearmen, null, null, null, players, fow, db)).thenReturn (xuSpearmen);
			when (xuSpearmen.getMovementSpeed ()).thenReturn (1);

			final MemoryUnit hellHounds = new MemoryUnit ();
			hellHounds.setOwningPlayerID (playerID);
			fow.getUnit ().add (hellHounds);

			final ExpandedUnitDetails xuHellHounds = mock (ExpandedUnitDetails.class);
			when (expand.expandUnitDetails (hellHounds, null, null, null, players, fow, db)).thenReturn (xuHellHounds);
			when (xuHellHounds.getMovementSpeed ()).thenReturn (2);
		}

		// Set up object to test
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setExpandUnitDetails (expand);
		multi.setFogOfWarMidTurnChanges (midTurn);
		
		// Run method
		multi.resetUnitOverlandMovement (0, players, fow, fogOfWarSettings, db);

		// Check results
		assertEquals (2, fow.getUnit ().get (0).getDoubleOverlandMovesLeft ());
		assertEquals (4, fow.getUnit ().get (1).getDoubleOverlandMovesLeft ());
		assertEquals (2, fow.getUnit ().get (2).getDoubleOverlandMovesLeft ());
		assertEquals (4, fow.getUnit ().get (3).getDoubleOverlandMovesLeft ());
		assertEquals (2, fow.getUnit ().get (4).getDoubleOverlandMovesLeft ());
		assertEquals (4, fow.getUnit ().get (5).getDoubleOverlandMovesLeft ());
	}

	/**
	 * Tests the resetUnitOverlandMovement method for a single player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testResetUnitOverlandMovement_OnePlayer () throws Exception
	{
		// Empty mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final FogOfWarSetting fogOfWarSettings = new FogOfWarSetting ();
		
		// Other lists
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Create units owned by 3 players
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		for (int playerID = 1; playerID <= 3; playerID++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setOwningPlayerID (playerID);
			fow.getUnit ().add (spearmen);
			
			final MemoryUnit hellHounds = new MemoryUnit ();
			hellHounds.setOwningPlayerID (playerID);
			fow.getUnit ().add (hellHounds);

			if (playerID == 2)
			{
				final ExpandedUnitDetails xuSpearmen = mock (ExpandedUnitDetails.class);
				when (expand.expandUnitDetails (spearmen, null, null, null, players, fow, db)).thenReturn (xuSpearmen);
				when (xuSpearmen.getMovementSpeed ()).thenReturn (1);
				
				final ExpandedUnitDetails xuHellHounds = mock (ExpandedUnitDetails.class);
				when (expand.expandUnitDetails (hellHounds, null, null, null, players, fow, db)).thenReturn (xuHellHounds);
				when (xuHellHounds.getMovementSpeed ()).thenReturn (2);
			}
		}

		// Set up object to test
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		
		final FogOfWarMidTurnMultiChangesImpl multi = new FogOfWarMidTurnMultiChangesImpl ();
		multi.setExpandUnitDetails (expand);
		multi.setFogOfWarMidTurnChanges (midTurn);

		// Run method
		multi.resetUnitOverlandMovement (2, players, fow, fogOfWarSettings, db);

		// Check results
		assertEquals (0, fow.getUnit ().get (0).getDoubleOverlandMovesLeft ());
		assertEquals (0, fow.getUnit ().get (1).getDoubleOverlandMovesLeft ());
		assertEquals (2, fow.getUnit ().get (2).getDoubleOverlandMovesLeft ());
		assertEquals (4, fow.getUnit ().get (3).getDoubleOverlandMovesLeft ());
		assertEquals (0, fow.getUnit ().get (4).getDoubleOverlandMovesLeft ());
		assertEquals (0, fow.getUnit ().get (5).getDoubleOverlandMovesLeft ());
	}
}