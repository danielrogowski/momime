package momime.common.utils;

import momime.common.database.CommonDatabaseConstants;

/**
 * Methods for working with wizardIDs
 */
public final class PlayerKnowledgeUtilsImpl implements PlayerKnowledgeUtils
{
	/**
	 * This is derived from getWizardID, and just avoids external reliance on the constants used for certain special values
	 * @param wizardID Wizard ID of player to check
	 * @return True if this player has picked a wizard (or Custom), false if we're still waiting for them too
	 */
	@Override
	public final boolean hasWizardBeenChosen (final String wizardID)
	{
		return (wizardID != null);
	}

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
			result = true;		// Player who just hasn't chosen which wizard to be yet
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
		return ((wizardID != null) && (wizardID.equals ("")));
	}
}