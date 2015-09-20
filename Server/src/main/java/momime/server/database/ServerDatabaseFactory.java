package momime.server.database;

/**
 * Factory interface for creating server database related objects from prototypes defined in the spring XML
 */
public interface ServerDatabaseFactory
{
	/**
	 * @return Newly created server database
	 */
	public ServerDatabaseExImpl createDatabase ();
}