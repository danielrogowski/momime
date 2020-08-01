package momime.client.ui.frames;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.language.database.SpellLang;
import momime.client.ui.MomUIConstants;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.renderer.QueuedSpellListCellRenderer;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.QueuedSpell;

/**
 * Screen showing all overland spells queued up to be cast, so we can cancel some if desired.
 */
public final class QueuedSpellsUI extends MomClientFrameUI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (QueuedSpellsUI.class);

	/** XML layout */
	private XmlLayoutContainerEx queuedSpellsLayout;

	/** Large font */
	private Font largeFont;
	
	/** Small font */
	private Font smallFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Renderer for the spells list */
	private QueuedSpellListCellRenderer queuedSpellListCellRenderer;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Title */
	private JLabel title;
	
	/** Close action */
	private Action closeAction;
	
	/** Items in the spells list box */
	private DefaultListModel<QueuedSpell> spellsItems; 
	
	/** Spells list box */
	private JList<QueuedSpell> spellsList;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/queuedSpells.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldPressed.png");

		// Actions
		closeAction = new LoggingAction ((ev) -> getFrame ().setVisible (false));
		
		// Initialize the content pane
		final JPanel contentPane = getUtils ().createPanelWithBackgroundImage (background);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getQueuedSpellsLayout ()));
		
		title = getUtils ().createLabel (MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (title, "frmSpellQueueTitle");
		
		contentPane.add (getUtils ().createImageButton (closeAction,
			MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmSpellQueueClose");
		
		// Set up the list
		getQueuedSpellListCellRenderer ().setFont (getSmallFont ());
		getQueuedSpellListCellRenderer ().setForeground (MomUIConstants.SILVER);
		getQueuedSpellListCellRenderer ().init ();
		
		spellsItems = new DefaultListModel<QueuedSpell> ();
		spellsList = new JList<QueuedSpell> ();
		spellsList.setOpaque (false);
		spellsList.setModel (spellsItems);
		spellsList.setCellRenderer (getQueuedSpellListCellRenderer ());
		spellsList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);

		final JScrollPane spellsScroll = getUtils ().createTransparentScrollPane (spellsList);
		spellsScroll.setHorizontalScrollBarPolicy (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		contentPane.add (spellsScroll, "frmSpellQueueList");
		
		updateQueuedSpells ();
		
		// Allow clicking on queued spells to cancel them
		spellsList.addListSelectionListener ((ev) ->
		{
			if ((spellsList.getSelectedIndex () >= 0) && (spellsList.getSelectedIndex () < getClient ().getOurPersistentPlayerPrivateKnowledge ().getQueuedSpell ().size ()))
				try
				{
					final MessageBoxUI messageBox = getPrototypeFrameCreator ().createMessageBox ();
					messageBox.setTitleLanguageCategoryID ("frmSpellQueue");
					messageBox.setTitleLanguageEntryID ("Title");

					final String spellID = getClient ().getOurPersistentPlayerPrivateKnowledge ().getQueuedSpell ().get (spellsList.getSelectedIndex ()).getQueuedSpellID ();
					if (spellID.equals (CommonDatabaseConstants.SPELL_ID_SPELL_OF_RETURN))
					{
						messageBox.setTextLanguageCategoryID ("frmSpellQueue");
						messageBox.setTextLanguageEntryID ("CantCancelSpellOfReturn");
					}
					else
					{
						final SpellLang spellLang = getLanguage ().findSpell (spellID);
						
						if ((spellLang != null) && (spellLang.getSpellName () != null))
							messageBox.setText (getLanguage ().findCategoryEntry ("frmSpellQueue", "ConfirmCancelSpell").replaceAll ("SPELL_NAME", spellLang.getSpellName ()));
						else
						{
							messageBox.setTextLanguageCategoryID ("frmSpellQueue");
							messageBox.setTextLanguageEntryID ("ConfirmCancelUnknownSpell");
						}
						
						// Don't actually tell the server to cancel the spell until the player clicks Yes
						messageBox.setRemoveQueuedSpellIndex (spellsList.getSelectedIndex ());
					}

					messageBox.setVisible (true);
				}
				catch (final IOException e)
				{
					log.error (e, e);
				}
		});
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		
		log.trace ("Exiting init");
	}
	
	/**
	 * Updates the list of queued spells
	 */
	public final void updateQueuedSpells ()
	{
		log.trace ("Entering updateQueuedSpells");
		
		if (spellsItems != null)
		{
			spellsItems.clear ();
			for (final QueuedSpell queued : getClient ().getOurPersistentPlayerPrivateKnowledge ().getQueuedSpell ())
				spellsItems.addElement (queued);
			
			spellsList.repaint ();
		}
		
		log.trace ("Exiting updateQueuedSpells");
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		getFrame ().setTitle (getLanguage ().findCategoryEntry ("frmSpellQueue", "Title"));
		title.setText (getFrame ().getTitle ());
		
		closeAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmSpellQueue", "Close"));
		
		updateQueuedSpells ();
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getQueuedSpellsLayout ()
	{
		return queuedSpellsLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setQueuedSpellsLayout (final XmlLayoutContainerEx layout)
	{
		queuedSpellsLayout = layout;
	}

	/**
	 * @return Large font
	 */
	public final Font getLargeFont ()
	{
		return largeFont;
	}

	/**
	 * @param font Large font
	 */
	public final void setLargeFont (final Font font)
	{
		largeFont = font;
	}

	/**
	 * @return Small font
	 */
	public final Font getSmallFont ()
	{
		return smallFont;
	}

	/**
	 * @param font Small font
	 */
	public final void setSmallFont (final Font font)
	{
		smallFont = font;
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

	/**
	 * @return Renderer for the spells list
	 */
	public final QueuedSpellListCellRenderer getQueuedSpellListCellRenderer ()
	{
		return queuedSpellListCellRenderer;
	}

	/**
	 * @param rend Renderer for the spells list
	 */
	public final void setQueuedSpellListCellRenderer (final QueuedSpellListCellRenderer rend)
	{
		queuedSpellListCellRenderer = rend;
	}

	/**
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
	}
}