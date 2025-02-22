package momime.client.utils;

import java.text.DecimalFormat;

import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;

/**
 * Utils for manipulating text strings, typically related to formatting text for display in the UI
 */
public final class TextUtilsImpl implements TextUtils
{
	/** Unicode for a 1/2 symbol */
	final static char HALF = '\u00BD';
	
	/** Format used by the intToStrCommas method */
	private final static DecimalFormat commas = new DecimalFormat ("#,###");

	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
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
	 * @return Number which always includes a plus or minus sign (unless 0), e.g. -15 or 0 or +12
	 */
	@Override
	public final String intToStrPlusMinus (final int n)
	{
		final String s = Integer.valueOf (n).toString ();
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
			s = Integer.valueOf (n / 2).toString ();		
		
		if (n % 2 != 0)
			s = s + HALF;
		
		return s;
	}

	/**
	 * @param n Double the number to convert
	 * @return String representation of number which always includes a plus or minus sign
	 */
	@Override
	public final String halfIntToStrPlusMinus (final int n)
	{
		return ((n > 0) ? "+" : "") + halfIntToStr (n);
	}

	/**
	 * @param n Number to convert
	 * @param dp Decimal places
	 * @return Number right-shifted dp times, so e.g. insertDecimalPoint (45, 3) = "0.045"
	 */
	@Override
	public final String insertDecimalPoint (final int n, final int dp)
	{
		String s = Integer.valueOf (n).toString ();
		while (s.length () < dp+1)
			s = "0" + s;
		
		final int l = s.length () - dp;
		return s.substring (0, l) + "." + s.substring (l);
	}

	/**
	 * @param s Comma-space delimited list "like, this, example"
	 * @return Input string, with final space converted to an and, "like, this and example"
	 */
	@Override
	public final String replaceFinalCommaByAnd (final String s)
	{
		final String result;
		if (s == null)
			result = null;
		else
		{
			final int pos = s.lastIndexOf (", ");
			if (pos < 0)
				result = s;
			else
				result = s.substring (0, pos) + " " + getLanguageHolder ().findDescription (getLanguages ().getSimple ().getAnd ()) + s.substring (pos + 1);
		}
		
		return result;
	}
	
	/**
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
	}
}