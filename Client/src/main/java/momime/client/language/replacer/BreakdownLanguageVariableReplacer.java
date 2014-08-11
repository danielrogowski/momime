package momime.client.language.replacer;

/**
 * Language variable replacers typically work based on some breakdown object, and need text utils + the language XML
 * so this handles all that common functionality so it doesn't need to be repeated.
 * 
 * @param <B> Class of breakdown object
 */
public interface BreakdownLanguageVariableReplacer<B> extends LanguageVariableReplacer
{
	/**
	 * @param obj Overall breakdown
	 */
	public void setBreakdown (final B obj);
}