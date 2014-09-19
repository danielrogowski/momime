package momime.assembly;

/**
 * Constants regarding locating files used by the unit tests in the assembly project
 */
public final class AssemblyConstants
{
	/** Location on classpath of unpacked server XML */
	public final static String SERVER_XML_LOCATION = "/server/databases/Original Master of Magic 1.31 rules.Master of Magic Server.xml";
	
	/** Location on classpath of unpacked graphics XML */
	public final static String GRAPHICS_XML_LOCATION = "/client/graphics/Default.Master of Magic Graphics.xml";
	
	/** Location on classpath of unpacked English language XML */
	public final static String ENGLISH_XML_LOCATION = "/client/languages/English.Master of Magic Language.xml";
	
	/**
	 * Prevent instantiation
	 */
	private AssemblyConstants ()
	{
	}
}