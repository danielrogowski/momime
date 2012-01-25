package momime.server.config;

import momime.common.database.CommonDatabaseConstants;

/**
 * Constants describing the server config XSD and XML file containing system paths
 */
public final class ServerConfigConstants
{
	/** Path and name to locate the server XSD file on the classpath */
	public static final String CONFIG_XSD_LOCATION = "/momime.server.config/MoMIMEServerConfig.xsd";

	/** Namespace of the server XSD */
	public static final String CONFIG_XSD_NAMESPACE_URI = "http://momime/server/config/" + CommonDatabaseConstants.MOM_IME_VERSION;

	/** Filename of the config XML */
	public static final String CONFIG_XML_LOCATION = "MoMIMEServerConfig.xml";

	/**
	 * Prevent instatiation of this class
	 */
	private ServerConfigConstants ()
	{
	}
}
