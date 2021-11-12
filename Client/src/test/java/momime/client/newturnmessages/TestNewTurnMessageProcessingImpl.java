package momime.client.newturnmessages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import momime.client.MomClient;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.MomException;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageData;

/**
 * Tests the NewTurnMessageProcessingImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestNewTurnMessageProcessingImpl
{
	/**
	 * Tests the expireMessages method
	 */
	@Test
	public final void testExpireMessages ()
	{
		// Make a sample list of messages, NB. we need a concrete message to test with - which one we choose to use is pretty irrelevant
		final NewTurnMessagePopulationChangeEx msg1 = new NewTurnMessagePopulationChangeEx ();
		msg1.setStatus (NewTurnMessageStatus.MAIN);
		
		final NewTurnMessagePopulationChangeEx msg2 = new NewTurnMessagePopulationChangeEx ();
		msg2.setStatus (NewTurnMessageStatus.AFTER_OUR_TURN_BEGAN);
		
		final NewTurnMessagePopulationChangeEx msg3 = new NewTurnMessagePopulationChangeEx ();
		msg3.setStatus (NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN);
		
		final NewTurnMessagePopulationChangeEx msg4 = new NewTurnMessagePopulationChangeEx ();
		msg4.setStatus (NewTurnMessageStatus.AFTER_OUR_TURN_BEGAN);
		
		final NewTurnMessagePopulationChangeEx msg5 = new NewTurnMessagePopulationChangeEx ();
		msg5.setStatus (NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN);
		
		final NewTurnMessagePopulationChangeEx msg6 = new NewTurnMessagePopulationChangeEx ();
		msg6.setStatus (NewTurnMessageStatus.MAIN);
		
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();
		trans.getNewTurnMessage ().add (msg1);
		trans.getNewTurnMessage ().add (msg2);
		trans.getNewTurnMessage ().add (msg3);
		trans.getNewTurnMessage ().add (msg4);
		trans.getNewTurnMessage ().add (msg5);
		trans.getNewTurnMessage ().add (msg6);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurTransientPlayerPrivateKnowledge ()).thenReturn (trans);
		
		// Set up object to test
		final NewTurnMessageProcessingImpl proc = new NewTurnMessageProcessingImpl ();
		proc.setClient (client);
		
		// Run method
		proc.expireMessages ();		 
		
		// Check results
		assertEquals (2, trans.getNewTurnMessage ().size ());
		assertSame (msg2, trans.getNewTurnMessage ().get (0));
		assertSame (msg4, trans.getNewTurnMessage ().get (1));
		
		assertEquals (NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN, msg2.getStatus ());
		assertEquals (NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN, msg4.getStatus ());
	}
	
	/**
	 * Tests the readNewTurnMessagesFromServer method
	 * @throws IOException If one of the messages doesn't support the NewTurnMessageExpiration interface or a preProcess method fails
	 */
	@Test
	public final void testReadNewTurnMessagesFromServer () throws IOException
	{
		// Set up some existing messages, NB. we need a concrete message to test with - which one we choose to use is pretty irrelevant
		final NewTurnMessagePopulationChangeEx msg1 = new NewTurnMessagePopulationChangeEx ();
		msg1.setStatus (NewTurnMessageStatus.MAIN);
		
		final NewTurnMessagePopulationChangeEx msg2 = new NewTurnMessagePopulationChangeEx ();
		msg2.setStatus (NewTurnMessageStatus.AFTER_OUR_TURN_BEGAN);
		
		final NewTurnMessagePopulationChangeEx msg3 = new NewTurnMessagePopulationChangeEx ();
		msg3.setStatus (NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN);

		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();
		trans.getNewTurnMessage ().add (msg1);
		trans.getNewTurnMessage ().add (msg2);
		trans.getNewTurnMessage ().add (msg3);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurTransientPlayerPrivateKnowledge ()).thenReturn (trans);
		
		// Set up some messages to add - be realistic by not giving them any assigned status
		final NewTurnMessagePopulationChangeEx msg4 = new NewTurnMessagePopulationChangeEx ();
		final NewTurnMessagePopulationChangeEx msg5 = new NewTurnMessagePopulationChangeEx ();
		final NewTurnMessagePopulationChangeEx msg6 = new NewTurnMessagePopulationChangeEx ();
		
		final List<NewTurnMessageData> msgs = new ArrayList<NewTurnMessageData> ();
		msgs.add (msg4);
		msgs.add (msg5);
		msgs.add (msg6);
		
		// Set up object to test
		final NewTurnMessageProcessingImpl proc = new NewTurnMessageProcessingImpl ();
		proc.setOverlandMapRightHandPanel (new OverlandMapRightHandPanel ());
		proc.setClient (client);
		
		// Run method
		proc.readNewTurnMessagesFromServer (msgs, NewTurnMessageStatus.MAIN);
		
		// Check results
		assertEquals (6, trans.getNewTurnMessage ().size ());
		assertSame (msg1, trans.getNewTurnMessage ().get (0));
		assertSame (msg2, trans.getNewTurnMessage ().get (1));
		assertSame (msg3, trans.getNewTurnMessage ().get (2));
		assertSame (msg4, trans.getNewTurnMessage ().get (3));
		assertSame (msg5, trans.getNewTurnMessage ().get (4));
		assertSame (msg6, trans.getNewTurnMessage ().get (5));
		
		assertEquals (NewTurnMessageStatus.MAIN, msg4.getStatus ());
		assertEquals (NewTurnMessageStatus.MAIN, msg5.getStatus ());
		assertEquals (NewTurnMessageStatus.MAIN, msg6.getStatus ());
	}
	
	/**
	 * Tests the sortAndAddCategories method
	 * @throws MomException If one of the messages doesn't support the NewTurnMessageUI interface
	 */
	@Test
	public final void testSortAndAddCategories () throws MomException
	{
		// Make a sample list of messages
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();
		trans.getNewTurnMessage ().add (new NewTurnMessageConstructBuildingEx ());
		trans.getNewTurnMessage ().add (new NewTurnMessageNodeEx ());
		trans.getNewTurnMessage ().add (new NewTurnMessageSpellEx ());
		trans.getNewTurnMessage ().add (new NewTurnMessageConstructUnitEx ());
		trans.getNewTurnMessage ().add (new NewTurnMessageSummonUnitEx ());
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurTransientPlayerPrivateKnowledge ()).thenReturn (trans);
		
		// Factory for creating categories
		final NewTurnMessagesFactory messageFactory = mock (NewTurnMessagesFactory.class);
		when (messageFactory.createNewTurnMessageCategory ()).thenAnswer ((i) -> new NewTurnMessageCategory ());
		
		// Set up object to test
		final NewTurnMessageProcessingImpl proc = new NewTurnMessageProcessingImpl ();
		proc.setNewTurnMessagesFactory (messageFactory);
		proc.setClient (client);
		
		// Run method
		final List<NewTurnMessageUI> msgs = proc.sortAndAddCategories ();		 
		
		// Check results
		assertEquals (8, msgs.size ());
		
		assertEquals (NewTurnMessageCategory.class, msgs.get (0).getClass ());
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_CONSTRUCTION, msgs.get (0).getSortOrder ());
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_CONSTRUCTION, msgs.get (1).getSortOrder ());
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_CONSTRUCTION, msgs.get (2).getSortOrder ());
		
		assertEquals (NewTurnMessageCategory.class, msgs.get (3).getClass ());
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_SPELLS, msgs.get (3).getSortOrder ());
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_SPELLS, msgs.get (4).getSortOrder ());
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_SPELLS, msgs.get (5).getSortOrder ());

		assertEquals (NewTurnMessageCategory.class, msgs.get (6).getClass ());
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_NODES, msgs.get (6).getSortOrder ());
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_NODES, msgs.get (7).getSortOrder ());
	}
}