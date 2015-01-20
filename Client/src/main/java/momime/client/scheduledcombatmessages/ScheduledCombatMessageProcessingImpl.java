package momime.client.scheduledcombatmessages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import momime.client.MomClient;
import momime.common.MomException;
import momime.common.messages.MomScheduledCombat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Methods dealing with generating the text and categories on the scheduled combats scroll, from the raw combat list.
 */
public final class ScheduledCombatMessageProcessingImpl implements ScheduledCombatMessageProcessing
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ScheduledCombatMessageProcessingImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Factory for creating scheduled combat messages from spring prototypes */
	private ScheduledCombatMessagesFactory scheduledCombatMessagesFactory;
	
	/** The number of scheduled combats that still need to be played, but we aren't involved in */
	private int scheduledCombatsNotInvolvedIn;
	
	/**
	 * @return List of NTMs sorted and with title categories added, ready to display in the UI
	 * @throws MomException If one of the messages doesn't support the NewTurnMessageUI interface
	 */
	@Override
	public final List<ScheduledCombatMessageUI> sortAndAddCategories () throws MomException
	{
		log.trace ("Entering sortAndAddCategories: " + getClient ().getOurTransientPlayerPrivateKnowledge ().getScheduledCombat ().size ());
		
		// Copy into a temporary list
		final List<ScheduledCombatMessageUI> msgs = new ArrayList<ScheduledCombatMessageUI> ();
		for (final MomScheduledCombat combat : getClient ().getOurTransientPlayerPrivateKnowledge ().getScheduledCombat ())
		{
			final ScheduledCombatMessageCombat msg = getScheduledCombatMessagesFactory ().createScheduledCombatMessageCombat ();
			msg.setCombat (combat);
			msgs.add (msg);
		}
		
		final ScheduledCombatMessageOther other = getScheduledCombatMessagesFactory ().createScheduledCombatMessageOther ();
		other.setScheduledCombatsNotInvolvedIn (getScheduledCombatsNotInvolvedIn ());
		msgs.add (other);
		
		// Sort it
		Collections.sort (msgs, new ScheduledCombatMessageSorter ());
		
		// Slot in categories into the right places
		int n = 0;
		ScheduledCombatMessageSortOrder sortOrder = null;
		
		while (n < msgs.size ())
		{
			final ScheduledCombatMessageUI msg = msgs.get (n);
			
			// Did the sort order/category change?
			if ((sortOrder == null) || (!sortOrder.equals (msg.getSortOrder ())))
			{
				sortOrder = msg.getSortOrder ();
				
				// Slot in a category title
				final ScheduledCombatMessageCategory category = getScheduledCombatMessagesFactory ().createScheduledCombatMessageCategory ();
				category.setSortOrder (sortOrder);
				msgs.add (n, category);
				n++;
			}
			
			n++;
		}

		log.trace ("Exiting sortAndAddCategories = " + msgs.size ());
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
	 * @return Factory for creating scheduled combat messages from spring prototypes
	 */
	public final ScheduledCombatMessagesFactory getScheduledCombatMessagesFactory ()
	{
		return scheduledCombatMessagesFactory;
	}

	/**
	 * @param fac Factory for creating scheduled combat messages from spring prototypes
	 */
	public final void setScheduledCombatMessagesFactory (final ScheduledCombatMessagesFactory fac)
	{
		scheduledCombatMessagesFactory = fac;
	}

	/**
	 * @return The number of scheduled combats that still need to be played, but we aren't involved in
	 */
	public final int getScheduledCombatsNotInvolvedIn ()
	{
		return scheduledCombatsNotInvolvedIn;
	}

	/**
	 * @param nbr The number of scheduled combats that still need to be played, but we aren't involved in
	 */
	@Override
	public final void setScheduledCombatsNotInvolvedIn (final int nbr)
	{
		scheduledCombatsNotInvolvedIn = nbr;
	}
}