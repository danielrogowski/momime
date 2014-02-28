package momime.client.language.database;

import javax.xml.bind.annotation.XmlRegistry;

import momime.client.language.database.v0_9_5.LanguageCategory;
import momime.client.language.database.v0_9_5.LanguageDatabase;
import momime.client.language.database.v0_9_5.ObjectFactory;


/**
 * Creates our custom extended LanguageDatabase when it is unmarshalled with JAXB
 */
@XmlRegistry
public final class LanguageDatabaseFactory extends ObjectFactory
{
	/**
	 * @return Creates our custom extended LanguageDatabase 
	 */
	@Override
	public final LanguageDatabase createLanguageDatabase ()
	{
		return new LanguageDatabaseExImpl ();
	}

	/**
	 * @return Creates our custom extended LanguageCategory 
	 */
	@Override
	public final LanguageCategory createLanguageCategory ()
	{
		return new LanguageCategoryEx ();
	}
}
