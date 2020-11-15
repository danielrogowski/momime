package momime.client.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.Simple;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.Pick;
import momime.common.database.PickPrerequisite;
import momime.common.database.PickType;

/**
 * Tests the PlayerPickClientUtilsImpl class
 */
public final class TestPlayerPickClientUtilsImpl extends ClientTestData
{
	/**
	 * Tests the describePickPreRequisites method on a pick with no prerequisites
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDescribePickPreRequisites_None () throws Exception
	{
		// Pick to test
		final Pick pick = new Pick ();
		
		// Set up object to test
		final PlayerPickClientUtilsImpl utils = new PlayerPickClientUtilsImpl ();

		// Run method
		assertNull (utils.describePickPreRequisites (pick));
	}

	/**
	 * Tests the describePickPreRequisites method on a pick which requires a single specific book
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDescribePickPreRequisites_SingleSpecific () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Pick pick = new Pick ();
		pick.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Life Book"));
		when (db.findPick ("MB01", "describePickPreRequisites")).thenReturn (pick);		

		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the language XML
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		
		// Pick to test
		final PickPrerequisite req = new PickPrerequisite ();
		req.setPrerequisiteID ("MB01");
		req.setPrerequisiteCount (1);
		
		final Pick mainPick = new Pick ();
		mainPick.getPickPrerequisite ().add (req);
		
		// Set up object to test
		final PlayerPickClientUtilsImpl utils = new PlayerPickClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setClient (client);
		utils.setTextUtils (new TextUtilsImpl ());

		// Run method
		assertEquals ("1 Life Book", utils.describePickPreRequisites (mainPick));
	}

	/**
	 * Tests the describePickPreRequisites method on a pick which requires a number of a specific book (e.g. Divine Power)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDescribePickPreRequisites_MultipleSpecific () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Pick pick = new Pick ();
		pick.getPickDescriptionPlural ().add (createLanguageText (Language.ENGLISH, "Life Books"));
		when (db.findPick ("MB01", "describePickPreRequisites")).thenReturn (pick);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the language XML
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		
		// Pick to test
		final PickPrerequisite req = new PickPrerequisite ();
		req.setPrerequisiteID ("MB01");
		req.setPrerequisiteCount (4);
		
		final Pick mainPick = new Pick ();
		mainPick.getPickPrerequisite ().add (req);
		
		// Set up object to test
		final PlayerPickClientUtilsImpl utils = new PlayerPickClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setClient (client);
		utils.setTextUtils (new TextUtilsImpl ());

		// Run method
		assertEquals ("4 Life Books", utils.describePickPreRequisites (mainPick));
	}

	/**
	 * Tests the describePickPreRequisites method on a pick which requires a number of multiple types of specific book (e.g. Node Mastery)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDescribePickPreRequisites_SpecificList () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Pick chaos = new Pick ();
		chaos.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Chaos Book"));
		when (db.findPick ("MB03", "describePickPreRequisites")).thenReturn (chaos);
		
		final Pick nature = new Pick ();
		nature.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Nature Book"));
		when (db.findPick ("MB04", "describePickPreRequisites")).thenReturn (nature);
		
		final Pick sorcery = new Pick ();
		sorcery.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Sorcery Book"));
		when (db.findPick ("MB05", "describePickPreRequisites")).thenReturn (sorcery);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getAnd ().add (createLanguageText (Language.ENGLISH, "and"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Pick to test
		final Pick pick = new Pick ();
		for (int n = 3; n <= 5; n++)
		{
			final PickPrerequisite req = new PickPrerequisite ();
			req.setPrerequisiteID ("MB0" + n);
			req.setPrerequisiteCount (1);
			pick.getPickPrerequisite ().add (req);
		}
		
		// Set up object to test
		final TextUtilsImpl textUtils = new TextUtilsImpl ();
		textUtils.setLanguageHolder (langHolder);
		
		final PlayerPickClientUtilsImpl utils = new PlayerPickClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setTextUtils (textUtils);
		utils.setClient (client);

		// Run method
		assertEquals ("1 Chaos Book, 1 Nature Book and 1 Sorcery Book", utils.describePickPreRequisites (pick));
	}

	/**
	 * Tests the describePickPreRequisites method on a pick which requires a single book
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDescribePickPreRequisites_SingleGeneric () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final PickType pickType = new PickType ();
		pickType.getPickTypeDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Spell Book"));
		pickType.getPickTypePrerequisiteSingular ().add (createLanguageText (Language.ENGLISH, "PICK_TYPE in any Realm of Magic"));
		when (db.findPickType ("B", "describePickPreRequisites")).thenReturn (pickType);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the language XML
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		
		// Pick to test
		final PickPrerequisite req = new PickPrerequisite ();
		req.setPrerequisiteTypeID ("B");
		req.setPrerequisiteCount (1);
		
		final Pick pick = new Pick ();
		pick.getPickPrerequisite ().add (req);
		
		// Set up object to test
		final PlayerPickClientUtilsImpl utils = new PlayerPickClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setClient (client);
		utils.setTextUtils (new TextUtilsImpl ());

		// Run method
		assertEquals ("1 Spell Book in any Realm of Magic", utils.describePickPreRequisites (pick));
	}

	/**
	 * Tests the describePickPreRequisites method on a pick which requires multiple of a single book
	 * (so 4 life books is OK, 2 life books + 2 chaos books is not, e.g. Archmage)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDescribePickPreRequisites_MultipleGeneric () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final PickType pickType = new PickType ();
		pickType.getPickTypeDescriptionPlural ().add (createLanguageText (Language.ENGLISH, "Spell Books"));
		pickType.getPickTypePrerequisiteSingular ().add (createLanguageText (Language.ENGLISH, "PICK_TYPE in any Realm of Magic"));
		when (db.findPickType ("B", "describePickPreRequisites")).thenReturn (pickType);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the language XML
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		
		// Pick to test
		final PickPrerequisite req = new PickPrerequisite ();
		req.setPrerequisiteTypeID ("B");
		req.setPrerequisiteCount (4);
		
		final Pick pick = new Pick ();
		pick.getPickPrerequisite ().add (req);
		
		// Set up object to test
		final PlayerPickClientUtilsImpl utils = new PlayerPickClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setClient (client);
		utils.setTextUtils (new TextUtilsImpl ());

		// Run method
		assertEquals ("4 Spell Books in any Realm of Magic", utils.describePickPreRequisites (pick));
	}

	/**
	 * Tests the describePickPreRequisites method on a pick which requires more than one single book
	 * (so 2 life books doesn't count, it has to be 1 life book + 1 chaos book, e.g. Sage Master) 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDescribePickPreRequisites_ManySingleGenerics () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final PickType pickType = new PickType ();
		pickType.getPickTypeDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Spell Book"));
		pickType.getPickTypePrerequisitePlural ().add (createLanguageText (Language.ENGLISH, "PICK_TYPE in any REPETITIONS Realms of Magic"));
		when (db.findPickType ("B", "describePickPreRequisites")).thenReturn (pickType);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the language XML
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		
		// Pick to test
		final Pick pick = new Pick ();
		for (int n = 0; n < 2; n++)
		{
			final PickPrerequisite req = new PickPrerequisite ();
			req.setPrerequisiteTypeID ("B");
			req.setPrerequisiteCount (1);
			pick.getPickPrerequisite ().add (req);
		}
		
		// Set up object to test
		final PlayerPickClientUtilsImpl utils = new PlayerPickClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setClient (client);
		utils.setTextUtils (new TextUtilsImpl ());

		// Run method
		assertEquals ("1 Spell Book in any 2 Realms of Magic", utils.describePickPreRequisites (pick));
	}

	/**
	 * Tests the describePickPreRequisites method on a pick which requires more than one single book (e.g. Runemaster)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDescribePickPreRequisites_ManyMultipleGenerics () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final PickType pickType = new PickType ();
		pickType.getPickTypeDescriptionPlural ().add (createLanguageText (Language.ENGLISH, "Spell Books"));
		pickType.getPickTypePrerequisitePlural ().add (createLanguageText (Language.ENGLISH, "PICK_TYPE in any REPETITIONS Realms of Magic"));
		when (db.findPickType ("B", "describePickPreRequisites")).thenReturn (pickType);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the language XML
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		
		// Pick to test
		final Pick pick = new Pick ();
		for (int n = 0; n < 3; n++)
		{
			final PickPrerequisite req = new PickPrerequisite ();
			req.setPrerequisiteTypeID ("B");
			req.setPrerequisiteCount (2);
			pick.getPickPrerequisite ().add (req);
		}
		
		// Set up object to test
		final PlayerPickClientUtilsImpl utils = new PlayerPickClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setClient (client);
		utils.setTextUtils (new TextUtilsImpl ());

		// Run method
		assertEquals ("2 Spell Books in any 3 Realms of Magic", utils.describePickPreRequisites (pick));
	}
}