package momime.server.fogofwar;

/**
 * KillUnitActionID is defined in the XSD, as all the possible ways a unit can be killed that the client needs to be informed of.
 * However there are a couple of additional ways units can die that are internally processed on the server only and
 * never transmitted in a message to the client, so I really didn't want to clutter the XSD with values never sent over messages.
 * So these additional untransmitted values are declared here instead. 
 */
public enum UntransmittedKillUnitActionID
{
	/**
	 * Unit killed by combat damage - we don't need to send the clients a separate 'kill' message, because they already
	 * know the unit died because of the previously sent DamageCalculationMessage.
	 */
	COMBAT_DAMAGE;
}
