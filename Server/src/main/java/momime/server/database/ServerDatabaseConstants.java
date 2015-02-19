package momime.server.database;

/**
 * Constants describing the server XSD and XML files containing all the core MoM IME rules
 */
public final class ServerDatabaseConstants
{
	/** Version string - used to build the namespaces of the XSD/XML files */
	private static final String MOM_IME_VERSION = "v0_9_6";

	/** Path and name to locate the server XSD file on the classpath */
	public static final String SERVER_XSD_LOCATION = "/momime.server.database/MoMIMEServerDatabase.xsd";

	/** Namespace of the server XSD */
	public static final String SERVER_XSD_NAMESPACE_URI = "http://momime/server/database/" + MOM_IME_VERSION;

	/**
	 * Prevent instatiation of this class
	 */
	private ServerDatabaseConstants ()
	{
	}
}