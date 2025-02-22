package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.ui.frames.CombatUI;
import momime.common.MomException;
import momime.common.messages.servertoclient.UpdateCombatMapMessage;

/**
 * Server sends this to the client when the combat terrain changes while a combat is in progress,
 * e.g. as a result of casting Wall of Fire/Darkness in combat.
 */
public final class UpdateCombatMapMessageImpl extends UpdateCombatMapMessage implements BaseServerToClientMessage
{
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
		if (!getCombatUI ().getCombatLocation ().equals (getCombatLocation ()))
			throw new MomException ("Server sent updated combat map for a location other than the combat we're playing");
		
		getCombatUI ().setCombatTerrain (getCombatTerrain ());
		getCombatUI ().regenerateBitmaps ();
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