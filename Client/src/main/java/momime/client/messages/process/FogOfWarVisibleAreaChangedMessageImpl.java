package momime.client.messages.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.areas.storage.MapArea3DArrayListImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.BaseServerToClientMessage;
import com.ndg.utils.Holder;

import momime.client.MomClient;
import momime.client.ui.frames.ArmyListUI;
import momime.client.ui.frames.CitiesListUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.FogOfWarStateMessageData;
import momime.common.messages.servertoclient.FogOfWarVisibleAreaChangedMessage;
import momime.common.messages.servertoclient.UpdateCityMessageData;
import momime.common.messages.servertoclient.UpdateTerrainMessageData;

/**
 * Server sends this main message to update the client on changes in their fog of war area and what units, buildings, spells, CAEs, etc. they can see.
 * It basically comprises 0..n of most of the other types of message defined above, sent together so that the client processes them in a single transaction/locked update.
 */
public final class FogOfWarVisibleAreaChangedMessageImpl extends FogOfWarVisibleAreaChangedMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (FogOfWarVisibleAreaChangedMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Factory for creating prototype message beans from spring */
	private ServerToClientMessagesFactory factory;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** Army list */
	private ArmyListUI armyListUI;
	
	/** Cities list */
	private CitiesListUI citiesListUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		if (log.isTraceEnabled ())
			log.trace ("Entering start: " + getTriggeredFrom () + ", " + getTerrainUpdate ().size () + ", " + getCityUpdate ().size () + ", " +
				getAddBuilding ().size () + ", " + getDestroyBuilding ().size () + ", " + getAddOrUpdateUnit ().size () + ", " + getKillUnit ().size () + ", " +
				getAddMaintainedSpell ().size () + ", " + getSwitchOffMaintainedSpell ().size () + ", " +
				getAddCombatAreaEffect ().size () + ", " + getCancelCombaAreaEffect ().size () + ", " + getFogOfWarUpdate ().size ());
		
		// Changes in Terrain
		if (getTerrainUpdate ().size () > 0)
		{
			final MapArea3D<Boolean> areaToSmooth = new MapArea3DArrayListImpl<Boolean> ();
			areaToSmooth.setCoordinateSystem (getClient ().getSessionDescription ().getOverlandMapSize ());
			
			final UpdateTerrainMessageImpl proc = getFactory ().createUpdateTerrainMessage ();
			for (final UpdateTerrainMessageData data : getTerrainUpdate ())
			{				
				proc.setData (data);
				proc.processOneUpdate (areaToSmooth);
			}
			proc.endUpdates (areaToSmooth);
		}
		
		// Changes in Cities
		if (getCityUpdate ().size () > 0)
		{
			final UpdateCityMessageImpl proc = getFactory ().createUpdateCityMessage ();
			for (final UpdateCityMessageData data : getCityUpdate ())
			{
				proc.setData (data);
				proc.processOneUpdate ();
			}
		}
		
		// Buildings added or come into view
		if (getAddBuilding ().size () > 0)
		{
			final AddBuildingMessageImpl proc = getFactory ().createAddBuildingMessage ();
			for (final MemoryBuilding thisBuilding : getAddBuilding ())
			{
				proc.setFirstBuilding (thisBuilding);
				proc.processOneUpdate ();
			}
		}
		
		// Buildings destroyed or gone out of view
		if (getDestroyBuilding ().size () > 0)
		{
			final DestroyBuildingMessageImpl proc = getFactory ().createDestroyBuildingMessage ();
			for (final Integer thisBuildingURN : getDestroyBuilding ())
			{
				proc.setBuildingURN (thisBuildingURN);
				proc.processOneUpdate ();
			}
		}
		
		// Units added, changed or come into view
		if (getAddOrUpdateUnit ().size () > 0)
		{
			final List<MapCoordinates3DEx> unitLocations = new ArrayList<MapCoordinates3DEx> ();
			final Holder<MapCoordinates3DEx> ourUnitLocation = new Holder<MapCoordinates3DEx> ();
			final Holder<Boolean> anyOfOurHeroes = new Holder<Boolean> (false);
			
			final AddOrUpdateUnitMessageImpl proc = getFactory ().createAddOrUpdateUnitMessage ();
			for (final MemoryUnit thisUnit : getAddOrUpdateUnit ())
			{
				proc.setMemoryUnit (thisUnit);
				proc.processOneUpdate (unitLocations, ourUnitLocation, anyOfOurHeroes);
			}
			proc.endUpdates (unitLocations, ourUnitLocation, anyOfOurHeroes);
		}
		
		// Units killed or gone out of view
		if (getKillUnit ().size () > 0)
		{
			final KillUnitMessageImpl proc = getFactory ().createKillUnitMessage ();
			// Leave newStatus as null so units are completed removed - obviously we're never going to lose sight of our own units
			
			for (final Integer thisUnitURN : getKillUnit ())
			{
				proc.setUnitURN (thisUnitURN);
				proc.start ();
			}
		}
		
		// Maintained spells added or come into view
		if (getAddMaintainedSpell ().size () > 0)
		{
			final AddMaintainedSpellMessageImpl proc = getFactory ().createAddMaintainedSpellMessage ();
			for (final MemoryMaintainedSpell thisSpell : getAddMaintainedSpell ())
			{				
				proc.setMaintainedSpell (thisSpell);
				proc.processOneUpdate ();
			}
		}
		
		// Maintained spells switched off or gone out of view
		if (getSwitchOffMaintainedSpell ().size () > 0)
		{
			final SwitchOffMaintainedSpellMessageImpl proc = getFactory ().createSwitchOffMaintainedSpellMessage ();
			for (final Integer thisSpellURN : getSwitchOffMaintainedSpell ())
			{
				proc.setSpellURN (thisSpellURN);
				proc.processOneUpdate ();
			}
		}
			
		// CAEs added or come into view
		if (getAddCombatAreaEffect ().size () > 0)
		{
			final AddCombatAreaEffectMessageImpl proc = getFactory ().createAddCombatAreaEffectMessage ();
			for (final MemoryCombatAreaEffect thisCAE : getAddCombatAreaEffect ())
			{
				proc.setMemoryCombatAreaEffect (thisCAE);
				proc.start ();
			}
		}
		
		// CAEs cancelled or gone out of view
		if (getCancelCombaAreaEffect ().size () > 0)
		{
			final CancelCombatAreaEffectMessageImpl proc = getFactory ().createCancelCombatAreaEffectMessage ();
			for (final Integer thisCombatAreaEffectURN : getCancelCombaAreaEffect ())
			{
				proc.setCombatAreaEffectURN (thisCombatAreaEffectURN);
				proc.start ();
			}
		}
		
		// Changes in Fog of War area
		if (getFogOfWarUpdate ().size () > 0)
		{
			for (final FogOfWarStateMessageData data : getFogOfWarUpdate ())
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWar ().getPlane ().get
					(data.getMapLocation ().getZ ()).getRow ().get (data.getMapLocation ().getY ()).getCell ().set (data.getMapLocation ().getX (), data.getState ());

			getOverlandMapUI ().regenerateFogOfWarBitmap ();
		}
			
		// Many items need regenerating, depending on the updates we got
		if ((getTerrainUpdate ().size () > 0) || (getCityUpdate ().size () > 0) || (getAddBuilding ().size () > 0) || (getDestroyBuilding ().size () > 0)) 
			getOverlandMapUI ().regenerateOverlandMapBitmaps ();
		
		if ((getTerrainUpdate ().size () > 0) || (getCityUpdate ().size () > 0))
		{
			getOverlandMapRightHandPanel ().regenerateMiniMapBitmap ();
			getArmyListUI ().regenerateMiniMapBitmaps ();
		}
		
		if (getCityUpdate ().size () > 0)
		{
			getCitiesListUI ().refreshCitiesList ();
			getCitiesListUI ().regenerateMiniMapBitmaps ();
		}

		log.trace ("Exiting start");
	}

	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}

	/**
	 * @return Factory for creating prototype message beans from spring
	 */
	public final ServerToClientMessagesFactory getFactory ()
	{
		return factory;
	}

	/**
	 * @param fac Factory for creating prototype message beans from spring
	 */
	public final void setFactory (final ServerToClientMessagesFactory fac)
	{
		factory = fac;
	}

	/**
	 * @return Overland map UI
	 */
	public final OverlandMapUI getOverlandMapUI ()
	{
		return overlandMapUI;
	}

	/**
	 * @param ui Overland map UI
	 */
	public final void setOverlandMapUI (final OverlandMapUI ui)
	{
		overlandMapUI = ui;
	}

	/**
	 * @return Overland map right hand panel showing economy etc
	 */
	public final OverlandMapRightHandPanel getOverlandMapRightHandPanel ()
	{
		return overlandMapRightHandPanel;
	}

	/**
	 * @param panel Overland map right hand panel showing economy etc
	 */
	public final void setOverlandMapRightHandPanel (final OverlandMapRightHandPanel panel)
	{
		overlandMapRightHandPanel = panel;
	}

	/**
	 * @return Army list
	 */
	public final ArmyListUI getArmyListUI ()
	{
		return armyListUI;
	}
	
	/**
	 * @param ui Army list
	 */
	public final void setArmyListUI (final ArmyListUI ui)
	{
		armyListUI = ui;
	}

	/**
	 * @return Cities list
	 */
	public final CitiesListUI getCitiesListUI ()
	{
		return citiesListUI;
	}

	/**
	 * @param ui Cities list
	 */
	public final void setCitiesListUI (final CitiesListUI ui)
	{
		citiesListUI = ui;
	}
}