package momime.common.utils;

/**
 * Methods for working with wizardIDs
 */
public interface PlayerKnowledgeUtils
{
	/**
	 * This is derived from getWizardID, and just avoids external reliance on the constants used for certain special values
	 * @param wizardID Wizard ID of player to check
	 * @return True if this player is a wizard (human or AI); false if its the special Raiders or Monsters player
	 */
	public boolean isWizard (final String wizardID);

	/**
	 * This is derived from getWizardID, and just avoids external reliance on the constants used for certain special values
	 * @param wizardID Wizard ID of player to check
	 * @return True if this player is a custom wizard (rather than one of the pre-defined ones)
	 */
	public boolean isCustomWizard (final String wizardID);
}