package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.OverlandMapUI;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.FogOfWarStateMessageData;
import momime.common.messages.servertoclient.FogOfWarVisibleAreaChangedMessage;
import momime.common.messages.servertoclient.KillUnitActionID;
import momime.common.messages.servertoclient.UpdateCityMessageData;
import momime.common.messages.servertoclient.UpdateTerrainMessageData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.areas.storage.MapArea3DArrayListImpl;
import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this main message to update the client on changes in their fog of war area and what units, buildings, spells, CAEs, etc. they can see.
 * It basically comprises 0..n of most of the other types of message defined above, sent together so that the client processes them in a single transaction/locked update.
 */
public final class FogOfWarVisibleAreaChangedMessageImpl extends FogOfWarVisibleAreaChangedMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (FogOfWarVisibleAreaChangedMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Factory for creating prototype message beans from spring */
	private ServerToClientMessagesFactory factory;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
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
				getAddBuilding ().size () + ", " + getDestroyBuilding ().size () + ", " + getAddUnit ().size () + ", " + getKillUnit ().size () + ", " +
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
		
		// Units added or come into view
		if (getAddUnit ().size () > 0)
		{
			final AddUnitMessageImpl proc = getFactory ().createAddUnitMessage ();
			for (final MemoryUnit thisUnit : getAddUnit ())
			{
				proc.setMemoryUnit (thisUnit);
				proc.start ();
			}
		}
		
		// Units killed or gone out of view
		if (getKillUnit ().size () > 0)
		{
			final KillUnitMessageImpl proc = getFactory ().createKillUnitMessage ();
			proc.setKillUnitActionID (KillUnitActionID.VISIBLE_AREA_CHANGED);
			
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
		for (final FogOfWarStateMessageData data : getFogOfWarUpdate ())
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWar ().getPlane ().get
				(data.getMapLocation ().getZ ()).getRow ().get (data.getMapLocation ().getY ()).getCell ().set (data.getMapLocation ().getX (), data.getState ());
		
		// So much will have changed (terrain, cities, node auras, fog of war area, units) that best to just regenerate the lot
		getOverlandMapUI ().regenerateOverlandMapBitmaps ();
		getOverlandMapUI ().regenerateFogOfWarBitmap ();

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
}