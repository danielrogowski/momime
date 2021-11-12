package momime.client.ui.frames;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.SpellBookScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.TextUtilsImpl;
import momime.common.database.AnimationEx;
import momime.common.database.AnimationFrame;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Language;
import momime.common.database.ProductionTypeEx;
import momime.common.database.Spell;
import momime.common.database.SpellBookSection;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellSetting;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;

/**
 * Tests the SpellBookUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestSpellBookUI extends ClientTestData
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

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		for (int n = 1; n <= 2; n++)
		{
			final SpellBookSection section = new SpellBookSection ();
			section.getSpellBookSectionName ().add (createLanguageText (Language.ENGLISH, "Spell book section " + (n+1)));
			when (db.findSpellBookSection (SpellBookSectionID.fromValue ("SC0" + n), "SpellBookUI")).thenReturn (section);
		}
		
		final ProductionTypeEx research = new ProductionTypeEx ();
		research.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "RP"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, "SpellBookUI")).thenReturn (research);
		
		final ProductionTypeEx mana = new ProductionTypeEx ();
		mana.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "MP"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "SpellBookUI")).thenReturn (mana);
		
		// Mock entries from the language XML
		final SpellBookScreen spellBookScreenLang = new SpellBookScreen ();
		spellBookScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Spells"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSpellBookScreen ()).thenReturn (spellBookScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock entries from the graphics XML
		final AnimationEx pageTurn = new AnimationEx ();
		pageTurn.setAnimationSpeed (5);
		for (int n = 1; n <= 4; n++)
		{
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile ("/momime.client.graphics/ui/spellBook/spellBookAnim-frame" + n + ".png");
			pageTurn.getFrame ().add (frame);
		}
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findAnimation (SpellBookUI.ANIM_PAGE_TURN, "SpellBookUI")).thenReturn (pageTurn);
		
		// Session description
		final SpellSetting spellSettings = new SpellSetting ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (spellSettings);
		
		// Mock 100 dummy spells
		// SP000 - SP009 are in section SC00
		// SP001 - SP019 are in section SC01
		// and so on
		// SP080 - SP089 are in section SC98 (researchable now)
		// SP090 - SP099 are in section SC99 (in book, can research in future)
		final List<Spell> spells = new ArrayList<Spell> ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final SpellUtils spellUtils = mock (SpellUtils.class);
		
		for (int n = 0; n < 100; n++)
		{
			final Spell spell = new Spell ();
			String spellID = Integer.valueOf (n).toString ();
			if (n < 10)
				spellID = "0" + spellID;
			spell.setSpellID ("SP0" + spellID);
			
			spell.getSpellName ().add (createLanguageText (Language.ENGLISH, "Spell " + spell.getSpellID ()));
			spell.getSpellDescription ().add (createLanguageText (Language.ENGLISH, "This is the long description of what spell " + spell.getSpellID () + " does that appears in the spell book"));
			
			// Cycle colours
			final int realm = n % 3;
			if (realm > 0)
				spell.setSpellRealm ("MB0" + realm);
			
			spell.setOverlandCastingCost (1);		// Actual values are irrelevant, since what's displayed comes from getReducedOverlandCastingCost
			spell.setCombatCastingCost (1);
			spell.setResearchCost (n * 10);
			spells.add (spell);
			
			if ((n == 10) || (n == 20) || (n == 21))
			{
				when (spellUtils.getReducedOverlandCastingCost (spell, null, null, pub.getPick (), spellSettings, db)).thenReturn (n * 5);
				when (spellUtils.getReducedCombatCastingCost (spell, null, pub.getPick (), spellSettings, db)).thenReturn (n * 2);
			}
		}
		doReturn (spells).when (db).getSpell ();
		
		// Research statuses - we know 0 of SP000 - SP009, 1 of SP010 - SP019 and so on
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		for (int m = 0; m < 10; m++)
		{
			final SpellBookSectionID sectionID;
			if (m == 9)
				sectionID = SpellBookSectionID.RESEARCHABLE;
			else if (m == 8)
				sectionID = SpellBookSectionID.RESEARCHABLE_NOW;
			else if (m == 0)
				sectionID = SpellBookSectionID.SUMMONING;
			else
				sectionID = SpellBookSectionID.fromValue ("SC0" + m);

			for (int n = 0; n < 10; n++)
			{
				final SpellResearchStatus researchStatus = new SpellResearchStatus ();
				researchStatus.setSpellID ("SP0" + m + n);
				researchStatus.setStatus ((n < m) ? SpellResearchStatusID.AVAILABLE : SpellResearchStatusID.UNAVAILABLE);
				
				when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), researchStatus.getSpellID ())).thenReturn (researchStatus);
				when (spellUtils.getModifiedSectionID (spells.get ((m*10) + n), researchStatus.getStatus (), true)).thenReturn ((n < m) ? sectionID : null);
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
		book.setCombatUI (new CombatUI ());
		book.setCastType (SpellCastType.OVERLAND);

		// Display form		
		book.setVisible (true);
		Thread.sleep (5000);
		book.setVisible (false);
	}	
}