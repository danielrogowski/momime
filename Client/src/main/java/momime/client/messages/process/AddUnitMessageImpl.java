package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.common.messages.servertoclient.v0_9_5.AddUnitMessage;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.utils.UnitUtils;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

/**
 * Server sends this to clients to tell them about a new unit added to the map, or can add them in bulk as part of fogOfWarVisibleAreaChanged.
 * 
 * If readSkillsFromXML is true, client will read unit skills from the XML database (otherwise the client, receiving a message with zero skills, cannot tell if it is a hero who
 * genuinely has no skills (?) or is expected to read in the skills from the XML database).
 * 
 * If skills are included, the Experience value is not used so is omitted, since the Experience value will be included in the skill list.
 * 
 * Bulk adds (fogOfWarVisibleAreaChanged) can contain a mixture of units with and without skill lists included.
 */
public final class AddUnitMessageImpl extends AddUnitMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (AddUnitMessageImpl.class.getName ());

	/** Unit utils */
	private UnitUtils unitUtils;
	
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
		log.entering (AddUnitMessageImpl.class.getName (), "process", getData ().getUnitURN ());
		
		// Since Java server now supports units set to 'remember as last seen', its possible to get an 'add unit' message just to
		// update a unit that we remember in a different state - easiest way to handle this is to see if the UnitURN already
		// exists in our list, and if it does, free it before adding the updated copy of it
		final MemoryUnit oldUnit = getUnitUtils ().findUnitURN (getData ().getUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ());
		if (oldUnit != null)
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ().remove (oldUnit);
		
		// Add it
		final MemoryUnit newUnit = getUnitUtils ().createMemoryUnit (getData ().getUnitID (), getData ().getUnitURN (),
			getData ().getWeaponGrade (), getData ().getExperience (), getData ().isReadSkillsFromXML (), getClient ().getClientDB ());
		
		newUnit.setOwningPlayerID (getData ().getOwningPlayerID ());
		newUnit.setUnitLocation (getData ().getUnitLocation ());
		newUnit.setHeroNameID (getData ().getHeroNameID ());
		newUnit.getUnitHasSkill ().addAll (getData ().getUnitHasSkill ());
		
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ().add (newUnit);
		
		log.exiting (AddUnitMessageImpl.class.getName (), "process");
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
