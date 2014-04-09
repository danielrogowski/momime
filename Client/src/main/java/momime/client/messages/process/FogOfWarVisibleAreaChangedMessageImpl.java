package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.servertoclient.v0_9_5.AddBuildingMessageData;
import momime.common.messages.servertoclient.v0_9_5.AddCombatAreaEffectMessageData;
import momime.common.messages.servertoclient.v0_9_5.AddMaintainedSpellMessageData;
import momime.common.messages.servertoclient.v0_9_5.AddUnitMessageData;
import momime.common.messages.servertoclient.v0_9_5.CancelCombatAreaEffectMessageData;
import momime.common.messages.servertoclient.v0_9_5.DestroyBuildingMessageData;
import momime.common.messages.servertoclient.v0_9_5.FogOfWarStateMessageData;
import momime.common.messages.servertoclient.v0_9_5.FogOfWarVisibleAreaChangedMessage;
import momime.common.messages.servertoclient.v0_9_5.KillUnitMessageData;
import momime.common.messages.servertoclient.v0_9_5.SwitchOffMaintainedSpellMessageData;
import momime.common.messages.servertoclient.v0_9_5.UpdateCityMessageData;
import momime.common.messages.servertoclient.v0_9_5.UpdateNodeLairTowerUnitIDMessageData;
import momime.common.messages.servertoclient.v0_9_5.UpdateTerrainMessageData;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

/**
 * Server sends this main message to update the client on changes in their fog of war area and what units, buildings, spells, CAEs, etc. they can see.
 * It basically comprises 0..n of most of the other types of message defined above, sent together so that the client processes them in a single transaction/locked update.
 */
public final class FogOfWarVisibleAreaChangedMessageImpl extends FogOfWarVisibleAreaChangedMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (FogOfWarVisibleAreaChangedMessageImpl.class.getName ());

	/**
	 * @param sender Connection to the server
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void process (final MultiplayerServerConnection sender)
		throws JAXBException, XMLStreamException, IOException
	{
		log.entering (FogOfWarVisibleAreaChangedMessageImpl.class.getName (), "process", new String []
			{getTriggeredFrom (), new Integer (getTerrainUpdate ().size ()).toString (), new Integer (getCityUpdate ().size ()).toString (),
			new Integer (getAddBuilding ().size ()).toString (), new Integer (getDestroyBuilding ().size ()).toString (),
			new Integer (getAddUnit ().size ()).toString (), new Integer (getKillUnit ().size ()).toString (),
			new Integer (getUpdateNodeLairTowerUnitID ().size ()).toString (), new Integer (getAddMaintainedSpell ().size ()).toString (),
			new Integer (getSwitchOffMaintainedSpell ().size ()).toString (), new Integer (getAddCombatAreaEffect ().size ()).toString (),
			new Integer (getCancelCombaAreaEffect ().size ()).toString (), new Integer (getFogOfWarUpdate ().size ()).toString ()});
		
		// Changes in Terrain
		for (final UpdateTerrainMessageData data : getTerrainUpdate ())
		{
			final UpdateTerrainMessageImpl proc = new UpdateTerrainMessageImpl ();
			proc.setData (data);
			proc.process (sender);
		}
		
		// Changes in Cities
		for (final UpdateCityMessageData data : getCityUpdate ())
		{
			final UpdateCityMessageImpl proc = new UpdateCityMessageImpl ();
			proc.setData (data);
			proc.process (sender);
		}
		
		// Buildings added or come into view
		for (final AddBuildingMessageData data : getAddBuilding ())
		{
			final AddBuildingMessageImpl proc = new AddBuildingMessageImpl ();
			proc.setData (data);
			proc.process (sender);
		}
		
		// Buildings destroyed or gone out of view
		for (final DestroyBuildingMessageData data : getDestroyBuilding ())
		{
			final DestroyBuildingMessageImpl proc = new DestroyBuildingMessageImpl ();
			proc.setData (data);
			proc.process (sender);
		}
		
		// Units added or come into view
		for (final AddUnitMessageData data : getAddUnit ())
		{
			final AddUnitMessageImpl proc = new AddUnitMessageImpl ();
			proc.setData (data);
			proc.process (sender);
		}
		
		// Units killed or gone out of view
		for (final KillUnitMessageData data : getKillUnit ())
		{
			final KillUnitMessageImpl proc = new KillUnitMessageImpl ();
			proc.setData (data);
			proc.process (sender);
		}
		
		// Scouted monster IDs changed
		for (final UpdateNodeLairTowerUnitIDMessageData data : getUpdateNodeLairTowerUnitID ())
		{
			final UpdateNodeLairTowerUnitIDMessageImpl proc = new UpdateNodeLairTowerUnitIDMessageImpl ();
			proc.setData (data);
			proc.process (sender);
		}
		
		// Maintained spells added or come into view
		for (final AddMaintainedSpellMessageData data : getAddMaintainedSpell ())
		{
			final AddMaintainedSpellMessageImpl proc = new AddMaintainedSpellMessageImpl ();
			proc.setData (data);
			proc.process (sender);
		}
		
		// Maintained spells switched off or gone out of view
		for (final SwitchOffMaintainedSpellMessageData data : getSwitchOffMaintainedSpell ())
		{
			final SwitchOffMaintainedSpellMessageImpl proc = new SwitchOffMaintainedSpellMessageImpl ();
			proc.setData (data);
			proc.process (sender);
		}
			
		// CAEs added or come into view
		for (final AddCombatAreaEffectMessageData data : getAddCombatAreaEffect ())
		{
			final AddCombatAreaEffectMessageImpl proc = new AddCombatAreaEffectMessageImpl ();
			proc.setData (data);
			proc.process (sender);
		}
		
		// CAEs cancelled or gone out of view
		for (final CancelCombatAreaEffectMessageData data : getCancelCombaAreaEffect ())
		{
			final CancelCombatAreaEffectMessageImpl proc = new CancelCombatAreaEffectMessageImpl ();
			proc.setData (data);
			proc.process (sender);
		}
		
		// Changes in Fog of War area
		for (final FogOfWarStateMessageData data : getFogOfWarUpdate ())
		{
		}

		log.exiting (FogOfWarVisibleAreaChangedMessageImpl.class.getName (), "process");
	}
}
