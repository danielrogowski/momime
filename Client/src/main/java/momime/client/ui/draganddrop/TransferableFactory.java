package momime.client.ui.draganddrop;

/**
 * Factory for creating all the possible Transferable drag and drop containers used by MoM IME
 */
public interface TransferableFactory
{
	/**
	 * @return Newly created TransferableHeroItem
	 */
	public TransferableHeroItem createTransferableHeroItem ();
}