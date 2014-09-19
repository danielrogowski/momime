package momime.server.database;

import java.io.InputStream;

import momime.common.database.CommonXsdResourceResolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

/**
 * Resolver to allow finding Server IME XSD file when it is referenced as an xsd:import statement from other XSDs
 */
public final class ServerXsdResourceResolver extends CommonXsdResourceResolver
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ServerXsdResourceResolver.class);
	
	/**
	 * Creates a resolver to allow finding referenced MoM IME XSD files
	 * @param aDomImplRegistry DOM Implementation registry used to find the load/save implementation
	 */
	public ServerXsdResourceResolver (final DOMImplementationRegistry aDomImplRegistry)
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
	protected final InputStream resolveResourceToStream (final String type, final String namespaceURI, final String publicId, final String systemId, final String baseURI)
	{
		log.trace ("Entering resolveResourceToStream: " +
			type + ", " + namespaceURI + ", " + publicId + ", " + systemId + ", " + baseURI);

		final InputStream result;

		// Server XSD
		if ((type.equals (XSD_URI)) && (namespaceURI.equals (ServerDatabaseConstants.SERVER_XSD_NAMESPACE_URI)))
		{
			log.debug ("Namespace matches server XSD");
			result = getClass ().getResourceAsStream (ServerDatabaseConstants.SERVER_XSD_LOCATION);
		}
		else
			result = super.resolveResourceToStream (type, namespaceURI, publicId, systemId, baseURI);

		log.trace ("Exiting resolveResourceToStream = " + result);
		return result;
	}
}