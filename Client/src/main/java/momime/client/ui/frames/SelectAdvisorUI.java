package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Font;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;

import momime.client.ui.MomUIConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

/**
 * Popup from clicking the Info button on the overland map.  Gives all the choices like F1=surveyor, F7=tax rate and so on.
 */
public final class SelectAdvisorUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SelectAdvisorUI.class);

	/** Width of the gold border */
	final static int BORDER_WIDTH = 15;

	/** Height of the gold border at the top */
	final static int TOP_HEIGHT = 17;
	
	/** Height of the gold border at the bottom */
	final static int BOTTOM_HEIGHT = 13;
	
	/** XML layout */
	private XmlLayoutContainerEx selectAdvisorLayout;
	
	/** Small font */
	private Font smallFont;
	
	/** Tax rate UI */
	private TaxRateUI taxRateUI;
	
	/** Title */
	private JLabel title;
	
	/** Surveyor action */
	private Action surveyorAction;
	
	/** Cartographer action */
	private Action cartographerAction;
	
	/** Apprentice action */
	private Action apprenticeAction;
	
	/** Historian action */
	private Action historianAction;
	
	/** Astrologer action */
	private Action astrologerAction;
	
	/** Chanellor action */
	private Action chancellorAction;
	
	/** Tax collector action */
	private Action taxCollectorAction;
	
	/** Grand vizier action */
	private Action grandVizierAction;
	
	/** Wizards action */
	private Action wizardsAction;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/selectAdvisor.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button234x14Normal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button234x14Pressed.png");
		
		// Actions
		surveyorAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};
		
		cartographerAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};
		
		apprenticeAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};
		
		historianAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};
		
		astrologerAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};
		
		chancellorAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};
		
		taxCollectorAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getTaxRateUI ().setVisible (true);
					setVisible (false);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		grandVizierAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};
		
		wizardsAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};
		
		// Initialize the content pane
		final JPanel contentPane = getUtils ().createPanelWithBackgroundImage (background);
		contentPane.setBackground (Color.BLACK);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getSelectAdvisorLayout ()));
		
		title = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		contentPane.add (title, "frmSelectAdvisorTitle");
		
		contentPane.add (getUtils ().createImageButton (surveyorAction,		MomUIConstants.DULL_GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmSelectAdvisor1");
		contentPane.add (getUtils ().createImageButton (cartographerAction,	MomUIConstants.DULL_GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmSelectAdvisor2");
		contentPane.add (getUtils ().createImageButton (apprenticeAction,		MomUIConstants.DULL_GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmSelectAdvisor3");
		contentPane.add (getUtils ().createImageButton (historianAction,		MomUIConstants.DULL_GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmSelectAdvisor4");
		contentPane.add (getUtils ().createImageButton (astrologerAction,		MomUIConstants.DULL_GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmSelectAdvisor5");
		contentPane.add (getUtils ().createImageButton (chancellorAction,		MomUIConstants.DULL_GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmSelectAdvisor6");
		contentPane.add (getUtils ().createImageButton (taxCollectorAction,	MomUIConstants.DULL_GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmSelectAdvisor7");
		contentPane.add (getUtils ().createImageButton (grandVizierAction,	MomUIConstants.DULL_GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmSelectAdvisor8");
		contentPane.add (getUtils ().createImageButton (wizardsAction,			MomUIConstants.DULL_GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmSelectAdvisor9");

		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);
		setCloseOnClick (true);
	
		getFrame ().setShape (new Polygon
			(new int [] {0, BORDER_WIDTH, BORDER_WIDTH, background.getWidth () - BORDER_WIDTH, background.getWidth () - BORDER_WIDTH, background.getWidth (), background.getWidth (), background.getWidth () - 2, background.getWidth () - 2, background.getWidth (), background.getWidth (), background.getWidth () - BORDER_WIDTH, background.getWidth () - BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, 0, 0, 2, 2, 0}, 
			new int [] {0, 0, 2, 2, 0, 0, TOP_HEIGHT, TOP_HEIGHT, background.getHeight () - BOTTOM_HEIGHT, background.getHeight () - BOTTOM_HEIGHT, background.getHeight (), background.getHeight (), background.getHeight () - 2, background.getHeight () - 2, background.getHeight (), background.getHeight (), background.getHeight () - BOTTOM_HEIGHT, background.getHeight () - BOTTOM_HEIGHT, TOP_HEIGHT, TOP_HEIGHT},
			20));		

		log.trace ("Exiting init");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");

		title.setText (getLanguage ().findCategoryEntry ("frmSelectAdvisor", "Title"));
		
		surveyorAction.putValue			(Action.NAME, getLanguage ().findCategoryEntry ("frmSelectAdvisor", "Surveyor"));
		cartographerAction.putValue	(Action.NAME, getLanguage ().findCategoryEntry ("frmSelectAdvisor", "Cartographer"));
		apprenticeAction.putValue		(Action.NAME, getLanguage ().findCategoryEntry ("frmSelectAdvisor", "Apprentice"));
		historianAction.putValue			(Action.NAME, getLanguage ().findCategoryEntry ("frmSelectAdvisor", "Historian"));
		astrologerAction.putValue		(Action.NAME, getLanguage ().findCategoryEntry ("frmSelectAdvisor", "Astrologer"));
		chancellorAction.putValue		(Action.NAME, getLanguage ().findCategoryEntry ("frmSelectAdvisor", "Chancellor"));
		taxCollectorAction.putValue		(Action.NAME, getLanguage ().findCategoryEntry ("frmSelectAdvisor", "TaxCollector"));
		grandVizierAction.putValue		(Action.NAME, getLanguage ().findCategoryEntry ("frmSelectAdvisor", "GrandVizier"));
		wizardsAction.putValue			(Action.NAME, getLanguage ().findCategoryEntry ("frmSelectAdvisor", "Wizards"));
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getSelectAdvisorLayout ()
	{
		return selectAdvisorLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setSelectAdvisorLayout (final XmlLayoutContainerEx layout)
	{
		selectAdvisorLayout = layout;
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
	 * @return Tax rate UI
	 */
	public final TaxRateUI getTaxRateUI ()
	{
		return taxRateUI;
	}

	/**
	 * @param ui Tax rate UI
	 */
	public final void setTaxRateUI (final TaxRateUI ui)
	{
		taxRateUI = ui;
	}
}