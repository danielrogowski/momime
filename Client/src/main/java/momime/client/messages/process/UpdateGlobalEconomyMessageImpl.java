package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.common.messages.servertoclient.v0_9_5.UpdateGlobalEconomyMessage;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

/**
 * Server sends this to each client to tell them what their current production rates and storage are.
 * 
 * This is a good place to send OverlandCastingSkillRemainingThisTurn to the client as well, since any instantly cast spells
 * will result in mana being reduced so new GPVs will need to be sent anyway (and recalc'd in case the new instantly cast spell has some maintenance).
 * 
 * Similarly the OverlandCastingSkillRemainingThisTurn value needs to be set on the client at the start of each turn, so why not include it in the GPV message.
 * 
 * Also both stored mana and OverlandCastingSkillRemainingThisTurn being set on the client simultaneously is convenient
 * for working out EffectiveCastingSkillRemainingThisTurn.
 * 
 * CastingSkillRemainingThisCombat is also sent by the server to avoid having to repeat the skill calc on the client,
 * since new GPVs are sent (to update mana) every time we cast a combat spell.
 */
public final class UpdateGlobalEconomyMessageImpl extends UpdateGlobalEconomyMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (UpdateGlobalEconomyMessageImpl.class.getName ());

	/** Multiplayer client */
	private MomClient client;

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
		log.entering (UpdateGlobalEconomyMessageImpl.class.getName (), "process");

		// Accept new values
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue ().clear ();
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue ().addAll (getResourceValue ());
		
		getClient ().getOurTransientPlayerPrivateKnowledge ().setOverlandCastingSkillRemainingThisTurn (getOverlandCastingSkillRemainingThisTurn ());
		
		log.exiting (UpdateGlobalEconomyMessageImpl.class.getName (), "process");
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
}
