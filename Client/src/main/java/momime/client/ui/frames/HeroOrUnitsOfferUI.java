package momime.client.ui.frames;

import java.awt.Dimension;
import java.awt.Polygon;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.WindowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.utils.swing.actions.LoggingAction;

import momime.client.MomClient;
import momime.client.newturnmessages.NewTurnMessageOfferEx;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.client.ui.panels.UnitInfoPanel;
import momime.common.MomException;
import momime.common.messages.AvailableUnit;
import momime.common.messages.clienttoserver.RequestAcceptOfferMessage;

/**
 * Dialog asking user to confirm or reject offer to hire a hero or some mercenary units
 */
public final class HeroOrUnitsOfferUI extends MomClientFrameUI implements OfferUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (HeroOrUnitsOfferUI.class);
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;

	/** Unit info panel */
	private UnitInfoPanel unitInfoPanel;
	
	/** The unit being hired */
	private AvailableUnit unit;
	
	/** The offer we're showing this UI for */
	private NewTurnMessageOfferEx newTurnMessageOffer;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** Hire action */
	private Action hireAction;

	/** Reject action */
	private Action rejectAction;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Actions
		hireAction = new LoggingAction ((ev) ->
		{
			final RequestAcceptOfferMessage msg = new RequestAcceptOfferMessage ();
			msg.setOfferURN (getNewTurnMessageOffer ().getOfferURN ());
			
			getClient ().getServerConnection ().sendMessageToServer (msg);
		});

		rejectAction = new LoggingAction ((ev) ->
		{
			getNewTurnMessageOffer ().setOfferAccepted (false);
			getNewTurnMessagesUI ().languageChanged ();
			getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
			getFrame ().dispose ();
		});

		getUnitInfoPanel ().getActions ().add (hireAction);
		getUnitInfoPanel ().getActions ().add (rejectAction);
		
		// Initialize the dialog
		final HeroOrUnitsOfferUI ui = this;
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getFrame ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (@SuppressWarnings ("unused") final WindowEvent ev)
			{
				try
				{
					getUnitInfoPanel ().unitInfoPanelClosing ();
				}
				catch (final MomException e)
				{
					log.error (e, e);
				}
				
				getLanguageChangeMaster ().removeLanguageChangeListener (ui);
				getClient ().getOffers ().remove (getNewTurnMessageOffer ().getOfferURN ());
			}
		});
		
		// Finish setting up the panel and frame
		getUnitInfoPanel ().setButtonsPositionRight (true);
		getFrame ().setContentPane (getUnitInfoPanel ().getPanel ());
		getUnitInfoPanel ().showUnit (getUnit ());
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);
		
		final Dimension panelSize = getUnitInfoPanel ().getPanel ().getPreferredSize ();
		getFrame ().setShape (new Polygon
			(new int [] {0, panelSize.width - getUnitInfoPanel ().getBackgroundButtonsWidth (), panelSize.width - getUnitInfoPanel ().getBackgroundButtonsWidth (), panelSize.width, panelSize.width, 0},
			new int [] {0, 0, panelSize.height - getUnitInfoPanel ().getBackgroundButtonsHeight (), panelSize.height - getUnitInfoPanel ().getBackgroundButtonsHeight (), panelSize.height, panelSize.height},
			6));
	}	

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		hireAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getUnitInfoScreen ().getHire ()));
		rejectAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getUnitInfoScreen ().getReject ()));
	}
	
	/**
	 * Close out the offer UI
	 */
	@Override
	public final void close ()
	{
		getFrame ().dispose ();
	}
	
	/**
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
	}

	/**
	 * @return Unit info panel
	 */
	public final UnitInfoPanel getUnitInfoPanel ()
	{
		return unitInfoPanel;
	}

	/**
	 * @param pnl Unit info panel
	 */
	public final void setUnitInfoPanel (final UnitInfoPanel pnl)
	{
		unitInfoPanel = pnl;
	}

	/**
	 * @return The unit being hired
	 */
	public final AvailableUnit getUnit ()
	{
		return unit;
	}

	/**
	 * @param obj The unit being hired
	 */
	public final void setUnit (final AvailableUnit obj)
	{
		unit = obj;
	}

	/**
	 * @return The offer we're showing this UI for
	 */
	public final NewTurnMessageOfferEx getNewTurnMessageOffer ()
	{
		return newTurnMessageOffer;
	}

	/**
	 * @param o The offer we're showing this UI for
	 */
	public final void setNewTurnMessageOffer (final NewTurnMessageOfferEx o)
	{
		newTurnMessageOffer = o;
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
	 * @return New turn messages UI
	 */
	public final NewTurnMessagesUI getNewTurnMessagesUI ()
	{
		return newTurnMessagesUI;
	}

	/**
	 * @param ui New turn messages UI
	 */
	public final void setNewTurnMessagesUI (final NewTurnMessagesUI ui)
	{
		newTurnMessagesUI = ui;
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
}