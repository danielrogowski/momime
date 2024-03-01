package momime.server;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.springframework.jmx.export.annotation.ManagedOperation;

import com.ndg.multiplayer.base.server.MultiplayerBaseServerThread;
import com.ndg.multiplayer.server.MultiplayerSessionServer;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.sessionbase.SessionDescription;

import jakarta.xml.bind.JAXBException;
import momime.client.database.AvailableDatabase;
import momime.common.database.DifficultyLevel;
import momime.common.database.FogOfWarSetting;
import momime.common.database.HeroItemSetting;
import momime.common.database.LandProportion;
import momime.common.database.NewGameDefaults;
import momime.common.database.NodeStrength;
import momime.common.database.OverlandMapSize;
import momime.common.database.SpellSetting;
import momime.common.database.UnitSetting;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.TurnSystem;
import momime.common.messages.servertoclient.NewGameDatabaseMessage;
import momime.common.utils.CombatMapUtils;

/**
 * Main server class to listen for client connection requests and manage list of sessions
 */
public final class MomServer extends MultiplayerSessionServer
{
	/** Message to send new game database to clients as they connect */
	private NewGameDatabaseMessage newGameDatabaseMessage;

	/** Factory interface for creating MomSessionThreads */
	private MomSessionThreadFactory sessionThreadFactory;

	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/**
	 * @throws DatatypeConfigurationException If there is a problem creating the DatatypeFactory
	 */
	public MomServer () throws DatatypeConfigurationException
	{
		super ("MomServer");
	}
	
	/**
	 * Send new game database to clients as they connect
	 * @param socket Socket on which a new client has connected
	 * @return Thread object to handle requests from this client
	 * @throws InterruptedException If there is a problem waiting for the thread to start up
	 * @throws JAXBException If there is a problem sending something to connecting client
	 * @throws XMLStreamException If there is a problem sending something to connecting client
	 */
	@Override
	protected final MultiplayerBaseServerThread createAndStartClientThread (final Socket socket) throws InterruptedException, JAXBException, XMLStreamException
	{
		final MomClientConnection conn = new MomClientConnection ("ClientConnection-" + socket, getNewGameDatabaseMessage ());
		conn.setServer (this);
		conn.setSocket (socket);
		conn.setSendContext (getServerToClientContext ());
		conn.setReceiveContext (getClientToServerContext ());
		conn.setReceiveObjectFactoryArray (getClientToServerContextFactoryArray ());
		conn.setConversationTag (getConversationTag ());
		conn.setCompress (isCompress ());
		conn.setDecompress (isDecompress ());
		conn.start ();
		
		return conn;
	}
	
	/**
	 * Starts off a new game with just AI players playing each other.  Passing in blank (empty string) for params will use defaults.
	 * 
	 * @param databaseName Name of XML database to use, if there is more than one; can leave blank if there is exactly one
	 * @param aiPlayerCount Number of AI players to put in the game; 0 will add every wizard from the DB
	 * @param overlandMapSizeID Overland map size to use; leaving blank will use the default
	 * @param landProportionID Land proportion to use; leaving blank will use the default
	 * @param nodeStrengthID Node strength to use; leaving blank will use the default
	 * @param difficultyLevelID Difficulty level to use; leaving blank will use the default
	 * @param fogOfWarSettingID Fog of war settings to use; leaving blank will use the default
	 * @param unitSettingID Unit settings to use; leaving blank will use the default
	 * @param spellSettingID Spell settings to use; leaving blank will use the default
	 * @param heroItemSettingID Hero item settings to use; leaving blank will use the default
	 * @throws JAXBException If there is an error dealing with any XML files during creation
	 * @throws XMLStreamException If there is an error dealing with any XML files during creation
	 * @throws IOException If there is another kind of problem
	 */
	@ManagedOperation
	public final void startAIGame (final String databaseName, final int aiPlayerCount, final String overlandMapSizeID, final String landProportionID, final String nodeStrengthID,
		final String difficultyLevelID, final String fogOfWarSettingID, final String unitSettingID, final String spellSettingID, final String heroItemSettingID)
		throws JAXBException, XMLStreamException, IOException
	{
		// First find the database to use
		final List<AvailableDatabase> matchingDatabases = getNewGameDatabaseMessage ().getNewGameDatabase ().getMomimeXmlDatabase ().stream ().filter
			(db -> (db.getDbName ().equals (databaseName)) || (databaseName.isEmpty ())).collect (Collectors.toList ());
		if (matchingDatabases.size () == 0)
			throw new IOException ("No matching XML databases found");
		if (matchingDatabases.size () > 1)
			throw new IOException ("Multiple matching XML databases found");
		
		final AvailableDatabase db = matchingDatabases.get (0);
		
		// Decide ID to use for each setting
		final NewGameDefaults defaults = db.getNewGameDefaults ();
		final String useOverlandMapSizeID = overlandMapSizeID.isEmpty () ? defaults.getDefaultOverlandMapSizeID () : overlandMapSizeID;
	    final String useLandProportionID = landProportionID.isEmpty () ? defaults.getDefaultLandProportionID () : landProportionID;
	    final String useNodeStrengthID = nodeStrengthID.isEmpty () ? defaults.getDefaultNodeStrengthID () : nodeStrengthID;
	    final String useDifficultyLevelID = difficultyLevelID.isBlank () ? defaults.getDefaultDifficultyLevelID () : difficultyLevelID;
	    final String useFogOfWarSettingID = fogOfWarSettingID.isBlank () ? defaults.getDefaultFogOfWarSettingID () : fogOfWarSettingID;
	    final String useUnitSettingID = unitSettingID.isBlank () ? defaults.getDefaultUnitSettingID () : unitSettingID;
	    final String useSpellSettingID = spellSettingID.isBlank () ? defaults.getDefaultSpellSettingID () : spellSettingID;
	    final String useHeroItemSettingID = heroItemSettingID.isBlank () ? defaults.getDefaultHeroItemSettingID () : heroItemSettingID;
	    
	    // Find actual data objects corresponding to each setting ID
	    final List<OverlandMapSize> overlandMapSizes = db.getOverlandMapSize ().stream ().filter (ms -> ms.getOverlandMapSizeID ().equals (useOverlandMapSizeID)).collect (Collectors.toList ());
		if (overlandMapSizes.size () == 0)
			throw new IOException ("No overland map size found");
		if (overlandMapSizes.size () > 1)
			throw new IOException ("Multiple overland map sizes found");

	    final List<LandProportion> landProportions = db.getLandProportion ().stream ().filter (ms -> ms.getLandProportionID ().equals (useLandProportionID)).collect (Collectors.toList ());
		if (landProportions.size () == 0)
			throw new IOException ("No land proportion found");
		if (landProportions.size () > 1)
			throw new IOException ("Multiple land proportions found");

	    final List<NodeStrength> nodeStrengths = db.getNodeStrength ().stream ().filter (ms -> ms.getNodeStrengthID ().equals (useNodeStrengthID)).collect (Collectors.toList ());
		if (nodeStrengths.size () == 0)
			throw new IOException ("No node strength found");
		if (nodeStrengths.size () > 1)
			throw new IOException ("Multiple node strengths found");

	    final List<DifficultyLevel> difficultyLevels = db.getDifficultyLevel ().stream ().filter (ms -> ms.getDifficultyLevelID ().equals (useDifficultyLevelID)).collect (Collectors.toList ());
		if (difficultyLevels.size () == 0)
			throw new IOException ("No difficulty level found");
		if (difficultyLevels.size () > 1)
			throw new IOException ("Multiple difficulty levels found");

	    final List<FogOfWarSetting> fogOfWarSettings = db.getFogOfWarSetting ().stream ().filter (ms -> ms.getFogOfWarSettingID ().equals (useFogOfWarSettingID)).collect (Collectors.toList ());
		if (fogOfWarSettings.size () == 0)
			throw new IOException ("No fog of war setting found");
		if (fogOfWarSettings.size () > 1)
			throw new IOException ("Multiple fog of war settings found");
		
	    final List<UnitSetting> unitSettings = db.getUnitSetting ().stream ().filter (ms -> ms.getUnitSettingID ().equals (useUnitSettingID)).collect (Collectors.toList ());
		if (unitSettings.size () == 0)
			throw new IOException ("No unit setting found");
		if (unitSettings.size () > 1)
			throw new IOException ("Multiple unit settings found");
		
	    final List<SpellSetting> spellSettings = db.getSpellSetting ().stream ().filter (ms -> ms.getSpellSettingID ().equals (useSpellSettingID)).collect (Collectors.toList ());
		if (spellSettings.size () == 0)
			throw new IOException ("No spell setting found");
		if (spellSettings.size () > 1)
			throw new IOException ("Multiple spell settings found");
		
	    final List<HeroItemSetting> heroItemSettings = db.getHeroItemSetting ().stream ().filter (ms -> ms.getHeroItemSettingID ().equals (useHeroItemSettingID)).collect (Collectors.toList ());
		if (heroItemSettings.size () == 0)
			throw new IOException ("No hero item setting found");
		if (heroItemSettings.size () > 1)
			throw new IOException ("Multiple hero item settings found");
		
	    // Build session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSessionName ("AI Game");
		sd.setXmlDatabaseName (db.getDbName ());
		sd.setAiPlayerCount ((aiPlayerCount <= 2) ? 14 : aiPlayerCount);
		sd.setMaxPlayers (sd.getAiPlayerCount () + 2);		// Raiders and Rampaging Monsters
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		
		sd.setOverlandMapSize (overlandMapSizes.get (0));
		sd.setCombatMapSize (getCombatMapUtils ().createCombatMapSize ());
		sd.setLandProportion (landProportions.get (0));
		sd.setNodeStrength (nodeStrengths.get (0));
		sd.setDifficultyLevel (difficultyLevels.get (0));
		sd.setFogOfWarSetting (fogOfWarSettings.get (0));
		sd.setUnitSetting (unitSettings.get (0));
		sd.setSpellSetting (spellSettings.get (0));
		sd.setHeroItemSetting (heroItemSettings.get (0));
		
		// Difficulty level is annoying, as the difficulty level node strength list contains 6, for both planes and all 3 node strengths
		// Can't just remove the ones we don't want as we'd be modifying the object that's in the new game database, so have to copy it field by field
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setDifficultyLevelID (useDifficultyLevelID);
	    difficultyLevel.setHumanSpellPicks (difficultyLevels.get (0).getHumanSpellPicks ());
	    difficultyLevel.setAiSpellPicks (difficultyLevels.get (0).getAiSpellPicks ());
	    difficultyLevel.setHumanStartingGold (difficultyLevels.get (0).getHumanStartingGold ());
	    difficultyLevel.setAiStartingGold (difficultyLevels.get (0).getAiStartingGold ());
	    difficultyLevel.setCustomWizards (difficultyLevels.get (0).isCustomWizards ());
	    difficultyLevel.setAiWizardsPopulationGrowthRateMultiplier (difficultyLevels.get (0).getAiWizardsPopulationGrowthRateMultiplier ());
	    difficultyLevel.setAiRaidersPopulationGrowthRateMultiplier (difficultyLevels.get (0).getAiRaidersPopulationGrowthRateMultiplier ());
	    difficultyLevel.setAiWizardsProductionRateMultiplier (difficultyLevels.get (0).getAiWizardsProductionRateMultiplier ());
	    difficultyLevel.setAiRaidersProductionRateMultiplier (difficultyLevels.get (0).getAiRaidersProductionRateMultiplier ());
	    difficultyLevel.setAiSpellResearchMultiplier (difficultyLevels.get (0).getAiSpellResearchMultiplier ());
	    difficultyLevel.setAiUpkeepMultiplier (difficultyLevels.get (0).getAiUpkeepMultiplier ());
	    difficultyLevel.setEventMinimumTurnNumber (difficultyLevels.get (0).getEventMinimumTurnNumber ());
	    difficultyLevel.setMinimumTurnsBetweenEvents (difficultyLevels.get (0).getMinimumTurnsBetweenEvents ());
	    difficultyLevel.setEventChance (difficultyLevels.get (0).getEventChance ());
	    difficultyLevel.setRampagingMonstersMinimumTurnNumber (difficultyLevels.get (0).getRampagingMonstersMinimumTurnNumber ());
	    difficultyLevel.setRampagingMonstersAccumulatorMaximum (difficultyLevels.get (0).getRampagingMonstersAccumulatorMaximum ());
	    difficultyLevel.setRampagingMonstersAccumulatorThreshold (difficultyLevels.get (0).getRampagingMonstersAccumulatorThreshold ());
	    difficultyLevel.setFameRazingPenalty (difficultyLevels.get (0).isFameRazingPenalty ());
	    difficultyLevel.setTowerMonstersMinimum (difficultyLevels.get (0).getTowerMonstersMinimum ());
	    difficultyLevel.setTowerMonstersMaximum (difficultyLevels.get (0).getTowerMonstersMaximum ());
	    difficultyLevel.setTowerTreasureMinimum (difficultyLevels.get (0).getTowerTreasureMinimum ());
	    difficultyLevel.setTowerTreasureMaximum (difficultyLevels.get (0).getTowerTreasureMaximum ());
	    difficultyLevel.setRaiderCityStartSizeMin (difficultyLevels.get (0).getRaiderCityStartSizeMin ());
	    difficultyLevel.setRaiderCityStartSizeMax (difficultyLevels.get (0).getRaiderCityStartSizeMax ());
	    difficultyLevel.setRaiderCityGrowthCap (difficultyLevels.get (0).getRaiderCityGrowthCap ());
	    difficultyLevel.setWizardCityStartSize (difficultyLevels.get (0).getWizardCityStartSize ());
	    difficultyLevel.setCityMaxSize (difficultyLevels.get (0).getCityMaxSize ());
		
		difficultyLevel.getDifficultyLevelDescription ().addAll (difficultyLevels.get (0).getDifficultyLevelDescription ());
		difficultyLevel.getDifficultyLevelPlane ().addAll (difficultyLevels.get (0).getDifficultyLevelPlane ());
		difficultyLevel.getDifficultyLevelNodeStrength ().addAll (difficultyLevels.get (0).getDifficultyLevelNodeStrength ().stream ().filter
			(ns -> ns.getNodeStrengthID ().equals (useNodeStrengthID)).collect (Collectors.toList ()));
		
		if (difficultyLevel.getDifficultyLevelNodeStrength ().size () != 2)
			throw new IOException ("Found wrong number of difficulty level node strengths");
		
		sd.setDifficultyLevel (difficultyLevel);
		
		// Pieces added by NewSessionImpl
		synchronized (getSessions ())
		{
			final GregorianCalendar now = new GregorianCalendar ();
			now.setTime (new Date ());
	
			sd.setSessionID (getNextAvailableSessionID () );
			sd.setStartedAt (getDatatypeFactory ().newXMLGregorianCalendar (now));
			setNextAvailableSessionID (getNextAvailableSessionID () + 1);
	    
			// Create the sesson
			final MomSessionThread sessionThread = (MomSessionThread) createSessionThread (sd);
			sessionThread.setMultiplayerSessionServer (this);
			getSessions ().add (sessionThread);
			sessionThread.initializeNewGame ();
			sessionThread.sessionAdded ();
			
			// Setting loaded = true is a sneaky way to get code to run instead of the thread going into its normal loop of waiting for messages from players
			// This causes initializeLoadedGame () to run instead, which looks for the aiGame flag being set
			sessionThread.setLoaded (true);
			sessionThread.setAiGame (true);
			sessionThread.start ();
		}
	}

	/**
	 * Descendant server classes will want to override this to create a thread that knows how to process useful messages
	 * @param sessionDescription Description of the new session
	 * @return Thread object to handle requests for this session
	 */
	@Override
	public final MultiplayerSessionThread createSessionThread (final SessionDescription sessionDescription)
	{
		final MomSessionThread thread = getSessionThreadFactory ().createThread ();
		thread.setSessionDescription (sessionDescription);

		return thread;
	}

	/**
	 * @return Message to send new game database to clients as they connect
	 */
	public final NewGameDatabaseMessage getNewGameDatabaseMessage ()
	{
		return newGameDatabaseMessage;
	}

	/**
	 * @param msg Message to send new game database to clients as they connect
	 */
	public final void setNewGameDatabaseMessage (final NewGameDatabaseMessage msg)
	{
		newGameDatabaseMessage = msg;
	}
	
	/** 
	 * @return Factory interface for creating MomSessionThreads
	 */
	public final MomSessionThreadFactory getSessionThreadFactory ()
	{
		return sessionThreadFactory;
	}

	/**
	 * @param factory Factory interface for creating MomSessionThreads
	 */
	public final void setSessionThreadFactory (final MomSessionThreadFactory factory)
	{
		sessionThreadFactory = factory;
	}

	/**
	 * @return Combat map utils
	 */
	public final CombatMapUtils getCombatMapUtils ()
	{
		return combatMapUtils;
	}

	/**
	 * @param util Combat map utils
	 */
	public final void setCombatMapUtils (final CombatMapUtils util)
	{
		combatMapUtils = util;
	}
}