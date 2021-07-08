package momime.client.calculations.damage;

import java.io.IOException;
import java.util.List;

import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.common.database.LanguageText;
import momime.common.messages.servertoclient.DamageCalculationWallData;

/**
 * Breakdown about a roll made with Wall Crusher skill
 */
public final class DamageCalculationWallDataEx extends DamageCalculationWallData implements DamageCalculationText
{
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/**
	 * There's no units, players, or anything else to look up, so nothing to do here.
	 */
	@Override
	public final void preProcess () throws IOException
	{
	}

	/**
	 * @return Text to display for this breakdown line
	 * @throws IOException If there is a problem
	 */
	@Override
	public final String getText () throws IOException
	{
		final List<LanguageText> languageText = isWrecked () ? getLanguages ().getCombatDamage ().getWallWrecked () : getLanguages ().getCombatDamage ().getWallFailed ();
		
		return "     " + getLanguageHolder ().findDescription (languageText).replaceAll
			("PERCENTAGE", Integer.valueOf (100 / getWreckTileChance ()).toString ());
	}

	/**
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
	}
}