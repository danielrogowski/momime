package momime.client.utils;

import java.text.DecimalFormat;

/**
 * Utils for manipulating text strings, typically related to formatting text for display in the UI
 */
public final class TextUtilsImpl implements TextUtils
{
	/** Unicode for a 1/2 symbol */
	final static char HALF = '\u00BD';
	
	/** Format used by the intToStrCommas method */
	private final static DecimalFormat commas = new DecimalFormat ("#,###");

	/**
	 * @param n Number to convert
	 * @return Number with digit grouping, e.g. 1,234,567
	 */
	@Override
	public final String intToStrCommas (final int n)
	{
		return commas.format (n);
	}
	
	/**
	 * @param n Number to convert
	 * @return Number which always includes a plus sign (unless 0), e.g. -15 or 0 or +12
	 */
	@Override
	public final String intToStrPlusMinus (final int n)
	{
		final String s = new Integer (n).toString ();
		return (n > 0) ? "+" + s : s;
	}

	/**
	 * @param n Double the number to convert
	 * @return String representation of number
	 */
	@Override
	public final String halfIntToStr (final int n)
	{
		String s;
		
		if (n == 1)
			s = "";
		else if (n == -1)
			s = "-";
		else
			s = new Integer (n / 2).toString ();		
		
		if (n % 2 != 0)
			s = s + HALF;
		
		return s;
	}
}
