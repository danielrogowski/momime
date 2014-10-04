package momime.client.newturnmessages;

import momime.common.messages.v0_9_5.NewTurnMessageBuildingSoldFromLackOfProduction;
import momime.common.messages.v0_9_5.NewTurnMessageConstructBuilding;
import momime.common.messages.v0_9_5.NewTurnMessageConstructUnit;
import momime.common.messages.v0_9_5.NewTurnMessageNode;
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
	/** Factory for creating NTMs from spring prototypes */
	private NewTurnMessagesFactory newTurnMessagesFactory;
	
	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageSpellSwitchedOffFromLackOfProduction createNewTurnMessageSpellSwitchedOffFromLackOfProduction ()
	{
		return getNewTurnMessagesFactory ().createNewTurnMessageSpellSwitchedOffFromLackOfProduction ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageBuildingSoldFromLackOfProduction createNewTurnMessageBuildingSoldFromLackOfProduction ()
	{
		return getNewTurnMessagesFactory ().createNewTurnMessageBuildingSoldFromLackOfProduction ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageConstructBuilding createNewTurnMessageConstructBuilding ()
	{
		return getNewTurnMessagesFactory ().createNewTurnMessageConstructBuilding ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageConstructUnit createNewTurnMessageConstructUnit ()
	{
		return getNewTurnMessagesFactory ().createNewTurnMessageConstructUnit ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageSummonUnit createNewTurnMessageSummonUnit ()
	{
		return getNewTurnMessagesFactory ().createNewTurnMessageSummonUnit ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessagePopulationChange createNewTurnMessagePopulationChange ()
	{
		return getNewTurnMessagesFactory ().createNewTurnMessagePopulationChange ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageUnitKilledFromLackOfProduction createNewTurnMessageUnitKilledFromLackOfProduction ()
	{
		return getNewTurnMessagesFactory ().createNewTurnMessageUnitKilledFromLackOfProduction ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageSpell createNewTurnMessageSpell ()
	{
		return getNewTurnMessagesFactory ().createNewTurnMessageSpell ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageNode createNewTurnMessageNode ()
	{
		return getNewTurnMessagesFactory ().createNewTurnMessageNode ();
	}

	/**
	 * @return Factory for creating NTMs from spring prototypes
	 */
	public final NewTurnMessagesFactory getNewTurnMessagesFactory ()
	{
		return newTurnMessagesFactory;
	}

	/**
	 * @param fac Factory for creating NTMs from spring prototypes
	 */
	public final void setNewTurnMessagesFactory (final NewTurnMessagesFactory fac)
	{
		newTurnMessagesFactory = fac;
	}
}