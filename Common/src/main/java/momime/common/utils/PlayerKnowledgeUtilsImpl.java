package momime.common.utils;

import momime.common.database.CommonDatabaseConstants;

/**
 * Methods for working with wizardIDs.  A wizardID of null means a human player chose a custom wizard.
 * A player who has not yet chosen which wizard to be will not have a knownWizardDetails record so that is N/A for all the methods here, as they all take
 * a wizardID input which already means the knownWizardDetails record must exist.
 */
public final class PlayerKnowledgeUtilsImpl implements PlayerKnowledgeUtils
{
	/**
	 * This is derived from getWizardID, and just avoids external reliance on the constants used for certain special values
	 * @param wizardID Wizard ID of player to check
	 * @return True if this player is a wizard (human or AI); false if its the special Raiders or Monsters player
	 */
	@Override
	public final boolean isWizard (final String wizardID)
	{
		boolean result;

		if (wizardID == null)
			result = true;		// Custom wizard
		else
			result = ((!wizardID.equals (CommonDatabaseConstants.WIZARD_ID_RAIDERS)) &&
						(!wizardID.equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS)));

		return result;
	}

	/**
	 * This is derived from getWizardID, and just avoids external reliance on the constants used for certain special values
	 * @param wizardID Wizard ID of player to check
	 * @return True if this player is a custom wizard (rather than one of the pre-defined ones)
	 */
	@Override
	public final boolean isCustomWizard (final String wizardID)
	{
		return (wizardID == null);
	}
}