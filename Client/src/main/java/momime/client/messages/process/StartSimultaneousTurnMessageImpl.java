package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.newturnmessages.NewTurnMessageProcessing;
import momime.client.newturnmessages.NewTurnMessageStatus;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.messages.servertoclient.StartSimultaneousTurnMessage;

/**
 * Server sends this to all clients at the start of a new turn in a simultaneous turns game
 */
public final class StartSimultaneousTurnMessageImpl extends StartSimultaneousTurnMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (StartSimultaneousTurnMessageImpl.class);

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
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: Turn " + getTurnNumber () + ", message count " + getMessage ().size () + ", expire? " + isExpireMessages ());

		// Update turn number
		getClient ().getGeneralPublicKnowledge ().setTurnNumber (getTurnNumber ());
		getOverlandMapUI ().updateTurnLabelText ();
		
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
		
		// Give units full movement
		getUnitCalculations ().resetUnitOverlandMovement (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), 0,
			getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ());

		// Simultaneous turns games have no 'continued movement' at the start of a turn, since its all processed at the end of the previous turn
		getOverlandMapProcessing ().setProcessingContinuedMovement (false);
		getOverlandMapProcessing ().buildUnitsLeftToMoveList ();
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
}