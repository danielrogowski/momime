package momime.client.scheduledcombatmessages;

/**
 * All of the scheduled combat messages need things like the font and language file injected, so need to be created from prototypes defined in spring
 */
public interface ScheduledCombatMessagesFactory
{
	/**
	 * @return Message about one scheduled combat that we're involved in and still needs to be played
	 */
	public ScheduledCombatMessageCombat createScheduledCombatMessageCombat ();

	/**
	 * @return Message about how many scheduled combats there are still left to play that we aren't involved in
	 */
	public ScheduledCombatMessageOther createScheduledCombatMessageOther ();
	
	/**
	 * @return Scheduled combat message category with injected dependencies
	 */
	public ScheduledCombatMessageCategory createScheduledCombatMessageCategory ();
}