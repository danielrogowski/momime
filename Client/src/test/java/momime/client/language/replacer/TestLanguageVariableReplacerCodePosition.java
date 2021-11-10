package momime.client.language.replacer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * Tests the LanguageVariableReplacerCodePosition class
 */
public final class TestLanguageVariableReplacerCodePosition
{
	/**
	 * Tests the toString method
	 */
	@Test
	public final void testToString ()
	{
		assertEquals ("[5, 12)", new LanguageVariableReplacerCodePosition (5, 12).toString ());
	}
	
	/**
	 * Tests the equals method on the same object
	 */
	@Test
	public final void testEquals_Same ()
	{
		final LanguageVariableReplacerCodePosition o = new LanguageVariableReplacerCodePosition (5, 12);
		assertTrue (o.equals (o));
	}

	/**
	 * Tests the equals method on a null
	 */
	@Test
	public final void testEquals_Null ()
	{
		final LanguageVariableReplacerCodePosition o = new LanguageVariableReplacerCodePosition (5, 12);
		assertFalse (o.equals (null));
	}

	/**
	 * Tests the equals method on the wrong type of object
	 */
	@Test
	public final void testEquals_WrongType ()
	{
		final LanguageVariableReplacerCodePosition o1 = new LanguageVariableReplacerCodePosition (5, 12);
		final Object o2 = new Object ();
		assertFalse (o1.equals (o2));
	}

	/**
	 * Tests the equals method on two equal objects
	 */
	@Test
	public final void testEquals_Equal ()
	{
		final LanguageVariableReplacerCodePosition o1 = new LanguageVariableReplacerCodePosition (5, 12);
		final LanguageVariableReplacerCodePosition o2 = new LanguageVariableReplacerCodePosition (5, 12);
		assertTrue (o1.equals (o2));
	}

	/**
	 * Tests the equals method on two unequal objects
	 */
	@Test
	public final void testEquals_NotEqual ()
	{
		final LanguageVariableReplacerCodePosition o1 = new LanguageVariableReplacerCodePosition (5, 12);
		final LanguageVariableReplacerCodePosition o2 = new LanguageVariableReplacerCodePosition (6, 12);
		assertFalse (o1.equals (o2));
	}
}