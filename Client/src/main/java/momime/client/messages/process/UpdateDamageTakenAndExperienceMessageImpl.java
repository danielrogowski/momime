package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.ui.frames.UnitInfoUI;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.UpdateDamageTakenAndExperienceMessage;
import momime.common.utils.UnitUtils;

/**
 * Server sends this to to update these values without showing any animations.
 * Used when units heal and gain experience at the start of a turn, and when units gain experience during combat.
 */
public final class UpdateDamageTakenAndExperienceMessageImpl extends UpdateDamageTakenAndExperienceMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UpdateDamageTakenAndExperienceMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: Unit URN " + getUnitURN () + ", " + getDamageTaken () + ", " + getExperience ());

		final MemoryUnit mu = getUnitUtils ().findUnitURN (getUnitURN (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "UpdateDamageTakenAndExperienceMessageImpl");
		
		mu.setDamageTaken (getDamageTaken ());

		// Is there a unit info screen open showing this unit?
		final UnitInfoUI ui = getClient ().getUnitInfos ().get (getUnitURN ());

		// Does this unit have an experience value?  (i.e. is it a normal or hero unit, not summoned)
		if (getExperience () >= 0)
			getUnitUtils ().setBasicSkillValue (mu, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, getExperience ());
		
		// Do we have a unit info screen up showing details about this unit?  If so then we need to force the greyed out hearts to redraw,
		// and possibly other attributes as well if its experience level changed
		if (ui != null)
			ui.getUnitInfoPanel ().showUnit (ui.getUnit ());
		
		log.trace ("Exiting start");
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