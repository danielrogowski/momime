package momime.server.database;

import momime.common.database.CommonDatabaseConstants;

/**
 * Constants describing the server XSD and XML files containing all the core MoM IME rules
 */
public final class ServerDatabaseConstants
{
	/** Path and name to locate the server XSD file on the classpath */
	public static final String SERVER_XSD_LOCATION = "/momime.server.database/MoMIMEServerDatabase.xsd";

	/** Namespace of the server XSD */
	public static final String SERVER_XSD_NAMESPACE_URI = "http://momime/server/database/" + CommonDatabaseConstants.MOM_IME_VERSION;

	/**
	 * Prevent instatiation of this class
	 */
	private ServerDatabaseConstants ()
	{
	}
}
