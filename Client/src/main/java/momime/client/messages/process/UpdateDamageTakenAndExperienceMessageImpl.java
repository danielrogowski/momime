package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.servertoclient.v0_9_5.UpdateDamageTakenAndExperienceMessage;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.utils.UnitUtils;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

/**
 * Server sends this to to update these values without showing any animations.
 * Used when units heal and gain experience at the start of a turn, and when units gain experience during combat.
 */
public final class UpdateDamageTakenAndExperienceMessageImpl extends UpdateDamageTakenAndExperienceMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (UpdateDamageTakenAndExperienceMessageImpl.class.getName ());

	/** Multiplayer client */
	private MomClient client;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/**
	 * @param sender Connection to the server
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void process (final MultiplayerServerConnection sender)
		throws JAXBException, XMLStreamException, IOException
	{
		log.entering (UpdateDamageTakenAndExperienceMessageImpl.class.getName (), "process", new int [] {getUnitURN (), getDamageTaken (), getExperience ()});

		final MemoryUnit mu = getUnitUtils ().findUnitURN (getUnitURN (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "UpdateDamageTakenAndExperienceMessageImpl");
		
		mu.setDamageTaken (getDamageTaken ());
		
		if (getExperience () >= 0)
			getUnitUtils ().setBasicSkillValue (mu, CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, getExperience ());
		
		log.exiting (UpdateDamageTakenAndExperienceMessageImpl.class.getName (), "process");
	}

	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}
}
