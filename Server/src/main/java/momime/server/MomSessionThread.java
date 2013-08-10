package momime.server;

import java.util.logging.Logger;

import momime.common.calculations.MomCityCalculations;
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
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.UnitUtils;
import momime.server.calculations.MomServerResourceCalculations;
import momime.server.calculations.MomServerUnitCalculations;
import momime.server.database.ServerDatabaseConverters;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.Spell;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.mapgenerator.CombatMapGeneratorImpl;
import momime.server.mapgenerator.OverlandMapGenerator;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;
import momime.server.process.CityProcessing;
import momime.server.process.CombatProcessing;
import momime.server.process.PlayerMessageProcessing;
import momime.server.process.SpellProcessing;
import momime.server.ui.MomServerUI;
import momime.server.utils.CityServerUtils;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.utils.PlayerPickServerUtils;
import momime.server.utils.SpellServerUtils;
import momime.server.utils.UnitServerUtils;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemType;
import com.ndg.multiplayer.server.MultiplayerServerUtils;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.sessionbase.PersistentPlayerPrivateKnowledge;
import com.ndg.multiplayer.sessionbase.PersistentPlayerPublicKnowledge;
import com.ndg.multiplayer.sessionbase.TransientPlayerPrivateKnowledge;
import com.ndg.multiplayer.sessionbase.TransientPlayerPublicKnowledge;

/**
 * Thread that handles everything going on in one MoM session
 */
public final class MomSessionThread extends MultiplayerSessionThread implements MomSessionVariables
{
	/** Class logger */
	private final Logger log = Logger.getLogger (MomSessionThread.class.getName ());
	
	/** Combat map coordinate system, expect this to be merged into session desc once client is also in Java */
	private final CoordinateSystem combatMapCoordinateSystem;
	
	/** Lookup lists built over the XML database */
	private ServerDatabaseEx db;

	/** UI being used by server */
	private MomServerUI ui;

	/** Logger for logging key messages relating to this session */
	private Logger sessionLogger;

	// These are all here so that message implementations and resource consumers can access them

	/** Server-side multiplayer utils */
	private MultiplayerServerUtils multiplayerServerUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Resource calculations */
	private MomServerResourceCalculations serverResourceCalculations;
	
	/** Methods for dealing with player msgs */
	private PlayerMessageProcessing playerMessageProcessing;

	/** Spell processing methods */
	private SpellProcessing spellProcessing;
	
	/** City processing methods */
	private CityProcessing cityProcessing;

	/** Combat processing */
	private CombatProcessing combatProcessing;
	
	/** City calculations */
	private MomCityCalculations cityCalculations;
	
	/** Database converters */
	private ServerDatabaseConverters serverDatabaseConverters;

	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;

	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Maintained spell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;

	/** Builiding utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Pending movement utils */
	private PendingMovementUtils pendingMovementUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;

	/** Server-only pick utils */
	private PlayerPickServerUtils playerPickServerUtils;
	
	/** Server-only city utils */
	private CityServerUtils cityServerUtils;

	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Server-only spell utils */
	private SpellServerUtils spellServerUtils;
	
	/** Server-only unit calculations */
	private MomServerUnitCalculations serverUnitCalculations;
	
	/** Overland map generator for this session */
	private OverlandMapGenerator overlandMapGenerator;	
	
	/** Server-only overland map utils */
	private OverlandMapServerUtils overlandMapServerUtils;

	/**
	 * Create combat map coordinate system
	 */
	public MomSessionThread ()
	{
		super ();
		
		combatMapCoordinateSystem = new CoordinateSystem ();
		combatMapCoordinateSystem.setWidth (CombatMapGeneratorImpl.COMBAT_MAP_WIDTH);
		combatMapCoordinateSystem.setHeight (CombatMapGeneratorImpl.COMBAT_MAP_HEIGHT);
		combatMapCoordinateSystem.setCoordinateSystemType (CoordinateSystemType.DIAMOND);
	}
	
	/**
	 * @return UI being used by server
	 */
	public final MomServerUI getUI ()
	{
		return ui;
	}		

	/**
	 * @param newUI UI being used by server
	 */
	public final void setUI (final MomServerUI newUI)
	{
		ui = newUI;
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
	 * @return Combat map coordinate system, expect this to be merged into session desc once client is also in Java
	 */
	@Override
	public final CoordinateSystem getCombatMapCoordinateSystem ()
	{
		return combatMapCoordinateSystem;
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
		log.entering (MomSessionThread.class.getName (), "createPersistentPlayerPrivateKnowledge");
		
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

		log.exiting (MomSessionThread.class.getName (), "createPersistentPlayerPrivateKnowledge", priv);
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
	 * Let the UI know when sessions are added
	 */
	@Override
	public final void sessionAdded ()
	{
		getUI ().sessionAdded (this);
	}

	/**
	 * Let the UI know when sessions are removed
	 */
	@Override
	public final void sessionRemoved ()
	{
		getUI ().sessionRemoved (this);
	}
	
	/**
	 * @return Server-side multiplayer utils
	 */
	@Override
	public final MultiplayerServerUtils getMultiplayerServerUtils ()
	{
		return multiplayerServerUtils;
	}
	
	/**
	 * @param utils Server-side multiplayer utils
	 */
	public final void setMultiplayerServerUtils (final MultiplayerServerUtils utils)
	{
		multiplayerServerUtils = utils;
	}
	
	/**
	 * @return Methods for updating true map + players' memory
	 */
	@Override
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}

	/**
	 * @return Resource calculations
	 */
	@Override
	public final MomServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final MomServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}

	/**
	 * @return Methods for dealing with player msgs
	 */
	@Override
	public final PlayerMessageProcessing getPlayerMessageProcessing ()
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
	 * @return Spell processing methods
	 */
	@Override
	public final SpellProcessing getSpellProcessing ()
	{
		return spellProcessing;
	}

	/**
	 * @param obj Spell processing methods
	 */
	public final void setSpellProcessing (final SpellProcessing obj)
	{
		spellProcessing = obj;
	}

	/**
	 * @return City processing methods
	 */
	@Override
	public final CityProcessing getCityProcessing ()
	{
		return cityProcessing;
	}

	/**
	 * @param obj City processing methods
	 */
	public final void setCityProcessing (final CityProcessing obj)
	{
		cityProcessing = obj;
	}

	/**
	 * @return Combat processing
	 */
	@Override
	public final CombatProcessing getCombatProcessing ()
	{
		return combatProcessing;
	}

	/**
	 * @param obj Combat processing
	 */
	public final void setCombatProcessing (final CombatProcessing obj)
	{
		combatProcessing = obj;
	}

	/**
	 * @return City calculations
	 */
	@Override
	public final MomCityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final MomCityCalculations calc)
	{
		cityCalculations = calc;
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
	 * @return Resource value utils
	 */
	@Override
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils utils)
	{
		resourceValueUtils = utils;
	}

	/**
	 * @return Spell utils
	 */
	@Override
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
	}
	
	/**
	 * @return Maintained spell utils
	 */
	@Override
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param utils Maintained spell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils utils)
	{
		memoryMaintainedSpellUtils = utils;
	}	

	/**
	 * @return Builiding utils 
	 */
	@Override
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}
	
	/**
	 * @return Pending movement utils
	 */
	@Override
	public final PendingMovementUtils getPendingMovementUtils ()
	{
		return pendingMovementUtils;
	}

	/**
	 * @param utils Pending movement utils
	 */
	public final void setPendingMovementUtils (final PendingMovementUtils utils)
	{
		pendingMovementUtils = utils;
	}
	
	/**
	 * @param utils Builiding utils 
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}
	
	/**
	 * @return Unit utils
	 */
	@Override
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}

	/**
	 * @return Player pick utils
	 */
	@Override
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}

	/**
	 * @return Server-only pick utils
	 */
	@Override
	public final PlayerPickServerUtils getPlayerPickServerUtils ()
	{
		return playerPickServerUtils;
	}

	/**
	 * @param utils Server-only pick utils
	 */
	public final void setPlayerPickServerUtils (final PlayerPickServerUtils utils)
	{
		playerPickServerUtils = utils;
	}

	/**
	 * @return Server-only city utils
	 */
	@Override
	public final CityServerUtils getCityServerUtils ()
	{
		return cityServerUtils;
	}

	/**
	 * @param utils Server-only city utils
	 */
	public final void setCityServerUtils (final CityServerUtils utils)
	{
		cityServerUtils = utils;
	}

	/**
	 * @return Server-only unit utils
	 */
	@Override
	public final UnitServerUtils getUnitServerUtils ()
	{
		return unitServerUtils;
	}

	/**
	 * @param utils Server-only unit utils
	 */
	public final void setUnitServerUtils (final UnitServerUtils utils)
	{
		unitServerUtils = utils;
	}
	
	/**
	 * @return Server-only spell utils
	 */
	@Override
	public final SpellServerUtils getSpellServerUtils ()
	{
		return spellServerUtils;
	}

	/**
	 * @param utils Server-only spell utils
	 */
	public final void setSpellServerUtils (final SpellServerUtils utils)
	{
		spellServerUtils = utils;
	}

	/**
	 * @return Server-only unit calculations
	 */
	@Override
	public final MomServerUnitCalculations getServerUnitCalculations ()
	{
		return serverUnitCalculations;
	}

	/**
	 * @param calc Server-only unit calculations
	 */
	public final void setServerUnitCalculations (final MomServerUnitCalculations calc)
	{
		serverUnitCalculations = calc;
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
	 * @return Server-only overland map utils
	 */
	@Override
	public final OverlandMapServerUtils getOverlandMapServerUtils ()
	{
		return overlandMapServerUtils;
	}
	
	/**
	 * @param utils Server-only overland map utils
	 */
	public final void setOverlandMapServerUtils (final OverlandMapServerUtils utils)
	{
		overlandMapServerUtils = utils;
	}
}
