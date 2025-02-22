package momime.server.fogofwar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarValue;
import momime.common.database.OverlandMapSize;
import momime.common.database.Plane;
import momime.common.database.Spell;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.movement.MovementUtils;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryGridCellUtilsImpl;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.ServerCityCalculations;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.database.ServerDatabaseValues;
import momime.server.messages.MomGeneralServerKnowledge;

/**
 * Tests the FogOfWarProcessing class
 */
@ExtendWith(MockitoExtension.class)
public final class TestFogOfWarProcessingImpl extends ServerTestData
{
	/**
	 * Tests the canSee method
	 */
	@Test
	public final void testCanSee ()
	{
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fogOfWarArea = createFogOfWarArea (sys);
		
		// True terrain
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

		// Towers
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		
		// Set up test object
		final FogOfWarProcessingImpl proc = new FogOfWarProcessingImpl ();
		proc.setMemoryGridCellUtils (memoryGridCellUtils);

		// Never seen, so now seeing it for the first time
		fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.NEVER_SEEN);
		proc.canSee (fogOfWarArea, trueTerrain, 20, 10, 1);
		assertEquals (FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME, fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().get (20));

		proc.canSee (fogOfWarArea, trueTerrain, 20, 10, 1);
		assertEquals (FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME, fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().get (20));

		// Have seen on a previous turn, then lost sight of it, and now seeing it again
		fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.HAVE_SEEN);
		proc.canSee (fogOfWarArea, trueTerrain, 20, 10, 1);
		assertEquals (FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT, fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().get (20));

		proc.canSee (fogOfWarArea, trueTerrain, 20, 10, 1);
		assertEquals (FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT, fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().get (20));

		// Can see, so could see it last turn and still can
		fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		proc.canSee (fogOfWarArea, trueTerrain, 20, 10, 1);
		assertEquals (FogOfWarStateID.TEMP_CAN_STILL_SEE, fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().get (20));

		proc.canSee (fogOfWarArea, trueTerrain, 20, 10, 1);
		assertEquals (FogOfWarStateID.TEMP_CAN_STILL_SEE, fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().get (20));
	}

	/**
	 * Tests the canSeeRadius method
	 */
	@Test
	public final void testCanSeeRadius ()
	{
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fogOfWarArea = createFogOfWarArea (sys);

		// True terrain
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		// Towers
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		
		// There's 6 FOW states, so put a 7x7 area with the top row clipped off the top of the map
		// The in the leftmost column (which is wrapped to the right edge of the map), put one of each FOW states to test they get modified correctly
		int y = 0;
		for (final FogOfWarStateID state : FogOfWarStateID.values ())
		{
			fogOfWarArea.getPlane ().get (1).getRow ().get (y).getCell ().set (59, state);
			y++;
		}

		// Set up test object
		final FogOfWarProcessingImpl proc = new FogOfWarProcessingImpl ();
		proc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		proc.setMemoryGridCellUtils (memoryGridCellUtils);

		// Run method
		proc.canSeeRadius (fogOfWarArea, trueTerrain, sys, 2, 2, 1, 3);

		// Check results of right hand area which is all the same value
		for (int x = 0; x <= 5; x++)
			for (y = 0; y <= 5; y++)
				assertEquals (FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME, fogOfWarArea.getPlane ().get (1).getRow ().get (y).getCell ().get (x));

		// Check results of left column
		assertEquals (FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME,				fogOfWarArea.getPlane ().get (1).getRow ().get (0).getCell ().get (59));
		assertEquals (FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT,	fogOfWarArea.getPlane ().get (1).getRow ().get (1).getCell ().get (59));
		assertEquals (FogOfWarStateID.TEMP_CAN_STILL_SEE,								fogOfWarArea.getPlane ().get (1).getRow ().get (2).getCell ().get (59));
		assertEquals (FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME,				fogOfWarArea.getPlane ().get (1).getRow ().get (3).getCell ().get (59));
		assertEquals (FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT,	fogOfWarArea.getPlane ().get (1).getRow ().get (4).getCell ().get (59));
		assertEquals (FogOfWarStateID.TEMP_CAN_STILL_SEE,								fogOfWarArea.getPlane ().get (1).getRow ().get (5).getCell ().get (59));

		// Check no other values got changed
		int count = 0;
		for (int plane = 0; plane < 2; plane++)
			for (y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
					if (fogOfWarArea.getPlane ().get (plane).getRow ().get (y).getCell ().get (x) == FogOfWarStateID.NEVER_SEEN)
						count++;

		assertEquals ((60*40*2)-(7*6), count);
	}

	/**
	 * Tests the markVisibleArea method
	 * @throws Exception If there is a problem
	 * @throws InvalidFormatException If the excel spreadsheet containing the expected results can't be loaded
	 */
	@Test
	public final void testMarkVisibleArea () throws Exception
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
		
		final Spell naturesEyeDef = new Spell ();
		naturesEyeDef.setSpellRadius (5);
		when (db.findSpell ("SP012", "markVisibleArea")).thenReturn (naturesEyeDef);
		
		// Map
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (overlandMapSize);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);

		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player);
		
		// Wizard
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), 2, "markVisibleArea")).thenReturn (wizardDetails);

		// Our cities
		final OverlandMapCityData ourCityOne = new OverlandMapCityData ();
		ourCityOne.setCityOwnerID (2);
		ourCityOne.setCityPopulation (1);
		trueTerrain.getPlane ().get (0).getRow ().get (25).getCell ().get (35).setCityData (ourCityOne);

		final OverlandMapCityData ourCityTwo = new OverlandMapCityData ();
		ourCityTwo.setCityOwnerID (2);
		ourCityTwo.setCityPopulation (1);
		trueTerrain.getPlane ().get (0).getRow ().get (12).getCell ().get (50).setCityData (ourCityTwo);

		final OverlandMapCityData ourCityThree = new OverlandMapCityData ();
		ourCityThree.setCityOwnerID (2);
		ourCityThree.setCityPopulation (1);
		trueTerrain.getPlane ().get (1).getRow ().get (1).getCell ().get (1).setCityData (ourCityThree);

		// Put buildings in that give cityTwo scouting range 3, and cityThree scouting range 4
		final MemoryBuilding cityWalls = new MemoryBuilding ();
		cityWalls.setBuildingID ("BL35");
		cityWalls.setCityLocation (new MapCoordinates3DEx (50, 12, 0));

		trueMap.getBuilding ().add (cityWalls);

		// Enemy cities
		final OverlandMapCityData enemyCityOne = new OverlandMapCityData ();
		enemyCityOne.setCityOwnerID (1);
		enemyCityOne.setCityPopulation (1);
		trueTerrain.getPlane ().get (0).getRow ().get (5).getCell ().get (5).setCityData (enemyCityOne);

		final OverlandMapCityData enemyCityTwo = new OverlandMapCityData ();
		enemyCityTwo.setCityOwnerID (1);
		enemyCityTwo.setCityPopulation (1);
		trueTerrain.getPlane ().get (1).getRow ().get (32).getCell ().get (54).setCityData (enemyCityTwo);

		// We can see enemy cities that we have a curse on
		final MemoryMaintainedSpellUtils spellUtils = mock (MemoryMaintainedSpellUtils.class);
		when (spellUtils.findMaintainedSpell (trueMap.getMaintainedSpell (), 2, null, null, null, new MapCoordinates3DEx (54, 32, 1), null)).thenReturn (new MemoryMaintainedSpell ());
		when (spellUtils.findMaintainedSpell (trueMap.getMaintainedSpell (), 2, null, null, null, new MapCoordinates3DEx (5, 5, 0), null)).thenReturn (null);
		when (spellUtils.findMaintainedSpell (trueMap.getMaintainedSpell (), 2, ServerDatabaseValues.SPELL_ID_NATURE_AWARENESS, null, null, null, null)).thenReturn (null);
		when (spellUtils.findMaintainedSpell (trueMap.getMaintainedSpell (), 2, ServerDatabaseValues.SPELL_ID_AWARENESS, null, null, null, null)).thenReturn (null);
		
		// City scouting ranges
		final ServerCityCalculations cityCalc = mock (ServerCityCalculations.class);
		when (cityCalc.calculateCityScoutingRange (trueMap.getBuilding (), new MapCoordinates3DEx (35, 25, 0), db)).thenReturn (-1);
		when (cityCalc.calculateCityScoutingRange (trueMap.getBuilding (), new MapCoordinates3DEx (50, 12, 0), db)).thenReturn (3);		// City walls can see 3
		when (cityCalc.calculateCityScoutingRange (trueMap.getBuilding (), new MapCoordinates3DEx (1, 1, 1), db)).thenReturn (4);			// Oracle can see 4

		// Units - a regular unit, flying unit (sees distance 2) and unit with actual scouting III skill
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		final MemoryUnit unitOne = new MemoryUnit ();
		unitOne.setUnitID ("UN105");
		unitOne.setStatus (UnitStatusID.ALIVE);
		unitOne.setUnitLocation (new MapCoordinates3DEx (54, 4, 1));
		unitOne.setOwningPlayerID (2);
		trueMap.getUnit ().add (unitOne);
		
		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unitOne, null, null, null, players, trueMap, db)).thenReturn (xu1);

		final MemoryUnit unitTwo = new MemoryUnit ();
		unitTwo.setUnitID ("UN067");
		unitTwo.setStatus (UnitStatusID.ALIVE);
		unitTwo.setUnitLocation (new MapCoordinates3DEx (14, 34, 1));
		unitTwo.setOwningPlayerID (2);
		trueMap.getUnit ().add (unitTwo);

		final ExpandedUnitDetails xu2 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unitTwo, null, null, null, players, trueMap, db)).thenReturn (xu2);
		
		final MemoryUnit unitThree = new MemoryUnit ();
		unitThree.setUnitID ("UN005");
		unitThree.setStatus (UnitStatusID.ALIVE);
		unitThree.setUnitLocation (new MapCoordinates3DEx (44, 17, 0));
		unitThree.setOwningPlayerID (2);
		trueMap.getUnit ().add (unitThree);

		final ExpandedUnitDetails xu3 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unitThree, null, null, null, players, trueMap, db)).thenReturn (xu3);
		
		// Unit in a tower
		for (final Plane plane : db.getPlane ())
		{
			final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
			terrainData.setMapFeatureID (CommonDatabaseConstants.FEATURE_CLEARED_TOWER_OF_WIZARDRY);

			trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (22).getCell ().get (22).setTerrainData (terrainData);
		}

		final MemoryUnit unitFour = new MemoryUnit ();
		unitFour.setUnitID ("UN105");
		unitFour.setStatus (UnitStatusID.ALIVE);
		unitFour.setUnitLocation (new MapCoordinates3DEx (22, 22, 0));
		unitFour.setOwningPlayerID (2);
		trueMap.getUnit ().add (unitFour);

		final ExpandedUnitDetails xu4 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unitFour, null, null, null, players, trueMap, db)).thenReturn (xu4);
		
		// Enemy unit
		final MemoryUnit unitFive = new MemoryUnit ();
		unitFive.setUnitID ("UN105");
		unitFive.setStatus (UnitStatusID.ALIVE);
		unitFive.setUnitLocation (new MapCoordinates3DEx (23, 9, 1));
		unitFive.setOwningPlayerID (1);
		trueMap.getUnit ().add (unitFive);

		// Unit scouting ranges
		final ServerUnitCalculations unitCalc = mock (ServerUnitCalculations.class);
		when (unitCalc.calculateUnitScoutingRange (xu1, db)).thenReturn (1);
		when (unitCalc.calculateUnitScoutingRange (xu2, db)).thenReturn (2);
		when (unitCalc.calculateUnitScoutingRange (xu3, db)).thenReturn (3);
		when (unitCalc.calculateUnitScoutingRange (xu4, db)).thenReturn (1);

		// Nature's eye spell
		final MemoryMaintainedSpell naturesEye = new MemoryMaintainedSpell ();
		naturesEye.setCastingPlayerID (2);
		naturesEye.setCityLocation (new MapCoordinates3DEx (11, 35, 0));
		naturesEye.setSpellID ("SP012");
		naturesEye.setCitySpellEffectID ("SE012");

		trueMap.getMaintainedSpell ().add (naturesEye);
		
		// Astral gates
		final MovementUtils movementUtils = mock (MovementUtils.class);

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Set up object to test
		final FogOfWarProcessingImpl proc = new FogOfWarProcessingImpl ();
		proc.setMemoryMaintainedSpellUtils (spellUtils);
		proc.setServerCityCalculations (cityCalc);
		proc.setServerUnitCalculations (unitCalc);
		proc.setExpandUnitDetails (expand);
		proc.setMovementUtils (movementUtils);
		proc.setKnownWizardUtils (knownWizardUtils);
		proc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		proc.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());

		// Test with no special spells
		priv.setFogOfWar (createFogOfWarArea (sd.getOverlandMapSize ()));
		proc.markVisibleArea (player, mom);

		try (final Workbook workbook = WorkbookFactory.create (getClass ().getResourceAsStream ("/markVisibleArea.xlsx")))
		{
			for (final Plane plane : db.getPlane ())
				for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
					{
						final Cell cell = workbook.getSheetAt (plane.getPlaneNumber ()).getRow (y + 1).getCell (x + 1);
	
						// The "A" cells mark locations we can only see after we cast Awareness
						if ((cell != null) && (cell.getCellType () != CellType.BLANK) && (!cell.getStringCellValue ().equals ("A")))
							assertEquals (FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME, priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x),
								x + "," + y + "," + plane.getPlaneNumber ());
						else
							assertEquals (FogOfWarStateID.NEVER_SEEN, priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x),
								x + "," + y + "," + plane.getPlaneNumber ());
					}
	
			// Awareness
			when (spellUtils.findMaintainedSpell (trueMap.getMaintainedSpell (), 2, ServerDatabaseValues.SPELL_ID_AWARENESS, null, null, null, null)).thenReturn (new MemoryMaintainedSpell ());
	
			priv.setFogOfWar (createFogOfWarArea (sd.getOverlandMapSize ()));
			proc.markVisibleArea (player, mom);
	
			for (final Plane plane : db.getPlane ())
				for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
					{
						final Cell cell = workbook.getSheetAt (plane.getPlaneNumber ()).getRow (y + 1).getCell (x + 1);
						if ((cell != null) && (cell.getCellType () != CellType.BLANK))
							assertEquals (FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME, priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x),
								x + "," + y + "," + plane.getPlaneNumber ());
						else
							assertEquals (FogOfWarStateID.NEVER_SEEN, priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x),
								x + "," + y + "," + plane.getPlaneNumber ());
					}
		}

		// Nature Awareness
		when (spellUtils.findMaintainedSpell (trueMap.getMaintainedSpell (), 2, ServerDatabaseValues.SPELL_ID_NATURE_AWARENESS, null, null, null, null)).thenReturn (new MemoryMaintainedSpell ());

		priv.setFogOfWar (createFogOfWarArea (sd.getOverlandMapSize ()));
		proc.markVisibleArea (player, mom);

		for (final Plane plane : db.getPlane ())
			for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
					assertEquals (FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME, priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x),
						x + "," + y + "," + plane.getPlaneNumber ());
	}

	/**
	 * Tests the determineVisibleAreaChangedUpdateAction method
	 */
	@Test
	public final void testDetermineVisibleAreaChangedUpdateAction ()
	{
		// Set up test object
		final FogOfWarProcessingImpl proc = new FogOfWarProcessingImpl ();

		// Just go through every possible combination against the expected outcome

		// Never seen - couldn't see before, and we still can't
		// So regardless of what FOW setting we're on, that means there's nothing to do
		for (final FogOfWarValue setting : FogOfWarValue.values ())
			assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NONE, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.NEVER_SEEN, setting));

		// Have seen - at some prior time we saw it then lost sight of it - and this turn it still didn't come back into view yet
		// So regardless of what FOW setting we're on, that means there's nothing to do
		// Although if we're on "always see once seen" then the fact that we saw the location ages ago still means we've got up to date info on it
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NONE, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.HAVE_SEEN, FogOfWarValue.FORGET));
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NONE, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.HAVE_SEEN, FogOfWarValue.REMEMBER_AS_LAST_SEEN));
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NEVER_LOST_SIGHT_OF, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.HAVE_SEEN, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN));

		// Can see - could see it last turn, but this turn we lost sight of it
		// If setting = forget then we need to forget our knowledge of it
		// If setting = remember then we need to remember our knowledge of it, so nothing to do
		// If setting = always see once seen then we're still able to see it and so know that nothing has changed, so still nothing to do
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.CAN_SEE, FogOfWarValue.FORGET));
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NONE, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.CAN_SEE, FogOfWarValue.REMEMBER_AS_LAST_SEEN));
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NEVER_LOST_SIGHT_OF, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.CAN_SEE, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN));

		// Seeing it for first time
		// So regardless of what FOW setting we're on, we need to add it
		for (final FogOfWarValue setting : FogOfWarValue.values ())
			assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME, setting));

		// Seeing after lost sight of it - we saw it once before, lost sight of it for a while, and it just came back into view again
		// If setting = forget then we'd have forgotten about it when it went out of view, so now need to re-add it
		// If setting = remember then maybe it changed while we couldn't see it, so our memory of it may now be incorrect, so need to update it
		// If setting = always see once seen then even when it went out of sight, we could watch changes to it even while it was out of sight, so we know our knowledge of it is already correct
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT, FogOfWarValue.FORGET));
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT, FogOfWarValue.REMEMBER_AS_LAST_SEEN));
		assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NEVER_LOST_SIGHT_OF, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN));

		// Can still see - could see it last turn, and still can
		// So regardless of what FOW setting we're on, we know we already have the correct values so no update required
		for (final FogOfWarValue setting : FogOfWarValue.values ())
			assertEquals (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NEVER_LOST_SIGHT_OF, proc.determineVisibleAreaChangedUpdateAction (FogOfWarStateID.TEMP_CAN_STILL_SEE, setting));
	}
}