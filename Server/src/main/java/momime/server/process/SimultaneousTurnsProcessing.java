package momime.server.process;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;

import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Processing methods specifically for dealing with simultaneous turns games
 */
public interface SimultaneousTurnsProcessing
{
	/**
	 * Processes PendingMovements at the end of a simultaneous turns game.
	 * 
	 * This routine will end when either we find a combat that needs to be played (in which case it ends with a call to startCombat)
	 * or when the only PendingMovements remaining are unit stacks that have no movement left (in which case it ends with a call to endPhase).
	 * 
	 * It must be able to carry on where it left off, so e.g. it is called the first time, and ends when it finds a combat that a human player needs
	 * to do, then the end of combatEnded will call it again, and it must be able to continue processing remaining movements.
	 * 
	 * Combats just between AI players still cause this routine to exit out, because combatEnded being triggered results in this routine
	 * being called again, so it will start another execution and continue where it left off, and the original invocation will exit out. 
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void processSimultaneousTurnsMovement (final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException;
	
	/**
	 * Processes all unit & building special orders in a simultaneous turns game 'end phase'	 * 
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void processSpecialOrders (final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException;
}
