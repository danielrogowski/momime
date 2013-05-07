package momime.server;

import java.util.logging.Logger;

import momime.common.messages.IPlayerPickUtils;
import momime.common.messages.IResourceValueUtils;
import momime.common.messages.ISpellUtils;
import momime.common.messages.IUnitUtils;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.FogOfWarStateID;
import momime.common.messages.v0_9_4.MagicPowerDistribution;
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
import momime.server.calculations.IMomServerUnitCalculations;
import momime.server.database.IServerDatabaseConverters;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.Spell;
import momime.server.fogofwar.IFogOfWarMidTurnChanges;
import momime.server.mapgenerator.IOverlandMapGenerator;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;
import momime.server.process.ICityProcessing;
import momime.server.process.IPlayerMessageProcessing;
import momime.server.process.ISpellProcessing;
import momime.server.ui.MomServerUI;
import momime.server.utils.ICityServerUtils;
import momime.server.utils.IPlayerPickServerUtils;
import momime.server.utils.ISpellServerUtils;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.sessionbase.PersistentPlayerPrivateKnowledge;
import com.ndg.multiplayer.sessionbase.PersistentPlayerPublicKnowledge;
import com.ndg.multiplayer.sessionbase.TransientPlayerPrivateKnowledge;
import com.ndg.multiplayer.sessionbase.TransientPlayerPublicKnowledge;

/**
 * Thread that handles everything going on in one MoM session
 */
public final class MomSessionThread extends MultiplayerSessionThread implements IMomSessionVariables
{
	/** Class logger */
	private final Logger log = Logger.getLogger (MomSessionThread.class.getName ());
	
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
	
	/** Database converters */
	private IServerDatabaseConverters serverDatabaseConverters;

	/** Resource value utils */
	private IResourceValueUtils resourceValueUtils;

	/** Spell utils */
	private ISpellUtils spellUtils;

	/** Unit utils */
	private IUnitUtils unitUtils;
	
	/** Player pick utils */
	private IPlayerPickUtils playerPickUtils;

	/** Server-only pick utils */
	private IPlayerPickServerUtils playerPickServerUtils;
	
	/** Server-only city utils */
	private ICityServerUtils cityServerUtils;
	
	/** Server-only spell utils */
	private ISpellServerUtils spellServerUtils;
	
	/** Server-only unit calculations */
	private IMomServerUnitCalculations serverUnitCalculations;
	
	/** Overland map generator for this session */
	private IOverlandMapGenerator overlandMapGenerator;	
	
	/**
	 * @return Logger for logging key messages relating to this session
	 */
	@Override
	public final Logger getSessionLogger ()
	{
		return sessionLogger;
	}

	/**
	 * @param logger Logger for logging key messages relating to this session
	 */
	public final void setSessionLogger (final Logger logger)
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
	 * @return Load XML file specified in session description
	 * @throws JAXBException If there is a problem loading the server XML file
	 */ 
/*	protected final GeneralServerKnowledge createGeneralServerKnowledge (final MomServerUI ui,
		final MomImeServerConfig config, final Logger fileLogger) throws JAXBException
	{
		log.entering (MomSessionThread.class.getName (), "createGeneralServerKnowledge", getSessionDescription ().getXmlDatabaseName ());

		// Start up UI for this session
		final SessionWindow sessionWindow = ui.createWindowForNewSession (getSessionDescription ());
		sessionLogger = ui.createLoggerForNewSession (getSessionDescription (), sessionWindow, fileLogger);

		// Generate map
		sessionLogger.info ("Generating overland map...");
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		try
		{
			final OverlandMapGenerator mapGen = new OverlandMapGenerator (trueMap, getSessionDescription (), db);
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

		log.exiting (MomSessionThread.class.getName (), "createGeneralServerKnowledge", gsk);
		return gsk;
	} */

	/**
	 * @return Server general knowledge, typecasted to MoM specific type
	 */
	@Override
	public final MomGeneralServerKnowledge getGeneralServerKnowledge ()
	{
		return (MomGeneralServerKnowledge) super.getGeneralServerKnowledge ();
	}

	/**
	 * Spring seems to need this to be able to set the property - odd, since the MP demo works without this
	 * @param gsk Server knowledge structure, typecasted to MoM specific type
	 */
	public final void setGeneralServerKnowledge (final MomGeneralServerKnowledge gsk)
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
		
		// Set default power distribution
		final MagicPowerDistribution dist = new MagicPowerDistribution ();
		dist.setManaRatio (80);
		dist.setResearchRatio (80);
		dist.setSkillRatio (80);
		priv.setMagicPowerDistribution (dist);

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

	/**
	 * @return Database converters
	 */
	public final IServerDatabaseConverters getServerDatabaseConverters ()
	{
		return serverDatabaseConverters;
	}

	/**
	 * @param conv Database converters
	 */
	public final void setServerDatabaseConverters (final IServerDatabaseConverters conv)
	{
		serverDatabaseConverters = conv;
	}

	/**
	 * @return Resource value utils
	 */
	@Override
	public final IResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final IResourceValueUtils utils)
	{
		resourceValueUtils = utils;
	}

	/**
	 * @return Spell utils
	 */
	@Override
	public final ISpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final ISpellUtils utils)
	{
		spellUtils = utils;
	}

	/**
	 * @return Unit utils
	 */
	@Override
	public final IUnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final IUnitUtils utils)
	{
		unitUtils = utils;
	}

	/**
	 * @return Player pick utils
	 */
	@Override
	public final IPlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final IPlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}

	/**
	 * @return Server-only pick utils
	 */
	@Override
	public final IPlayerPickServerUtils getPlayerPickServerUtils ()
	{
		return playerPickServerUtils;
	}

	/**
	 * @param utils Server-only pick utils
	 */
	public final void setPlayerPickServerUtils (final IPlayerPickServerUtils utils)
	{
		playerPickServerUtils = utils;
	}

	/**
	 * @return Server-only city utils
	 */
	@Override
	public final ICityServerUtils getCityServerUtils ()
	{
		return cityServerUtils;
	}

	/**
	 * @param utils Server-only city utils
	 */
	public final void setCityServerUtils (final ICityServerUtils utils)
	{
		cityServerUtils = utils;
	}

	/**
	 * @return Server-only spell utils
	 */
	@Override
	public final ISpellServerUtils getSpellServerUtils ()
	{
		return spellServerUtils;
	}

	/**
	 * @param utils Server-only spell utils
	 */
	public final void setSpellServerUtils (final ISpellServerUtils utils)
	{
		spellServerUtils = utils;
	}

	/**
	 * @return Server-only unit calculations
	 */
	@Override
	public final IMomServerUnitCalculations getServerUnitCalculations ()
	{
		return serverUnitCalculations;
	}

	/**
	 * @param calc Server-only unit calculations
	 */
	public final void setServerUnitCalculations (final IMomServerUnitCalculations calc)
	{
		serverUnitCalculations = calc;
	}

	/**
	 * @return Overland map generator for this session
	 */
	@Override
	public final IOverlandMapGenerator getOverlandMapGenerator ()
	{
		return overlandMapGenerator;
	}

	/**
	 * @param mapGen Overland map generator for this session
	 */
	public final void setOverlandMapGenerator (final IOverlandMapGenerator mapGen)
	{
		overlandMapGenerator = mapGen;
	}
}
