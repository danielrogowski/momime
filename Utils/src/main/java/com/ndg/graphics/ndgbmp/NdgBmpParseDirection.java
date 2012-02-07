package com.ndg.graphics.ndgbmp;

/**
 * The .ndgbmp format allows tracing the pixels of an image via different paths, so that the path that yields the best
 * compression ratio can be found.  This enum lists all the possible paths.
 */
public enum NdgBmpParseDirection
{
	/**
	 * Scan along the top row of pixels from left to right, then along the the 2nd row from left to right, and so on 
	 */
	NDGBMP_PARSE_DIRECTION_ROW_FIRST (0),
	
	/**
	 * Scan along the left column of pixels from top to bottom, then along the 2nd column from top to bottom, and so on
	 */
	NDGBMP_PARSE_DIRECTION_COLUMN_FIRST (1);
	
	/**
	 * The value used in the actual .ndgbmp file to represent this parsing direction
	 */
	private int externalValue;
	
	/**
	 * Creates a new .ndgbmp parse direction enum
	 * @param anExternalValue The value used in the actual .ndgbmp file to represent this parsing direction
	 */
	private NdgBmpParseDirection (int anExternalValue)
	{
		externalValue = anExternalValue;
	}
	
	/**
	 * @return The value used in the actual .ndgbmp file to represent this parsing direction
	 */
	public int convertToExternalValue ()
	{
		return externalValue;
	}
	
	/**
	 * Converts an external parsing direction value from a .ndgbmp file into an enum value 
	 * @param anExternalValue The value used in the actual .ndgbmp file to represent this parsing direction
	 * @return Enum value for the parsing direction
	 * @throws NdgBmpException If the external value is not a valid parsing direction value
	 */
	public static NdgBmpParseDirection convertFromExternalValue (int anExternalValue)
		throws NdgBmpException
	{
		NdgBmpParseDirection result = null;
		
		for (NdgBmpParseDirection direction: NdgBmpParseDirection.values ())
			if (direction.convertToExternalValue () == anExternalValue)
				result = direction;
		
		if (result == null)
			throw new NdgBmpException ("Invalid .ndgbmp parse direction value: " + anExternalValue);
		
		return result;
	}

}
