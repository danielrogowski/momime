package momime.server.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.SpellSetting;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;

/**
 * Tests the AIUnitCalculationsImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestAIUnitCalculationsImpl
{
	/**
	 * Tests the canAffordUnitMaintenance method on a unit that's constructed in a city (rations+gold maintainence)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanAffordUnitMaintenance_Constructed () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, null);
		
		// Test unit
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		final AvailableUnit unit = new AvailableUnit ();
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unit, null, null, null, players, fow, db)).thenReturn (xu);
		
		// Resources it consumes
		final Set<String> upkeeps = new HashSet<String> ();
		upkeeps.add (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		upkeeps.add (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		
		when (xu.listModifiedUpkeepProductionTypeIDs ()).thenReturn (upkeeps);
		when (xu.getModifiedUpkeepValue (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD)).thenReturn (5);
		
		// Resources we have (note we aren't generating enough rations but that's ignored)
		final SpellSetting spellSettings = new SpellSetting ();

		final ResourceValueUtils resources = mock (ResourceValueUtils.class);
		when (resources.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, spellSettings, db)).thenReturn (6);
		
		// Set up object to test
		final AIUnitCalculationsImpl ai = new AIUnitCalculationsImpl ();
		ai.setExpandUnitDetails (expand);
		ai.setResourceValueUtils (resources);
		
		// Run method
		assertTrue (ai.canAffordUnitMaintenance (player, players, unit, spellSettings, db));

		// Now we produce less, so no longer enough
		when (resources.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, spellSettings, db)).thenReturn (4);
		assertFalse (ai.canAffordUnitMaintenance (player, players, unit, spellSettings, db));
	}

	/**
	 * Tests the canAffordUnitMaintenance method on a unit from a summoning spell (mana maintainence)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanAffordUnitMaintenance_Summoned () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, null);
		
		// Test unit
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		final AvailableUnit unit = new AvailableUnit ();
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unit, null, null, null, players, fow, db)).thenReturn (xu);
		
		// Resources it consumes
		final Set<String> upkeeps = new HashSet<String> ();
		upkeeps.add (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		
		when (xu.listModifiedUpkeepProductionTypeIDs ()).thenReturn (upkeeps);
		when (xu.getModifiedUpkeepValue (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (8);
		
		// Resources we have
		final SpellSetting spellSettings = new SpellSetting ();
		
		final ResourceValueUtils resources = mock (ResourceValueUtils.class);
		when (resources.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, spellSettings, db)).thenReturn (5);
		
		// Set up object to test
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		
		final AIUnitCalculationsImpl ai = new AIUnitCalculationsImpl ();
		ai.setExpandUnitDetails (expand);
		ai.setResourceValueUtils (resources);
		ai.setPlayerPickUtils (playerPickUtils);
		
		// Run method
		assertFalse (ai.canAffordUnitMaintenance (player, players, unit, spellSettings, db));
		
		// Now we have channeler retort, so upkeep reduced to 4
		when (playerPickUtils.getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_CHANNELER)).thenReturn (1);

		// Now we produce less, so no longer enough
		when (resources.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, spellSettings, db)).thenReturn (3);
		assertFalse (ai.canAffordUnitMaintenance (player, players, unit, spellSettings, db));
	}
}