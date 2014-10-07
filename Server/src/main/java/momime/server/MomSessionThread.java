package momime.server;

import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.FogOfWarStateID;
import momime.common.messages.v0_9_5.MagicPowerDistribution;
import momime.common.messages.v0_9_5.MapAreaOfFogOfWarStates;
import momime.common.messages.v0_9_5.MapAreaOfMemoryGridCells;
import momime.common.messages.v0_9_5.MapRowOfFogOfWarStates;
import momime.common.messages.v0_9_5.MapRowOfMemoryGridCells;
import momime.common.messages.v0_9_5.MapVolumeOfFogOfWarStates;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.MemoryGridCell;
import momime.common.messages.v0_9_5.MomGeneralPublicKnowledge;
import momime.common.messages.v0_9_5.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.MomTransientPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.SpellResearchStatus;
import momime.common.messages.v0_9_5.SpellResearchStatusID;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_5.Spell;
import momime.server.mapgenerator.CombatMapGeneratorImpl;
import momime.server.mapgenerator.OverlandMapGenerator;
import momime.server.messages.v0_9_5.MomGeneralServerKnowledge;
import momime.server.ui.MomServerUI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
	private final Log log = LogFactory.getLog (MomSessionThread.class);
	
	/** Combat map coordinate system, expect this to be merged into session desc once client is also in Java */
	private final CoordinateSystem combatMapCoordinateSystem;
	
	/** Lookup lists built over the XML database */
	private ServerDatabaseEx db;

	/** UI being used by server */
	private MomServerUI ui;

	/** Logger for logging key messages relating to this session */
	private Log sessionLogger;

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
		log.trace ("Entering createPersistentPlayerPrivateKnowledge");
		
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
		dist.setManaRatio		(CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
		dist.setResearchRatio	(CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
		dist.setSkillRatio			(CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
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

		log.trace ("Exiting createPersistentPlayerPrivateKnowledge = " + priv);
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