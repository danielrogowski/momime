package momime.client.ui.dialogs;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.UnitGfx;
import momime.client.graphics.database.UnitSkillGfx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.SpellBookSectionLang;
import momime.client.language.database.SpellLang;
import momime.client.ui.PlayerColourImageGeneratorImpl;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.components.UnitRowDisplayButton;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitSkill;
import momime.common.database.UnitSkillTypeID;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;

/**
 * Tests the UnitRowDisplayUI class
 */
public final class TestUnitRowDisplayUI extends ClientTestData
{
	/**
	 * Tests the UnitRowDisplayUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitRowDisplayUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		final BufferedImage meleeIcon = utils.loadImage ("/momime.client.graphics/unitSkills/meleeNormal.png");
		final BufferedImage hpIcon = utils.loadImage ("/momime.client.graphics/unitSkills/hitPoints.png");
		
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final UnitGfx unitGfx = new UnitGfx ();
		unitGfx.setUnitOverlandImageFile ("/momime.client.graphics/units/UN176/overland.png");
		when (gfx.findUnit ("UN176", "UnitRowDisplayButton")).thenReturn (unitGfx);
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmUnitRowDisplay", "Cancel")).thenReturn ("Cancel");
		
		final SpellBookSectionLang section = new SpellBookSectionLang ();
		section.setSpellTargetPrompt ("Select a friendly unit as the target for your SPELL_NAME spell.");
		when (lang.findSpellBookSection (SpellBookSectionID.UNIT_ENCHANTMENTS)).thenReturn (section);
		
		final SpellLang spellLang = new SpellLang ();
		spellLang.setSpellName ("Endurance");
		when (lang.findSpell ("SP001")).thenReturn (spellLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		when (db.findSpell ("SP001", "UnitRowDisplayUI")).thenReturn (spell);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Unit attributes
		final List<UnitSkill> unitSkills = new ArrayList<UnitSkill> ();
		for (int attrNo = 1; attrNo <= 6; attrNo++)
		{
			final UnitSkill attrDef = new UnitSkill ();
			attrDef.setUnitSkillID ("UA0" + attrNo);
			unitSkills.add (attrDef);
			
			final UnitSkillGfx attrGfx = new UnitSkillGfx ();
			attrGfx.setUnitSkillTypeID (UnitSkillTypeID.ATTRIBUTE);
			when (gfx.findUnitSkill (attrDef.getUnitSkillID (), "UnitRowDisplayUI")).thenReturn (attrGfx);
		}
		
		doReturn (unitSkills).when (db).getUnitSkills ();
		
		// Set up player
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		
		final MomPersistentPlayerPublicKnowledge pub1 = new MomPersistentPlayerPublicKnowledge ();

		final MomTransientPlayerPublicKnowledge trans1 = new MomTransientPlayerPublicKnowledge ();
		trans1.setFlagColour ("FF8050");
		
		final PlayerPublicDetails player1 = new PlayerPublicDetails (pd1, pub1, trans1);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player1);
		
		when (client.getPlayers ()).thenReturn (players);
		
		// FOW memory
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);

		// Unit
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		
		final MemoryUnit unit = new MemoryUnit ();
		unit.setOwningPlayerID (pd1.getPlayerID ());
		unit.setUnitID ("UN176");
		when (unitClientUtils.getUnitName (unit, UnitNameType.RACE_UNIT_NAME)).thenReturn ("Unicorns");

		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (unit, null, null, null, players, fow, db)).thenReturn (xu);
		
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		units.add (unit);
		
		// Skills
		final Set<String> unitSkillIDs = new HashSet<String> ();
		for (int skillNo = 1; skillNo <= 3; skillNo++)
		{
			unitSkillIDs.add ("US03" + skillNo);

			when (unitClientUtils.getUnitSkillSingleIcon (xu, "US03" + skillNo)).thenReturn (utils.loadImage ("/momime.client.graphics/unitSkills/US03" + skillNo + "-icon.png"));
			
			final UnitSkillGfx skillGfx = new UnitSkillGfx ();
			skillGfx.setUnitSkillTypeID (UnitSkillTypeID.NO_VALUE);
			when (gfx.findUnitSkill ("US03" + skillNo, "UnitRowDisplayUI")).thenReturn (skillGfx);
		}
		when (xu.listModifiedSkillIDs ()).thenReturn (unitSkillIDs);

		// Attributes
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (2);
		when (unitClientUtils.getUnitSkillComponentBreakdownIcon (xu, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (meleeIcon);
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (15);

		when (unitClientUtils.getUnitSkillComponentBreakdownIcon (xu, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (hpIcon);
			
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd1.getPlayerID (), "PlayerColourImageGeneratorImpl")).thenReturn (player1);
		
		// Background generator
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setUtils (utils);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		gen.setClient (client);
		
		// Component factory
		final UIComponentFactory uiComponentFactory = mock (UIComponentFactory.class);
		when (uiComponentFactory.createUnitRowDisplayButton ()).thenAnswer (new Answer<UnitRowDisplayButton> ()
		{
			@Override
			public final UnitRowDisplayButton answer (@SuppressWarnings ("unused") final InvocationOnMock invocation) throws Throwable
			{
				final UnitRowDisplayButton button = new UnitRowDisplayButton ();
				button.setUtils (utils);
				button.setGraphicsDB (gfx);
				button.setPlayerColourImageGenerator (gen);
				return button;
			}
		});
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/UnitRowDisplayUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final UnitRowDisplayUI display = new UnitRowDisplayUI ();
		display.setUtils (utils);
		display.setLanguageHolder (langHolder);
		display.setLanguageChangeMaster (langMaster);
		display.setGraphicsDB (gfx);
		display.setUnits (units);
		display.setTargetSpellID ("SP001");
		display.setClient (client);
		display.setUiComponentFactory (uiComponentFactory);
		display.setUnitClientUtils (unitClientUtils);
		display.setUnitUtils (unitUtils);
		display.setSmallFont (CreateFontsForTests.getSmallFont ());
		display.setMediumFont (CreateFontsForTests.getMediumFont ());
		display.setUnitRowDisplayLayout (layout);
		
		// Display form
		display.setModal (false);
		display.setVisible (true);
		Thread.sleep (5000);
		display.setVisible (false);
	}
}