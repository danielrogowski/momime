package momime.client.newturnmessages;

import momime.common.messages.v0_9_5.NewTurnMessageBuildingSoldFromLackOfProduction;
import momime.common.messages.v0_9_5.NewTurnMessageConstructBuilding;
import momime.common.messages.v0_9_5.NewTurnMessageConstructUnit;
import momime.common.messages.v0_9_5.NewTurnMessageNode;
import momime.common.messages.v0_9_5.NewTurnMessageOverlandEnchantment;
import momime.common.messages.v0_9_5.NewTurnMessagePopulationChange;
import momime.common.messages.v0_9_5.NewTurnMessageSpell;
import momime.common.messages.v0_9_5.NewTurnMessageSpellSwitchedOffFromLackOfProduction;
import momime.common.messages.v0_9_5.NewTurnMessageSummonUnit;
import momime.common.messages.v0_9_5.NewTurnMessageUnitKilledFromLackOfProduction;
import momime.common.messages.v0_9_5.ObjectFactory;

/**
 * Create extended versions of all NTMs that support the client-side interfaces
 */
public final class NewTurnMessagesObjectFactory extends ObjectFactory
{
	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageSpellSwitchedOffFromLackOfProduction createNewTurnMessageSpellSwitchedOffFromLackOfProduction ()
	{
		return new NewTurnMessageSpellSwitchedOffFromLackOfProductionEx ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageBuildingSoldFromLackOfProduction createNewTurnMessageBuildingSoldFromLackOfProduction ()
	{
		return new NewTurnMessageBuildingSoldFromLackOfProductionEx ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageConstructBuilding createNewTurnMessageConstructBuilding ()
	{
		return new NewTurnMessageConstructBuildingEx ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageConstructUnit createNewTurnMessageConstructUnit ()
	{
		return new NewTurnMessageConstructUnitEx ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageSummonUnit createNewTurnMessageSummonUnit ()
	{
		return new NewTurnMessageSummonUnitEx ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageOverlandEnchantment createNewTurnMessageOverlandEnchantment ()
	{
		return new NewTurnMessageOverlandEnchantmentEx ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessagePopulationChange createNewTurnMessagePopulationChange ()
	{
		return new NewTurnMessagePopulationChangeEx ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageUnitKilledFromLackOfProduction createNewTurnMessageUnitKilledFromLackOfProduction ()
	{
		return new NewTurnMessageUnitKilledFromLackOfProductionEx ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageSpell createNewTurnMessageSpell ()
	{
		return new NewTurnMessageSpellEx ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageNode createNewTurnMessageNode ()
	{
		return new NewTurnMessageNodeEx ();
	}
}