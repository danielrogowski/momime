package momime.client.language.replacer;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Takes a line of text from the language XML file that contains variables and replaces the variables with appropriate text
 */
public abstract class LanguageVariableReplacerImpl implements LanguageVariableReplacer
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (LanguageVariableReplacerImpl.class);
	
	/**
	 * @param description Text containing codes to replace
	 * @return Input text, with codes replaced accordingly
	 */
	@Override
	public final String replaceVariables (final String description)
	{
		String text = description;
		
		LanguageVariableReplacerCodePosition position = findCode (text);
		while (position != null)
		{
			// Read out the code
			final String code = text.substring (position.getCodeStart (), position.getCodeEnd ());
			
			// Find the value to replace the code with
			String replacement = "";
			try
			{
				replacement = determineVariableValue (code);
				if (replacement == null)
				{
					log.warn (getClass ().getName () + " doesn't know what to replace code \"" + code + "\" with");
					replacement = "";
				}
			}
			catch (final IOException e)
			{
				log.error (getClass ().getName () + " threw an exception while trying to replace code \"" + code + "\"", e);
			}
			
			// Perform the replacement
			text = text.substring (0, position.getCodeStart ()) + replacement + text.substring (position.getCodeEnd ());
			
			// Search for more codes
			position = findCode (text);
		}
		
		return text;
	}

	/**
	 * @param text Text buffer to add to
	 * @param line Text to add, containing codes to replace
	 */
	@Override
	public final void addLine (final StringBuilder text, final String line)
	{
		if (text.length () > 0)
			text.append (System.lineSeparator ());
		
		if (line != null)
			text.append (replaceVariables (line));
	}
}