package momime.server;

import java.util.logging.Logger;

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
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.Spell;
import momime.server.mapgenerator.CombatMapGeneratorImpl;
import momime.server.mapgenerator.OverlandMapGenerator;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;
import momime.server.ui.MomServerUI;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemType;
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

	/** Overland map generator for this session */
	private OverlandMapGenerator overlandMapGenerator;	
	
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
}
