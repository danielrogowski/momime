package momime.client.ui.dialogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.SpellLang;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.renderer.CastCombatSpellFrom;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.database.Unit;
import momime.common.database.UnitCanCast;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.NumberedHeroItem;

/**
 * Tests the CastCombatSpellFromUI class
 */
public final class TestCastCombatSpellFromUI
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
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("SpellCasting", "WhoWillCastTitle")).thenReturn ("Select who, or which item, will cast a spell");
		
		final SpellLang firstSpell = new SpellLang ();
		firstSpell.setSpellName ("High Prayer");
		when (lang.findSpell ("SP001")).thenReturn (firstSpell);

		final SpellLang secondSpell = new SpellLang ();
		secondSpell.setSpellName ("Web");
		when (lang.findSpell ("SP002")).thenReturn (secondSpell);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock client
		final MomClient client = mock (MomClient.class);
		when (client.getOurPlayerName ()).thenReturn ("Ariel");
		
		// Unit definition
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		when (client.getClientDB ()).thenReturn (db);
		
		final UnitCanCast unitCanCast = new UnitCanCast ();
		unitCanCast.setUnitSpellID ("SP002");
		
		final Unit unitDef = new Unit ();
		unitDef.getUnitCanCast ().add (unitCanCast);
		when (db.findUnit ("UN001", "CastCombatSpellFromUI")).thenReturn (unitDef);
		
		// Mock casting unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		when (unitClientUtils.getUnitName (unit, UnitNameType.SIMPLE_UNIT_NAME)).thenReturn ("Archangel");
		
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
		castingSources.add (new CastCombatSpellFrom (unit, null, null));
		castingSources.add (new CastCombatSpellFrom (unit, 0, null));
		castingSources.add (new CastCombatSpellFrom (unit, null, 1));

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/SelectAdvisorUI.xml"));
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