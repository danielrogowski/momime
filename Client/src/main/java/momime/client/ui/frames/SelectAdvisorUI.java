package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Font;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.ndg.utils.swing.actions.LoggingAction;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.config.WindowID;
import momime.client.languages.database.Shortcut;
import momime.client.ui.MomUIConstants;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.client.ui.panels.OverlandMapRightHandPanelBottom;
import momime.client.ui.panels.OverlandMapRightHandPanelTop;

/**
 * Popup from clicking the Info button on the overland map.  Gives all the choices like F1=surveyor, F7=tax rate and so on.
 */
public final class SelectAdvisorUI extends MomClientFrameUI
{
	/** Width of the gold border */
	public final static int BORDER_WIDTH = 15;

	/** Height of the gold border at the top */
	public final static int TOP_HEIGHT = 17;
	
	/** Height of the gold border at the bottom */
	public final static int BOTTOM_HEIGHT = 13;
	
	/** XML layout */
	private XmlLayoutContainerEx selectAdvisorLayout;
	
	/** Small font */
	private Font smallFont;

	/** Queued spells UI */
	private QueuedSpellsUI queuedSpellsUI;
	
	/** Tax rate UI */
	private TaxRateUI taxRateUI;
	
	/** Wizards UI */
	private WizardsUI wizardsUI;
	
	/** UI for screen showing power base history for each wizard */
	private HistoryUI historyUI;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
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
	
	/** Content pane */
	private JPanel contentPane;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/selectAdvisor.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button234x14Normal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button234x14Pressed.png");
		
		// Actions
		surveyorAction = new LoggingAction ((ev) ->
		{
			getOverlandMapRightHandPanel ().setTop (OverlandMapRightHandPanelTop.SURVEYOR);
			getOverlandMapRightHandPanel ().setBottom (OverlandMapRightHandPanelBottom.CANCEL);
			getOverlandMapRightHandPanel ().setSurveyorLocation (null);
			
			setVisible (false);
		});
		
		cartographerAction = new LoggingAction ((ev) -> {});
		
		apprenticeAction = new LoggingAction ((ev) ->
		{
			getQueuedSpellsUI ().setVisible (true);
			setVisible (false);
		});
		
		historianAction = new LoggingAction ((ev) ->
		{
			getHistoryUI ().setVisible (true);
			setVisible (false);
		});
		
		astrologerAction = new LoggingAction ((ev) -> {});
		chancellorAction = new LoggingAction ((ev) -> {});
		
		taxCollectorAction = new LoggingAction ((ev) ->
		{
			getTaxRateUI ().setVisible (true);
			setVisible (false);
		});
		
		grandVizierAction = new LoggingAction ((ev) -> {});
		
		wizardsAction = new LoggingAction ((ev) ->
		{
			getWizardsUI ().setVisible (true);
			setVisible (false);
		});
		
		// Initialize the content pane
		contentPane = getUtils ().createPanelWithBackgroundImage (background);
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
		setWindowID (WindowID.ADVISORS);

		getFrame ().setShape (new Polygon
			(new int [] {0, BORDER_WIDTH, BORDER_WIDTH, background.getWidth () - BORDER_WIDTH, background.getWidth () - BORDER_WIDTH, background.getWidth (), background.getWidth (), background.getWidth () - 2, background.getWidth () - 2, background.getWidth (), background.getWidth (), background.getWidth () - BORDER_WIDTH, background.getWidth () - BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, 0, 0, 2, 2, 0}, 
			new int [] {0, 0, 2, 2, 0, 0, TOP_HEIGHT, TOP_HEIGHT, background.getHeight () - BOTTOM_HEIGHT, background.getHeight () - BOTTOM_HEIGHT, background.getHeight (), background.getHeight (), background.getHeight () - 2, background.getHeight () - 2, background.getHeight (), background.getHeight (), background.getHeight () - BOTTOM_HEIGHT, background.getHeight () - BOTTOM_HEIGHT, TOP_HEIGHT, TOP_HEIGHT},
			20));		

		// Shortcut keys
		contentPane.getActionMap ().put (Shortcut.ADVISOR_SURVEYOR,				surveyorAction);
		contentPane.getActionMap ().put (Shortcut.ADVISOR_PARCHMENT_MAP,	cartographerAction);
		contentPane.getActionMap ().put (Shortcut.ADVISOR_SPELL_QUEUE,			apprenticeAction);
		contentPane.getActionMap ().put (Shortcut.ADVISOR_POWER_GRAPH,		historianAction);
		contentPane.getActionMap ().put (Shortcut.ADVISOR_WIZARD_STATS,		astrologerAction);
		contentPane.getActionMap ().put (Shortcut.ADVISOR_MESSAGES,				chancellorAction);
		contentPane.getActionMap ().put (Shortcut.ADVISOR_TAX_RATE,				taxCollectorAction);
		contentPane.getActionMap ().put (Shortcut.ADVISOR_AUTO_CONTROL,		grandVizierAction);
		contentPane.getActionMap ().put (Shortcut.ADVISOR_WIZARDS,					wizardsAction);
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		title.setText (getLanguageHolder ().findDescription (getLanguages ().getSelectAdvisorScreen ().getTitle ()));
		getFrame ().setTitle (title.getText ());
		
		surveyorAction.putValue			(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSelectAdvisorScreen ().getSurveyor ()));
		cartographerAction.putValue	(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSelectAdvisorScreen ().getCartographer ()));
		apprenticeAction.putValue		(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSelectAdvisorScreen ().getApprentice ()));
		historianAction.putValue			(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSelectAdvisorScreen ().getHistorian ()));
		astrologerAction.putValue		(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSelectAdvisorScreen ().getAstrologer ()));
		chancellorAction.putValue		(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSelectAdvisorScreen ().getChancellor ()));
		taxCollectorAction.putValue		(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSelectAdvisorScreen ().getTaxCollector ()));
		grandVizierAction.putValue		(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSelectAdvisorScreen ().getGrandVizier ()));
		wizardsAction.putValue			(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSelectAdvisorScreen ().getWizards ()));
		
		// Shortcut keys
		getLanguageHolder ().configureShortcutKeys (contentPane);
	}
	
	/**
	 * @return Function key mappings, so the overland map UI can copy them
	 */
	public final ActionMap getActionMap ()
	{
		return contentPane.getActionMap ();
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
	 * @return Queued spells UI
	 */
	public final QueuedSpellsUI getQueuedSpellsUI ()
	{
		return queuedSpellsUI;
	}

	/**
	 * @param ui Queued spells UI
	 */
	public final void setQueuedSpellsUI (final QueuedSpellsUI ui)
	{
		queuedSpellsUI = ui;
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

	/**
	 * @return Wizards UI
	 */
	public final WizardsUI getWizardsUI ()
	{
		return wizardsUI;
	}

	/**
	 * @param ui Wizards UI
	 */
	public final void setWizardsUI (final WizardsUI ui)
	{
		wizardsUI = ui;
	}

	/**
	 * @return UI for screen showing power base history for each wizard
	 */
	public final HistoryUI getHistoryUI ()
	{
		return historyUI;
	}

	/**
	 * @param h UI for screen showing power base history for each wizard
	 */
	public final void setHistoryUI (final HistoryUI h)
	{
		historyUI = h;
	}
	
	/**
	 * @return Overland map right hand panel showing economy etc
	 */
	public final OverlandMapRightHandPanel getOverlandMapRightHandPanel ()
	{
		return overlandMapRightHandPanel;
	}

	/**
	 * @param panel Overland map right hand panel showing economy etc
	 */
	public final void setOverlandMapRightHandPanel (final OverlandMapRightHandPanel panel)
	{
		overlandMapRightHandPanel = panel;
	}
}