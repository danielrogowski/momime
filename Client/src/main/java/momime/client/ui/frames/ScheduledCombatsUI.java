package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import momime.client.newturnmessages.NewTurnMessageClickable;
import momime.client.scheduledcombatmessages.ScheduledCombatMessageUI;
import momime.client.ui.renderer.ScheduledCombatMessageRenderer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

/**
 * UI displaying all scheduled combats that us and others need to play out in the combat phase of this simultaneous turn.
 * This shares the layout/background with the NewTurnMessagesUI, except that there's no close button - the list of
 * combats is closed automatically after all combats have been played; you can't manually close it.
 */
public final class ScheduledCombatsUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ScheduledCombatsUI.class);
	
	/** XML layout */
	private XmlLayoutContainerEx newTurnMessagesLayout;

	/** Stores the list of combats to display on the scroll */
	private DefaultListModel<ScheduledCombatMessageUI> combats;

	/** Combats list box */
	private JList<ScheduledCombatMessageUI> combatsList;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/scroll/background.png");
		final BufferedImage roller = getUtils ().loadImage ("/momime.client.graphics/ui/scroll/position3-0.png");
		
		final int backgroundTop = roller.getHeight () - NewTurnMessagesUI.SCROLL_OVERLAP_TOP;
		final int backgroundLeft = (roller.getWidth () - background.getWidth ()) / 2;
		final int bottomRollerTop = backgroundTop + background.getHeight () - NewTurnMessagesUI.SCROLL_OVERLAP_BOTTOM;
		final int bottomRollerBottom = bottomRollerTop + roller.getHeight ();
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				g.drawImage (background, backgroundLeft, backgroundTop, null);
				
				g.drawImage (roller, 0, 0, null);
				g.drawImage (roller, 0, bottomRollerTop, null);
			}
		};
		
		contentPane.setBackground (Color.BLACK);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getNewTurnMessagesLayout ()));

		combats = new DefaultListModel<ScheduledCombatMessageUI> ();
		
		combatsList = new JList<ScheduledCombatMessageUI> ();
		combatsList.setOpaque (false);
		combatsList.setModel (combats);
		combatsList.setCellRenderer (new ScheduledCombatMessageRenderer ());		// The renderer has no injections (yet) so doesn't have a spring prototype
		combatsList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		
		contentPane.add (combatsList, "frmNewTurnMessagesList");
		
		// Pass clicks to the NTM objects
		combatsList.addMouseListener (new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				if (combatsList.getSelectedIndex () >= 0)
				{
					final ScheduledCombatMessageUI msg = combats.get (combatsList.getSelectedIndex ());
					
					// Not all types of message are clickable
					if (msg instanceof NewTurnMessageClickable)
						try
						{
							((NewTurnMessageClickable) msg).clicked ();
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
				}
			}
		});
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);

		// Trying to get the shape of this right is very long and complicated, and impossible to get perfect because the edges are all jagged,
		// so this is just as close as I can get it.  It starts from the top-right corner under the roller and proceeds clockwise.
		
		// Also note the rollers are lopsided, with 7 pixels above them but only 6 below
		getFrame ().setShape (new Polygon
			(new int [] {backgroundLeft + background.getWidth (), backgroundLeft + background.getWidth (),
					
				// Bottom-right roller
				backgroundLeft + background.getWidth () + 2, getNewTurnMessagesLayout ().getFormWidth () - 29, getNewTurnMessagesLayout ().getFormWidth () - 22, getNewTurnMessagesLayout ().getFormWidth () - 16, getNewTurnMessagesLayout ().getFormWidth () - 7, getNewTurnMessagesLayout ().getFormWidth (), getNewTurnMessagesLayout ().getFormWidth (), getNewTurnMessagesLayout ().getFormWidth () - 7, getNewTurnMessagesLayout ().getFormWidth () - 16, getNewTurnMessagesLayout ().getFormWidth () - 22, getNewTurnMessagesLayout ().getFormWidth () - 29, backgroundLeft + background.getWidth () + 2,
				
				// Bottom edge
				backgroundLeft + background.getWidth (), backgroundLeft,
					
				// Bottom-left roller
				backgroundLeft - 2, 29, 22, 16, 7, 0, 0, 7, 16, 22, 29, backgroundLeft - 2,
					
				// Left edge
				backgroundLeft, backgroundLeft,
					
				// Top-left roller
				backgroundLeft - 2, 29, 22, 16, 7, 0, 0, 7, 16, 22, 29, backgroundLeft - 2,
				
				// Top edge
				backgroundLeft, backgroundLeft + background.getWidth (),
					
				// Top-right roller
				backgroundLeft + background.getWidth () + 2, getNewTurnMessagesLayout ().getFormWidth () - 29, getNewTurnMessagesLayout ().getFormWidth () - 22, getNewTurnMessagesLayout ().getFormWidth () - 16, getNewTurnMessagesLayout ().getFormWidth () - 7, getNewTurnMessagesLayout ().getFormWidth (), getNewTurnMessagesLayout ().getFormWidth (), getNewTurnMessagesLayout ().getFormWidth () - 7, getNewTurnMessagesLayout ().getFormWidth () - 16, getNewTurnMessagesLayout ().getFormWidth () - 22, getNewTurnMessagesLayout ().getFormWidth () - 29, backgroundLeft + background.getWidth () + 2},
					
			new int [] {backgroundTop, backgroundTop + background.getHeight (),
						
				// Bottom-right roller
				bottomRollerTop, bottomRollerTop, bottomRollerTop + 9, bottomRollerTop + 10, bottomRollerTop + 6, bottomRollerTop + 14, bottomRollerBottom - 14, bottomRollerBottom - 6, bottomRollerBottom - 10, bottomRollerBottom - 9, bottomRollerBottom, bottomRollerBottom,

				// Bottom edge
				bottomRollerBottom - 6, bottomRollerBottom - 6,
				
				// Bottom-left roller
				bottomRollerBottom, bottomRollerBottom, bottomRollerBottom - 9, bottomRollerBottom - 10, bottomRollerBottom - 6, bottomRollerBottom - 14, bottomRollerTop + 14, bottomRollerTop + 6, bottomRollerTop + 10, bottomRollerTop + 9, bottomRollerTop, bottomRollerTop,
					
				// Left edge
				backgroundTop + background.getHeight (), backgroundTop,
					
				// Top-left roller
				roller.getHeight (), roller.getHeight (), roller.getHeight () - 9, roller.getHeight () - 10, roller.getHeight () - 6, roller.getHeight () - 14, 14, 6, 10, 9, 0, 0,

				// Top edge
				7, 7,
				
				// Top-right roller
				0, 0, 9, 10, 6, 14, roller.getHeight () - 14, roller.getHeight () - 6, roller.getHeight () - 10, roller.getHeight () - 9, roller.getHeight (), roller.getHeight ()},
					
			56));
		
		log.trace ("Exiting init");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		getFrame ().setTitle (getLanguage ().findCategoryEntry ("ScheduledCombats", "Title"));
		combatsList.repaint ();
		
		log.trace ("Exiting languageChanged");
	}

	/**
	 * @param msgs The replacement list of messages to display on the scroll
	 * @throws IOException If there is a problem
	 */
	public final void setCombatMessages (final List<ScheduledCombatMessageUI> msgs) throws IOException
	{
		log.trace ("Entering setCombatMessages: " + msgs.size ());

		// Clear out the old messages
		combats.clear ();
		
		// Add in the new messages
		for (final ScheduledCombatMessageUI msg : msgs)
			combats.addElement (msg);
		
		log.trace ("Exiting setCombatMessages");
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getNewTurnMessagesLayout ()
	{
		return newTurnMessagesLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setNewTurnMessagesLayout (final XmlLayoutContainerEx layout)
	{
		newTurnMessagesLayout = layout;
	}
}