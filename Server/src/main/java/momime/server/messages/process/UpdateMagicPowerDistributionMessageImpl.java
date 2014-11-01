package momime.server.messages.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.clienttoserver.UpdateMagicPowerDistributionMessage;
import momime.common.messages.servertoclient.TextPopupMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Client sends updated slider bar positions when clicking OK from the magic screen
 */
public final class UpdateMagicPowerDistributionMessageImpl extends UpdateMagicPowerDistributionMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UpdateMagicPowerDistributionMessageImpl.class);
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException
	{
		log.trace ("Entering process: Player ID " + sender.getPlayerDescription ().getPlayerID ());

		final String error;

		// Check all the values are within valid ranges
		if ((getDistribution ().getManaRatio () < 0) || (getDistribution ().getResearchRatio () < 0) || (getDistribution ().getSkillRatio () < 0) ||
			(getDistribution ().getManaRatio () > CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX) || (getDistribution ().getResearchRatio () > CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX) || (getDistribution ().getSkillRatio () > CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX))

			error = "One or more of the Magic Power Ratios was outside the range 0..100%";

		else
		{
			// Check they add up to 100%
			final int total = getDistribution ().getManaRatio () + getDistribution ().getResearchRatio () + getDistribution ().getSkillRatio ();
			if (total != CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX)
				error = "The total of all three Magic Power Ratios did not add up to 100% (" + total + ")";
			else
				error = null;
		}

		// All ok?
		if (error != null)
		{
			// Return error
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Do the update
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
			priv.setMagicPowerDistribution (getDistribution ());

			// No confirmation - client assumes change will be OK
		}

		log.trace ("Exiting process");
	}
}