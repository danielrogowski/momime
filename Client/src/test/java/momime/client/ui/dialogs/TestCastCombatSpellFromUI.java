package momime.client.ui.dialogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.NdgUIUtilsImpl;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.SpellCasting;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.renderer.CastCombatSpellFrom;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.Spell;
import momime.common.database.UnitCanCast;
import momime.common.database.UnitEx;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.NumberedHeroItem;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Tests the CastCombatSpellFromUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCastCombatSpellFromUI extends ClientTestData
{
	/**
	 * Tests the CastCombatSpellFromUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastCombatSpellFromUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock entries from the language XML
		final SpellCasting spellCastingLang = new SpellCasting ();
		spellCastingLang.getWhoWillCastTitle ().add (createLanguageText (Language.ENGLISH, "Select who, or which item, will cast a spell"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSpellCasting ()).thenReturn (spellCastingLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock client
		final MomClient client = mock (MomClient.class);
		when (client.getOurPlayerName ()).thenReturn ("Ariel");
		
		// Unit definition
		final CommonDatabase db = mock (CommonDatabase.class);
		when (client.getClientDB ()).thenReturn (db);
		
		final UnitCanCast unitCanCast = new UnitCanCast ();
		unitCanCast.setUnitSpellID ("SP002");
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.getUnitCanCast ().add (unitCanCast);
		when (db.findUnit ("UN001", "CastCombatSpellFromUI")).thenReturn (unitDef);

		final Spell firstSpell = new Spell ();
		firstSpell.getSpellName ().add (createLanguageText (Language.ENGLISH, "High Prayer"));
		when (db.findSpell ("SP001", "CastCombatSpellFromUI")).thenReturn (firstSpell);

		final Spell secondSpell = new Spell ();
		secondSpell.getSpellName ().add (createLanguageText (Language.ENGLISH, "Web"));
		when (db.findSpell ("SP002", "CastCombatSpellFromUI")).thenReturn (secondSpell);
		
		// Mock casting unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		when (unitClientUtils.getUnitName (unit, UnitNameType.SIMPLE_UNIT_NAME)).thenReturn ("Archangel");
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getMemoryUnit ()).thenReturn (unit);
		when (xu.getUnitID ()).thenReturn ("UN001");
		
		// Mock fixed spells, and item with spell charges
		unit.getFixedSpellsRemaining ().add (1);
		unit.getHeroItemSpellChargesRemaining ().add (-1);
		unit.getHeroItemSpellChargesRemaining ().add (3);
		unit.getHeroItemSpellChargesRemaining ().add (-1);
		
		final NumberedHeroItem item = new NumberedHeroItem ();
		item.setHeroItemName ("Axe of Casting");
		item.setSpellID ("SP001");
		
		final MemoryUnitHeroItemSlot slot = new MemoryUnitHeroItemSlot ();
		slot.setHeroItem (item);
		
		unit.getHeroItemSlot ().add (new MemoryUnitHeroItemSlot ());
		unit.getHeroItemSlot ().add (slot);
		unit.getHeroItemSlot ().add (new MemoryUnitHeroItemSlot ());
		
		// Sample list of casting choices
		final List<CastCombatSpellFrom> castingSources = new ArrayList<CastCombatSpellFrom> ();
		castingSources.add (new CastCombatSpellFrom (null, null, null));
		castingSources.add (new CastCombatSpellFrom (xu, null, null));
		castingSources.add (new CastCombatSpellFrom (xu, 0, null));
		castingSources.add (new CastCombatSpellFrom (xu, null, 1));

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/SelectAdvisorUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final CastCombatSpellFromUI castingPopup = new CastCombatSpellFromUI ();
		castingPopup.setSelectAdvisorLayout (layout);
		castingPopup.setUtils (utils);
		castingPopup.setLanguageHolder (langHolder);
		castingPopup.setLanguageChangeMaster (langMaster);
		castingPopup.setSmallFont (CreateFontsForTests.getSmallFont ());
		castingPopup.setClient (client);
		castingPopup.setUnitClientUtils (unitClientUtils);
		castingPopup.setCastingSources (castingSources);

		// Display form		
		castingPopup.setModal (false);
		castingPopup.setVisible (true);
		Thread.sleep (5000);
		castingPopup.setVisible (false);
	}
}