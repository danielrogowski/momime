package momime.server.ai;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.SpellSetting;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.server.database.ServerDatabaseEx;

/**
 * Tests the AIUnitCalculationsImpl class
 */
public final class TestAIUnitCalculationsImpl
{
	/**
	 * Tests the canAffordUnitMaintenance method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanAffordUnitMaintenance () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		// Player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, null);
		
		// Session description
		final SpellSetting spellSettings = new SpellSetting ();
		
		// Test unit
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final AvailableUnit unit = new AvailableUnit ();
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (unit, null, null, null, players, fow, db)).thenReturn (xu);
		
		// Resources it consumes
		final Set<String> upkeeps = new HashSet<String> ();
		upkeeps.add (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		upkeeps.add (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		
		when (xu.listModifiedUpkeepProductionTypeIDs ()).thenReturn (upkeeps);
		when (xu.getModifiedUpkeepValue (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD)).thenReturn (5);
		when (xu.getModifiedUpkeepValue (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS)).thenReturn (2);
		
		// Resources we have
		final ResourceValueUtils resources = mock (ResourceValueUtils.class);
		when (resources.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, spellSettings, db)).thenReturn (6);
		when (resources.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, spellSettings, db)).thenReturn (1);
		
		// Set up object to test
		final AIUnitCalculationsImpl ai = new AIUnitCalculationsImpl ();
		ai.setUnitUtils (unitUtils);
		ai.setResourceValueUtils (resources);
		
		// Run method
		assertTrue (ai.canAffordUnitMaintenance (player, players, unit, spellSettings, db));

		when (resources.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, spellSettings, db)).thenReturn (4);
		assertFalse (ai.canAffordUnitMaintenance (player, players, unit, spellSettings, db));
	}
}