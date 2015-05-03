package momime.client.language.replacer;

/**
 * Stores the start and end index into a string of a replacer code
 */
public final class LanguageVariableReplacerCodePosition
{
	/** Start position, inclusive */
	private final int codeStart;
	
	/** End position, exclusive */
	private final int codeEnd;
	
	/**
	 * @param aCodeStart Start position, inclusive
	 * @param aCodeEnd End position, exclusive
	 */
	public LanguageVariableReplacerCodePosition (final int aCodeStart, final int aCodeEnd)
	{
		codeStart = aCodeStart;
		codeEnd = aCodeEnd;
	}
	
	/**
	 * @return Start position, inclusive
	 */
	public final int getCodeStart ()
	{
		return codeStart;
	}
	
	/**
	 * @return End position, exclusive
	 */
	public final int getCodeEnd ()
	{
		return codeEnd;
	}
	
	/**
	 * @return String representation of object values
	 */
	@Override
	public final String toString ()
	{
		return "[" + getCodeStart () + ", " + getCodeEnd () + ")";
	}

	/**
	 * @param o Object to compare against
	 * @return Whether o and this object have equal values 
	 */
	@Override
	public final boolean equals (final Object o)
	{
		if (o == this)
			return true;
		
		else if (!(o instanceof LanguageVariableReplacerCodePosition))
			return false;
		
		else
		{
			final LanguageVariableReplacerCodePosition o2 = (LanguageVariableReplacerCodePosition) o;
			return (getCodeStart () == o2.getCodeStart ()) && (getCodeEnd () == o2.getCodeEnd ());
		}
	}
}