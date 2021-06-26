package momime.client.resourceconversion;

/**
 * Correct image and offset for flag
 */
final class FlagCheckResult
{
	/** Flag image */
	private final String flagImage;
	
	/** Flag offset x */
	private final int flagOffsetX;
	
	/** Flag offset y */
	private final int flagOffsetY;
	
	/**
	 * @param aFlagImage Flag image
	 * @param aFlagOffsetX Flag offset x
	 * @param aFlagOffsetY Flag offset y
	 */
	FlagCheckResult (final String aFlagImage, final int aFlagOffsetX, final int aFlagOffsetY)
	{
		flagImage = aFlagImage;
		flagOffsetX = aFlagOffsetX;
		flagOffsetY = aFlagOffsetY;
	}

	/**
	 * @return Flag image
	 */
	final String getFlagImage ()
	{
		return flagImage;
	}
	
	/**
	 * @return Flag offset x
	 */
	final int getFlagOffsetX ()
	{
		return flagOffsetX;
	}
	
	/**
	 * @return Flag offset y
	 */
	final int getFlagOffsetY ()
	{
		return flagOffsetY;
	}
}