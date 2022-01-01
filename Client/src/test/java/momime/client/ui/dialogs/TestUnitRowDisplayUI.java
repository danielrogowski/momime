package momime.client.ui.dialogs;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.Simple;
import momime.client.ui.PlayerColourImageGeneratorImpl;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.components.UnitRowDisplayButton;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Language;
import momime.common.database.Spell;
import momime.common.database.SpellBookSection;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSkillTypeID;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KnownWizardUtils;

/**
 * Tests the UnitRowDisplayUI class
 */
@ExtendWith(MockitoExtension.class)
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
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitOverlandImageFile ("/momime.client.graphics/units/UN176/overland.png");
		when (db.findUnit ("UN176", "UnitRowDisplayButton")).thenReturn (unitDef);
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getCancel ().add (createLanguageText (Language.ENGLISH, "Cancel"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.getSpellName ().add (createLanguageText (Language.ENGLISH, "Endurance"));
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		
		when (db.findSpell ("SP001", "UnitRowDisplayUI")).thenReturn (spell);
		
		final SpellBookSection section = new SpellBookSection ();
		section.getSpellTargetPrompt ().add (createLanguageText (Language.ENGLISH, "Select a friendly unit as the target for your SPELL_NAME spell."));
		when (db.findSpellBookSection (SpellBookSectionID.UNIT_ENCHANTMENTS, "UnitRowDisplayUI")).thenReturn (section);
		
		// Client
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Unit attributes
		final List<UnitSkillEx> unitSkills = new ArrayList<UnitSkillEx> ();
		for (int attrNo = 1; attrNo <= 6; attrNo++)
		{
			final UnitSkillEx attrDef = new UnitSkillEx ();
			attrDef.setUnitSkillID ("UA0" + attrNo);
			attrDef.setUnitSkillTypeID (UnitSkillTypeID.ATTRIBUTE);
			
			unitSkills.add (attrDef);
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
		
		// Wizard
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		final KnownWizardDetails wizardDetails1 = new KnownWizardDetails ();
		when (knownWizardUtils.findKnownWizardDetails (priv.getKnownWizardDetails (), pd1.getPlayerID (), "getModifiedImage")).thenReturn (wizardDetails1);
		
		// FOW memory
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);

		// Unit
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		
		final MemoryUnit unit = new MemoryUnit ();
		unit.setOwningPlayerID (pd1.getPlayerID ());
		unit.setUnitID ("UN176");
		when (unitClientUtils.getUnitName (unit, UnitNameType.RACE_UNIT_NAME)).thenReturn ("Unicorns");

		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unit, null, null, null, players, fow, db)).thenReturn (xu);
		
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		units.add (unit);
		
		// Skills
		final Set<String> unitSkillIDs = new HashSet<String> ();
		for (int skillNo = 1; skillNo <= 3; skillNo++)
		{
			unitSkillIDs.add ("US03" + skillNo);

			when (unitClientUtils.getUnitSkillSingleIcon (xu, "US03" + skillNo)).thenReturn (utils.loadImage ("/momime.client.graphics/unitSkills/US03" + skillNo + "-icon.png"));
			
			final UnitSkillEx skillGfx = new UnitSkillEx ();
			skillGfx.setUnitSkillTypeID (UnitSkillTypeID.NO_VALUE);
			when (db.findUnitSkill ("US03" + skillNo, "UnitRowDisplayUI")).thenReturn (skillGfx);
		}
		when (xu.listModifiedSkillIDs ()).thenReturn (unitSkillIDs);

		// Attributes
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (true);
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (2);
		when (unitClientUtils.getUnitSkillComponentBreakdownIcon (xu, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (meleeIcon);
		
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (true);
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (15);
		when (unitClientUtils.getUnitSkillComponentBreakdownIcon (xu, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (hpIcon);

		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn (false);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT)).thenReturn (false);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (false);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (false);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd1.getPlayerID (), "getModifiedImage")).thenReturn (player1);
		
		// Background generator
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setUtils (utils);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		gen.setClient (client);
		gen.setKnownWizardUtils (knownWizardUtils);
		
		// Component factory
		final UIComponentFactory uiComponentFactory = mock (UIComponentFactory.class);
		when (uiComponentFactory.createUnitRowDisplayButton ()).thenAnswer ((i) ->
		{
			final UnitRowDisplayButton button = new UnitRowDisplayButton ();
			button.setUtils (utils);
			button.setClient (client);
			button.setPlayerColourImageGenerator (gen);
			return button;
		});
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/UnitRowDisplayUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final UnitRowDisplayUI display = new UnitRowDisplayUI ();
		display.setUtils (utils);
		display.setLanguageHolder (langHolder);
		display.setLanguageChangeMaster (langMaster);
		display.setUnits (units);
		display.setTargetSpellID ("SP001");
		display.setClient (client);
		display.setUiComponentFactory (uiComponentFactory);
		display.setUnitClientUtils (unitClientUtils);
		display.setExpandUnitDetails (expand);
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