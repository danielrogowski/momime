package momime.client.newturnmessages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javazoom.jl.decoder.JavaLayerException;
import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.MomException;
import momime.common.messages.NewTurnMessageData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Methods dealing with new turn messages
 */
public final class NewTurnMessageProcessingImpl implements NewTurnMessageProcessing
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (NewTurnMessageProcessingImpl.class);
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Factory for creating NTMs from spring prototypes */
	private NewTurnMessagesFactory newTurnMessagesFactory;
	
	/** Music player */
	private AudioPlayer musicPlayer;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/**
	 * At the start of a new turn, we get a new block of new turn messages, so need to get rid of the old ones.
	 * However we also may get messages show up during a turn, and we may or may not have had a chance to look at those, so we leave them for an additional turn.
	 * See much longer explanation of this in the comments of NewTurnMessageStatus.
	 */
	@Override
	public final void expireMessages ()
	{
		final Iterator<NewTurnMessageData> iter = getClient ().getOurTransientPlayerPrivateKnowledge ().getNewTurnMessage ().iterator ();
		while (iter.hasNext ())
		{
			// They must support the expiration interface, or they could not have been added to the list in the first place, see readNewTurnMessagesFromServer below
			final NewTurnMessageExpiration msg = (NewTurnMessageExpiration) iter.next ();
			
			if (msg.getStatus () == NewTurnMessageStatus.AFTER_OUR_TURN_BEGAN)
				msg.setStatus (NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN);
			else
				// It must be MAIN or BEFORE_OUR_TURN_BEGAN
				iter.remove ();
		}
	}
	
	/**
	 * Reads a block of new turn messages that the server has sent us.
	 * Kept separate since this is used for 3 types of message.
	 * 
	 * @param msgs New messages from server
	 * @param statusForNewMessages Status to give to the new messages
	 * @throws IOException If one of the messages doesn't support the NewTurnMessageExpiration interface or a preProcess method fails
	 */
	@Override
	public final void readNewTurnMessagesFromServer (final List<NewTurnMessageData> msgs, final NewTurnMessageStatus statusForNewMessages)
		throws IOException
	{
		String musicResourceName = null;
		Integer musicSortOrder = null;
		
		for (final NewTurnMessageData msg : msgs)
			if (msg instanceof NewTurnMessageExpiration)
			{
				// Does the message need to pre-process anything?
				if (msg instanceof NewTurnMessagePreProcess)
					((NewTurnMessagePreProcess) msg).preProcess ();
				
				// Set the correct status and add the message
				((NewTurnMessageExpiration) msg).setStatus (statusForNewMessages);
				getClient ().getOurTransientPlayerPrivateKnowledge ().getNewTurnMessage ().add (msg);
				
				// See if the message should play some music, and if so, how to prioritise between multiple messages each with music.
				// This is done here, rather than NewTurnMessagesUI, because here we only process new messages,
				// whereas NewTurnMessagesUI can't tell the difference between old and new messages.
				if ((msg instanceof NewTurnMessageUI) && (msg instanceof NewTurnMessageMusic))
				{
					final String thisMusicResourceName = ((NewTurnMessageMusic) msg).getMusicResourceName ();
					final int thisSortOrder = ((NewTurnMessageUI) msg).getSortOrder ().getSortOrder ();
					
					if ((thisMusicResourceName != null) &&
						((musicSortOrder == null) || (thisSortOrder > musicSortOrder)))
					{
						musicResourceName = thisMusicResourceName;
						musicSortOrder = thisSortOrder;
					}
				}
			}
			else
				throw new MomException ("readNewTurnMessagesFromServer: One of the messages in the list, with class " + msg.getClass ().getName () +
					", doesn't support the NewTurnMessageExpiration interface");
		
		if (musicResourceName != null)
			try
			{
				getMusicPlayer ().playThenResume (musicResourceName);
			}
			catch (final JavaLayerException e)
			{
				log.error (e, e);
			}
		
		// See if any new NTM must be acted on
		getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
	}
	
	/**
	 * @return List of NTMs sorted and with title categories added, ready to display in the UI
	 * @throws MomException If one of the messages doesn't support the NewTurnMessageUI interface
	 */
	@Override
	public final List<NewTurnMessageUI> sortAndAddCategories () throws MomException
	{
		// Copy into a temporary list and get the typecasting done
		final List<NewTurnMessageUI> msgs = new ArrayList<NewTurnMessageUI> ();
		for (final NewTurnMessageData msg : getClient ().getOurTransientPlayerPrivateKnowledge ().getNewTurnMessage ())
			if (msg instanceof NewTurnMessageUI)
				msgs.add ((NewTurnMessageUI) msg);
			else
				throw new MomException ("sortAndAddCategories: One of the messages in the list, with class " + msg.getClass ().getName () +
					", doesn't support the NewTurnMessageUI interface");
		
		// Sort it
		Collections.sort (msgs, new NewTurnMessageSorter ());
		
		// Slot in categories into the right places
		int n = 0;
		NewTurnMessageSortOrder sortOrder = null;
		
		while (n < msgs.size ())
		{
			final NewTurnMessageUI msg = msgs.get (n);
			
			// Did the sort order/category change?
			if ((sortOrder == null) || (!sortOrder.equals (msg.getSortOrder ())))
			{
				sortOrder = msg.getSortOrder ();
				
				// Slot in a category title
				final NewTurnMessageCategory category = getNewTurnMessagesFactory ().createNewTurnMessageCategory ();
				category.setSortOrder (sortOrder);
				msgs.add (n, category);
				n++;
			}
			
			n++;
		}

		return msgs;
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
	 * @return Factory for creating NTMs from spring prototypes
	 */
	public final NewTurnMessagesFactory getNewTurnMessagesFactory ()
	{
		return newTurnMessagesFactory;
	}

	/**
	 * @param fac Factory for creating NTMs from spring prototypes
	 */
	public final void setNewTurnMessagesFactory (final NewTurnMessagesFactory fac)
	{
		newTurnMessagesFactory = fac;
	}

	/**
	 * @return Music player
	 */
	public final AudioPlayer getMusicPlayer ()
	{
		return musicPlayer;
	}

	/**
	 * @param player Music player
	 */
	public final void setMusicPlayer (final AudioPlayer player)
	{
		musicPlayer = player;
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