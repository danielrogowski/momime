package momime.server;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.JoinSuccessfulReason;
import com.ndg.multiplayer.sessionbase.PersistentPlayerPrivateKnowledge;
import com.ndg.multiplayer.sessionbase.PersistentPlayerPublicKnowledge;
import com.ndg.multiplayer.sessionbase.TransientPlayerPrivateKnowledge;
import com.ndg.multiplayer.sessionbase.TransientPlayerPublicKnowledge;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItem;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.MagicPowerDistribution;
import momime.common.messages.MapAreaOfFogOfWarStates;
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapRowOfFogOfWarStates;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.server.database.ServerDatabaseConverters;
import momime.server.database.ServerDatabaseConvertersImpl;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseExImpl;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.SpellSvr;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;
import momime.server.mapgenerator.OverlandMapGenerator;
import momime.server.mapgenerator.OverlandMapGeneratorImpl;
import momime.server.process.PlayerMessageProcessing;
import momime.server.ui.MomServerUI;
import momime.server.ui.MomServerUIHolder;
import momime.server.utils.HeroItemServerUtils;

/**
 * Thread that handles everything going on in one MoM session
 */
public final class MomSessionThread extends MultiplayerSessionThread implements MomSessionVariables
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MomSessionThread.class);

	/** Prefix for all session loggers */
	public static final String MOM_SESSION_LOGGER_PREFIX = "MoMIMESession.";
	
	/** Lookup lists built over the XML database */
	private ServerDatabaseEx db;

	/** Logger for logging key messages relating to this session */
	private Log sessionLogger;

	/** Overland map generator for this session */
	private OverlandMapGenerator overlandMapGenerator;	
	
	/** Database converters */
	private ServerDatabaseConverters serverDatabaseConverters;
	
	/** Path to where all the server database XMLs are - from config file */
	private String pathToServerXmlDatabases;
	
	/** JAXB unmarshaller for reading server databases */
	private Unmarshaller serverDatabaseUnmarshaller;
	
	/** Methods for dealing with player msgs */
	private PlayerMessageProcessing playerMessageProcessing;
	
	/** Methods dealing with hero items */
	private HeroItemServerUtils heroItemServerUtils;
	
	/**
	 * Descendant server classes will want to override this to create a thread that knows how to process useful messages
	 * 
	 * @throws JAXBException If there is an error dealing with any XML files during creation
	 * @throws XMLStreamException If there is an error dealing with any XML files during creation
	 * @throws IOException If there is a problem generating the client database for this session
	 */
	@Override
	public final void initializeNewGame () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering initializeNewGame: Session ID " + getSessionDescription ().getSessionID ());

		// Start logger for this sesssion.  These are much the same as the class loggers, except named MoMIMESession.1, MoMIMESession.2 and so on.
		if (getUI () != null)
			getUI ().createWindowForNewSession (getSessionDescription ());
		
		setSessionLogger (LogFactory.getLog (MOM_SESSION_LOGGER_PREFIX + getSessionDescription ().getSessionID ()));

		// Load server XML
		getSessionLogger ().info ("Loading server XML...");
		final File fullFilename = new File (getPathToServerXmlDatabases () + "/" + getSessionDescription ().getXmlDatabaseName () +
			ServerDatabaseConvertersImpl.SERVER_XML_FILE_EXTENSION);
		final ServerDatabaseExImpl sdb = (ServerDatabaseExImpl) getServerDatabaseUnmarshaller ().unmarshal (fullFilename); 

		// Create hash maps to look up all the values from the DB
		getSessionLogger ().info ("Building maps and running checks over XML data...");
		sdb.buildMaps ();
		sdb.consistencyChecks ();
		setServerDB (sdb); 
		
		// Create client database
		getSessionLogger ().info ("Generating client XML...");
		getGeneralPublicKnowledge ().setClientDatabase (getServerDatabaseConverters ().buildClientDatabase
			(getServerDB (), getSessionDescription ().getDifficultyLevel ().getHumanSpellPicks ()));
		
		// Generate the overland map
		getSessionLogger ().info ("Generating overland map...");
		final OverlandMapGeneratorImpl mapGen = (OverlandMapGeneratorImpl) getOverlandMapGenerator ();
		mapGen.setSessionDescription (getSessionDescription ());
		mapGen.setServerDB (getServerDB ());
		mapGen.setGsk (getGeneralServerKnowledge ());		// See comment in spring XML for why this isn't just injected
		mapGen.generateOverlandTerrain ();
		mapGen.generateInitialCombatAreaEffects ();
		
		// Make all predefined hero items available - need to allocate a number for each
		for (final HeroItem item : getServerDB ().getHeroItem ())
			getGeneralServerKnowledge ().getAvailableHeroItem ().add (getHeroItemServerUtils ().createNumberedHeroItem (item, getGeneralServerKnowledge ()));

		getSessionLogger ().info ("Session startup completed");
		log.trace ("Exiting initializeNewGame");
	}
	
	/**
	 * Must load server XML and regenerate client XML before adding players
	 *    
	 * @throws JAXBException If there is an error dealing with any XML files during creation
	 * @throws XMLStreamException If there is an error dealing with any XML files during creation
	 * @throws IOException Can be used for non-JAXB related errors
	 */
	@Override
	public final void preInitializeLoadedGame () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering preInitializeLoadedGame");

		// Start logger for this sesssion.  These are much the same as the class loggers, except named MoMIMESession.1, MoMIMESession.2 and so on.
		if (getUI () != null)
			getUI ().createWindowForNewSession (getSessionDescription ());
		
		setSessionLogger (LogFactory.getLog (MOM_SESSION_LOGGER_PREFIX + getSessionDescription ().getSessionID ()));

		// Load server XML
		getSessionLogger ().info ("Loading server XML...");
		final File fullFilename = new File (getPathToServerXmlDatabases () + "/" + getSessionDescription ().getXmlDatabaseName () +
			ServerDatabaseConvertersImpl.SERVER_XML_FILE_EXTENSION);
		final ServerDatabaseExImpl sdb = (ServerDatabaseExImpl) getServerDatabaseUnmarshaller ().unmarshal (fullFilename); 
		
		// Create hash maps to look up all the values from the DB
		getSessionLogger ().info ("Building maps and running checks over XML data...");
		sdb.buildMaps ();
		sdb.consistencyChecks ();
		setServerDB (sdb); 

		// Create client database
		getSessionLogger ().info ("Generating client XML...");
		getGeneralPublicKnowledge ().setClientDatabase (getServerDatabaseConverters ().buildClientDatabase
			(getServerDB (), getSessionDescription ().getDifficultyLevel ().getHumanSpellPicks ()));
		
		log.trace ("Exiting preInitializeLoadedGame");
	}
	
	/**
	 * Kick off the first turn after loading a game.  
	 * 
	 * @throws JAXBException If there is an error dealing with any XML files during creation
	 * @throws XMLStreamException If there is an error dealing with any XML files during creation
	 * @throws IOException Can be used for non-JAXB related errors
	 */
	@Override
	public final void initializeLoadedGame () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering initializeLoadedGame");

		// If its a single player game, then start it immediately
		getPlayerMessageProcessing ().checkIfCanStartLoadedGame (this);
		
		log.trace ("Exiting initializeLoadedGame");
	}
	
	/**
	 * Called after players are added to the session for any reason (when setting up a new game, joining, rejoining or loading)
	 *
	 * @param player The player who just joined the session
	 * @param reason The type of session the player joined into; ignored and can be null if connection is null OR sendMessages is false
	 * @throws JAXBException If there is a problem converting the reply into XML
	 * @throws XMLStreamException If there is a problem writing the reply to the XML stream
	 * @throws IOException If there is a problem sending any reply back to the client
	 */
	@Override
	public final void playerAdded (final PlayerServerDetails player, final JoinSuccessfulReason reason)
		throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering playerAdded: Player ID " + player.getPlayerDescription ().getPlayerID () + ", " + reason);
		
		// Only do this for *additional* players joining saved games - if we've loading a single player saved game, the player is added too early so these are handled above
		if (reason == JoinSuccessfulReason.REJOINED_SESSION)
			getPlayerMessageProcessing ().checkIfCanStartLoadedGame (this);
		
		log.trace ("Exiting playerAdded");
	}
	
	/**
	 * @return UI being used by server
	 */
	public final MomServerUI getUI ()
	{
		return MomServerUIHolder.getUI ();
	}		

	/**
	 * @return Logger for logging key messages relating to this session
	 */
	@Override
	public final Log getSessionLogger ()
	{
		return sessionLogger;
	}

	/**
	 * @param logger Logger for logging key messages relating to this session
	 */
	public final void setSessionLogger (final Log logger)
	{
		sessionLogger = logger;
	}
	
	/**
	 * @return Session description, typecasted to MoM specific type
	 */
	@Override
	public final MomSessionDescription getSessionDescription ()
	{
		return (MomSessionDescription) super.getSessionDescription ();
	}

	/**
	 * @return Server general knowledge, typecasted to MoM specific type
	 */
	@Override
	public final MomGeneralServerKnowledgeEx getGeneralServerKnowledge ()
	{
		return (MomGeneralServerKnowledgeEx) super.getGeneralServerKnowledge ();
	}

	/**
	 * Spring seems to need this to be able to set the property - odd, since the MP demo works without this
	 * @param gsk Server knowledge structure, typecasted to MoM specific type
	 */
	public final void setGeneralServerKnowledge (final MomGeneralServerKnowledgeEx gsk)
	{
		super.setGeneralServerKnowledge (gsk);
	}

	/**
	 * @return Server XML in use for this session
	 */
	@Override
	public final ServerDatabaseEx getServerDB ()
	{
		return db;
	}

	/**
	 * @param ex Server XML in use for this session
	 */
	public final void setServerDB (final ServerDatabaseEx ex)
	{
		db = ex;
	}
	
	/**
	 * @return Public knowledge structure, typecasted to MoM specific type
	 */
	@Override
	public final MomGeneralPublicKnowledge getGeneralPublicKnowledge ()
	{
		return (MomGeneralPublicKnowledge) super.getGeneralPublicKnowledge ();
	}

	/**
	 * Spring seems to need this to be able to set the property - odd, since the MP demo works without this
	 * @param gpk Public knowledge structure, typecasted to MoM specific type
	 */
	public final void setGeneralPublicKnowledge (final MomGeneralPublicKnowledge gpk)
	{
		super.setGeneralPublicKnowledge (gpk);
	}
	
	/**
	 * @return Descendant of PersistentPlayerPublicKnowledge, or can be left as null if not required
	 */
	@Override
	protected final PersistentPlayerPublicKnowledge createPersistentPlayerPublicKnowledge ()
	{
		return new MomPersistentPlayerPublicKnowledge ();
	}

	/**
	 * @return Descendant of PersistentPlayerPrivateKnowledge, or can be left as null if not required
	 */
	@Override
	protected final PersistentPlayerPrivateKnowledge createPersistentPlayerPrivateKnowledge ()
	{
		log.trace ("Entering createPersistentPlayerPrivateKnowledge");
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();

		// Initialize all spell research statuses
		for (final SpellSvr spell : getServerDB ().getSpells ())
		{
			final SpellResearchStatus status = new SpellResearchStatus ();
			status.setSpellID (spell.getSpellID ());

			// Regular spells must be leared
			if (spell.getSpellRealm () != null)
			{
				status.setStatus (SpellResearchStatusID.UNAVAILABLE);
				status.setRemainingResearchCost (spell.getResearchCost ());
			}

			// Some arcane spells are free
			else if (spell.getResearchCost () == null)
				status.setStatus (SpellResearchStatusID.AVAILABLE);

			// But all arcane spells are at least researchable
			else
			{
				status.setStatus (SpellResearchStatusID.RESEARCHABLE);
				status.setRemainingResearchCost (spell.getResearchCost ());
			}

			priv.getSpellResearchStatus ().add (status);
		}

		// Set default tax rate
		priv.setTaxRateID (ServerDatabaseValues.TAX_RATE_DEFAULT);
		
		// Set default power distribution
		final MagicPowerDistribution dist = new MagicPowerDistribution ();
		dist.setManaRatio		(CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
		dist.setResearchRatio	(CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
		dist.setSkillRatio			(CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
		priv.setMagicPowerDistribution (dist);

		// Create and initialize fog of war area
		final MapVolumeOfFogOfWarStates fogOfWar = new MapVolumeOfFogOfWarStates ();
		for (int plane = 0; plane < db.getPlanes ().size (); plane++)
		{
			final MapAreaOfFogOfWarStates fogOfWarPlane = new MapAreaOfFogOfWarStates ();
			for (int y = 0; y < getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
			{
				final MapRowOfFogOfWarStates fogOfWarRow = new MapRowOfFogOfWarStates ();
				for (int x = 0; x < getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					fogOfWarRow.getCell ().add (FogOfWarStateID.NEVER_SEEN);

				fogOfWarPlane.getRow ().add (fogOfWarRow);
			}

			fogOfWar.getPlane ().add (fogOfWarPlane);
		}

		priv.setFogOfWar (fogOfWar);

		// Create and initialize fog of war memory
		// Note we create a MemoryGridCell at every location even if we've never seen that location,
		// but the terrain and city data elements will remain null until we actually see it.
		// This is just because it would make the code overly complex to have null checks everywhere this gets accessed.
		final MapVolumeOfMemoryGridCells fogOfWarMap = new MapVolumeOfMemoryGridCells ();
		for (int plane = 0; plane < db.getPlanes ().size (); plane++)
		{
			final MapAreaOfMemoryGridCells fogOfWarPlane = new MapAreaOfMemoryGridCells ();
			for (int y = 0; y < getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
			{
				final MapRowOfMemoryGridCells fogOfWarRow = new MapRowOfMemoryGridCells ();
				for (int x = 0; x < getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					fogOfWarRow.getCell ().add (new MemoryGridCell ());

				fogOfWarPlane.getRow ().add (fogOfWarRow);
			}

			fogOfWarMap.getPlane ().add (fogOfWarPlane);
		}

		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();
		fogOfWarMemory.setMap (fogOfWarMap);
		priv.setFogOfWarMemory (fogOfWarMemory);

		log.trace ("Exiting createPersistentPlayerPrivateKnowledge = " + priv);
		return priv;
	}

	/**
	 * @return Descendant of TransientPlayerPublicKnowledge, or can be left as null if not required
	 */
	@Override
	public final TransientPlayerPublicKnowledge createTransientPlayerPublicKnowledge ()
	{
		return new MomTransientPlayerPublicKnowledge ();
	}

	/**
	 * @return Descendant of TransientPlayerPrivateKnowledge, or can be left as null if not required
	 */
	@Override
	public final TransientPlayerPrivateKnowledge createTransientPlayerPrivateKnowledge ()
	{
		return new MomTransientPlayerPrivateKnowledge ();
	}

	/**
	 * Let the UI know when sessions are added
	 */
	@Override
	public final void sessionAdded ()
	{
		if (getUI () != null)
			getUI ().sessionAdded (this);
	}

	/**
	 * Let the UI know when sessions are removed
	 */
	@Override
	public final void sessionRemoved ()
	{
		if (getUI () != null)
			getUI ().sessionRemoved (this);
	}

	/**
	 * @return Overland map generator for this session
	 */
	@Override
	public final OverlandMapGenerator getOverlandMapGenerator ()
	{
		return overlandMapGenerator;
	}

	/**
	 * @param mapGen Overland map generator for this session
	 */
	public final void setOverlandMapGenerator (final OverlandMapGenerator mapGen)
	{
		overlandMapGenerator = mapGen;
	}

	/**
	 * @return Database converters
	 */
	public final ServerDatabaseConverters getServerDatabaseConverters ()
	{
		return serverDatabaseConverters;
	}

	/**
	 * @param conv Database converters
	 */
	public final void setServerDatabaseConverters (final ServerDatabaseConverters conv)
	{
		serverDatabaseConverters = conv;
	}
	
	/**
	 * @return Path to where all the server database XMLs are - from config file
	 */
	public final String getPathToServerXmlDatabases ()
	{
		return pathToServerXmlDatabases;
	}

	/**
	 * @param path Path to where all the server database XMLs are - from config file
	 */
	public final void setPathToServerXmlDatabases (final String path)
	{
		pathToServerXmlDatabases = path;
	}

	/**
	 * @return JAXB unmarshaller for reading server databases
	 */
	public final Unmarshaller getServerDatabaseUnmarshaller ()
	{
		return serverDatabaseUnmarshaller;
	}

	/**
	 * @param unmarshaller JAXB unmarshaller for reading server databases
	 */
	public final void setServerDatabaseUnmarshaller (final Unmarshaller unmarshaller)
	{
		serverDatabaseUnmarshaller = unmarshaller;
	}

	/**
	 * @return Methods for dealing with player msgs
	 */
	public PlayerMessageProcessing getPlayerMessageProcessing ()
	{
		return playerMessageProcessing;
	}

	/**
	 * @param obj Methods for dealing with player msgs
	 */
	public final void setPlayerMessageProcessing (final PlayerMessageProcessing obj)
	{
		playerMessageProcessing = obj;
	}

	/**
	 * @return Methods dealing with hero items
	 */
	public final HeroItemServerUtils getHeroItemServerUtils ()
	{
		return heroItemServerUtils;
	}

	/**
	 * @param util Methods dealing with hero items
	 */
	public final void setHeroItemServerUtils (final HeroItemServerUtils util)
	{
		heroItemServerUtils = util;
	}
}