package momime.client.database;

import javax.xml.bind.annotation.XmlRegistry;

import momime.client.database.ClientDatabase;
import momime.client.database.ObjectFactory;

/**
 * Creates our custom extended ServerDatabase when it is unmarshalled with JAXB
 */
@XmlRegistry
public final class ClientDatabaseFactory extends ObjectFactory
{
	/**
	 * @return Creates our custom extended ClientDatabase 
	 */
	@Override
	public final ClientDatabase createClientDatabase ()
	{
		return new ClientDatabaseExImpl ();
	}
}