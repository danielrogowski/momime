package momime.server.database;

import java.util.List;

import momime.server.database.v0_9_7.Wizard;

/**
 * Extended server side wizard class, just for typecasting the pick count list
 */
public final class WizardSvr extends Wizard
{
	/**
	 * @return List of all the possible numbers of picks a wizard can get at the start of the game (0-20)
	 */
	@SuppressWarnings ("unchecked")
	public final List<WizardPickCountSvr> getWizardPickCounts ()
	{
		return (List<WizardPickCountSvr>) (List<?>) getWizardPickCount ();
	}
}