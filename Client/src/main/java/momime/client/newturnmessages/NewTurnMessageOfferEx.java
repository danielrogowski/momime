package momime.client.newturnmessages;

/**
 * Methods that all 3 kinds of offer NTMs must support
 */
public interface NewTurnMessageOfferEx
{
    /**
     * @return Unique identifier for this offer
     */
    public int getOfferURN ();

	/**
	 * @return null = not yet decided; true = accepted; false = rejected
	 */
	public Boolean isOfferAccepted ();
	
	/**
	 * @param a null = not yet decided; true = accepted; false = rejected
	 */
	public void setOfferAccepted (final Boolean a);
}