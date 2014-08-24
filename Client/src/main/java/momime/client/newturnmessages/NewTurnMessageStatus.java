package momime.client.newturnmessages;

/**
 * Records when we received NTMs from the server, so we know when to expire them and remove them from our list.  The behaviour we want in the end is that:
 * 1) The main block of NTMs that arrive at the start of our turn cause the scroll to be popped up to the player, so they act on the messages.
 * 2) The player can close the scroll, and click the Msgs button to view them again without affecting its contents.
 * 3) NTMs generated during our turn or during other player's turns add to the scroll, and light up the button, but don't force it to pop up.
 *		(That's TBD, it makes sense for mid-turn 'target spell' NTMs to not only force popping up the NTM scroll but also auto-click the message).
 * 4) The player may or may not have the scroll still open, or click the Msgs button, during/after their turn to view mid-turn NTMs, that's their choice
 * 5) At the start of the player's next turn, the scroll will pop up again and contain a mixture of new msgs for this turn, and mid-turn NTMs that arrived since
 * 	their previous turn, distinguished by their wording being slightly different (e.g. "You have completed casting X" vs. "Last turn you completed casting X").
 * 
 * So the aim is that the player has the choice to view mid-turn NTMs prior to their next turn starting, at which point they're then forced to view them alongside new msgs.
 * 
 * NB. The Delphi client called these 'Main', 'AddedDuringThisTurn', and 'AddedDuringLastTurn' but that implies this could be solved by
 * recording the turn number in which messages were added, rather than a status value, which isn't true, so those names were
 * a little misleading and so have been improved on here.
 * 
 * Consider the example where we play our turn 5, we own some nodes, there's 3 players in the game, and we take our turn 2nd.
 * So the next player to take their turn is the 3rd player playing their turn 5.  They capture a node of ours - so that generates and sends to us an NTM during turn 5.
 * The next player to take their turn is the 1st player playing their turn 6.  They also  capture a node of ours - so that generates and sends to us an NTM during turn 6.
 * Then its our turn 6.  At this point if we consider all NTMs from turn <= 5 to be 'old' and remove them, we'll never get to see that we lost the first node.
 * This is why recording a turn number doesn't work and we need these status values instead, and why they've been renamed as such.
 */
public enum NewTurnMessageStatus
{
	/** Main block of NTMs received as our turn begins */
	MAIN,
	
	/** NTMs sent separately after the beginning of our turn - so includes both NTMs we generate during our own turn, and generated during other players' turns */
	AFTER_OUR_TURN_BEGAN,
	
	/** As we begin a new turn, any old MAIN messages are removed, and any AFTER_OUR_TURN_BEGAN messages are downgraded to BEFORE_OUR_TURN_BEGAN */ 
	BEFORE_OUR_TURN_BEGAN;
}