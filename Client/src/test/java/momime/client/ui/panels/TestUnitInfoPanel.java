package momime.client.ui.panels;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import momime.client.MomClient;
import momime.client.calculations.MomClientCityCalculations;
import momime.client.calculations.MomClientUnitCalculations;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.ProductionTypeEx;
import momime.client.graphics.database.RangedAttackTypeEx;
import momime.client.graphics.database.UnitAttributeEx;
import momime.client.graphics.database.v0_9_5.CityViewElement;
import momime.client.graphics.database.v0_9_5.ProductionTypeImage;
import momime.client.graphics.database.v0_9_5.RangedAttackTypeWeaponGrade;
import momime.client.graphics.database.v0_9_5.UnitAttributeWeaponGrade;
import momime.client.graphics.database.v0_9_5.UnitSkill;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.renderer.CellRendererFactory;
import momime.client.ui.renderer.UnitSkillListCellRenderer;
import momime.client.utils.AnimationControllerImpl;
import momime.client.utils.ResourceValueClientUtilsImpl;
import momime.client.utils.TextUtilsImpl;
import momime.common.calculations.MomUnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.v0_9_5.Building;
import momime.common.database.v0_9_5.BuildingPopulationProductionModifier;
import momime.common.database.v0_9_5.Unit;
import momime.common.database.v0_9_5.UnitAttribute;
import momime.common.database.v0_9_5.UnitHasSkill;
import momime.common.database.v0_9_5.UnitUpkeep;
import momime.common.messages.v0_9_5.AvailableUnit;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.MemoryBuilding;
import momime.common.messages.v0_9_5.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.MomUnitAttributeComponent;
import momime.common.utils.MomUnitAttributePositiveNegative;
import momime.common.utils.UnitUtils;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the UnitInfoPanel class
 */
public final class TestUnitInfoPanel
{
	/**
	 * Tests the info panel to display a building
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testBuildingInfoPanel () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		
		when (lang.findCategoryEntry ("frmChangeConstruction", "Upkeep")).thenReturn ("Upkeep");
		when (lang.findCategoryEntry ("frmChangeConstruction", "Moves")).thenReturn ("Moves");
		when (lang.findCategoryEntry ("frmChangeConstruction", "Cost")).thenReturn ("Cost");

		final momime.client.language.database.v0_9_5.Building granaryName = new momime.client.language.database.v0_9_5.Building ();
		granaryName.setBuildingName ("Granary");
		granaryName.setBuildingHelpText ("This is the long description of what a Granary does");
		when (lang.findBuilding ("BL01")).thenReturn (granaryName);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Client city calculations derive language strings
		final MomClientCityCalculations clientCityCalc = mock (MomClientCityCalculations.class);
		when (clientCityCalc.describeWhatBuildingAllows ("BL01", new MapCoordinates3DEx (20, 10, 0))).thenReturn ("This is what the Granary allows");
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock entries from the graphics XML
		final CityViewElement granaryImage = new CityViewElement ();
		granaryImage.setCityViewImageFile ("/momime.client.graphics/cityView/buildings/BL29.png");
		
		final ProductionTypeImage goldImageContainer = new ProductionTypeImage ();
		goldImageContainer.setProductionImageFile ("/momime.client.graphics/production/gold/1.png");
		goldImageContainer.setProductionValue ("1");
		
		final ProductionTypeEx goldImages = new ProductionTypeEx ();
		goldImages.getProductionTypeImage ().add (goldImageContainer);
		goldImages.buildMap ();
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findBuilding (eq ("BL01"), anyString ())).thenReturn (granaryImage);
		when (gfx.findProductionType ("RE01", "generateUpkeepImage")).thenReturn (goldImages);
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final Building granary = new Building ();
		granary.setProductionCost (60);

		final BuildingPopulationProductionModifier upkeep = new BuildingPopulationProductionModifier ();
		upkeep.setProductionTypeID ("RE01");
		upkeep.setDoubleAmount (-4);
		granary.getBuildingPopulationProductionModifier ().add (upkeep);
		
		when (db.findBuilding ("BL01", "showBuilding")).thenReturn (granary);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Set up production image generator
		final ResourceValueClientUtilsImpl resourceValueClientUtils = new ResourceValueClientUtilsImpl ();
		resourceValueClientUtils.setGraphicsDB (gfx);
		resourceValueClientUtils.setUtils (utils);
		
		// Animation controller
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setGraphicsDB (gfx);
		anim.setUtils (utils);
		
		// Set up building to display
		final MemoryBuilding building = new MemoryBuilding ();
		building.setBuildingID ("BL01");
		building.setCityLocation (new MapCoordinates3DEx (20, 10, 0));

		// Cell renderer
		final UnitSkillListCellRenderer renderer = new UnitSkillListCellRenderer ();
		renderer.setLanguageHolder (langHolder);
		renderer.setGraphicsDB (gfx);
		renderer.setUtils (utils);
		
		final CellRendererFactory cellRendererFactory = mock (CellRendererFactory.class);
		when (cellRendererFactory.createUnitSkillListCellRenderer ()).thenReturn (renderer);
		
		// Set up panel
		final UnitInfoPanel panel = new UnitInfoPanel ();
		panel.setUtils (utils);
		panel.setLanguageHolder (langHolder);
		panel.setLanguageChangeMaster (langMaster);
		panel.setClient (client);
		panel.setGraphicsDB (gfx);
		panel.setResourceValueClientUtils (resourceValueClientUtils);
		panel.setAnim (anim);
		panel.setClientCityCalculations (clientCityCalc);
		panel.setTextUtils (new TextUtilsImpl ());
		panel.setCellRendererFactory (cellRendererFactory);
		panel.setMediumFont (CreateFontsForTests.getMediumFont ());
		panel.setSmallFont (CreateFontsForTests.getSmallFont ());
		panel.getPanel ();
		panel.showBuilding (building);

		// Set up a dummy frame to display the panel
		final JFrame frame = new JFrame ("testBuildingInfoPanel");
		frame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		frame.setContentPane (panel.getPanel ());
		frame.pack ();
		frame.setLocationRelativeTo (null);
		frame.setVisible (true);
		
		Thread.sleep (5000);
	}

	/**
	 * Tests the info panel to display a unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitInfoPanel () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		when (lang.findCategoryEntry ("frmChangeConstruction", "Upkeep")).thenReturn ("Upkeep");
		when (lang.findCategoryEntry ("frmChangeConstruction", "Moves")).thenReturn ("Moves");
		when (lang.findCategoryEntry ("frmChangeConstruction", "Cost")).thenReturn ("Cost");
		
		final momime.client.language.database.v0_9_5.Unit longbowmenName = new momime.client.language.database.v0_9_5.Unit ();
		longbowmenName.setUnitName ("Longbowmen");
		when (lang.findUnit ("UN001")).thenReturn (longbowmenName);
		
		int unitAttrNo = 0;
		for (final String unitAttributeDesc : new String [] {"Melee", "Ranged", "+ to Hit", "Defence", "Resistance", "Hit Points", "+ to Block"})
		{
			final momime.client.language.database.v0_9_5.UnitAttribute unitAttrLang = new momime.client.language.database.v0_9_5.UnitAttribute ();
			unitAttrLang.setUnitAttributeDescription (unitAttributeDesc);

			unitAttrNo++;
			when (lang.findUnitAttribute ("UA0" + unitAttrNo)).thenReturn (unitAttrLang);
		}

		for (int n = 1; n <= 5; n++)
		{
			final momime.client.language.database.v0_9_5.UnitSkill skill = new momime.client.language.database.v0_9_5.UnitSkill ();
			skill.setUnitSkillDescription ("Name of skill US0" + n);
			
			when (lang.findUnitSkill ("US0" + n)).thenReturn (skill);
		}
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final ProductionTypeImage goldImageContainer = new ProductionTypeImage ();
		goldImageContainer.setProductionImageFile ("/momime.client.graphics/production/gold/1.png");
		goldImageContainer.setProductionValue ("1");
		
		final ProductionTypeEx goldImages = new ProductionTypeEx ();
		goldImages.getProductionTypeImage ().add (goldImageContainer);
		goldImages.buildMap ();
		when (gfx.findProductionType ("RE01", "generateUpkeepImage")).thenReturn (goldImages);

		final ProductionTypeImage rationsImageContainer = new ProductionTypeImage ();
		rationsImageContainer.setProductionImageFile ("/momime.client.graphics/production/rations/1.png");
		rationsImageContainer.setProductionValue ("1");

		final ProductionTypeEx rationsImages = new ProductionTypeEx ();
		rationsImages.getProductionTypeImage ().add (rationsImageContainer);
		rationsImages.buildMap ();
		when (gfx.findProductionType ("RE02", "generateUpkeepImage")).thenReturn (rationsImages);
		
		unitAttrNo = 0;
		for (final String unitAttributeImage : new String [] {"melee", null, "plusToHit", "defence", "resist", "hitPoints", "plusToBlock"})
		{
			final UnitAttributeEx unitAttrGfx = new UnitAttributeEx ();
			unitAttrNo++;
			
			if (unitAttributeImage != null)
			{
				if ((unitAttrNo == 1) || (unitAttrNo == 4))
				{
					int wepGradeNbr = 0;
					for (final String weaponGrade : new String [] {"Normal", "Alchemy", "Mithril", "Adamantium"})
					{
						final UnitAttributeWeaponGrade attrWeaponGrade = new UnitAttributeWeaponGrade ();
						attrWeaponGrade.setWeaponGradeNumber (wepGradeNbr);
						attrWeaponGrade.setAttributeImageFile ("/momime.client.graphics/unitAttributes/" + unitAttributeImage + weaponGrade + ".png");					
						unitAttrGfx.getUnitAttributeWeaponGrade ().add (attrWeaponGrade);
						
						wepGradeNbr++;
					}
				}
				else
				{
					final UnitAttributeWeaponGrade attrWeaponGrade = new UnitAttributeWeaponGrade ();
					attrWeaponGrade.setAttributeImageFile ("/momime.client.graphics/unitAttributes/" + unitAttributeImage + ".png");					
					unitAttrGfx.getUnitAttributeWeaponGrade ().add (attrWeaponGrade);
				}
			}

			unitAttrGfx.buildMap ();
			when (gfx.findUnitAttribute ("UA0" + unitAttrNo, "unitInfoPanel.paintComponent")).thenReturn (unitAttrGfx);
		}
		
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();
		int wepGradeNbr = 0;
		for (final String weaponGrade : new String [] {"Normal", "Alchemy", "Mithril", "Adamantium"})
		{
			final RangedAttackTypeWeaponGrade ratWeaponGrade = new RangedAttackTypeWeaponGrade ();
			ratWeaponGrade.setWeaponGradeNumber (wepGradeNbr);
			ratWeaponGrade.setUnitDisplayRangedImageFile ("/momime.client.graphics/rangedAttacks/arrow/icon" + weaponGrade + ".png");					
			rat.getRangedAttackTypeWeaponGrade ().add (ratWeaponGrade);
			
			wepGradeNbr++;
		}

		rat.buildMap ();
		when (gfx.findRangedAttackType ("RAT01", "unitInfoPanel.paintComponent")).thenReturn (rat);

		for (int n = 1; n <= 5; n++)
		{
			final UnitSkill skill = new UnitSkill ();
			skill.setUnitSkillImageFile ("/momime.client.graphics/unitSkills/US0" + (n+13) + "-icon.png");
			
			when (gfx.findUnitSkill (eq ("US0" + n), anyString ())).thenReturn (skill);
		}
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final Unit longbowmen = new Unit ();
		longbowmen.setProductionCost (80);
		longbowmen.setRangedAttackType ("RAT01");
		longbowmen.setDoubleMovement (4);

		final UnitUpkeep goldUpkeep = new UnitUpkeep ();
		goldUpkeep.setProductionTypeID ("RE01");
		longbowmen.getUnitUpkeep ().add (goldUpkeep);

		final UnitUpkeep rationsUpkeep = new UnitUpkeep ();
		rationsUpkeep.setProductionTypeID ("RE02");
		longbowmen.getUnitUpkeep ().add (rationsUpkeep);
		
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (longbowmen);
		
		final List<UnitAttribute> unitAttributes = new ArrayList<UnitAttribute> ();
		for (int n = 1; n <= 7; n++)
		{
			final UnitAttribute attr = new UnitAttribute ();
			attr.setUnitAttributeID ("UA0" + n);
			unitAttributes.add (attr);
		}
		doReturn (unitAttributes).when (db).getUnitAttribute ();
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		when (client.getPlayers ()).thenReturn (players);
		
		// FOW memory
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);

		// Set up production image generator
		final ResourceValueClientUtilsImpl resourceValueClientUtils = new ResourceValueClientUtilsImpl ();
		resourceValueClientUtils.setGraphicsDB (gfx);
		resourceValueClientUtils.setUtils (utils);
		
		// Set up unit to display
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		unit.setWeaponGrade (2);
		
		// Skills
		for (int n = 1; n <= 5; n++)
		{
			final UnitHasSkill skill = new UnitHasSkill ();
			skill.setUnitSkillID ("US0" + n);
			unit.getUnitHasSkill ().add (skill);
		}
		
		// Upkeep
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getModifiedUpkeepValue (unit, "RE01", players, db)).thenReturn (2);
		when (unitUtils.getModifiedUpkeepValue (unit, "RE02", players, db)).thenReturn (1);
		
		// Attributes
		final MomUnitCalculations unitCalc = mock (MomUnitCalculations.class);
		for (int n = 1; n <= 7; n++)
		{
			final String attrID = "UA0" + n;
			int attrNo = 0;
			int total = 0;
			for (final MomUnitAttributeComponent attrComponent : MomUnitAttributeComponent.values ())
				if (attrComponent != MomUnitAttributeComponent.ALL)
				{
					attrNo++;
					final int value = (n + attrNo) / 2;
					total = total + value;
				
					when (unitUtils.getModifiedAttributeValue (unit, attrID, attrComponent,
						attrID.equals (CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_HIT_POINTS) ? MomUnitAttributePositiveNegative.BOTH : MomUnitAttributePositiveNegative.POSITIVE,
						players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (value);
				}

			// Lets say we're losing -5 defence from some curse like Black Prayer, and taken -7 damage from HP
			if (attrID.equals (CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_DEFENCE))
				total = total - 5;
			else if (attrID.equals (CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_HIT_POINTS))
				total = total - 7;
			
			if (attrID.equals (CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_HIT_POINTS))
				when (unitCalc.calculateHitPointsRemainingOfFirstFigure (unit, players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (total);
			else
				when (unitUtils.getModifiedAttributeValue (unit, attrID,
					MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (total);
		}
		
		// Movement
		final MomClientUnitCalculations clientUnitCalc = mock (MomClientUnitCalculations.class);
		
		final UnitSkill movementSkill = new UnitSkill ();
		movementSkill.setMovementIconImageFile ("/momime.client.graphics/unitSkills/USX01-move.png");
		when (clientUnitCalc.findPreferredMovementSkillGraphics (unit)).thenReturn (movementSkill);
		
		// Cell renderer
		final UnitStatsLanguageVariableReplacer replacer = mock (UnitStatsLanguageVariableReplacer.class);
		for (int n = 1; n <= 5; n++)
			when (replacer.replaceVariables ("Name of skill US0" + n)).thenReturn ("Name of skill US0" + n);
		
		final UnitSkillListCellRenderer renderer = new UnitSkillListCellRenderer ();
		renderer.setUnitStatsReplacer (replacer);
		renderer.setLanguageHolder (langHolder);
		renderer.setGraphicsDB (gfx);
		renderer.setUtils (utils);
		
		final CellRendererFactory cellRendererFactory = mock (CellRendererFactory.class);
		when (cellRendererFactory.createUnitSkillListCellRenderer ()).thenReturn (renderer);
		
		// Set up panel
		final UnitInfoPanel panel = new UnitInfoPanel ();
		panel.setUtils (utils);
		panel.setLanguageHolder (langHolder);
		panel.setLanguageChangeMaster (langMaster);
		panel.setClient (client);
		panel.setGraphicsDB (gfx);
		panel.setResourceValueClientUtils (resourceValueClientUtils);
		panel.setUnitUtils (unitUtils);
		panel.setUnitCalculations (unitCalc);
		panel.setClientUnitCalculations (clientUnitCalc);
		panel.setCellRendererFactory (cellRendererFactory);
		panel.setTextUtils (new TextUtilsImpl ());
		panel.setMediumFont (CreateFontsForTests.getMediumFont ());
		panel.setSmallFont (CreateFontsForTests.getSmallFont ());
		panel.getPanel ();
		panel.showUnit (unit);

		// Set up a dummy frame to display the panel
		final JFrame frame = new JFrame ("testUnitInfoPanel");
		frame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		frame.setContentPane (panel.getPanel ());
		frame.pack ();
		frame.setLocationRelativeTo (null);
		frame.setVisible (true);
		
		Thread.sleep (5000);
	}
}