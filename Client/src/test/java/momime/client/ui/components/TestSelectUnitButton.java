package momime.client.ui.components;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.UnitGfx;
import momime.client.graphics.database.WeaponGradeGfx;
import momime.client.ui.PlayerColourImageGeneratorImpl;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.UnitAttributeComponent;
import momime.common.database.UnitAttributePositiveNegative;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.utils.UnitSkillUtils;
import momime.common.utils.UnitUtils;

import org.junit.Test;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the SelectUnitButton class
 */
public final class TestSelectUnitButton
{
	/**
	 * Tests a button showing no unit at all (this isn't really a valid situation, but equally it shouldn't throw an exception in this situation either)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSelectUnitButton_NoUnit () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Set up button
		final SelectUnitButton button = new SelectUnitButton ();
		button.setUtils (utils);
		button.init ();

		button.setSelected (true);
		
		// Set up dummy panel to display the button
		final JPanel panel = new JPanel ();
		panel.setPreferredSize (new Dimension (100, 70));
		panel.setLayout (new GridBagLayout ());
		
		panel.add (button, utils.createConstraintsNoFill (0, 0, 1, 1, 0, GridBagConstraintsNoFill.CENTRE));		

		// Set up dummy frame to display the panel
		final JFrame frame = new JFrame ("TestSelectUnitButton");
		frame.setContentPane (panel);
		frame.setLocationRelativeTo (null);
		frame.setResizable (false);		// Must turn resizeable off before calling pack, so pack uses the size for the correct type of window decorations
		frame.pack ();
		frame.setPreferredSize (frame.getSize ());
		
		frame.setVisible (true);
		Thread.sleep (5000);
		frame.setVisible (false);
	}

	/**
	 * Tests a button showing a simple unit with no experience or magic weapons at full health
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSelectUnitButton_SimpleUnit () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final UnitGfx unit = new UnitGfx ();
		unit.setUnitOverlandImageFile ("/momime.client.graphics/units/UN176/overland.png");
		when (gfx.findUnit ("UN176", "SelectUnitButton")).thenReturn (unit);
		
		// Set up player
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		
		final MomPersistentPlayerPublicKnowledge pub1 = new MomPersistentPlayerPublicKnowledge ();

		final MomTransientPlayerPublicKnowledge trans1 = new MomTransientPlayerPublicKnowledge ();
		trans1.setFlagColour ("900000");
		
		final PlayerPublicDetails player1 = new PlayerPublicDetails (pd1, pub1, trans1);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player1);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPlayerID ()).thenReturn (pd1.getPlayerID ());

		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd1.getPlayerID (), "PlayerColourImageGeneratorImpl")).thenReturn (player1);
		
		// Player knowledge
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);
		
		// Set up unit
		final MemoryUnit u = new MemoryUnit ();
		u.setOwningPlayerID (pd1.getPlayerID ());
		u.setUnitID ("UN176");
		
		// Coloured image generator
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setClient (client);
		gen.setUtils (utils);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Set up button
		final SelectUnitButton button = new SelectUnitButton ();
		button.setUtils (utils);
		button.setClient (client);
		button.setGraphicsDB (gfx);
		button.setUnitUtils (mock (UnitUtils.class));
		button.setPlayerColourImageGenerator (gen);
		button.init ();
		
		button.setSelected (true);
		button.setUnit (u);
		
		// Set up dummy panel to display the button
		final JPanel panel = new JPanel ();
		panel.setPreferredSize (new Dimension (100, 70));
		panel.setLayout (new GridBagLayout ());
		
		panel.add (button, utils.createConstraintsNoFill (0, 0, 1, 1, 0, GridBagConstraintsNoFill.CENTRE));		

		// Set up dummy frame to display the panel
		final JFrame frame = new JFrame ("TestSelectUnitButton");
		frame.setContentPane (panel);
		frame.setLocationRelativeTo (null);
		frame.setResizable (false);		// Must turn resizeable off before calling pack, so pack uses the size for the correct type of window decorations
		frame.pack ();
		frame.setPreferredSize (frame.getSize ());
		
		frame.setVisible (true);
		Thread.sleep (5000);
		frame.setVisible (false);
	}

	/**
	 * Tests a button showing a damaged, experienced unit with magic weapons
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSelectUnitButton_ExperiencedUnit () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock entries from the client XML
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final momime.common.database.Unit unitDef = new momime.common.database.Unit ();
		when (db.findUnit ("UN102", "SelectUnitButton")).thenReturn (unitDef);
		
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final UnitGfx unit = new UnitGfx ();
		unit.setUnitOverlandImageFile ("/momime.client.graphics/units/UN102/overland.png");
		when (gfx.findUnit ("UN102", "SelectUnitButton")).thenReturn (unit);
		
		final WeaponGradeGfx wepGrade = new WeaponGradeGfx ();
		wepGrade.setWeaponGradeMiniImageFile ("/momime.client.graphics/weaponGrades/weaponGradeMiniImageMithril.png");
		when (gfx.findWeaponGrade (2, "SelectUnitButton")).thenReturn (wepGrade);
		
		// Set up player
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);

		final MomPersistentPlayerPublicKnowledge pub1 = new MomPersistentPlayerPublicKnowledge ();
		
		final MomTransientPlayerPublicKnowledge trans1 = new MomTransientPlayerPublicKnowledge ();
		trans1.setFlagColour ("900000");
		
		final PlayerPublicDetails player1 = new PlayerPublicDetails (pd1, pub1, trans1);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player1);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPlayerID ()).thenReturn (pd1.getPlayerID ());
		when (client.getClientDB ()).thenReturn (db);

		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd1.getPlayerID (), "PlayerColourImageGeneratorImpl")).thenReturn (player1);
		
		// Player knowledge
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);
		
		// Set up unit
		final MemoryUnit u = new MemoryUnit ();
		u.setOwningPlayerID (pd1.getPlayerID ());
		u.setUnitID ("UN102");
		u.setWeaponGrade (2);
		u.setDamageTaken (6);
		
		// Experience level
		final ExperienceLevel expLevel = new ExperienceLevel ();
		expLevel.setRingCount (3);
		expLevel.setRingColour ("0000FF");
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final UnitSkillUtils unitSkillUtils = mock (UnitSkillUtils.class);
		when (unitUtils.getExperienceLevel (u, true, players, fow.getCombatAreaEffect (), db)).thenReturn (expLevel);
		
		// Hit points
		when (unitUtils.getFullFigureCount (unitDef)).thenReturn (5);
		when (unitSkillUtils.getModifiedAttributeValue (u, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (2);
		
		// Coloured image generator
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setClient (client);
		gen.setUtils (utils);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Set up button
		final SelectUnitButton button = new SelectUnitButton ();
		button.setUtils (utils);
		button.setClient (client);
		button.setGraphicsDB (gfx);
		button.setUnitUtils (unitUtils);
		button.setUnitSkillUtils (unitSkillUtils);
		button.setPlayerColourImageGenerator (gen);
		button.init ();
		
		button.setSelected (true);
		button.setUnit (u);
		
		// Set up dummy panel to display the button
		final JPanel panel = new JPanel ();
		panel.setPreferredSize (new Dimension (100, 70));
		panel.setLayout (new GridBagLayout ());
		
		panel.add (button, utils.createConstraintsNoFill (0, 0, 1, 1, 0, GridBagConstraintsNoFill.CENTRE));		

		// Set up dummy frame to display the panel
		final JFrame frame = new JFrame ("TestSelectUnitButton");
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