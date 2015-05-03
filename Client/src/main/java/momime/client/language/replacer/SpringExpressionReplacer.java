package momime.client.language.replacer;

import org.springframework.expression.EvaluationContext;

/**
 * Replacer that finds EL expressions #{..} and evaluates them
 */
public interface SpringExpressionReplacer extends LanguageVariableReplacer
{
	/**
	 * @return Context to use when evaluating expressions
	 */ 
	public EvaluationContext getEvaluationContext ();

	/**
	 * @param context Context to use when evaluating expressions
	 */ 
	public void setEvaluationContext (final EvaluationContext context);
}