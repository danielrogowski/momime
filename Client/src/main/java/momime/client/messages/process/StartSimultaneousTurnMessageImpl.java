package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.newturnmessages.NewTurnMessageProcessing;
import momime.client.newturnmessages.NewTurnMessageStatus;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.frames.HistoryUI;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.calculations.CityCalculations;
import momime.common.messages.TurnPhase;
import momime.common.messages.TurnSystem;
import momime.common.messages.servertoclient.StartSimultaneousTurnMessage;

/**
 * Server sends this to all clients at the start of a new turn in a simultaneous turns game
 */
public final class StartSimultaneousTurnMessageImpl extends StartSimultaneousTurnMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** New turn messages helper methods */
	private NewTurnMessageProcessing newTurnMessageProcessing;
	
	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/** UI for screen showing power base history for each wizard */
	private HistoryUI historyUI;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		// Update turn number
		getClient ().getGeneralPublicKnowledge ().setTurnNumber (getTurnNumber ());
		getOverlandMapUI ().updateTurnLabelText ();
		getHistoryUI ().updateTurnLabelText ();
		
		// Allocating moves
		if (getClient ().getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS)
		{
			getClient ().getGeneralPublicKnowledge ().setTurnPhase (TurnPhase.ALLOCATING_MOVES);
			getOverlandMapRightHandPanel ().turnSystemOrCurrentPlayerChanged ();
		}
		
		// Allow selling buildings
		getCityCalculations ().blankBuildingsSoldThisTurn (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (), 0);
		
		// Read in the new turn messages
		if (isExpireMessages ())
			getNewTurnMessageProcessing ().expireMessages ();
		
		getNewTurnMessageProcessing ().readNewTurnMessagesFromServer (getMessage (), NewTurnMessageStatus.MAIN);
		getNewTurnMessagesUI ().setNewTurnMessages (getNewTurnMessageProcessing ().sortAndAddCategories ());
		
		// Only show the form if we got new messages - doesn't matter if there's some old ones we've already seen
		if (getMessage ().size () > 0)
			getNewTurnMessagesUI ().setVisible (true);
		
		// Simultaneous turns games have no 'continued movement' at the start of a turn, since its all processed at the end of the previous turn
		getOverlandMapProcessing ().setProcessingContinuedMovement (false);
		getOverlandMapProcessing ().buildUnitsLeftToMoveList ();
		getOverlandMapProcessing ().updateMovementRemaining ();
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
	 * @return City calculations
	 */
	public final CityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final CityCalculations calc)
	{
		cityCalculations = calc;
	}
	
	/**
	 * @return New turn messages helper methods
	 */
	public final NewTurnMessageProcessing getNewTurnMessageProcessing ()
	{
		return newTurnMessageProcessing;
	}

	/**
	 * @param proc New turn messages helper methods
	 */
	public final void setNewTurnMessageProcessing (final NewTurnMessageProcessing proc)
	{
		newTurnMessageProcessing = proc;
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

	/**
	 * @return UI for screen showing power base history for each wizard
	 */
	public final HistoryUI getHistoryUI ()
	{
		return historyUI;
	}

	/**
	 * @param h UI for screen showing power base history for each wizard
	 */
	public final void setHistoryUI (final HistoryUI h)
	{
		historyUI = h;
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