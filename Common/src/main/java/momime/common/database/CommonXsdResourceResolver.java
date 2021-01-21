package momime.common.database;

import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import com.ndg.map.MapConstants;
import com.ndg.multiplayer.session.MultiplayerSessionXsdResourceResolver;

/**
 * Resolver to allow finding Multiplayer, Map and common MoM IME XSDs files when it is referenced as an xsd:import statement from other XSDs
 */
public class CommonXsdResourceResolver extends MultiplayerSessionXsdResourceResolver
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (CommonXsdResourceResolver.class);
	
	/**
	 * Creates a resolver to allow finding referenced MoM IME XSD files
	 * @param aDomImplRegistry DOM Implementation registry used to find the load/save implementation
	 */
	public CommonXsdResourceResolver (final DOMImplementationRegistry aDomImplRegistry)
	{
		super (aDomImplRegistry);
	}

	/**
	 * Checks to see if the requested resource is a known MoM IME XSD file, returning it as an input stream
	 * @param type Type of resource to locate - expect this to be "http://www.w3.org/2001/XMLSchema"
	 * @param namespaceURI Namespace of the resource to locate - which is what we used to locate it
	 * @param publicId The public identifier of the external entity being referenced (in this case, null)
	 * @param systemId The location suggested for the resource (e.g. the schemaLocation specified for the Server XSD in the Language XSD)
	 * @param baseURI The absolute base URI of the resource being parsed (in this case, null)
	 * @return Stream to the located resource
	 */
	@SuppressWarnings ("resource")
	@Override
	protected InputStream resolveResourceToStream (final String type, final String namespaceURI, final String publicId, final String systemId, final String baseURI)
	{
		final InputStream result;

		// Map XSD
		if ((type.equals (XSD_URI)) && (namespaceURI.equals (MapConstants.MAP_XSD_NAMESPACE_URI)))
		{
			log.debug ("Namespace matches map XSD");
			result = getClass ().getResourceAsStream (MapConstants.MAP_XSD_LOCATION);
		}

		// Common XSD
		else if ((type.equals (XSD_URI)) && (namespaceURI.equals (CommonDatabaseConstants.COMMON_XSD_NAMESPACE_URI)))
		{
			log.debug ("Namespace matches common XSD");
			result = getClass ().getResourceAsStream (CommonDatabaseConstants.COMMON_XSD_LOCATION);
		}

		// Client XSD
		else if ((type.equals (XSD_URI)) && (namespaceURI.equals (CommonDatabaseConstants.CLIENT_XSD_NAMESPACE_URI)))
		{
			log.debug ("Namespace matches client XSD");
			result = getClass ().getResourceAsStream (CommonDatabaseConstants.CLIENT_XSD_LOCATION);
		}

		// Messages XSD
		else if ((type.equals (XSD_URI)) && (namespaceURI.equals (CommonDatabaseConstants.MESSAGES_XSD_NAMESPACE_URI)))
		{
			log.debug ("Namespace matches messages XSD");
			result = getClass ().getResourceAsStream (CommonDatabaseConstants.MESSAGES_XSD_LOCATION);
		}

		// Client-to-server messages XSD
		else if ((type.equals (XSD_URI)) && (namespaceURI.equals (CommonDatabaseConstants.CTOS_MESSAGES_XSD_NAMESPACE_URI)))
		{
			log.debug ("Namespace matches client-to-server-messages XSD");
			result = getClass ().getResourceAsStream (CommonDatabaseConstants.CTOS_MESSAGES_XSD_LOCATION);
		}

		// Server-to-client messages XSD
		else if ((type.equals (XSD_URI)) && (namespaceURI.equals (CommonDatabaseConstants.STOC_MESSAGES_XSD_NAMESPACE_URI)))
		{
			log.debug ("Namespace matches server-to-client-messages XSD");
			result = getClass ().getResourceAsStream (CommonDatabaseConstants.STOC_MESSAGES_XSD_LOCATION);
		}
		else
			result = super.resolveResourceToStream (type, namespaceURI, publicId, systemId, baseURI);

		return result;
	}
}