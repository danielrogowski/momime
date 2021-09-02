package momime.client.newturnmessages;

import momime.common.messages.NewTurnMessageBuildingSoldFromLackOfProduction;
import momime.common.messages.NewTurnMessageConstructBuilding;
import momime.common.messages.NewTurnMessageConstructUnit;
import momime.common.messages.NewTurnMessageCreateArtifact;
import momime.common.messages.NewTurnMessageDestroyBuilding;
import momime.common.messages.NewTurnMessageHeroGainedALevel;
import momime.common.messages.NewTurnMessageNode;
import momime.common.messages.NewTurnMessageOfferHero;
import momime.common.messages.NewTurnMessageOfferItem;
import momime.common.messages.NewTurnMessageOfferUnits;
import momime.common.messages.NewTurnMessagePopulationChange;
import momime.common.messages.NewTurnMessageSpell;
import momime.common.messages.NewTurnMessageSpellSwitchedOffFromLackOfProduction;
import momime.common.messages.NewTurnMessageSummonUnit;
import momime.common.messages.NewTurnMessageUnitKilledFromLackOfProduction;
import momime.common.messages.ObjectFactory;

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
	public final NewTurnMessageDestroyBuilding createNewTurnMessageDestroyBuilding ()
	{
		return getNewTurnMessagesFactory ().createNewTurnMessageDestroyBuilding ();
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
	public final NewTurnMessageCreateArtifact createNewTurnMessageCreateArtifact ()
	{
		return getNewTurnMessagesFactory ().createNewTurnMessageCreateArtifact ();
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
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageHeroGainedALevel createNewTurnMessageHeroGainedALevel ()
	{
		return getNewTurnMessagesFactory ().createNewTurnMessageHeroGainedALevel ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageOfferHero createNewTurnMessageOfferHero ()
	{
		return getNewTurnMessagesFactory ().createNewTurnMessageOfferHero ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageOfferUnits createNewTurnMessageOfferUnits ()
	{
		return getNewTurnMessagesFactory ().createNewTurnMessageOfferUnits ();
	}

	/**
	 * @return Custom extended NTM
	 */
	@Override
	public final NewTurnMessageOfferItem createNewTurnMessageOfferItem ()
	{
		return getNewTurnMessagesFactory ().createNewTurnMessageOfferItem ();
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