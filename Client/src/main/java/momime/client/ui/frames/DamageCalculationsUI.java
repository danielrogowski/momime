package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

import momime.client.language.replacer.DamageCalculationBreakdown;
import momime.client.language.replacer.DamageCalculationVariableReplacer;
import momime.client.ui.MomUIConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

/**
 * Frame which displays a log of all dice rolls in a combat
 */
public final class DamageCalculationsUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (DamageCalculationsUI.class);
	
	/** How many messages we allow before we start rolling the old ones off the top of the list */
	private static final int MAX_MESSAGES = 100;

	/** XML layout */
	private XmlLayoutContainerEx damageCalculationsLayout;
	
	/** Variable replacer */
	private DamageCalculationVariableReplacer damageCalculationVariableReplacer;
	
	/** Tiny font */
	private Font tinyFont;

	/** Small font */
	private Font smallFont;
	
	/** OK action */
	private Action okAction;

	/** Items in the messages list box */
	private DefaultListModel<String> messagesItems; 
	
	/** Messages list box*/
	private JList<String> messagesList;

	/** Language category ID to use for the title; null if title is not language-variable */
	private List<DamageCalculationBreakdown> breakdowns = new ArrayList<DamageCalculationBreakdown> ();
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/damageCalculations.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldPressed.png");

		// Actions
		okAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getFrame ().dispose ();
			}
		};
		
		// Initialize the frame
		final DamageCalculationsUI ui = this;
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getFrame ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (final WindowEvent ev)
			{
				getLanguageChangeMaster ().removeLanguageChangeListener (ui);
			}
		});
		
		// Initialize the content pane
		final JPanel contentPane = getUtils ().createPanelWithBackgroundImage (background);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getDamageCalculationsLayout ()));
		
		messagesItems = new DefaultListModel<String> ();
		messagesList = new JList<String> ();
		messagesList.setOpaque (false);
		messagesList.setModel (messagesItems);
		messagesList.setFont (getTinyFont ());
		messagesList.setForeground (MomUIConstants.SILVER);
		messagesList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		((DefaultListCellRenderer) messagesList.getCellRenderer ()).setOpaque (false);

		contentPane.add (getUtils ().createTransparentScrollPane (messagesList), "frmCalculationBoxText");

		contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmCalculationBoxOK");
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);

		log.trace ("Exiting init");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		// Title
		getFrame ().setTitle (getLanguage ().findCategoryEntry ("CombatDamage", "Title"));
		
		// Messages
		messagesItems.clear ();
		for (final DamageCalculationBreakdown breakdown : breakdowns)
		{
			getDamageCalculationVariableReplacer ().setBreakdown (breakdown);
			messagesItems.addElement (breakdown.getIndent () + getDamageCalculationVariableReplacer ().replaceVariables
				(getLanguage ().findCategoryEntry ("CombatDamage", breakdown.getLanguageEntryID ())));
		}
		
		messagesList.ensureIndexIsVisible (messagesItems.size () - 1);
		
		// Button
		okAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMessageBox", "OK"));
	}
	
	/**
	 * @param breakdown Breakdown to add
	 */
	public final void addBreakdown (final DamageCalculationBreakdown breakdown)
	{
		log.trace ("Entering addBreakdown: " + breakdown.getMessageType ());
		
		// See if we need to remove the oldest entry
		while (breakdowns.size () >= MAX_MESSAGES)
			breakdowns.remove (0);
		
		if (messagesItems != null)
			while (messagesItems.size () >= MAX_MESSAGES)
				messagesItems.remove (0);
		
		// Add the new one
		breakdowns.add (breakdown);

		if (messagesItems != null)
		{
			getDamageCalculationVariableReplacer ().setBreakdown (breakdown);
			messagesItems.addElement (breakdown.getIndent () + getDamageCalculationVariableReplacer ().replaceVariables
				(getLanguage ().findCategoryEntry ("CombatDamage", breakdown.getLanguageEntryID ())));
			
			messagesList.ensureIndexIsVisible (messagesItems.size () - 1);
		}

		log.trace ("Exiting addBreakdown");
	}

	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getDamageCalculationsLayout ()
	{
		return damageCalculationsLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setDamageCalculationsLayout (final XmlLayoutContainerEx layout)
	{
		damageCalculationsLayout = layout;
	}

	/**
	 * @return Tiny font
	 */
	public final Font getTinyFont ()
	{
		return tinyFont;
	}

	/**
	 * @param font Tiny font
	 */
	public final void setTinyFont (final Font font)
	{
		tinyFont = font;
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
	 * @return Variable replacer
	 */
	public final DamageCalculationVariableReplacer getDamageCalculationVariableReplacer ()
	{
		return damageCalculationVariableReplacer;
	}

	/**
	 * @param replacer Variable replacer
	 */
	public final void setDamageCalculationVariableReplacer (final DamageCalculationVariableReplacer replacer)
	{
		damageCalculationVariableReplacer = replacer;
	}
}