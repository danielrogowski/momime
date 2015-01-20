package momime.client.scheduledcombatmessages;

import java.util.List;

import momime.common.MomException;

/**
 * Methods dealing with generating the text and categories on the scheduled combats scroll, from the raw combat list.
 */
public interface ScheduledCombatMessageProcessing
{
	/**
	 * @return List of NTMs sorted and with title categories added, ready to display in the UI
	 * @throws MomException If one of the messages doesn't support the NewTurnMessageUI interface
	 */
	public List<ScheduledCombatMessageUI> sortAndAddCategories () throws MomException;

	/**
	 * @param nbr The number of scheduled combats that still need to be played, but we aren't involved in
	 */
	public void setScheduledCombatsNotInvolvedIn (final int nbr);
}