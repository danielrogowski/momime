package com.ndg.graphics.ndgbmp;

/**
 * Common data structures and constants used by more than one of the .ndgbmp classes
 */
public class NdgBmpCommon
{
	/** Byte values for "ndgbmp", the file format identifier */
	public static final byte [] NDGBMP_FORMAT_IDENTIFIER = {0x6E, 0x64, 0x67, 0x62, 0x6D, 0x70};

	/** Major version of the current .ndgbmp format */
	public static final int NDGBMP_MAJOR_VERSION = 1;

	/** Minor version of the current .ndgbmp format */
	public static final int NDGBMP_MINOR_VERSION = 0;

	/**
	 * @param b Byte array to test
	 * @return True if the passed in byte array has the same values as NDGBMP_FORMAT_IDENTIFIER
	 */
	public static boolean equalsFormatIdentifier (final byte [] b)
	{
		boolean result;

		if (b == null)
			result = false;

		else if (b.length != NDGBMP_FORMAT_IDENTIFIER.length)
			result = false;

		else
		{
			// Assume true until we find one that doesn't match
			result = true;
			int i = 0;
			while ((result) && (i < NDGBMP_FORMAT_IDENTIFIER.length))
			{
				if (b [i] != NDGBMP_FORMAT_IDENTIFIER [i])
					result = false;
				else
					i++;
			}
		}

		return result;
	}

	/**
	 * @param maximumValue Maximum value we need to fit
	 * @return Number of bits required to store n, i.e. for 255 we want 8, for 256 we want 9
	 */
	public static int bitsNeededToStore (final long maximumValue)
	{
		int result = 0;
		long value = maximumValue;

		while (value > 0)
		{
			result++;
			value = value >> 1;
		}

		return result;
	}
}
