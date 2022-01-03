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
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSkillWeaponGrade;
import momime.common.database.WeaponGrade;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KnownWizardUtils;

/**
 * Tests the SelectUnitButton class
 */
@ExtendWith(MockitoExtension.class)
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
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unit = new UnitEx ();
		unit.setUnitOverlandImageFile ("/momime.client.graphics/units/UN197/overland.png");
		when (db.findUnit ("UN197", "SelectUnitButton")).thenReturn (unit);
		
		// Set up player
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		
		final MomPersistentPlayerPublicKnowledge pub1 = new MomPersistentPlayerPublicKnowledge ();

		final MomTransientPlayerPublicKnowledge trans1 = new MomTransientPlayerPublicKnowledge ();
		trans1.setFlagColour ("900000");
		
		final PlayerPublicDetails player1 = new PlayerPublicDetails (pd1, pub1, trans1);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player1);
		
		// Player knowledge
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		// Wizard
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		final KnownWizardDetails wizardDetails1 = new KnownWizardDetails ();
		when (knownWizardUtils.findKnownWizardDetails (fow.getWizardDetails (), pd1.getPlayerID (), "getModifiedImage")).thenReturn (wizardDetails1);
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);

		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd1.getPlayerID (), "getModifiedImage")).thenReturn (player1);
		
		// Set up unit
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getOwningPlayerID ()).thenReturn (pd1.getPlayerID ());
		when (xu.getUnitID ()).thenReturn ("UN197");
		
		// Coloured image generator
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setClient (client);
		gen.setUtils (utils);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		gen.setKnownWizardUtils (knownWizardUtils);
		
		// Set up button
		final SelectUnitButton button = new SelectUnitButton ();
		button.setUtils (utils);
		button.setClient (client);
		button.setPlayerColourImageGenerator (gen);
		button.init ();
		
		button.setSelected (true);
		button.setUnit (xu);
		
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
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitOverlandImageFile ("/momime.client.graphics/units/UN102/overland.png");
		when (db.findUnit ("UN102", "SelectUnitButton")).thenReturn (unitDef);
		
		final WeaponGrade wepGradeDef = new WeaponGrade ();
		wepGradeDef.setWeaponGradeNumber (2);
		
		final UnitSkillEx melee = new UnitSkillEx ();
		when (db.findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, "SelectUnitButton")).thenReturn (melee);
		
		final UnitSkillWeaponGrade meleeWeaponGrade = new UnitSkillWeaponGrade ();
		meleeWeaponGrade.setWeaponGradeNumber (2);
		meleeWeaponGrade.setSkillImageFile ("/momime.client.graphics/unitSkills/meleeMithril.png");
		melee.getUnitSkillWeaponGrade ().add (meleeWeaponGrade);
		melee.buildMap ();
		
		// Set up player
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);

		final MomPersistentPlayerPublicKnowledge pub1 = new MomPersistentPlayerPublicKnowledge ();
		
		final MomTransientPlayerPublicKnowledge trans1 = new MomTransientPlayerPublicKnowledge ();
		trans1.setFlagColour ("900000");
		
		final PlayerPublicDetails player1 = new PlayerPublicDetails (pd1, pub1, trans1);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player1);

		// Player knowledge
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		// Wizard
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		final KnownWizardDetails wizardDetails1 = new KnownWizardDetails ();
		when (knownWizardUtils.findKnownWizardDetails (fow.getWizardDetails (), pd1.getPlayerID (), "getModifiedImage")).thenReturn (wizardDetails1);

		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd1.getPlayerID (), "getModifiedImage")).thenReturn (player1);
		
		// Set up unit
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getOwningPlayerID ()).thenReturn (pd1.getPlayerID ());
		when (xu.getUnitID ()).thenReturn ("UN102");
		when (xu.getWeaponGrade ()).thenReturn (wepGradeDef);
		when (xu.getTotalDamageTaken ()).thenReturn (6);
		
		// Experience level
		final ExperienceLevel expLevel = new ExperienceLevel ();
		expLevel.setRingCount (3);
		expLevel.setRingColour ("0000FF");
		
		when (xu.getModifiedExperienceLevel ()).thenReturn (expLevel);
		
		// Hit points
		when (xu.getFullFigureCount ()).thenReturn (5);
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (2);
		
		// Coloured image generator
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setClient (client);
		gen.setUtils (utils);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		gen.setKnownWizardUtils (knownWizardUtils);
		
		// Set up button
		final SelectUnitButton button = new SelectUnitButton ();
		button.setUtils (utils);
		button.setClient (client);
		button.setPlayerColourImageGenerator (gen);
		button.init ();
		
		button.setSelected (true);
		button.setUnit (xu);
		
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