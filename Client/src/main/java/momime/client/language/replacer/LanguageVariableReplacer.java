package momime.client.language.replacer;

/**
 * Takes a line of text from the language XML file that contains variables and replaces the variables with appropriate text
 */
public interface LanguageVariableReplacer
{
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