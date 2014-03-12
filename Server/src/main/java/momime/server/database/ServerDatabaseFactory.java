package momime.server.database;

import javax.xml.bind.annotation.XmlRegistry;

import momime.server.database.v0_9_4.ObjectFactory;
import momime.server.database.v0_9_4.ServerDatabase;

/**
 * Creates our custom extended ServerDatabase when it is unmarshalled with JAXB
 */
@XmlRegistry
public final class ServerDatabaseFactory extends ObjectFactory
{
	/**
	 * @return Creates our custom extended ServerDatabase 
	 */
	@Override
	public final ServerDatabase createServerDatabase ()
	{
		return new ServerDatabaseExImpl ();
	}
}
