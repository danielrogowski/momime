package momime.server.messages.process;

import javax.xml.bind.annotation.XmlRegistry;

import momime.common.messages.clienttoserver.v0_9_4.AlchemyMessage;
import momime.common.messages.clienttoserver.v0_9_4.ChangeCityConstructionMessage;
import momime.common.messages.clienttoserver.v0_9_4.ChangeOptionalFarmersMessage;
import momime.common.messages.clienttoserver.v0_9_4.ChooseCityNameMessage;
import momime.common.messages.clienttoserver.v0_9_4.ChooseCustomFlagColourMessage;
import momime.common.messages.clienttoserver.v0_9_4.ChooseCustomPicksMessage;
import momime.common.messages.clienttoserver.v0_9_4.ChooseInitialSpellsMessage;
import momime.common.messages.clienttoserver.v0_9_4.ChooseRaceMessage;
import momime.common.messages.clienttoserver.v0_9_4.ChooseStandardPhotoMessage;
import momime.common.messages.clienttoserver.v0_9_4.ChooseWizardMessage;
import momime.common.messages.clienttoserver.v0_9_4.ObjectFactory;
import momime.common.messages.clienttoserver.v0_9_4.RequestOverlandMovementDistancesMessage;
import momime.common.messages.clienttoserver.v0_9_4.UpdateMagicPowerDistributionMessage;
import momime.common.messages.clienttoserver.v0_9_4.UploadCustomPhotoMessage;

/**
 * On the server, make sure we create the special versions of each message that include the processing interface
 */
@XmlRegistry
public final class ObjectFactoryClientToServerMessages extends ObjectFactory
{
	/**
	 * @return Newly created ChooseWizardMessage
	 */
	@Override
	public final ChooseWizardMessage createChooseWizardMessage ()
	{
        return new ChooseWizardMessageImpl ();
	}

	/**
	 * @return Newly created UploadCustomPhotoMessage
	 */
	@Override
	public final UploadCustomPhotoMessage createUploadCustomPhotoMessage ()
	{
		return new UploadCustomPhotoMessageImpl ();
	}

	/**
	 * @return Newly created ChooseInitialSpellsMessage
	 */
	@Override
	public final ChooseInitialSpellsMessage createChooseInitialSpellsMessage ()
	{
		return new ChooseInitialSpellsMessageImpl ();
	}

	/**
	 * @return Newly created ChooseCityNameMessage
	 */
	@Override
	public final ChooseCityNameMessage createChooseCityNameMessage ()
	{
		return new ChooseCityNameMessageImpl ();
	}

	/**
	 * @return Newly created ChooseStandardPhotoMessage
	 */
	@Override
	public final ChooseStandardPhotoMessage createChooseStandardPhotoMessage ()
	{
		return new ChooseStandardPhotoMessageImpl ();
	}

	/**
	 * @return Newly created ChooseCustomFlagColourMessage
	 */
	@Override
	public final ChooseCustomFlagColourMessage createChooseCustomFlagColourMessage ()
	{
		return new ChooseCustomFlagColourMessageImpl ();
	}

	/**
	 * @return Newly created ChooseCustomPicksMessage
	 */
	@Override
	public final ChooseCustomPicksMessage createChooseCustomPicksMessage ()
	{
		return new ChooseCustomPicksMessageImpl ();
	}

	/**
	 * @return Newly created ChooseRaceMessage
	 */
	@Override
	public final ChooseRaceMessage createChooseRaceMessage ()
	{
		return new ChooseRaceMessageImpl ();
	}

	/**
	 * @return Newly created ChangeCityConstructionMessage
	 */
	@Override
	public final ChangeCityConstructionMessage createChangeCityConstructionMessage ()
	{
		return new ChangeCityConstructionMessageImpl ();
	}

	/**
	 * @return Newly created ChangeOptionalFarmersMessage
	 */
	@Override
	public final ChangeOptionalFarmersMessage createChangeOptionalFarmersMessage ()
	{
		return new ChangeOptionalFarmersMessageImpl ();
	}

	/**
	 * @return Newly created UpdateMagicPowerDistributionMessage
	 */
	@Override
	public final UpdateMagicPowerDistributionMessage createUpdateMagicPowerDistributionMessage ()
	{
		return new UpdateMagicPowerDistributionMessageImpl ();
	}

	/**
	 * @return Newly created AlchemyMessage
	 */
	@Override
	public final AlchemyMessage createAlchemyMessage ()
	{
		return new AlchemyMessageImpl ();
	}

	/**
	 * @return Newly created RequestOverlandMovementDistancesMessage
	 */
	@Override
	public final RequestOverlandMovementDistancesMessage createRequestOverlandMovementDistancesMessage ()
	{
		return new RequestOverlandMovementDistancesMessageImpl ();
	}
}
