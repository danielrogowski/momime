package momime.client.language.database;

import static org.junit.Assert.assertEquals;
import momime.client.language.database.v0_9_5.DifficultyLevel;
import momime.client.language.database.v0_9_5.FogOfWarSetting;
import momime.client.language.database.v0_9_5.LandProportion;
import momime.client.language.database.v0_9_5.LanguageEntry;
import momime.client.language.database.v0_9_5.MapSize;
import momime.client.language.database.v0_9_5.NodeStrength;
import momime.client.language.database.v0_9_5.SpellSetting;
import momime.client.language.database.v0_9_5.UnitSetting;
import momime.client.language.database.v0_9_5.Wizard;

import org.junit.Test;

/**
 * Tests the LanguageDatabaseExImpl class
 */
public final class TestLanguageDatabaseExImpl
{
	/**
	 * Tests the findWizardName method
	 */
	@Test
	public final void findWizardName ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Wizard newWizard = new Wizard ();
			newWizard.setWizardID ("WZ0" + n);
			newWizard.setWizardName ("WZDesc0" + n);
			lang.getWizard ().add (newWizard);
		}

		lang.buildMaps ();

		assertEquals ("WZDesc02", lang.findWizardName ("WZ02"));
		assertEquals ("WZ04", lang.findWizardName ("WZ04"));
	}

	/**
	 * Tests the findMapSizeDescription method
	 */
	@Test
	public final void findMapSizeDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final MapSize newMapSize = new MapSize ();
			newMapSize.setMapSizeID ("MS0" + n);
			newMapSize.setMapSizeDescription ("MSDesc0" + n);
			lang.getMapSize ().add (newMapSize);
		}

		lang.buildMaps ();

		assertEquals ("MSDesc02", lang.findMapSizeDescription ("MS02"));
		assertEquals ("MS04", lang.findMapSizeDescription ("MS04"));
	}
	
	/**
	 * Tests the findLandProportionDescription method
	 */
	@Test
	public final void findLandProportionDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final LandProportion newLandProportion = new LandProportion ();
			newLandProportion.setLandProportionID ("LP0" + n);
			newLandProportion.setLandProportionDescription ("LPDesc0" + n);
			lang.getLandProportion ().add (newLandProportion);
		}

		lang.buildMaps ();

		assertEquals ("LPDesc02", lang.findLandProportionDescription ("LP02"));
		assertEquals ("LP04", lang.findLandProportionDescription ("LP04"));
	}
	
	/**
	 * Tests the findNodeStrengthDescription method
	 */
	@Test
	public final void findNodeStrengthDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final NodeStrength newNodeStrength = new NodeStrength ();
			newNodeStrength.setNodeStrengthID ("NS0" + n);
			newNodeStrength.setNodeStrengthDescription ("NSDesc0" + n);
			lang.getNodeStrength ().add (newNodeStrength);
		}

		lang.buildMaps ();

		assertEquals ("NSDesc02", lang.findNodeStrengthDescription ("NS02"));
		assertEquals ("NS04", lang.findNodeStrengthDescription ("NS04"));
	}
	
	/**
	 * Tests the findDifficultyLevelDescription method
	 */
	@Test
	public final void findDifficultyLevelDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final DifficultyLevel newDifficultyLevel = new DifficultyLevel ();
			newDifficultyLevel.setDifficultyLevelID ("DL0" + n);
			newDifficultyLevel.setDifficultyLevelDescription ("DLDesc0" + n);
			lang.getDifficultyLevel ().add (newDifficultyLevel);
		}

		lang.buildMaps ();

		assertEquals ("DLDesc02", lang.findDifficultyLevelDescription ("DL02"));
		assertEquals ("DL04", lang.findDifficultyLevelDescription ("DL04"));
	}
	
	/**
	 * Tests the findFogOfWarSettingDescription method
	 */
	@Test
	public final void findFogOfWarSettingDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final FogOfWarSetting newFogOfWarSetting = new FogOfWarSetting ();
			newFogOfWarSetting.setFogOfWarSettingID ("FS0" + n);
			newFogOfWarSetting.setFogOfWarSettingDescription ("FSDesc0" + n);
			lang.getFogOfWarSetting ().add (newFogOfWarSetting);
		}

		lang.buildMaps ();

		assertEquals ("FSDesc02", lang.findFogOfWarSettingDescription ("FS02"));
		assertEquals ("FS04", lang.findFogOfWarSettingDescription ("FS04"));
	}
	
	/**
	 * Tests the findUnitSettingDescription method
	 */
	@Test
	public final void findUnitSettingDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSetting newUnitSetting = new UnitSetting ();
			newUnitSetting.setUnitSettingID ("US0" + n);
			newUnitSetting.setUnitSettingDescription ("USDesc0" + n);
			lang.getUnitSetting ().add (newUnitSetting);
		}

		lang.buildMaps ();

		assertEquals ("USDesc02", lang.findUnitSettingDescription ("US02"));
		assertEquals ("US04", lang.findUnitSettingDescription ("US04"));
	}
	
	/**
	 * Tests the findSpellSettingDescription method
	 */
	@Test
	public final void findSpellSettingDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final SpellSetting newSpellSetting = new SpellSetting ();
			newSpellSetting.setSpellSettingID ("SS0" + n);
			newSpellSetting.setSpellSettingDescription ("SSDesc0" + n);
			lang.getSpellSetting ().add (newSpellSetting);
		}

		lang.buildMaps ();

		assertEquals ("SSDesc02", lang.findSpellSettingDescription ("SS02"));
		assertEquals ("SS04", lang.findSpellSettingDescription ("SS04"));
	}
	
	/**
	 * Tests the findCategoryEntry method 
	 */
	@Test
	public final void testFindCategoryEntry ()
	{
		// Create some dummy categories and entries
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		
		for (int catNo = 1; catNo <= 3; catNo++)
		{
			final LanguageCategoryEx cat = new LanguageCategoryEx ();
			cat.setLanguageCategoryID ("C" + catNo);
			
			for (int entryNo = 1; entryNo <= 3; entryNo++)
			{
				final LanguageEntry entry = new LanguageEntry ();
				entry.setLanguageEntryID ("C" + catNo + "E" + entryNo);
				entry.setLanguageEntryText ("Blah" + catNo + entryNo);
				cat.getLanguageEntry ().add (entry);
			}			
			
			lang.getLanguageCategory ().add (cat);
		}
		
		lang.buildMaps ();

		// Entry exists
		assertEquals ("Blah22", lang.findCategoryEntry ("C2", "C2E2"));
		
		// Entry missing
		assertEquals ("C2/C2E4", lang.findCategoryEntry ("C2", "C2E4"));
		
		// Whole category missing
		assertEquals ("C4/C4E4", lang.findCategoryEntry ("C4", "C4E4"));
	}
}
