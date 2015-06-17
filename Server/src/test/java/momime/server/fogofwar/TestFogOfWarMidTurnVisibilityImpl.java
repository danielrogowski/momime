package momime.server.fogofwar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.FogOfWarSetting;
import momime.common.database.FogOfWarValue;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.UnitStatusID;
import momime.common.utils.UnitUtils;
import momime.server.ServerTestData;
import momime.server.calculations.FogOfWarCalculations;
import momime.server.database.ServerDatabaseEx;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the FogOfWarMidTurnVisibilityImpl class
 */
public final class TestFogOfWarMidTurnVisibilityImpl
{
	/**
	 * Tests the canSeeUnitMidTurn method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeUnitMidTurn () throws Exception
	{
		// Mock server database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Session description
		final FogOfWarSetting settings = new FogOfWarSetting ();
		settings.setUnits (FogOfWarValue.REMEMBER_AS_LAST_SEEN);

		// True terrain
		final MapVolumeOfMemoryGridCells trueTerrain = new MapVolumeOfMemoryGridCells ();

		// Player who is trying to see it
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWar (new MapVolumeOfFogOfWarStates ());
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		// The unit we're trying to see
		final MemoryUnit spearmen = new MemoryUnit ();
		spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		spearmen.setStatus (UnitStatusID.ALIVE);

		// Set up object to test
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);

		final FogOfWarMidTurnVisibilityImpl calc = new FogOfWarMidTurnVisibilityImpl ();
		calc.setFogOfWarCalculations (single);
		
		// Regular situation of a unit we can't see because we can't see that location
		when (single.canSeeMidTurnOnAnyPlaneIfTower (new MapCoordinates3DEx (20, 10, 0), settings.getUnits (), trueTerrain, priv.getFogOfWar (), db)).thenReturn (false);
		assertFalse (calc.canSeeUnitMidTurn (spearmen, trueTerrain, player, db, settings));

		// Regular situation of a unit we can see because we can see that location
		when (single.canSeeMidTurnOnAnyPlaneIfTower (new MapCoordinates3DEx (20, 10, 0), settings.getUnits (), trueTerrain, priv.getFogOfWar (), db)).thenReturn (true);
		assertTrue (calc.canSeeUnitMidTurn (spearmen, trueTerrain, player, db, settings));

		// Can't see dead units, even if we can see their location
		spearmen.setStatus (UnitStatusID.DEAD);
		assertFalse (calc.canSeeUnitMidTurn (spearmen, trueTerrain, player, db, settings));
	}
	
	/**
	 * Tests the canSeeSpellMidTurn on a unit enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeSpellMidTurn_UnitEnchantment () throws Exception
	{
		// Mock server database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Session description
		final FogOfWarSetting settings = new FogOfWarSetting ();
		settings.setUnits (FogOfWarValue.REMEMBER_AS_LAST_SEEN);

		// True terrain
		final MapVolumeOfMemoryGridCells trueTerrain = new MapVolumeOfMemoryGridCells ();

		// Player who is trying to see it
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWar (new MapVolumeOfFogOfWarStates ());
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);

		// The unit we're trying to see
		final MemoryUnit spearmen = new MemoryUnit ();
		spearmen.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		spearmen.setStatus (UnitStatusID.ALIVE);

		final List<MemoryUnit> trueUnits = new ArrayList<MemoryUnit> ();
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (11, trueUnits, "canSeeSpellMidTurn")).thenReturn (spearmen);
		
		// The spell we're trying to see
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setUnitURN (11);
		
		// Set up object to test
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);

		final FogOfWarMidTurnVisibilityImpl calc = new FogOfWarMidTurnVisibilityImpl ();
		calc.setFogOfWarCalculations (single);
		calc.setUnitUtils (unitUtils);

		// Regular situation of a unit we can't see because we can't see that location
		when (single.canSeeMidTurnOnAnyPlaneIfTower (new MapCoordinates3DEx (20, 10, 0), settings.getUnits (), trueTerrain, priv.getFogOfWar (), db)).thenReturn (false);
		assertFalse (calc.canSeeSpellMidTurn (spell, trueTerrain, trueUnits, player, db, settings));

		// Regular situation of a unit we can see because we can see that location
		when (single.canSeeMidTurnOnAnyPlaneIfTower (new MapCoordinates3DEx (20, 10, 0), settings.getUnits (), trueTerrain, priv.getFogOfWar (), db)).thenReturn (true);
		assertTrue (calc.canSeeSpellMidTurn (spell, trueTerrain, trueUnits, player, db, settings));
	}
	
	/**
	 * Tests the canSeeSpellMidTurn on a city enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeSpellMidTurn_CityEnchantment () throws Exception
	{
		// Mock server database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Session description
		final FogOfWarSetting settings = new FogOfWarSetting ();
		settings.setCitiesSpellsAndCombatAreaEffects (FogOfWarValue.ALWAYS_SEE_ONCE_SEEN);
		
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();

		// Player who is trying to see it
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWar (ServerTestData.createFogOfWarArea (sys));
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);

		// The spell we're trying to see
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setSpellURN (1);
		spell.setCityLocation (new MapCoordinates3DEx (20, 10, 1));

		// Set up object to test
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);
		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (false);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (true);

		final FogOfWarMidTurnVisibilityImpl calc = new FogOfWarMidTurnVisibilityImpl ();
		calc.setFogOfWarCalculations (single);
		
		// Run test
		assertFalse (calc.canSeeSpellMidTurn (spell, null, null, player, db, settings));
		
		priv.getFogOfWar ().getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		assertTrue (calc.canSeeSpellMidTurn (spell, null, null, player, db, settings));
	}
	
	/**
	 * Tests the canSeeSpellMidTurn on an overland enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeSpellMidTurn_OverlandEnchantment () throws Exception
	{
		// Player who is trying to see it
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// The spell we're trying to see - assumed to be overland since it has no UnitURN or CityLocation set
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setSpellURN (1);
		
		// Set up object to test
		final FogOfWarMidTurnVisibilityImpl calc = new FogOfWarMidTurnVisibilityImpl ();

		// Run test
		assertTrue (calc.canSeeSpellMidTurn (spell, null, null, player, null, null));
	}
	
	/**
	 * Tests the canSeeCombatAreaEffectMidTurn method on a global CAE
	 */
	@Test
	public final void testCanSeeCombatAreaEffectMidTurn_Global ()
	{
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fogOfWarArea = ServerTestData.createFogOfWarArea (sys);

		// The CAE we're trying to see
		final MemoryCombatAreaEffect cae = new MemoryCombatAreaEffect ();

		// Set up object to test
		final FogOfWarMidTurnVisibilityImpl calc = new FogOfWarMidTurnVisibilityImpl ();
		
		// Can see this regardless of settings or visible area
		for (final FogOfWarValue setting : FogOfWarValue.values ())
			assertTrue (calc.canSeeCombatAreaEffectMidTurn (cae, fogOfWarArea, setting));
	}

	/**
	 * Tests the canSeeCombatAreaEffectMidTurn method on a localized CAE
	 */
	@Test
	public final void testCanSeeCombatAreaEffectMidTurn_Localized ()
	{
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fogOfWarArea = ServerTestData.createFogOfWarArea (sys);

		// Set up object to test
		final FogOfWarCalculations fow = mock (FogOfWarCalculations.class);
		
		final FogOfWarMidTurnVisibilityImpl calc = new FogOfWarMidTurnVisibilityImpl ();
		calc.setFogOfWarCalculations (fow);
		
		// One cell we can see, another that we can't
		when (fow.canSeeMidTurn (FogOfWarStateID.CAN_SEE, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (true);
		when (fow.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN)).thenReturn (false);

		// Set matching states in two locations
		fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		fogOfWarArea.getPlane ().get (1).getRow ().get (10).getCell ().set (21, FogOfWarStateID.NEVER_SEEN);
		
		// The CAE we're trying to see
		final MapCoordinates3DEx caeLocation = new MapCoordinates3DEx (20, 10, 1);

		final MemoryCombatAreaEffect cae = new MemoryCombatAreaEffect ();
		cae.setMapLocation (caeLocation);

		// Method should return the value from wherever the location of the CAE is
		assertTrue (calc.canSeeCombatAreaEffectMidTurn (cae, fogOfWarArea, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN));
		caeLocation.setX (21);
		assertFalse (calc.canSeeCombatAreaEffectMidTurn (cae, fogOfWarArea, FogOfWarValue.ALWAYS_SEE_ONCE_SEEN));
	}
}