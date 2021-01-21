package momime.client.language.replacer;

/**
 * Processes standard tokens that consist of upper case letters, numbers and at least one underscore
 */
public abstract class LanguageVariableReplacerTokenImpl extends LanguageVariableReplacerImpl
{
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
	 * @param description Text containing codes to replace
	 * @return Start and end positions of a code that requires replacing, or null if no replacement codes are present in the input string
	 */
	@Override
	public final LanguageVariableReplacerCodePosition findCode (final String description)
	{
		final LanguageVariableReplacerCodePosition position;
		int codeStart = description.indexOf ('_');
		if (codeStart < 0)
			position = null;
		else
		{
			// Scan to the end of the code
			int codeEnd = codeStart+1;
			while ((codeEnd < description.length ()) && (isCodeChar (description.charAt (codeEnd))))
				codeEnd++;
			
			// Scan to the start of the code
			while ((codeStart > 0) && (isCodeChar (description.charAt (codeStart-1))))
				codeStart--;
			
			position = new LanguageVariableReplacerCodePosition (codeStart, codeEnd);
		}

		return position;
	}
}