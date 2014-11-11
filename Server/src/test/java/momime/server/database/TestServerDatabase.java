package momime.server.database;

import static org.junit.Assert.assertEquals;
import momime.server.ServerTestData;

import org.junit.Test;

/**
 * Tests reading the server XSD and XML files
 */
public final class TestServerDatabase
{
	/**
	 * Tests reading the database XML in using JAXB
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testReadDatabase () throws Exception
	{
		final ServerDatabaseExImpl serverDB = (ServerDatabaseExImpl) ServerTestData.loadServerDatabase ();
		
		// This doesn't exhaustively test that every element of the database is loaded correctly, it only checks that the right number of records of each type are loaded
		// Common entities
		assertEquals ("Failed to load correct number of planes",									2,					serverDB.getPlane ().size ());
		assertEquals ("Failed to load correct number of production types",						12,				serverDB.getProductionType ().size ());
		assertEquals ("Failed to load correct number of map features",							20,				serverDB.getMapFeature ().size ());
		assertEquals ("Failed to load correct number of tile types",									15 + 2 + 2,	serverDB.getTileType ().size ());
		assertEquals ("Failed to load correct number of pick types",								2,					serverDB.getPickType ().size ());
		assertEquals ("Failed to load correct number of picks",										5 + 18,			serverDB.getPick ().size ());
		assertEquals ("Failed to load correct number of wizards",									14 + 2,			serverDB.getWizard ().size ());
		assertEquals ("Failed to load correct number of races",										14,				serverDB.getRace ().size ());
		assertEquals ("Failed to load correct number of tax rates",									7,					serverDB.getTaxRate ().size ());
		assertEquals ("Failed to load correct number of buildings",									37,				serverDB.getBuilding ().size ());
		assertEquals ("Failed to load correct number of ranged attack types",					12,				serverDB.getRangedAttackType ().size ());
		assertEquals ("Failed to load correct number of weapon grades",						4,					serverDB.getWeaponGrade ().size ());
		assertEquals ("Failed to load correct number of unit attributes",							7,					serverDB.getUnitAttribute ().size ());
		assertEquals ("Failed to load correct number of unit skills",									116,				serverDB.getUnitSkill ().size ());
		assertEquals ("Failed to load correct number of unit types",								3,					serverDB.getUnitType ().size ());
		assertEquals ("Failed to load correct number of units",										198,				serverDB.getUnit ().size ());
		assertEquals ("Failed to load correct number of unit magic realm/lifeform types",	10,				serverDB.getUnitMagicRealm ().size ());
		assertEquals ("Failed to load correct number of combat area effects",					26,				serverDB.getCombatAreaEffect ().size ());
		assertEquals ("Failed to load correct number of spells",										214,				serverDB.getSpell ().size ());
		assertEquals ("Failed to load correct number of combat tile types",						3 + 3 + 12,	serverDB.getCombatTileType ().size ());
		assertEquals ("Failed to load correct number of combat tile borders",					5,					serverDB.getCombatTileBorder ().size ());

		// Server only entities
		assertEquals ("Failed to load correct number of city sizes",									6,					serverDB.getCitySize ().size ());
		assertEquals ("Failed to load correct number of hero item slot types",					6,					serverDB.getHeroItemSlotType ().size ());
		assertEquals ("Failed to load correct number of movement rate rules",				69,				serverDB.getMovementRateRule ().size ());
		assertEquals ("Failed to load correct number of combat map elements",				106,				serverDB.getCombatMapElement ().size ());

		// Server only entities - new game settings
		assertEquals ("Failed to load correct number of map sizes",								1,					serverDB.getMapSize ().size ());
		assertEquals ("Failed to load correct number of land proportions",						3,					serverDB.getLandProportion ().size ());
		assertEquals ("Failed to load correct number of node strengths",						3,					serverDB.getNodeStrength ().size ());
		assertEquals ("Failed to load correct number of difficulty levels",							5,					serverDB.getDifficultyLevel ().size ());
		assertEquals ("Failed to load correct number of fog of war settings",					2,					serverDB.getFogOfWarSetting ().size ());
		assertEquals ("Failed to load correct number of unit settings",							2,					serverDB.getUnitSetting ().size ());
		assertEquals ("Failed to load correct number of spell settings",							2,					serverDB.getSpellSetting ().size ());

		// Server only useless entities
		assertEquals ("Failed to load correct number of population task types",				3,					serverDB.getPopulationTask ().size ());
		assertEquals ("Failed to load correct number of hero skill types",						2,					serverDB.getHeroSkillType ().size ());
		assertEquals ("Failed to load correct number of spell ranks",								5,					serverDB.getSpellRank ().size ());
		assertEquals ("Failed to load correct number of city spell effects",						24,				serverDB.getCitySpellEffect ().size ());
		assertEquals ("Failed to load correct number of known servers",							2,					serverDB.getKnownServer ().size ());
		assertEquals ("Failed to load correct number of language text categories",			60,				serverDB.getLanguageCategory ().size ());

		// Second level entities
		assertEquals ("Failed to load correct number of fortress plane productions",		1,					serverDB.getPlane ().get (1).getFortressPlaneProduction ().size ());
		assertEquals ("Failed to load correct number of map feature magic realms",		5,					serverDB.getMapFeature ().get (11).getMapFeatureMagicRealm ().size ());
		assertEquals ("Failed to load correct number of map feature productions",			2,					serverDB.getMapFeature ().get (9).getMapFeatureProduction ().size ());
		assertEquals ("Failed to load correct number of tile type area effects",				2,					serverDB.getTileType ().get (11).getTileTypeAreaEffect ().size ());
		assertEquals ("Failed to load correct number of tile type feature chances",			11,				serverDB.getTileType ().get (0).getTileTypeFeatureChance ().size ());
		assertEquals ("Failed to load correct number of fortress pick type productions",	1,					serverDB.getPickType ().get (0).getFortressPickTypeProduction ().size ());
		assertEquals ("Failed to load correct number of pick type counts",						20,				serverDB.getPickType ().get (0).getPickTypeCount ().size ());
		assertEquals ("Failed to load correct number of pick exclusive froms",					1,					serverDB.getPick ().get (0).getPickExclusiveFrom ().size ());
		assertEquals ("Failed to load correct number of pick free spells",						2,					serverDB.getPick ().get (5 + 4).getPickFreeSpell ().size ());
		assertEquals ("Failed to load correct number of pick pre-requisites",					2,					serverDB.getPick ().get (5 + 6).getPickPrerequisite ().size ());
		assertEquals ("Failed to load correct number of pick production bonuses",			3,					serverDB.getPick ().get (5 + 5).getPickProductionBonus ().size ());
		assertEquals ("Failed to load correct number of wizard pick counts",					21,				serverDB.getWizard ().get (2 + 0).getWizardPickCount ().size ());
		assertEquals ("Failed to load correct number of city names",								20,				serverDB.getRace ().get (0).getCityName ().size ());
		assertEquals ("Failed to load correct number of buildings race cannot build",		3,					serverDB.getRace ().get (0).getRaceCannotBuild ().size ());
		assertEquals ("Failed to load correct number of race population tasks",				3,					serverDB.getRace ().get (0).getRacePopulationTask ().size ());
		assertEquals ("Failed to load correct number of race unrest",								11,				serverDB.getRace ().get (0).getRaceUnrest ().size ());
		assertEquals ("Failed to load correct number of building pop prod modifiers",		1,					serverDB.getBuilding ().get (3).getBuildingPopulationProductionModifier ().size ());
		assertEquals ("Failed to load correct number of building pre-requisites",				2,					serverDB.getBuilding ().get (3).getBuildingPrerequisite ().size ());
		assertEquals ("Failed to load correct number of building requires tile types",		3,					serverDB.getBuilding ().get (11).getBuildingRequiresTileType ().size ());
		assertEquals ("Failed to load correct number of weapon grade attr bonuses",		4,					serverDB.getWeaponGrade ().get (2).getWeaponGradeAttributeBonus ().size ());
		assertEquals ("Failed to load correct number of weapon grade skill bonuses",		1,					serverDB.getWeaponGrade ().get (2).getWeaponGradeSkillBonus ().size ());
		assertEquals ("Failed to load correct number of experience levels",						6,					serverDB.getUnitType ().get (0).getExperienceLevel ().size ());
		assertEquals ("Failed to load correct number of unit hero item types",					3,					serverDB.getUnit ().get (0).getHeroItemType ().size ());
		assertEquals ("Failed to load correct number of hero names",								5,					serverDB.getUnit ().get (0).getHeroName ().size ());
		assertEquals ("Failed to load correct number of unit attribute values",					4,					serverDB.getUnit ().get (0).getUnitAttributeValue ().size ());
		assertEquals ("Failed to load correct number of spells unit can cast",					1,					serverDB.getUnit ().get (50).getUnitCanCast ().size ());
		assertEquals ("Failed to load correct number of unit skills",									3,					serverDB.getUnit ().get (0).getUnitHasSkill ().size ());
		assertEquals ("Failed to load correct number of unit pre-requisites",					2,					serverDB.getUnit ().get (38).getUnitPrerequisite ().size ());
		assertEquals ("Failed to load correct number of unit upkeeps",							1,					serverDB.getUnit ().get (0).getUnitUpkeep ().size ());
		assertEquals ("Failed to load correct number of CAE attribute bonuses",				8,					serverDB.getCombatAreaEffect ().get (1).getCombatAreaEffectAttributeBonus ().size ());
		assertEquals ("Failed to load correct number of CAE skill bonuses",						8,					serverDB.getCombatAreaEffect ().get (1).getCombatAreaEffectSkillBonus ().size ());
		assertEquals ("Failed to load correct number of spell has city effects",					5,					serverDB.getSpell ().get (71).getSpellHasCityEffect ().size ());
		assertEquals ("Failed to load correct number of spell has combat effects",			1,					serverDB.getSpell ().get (196).getSpellHasCombatEffect ().size ());
		assertEquals ("Failed to load correct number of spell upkeeps",							1,					serverDB.getSpell ().get (1).getSpellUpkeep ().size ());
		assertEquals ("Failed to load correct number of spell valid unit targets",				3,					serverDB.getSpell ().get (85).getSpellValidUnitTarget ().size ());
		assertEquals ("Failed to load correct number of spell summoned units",				25,				serverDB.getSpell ().get (207).getSummonedUnit ().size ());
		assertEquals ("Failed to load correct number of spell unit effects",						3,					serverDB.getSpell ().get (92).getUnitSpellEffect ().size ());
		assertEquals ("Failed to load correct number of land proportion planes",				2,					serverDB.getLandProportion ().get (0).getLandProportionPlane ().size ());
		assertEquals ("Failed to load correct number of land proportion tile types",			3,					serverDB.getLandProportion ().get (0).getLandProportionTileType ().size ());
		assertEquals ("Failed to load correct number of node strength planes",				2,					serverDB.getNodeStrength ().get (0).getNodeStrengthPlane ().size ());
		assertEquals ("Failed to load correct number of difficulty level node strengths",	6,					serverDB.getDifficultyLevel ().get (0).getDifficultyLevelNodeStrength ().size ());
		assertEquals ("Failed to load correct number of difficulty level planes",				2,					serverDB.getDifficultyLevel ().get (0).getDifficultyLevelPlane ().size ());
		assertEquals ("Failed to load correct number of language text entries",				2,					serverDB.getLanguageCategory ().get (0).getLanguageEntry ().size ());

		// Third level entities
		assertEquals ("Failed to load correct number of pick type spell counts",				3,					serverDB.getPickType ().get (0).getPickTypeCount ().get (1).getSpellCount ().size ());
		assertEquals ("Failed to load correct number of wizard picks",							3,					serverDB.getWizard ().get (2 + 0).getWizardPickCount ().get (20).getWizardPick ().size ());
		assertEquals ("Failed to load correct number of race pop task production",			2,					serverDB.getRace ().get (0).getRacePopulationTask ().get (0).getRacePopulationTaskProduction ().size ());
		assertEquals ("Failed to load correct number of experience attribute bonuses",	3,					serverDB.getUnitType ().get (0).getExperienceLevel ().get (1).getExperienceAttributeBonus ().size ());
		assertEquals ("Failed to load correct number of experience skill bonuses",			4,					serverDB.getUnitType ().get (0).getExperienceLevel ().get (1).getExperienceSkillBonus ().size ());
	}
}