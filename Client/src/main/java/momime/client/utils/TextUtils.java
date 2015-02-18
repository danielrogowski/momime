package momime.client.utils;

/**
 * Utils for manipulating text strings, typically related to formatting text for display in the UI
 */
public interface TextUtils
{
	/**
	 * @param n Number to convert
	 * @return Number with digit grouping, e.g. 1,234,567
	 */
	public String intToStrCommas (final int n);
	
	/**
	 * @param n Number to convert
	 * @return Number which always includes a plus or minus sign (unless 0), e.g. -15 or 0 or +12
	 */
	public String intToStrPlusMinus (final int n);

	/**
	 * @param n Double the number to convert
	 * @return String representation of number
	 */
	public String halfIntToStr (final int n);

	/**
	 * @param n Double the number to convert
	 * @return String representation of number which always includes a plus or minus sign
	 */
	public String halfIntToStrPlusMinus (final int n);
	
	/**
	 * @param n Number to convert
	 * @param dp Decimal places
	 * @return Number right-shifted dp times, so e.g. insertDecimalPoint (45, 3) = "0.045"
	 */
	public String insertDecimalPoint (final int n, final int dp);

	/**
	 * @param s Comma-space delimited list "like, this, example"
	 * @return Input string, with final space converted to an and, "like, this and example"
	 */
	public String replaceFinalCommaByAnd (final String s);
}