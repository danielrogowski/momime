package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;

/**
 * Tests the SampleUnitUtilsImpl method
 */
@ExtendWith(MockitoExtension.class)
public final class TestSampleUnitUtilsImpl
{
	/**
	 * Tests the createSampleAvailableUnit method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateSampleAvailableUnit () throws Exception
	{
		// Just needed for mocks
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final SampleUnitUtilsImpl utils = new SampleUnitUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		
		// Run method
		final AvailableUnit unit = utils.createSampleAvailableUnit ("UN001", 2, 60, db);
		
		// Check results
		assertEquals ("UN001", unit.getUnitID ());
		assertEquals (2, unit.getOwningPlayerID ());
		
		verify (unitUtils).initializeUnitSkills (unit, 60, db);
	}
	
	/**
	 * Tests the createSampleUnit method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateSampleUnit () throws Exception
	{
		// Just needed for mocks
		final CommonDatabase db = mock (CommonDatabase.class);
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final FogOfWarMemory mem = new FogOfWarMemory ();

		// Mock expanding unit details
		final ExpandUnitDetails expandUnitDetails = mock (ExpandUnitDetails.class);
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		final ArgumentCaptor<AvailableUnit> captureUnit = ArgumentCaptor.forClass (AvailableUnit.class);
		
		when (expandUnitDetails.expandUnitDetails (captureUnit.capture (), isNull (), isNull (), isNull (), eq (players), eq (mem), eq (db))).thenReturn (xu);
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final SampleUnitUtilsImpl utils = new SampleUnitUtilsImpl ();
		utils.setExpandUnitDetails (expandUnitDetails);
		utils.setUnitUtils (unitUtils);
		
		// Run method
		final ExpandedUnitDetails unit = utils.createSampleUnit ("UN001", 2, 60, players, mem, db);
		
		// Check results
		assertEquals ("UN001", captureUnit.getValue ().getUnitID ());
		assertEquals (2, captureUnit.getValue ().getOwningPlayerID ());
		
		verify (unitUtils).initializeUnitSkills (captureUnit.getValue (), 60, db);
		
		assertSame (xu, unit);
	}
	
	/**
	 * Tests the createSampleAvailableUnitFromCity method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateSampleAvailableUnitFromCity () throws Exception
	{
		// Just needed for mocks
		final CommonDatabase db = mock (CommonDatabase.class);
		final FogOfWarMemory mem = new FogOfWarMemory ();
		final CoordinateSystem sys = new CoordinateSystem ();
		
		// Owning wizard
		final KnownWizardDetails owningWizard = new KnownWizardDetails ();
		owningWizard.setPlayerID (2);
		
		// Starting experience
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.experienceFromBuildings (mem.getBuilding (), mem.getMaintainedSpell (), new MapCoordinates3DEx (20, 11, 1), db)).thenReturn (60);
		
		// Weapon grade
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort
			(mem.getBuilding (), mem.getMap (), new MapCoordinates3DEx (20, 11, 1), owningWizard.getPick (), sys, db)).thenReturn (1);
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final SampleUnitUtilsImpl utils = new SampleUnitUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setMemoryBuildingUtils (memoryBuildingUtils);
		utils.setUnitCalculations (unitCalculations);

		// Run method
		final AvailableUnit unit = utils.createSampleAvailableUnitFromCity ("UN001", owningWizard, new MapCoordinates3DEx (20, 11, 1), mem, sys, db);
		
		// Check results
		assertEquals ("UN001", unit.getUnitID ());
		assertEquals (2, unit.getOwningPlayerID ());
		assertEquals (new MapCoordinates3DEx (20, 11, 1), unit.getUnitLocation ());
		assertEquals (1, unit.getWeaponGrade ());

		verify (unitUtils).initializeUnitSkills (unit, 60, db);
	}

	/**
	 * Tests the createSampleUnitFromCity method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateSampleUnitFromCity () throws Exception
	{
		// Just needed for mocks
		final CommonDatabase db = mock (CommonDatabase.class);
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final FogOfWarMemory mem = new FogOfWarMemory ();
		final CoordinateSystem sys = new CoordinateSystem ();
		
		// Owning wizard
		final KnownWizardDetails owningWizard = new KnownWizardDetails ();
		owningWizard.setPlayerID (2);
		
		// Starting experience
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.experienceFromBuildings (mem.getBuilding (), mem.getMaintainedSpell (), new MapCoordinates3DEx (20, 11, 1), db)).thenReturn (60);
		
		// Weapon grade
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort
			(mem.getBuilding (), mem.getMap (), new MapCoordinates3DEx (20, 11, 1), owningWizard.getPick (), sys, db)).thenReturn (1);
		
		// Mock expanding unit details
		final ExpandUnitDetails expandUnitDetails = mock (ExpandUnitDetails.class);
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		final ArgumentCaptor<AvailableUnit> captureUnit = ArgumentCaptor.forClass (AvailableUnit.class);
		
		when (expandUnitDetails.expandUnitDetails (captureUnit.capture (), isNull (), isNull (), isNull (), eq (players), eq (mem), eq (db))).thenReturn (xu);

		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final SampleUnitUtilsImpl utils = new SampleUnitUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setMemoryBuildingUtils (memoryBuildingUtils);
		utils.setUnitCalculations (unitCalculations);
		utils.setExpandUnitDetails (expandUnitDetails);

		// Run method
		final ExpandedUnitDetails unit = utils.createSampleUnitFromCity ("UN001", owningWizard, new MapCoordinates3DEx (20, 11, 1), players, mem, sys, db);
		
		// Check results
		assertEquals ("UN001", captureUnit.getValue ().getUnitID ());
		assertEquals (2, captureUnit.getValue ().getOwningPlayerID ());
		assertEquals (new MapCoordinates3DEx (20, 11, 1), captureUnit.getValue ().getUnitLocation ());
		assertEquals (1, captureUnit.getValue ().getWeaponGrade ());

		verify (unitUtils).initializeUnitSkills (captureUnit.getValue (), 60, db);
		
		assertSame (xu, unit);
	}
}