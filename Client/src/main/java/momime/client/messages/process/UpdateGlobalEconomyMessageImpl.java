package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.AlchemyUI;
import momime.client.ui.frames.MagicSlidersUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.messages.servertoclient.v0_9_5.UpdateGlobalEconomyMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
	private final Log log = LogFactory.getLog (UpdateGlobalEconomyMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;

	/** Magic sliders screen */
	private MagicSlidersUI magicSlidersUI;
	
	/** Alchemy UI */
	private AlchemyUI alchemyUI;
	
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
		log.trace ("Entering process");

		// Accept new values
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue ().clear ();
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue ().addAll (getResourceValue ());
		
		getClient ().getOurTransientPlayerPrivateKnowledge ().setOverlandCastingSkillRemainingThisTurn (getOverlandCastingSkillRemainingThisTurn ());
		
		// Update new values in UI screens that rely on any resource values
		getOverlandMapRightHandPanel ().updateGlobalEconomyValues ();
		getMagicSlidersUI ().updateProductionLabels ();
		getAlchemyUI ().updateSliderMaximum ();
		
		log.trace ("Exiting process");
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
	 * @return Overland map right hand panel showing economy etc
	 */
	public final OverlandMapRightHandPanel getOverlandMapRightHandPanel ()
	{
		return overlandMapRightHandPanel;
	}

	/**
	 * @param panel Overland map right hand panel showing economy etc
	 */
	public final void setOverlandMapRightHandPanel (final OverlandMapRightHandPanel panel)
	{
		overlandMapRightHandPanel = panel;
	}

	/**
	 * @return Magic sliders screen
	 */
	public final MagicSlidersUI getMagicSlidersUI ()
	{
		return magicSlidersUI;
	}

	/**
	 * @param ui Magic sliders screen
	 */
	public final void setMagicSlidersUI (final MagicSlidersUI ui)
	{
		magicSlidersUI = ui;
	}

	/**
	 * @return Alchemy UI
	 */
	public final AlchemyUI getAlchemyUI ()
	{
		return alchemyUI;
	}

	/**
	 * @param ui Alchemy UI
	 */
	public final void setAlchemyUI (final AlchemyUI ui)
	{
		alchemyUI = ui;
	}
}