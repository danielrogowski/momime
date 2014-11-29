package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.Unmarshaller;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.calculations.CombatMapBitmapGenerator;
import momime.client.calculations.ClientUnitCalculations;
import momime.client.database.ClientDatabaseEx;
import momime.client.database.MapFeature;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.TileSetEx;
import momime.client.graphics.database.UnitEx;
import momime.client.graphics.database.v0_9_5.CombatAreaEffect;
import momime.client.graphics.database.v0_9_5.UnitSkill;
import momime.client.graphics.database.v0_9_5.Wizard;
import momime.client.graphics.database.v0_9_5.WizardCombatPlayList;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.process.CombatMapProcessing;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.TextUtilsImpl;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.client.utils.WizardClientUtilsImpl;
import momime.common.calculations.SpellCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.TileType;
import momime.common.database.newgame.MapSizeData;
import momime.common.messages.CombatMapSizeData;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatPlayers;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.UnitAttributeComponent;
import momime.common.utils.UnitAttributePositiveNegative;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the CombatUI class
 */
public final class TestCombatUI
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
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmCombat", "Title")).thenReturn ("Combat");
		when (lang.findCategoryEntry ("frmCombat", "Spell")).thenReturn ("Spell");
		when (lang.findCategoryEntry ("frmCombat", "Wait")).thenReturn ("Wait");
		when (lang.findCategoryEntry ("frmCombat", "Done")).thenReturn ("Done");
		when (lang.findCategoryEntry ("frmCombat", "Flee")).thenReturn ("Flee");
		when (lang.findCategoryEntry ("frmCombat", "Auto")).thenReturn ("Auto");

		when (lang.findCategoryEntry ("frmCombat", "Skill")).thenReturn ("Skill");
		when (lang.findCategoryEntry ("frmCombat", "Mana")).thenReturn ("Mana");
		when (lang.findCategoryEntry ("frmCombat", "Range")).thenReturn ("Range");
		when (lang.findCategoryEntry ("frmCombat", "Castable")).thenReturn ("Max");
		when (lang.findCategoryEntry ("frmCombat", "AveragePrefix")).thenReturn ("avg");
		
		when (lang.findWizardName (CommonDatabaseConstants.WIZARD_ID_MONSTERS)).thenReturn ("Rampaging Monsters");
		
		final momime.client.language.database.v0_9_5.MapFeature mapFeatureLang = new momime.client.language.database.v0_9_5.MapFeature ();
		mapFeatureLang.setMapFeatureDescription ("Abandoned Keep");
		when (lang.findMapFeature ("MF01")).thenReturn (mapFeatureLang);

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Mock entries from the graphics XML
		final Wizard monsterWizardGfx = new Wizard ();
		monsterWizardGfx.getCombatPlayList ().add (new WizardCombatPlayList ());
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findWizard (CommonDatabaseConstants.WIZARD_ID_MONSTERS, "initNewCombat")).thenReturn (monsterWizardGfx);
		
		final TileSetEx combatMapTileSet = new TileSetEx ();
		combatMapTileSet.setAnimationSpeed (2.0);
		combatMapTileSet.setAnimationFrameCount (3);
		when (gfx.findTileSet (GraphicsDatabaseConstants.VALUE_TILE_SET_COMBAT_MAP, "CombatUI")).thenReturn (combatMapTileSet);
		
		final UnitEx unitGfx = new UnitEx ();
		unitGfx.setUnitOverlandImageFile ("/momime.client.graphics/units/UN197/overland.png");
		when (gfx.findUnit ("UN197", "setSelectedUnitInCombat")).thenReturn (unitGfx);
		
		for (int n = 1; n <= 6; n++)
		{
			final CombatAreaEffect cae = new CombatAreaEffect ();
			cae.setCombatAreaEffectImageFile ("/momime.client.graphics/combat/effects/CAE0" + n + ".png");
			when (gfx.findCombatAreaEffect ("CAE0" + n, "generateCombatAreaEffectIcons")).thenReturn (cae);
		}
		
		// Mock entries from the client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final TileType tileType = new TileType ();
		when (db.findTileType ("TT01", "CombatUI")).thenReturn (tileType);
		
		final MapFeature mapFeature = new MapFeature ();
		mapFeature.setAnyMagicRealmsDefined (true);
		when (db.findMapFeature ("MF01", "CombatUI")).thenReturn (mapFeature);
		
		// Overland map
		final MapSizeData mapSize = ClientTestData.createMapSizeData ();
		final MapVolumeOfMemoryGridCells terrain = ClientTestData.createOverlandMap (mapSize);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (terrain);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getClientDB ()).thenReturn (db);
		
		// Session description
		final CombatMapSizeData combatMapSize = ClientTestData.createCombatMapSizeData ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (mapSize);
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
		
		final MomPersistentPlayerPublicKnowledge atkPub = new MomPersistentPlayerPublicKnowledge ();
		atkPub.setWizardID ("WZ01");
		atkPub.setStandardPhotoID ("WZ01");
		
		final MomTransientPlayerPublicKnowledge atkTrans = new MomTransientPlayerPublicKnowledge ();
		atkTrans.setFlagColour ("FF0000");

		final PlayerPublicDetails attackingPlayer = new PlayerPublicDetails (atkPd, atkPub, atkTrans);
		
		// Defender
		final PlayerDescription defPd = new PlayerDescription ();
		defPd.setPlayerID (-1);
		defPd.setHuman (false);
		defPd.setPlayerName ("Mr. Defender");
		
		final MomPersistentPlayerPublicKnowledge defPub = new MomPersistentPlayerPublicKnowledge ();
		defPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		defPub.setStandardPhotoID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);

		final MomTransientPlayerPublicKnowledge defTrans = new MomTransientPlayerPublicKnowledge ();
		defTrans.setFlagColour ("0000FF");
		
		final PlayerPublicDetails defendingPlayer = new PlayerPublicDetails (defPd, defPub, defTrans);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		when (client.getPlayers ()).thenReturn (players);

		// We're the attacker
		when (client.getOurPlayerID ()).thenReturn (atkPd.getPlayerID ());
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, atkPd.getPlayerID (), "initNewCombat")).thenReturn (attackingPlayer);
		
		// Spell stats
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.calculateCastingSkillOfPlayer (priv.getResourceValue ())).thenReturn (22);
		
		when (resourceValueUtils.findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA)).thenReturn (97);
		
		final SpellCalculations spellCalculations = mock (SpellCalculations.class);
		when (spellCalculations.calculateDoubleCombatCastingRangePenalty
			(attackingPlayer, new MapCoordinates3DEx (20, 10, 0), false, fow.getMap (), fow.getBuilding (), mapSize)).thenReturn (3);
		
		// Players involved in combat
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (new MapCoordinates3DEx (20, 10, 0), fow.getUnit (), players)).thenReturn
			(new CombatPlayers (attackingPlayer, defendingPlayer));
		
		// Player name generator
		final WizardClientUtilsImpl wizardClientUtils = new WizardClientUtilsImpl ();
		wizardClientUtils.setLanguageHolder (langHolder);
		
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
		{
			final BufferedImage bitmap = new BufferedImage (640, 362, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = bitmap.createGraphics ();
			try
			{
				switch (n)
				{
					case 0:
						g.setColor (new Color (0x200000));
						break;
						
					case 1:
						g.setColor (new Color (0x002000));
						break;
						
					case 2:
						g.setColor (new Color (0x000020));
						break;
				}
				
				g.fillRect (0, 0, 640, 362);
			}
			finally
			{
				g.dispose ();
			}
			combatMapBitmaps [n] = bitmap;
		}
		
		final CombatMapBitmapGenerator gen = mock (CombatMapBitmapGenerator.class);
		when (gen.generateCombatMapBitmaps ()).thenReturn (combatMapBitmaps);
		
		// A dummy unit to select
		final MemoryUnit selectedUnit = new MemoryUnit ();
		selectedUnit.setUnitID ("UN197");
		selectedUnit.setDoubleCombatMovesLeft (2);
		
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		when (unitClientUtils.getUnitName (selectedUnit, UnitNameType.RACE_UNIT_NAME)).thenReturn ("High Elf Swordsmen");
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getModifiedAttributeValue (selectedUnit, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_PLUS_TO_HIT,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (1);
		when (unitUtils.getModifiedAttributeValue (selectedUnit, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (2);
		when (unitUtils.getModifiedAttributeValue (selectedUnit, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (3);
		
		final UnitCalculations unitCalc = mock (UnitCalculations.class);
		when (unitCalc.calculateAliveFigureCount (selectedUnit, players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (6);
		
		// Unit icons
		when (unitClientUtils.getUnitAttributeIcon (selectedUnit, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn
			(utils.loadImage ("/momime.client.graphics/unitAttributes/meleeNormal.png"));

		when (unitClientUtils.getUnitAttributeIcon (selectedUnit, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn
			(utils.loadImage ("/momime.client.graphics/rangedAttacks/rock/iconNormal.png"));
		
		final UnitSkill movementSkill = new UnitSkill ();
		movementSkill.setMovementIconImageFile ("/momime.client.graphics/unitSkills/USX01-move.png");
		
		final ClientUnitCalculations clientUnitCalculations = mock (ClientUnitCalculations.class);
		when (clientUnitCalculations.findPreferredMovementSkillGraphics (selectedUnit)).thenReturn (movementSkill);
		
		// Layouts
		final Unmarshaller unmarshaller = ClientTestData.createXmlLayoutUnmarshaller ();
		final XmlLayoutContainerEx mainLayout = (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/CombatUI-Main.xml"));
		final XmlLayoutContainerEx bottomLayout = (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/CombatUI-Bottom.xml"));
		mainLayout.buildMaps ();
		bottomLayout.buildMaps ();
		
		// Set up form
		final CombatUI combat = new CombatUI ();
		combat.setUtils (utils);
		combat.setLanguageHolder (langHolder);
		combat.setLanguageChangeMaster (langMaster);
		combat.setCombatMapBitmapGenerator (gen);
		combat.setCombatMapUtils (combatMapUtils);
		combat.setClient (client);
		combat.setGraphicsDB (gfx);
		combat.setMultiplayerSessionUtils (multiplayerSessionUtils);
		combat.setResourceValueUtils (resourceValueUtils);
		combat.setWizardClientUtils (wizardClientUtils);
		combat.setMemoryGridCellUtils (memoryGridCellUtils);
		combat.setSpellCalculations (spellCalculations);
		combat.setUnitClientUtils (unitClientUtils);
		combat.setUnitUtils (unitUtils);
		combat.setClientUnitCalculations (clientUnitCalculations);
		combat.setCombatLocation (new MapCoordinates3DEx (20, 10, 0));
		combat.setRandomUtils (mock (RandomUtils.class));
		combat.setMusicPlayer (mock (AudioPlayer.class));
		combat.setCombatMapProcessing (mock (CombatMapProcessing.class));
		combat.setUnitCalculations (unitCalc);
		combat.setTextUtils (new TextUtilsImpl ());
		combat.setSpellBookUI (new SpellBookUI ());
		combat.setSmallFont (CreateFontsForTests.getSmallFont ());
		combat.setMediumFont (CreateFontsForTests.getMediumFont ());
		combat.setLargeFont (CreateFontsForTests.getLargeFont ());
		combat.setCombatLayoutMain (mainLayout);
		combat.setCombatLayoutBottom (bottomLayout);

		// Display form
		combat.initNewCombat ();
		combat.setVisible (true);
		combat.setSelectedUnitInCombat (selectedUnit);
		
		Thread.sleep (5000);
		combat.setVisible (false);
	}
}