package momime.client.language.replacer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Tests the SpringExpressionReplacerImpl class
 * 
 * NB. Its extremely difficult to test the classpath resource lookup since the resulting URL will vary greatly depending
 * on the location of the source code during a build or test run, though this is covered by the inline images in TestHelpUI
 */
@ExtendWith(MockitoExtension.class)
public final class TestSpringExpressionReplacerImpl
{
	/**
	 * Tests the findCode method on a string which includes a code
	 */
	@Test
	public final void testFindCode_Found ()
	{
		// Set up object to test
		final SpringExpressionReplacerImpl replacer = new SpringExpressionReplacerImpl ();

		// Run method
		final String text = "Test #{expression(1<x>)} string ${second} end";
		final LanguageVariableReplacerCodePosition position = replacer.findCode (text);
		assertEquals ("#{expression(1<x>)}", text.substring (position.getCodeStart (), position.getCodeEnd ()));
	}
	
	/**
	 * Tests the findCode method on a string which doesn't include a code
	 */
	@Test
	public final void testFindCode_NotFound ()
	{
		// Set up object to test
		final SpringExpressionReplacerImpl replacer = new SpringExpressionReplacerImpl ();

		// Run method
		assertNull (replacer.findCode ("Test FIRST_CODE end"));
	}
	
	/**
	 * Tests the determineVariableValue method, without any resource lookup or img tag post processing
	 * @throws IOException If there is a problem evaluating the EL expression
	 */
	@Test
	public final void testDetermineVariableValue_Raw () throws IOException
	{
		// Set up dummy object to test evaluating spring expressions against
		final DummyRootObject obj = new DummyRootObject ();
		obj.setMyValue (5);
		
		// Set up object to test
		final StandardEvaluationContext context = new StandardEvaluationContext ();
		context.setRootObject (obj);
		
		final SpringExpressionReplacerImpl replacer = new SpringExpressionReplacerImpl ();
		replacer.setEvaluationContext (context);

		// Run method
		assertEquals ("5-xyz.6", replacer.determineVariableValue ("#{myValue + '-' + myMethod('xyz')}"));
	}
	
	/**
	 * Tests the determineVariableValue method, with the result wrapped in an img tag
	 * @throws IOException If there is a problem evaluating the EL expression
	 */
	@Test
	public final void testDetermineVariableValue_HtmlImage () throws IOException
	{
		// Set up dummy object to test evaluating spring expressions against
		final DummyRootObject obj = new DummyRootObject ();
		obj.setMyValue (5);
		
		// Set up object to test
		final StandardEvaluationContext context = new StandardEvaluationContext ();
		context.setRootObject (obj);
		
		final SpringExpressionReplacerImpl replacer = new SpringExpressionReplacerImpl ();
		replacer.setEvaluationContext (context);
		replacer.setHtmlImage (true);

		// Run method
		assertEquals ("<img src=\"5\"/>", replacer.determineVariableValue ("#{myValue}"));
	}
	
	/**
	 * Dummy object to test evaluating spring expressions against  
	 */
	private final class DummyRootObject
	{
		/** Dummy int value */
		private int myValue;

		/**
		 * @param s Dummy string input
		 * @return Value to prove we can invoke methods from EL
		 */
		@SuppressWarnings ("unused")
		public final String myMethod (final String s)
		{
			return s + "." + Integer.valueOf (getMyValue () + 1);
		}
		
		/**
		 * @return Dummy int value
		 */
		public final int getMyValue ()
		{
			return myValue;
		}

		/**
		 * @param value Dummy int value
		 */
		public final void setMyValue (final int value)
		{
			myValue = value;
		}
	}
}