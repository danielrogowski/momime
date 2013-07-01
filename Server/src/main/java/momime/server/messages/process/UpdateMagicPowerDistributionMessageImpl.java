package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.clienttoserver.v0_9_4.UpdateMagicPowerDistributionMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;

import com.ndg.multiplayer.server.ProcessableClientToServerMessage;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Client sends updated slider bar positions when clicking OK from the magic screen
 */
public final class UpdateMagicPowerDistributionMessageImpl extends UpdateMagicPowerDistributionMessage implements ProcessableClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (UpdateMagicPowerDistributionMessageImpl.class.getName ());
	
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
		log.entering (UpdateMagicPowerDistributionMessageImpl.class.getName (), "process", sender.getPlayerDescription ().getPlayerID ());

		final String error;

		// Check all the values are within valid ranges
		if ((getDistribution ().getManaRatio () < 0) || (getDistribution ().getResearchRatio () < 0) || (getDistribution ().getSkillRatio () < 0) ||
			(getDistribution ().getManaRatio () > 240) || (getDistribution ().getResearchRatio () > 240) || (getDistribution ().getSkillRatio () > 240))

			error = "One or more of the Magic Power Ratios was outside the range 0..100%";

		else
		{
			// Check they add up to 100%, with a tolerance here to make up for floating point inexactness
			final double total = getDistribution ().getManaRatio () + getDistribution ().getResearchRatio () + getDistribution ().getSkillRatio ();
			if ((total < 239.999) || (total > 240.001))
				error = "The total of all three Magic Power Ratios did not add up to 100% (" + total + ")";
			else
				error = null;
		}

		// All ok?
		if (error != null)
		{
			// Return error
			log.warning (UpdateMagicPowerDistributionMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

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

		log.exiting (UpdateMagicPowerDistributionMessageImpl.class.getName (), "process");
	}
}
