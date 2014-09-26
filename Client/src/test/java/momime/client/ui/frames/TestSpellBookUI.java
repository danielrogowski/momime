package momime.client.ui.frames;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.AnimationEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.AnimationFrame;
import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.language.database.v0_9_5.SpellBookSection;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.TextUtilsImpl;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.newgame.v0_9_5.SpellSettingData;
import momime.common.database.v0_9_5.Spell;
import momime.common.database.v0_9_5.SpellBookSectionID;
import momime.common.messages.v0_9_5.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.SpellResearchStatus;
import momime.common.messages.v0_9_5.SpellResearchStatusID;
import momime.common.utils.SpellUtils;

import org.junit.Test;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the SpellBookUI class
 */
public final class TestSpellBookUI
{
	/**
	 * Tests the SpellBookUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSpellBookUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmSpellBook", "Title")).thenReturn ("Spells");
		
		final ProductionType research = new ProductionType ();
		research.setProductionTypeSuffix ("RP");
		when (lang.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH)).thenReturn (research);
		
		final ProductionType mana = new ProductionType ();
		mana.setProductionTypeSuffix ("MP");
		when (lang.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA)).thenReturn (mana);
		
		for (int n = 1; n < 8; n++)
		{
			final SpellBookSection section = new SpellBookSection ();
			section.setSpellBookSectionName ("Spell book section " + (n+1));
			when (lang.findSpellBookSection (SpellBookSectionID.fromValue ("SC0" + n))).thenReturn (section);
		}

		final SpellBookSection sectionResearch = new SpellBookSection ();
		sectionResearch.setSpellBookSectionName ("Researchable spells");
		when (lang.findSpellBookSection (SpellBookSectionID.RESEARCHABLE_NOW)).thenReturn (sectionResearch);
		
		final SpellBookSection sectionUnknown = new SpellBookSection ();
		sectionUnknown.setSpellBookSectionName ("Unknown spells");
		when (lang.findSpellBookSection (SpellBookSectionID.RESEARCHABLE)).thenReturn (sectionUnknown);
		
		for (int n = 0; n < 100; n++)
		{
			final momime.client.language.database.v0_9_5.Spell spell = new momime.client.language.database.v0_9_5.Spell ();
			String spellID = new Integer (n).toString ();
			if (n < 10)
				spellID = "0" + spellID;
			spell.setSpellID ("SP0" + spellID);
			
			spell.setSpellName ("Spell " + spell.getSpellID ());
			spell.setSpellDescription ("This is the long description of what spell " + spell.getSpellID () + " does that appears in the spell book");
			
			when (lang.findSpell (spell.getSpellID ())).thenReturn (spell);
		}
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock entries from the graphics XML
		final AnimationEx pageTurn = new AnimationEx ();
		pageTurn.setAnimationSpeed (5);
		for (int n = 0; n < 4; n++)
		{
			final AnimationFrame frame = new AnimationFrame ();
			frame.setFrameImageFile ("/momime.client.graphics/ui/spellBook/spellBookAnim-frame" + n + ".png");
			
			pageTurn.getFrame ().add (frame);
		}
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findAnimation (SpellBookUI.ANIM_PAGE_TURN, "SpellBookUI")).thenReturn (pageTurn);
		
		final Pick redPick = new Pick ();
		redPick.setPickBookshelfTitleColour ("FF0000");
		when (gfx.findPick ("MB01", "languageOrPageChanged")).thenReturn (redPick);
		
		final Pick greenPick = new Pick ();
		greenPick.setPickBookshelfTitleColour ("00FF00");
		when (gfx.findPick ("MB02", "languageOrPageChanged")).thenReturn (greenPick);
		
		// Session description
		final SpellSettingData spellSettings = new SpellSettingData ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (spellSettings);
		
		// Mock 100 dummy spells
		// SP000 - SP009 are in section SC00
		// SP001 - SP019 are in section SC01
		// and so on
		// SP080 - SP089 are in section SC98 (researchable now)
		// SP090 - SP099 are in section SC99 (in book, can research in future)
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		final List<Spell> spells = new ArrayList<Spell> ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final SpellUtils spellUtils = mock (SpellUtils.class);
		
		for (int n = 0; n < 100; n++)
		{
			final Spell spell = new Spell ();
			String spellID = new Integer (n).toString ();
			if (n < 10)
				spellID = "0" + spellID;
			spell.setSpellID ("SP0" + spellID);
			
			// Cycle colours
			final int realm = n % 3;
			if (realm > 0)
				spell.setSpellRealm ("MB0" + realm);
			
			spell.setOverlandCastingCost (1);		// Actual values are irrelevant, since what's displayed comes from getReducedOverlandCastingCost
			spell.setCombatCastingCost (1);
			spell.setResearchCost (n * 10);
			spells.add (spell);
			
			when (spellUtils.getReducedOverlandCastingCost (spell, pub.getPick (), spellSettings, db)).thenReturn (n * 5);
			when (spellUtils.getReducedCombatCastingCost (spell, pub.getPick (), spellSettings, db)).thenReturn (n * 2);
		}
		doReturn (spells).when (db).getSpell ();
		
		// Research statuses - we know 0 of SP000 - SP009, 1 of SP010 - SP019 and so on
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		for (int m = 1; m < 10; m++)
		{
			final SpellBookSectionID sectionID;
			if (m == 9)
				sectionID = SpellBookSectionID.RESEARCHABLE;
			else if (m == 8)
				sectionID = SpellBookSectionID.RESEARCHABLE_NOW;
			else
				sectionID = SpellBookSectionID.fromValue ("SC0" + m);

			for (int n = 0; n < 10; n++)
			{
				final SpellResearchStatus researchStatus = new SpellResearchStatus ();
				researchStatus.setSpellID ("SP0" + m + n);
				researchStatus.setStatus ((n < m) ? SpellResearchStatusID.AVAILABLE : SpellResearchStatusID.UNAVAILABLE);
				
				when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), researchStatus.getSpellID ())).thenReturn (researchStatus);
				when (spellUtils.getModifiedSectionID (spells.get ((m*10) + n), researchStatus, true)).thenReturn ((n < m) ? sectionID : null);
			}
		}
		
		// Player
		final PlayerPublicDetails ourPlayer = new PlayerPublicDetails (null, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, 2, "languageOrPageChanged")).thenReturn (ourPlayer);
		
		// Mock client
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPlayerID ()).thenReturn (2);
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// Set up form
		final SpellBookUI book = new SpellBookUI ();
		book.setUtils (utils);
		book.setLanguageHolder (langHolder);
		book.setLanguageChangeMaster (langMaster);
		book.setGraphicsDB (gfx);
		book.setClient (client);
		book.setSpellUtils (spellUtils);
		book.setMultiplayerSessionUtils (multiplayerSessionUtils);
		book.setTextUtils (new TextUtilsImpl ());
		book.setSmallFont (CreateFontsForTests.getSmallFont ());
		book.setMediumFont (CreateFontsForTests.getMediumFont ());
		book.setLargeFont (CreateFontsForTests.getLargeFont ());

		// Display form		
		book.setVisible (true);
		Thread.sleep (5000);
	}	
}