package momime.client.ui.components;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

import momime.client.MomClient;
import momime.client.ui.PlayerColourImageGeneratorImpl;
import momime.common.database.CommonDatabase;
import momime.common.database.UnitEx;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;

/**
 * Tests the UnitRowDisplayButton class
 */
@ExtendWith(MockitoExtension.class)
public final class TestUnitRowDisplayButton
{
	/**
	 * Sets up a dummy panel and frame to display a unit button
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitRowDisplayButton () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitOverlandImageFile ("/momime.client.graphics/units/UN176/overland.png");
		when (db.findUnit ("UN176", "UnitRowDisplayButton")).thenReturn (unitDef);
		
		// Set up player
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		
		final MomPersistentPlayerPublicKnowledge pub1 = new MomPersistentPlayerPublicKnowledge ();

		final MomTransientPlayerPublicKnowledge trans1 = new MomTransientPlayerPublicKnowledge ();
		trans1.setFlagColour ("FF8050");
		
		final PlayerPublicDetails player1 = new PlayerPublicDetails (pd1, pub1, trans1);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player1);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getClientDB ()).thenReturn (db);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd1.getPlayerID (), "getModifiedImage")).thenReturn (player1);
		
		// Unit to display
		final MemoryUnit unit = new MemoryUnit ();
		unit.setOwningPlayerID (pd1.getPlayerID ());
		unit.setUnitID ("UN176");

		// Set up button
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setUtils (utils);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		gen.setClient (client);
		
		final UnitRowDisplayButton button = new UnitRowDisplayButton ();
		button.setUtils (utils);
		button.setPlayerColourImageGenerator (gen);
		button.setUnit (unit);
		button.setClient (client);
		button.init ();
		
		// Set up dummy panel to display the button
		final JPanel panel = new JPanel ();
		panel.setPreferredSize (new Dimension (100, 100));
		panel.setLayout (new GridBagLayout ());
		
		panel.add (button, utils.createConstraintsNoFill (0, 0, 1, 1, 0, GridBagConstraintsNoFill.CENTRE));		

		// Set up dummy frame to display the panel
		final JFrame frame = new JFrame ("testUnitRowDisplayButton");
		frame.setContentPane (panel);
		frame.setLocationRelativeTo (null);
		frame.setResizable (false);		// Must turn resizeable off before calling pack, so pack uses the size for the correct type of window decorations
		frame.pack ();
		frame.setPreferredSize (frame.getSize ());
		
		frame.setVisible (true);
		Thread.sleep (5000);
		frame.setVisible (false);
	}
}