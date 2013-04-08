package momime.server;

import java.io.File;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.FogOfWarStateID;
import momime.common.messages.v0_9_4.MapAreaOfFogOfWarStates;
import momime.common.messages.v0_9_4.MapAreaOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapAreaOfStrings;
import momime.common.messages.v0_9_4.MapRowOfFogOfWarStates;
import momime.common.messages.v0_9_4.MapRowOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapRowOfStrings;
import momime.common.messages.v0_9_4.MapVolumeOfFogOfWarStates;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapVolumeOfStrings;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MomGeneralPublicKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomTransientPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.server.calculations.IMomServerResourceCalculations;
import momime.server.config.v0_9_4.MomImeServerConfig;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseConverters;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseFactory;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.Spell;
import momime.server.fogofwar.IFogOfWarMidTurnChanges;
import momime.server.mapgenerator.OverlandMapGenerator;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;
import momime.server.process.ICityProcessing;
import momime.server.process.IPlayerMessageProcessing;
import momime.server.process.ISpellProcessing;
import momime.server.ui.MomServerUI;
import momime.server.ui.SessionWindow;

import com.ndg.multiplayer.server.IMultiplayerServerMessageProcesser;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.sessionbase.GeneralPublicKnowledge;
import com.ndg.multiplayer.sessionbase.GeneralServerKnowledge;
import com.ndg.multiplayer.sessionbase.PersistentPlayerPrivateKnowledge;
import com.ndg.multiplayer.sessionbase.PersistentPlayerPublicKnowledge;
import com.ndg.multiplayer.sessionbase.SessionDescription;
import com.ndg.multiplayer.sessionbase.TransientPlayerPrivateKnowledge;
import com.ndg.multiplayer.sessionbase.TransientPlayerPublicKnowledge;

/**
 * Thread that handles everything going on in one MoM session
 */
final class MomSessionThread extends MultiplayerSessionThread implements IMomSessionVariables
{
	/** Lookup lists built over the XML database */
	private ServerDatabaseEx db;

	/** UI being used by server */
	private MomServerUI ui;

	/** Logger for logging key messages relating to this session */
	private Logger sessionLogger;

	// These are all here so that message implementations and resource consumers can access them
	
	/** Methods for updating true map + players' memory */
	private IFogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Resource calculations */
	private IMomServerResourceCalculations serverResourceCalculations;
	
	/** Methods for dealing with player msgs */
	private IPlayerMessageProcessing playerMessageProcessing;

	/** Spell processing methods */
	private ISpellProcessing spellProcessing;
	
	/** City processing methods */
	private ICityProcessing cityProcessing;
	
	/**
	 * @param aServer Server this connection belongs to
	 * @param aSessionDescription Details of this session
	 * @param config Server config loaded from XML config file
	 * @param aUI UI being used by server
	 * @param fileLogger Logger which writes to a disk file, if enabled
	 * @param aDebugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem loading the server XML file
	 */
	public MomSessionThread (final IMultiplayerServerMessageProcesser aServer, final SessionDescription aSessionDescription,
		final MomImeServerConfig config, final MomServerUI aUI, final Logger fileLogger, final Logger aDebugLogger)
		throws JAXBException
	{
		super (aServer, aSessionDescription, new MomSessionConstructorParam (config, aUI, fileLogger), aDebugLogger);
	}

	/**
	 * @return Logger for logging key messages relating to this session
	 */
	@Override
	public final Logger getSessionLogger ()
	{
		return sessionLogger;
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
	 * @return Load XML file specified in session description
	 * @throws JAXBException If there is a problem loading the server XML file
	 */
	@Override
	protected final GeneralServerKnowledge createGeneralServerKnowledge () throws JAXBException
	{
		debugLogger.entering (MomSessionThread.class.getName (), "createGeneralServerKnowledge", getSessionDescription ().getXmlDatabaseName ());

		// Get access to the server config and UI
		final MomSessionConstructorParam param = (MomSessionConstructorParam) getConstructorParam ();
		ui = param.getUI ();

		// Start up UI for this session
		final SessionWindow sessionWindow = ui.createWindowForNewSession (getSessionDescription ());
		sessionLogger = ui.createLoggerForNewSession (getSessionDescription (), sessionWindow, debugLogger, param.getFileLogger ());

		// Load server XML
		sessionLogger.info ("Loading server database \"" + getSessionDescription ().getXmlDatabaseName () + "\"...");
		final File fullFilename = new File (param.getConfig ().getPathToServerXmlDatabases () + getSessionDescription ().getXmlDatabaseName () + ServerDatabaseConverters.SERVER_XML_FILE_EXTENSION);
		
		final Unmarshaller unmarshaller = JAXBContextCreator.createServerDatabaseContext ().createUnmarshaller ();		
		unmarshaller.setProperty ("com.sun.xml.bind.ObjectFactory", new Object [] {new ServerDatabaseFactory ()});
		db = (ServerDatabaseEx) unmarshaller.unmarshal (fullFilename);

		// Create hash maps to look up all the values from the DB
		sessionLogger.info ("Caching lookups into server database...");
		db.buildMaps ();

		// Generate map
		sessionLogger.info ("Generating overland map...");
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		try
		{
			final OverlandMapGenerator mapGen = new OverlandMapGenerator (trueMap, getSessionDescription (), db, debugLogger);
			mapGen.generateOverlandTerrain ();
		}
		catch (final MomException e)
		{
			e.printStackTrace ();
			throw new JAXBException (e.getMessage ());
		}
		catch (final RecordNotFoundException e)
		{
			e.printStackTrace ();
			throw new JAXBException (e.getMessage ());
		}

		// Put into server knowledge structure
		sessionLogger.info ("Creating server side knowledge structure...");
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setServerDatabase (db);
		gsk.setTrueMap (trueMap);
		gsk.setNextFreeUnitURN (1);

		debugLogger.exiting (MomSessionThread.class.getName (), "createGeneralServerKnowledge", gsk);
		return gsk;
	}

	/**
	 * @return Server general knowledge, typecasted to MoM specific type
	 */
	@Override
	public final MomGeneralServerKnowledge getGeneralServerKnowledge ()
	{
		return (MomGeneralServerKnowledge) super.getGeneralServerKnowledge ();
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
	 * @return Derive client XML file from server XML file
	 * @throws JAXBException If one of the wizards does not have picks for the specified number of human picks defined
	 */
	@Override
	protected final GeneralPublicKnowledge createGeneralPublicKnowledge () throws JAXBException
	{
		sessionLogger.info ("Generating client database...");

		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();

		try
		{
			gpk.setClientDatabase (ServerDatabaseConverters.buildClientDatabase (getServerDB (), getSessionDescription ().getDifficultyLevel ().getHumanSpellPicks (), debugLogger));
		}
		catch (final RecordNotFoundException e)
		{
			// This is a bit of a fudge since the exception has nothing to do with JAXB, but RecordNotFoundException is MoM-specific and so the multiplayer layer can't handle it
			throw new JAXBException (e.getMessage ());
		}

		return gpk;
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
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();

		// Initialize all spell research statuses
		for (final Spell spell : getServerDB ().getSpell ())
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
		priv.setTaxRateID (ServerDatabaseValues.VALUE_TAX_RATE_DEFAULT);

		// Create and initialize fog of war area
		final MapVolumeOfFogOfWarStates fogOfWar = new MapVolumeOfFogOfWarStates ();
		for (int plane = 0; plane < db.getPlane ().size (); plane++)
		{
			final MapAreaOfFogOfWarStates fogOfWarPlane = new MapAreaOfFogOfWarStates ();
			for (int y = 0; y < getSessionDescription ().getMapSize ().getHeight (); y++)
			{
				final MapRowOfFogOfWarStates fogOfWarRow = new MapRowOfFogOfWarStates ();
				for (int x = 0; x < getSessionDescription ().getMapSize ().getWidth (); x++)
					fogOfWarRow.getCell ().add (FogOfWarStateID.NEVER_SEEN);

				fogOfWarPlane.getRow ().add (fogOfWarRow);
			}

			fogOfWar.getPlane ().add (fogOfWarPlane);
		}

		priv.setFogOfWar (fogOfWar);

		// Create and initialize scouted unit IDs in all the nodes/lairs/towers
		final MapVolumeOfStrings nodeLairTowerKnownUnitIDs = new MapVolumeOfStrings ();
		for (int plane = 0; plane < db.getPlane ().size (); plane++)
		{
			final MapAreaOfStrings nodeLairTowerKnownUnitIDsPlane = new MapAreaOfStrings ();
			for (int y = 0; y < getSessionDescription ().getMapSize ().getHeight (); y++)
			{
				final MapRowOfStrings nodeLairTowerKnownUnitIDsRow = new MapRowOfStrings ();
				for (int x = 0; x < getSessionDescription ().getMapSize ().getWidth (); x++)
					nodeLairTowerKnownUnitIDsRow.getCell ().add (null);	// Unknown

				nodeLairTowerKnownUnitIDsPlane.getRow ().add (nodeLairTowerKnownUnitIDsRow);
			}

			nodeLairTowerKnownUnitIDs.getPlane ().add (nodeLairTowerKnownUnitIDsPlane);
		}

		priv.setNodeLairTowerKnownUnitIDs (nodeLairTowerKnownUnitIDs);

		// Create and initialize fog of war memory
		// Note we create a MemoryGridCell at every location even if we've never seen that location,
		// but the terrain and city data elements will remain null until we actually see it.
		// This is just because it would make the code overly complex to have null checks everywhere this gets accessed.
		final MapVolumeOfMemoryGridCells fogOfWarMap = new MapVolumeOfMemoryGridCells ();
		for (int plane = 0; plane < db.getPlane ().size (); plane++)
		{
			final MapAreaOfMemoryGridCells fogOfWarPlane = new MapAreaOfMemoryGridCells ();
			for (int y = 0; y < getSessionDescription ().getMapSize ().getHeight (); y++)
			{
				final MapRowOfMemoryGridCells fogOfWarRow = new MapRowOfMemoryGridCells ();
				for (int x = 0; x < getSessionDescription ().getMapSize ().getWidth (); x++)
					fogOfWarRow.getCell ().add (new MemoryGridCell ());

				fogOfWarPlane.getRow ().add (fogOfWarRow);
			}

			fogOfWarMap.getPlane ().add (fogOfWarPlane);
		}

		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();
		fogOfWarMemory.setMap (fogOfWarMap);
		priv.setFogOfWarMemory (fogOfWarMemory);

		return priv;
	}

	/**
	 * @return Descendant of TransientPlayerPublicKnowledge, or can be left as null if not required
	 */
	@Override
	protected final TransientPlayerPublicKnowledge createTransientPlayerPublicKnowledge ()
	{
		return new MomTransientPlayerPublicKnowledge ();
	}

	/**
	 * @return Descendant of TransientPlayerPrivateKnowledge, or can be left as null if not required
	 */
	@Override
	protected final TransientPlayerPrivateKnowledge createTransientPlayerPrivateKnowledge ()
	{
		return new MomTransientPlayerPrivateKnowledge ();
	}

	/**
	 * @return Methods for updating true map + players' memory
	 */
	@Override
	public final IFogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final IFogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}

	/**
	 * @return Resource calculations
	 */
	@Override
	public final IMomServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final IMomServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}

	/**
	 * @return Methods for dealing with player msgs
	 */
	@Override
	public final IPlayerMessageProcessing getPlayerMessageProcessing ()
	{
		return playerMessageProcessing;
	}

	/**
	 * @param obj Methods for dealing with player msgs
	 */
	public final void setPlayerMessageProcessing (final IPlayerMessageProcessing obj)
	{
		playerMessageProcessing = obj;
	}

	/**
	 * @return Spell processing methods
	 */
	@Override
	public final ISpellProcessing getSpellProcessing ()
	{
		return spellProcessing;
	}

	/**
	 * @param obj Spell processing methods
	 */
	public final void setSpellProcessing (final ISpellProcessing obj)
	{
		spellProcessing = obj;
	}

	/**
	 * @return City processing methods
	 */
	@Override
	public final ICityProcessing getCityProcessing ()
	{
		return cityProcessing;
	}

	/**
	 * @param obj City processing methods
	 */
	public final void setCityProcessing (final ICityProcessing obj)
	{
		cityProcessing = obj;
	}
}
