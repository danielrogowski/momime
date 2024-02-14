package momime.server.worldupdates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.PlayerType;

import momime.common.database.CommonDatabase;
import momime.common.database.Spell;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.servertoclient.SwitchOffMaintainedSpellMessage;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.FogOfWarMidTurnVisibility;
import momime.server.messages.MomGeneralServerKnowledge;

/**
 * Tests the SwitchOffSpellUpdate class
 */
@ExtendWith(MockitoExtension.class)
public final class TestSwitchOffSpellUpdate
{
	/**
	 * Tests the process method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcess () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Spell spellDef = new Spell ();
		when (db.findSpell ("SP001", "SwitchOffSpellUpdate")).thenReturn (spellDef);
		
		// Server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = new MapVolumeOfMemoryGridCells (); 
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		// The spell being switched off
		final MemoryMaintainedSpell trueSpell = new MemoryMaintainedSpell ();
		trueSpell.setSpellURN (101);
		trueSpell.setSpellID ("SP001");
		trueSpell.setCastingPlayerID (4);
		
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		when (memoryMaintainedSpellUtils.findSpellURN (trueSpell.getSpellURN (), trueMap.getMaintainedSpell (), "SwitchOffSpellUpdate")).thenReturn (trueSpell);

		// Session variables
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Players can see the spell or not, and be human/AI, so create 4 players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final FogOfWarMidTurnVisibility vis = mock (FogOfWarMidTurnVisibility.class);

		int playerID = 0;
		for (final boolean canSee : new boolean [] {false, true})
			for (final boolean human : new boolean [] {false, true})
			{
				playerID++;
				
				final PlayerDescription pd = new PlayerDescription ();
				pd.setPlayerID (playerID);
				pd.setPlayerType (human ? PlayerType.HUMAN : PlayerType.AI);
				
				// Need to make the spell lists unique for verify to work correctly
				final FogOfWarMemory fow = new FogOfWarMemory ();
				final MemoryMaintainedSpell playerCopyOfSpell = new MemoryMaintainedSpell ();
				fow.getMaintainedSpell ().add (playerCopyOfSpell);
				
				when (memoryMaintainedSpellUtils.findSpellURN (trueSpell.getSpellURN (), fow.getMaintainedSpell ())).thenReturn (playerCopyOfSpell);
				
				final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
				priv.setFogOfWarMemory (fow);
				
				final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
				if (human)
					player.setConnection (new DummyServerToClientConnection ());
				
				players.add (player);

				// Mock whether the player can see the spell
				when (vis.canSeeSpellMidTurn (trueSpell, player, mom)).thenReturn (canSee);
			}

		when (mom.getPlayers ()).thenReturn (players);
		
		// Set up object to test
		final SwitchOffSpellUpdate update = new SwitchOffSpellUpdate ();
		update.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		update.setFogOfWarMidTurnVisibility (vis);
		
		// Run method
		update.setSpellURN (trueSpell.getSpellURN ());
		update.process (mom);

		// Players 1-2 can't even see the spell so shouldn't get the remove method called; players 3-4 should
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
			verify (memoryMaintainedSpellUtils, times (playerIndex < 2 ? 0 : 1)).removeSpellURN (trueSpell.getSpellURN (), priv.getFogOfWarMemory ().getMaintainedSpell ());
		}

		// Only player 4 (can see spell, and is human) should get a message
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final PlayerServerDetails player = players.get (playerIndex);
			if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
			{
				final DummyServerToClientConnection conn = (DummyServerToClientConnection) player.getConnection ();
				if (playerIndex != 3)
					assertEquals (0, conn.getMessages ().size ());
				else
				{
					assertEquals (1, conn.getMessages ().size ());
					
					final SwitchOffMaintainedSpellMessage removeMsg = (SwitchOffMaintainedSpellMessage) conn.getMessages ().get (0);
					assertSame (trueSpell.getSpellURN (), removeMsg.getSpellURN ());
				}
			}
		}

		// Only the casters FOW should be updated
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
			verify (wu, times (playerIndex == (trueSpell.getCastingPlayerID () - 1) ? 1 : 0)).recalculateFogOfWar
				(players.get (playerIndex).getPlayerDescription ().getPlayerID ());
	}
}