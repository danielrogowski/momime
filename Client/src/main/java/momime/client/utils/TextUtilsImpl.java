package momime.client.utils;

import java.text.DecimalFormat;

/**
 * Utils for manipulating text strings, typically related to formatting text for display in the UI
 */
public final class TextUtilsImpl implements TextUtils
{
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
}
