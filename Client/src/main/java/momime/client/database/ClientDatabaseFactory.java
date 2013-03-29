package momime.client.database;

import javax.xml.bind.annotation.XmlRegistry;

import momime.client.database.v0_9_4.ObjectFactory;

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
	public ClientDatabaseEx createClientDatabase ()
	{
		return new ClientDatabaseEx ();
	}
}
