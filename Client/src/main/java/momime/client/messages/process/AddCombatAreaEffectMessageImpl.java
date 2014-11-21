package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.CombatUI;
import momime.common.messages.servertoclient.AddCombatAreaEffectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this to notify clients of new CAEs, or those that have newly come into view.
 * Besides the info we remember, the client also needs the spell ID for animation purposes
 */
public final class AddCombatAreaEffectMessageImpl extends AddCombatAreaEffectMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (AddCombatAreaEffectMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getMemoryCombatAreaEffect ().getMapLocation () + ", " + getMemoryCombatAreaEffect ().getCombatAreaEffectID ());

		// Add it
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect ().add (getMemoryCombatAreaEffect ());
		
		// If there's a combat in progress, the icon for this CAE might need to be added to it
		if (getCombatUI ().isVisible ())
			getCombatUI ().generateCombatAreaEffectIcons ();
		
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
	 * @return Combat UI
	 */
	public final CombatUI getCombatUI ()
	{
		return combatUI;
	}

	/**
	 * @param ui Combat UI
	 */
	public final void setCombatUI (final CombatUI ui)
	{
		combatUI = ui;
	}
}