package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.components.SelectUnitButton;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.MomException;
import momime.common.messages.servertoclient.v0_9_5.KillUnitMessage;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.UnitStatusID;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

/**
 * Server sends this to everyone to notify of dead units, except where it is already obvious from an Apply Damage message that a unit is dead
 */
public final class KillUnitMessageImpl extends KillUnitMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (KillUnitMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Pending movement utils */
	private PendingMovementUtils pendingMovementUtils;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
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
		log.trace ("Entering process: Unit URN " + getData ().getUnitURN () + ", " + getData ().getKillUnitActionID ());

		// Even if not actually freeing the unit, we still need to eliminate all references to it, except for it being in the main unit list
		getPendingMovementUtils ().removeUnitFromAnyPendingMoves (getClient ().getOurTransientPlayerPrivateKnowledge ().getPendingMovement (), getData ().getUnitURN ());
		getUnitUtils ().beforeKillingUnit (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getData ().getUnitURN ());	// Removes spells cast on unit
		
		// Is there a unit info screen open for it?
		final UnitInfoUI unitInfo = getClient ().getUnitInfos ().get (getData ().getUnitURN ());
		if (unitInfo != null)
			unitInfo.close ();
		
		// Select unit buttons on the Map
		for (final SelectUnitButton button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if ((button.getUnit () != null) && (button.getUnit ().getUnitURN () == getData ().getUnitURN ()))
			{
				button.setUnit (null);
				
				final boolean updateMovement = (button.isVisible ()) && (button.isSelected ());
				button.setSelected (false);
				button.setVisible (false);
					
				if (updateMovement)
				{
					// Do same processing as if button was manually clicked
					getOverlandMapProcessing ().enableOrDisableSpecialOrderButtons ();
					getOverlandMapProcessing ().updateMovementRemaining ();
				}
			}
		
		// Find the unit being removed
		final MemoryUnit unit = getUnitUtils ().findUnitURN (getData ().getUnitURN (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "KillUnitMessageImpl");
		
		// The server works out what action we need to take
		switch (getData ().getKillUnitActionID ())
		{
			// Phyically free the unit
			case FREE:
			case VISIBLE_AREA_CHANGED:
			case UNIT_LACK_OF_PRODUCTION:		// <-- Delphi client had different logic for this due to finicky memory management, TBC if needs to be different here or not
			case HERO_LACK_OF_PRODUCTION:
				getUnitUtils ().removeUnitURN (getData ().getUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ());
				break;
				
			// The two special statuses generated by ApplyDamageMessage on the client aren't handled yet
			
			default:
				throw new MomException ("KillUnitMessageImpl got an KillUnitAction that it doesn't know how to handle: " + getData ().getKillUnitActionID ());
		}

		// Select unit buttons on the City screen
		if ((unit.getStatus () == UnitStatusID.ALIVE) && (unit.getUnitLocation () != null))
		{
			final CityViewUI cityView = getClient ().getCityViews ().get (unit.getUnitLocation ().toString ());
			if (cityView != null)
				cityView.unitsChanged ();
		}
		
		log.trace ("Exiting process");
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
	 * @return Unit utils
	 */
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
	 * @return Pending movement utils
	 */
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
	 * @return Turn sequence and movement helper methods
	 */
	public final OverlandMapProcessing getOverlandMapProcessing ()
	{
		return overlandMapProcessing;
	}

	/**
	 * @param proc Turn sequence and movement helper methods
	 */
	public final void setOverlandMapProcessing (final OverlandMapProcessing proc)
	{
		overlandMapProcessing = proc;
	}
}