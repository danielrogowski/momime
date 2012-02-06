package momime.common.database;

import java.io.InputStream;
import java.util.logging.Logger;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import com.ndg.map.MapConstants;
import com.ndg.multiplayer.base.MultiplayerBaseConstants;
import com.ndg.multiplayer.session.MultiplayerSessionBaseConstants;

/**
 * Resolver to allow finding Server IME XSD file when it is referenced as an xsd:import statement from other XSDs
 */
public class CommonXsdResourceResolver implements LSResourceResolver
{
	/** Logger to write to debug text file when the debug log is enabled */
	protected final Logger debugLogger;

	/** Full URI of the xsd: namespace */
	protected static final String XSD_URI = "http://www.w3.org/2001/XMLSchema";

	/** DOM Implementation registry used to find the load/save implementation */
	private final DOMImplementationRegistry domImplRegistry;

	/**
	 * Creates a resolver to allow finding referenced MoM IME XSD files
	 * @param aDomImplRegistry DOM Implementation registry used to find the load/save implementation
	 * @param aDebugLogger Logger to write to debug text file when the debug log is enabled
	 */
	public CommonXsdResourceResolver (final DOMImplementationRegistry aDomImplRegistry, final Logger aDebugLogger)
	{
		super ();

		debugLogger = aDebugLogger;
		domImplRegistry = aDomImplRegistry;
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
	protected InputStream resolveResourceToStream (final String type, final String namespaceURI, final String publicId, final String systemId, final String baseURI)
	{
		debugLogger.entering (CommonXsdResourceResolver.class.getName (), "resolveResourceToStream",
			new String [] {type, namespaceURI, publicId, systemId, baseURI});

		final InputStream result;

		// Multiplayer XSD
		if ((type.equals (XSD_URI)) && (namespaceURI.equals (MultiplayerBaseConstants.MULTIPLAYER_BASE_XSD_NAMESPACE_URI)))
		{
			debugLogger.finest ("Namespace matches multiplayer XSD");
			result = getClass ().getResourceAsStream (MultiplayerBaseConstants.MULTIPLAYER_BASE_XSD_LOCATION);
		}

		// Multiplayer session XSD
		else if ((type.equals (XSD_URI)) && (namespaceURI.equals (MultiplayerSessionBaseConstants.MULTIPLAYER_SESSION_BASE_XSD_NAMESPACE_URI)))
		{
			debugLogger.finest ("Namespace matches multiplayer session XSD");
			result = getClass ().getResourceAsStream (MultiplayerSessionBaseConstants.MULTIPLAYER_SESSION_BASE_XSD_LOCATION);
		}

		// Map XSD
		else if ((type.equals (XSD_URI)) && (namespaceURI.equals (MapConstants.MAP_XSD_NAMESPACE_URI)))
		{
			debugLogger.finest ("Namespace matches map XSD");
			result = getClass ().getResourceAsStream (MapConstants.MAP_XSD_LOCATION);
		}

		// New game XSD
		else if ((type.equals (XSD_URI)) && (namespaceURI.equals (CommonDatabaseConstants.NEW_GAME_XSD_NAMESPACE_URI)))
		{
			debugLogger.finest ("Namespace matches new game XSD");
			result = getClass ().getResourceAsStream (CommonDatabaseConstants.NEW_GAME_XSD_LOCATION);
		}

		// Common XSD
		else if ((type.equals (XSD_URI)) && (namespaceURI.equals (CommonDatabaseConstants.COMMON_XSD_NAMESPACE_URI)))
		{
			debugLogger.finest ("Namespace matches common XSD");
			result = getClass ().getResourceAsStream (CommonDatabaseConstants.COMMON_XSD_LOCATION);
		}
		else
			result = null;

		debugLogger.exiting (CommonXsdResourceResolver.class.getName (), "resolveResourceToStream", result);
		return result;
	}

	/**
	 * Checks to see if the requested resource is a known MoM IME XSD file, returning it as an LSInput
	 * @param type Type of resource to locate - expect this to be "http://www.w3.org/2001/XMLSchema"
	 * @param namespaceURI Namespace of the resource to locate - which is what we used to locate it
	 * @param publicId The public identifier of the external entity being referenced (in this case, null)
	 * @param systemId The location suggested for the resource (e.g. the schemaLocation specified for the Server XSD in the Language XSD)
	 * @param baseURI The absolute base URI of the resource being parsed (in this case, null)
	 * @return Located XSD file
	 */
	@Override
	public final LSInput resolveResource (final String type, final String namespaceURI, final String publicId, final String systemId, final String baseURI)
	{
		debugLogger.entering (CommonXsdResourceResolver.class.getName (), "resolveResource",
			new String [] {type, namespaceURI, publicId, systemId, baseURI});

		final InputStream stream = resolveResourceToStream (type, namespaceURI, publicId, systemId, baseURI);

		final LSInput result;
		if (stream == null)
			result = null;
		else
		{
			final DOMImplementationLS domImplementationLS = (DOMImplementationLS) domImplRegistry.getDOMImplementation ("LS");
			result = domImplementationLS.createLSInput ();

			result.setBaseURI (baseURI);
			result.setCertifiedText (false);
			result.setPublicId (publicId);
			result.setSystemId (systemId);
			result.setByteStream (stream);
		}

		debugLogger.exiting (CommonXsdResourceResolver.class.getName (), "resolveResource", result);
		return result;
	}
}
