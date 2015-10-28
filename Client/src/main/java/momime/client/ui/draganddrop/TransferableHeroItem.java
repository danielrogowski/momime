package momime.client.ui.draganddrop;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import momime.common.database.HeroItem;

/**
 * Provides support for dragging and dropping hero items
 */
public final class TransferableHeroItem implements Transferable
{
	/** The data flavour for hero items */
	private DataFlavor heroItemFlavour;
	
	/** The item being dragged and dropped */
	private HeroItem heroItem;
	
	/**
	 * @return Hero items are the only data flavour supported
	 */
	@Override
	public final DataFlavor [] getTransferDataFlavors ()
	{
		return new DataFlavor [] {getHeroItemFlavour ()};
	}

	/**
	 * @param flavour Flavour to test
	 * @return Hero items are the only data flavour supported
	 */
	@Override
	public final boolean isDataFlavorSupported (final DataFlavor flavour)
	{
		return flavour.equals (getHeroItemFlavour ());
	}

	/**
	 * @param flavour The requested flavour
	 * @return The item being dragged and dropped 
	 */
	@Override
	public final Object getTransferData (final DataFlavor flavour) throws UnsupportedFlavorException
	{
		if (!flavour.equals (getHeroItemFlavour ()))
			throw new UnsupportedFlavorException (flavour);
				
		return getHeroItem ();
	}

	/**
	 * @return The data flavour for hero items
	 */
	public final DataFlavor getHeroItemFlavour ()
	{
		return heroItemFlavour;
	}

	/**
	 * @param flavour The data flavour for hero items
	 */
	public final void setHeroItemFlavour (final DataFlavor flavour)
	{
		heroItemFlavour = flavour;
	}

	/**
	 * @return The item being dragged and dropped
	 */
	public final HeroItem getHeroItem ()
	{
		return heroItem;
	}

	/**
	 * @param item The item being dragged and dropped
	 */
	public final void setHeroItem (final HeroItem item)
	{
		heroItem = item;
	}
}