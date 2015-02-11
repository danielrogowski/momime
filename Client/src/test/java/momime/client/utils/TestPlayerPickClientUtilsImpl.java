package momime.client.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.PickLang;
import momime.client.language.database.PickTypeLang;
import momime.common.database.Pick;
import momime.common.database.PickPrerequisite;

import org.junit.Test;

/**
 * Tests the PlayerPickClientUtilsImpl class
 */
public final class TestPlayerPickClientUtilsImpl
{
	/**
	 * Tests the describePickPreRequisites method on a pick with no prerequisites
	 */
	@Test
	public final void testDescribePickPreRequisites_None ()
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
	 */
	@Test
	public final void testDescribePickPreRequisites_SingleSpecific ()
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		final PickLang pickLang = new PickLang ();
		pickLang.setPickDescriptionSingular ("Life Book");
		when (lang.findPick ("MB01")).thenReturn (pickLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Pick to test
		final PickPrerequisite req = new PickPrerequisite ();
		req.setPrerequisiteID ("MB01");
		req.setPrerequisiteCount (1);
		
		final Pick pick = new Pick ();
		pick.getPickPrerequisite ().add (req);
		
		// Set up object to test
		final PlayerPickClientUtilsImpl utils = new PlayerPickClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);

		// Run method
		assertEquals ("1 Life Book", utils.describePickPreRequisites (pick));
	}

	/**
	 * Tests the describePickPreRequisites method on a pick which requires a number of a specific book (e.g. Divine Power)
	 */
	@Test
	public final void testDescribePickPreRequisites_MultipleSpecific ()
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		final PickLang pickLang = new PickLang ();
		pickLang.setPickDescriptionPlural ("Life Books");
		when (lang.findPick ("MB01")).thenReturn (pickLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Pick to test
		final PickPrerequisite req = new PickPrerequisite ();
		req.setPrerequisiteID ("MB01");
		req.setPrerequisiteCount (4);
		
		final Pick pick = new Pick ();
		pick.getPickPrerequisite ().add (req);
		
		// Set up object to test
		final PlayerPickClientUtilsImpl utils = new PlayerPickClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);

		// Run method
		assertEquals ("4 Life Books", utils.describePickPreRequisites (pick));
	}

	/**
	 * Tests the describePickPreRequisites method on a pick which requires a number of multiple types of specific book (e.g. Node Mastery)
	 */
	@Test
	public final void testDescribePickPreRequisites_SpecificList ()
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		final PickLang chaos = new PickLang ();
		chaos.setPickDescriptionSingular ("Chaos Book");
		when (lang.findPick ("MB03")).thenReturn (chaos);
		
		final PickLang nature = new PickLang ();
		nature.setPickDescriptionSingular ("Nature Book");
		when (lang.findPick ("MB04")).thenReturn (nature);
		
		final PickLang sorcery = new PickLang ();
		sorcery.setPickDescriptionSingular ("Sorcery Book");
		when (lang.findPick ("MB05")).thenReturn (sorcery);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
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
		final PlayerPickClientUtilsImpl utils = new PlayerPickClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);

		// Run method
		assertEquals ("1 Chaos Book, 1 Nature Book, 1 Sorcery Book", utils.describePickPreRequisites (pick));
	}

	/**
	 * Tests the describePickPreRequisites method on a pick which requires a single book
	 */
	@Test
	public final void testDescribePickPreRequisites_SingleGeneric ()
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		final PickTypeLang pickTypeLang = new PickTypeLang ();
		pickTypeLang.setPickTypeDescriptionSingular ("Spell Book");
		pickTypeLang.setPickTypePrerequisiteSingular ("PICK_TYPE in any Realm of Magic");
		when (lang.findPickType ("B")).thenReturn (pickTypeLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Pick to test
		final PickPrerequisite req = new PickPrerequisite ();
		req.setPrerequisiteTypeID ("B");
		req.setPrerequisiteCount (1);
		
		final Pick pick = new Pick ();
		pick.getPickPrerequisite ().add (req);
		
		// Set up object to test
		final PlayerPickClientUtilsImpl utils = new PlayerPickClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);

		// Run method
		assertEquals ("1 Spell Book in any Realm of Magic", utils.describePickPreRequisites (pick));
	}

	/**
	 * Tests the describePickPreRequisites method on a pick which requires multiple of a single book
	 * (so 4 life books is OK, 2 life books + 2 chaos books is not, e.g. Archmage)
	 */
	@Test
	public final void testDescribePickPreRequisites_MultipleGeneric ()
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		final PickTypeLang pickTypeLang = new PickTypeLang ();
		pickTypeLang.setPickTypeDescriptionPlural ("Spell Books");
		pickTypeLang.setPickTypePrerequisiteSingular ("PICK_TYPE in any Realm of Magic");
		when (lang.findPickType ("B")).thenReturn (pickTypeLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Pick to test
		final PickPrerequisite req = new PickPrerequisite ();
		req.setPrerequisiteTypeID ("B");
		req.setPrerequisiteCount (4);
		
		final Pick pick = new Pick ();
		pick.getPickPrerequisite ().add (req);
		
		// Set up object to test
		final PlayerPickClientUtilsImpl utils = new PlayerPickClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);

		// Run method
		assertEquals ("4 Spell Books in any Realm of Magic", utils.describePickPreRequisites (pick));
	}

	/**
	 * Tests the describePickPreRequisites method on a pick which requires more than one single book
	 * (so 2 life books doesn't count, it has to be 1 life book + 1 chaos book, e.g. Sage Master) 
	 */
	@Test
	public final void testDescribePickPreRequisites_ManySingleGenerics ()
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		final PickTypeLang pickTypeLang = new PickTypeLang ();
		pickTypeLang.setPickTypeDescriptionSingular ("Spell Book");
		pickTypeLang.setPickTypePrerequisitePlural ("PICK_TYPE in any REPETITIONS Realms of Magic");
		when (lang.findPickType ("B")).thenReturn (pickTypeLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
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

		// Run method
		assertEquals ("1 Spell Book in any 2 Realms of Magic", utils.describePickPreRequisites (pick));
	}

	/**
	 * Tests the describePickPreRequisites method on a pick which requires more than one single book (e.g. Runemaster)
	 */
	@Test
	public final void testDescribePickPreRequisites_ManyMultipleGenerics ()
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		final PickTypeLang pickTypeLang = new PickTypeLang ();
		pickTypeLang.setPickTypeDescriptionPlural ("Spell Books");
		pickTypeLang.setPickTypePrerequisitePlural ("PICK_TYPE in any REPETITIONS Realms of Magic");
		when (lang.findPickType ("B")).thenReturn (pickTypeLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
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

		// Run method
		assertEquals ("2 Spell Books in any 3 Realms of Magic", utils.describePickPreRequisites (pick));
	}
}