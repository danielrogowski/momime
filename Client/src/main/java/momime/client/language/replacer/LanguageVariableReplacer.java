package momime.client.language.replacer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Takes a line of text from the language XML file that contains variables and replaces the variables with appropriate text
 */
public abstract class LanguageVariableReplacer
{
	/** Class logger */
	private final Log log = LogFactory.getLog (LanguageVariableReplacer.class);
	
	/**
	 * @param c Char to test 
	 * @return True if this char is used for replacable codes
	 */
	boolean isCodeChar (final char c)
	{
		// Spaces, any other punctuation, and lower case letters all signify breaks in codes, notably this includes +-%
		return (c == '_') || ((c >= 'A') && (c <= 'Z')) || ((c >= '0') && (c <= '9')); 
	}
	
	/**
	 * @param code Code to replace
	 * @return Replacement value; or null if the code is not recognized
	 */
	protected abstract String determineVariableValue (final String code);

	/**
	 * @param description Text containing codes to replace
	 * @return Input text, with codes replaced accordingly
	 */
	public final String replaceVariables (final String description)
	{
		log.trace ("Entering replaceVariables: " + description);
		
		String text = description;
		
		int codeStart = text.indexOf ('_');
		while (codeStart >= 0)
		{
			// Scan to the end of the code
			int codeEnd = codeStart+1;
			while ((codeEnd < text.length ()) && (isCodeChar (text.charAt (codeEnd))))
				codeEnd++;
			
			// Scan to the start of the code
			while ((codeStart > 0) && (isCodeChar (text.charAt (codeStart-1))))
				codeStart--;
			
			// Read out the code
			final String code = text.substring (codeStart, codeEnd);
			
			// Find the value to replace the code with
			String replacement = determineVariableValue (code);
			if (replacement == null)
			{
				log.warn (getClass ().getName () + " doesn't know what to replace code \"" + code + "\" with");
				replacement = "";
			}
			
			// Perform the replacement
			text = text.substring (0, codeStart) + replacement + text.substring (codeEnd);
			
			// Search for more codes
			codeStart = text.indexOf ('_');
		}
		
		log.trace ("Exiting replaceVariables = " + text);
		return text;
	}

	/**
	 * @param text Text buffer to add to
	 * @param line Text to add, containing codes to replace
	 */
	public final void addLine (final StringBuilder text, final String line)
	{
		if (text.length () > 0)
			text.append ("\r\n");
		
		if (line != null)
			text.append (replaceVariables (line));
	}
}