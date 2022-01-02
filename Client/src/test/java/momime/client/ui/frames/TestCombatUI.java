package momime.client.ui.frames;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import jakarta.xml.bind.Unmarshaller;
import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.calculations.CombatMapBitmapGenerator;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.CombatScreen;
import momime.client.process.CombatMapProcessing;
import momime.client.ui.PlayerColourImageGenerator;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.TextUtilsImpl;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.client.utils.WizardClientUtils;
import momime.common.calculations.SpellCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CombatMapLayerID;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Language;
import momime.common.database.MapFeatureEx;
import momime.common.database.OverlandMapSize;
import momime.common.database.Plane;
import momime.common.database.SmoothedTile;
import momime.common.database.TileSetEx;
import momime.common.database.TileTypeEx;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillEx;
import momime.common.database.WizardEx;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.movement.UnitMovement;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatPlayers;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.ResourceValueUtils;

/**
 * Tests the CombatUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCombatUI extends ClientTestData
{
	/**
	 * Tests the CombatUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final CombatScreen combatScreenLang = new CombatScreen ();
		combatScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Combat"));
		combatScreenLang.getSpell ().add (createLanguageText (Language.ENGLISH, "Spell"));
		combatScreenLang.getWait ().add (createLanguageText (Language.ENGLISH, "Wait"));
		combatScreenLang.getDone ().add (createLanguageText (Language.ENGLISH, "Done"));
		combatScreenLang.getFlee ().add (createLanguageText (Language.ENGLISH, "Flee"));
		combatScreenLang.getAuto ().add (createLanguageText (Language.ENGLISH, "Auto"));
		
		combatScreenLang.getSkill ().add (createLanguageText (Language.ENGLISH, "Skill"));
		combatScreenLang.getMana ().add (createLanguageText (Language.ENGLISH, "Mana"));
		combatScreenLang.getRange ().add (createLanguageText (Language.ENGLISH, "Range"));
		combatScreenLang.getCastable ().add (createLanguageText (Language.ENGLISH, "Max"));
		combatScreenLang.getAveragePrefix ().add (createLanguageText (Language.ENGLISH, "avg"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getCombatScreen ()).thenReturn (combatScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Plane arcanus = new Plane ();
		when (db.findPlane (0, "smoothCombatMapAndGenerateBitmaps")).thenReturn (arcanus);
		
		final WizardEx monsterWizardEx = new WizardEx ();
		monsterWizardEx.getCombatPlayList ().add ("PLAY_LIST");
		monsterWizardEx.setRandomUtils (mock (RandomUtils.class));
		
		when (db.findWizard (CommonDatabaseConstants.WIZARD_ID_MONSTERS, "initNewCombat")).thenReturn (monsterWizardEx);
		
		final TileSetEx combatMapTileSet = new TileSetEx ();
		combatMapTileSet.setAnimationSpeed (2.0);
		combatMapTileSet.setAnimationFrameCount (3);
		when (db.findTileSet (GraphicsDatabaseConstants.TILE_SET_COMBAT_MAP, "CombatUI")).thenReturn (combatMapTileSet);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitOverlandImageFile ("/momime.client.graphics/units/UN197/overland.png");
		
		final TileTypeEx tileType = new TileTypeEx ();
		when (db.findTileType ("TT01", "CombatUI")).thenReturn (tileType);
		
		final MapFeatureEx mapFeature = new MapFeatureEx ();
		mapFeature.getMapFeatureDescription ().add (createLanguageText (Language.ENGLISH, "Abandoned Keep"));
		mapFeature.getMapFeatureMagicRealm ().add (null);
		when (db.findMapFeature ("MF01", "CombatUI")).thenReturn (mapFeature);
		
		for (int n = 1; n <= 6; n++)
		{
			final CombatAreaEffect cae = new CombatAreaEffect ();
			cae.setCombatAreaEffectImageFile ("/momime.client.graphics/combat/effects/CAE0" + n + ".png");
			
			if ((n != 3) && (n != 6))
				when (db.findCombatAreaEffect ("CAE0" + n, "generateCombatAreaEffectIcons")).thenReturn (cae);
		}
		
		// Overland map
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (terrain);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getClientDB ()).thenReturn (db);
		
		// Combat map
		final CombatMapSize combatMapSize = createCombatMapSize ();
		final MapAreaOfCombatTiles combatMap = createCombatMap (combatMapSize);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		sd.setCombatMapSize (combatMapSize);
		
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// Combat location
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		terrainData.setMapFeatureID ("MF01");
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (terrainData)).thenReturn (false);
		
		// Attacker
		final PlayerDescription atkPd = new PlayerDescription ();
		atkPd.setPlayerID (3);
		atkPd.setHuman (true);
		atkPd.setPlayerName ("Mr. Attacker");
		
		final MomTransientPlayerPublicKnowledge atkTrans = new MomTransientPlayerPublicKnowledge ();
		atkTrans.setFlagColour ("FF0000");

		final PlayerPublicDetails attackingPlayer = new PlayerPublicDetails (atkPd, null, atkTrans);
		
		// Defender
		final PlayerDescription defPd = new PlayerDescription ();
		defPd.setPlayerID (-1);
		defPd.setHuman (false);
		defPd.setPlayerName ("Mr. Defender");
		
		final MomTransientPlayerPublicKnowledge defTrans = new MomTransientPlayerPublicKnowledge ();
		defTrans.setFlagColour ("0000FF");
		
		final PlayerPublicDetails defendingPlayer = new PlayerPublicDetails (defPd, null, defTrans);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		when (client.getPlayers ()).thenReturn (players);
		
		// Wizards
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);

		final KnownWizardDetails atkWizard = new KnownWizardDetails ();
		atkWizard.setWizardID ("WZ01");
		atkWizard.setStandardPhotoID ("WZ01");
		
		final KnownWizardDetails defWizard = new KnownWizardDetails ();
		defWizard.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		defWizard.setStandardPhotoID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		when (knownWizardUtils.findKnownWizardDetails (eq (priv.getKnownWizardDetails ()), eq (defPd.getPlayerID ()), anyString ())).thenReturn (defWizard);
		
		// We're the attacker
		when (client.getOurPlayerID ()).thenReturn (atkPd.getPlayerID ());
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, atkPd.getPlayerID (), "initNewCombat")).thenReturn (attackingPlayer);
		
		// Spell stats
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.calculateModifiedCastingSkill (priv.getResourceValue (), attackingPlayer, players, fow, db, false)).thenReturn (22);
		
		when (resourceValueUtils.findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (97);
		
		final SpellCalculations spellCalculations = mock (SpellCalculations.class);
		when (spellCalculations.calculateDoubleCombatCastingRangePenalty
			(attackingPlayer, new MapCoordinates3DEx (20, 10, 0), false, fow.getMap (), fow.getBuilding (), overlandMapSize)).thenReturn (3);
		
		// Players involved in combat
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (new MapCoordinates3DEx (20, 10, 0), fow.getUnit (), players, db)).thenReturn
			(new CombatPlayers (attackingPlayer, defendingPlayer));
		
		// Player name generator
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		when (wizardClientUtils.getPlayerName (defendingPlayer)).thenReturn ("Rampaging Monsters");	// This name doesn't appear, because its overwritten by "Abandoned Keep"
		when (wizardClientUtils.getPlayerName (attackingPlayer)).thenReturn (atkPd.getPlayerName ());
		
		// CAEs
		final MemoryCombatAreaEffect cae1 = new MemoryCombatAreaEffect ();
		cae1.setCombatAreaEffectID ("CAE01");
		fow.getCombatAreaEffect ().add (cae1);

		final MemoryCombatAreaEffect cae2 = new MemoryCombatAreaEffect ();
		cae2.setCombatAreaEffectID ("CAE02");
		cae2.setMapLocation (new MapCoordinates3DEx (20, 10, 0));
		fow.getCombatAreaEffect ().add (cae2);

		final MemoryCombatAreaEffect cae3 = new MemoryCombatAreaEffect ();
		cae3.setCombatAreaEffectID ("CAE03");
		cae3.setMapLocation (new MapCoordinates3DEx (21, 10, 0));		// <-- wrong location
		fow.getCombatAreaEffect ().add (cae3);
		
		final MemoryCombatAreaEffect cae4 = new MemoryCombatAreaEffect ();
		cae4.setCombatAreaEffectID ("CAE04");
		cae4.setCastingPlayerID (atkPd.getPlayerID ());
		fow.getCombatAreaEffect ().add (cae4);

		final MemoryCombatAreaEffect cae5 = new MemoryCombatAreaEffect ();
		cae5.setCombatAreaEffectID ("CAE05");
		cae5.setCastingPlayerID (defPd.getPlayerID ());
		fow.getCombatAreaEffect ().add (cae5);

		// Give it some dummy images for the terrain
		final BufferedImage [] combatMapBitmaps = new BufferedImage [combatMapTileSet.getAnimationFrameCount ()];
		for (int n = 0; n < combatMapBitmaps.length; n++)
			combatMapBitmaps [n] = createSolidImage (640, 362, (new int [] {0x200000, 0x002000, 0x000020}) [n]);
		
		final CombatMapBitmapGenerator gen = mock (CombatMapBitmapGenerator.class);
		when (gen.generateCombatMapBitmaps (combatMap, arcanus)).thenReturn (combatMapBitmaps);
		
		// Mock other outputs from the bitmap generator, used to draw the building layer
		final SmoothedTile [] [] buildingTiles = new SmoothedTile [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
		
		final Map<CombatMapLayerID, SmoothedTile [] []> smoothedTiles = new HashMap<CombatMapLayerID, SmoothedTile [] []> ();
		smoothedTiles.put (CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES, buildingTiles);
		when (gen.getSmoothedTiles ()).thenReturn (smoothedTiles);
		
		// A dummy unit to select
		final MemoryUnit selectedUnit = new MemoryUnit ();
		selectedUnit.setUnitID ("UN197");
		selectedUnit.setDoubleCombatMovesLeft (2);
		
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		when (unitClientUtils.getUnitName (selectedUnit, UnitNameType.RACE_UNIT_NAME)).thenReturn ("High Elf Swordsmen");
		
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final ExpandedUnitDetails xuSelectedUnit = mock (ExpandedUnitDetails.class);
		when (xuSelectedUnit.getUnitDefinition ()).thenReturn (unitDef);
		when (xuSelectedUnit.getOwningPlayerID ()).thenReturn (atkPd.getPlayerID ());
		when (expand.expandUnitDetails (selectedUnit, null, null, null, players, fow, db)).thenReturn (xuSelectedUnit);

		when (xuSelectedUnit.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT)).thenReturn (true);
		when (xuSelectedUnit.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (true);
		when (xuSelectedUnit.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn (true);
		
		when (xuSelectedUnit.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT)).thenReturn (1);
		when (xuSelectedUnit.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (2);
		when (xuSelectedUnit.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn (3);
		
		final UnitCalculations unitCalc = mock (UnitCalculations.class);
		when (xuSelectedUnit.calculateAliveFigureCount ()).thenReturn (6);
		
		// Unit icons
		when (unitClientUtils.getUnitSkillComponentBreakdownIcon (xuSelectedUnit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn
			(utils.loadImage ("/momime.client.graphics/unitSkills/meleeNormal.png"));

		when (unitClientUtils.getUnitSkillComponentBreakdownIcon (xuSelectedUnit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn
			(utils.loadImage ("/momime.client.graphics/rangedAttacks/rock/iconNormal.png"));
		
		final UnitSkillEx movementSkill = new UnitSkillEx ();
		movementSkill.setMovementIconImageFile ("/momime.client.graphics/unitSkills/USX01-move.png");
		
		when (unitCalc.findPreferredMovementSkillGraphics (xuSelectedUnit, db)).thenReturn (movementSkill);
		
		// Image generator
		final PlayerColourImageGenerator generator = mock (PlayerColourImageGenerator.class);
		when (generator.getOverlandUnitImage (unitDef, atkPd.getPlayerID ())).thenReturn (utils.loadImage (unitDef.getUnitOverlandImageFile ()));
		
		// Layouts
		final Unmarshaller unmarshaller = createXmlLayoutUnmarshaller ();
		final XmlLayoutContainerEx mainLayout = (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/CombatUI-Main.xml"));
		final XmlLayoutContainerEx bottomLayout = (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/CombatUI-Bottom.xml"));
		mainLayout.buildMaps ();
		bottomLayout.buildMaps ();
		
		// Set up form
		final UnitMovement unitMovement = mock (UnitMovement.class);
		
		final CombatUI combat = new CombatUI ();
		combat.setUtils (utils);
		combat.setLanguageHolder (langHolder);
		combat.setLanguageChangeMaster (langMaster);
		combat.setCombatMapBitmapGenerator (gen);
		combat.setCombatMapUtils (combatMapUtils);
		combat.setClient (client);
		combat.setMultiplayerSessionUtils (multiplayerSessionUtils);
		combat.setResourceValueUtils (resourceValueUtils);
		combat.setWizardClientUtils (wizardClientUtils);
		combat.setMemoryGridCellUtils (memoryGridCellUtils);
		combat.setSpellCalculations (spellCalculations);
		combat.setUnitClientUtils (unitClientUtils);
		combat.setCombatLocation (new MapCoordinates3DEx (20, 10, 0));
		combat.setMusicPlayer (mock (AudioPlayer.class));
		combat.setCombatMapProcessing (mock (CombatMapProcessing.class));
		combat.setUnitMovement (unitMovement);
		combat.setUnitCalculations (unitCalc);
		combat.setExpandUnitDetails (expand);
		combat.setTextUtils (new TextUtilsImpl ());
		combat.setSpellBookUI (new SpellBookUI ());
		combat.setSmallFont (CreateFontsForTests.getSmallFont ());
		combat.setMediumFont (CreateFontsForTests.getMediumFont ());
		combat.setLargeFont (CreateFontsForTests.getLargeFont ());
		combat.setCombatLayoutMain (mainLayout);
		combat.setCombatLayoutBottom (bottomLayout);
		combat.setCombatTerrain (combatMap);
		combat.setPlayerColourImageGenerator (generator);
		combat.setKnownWizardUtils (knownWizardUtils);

		// Display form
		combat.initNewCombat ();
		combat.setVisible (true);
		combat.setSelectedUnitInCombat (selectedUnit);
		
		Thread.sleep (5000);
		combat.setVisible (false);
	}
}