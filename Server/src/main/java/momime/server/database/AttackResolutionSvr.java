package momime.server.database;

import java.util.List;

import momime.server.database.v0_9_6.AttackResolution;

/**
 * Empty extension, just so that majority of code doesn't need to reference a package that changes between versions
 */
public class AttackResolutionSvr extends AttackResolution
{
	/**
	 * @return List of possible rules by which to resolve attacks based on this attribute
	 */
	@SuppressWarnings ("unchecked")
	public final List<AttackResolutionConditionSvr> getAttackResolutionConditions ()
	{
		return (List<AttackResolutionConditionSvr>) (List<?>) getAttackResolutionCondition ();
	}

	/**
	 * @return List of possible rules by which to resolve attacks based on this attribute
	 */
	@SuppressWarnings ("unchecked")
	public final List<AttackResolutionStepSvr> getAttackResolutionSteps ()
	{
		return (List<AttackResolutionStepSvr>) (List<?>) getAttackResolutionStep ();
	}
}