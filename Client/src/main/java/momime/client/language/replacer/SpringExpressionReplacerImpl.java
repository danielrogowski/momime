package momime.client.language.replacer;

import java.io.IOException;
import java.net.URL;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Replacer that finds EL expressions #{..} and evaluates them
 */
public final class SpringExpressionReplacerImpl extends LanguageVariableReplacerImpl implements SpringExpressionReplacer
{
	/** Context to use when evaluating expressions */ 
	private EvaluationContext evaluationContext;
	
	/** Whether results of EL expressions should be treated as classpath resources */
	private boolean classpathResource;
	
	/** Whether results of EL expressions should be wrapped in HTML img tags */
	private boolean htmlImage;
	
	/**
	 * @param description Text containing codes to replace
	 * @return Start and end positions of a code that requires replacing, or null if no replacement codes are present in the input string
	 */
	@Override
	public final LanguageVariableReplacerCodePosition findCode (final String description)
	{
		final LanguageVariableReplacerCodePosition position;
		final int codeStart = description.indexOf ("#{");
		if (codeStart < 0)
			position = null;
		else
		{
			// Scan to the end of the code
			final int codeEnd = description.indexOf ("}", codeStart);
			if (codeEnd < 0)
				position = null;
			else
				position = new LanguageVariableReplacerCodePosition (codeStart, codeEnd + 1);
		}

		return position;
	}
	
	/**
	 * @param code Code to replace
	 * @return Replacement value; or null if the code is not recognized
	 * @throws IOException If there is a problem evaluating the EL expression
	 */
	@Override
	public final String determineVariableValue (final String code) throws IOException
	{
		// Strip off the #{..} container
		final String expression = code.substring (2, code.length () - 1);
		
		// Evaluate it
		ExpressionParser parser = new SpelExpressionParser ();
		String value;
		try
		{
			value = parser.parseExpression (expression).getValue (getEvaluationContext (), String.class);
			
			if ((value != null) && (isClasspathResource ()))
			{
				final URL resource = getClass ().getResource (value);
				value = (resource == null) ? null : resource.toString ();
			}
			
			if ((value != null) && (isHtmlImage ()))
				value = "<img src=\"" + value + "\"/>";
		}
		catch (final ExpressionException e)
		{
			throw new IOException ("Error evaluating expression " + code, e);
		}

		return value;
	}

	/**
	 * @return Context to use when evaluating expressions
	 */ 
	@Override
	public final EvaluationContext getEvaluationContext ()
	{
		return evaluationContext;
	}

	/**
	 * @param context Context to use when evaluating expressions
	 */
	@Override
	public final void setEvaluationContext (final EvaluationContext context)
	{
		evaluationContext = context;
	}

	/**
	 * @return Whether results of EL expressions should be treated as classpath resources
	 */
	public final boolean isClasspathResource ()
	{
		return classpathResource;
	}

	/**
	 * @param cpr Whether results of EL expressions should be treated as classpath resources
	 */
	public final void setClasspathResource (final boolean cpr)
	{
		classpathResource = cpr;
	}
	
	/**
	 * @return Whether results of EL expressions should be wrapped in HTML img tags
	 */
	public final boolean isHtmlImage ()
	{
		return htmlImage;
	}
	
	/**
	 * @param img Whether results of EL expressions should be wrapped in HTML img tags
	 */
	public final void setHtmlImage (final boolean img)
	{
		htmlImage = img;
	}
}