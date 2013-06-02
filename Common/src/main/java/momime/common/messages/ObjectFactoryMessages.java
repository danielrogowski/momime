package momime.common.messages;

import momime.common.messages.v0_9_4.CombatMapCoordinates;
import momime.common.messages.v0_9_4.ObjectFactory;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;

/**
 * Create extended versions of coordinates objects
 */
public final class ObjectFactoryMessages extends ObjectFactory
{
	/**
	 * @return Newly created OverlandMapCoordinates
	 */
	@Override
	public final OverlandMapCoordinates createOverlandMapCoordinates ()
	{
		return new OverlandMapCoordinatesEx ();
	}

	/**
	 * @return Newly created CombatMapCoordinates
	 */
	@Override
	public final CombatMapCoordinates createCombatMapCoordinates ()
	{
		return new CombatMapCoordinatesEx ();
	}
}
