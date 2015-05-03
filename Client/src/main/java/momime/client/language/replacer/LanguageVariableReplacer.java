package momime.client.language.replacer;

import java.io.IOException;

/**
 * Takes a line of text from the language XML file that contains variables and replaces the variables with appropriate text
 */
public interface LanguageVariableReplacer
{
	/**
	 * @param description Text containing codes to replace
	 * @return Start and end positions of a code that requires replacing, or null if no replacement codes are present in the input string
	 */
	public LanguageVariableReplacerCodePosition findCode (final String description);
	
	/**
	 * @param code Code to replace
	 * @return Replacement value; or null if the code is not recognized
	 * @throws IOException If there is an error calculating a replacement value
	 */
	public String determineVariableValue (final String code) throws IOException;
	
	/**
	 * @param description Text containing codes to replace
	 * @return Input text, with codes replaced accordingly
	 */
	public String replaceVariables (final String description);

	/**
	 * @param text Text buffer to add to
	 * @param line Text to add, containing codes to replace
	 */
	public void addLine (final StringBuilder text, final String line);
}