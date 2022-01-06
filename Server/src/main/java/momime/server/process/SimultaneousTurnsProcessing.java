package momime.server.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import jakarta.xml.bind.JAXBException;
import momime.server.MomSessionVariables;

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
	 * @throws IOException If there is another kind of problem
	 */
	public void processSimultaneousTurnsMovement (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
	
	/**
	 * Processes all unit & building special orders in a simultaneous turns game 'end phase'	 * 
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public void processSpecialOrders (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
}
