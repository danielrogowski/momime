package momime.server.ai;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.utils.Holder;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.SpellSvr;
import momime.server.database.UnitSvr;

/**
 * Tests the AISpellCalculationsImpl class
 */
public final class TestAISpellCalculationsImpl
{
	/**
	 * Tests the canAffordSpellMaintenance method on a regular spell with its own maintenance cost
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanAffordSpellMaintenance_Normal () throws Exception
	{
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, null);

		// Test spell
		final ProductionTypeAndUndoubledValue spellUpkeep = new ProductionTypeAndUndoubledValue ();
		spellUpkeep.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		spellUpkeep.setUndoubledProductionValue (6);
		
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellBookSectionID (SpellBookSectionID.OVERLAND_ENCHANTMENTS);
		spell.getSpellUpkeep ().add (spellUpkeep);		
		
		// Resources we have
		final ResourceValueUtils resources = mock (ResourceValueUtils.class);
		when (resources.findAmountPerTurnForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (5);
		
		// Set up object to test
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		
		final AISpellCalculationsImpl ai = new AISpellCalculationsImpl ();
		ai.setPlayerPickUtils (playerPickUtils);
		ai.setResourceValueUtils (resources);
		
		// Run method
		assertFalse (ai.canAffordSpellMaintenance (player, null, spell, null, null));
	
		// Now we have channeler retort, so upkeep reduced to 3
		when (playerPickUtils.getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_CHANNELER)).thenReturn (1);
		assertTrue (ai.canAffordSpellMaintenance (player, null, spell, null, null));

		// Now we produce less, so no longer enough
		when (resources.findAmountPerTurnForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (2);
		assertFalse (ai.canAffordSpellMaintenance (player, null, spell, null, null));
	}

	/**
	 * Tests the canAffordSpellMaintenance method on a summoning spell where the maintenance cost comes from the summoned unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanAffordSpellMaintenance_Summoning () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, null);

		// Test spell
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		
		final List<UnitSvr> unitDefs = new ArrayList<UnitSvr> ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSvr unitDef = new UnitSvr ();
			unitDef.setUnitID ("UN00" + n);
			unitDefs.add (unitDef);
		}
		
		final List<MemoryUnit> trueUnits = new ArrayList<MemoryUnit> ();
		final ServerUnitCalculations serverUnitCalculations = mock (ServerUnitCalculations.class);
		when (serverUnitCalculations.listUnitsSpellMightSummon (spell, player, trueUnits, db)).thenReturn (unitDefs);
		
		// Summoned units
		final AIUnitCalculations aiUnitCalculations = mock (AIUnitCalculations.class);
		
		final ArgumentCaptor<AvailableUnit> unit = ArgumentCaptor.forClass (AvailableUnit.class);
		final Holder<Boolean> canAffordUnit3 = new Holder<Boolean> (false);
		when (aiUnitCalculations.canAffordUnitMaintenance (eq (player), eq (players), unit.capture (), eq (db))).thenAnswer ((i) ->
		{
			final String unitID = unit.getValue ().getUnitID ();
			
			// We can afford units 1 & 2, but 3 is more expensive
			return unitID.equals ("UN003") ? canAffordUnit3.getValue () : true;
		});
		
		// Set up object to test
		final AISpellCalculationsImpl ai = new AISpellCalculationsImpl ();
		ai.setAiUnitCalculations (aiUnitCalculations);
		ai.setUnitUtils (mock (UnitUtils.class));
		ai.setServerUnitCalculations (serverUnitCalculations);
		
		// Run method
		assertFalse (ai.canAffordSpellMaintenance (player, players, spell, trueUnits, db));
		
		// Now something changes (checking resource values, channeler retort and so on are all done inside the mocked method), and we can afford unit 3
		canAffordUnit3.setValue (true);
		assertTrue (ai.canAffordSpellMaintenance (player, players, spell, trueUnits, db));
	}
}