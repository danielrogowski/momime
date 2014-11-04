package momime.client.language.replacer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests the LanguageVariableReplacerImpl class
 */
public final class TestLanguageVariableReplacerImpl
{
	/**
	 * Tests the isCodeChar method
	 */
	@Test
	public final void testIsCodeChar ()
	{
		// Set up object to test
		final DummyLanguageVariableReplacer replacer = new DummyLanguageVariableReplacer ();
		
		// Run method
		assertTrue (replacer.isCodeChar ('_'));
		assertTrue (replacer.isCodeChar ('C'));
		assertTrue (replacer.isCodeChar ('5'));
		
		assertFalse (replacer.isCodeChar ('+'));
		assertFalse (replacer.isCodeChar ('%'));
		assertFalse (replacer.isCodeChar ('c'));
	}
	
	/**
	 * Tests the replaceVariables method
	 */
	@Test
	public final void testReplaceVariables ()
	{
		// Set up object to test
		final DummyLanguageVariableReplacer replacer = new DummyLanguageVariableReplacer ();
		
		// Run method
		assertEquals ("This has no codes", replacer.replaceVariables ("This has no codes"));
		assertEquals ("Has Value1 codes Value2 in the Value1 middle", replacer.replaceVariables ("Has CODE_1 codes CODE_2 in the CODE_1 middle"));
		assertEquals ("HasValue1codes%Value2-in the.Value1middle", replacer.replaceVariables ("HasCODE_1codes%CODE_2-in the.CODE_1middle"));
		assertEquals ("Value1.Has codes at the start and end.Value2", replacer.replaceVariables ("CODE_1.Has codes at the start and end.CODE_2"));
		assertEquals ("Has an unknown code", replacer.replaceVariables ("Has an CODE_3unknown code"));
		assertEquals ("Value1", replacer.replaceVariables ("CODE_1"));		// Tests a code being the entire string
	}
	
	/**
	 * Tests the addLine method
	 */
	@Test
	public final void testAddLine ()
	{
		// Set up object to test
		final DummyLanguageVariableReplacer replacer = new DummyLanguageVariableReplacer ();
		final StringBuilder s = new StringBuilder ();
		
		// Run method
		replacer.addLine (s, "First CODE_1 line");
		replacer.addLine (s, "Second CODE_2 line");
		replacer.addLine (s, "Third CODE_3 line");
		
		assertEquals ("First Value1 line" + System.lineSeparator () + "Second Value2 line" + System.lineSeparator () + "Third  line", s.toString ());
	}
	
	/**
	 * Dummy implementation to test with
	 */
	private final class DummyLanguageVariableReplacer extends LanguageVariableReplacerImpl
	{
		/**
		 * @param code Code to replace
		 * @return Replacement value; or null if the code is not recognized
		 */
		@Override
		protected final String determineVariableValue (final String code)
		{
			final String text;
			switch (code)
			{
				case "CODE_1":
					text = "Value1";
					break;

				case "CODE_2":
					text = "Value2";
					break;
					
				default:
					text = null;
			}
			return text;
		}
	}
}