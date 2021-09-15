package momime.client.language.database;

import momime.client.languages.database.LanguageOption;
import momime.client.languages.database.MomLanguages;
import momime.client.languages.database.ObjectFactory;
import momime.client.languages.database.SpellTargeting;

/**
 * Creates our custom extended MomLanguages when it is unmarshalled with JAXB
 */
public final class MomLanguagesFactory extends ObjectFactory
{
	/**
	 * @return Custom extended MomLanguages 
	 */
	@Override
	public final MomLanguages createMomLanguages ()
	{
		return new MomLanguagesExImpl ();
	}

	/**
	 * @return Custom extended LanguageOption 
	 */
	@Override
	public final LanguageOption createLanguageOption ()
	{
		return new LanguageOptionEx ();
	}

	/**
	 * @return Custom extended SpellTargeting 
	 */
	@Override
	public final SpellTargeting createSpellTargeting ()
	{
		return new SpellTargetingEx ();
	}
}