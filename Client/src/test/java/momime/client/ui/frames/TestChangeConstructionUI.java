package momime.client.ui.frames;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.calculations.ClientCityCalculations;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.ChangeConstructionScreen;
import momime.client.languages.database.Simple;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.panels.UnitInfoPanel;
import momime.client.ui.renderer.BuildingListCellRenderer;
import momime.client.ui.renderer.UnitAttributeListCellRenderer;
import momime.client.ui.renderer.UnitListCellRenderer;
import momime.client.ui.renderer.UnitSkillListCellRenderer;
import momime.client.utils.AnimationControllerImpl;
import momime.client.utils.ResourceValueClientUtilsImpl;
import momime.client.utils.TextUtilsImpl;
import momime.common.calculations.CityCalculations;
import momime.common.database.AnimationEx;
import momime.common.database.AnimationFrame;
import momime.common.database.Building;
import momime.common.database.BuildingPopulationProductionModifier;
import momime.common.database.CitySize;
import momime.common.database.CityViewElement;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Language;
import momime.common.database.OverlandMapSize;
import momime.common.database.ProductionTypeEx;
import momime.common.database.ProductionTypeImage;
import momime.common.database.RaceEx;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;

/**
 * Tests the ChangeConstructionUI class
 */
public final class TestChangeConstructionUI extends ClientTestData
{
	/**
	 * Tests the ChangeConstructionUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChangeConstructionUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final CitySize citySize = new CitySize ();
		citySize.getCitySizeName ().add (createLanguageText (Language.ENGLISH, "City of CITY_NAME"));
		when (db.findCitySize (eq ("CS01"), anyString ())).thenReturn (citySize);

		final List<Building> buildings = new ArrayList<Building> ();
		for (int n = 1; n <= 6; n++)
		{
			final Building building = new Building ();
			building.setBuildingID ("BL0" + n);
			building.setProductionCost (n * 50);
			building.getBuildingName ().add (createLanguageText (Language.ENGLISH, "Building " + n));
			
			final BuildingPopulationProductionModifier upkeep = new BuildingPopulationProductionModifier ();
			upkeep.setProductionTypeID ("RE01");
			upkeep.setDoubleAmount (n * -2);
			building.getBuildingPopulationProductionModifier ().add (upkeep);
			
			when (db.findBuilding (eq (building.getBuildingID ()), anyString ())).thenReturn (building);
			buildings.add (building);
		}
		
		final RaceEx race = new RaceEx ();
		race.getRaceCannotBuild ().add ("BL02");
		
		final List<UnitEx> units = new ArrayList<UnitEx> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitEx unit = new UnitEx ();
			unit.setUnitID ("UN00" + n);

//			if (n == 1)
				unit.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
//			else
//				unit.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
			
			if (n == 2)
				unit.setUnitRaceID ("RC01");
			else if (n == 3)
				unit.setUnitRaceID ("RC02");
			
			units.add (unit);
		}
		
		doReturn (buildings).when (db).getBuilding ();
		doReturn (units).when (db).getUnits ();
		when (db.findRace ("RC01", "updateWhatCanBeConstructed")).thenReturn (race);

		final ProductionTypeImage plusOneImageContainer = new ProductionTypeImage ();
		plusOneImageContainer.setProductionImageFile ("/momime.client.graphics/production/gold/1.png");
		plusOneImageContainer.setProductionValue ("1");
		
		final ProductionTypeEx productionTypeImages = new ProductionTypeEx ();
		productionTypeImages.getProductionTypeImage ().add (plusOneImageContainer);
		productionTypeImages.buildMap ();

		when (db.findProductionType ("RE01", "generateUpkeepImage")).thenReturn (productionTypeImages);

		// Some buildings
		final CityViewElement granary = new CityViewElement ();
		granary.setCityViewImageFile ("/momime.client.graphics/cityView/buildings/BL29.png");
		
		final AnimationEx fightersGuildAnim = new AnimationEx ();
		fightersGuildAnim.setAnimationSpeed (4);
		for (int n = 1; n <= 9; n++)
		{
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile ("/momime.client.graphics/cityView/buildings/BL05-frame" + n + ".png");
			fightersGuildAnim.getFrame ().add (frame);
		}
		
		final CityViewElement fightersGuild = new CityViewElement ();
		fightersGuild.setCityViewAnimation ("FIGHTERS_GUILD");
		
		when (db.findCityViewElementBuilding (eq ("BL04"), anyString ())).thenReturn (granary);
		when (db.findCityViewElementBuilding (eq ("BL05"), anyString ())).thenReturn (fightersGuild);
		when (db.findAnimation ("FIGHTERS_GUILD", "registerRepaintTrigger")).thenReturn (fightersGuildAnim);

		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		simpleLang.getCancel ().add (createLanguageText (Language.ENGLISH, "Cancel"));
		
		final ChangeConstructionScreen changeConstructionScreenLang = new ChangeConstructionScreen ();
		changeConstructionScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Change construction for CITY_NAME"));
		changeConstructionScreenLang.getUpkeep ().add (createLanguageText (Language.ENGLISH, "Upkeep"));
		changeConstructionScreenLang.getCost ().add (createLanguageText (Language.ENGLISH, "Cost"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getChangeConstructionScreen ()).thenReturn (changeConstructionScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Client city calculations derive language strings
		final ClientCityCalculations clientCityCalc = mock (ClientCityCalculations.class);
		when (clientCityCalc.describeWhatBuildingAllows ("BL04", new MapCoordinates3DEx (20, 10, 0))).thenReturn ("This is what the Granary allows");
		when (clientCityCalc.describeWhatBuildingAllows ("BL05", new MapCoordinates3DEx (20, 10, 0))).thenReturn ("This is what the Fighters' Guild allows");
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// City data
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCitySizeID ("CS01");
		cityData.setCityName ("Blahdy Blah");

		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (overlandMapSize);
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (terrain);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// What city can construct
		final ClientCityCalculations clientCityCalculations = mock (ClientCityCalculations.class);
		
		final List<Building> buildingsCanConstruct = new ArrayList<Building> ();
		buildingsCanConstruct.add (buildings.get (3));
		buildingsCanConstruct.add (buildings.get (4));
		when (clientCityCalculations.listBuildingsCityCanConstruct (new MapCoordinates3DEx (20, 10, 0))).thenReturn (buildingsCanConstruct);

		// Current construction
		cityData.setCurrentlyConstructingBuildingID ("BL05");
		when (db.findBuilding (eq ("BL04"), anyString ())).thenReturn (buildings.get (3));
		when (db.findBuilding (eq ("BL05"), anyString ())).thenReturn (buildings.get (4));
		
		// Set up production image generator
		final ResourceValueClientUtilsImpl resourceValueClientUtils = new ResourceValueClientUtilsImpl ();
		resourceValueClientUtils.setClient (client);
		resourceValueClientUtils.setUtils (utils);
		
		// Animation controller
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setClient (client);
		anim.setUtils (utils);

		// Cell renderers
		final BuildingListCellRenderer buildingRenderer = new BuildingListCellRenderer ();
		buildingRenderer.setLanguageHolder (langHolder);
		buildingRenderer.setClient (client);
		buildingRenderer.setAnim (anim);
		
		final UnitListCellRenderer unitRenderer = new UnitListCellRenderer ();
		unitRenderer.setLanguageHolder (langHolder);

		final UnitAttributeListCellRenderer attributeRenderer = new UnitAttributeListCellRenderer ();
		attributeRenderer.setLanguageHolder (langHolder);
		
		final UnitSkillListCellRenderer renderer = new UnitSkillListCellRenderer ();
		renderer.setLanguageHolder (langHolder);
		renderer.setClient (client);
		renderer.setUtils (utils);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.panels/UnitInfoPanel.xml"));
		layout.buildMaps ();
		
		// Set up panel
		final CityCalculations cityCalc = mock (CityCalculations.class);

		final UnitInfoPanel panel = new UnitInfoPanel ();
		panel.setUnitInfoLayout (layout);
		panel.setUtils (utils);
		panel.setLanguageHolder (langHolder);
		panel.setLanguageChangeMaster (langMaster);
		panel.setClient (client);
		panel.setResourceValueClientUtils (resourceValueClientUtils);
		panel.setClientCityCalculations (clientCityCalc);
		panel.setUnitSkillListCellRenderer (renderer);
		panel.setUnitAttributeListCellRenderer (attributeRenderer);
		panel.setTextUtils (new TextUtilsImpl ());
		panel.setMediumFont (CreateFontsForTests.getMediumFont ());
		panel.setSmallFont (CreateFontsForTests.getSmallFont ());
		panel.setAnim (anim);
		
		// Set up form
		final ChangeConstructionUI changeConstruction = new ChangeConstructionUI ();
		changeConstruction.setUtils (utils);
		changeConstruction.setLanguageHolder (langHolder);
		changeConstruction.setLanguageChangeMaster (langMaster);
		changeConstruction.setClient (client);
		changeConstruction.setCityCalculations (cityCalc);
		changeConstruction.setClientCityCalculations (clientCityCalculations);
		changeConstruction.setUnitInfoPanel (panel);
		changeConstruction.setBuildingListCellRenderer (buildingRenderer);
		changeConstruction.setUnitListCellRenderer (unitRenderer);
		changeConstruction.setCityLocation (new MapCoordinates3DEx (20, 10, 0));
		changeConstruction.setMediumFont (CreateFontsForTests.getMediumFont ());
		changeConstruction.setSmallFont (CreateFontsForTests.getSmallFont ());
		changeConstruction.setAnim (anim);

		// Display form		
		changeConstruction.setVisible (true);
		Thread.sleep (5000);
		changeConstruction.setVisible (false);
	}
}