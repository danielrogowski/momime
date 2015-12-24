package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.client.newturnmessages.NewTurnMessageProcessing;
import momime.client.newturnmessages.NewTurnMessageStatus;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.messages.servertoclient.SetCurrentPlayerMessage;

/**
 * Server sends this to all clients at the start of a new players' turn in a one-at-a-time turns game
 */
public final class SetCurrentPlayerMessageImpl extends SetCurrentPlayerMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SetCurrentPlayerMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/** New turn messages helper methods */
	private NewTurnMessageProcessing newTurnMessageProcessing;
	
	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: Turn " + getTurnNumber () + ", player ID " + getCurrentPlayerID () + ", message count " + getMessage ().size () + ", expire? " + isExpireMessages ());
		
		// Even if we're just changing player, and not turn, update the turn number label anyway, because when loading a saved game
		// it will appear that the turn number isn't changing, so we need to make sure the label gets set correctly then too.
		getClient ().getGeneralPublicKnowledge ().setTurnNumber (getTurnNumber ());
		getOverlandMapUI ().updateTurnLabelText ();
		
		// Update player
		getClient ().getGeneralPublicKnowledge ().setCurrentPlayerID (getCurrentPlayerID ());

		// Update label to show current player (if its our turn, this is hidden behind the next turn button)
		getOverlandMapRightHandPanel ().turnSystemOrCurrentPlayerChanged ();
		
		// Work out the position to scroll the colour patch to
		final PlayerPublicDetails currentPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getCurrentPlayerID (), "SetCurrentPlayerMessageImpl");
		getOverlandMapRightHandPanel ().setIndexOfCurrentPlayer (getClient ().getPlayers ().indexOf (currentPlayer));

		// Read in the new turn messages
		if (isExpireMessages ())
			getNewTurnMessageProcessing ().expireMessages ();
		
		getNewTurnMessageProcessing ().readNewTurnMessagesFromServer (getMessage (), NewTurnMessageStatus.MAIN);
		getNewTurnMessagesUI ().setNewTurnMessages (getNewTurnMessageProcessing ().sortAndAddCategories ());
		
		// Only show the form if we got new messages - doesn't matter if there's some old ones we've already seen
		if (getMessage ().size () > 0)
			getNewTurnMessagesUI ().setVisible (true);
		
		// Allow selling buildings
		getCityCalculations ().blankBuildingsSoldThisTurn (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (), getCurrentPlayerID ());

		// Give units full movement
		if (getCurrentPlayerID () == getClient ().getOurPlayerID ())
			getUnitCalculations ().resetUnitOverlandMovement (getCurrentPlayerID (),
				getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());

		// This gets triggered before the server sends us any continued movement
		// (i.e. before the server processes pending movements).		
		// So at this stage we definitely don't want to allow the next turn button.
		getOverlandMapProcessing ().setProcessingContinuedMovement (true);
		
		// This also sets whether the next turn button is visible or not
		getOverlandMapProcessing ().updateMovementRemaining ();
		
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
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
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
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}
}