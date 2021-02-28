package momime.client.ui.frames;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.languages.database.Shortcut;
import momime.client.ui.MomUIConstants;
import momime.client.ui.components.MagicSlider;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.client.ui.renderer.MemoryMaintainedSpellTableCellRenderer;
import momime.client.utils.TextUtils;
import momime.common.calculations.SkillCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ProductionType;
import momime.common.database.Spell;
import momime.common.messages.MagicPowerDistribution;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.QueuedSpell;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.clienttoserver.UpdateMagicPowerDistributionMessage;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;

/**
 * Screen allowing setting magic power distribution into mana/research/skill, and viewing overland enchantments
 */
public final class MagicSlidersUI extends MomClientFrameUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (MagicSlidersUI.class);
	
	/** XML layout */
	private XmlLayoutContainerEx magicSlidersLayout;
	
	/** UI component factory */
	private UIComponentFactory uiComponentFactory;
	
	/** Large font */
	private Font largeFont;
	
	/** Small font */
	private Font smallFont;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Skill calculations */
	private SkillCalculations skillCalculations;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Alchemy UI */
	private AlchemyUI alchemyUI;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Renderer for the enchantments table */
	private MemoryMaintainedSpellTableCellRenderer memoryMaintainedSpellTableCellRenderer;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;

	/** Help text scroll */
	private HelpUI helpUI;
	
	/** Mana title above the slider */
	private JLabel manaTitle;
	
	/** Research title above the slider */
	private JLabel researchTitle;
	
	/** Skill title above the slider */
	private JLabel skillTitle;
	
	/** Mana slide control */
	private MagicSlider manaSlider;

	/** Research slide control */
	private MagicSlider researchSlider;

	/** Skill slide control */
	private MagicSlider skillSlider;

	/** Power base, before being split into mana/research/skill */
	private JLabel magicPowerPerTurn;

	/** Mana per turn according to current slider setting */
	private JProgressBar manaPerTurn;
	
	/** Research per turn according to current slider setting */
	private JProgressBar researchPerTurn;
	
	/** Skill per turn according to current slider setting */
	private JProgressBar skillPerTurn;
	
	/** Mana label below the progress bar */
	private JLabel manaLabel;
	
	/** Research label below the progress bar */
	private JLabel researchLabel;
	
	/** Skill label below the progress bar */
	private JLabel skillLabel;

	/** Spell currently being cast overland */
	private JLabel currentlyCasting;
	
	/** Spell currently being research */
	private JLabel currentlyResearching;
	
	/** Current casting skill */
	private JLabel castingSkill;
	
	/** Overland enchantments title */
	private JLabel overlandEnchantmentsTitle;
	
	/** Alchemy action */
	private Action alchemyAction;
	
	/** OK action */
	private Action okAction;
	
	/** Apply action */
	private Action applyAction;
	
	/** Last values sent to the server - so we don't bother resending if we just open up the screen and click OK without making changes */
	private MagicPowerDistribution lastValuesSentToServer;
	
	/** Items in the Enchantments box */
	private SpellsTableModel spellsTableModel;

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
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/magicSliders/background.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldPressed.png");
		
		// Actions
		alchemyAction = new LoggingAction ((ev) -> getAlchemyUI ().setVisible (true));
		
		applyAction = new LoggingAction ((ev) ->
		{
			// Do we need to inform the server of changed values?
			final MagicPowerDistribution dist = getClient ().getOurPersistentPlayerPrivateKnowledge ().getMagicPowerDistribution ();
			if ((lastValuesSentToServer == null) || (lastValuesSentToServer.getManaRatio () != dist.getManaRatio ()) ||
				(lastValuesSentToServer.getResearchRatio () != dist.getResearchRatio ()) || (lastValuesSentToServer.getSkillRatio () != dist.getSkillRatio ()))
			{
				// Remember what we're sending - take a full copy of it - don't just point at the same object
				lastValuesSentToServer = new MagicPowerDistribution ();
				lastValuesSentToServer.setManaRatio			(dist.getManaRatio ());
				lastValuesSentToServer.setResearchRatio	(dist.getResearchRatio ());
				lastValuesSentToServer.setSkillRatio			(dist.getSkillRatio ());
				
				// Send it
				final UpdateMagicPowerDistributionMessage msg = new UpdateMagicPowerDistributionMessage ();
				msg.setDistribution (dist);
				getClient ().getServerConnection ().sendMessageToServer (msg);
			
				// Update the mana per turn shown on the right hand panel of the overland map
				getOverlandMapRightHandPanel ().updateGlobalEconomyValues ();
				
				// We might not need to research a spell anymore if we set RP slider to nothing
				getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
			}
		});
		
		okAction = new LoggingAction ((ev) ->
		{
			applyAction.actionPerformed (ev);
			getFrame ().setVisible (false);
		});				
				
		// Set whether the 3 slides are enabled.
		// Even if a slider is unlocked, we only want to enable it if at least two sliders are unlocked.
		final List<MagicSlider> sliders = new ArrayList<MagicSlider> ();
		final Action lockAction = new LoggingAction ((ev) ->
		{
			int unlocked = 0;
			for (final MagicSlider slider : sliders)
				if (!slider.isLocked ())
					unlocked++;

			for (final MagicSlider slider : sliders)
				slider.getSlider ().setEnabled ((!slider.isLocked ()) && (unlocked >= 2));
		});

		final ChangeListener sliderChanged = new ChangeListener ()
		{
			/** Controls which of a pair of sliders gets the extra 1 in the case of splitting an odd value */
			boolean invertRounding = false;
			
			@Override
			public final void stateChanged (final ChangeEvent ev)
			{
				// Find the slider that was moved
				final JSlider thisSliderControl = (JSlider) ev.getSource ();
				MagicSlider thisSlider = null;

				// Do 3 things while looping over the sliders
				final List<MagicSlider> otherSliders = new ArrayList<MagicSlider> ();
				int adjustment = CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX;
				
				for (final MagicSlider slider : sliders)
				{
					// Find the slider that was moved
					if (slider.getSlider () == thisSliderControl)
						thisSlider = slider;
					
					// Find the other unlocked slider(s)
					else if ((!slider.isLocked ()) && (slider != thisSlider))
						otherSliders.add (slider);
				
					// How wrong are we?   We'll end up with a -ve value if >100% allocated, +ve if <100%
					adjustment = adjustment - slider.getValue ();
				}
				
				// How many sliders do we have available to make that adjustment to?
				if (adjustment != 0)
					switch (otherSliders.size ())
					{
						case 1:
							final MagicSlider otherSlider = otherSliders.get (0);
					
							// If reducing a value, it is impossible that there not be enough space in otherSlider to accept the additional value.
							// but if trying to increase a value, its possible the otherSlider is 0 or a low enough number that it doesn't have enough
							// spare value to accomodate the increase in which case we have to deny increasing this slider to the requested value,
							// and reduce it back down by a bit.
							int thisSliderValue = thisSlider.getValue ();
							int otherSliderValue = otherSlider.getValue () + adjustment;
					
							if (otherSliderValue < 0)
							{
								thisSliderValue = thisSliderValue + otherSliderValue;
								otherSliderValue = 0;		// really, = otherSliderValue - otherSliderValue
							}
					
							thisSlider.setValue (thisSliderValue);
							otherSlider.setValue (otherSliderValue);
							break;
						
						case 2:
							final MagicSlider otherSlider1 = otherSliders.get (0);
							final MagicSlider otherSlider2 = otherSliders.get (1);
						
							// When all 3 sliders are unlocked, we can't get a situation like above where we attempt to set a slider to a particular
							// value, but we have to deny that request - here the requested value always has to be valid (the slider can only be set
							// from 0..240, and whether we're increasing or decreasing it, there's always somewhere to take the value from/add it to).
						
							// So its just a case of making sure otherSlider1 & 2 don't go out of range, and again its impossible to have a problem with
							// reducing a value, the only problem is we're increasing this value, and in taking 1/2 of that value from one of the other
							// 2 sliders, we set it negative.
						
							// invertRounding is an attempt at covering up rounding errors caused by halving odd numbers, by flipping which slider
							// gets the extra 1 from the rounding error each time.
							int otherSlider1Value;
							int otherSlider2Value;
						
							if (invertRounding)
							{
								otherSlider1Value = otherSlider1.getValue () + (adjustment - (adjustment / 2));
								otherSlider2Value = otherSlider2.getValue () + (adjustment / 2);
							}
							else
							{
								otherSlider1Value = otherSlider1.getValue () + (adjustment / 2);
								otherSlider2Value = otherSlider2.getValue () + (adjustment - (adjustment / 2));
							}
							invertRounding = !invertRounding;
						
							if (otherSlider1Value < 0)
							{
								otherSlider2Value = otherSlider2Value + otherSlider1Value;
								otherSlider1Value = 0;	// really, = otherSlider1Value - otherSlider1Value
							}
						
							if (otherSlider2Value < 0)
							{
								otherSlider1Value = otherSlider1Value + otherSlider2Value;
								otherSlider2Value = 0;	// really, = otherSlider2Value - otherSlider2Value
							}
						
							otherSlider1.setValue (otherSlider1Value);
							otherSlider2.setValue (otherSlider2Value);
							break;
						
						default:
							// This should be impossible - there can't be 3 other sliders, and if only 1 is unlocked
							// then they're all disabled and no event can be triggered
							log.error ("sliderChanged triggered with otherSliders.size () = " + otherSliders.size ());
					}
			
				// Update actual power distribution
				final MagicPowerDistribution dist = getClient ().getOurPersistentPlayerPrivateKnowledge ().getMagicPowerDistribution ();
				dist.setManaRatio		(manaSlider.getValue ());
				dist.setResearchRatio	(researchSlider.getValue ());
				dist.setSkillRatio			(skillSlider.getValue ());
				
				// Update labels				
				updateProductionLabels ();
			}
		};
		
		// Initialize the content pane
		contentPane = getUtils ().createPanelWithBackgroundImage (background);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getMagicSlidersLayout ()));

		// Big heading showing power base at the top
		magicPowerPerTurn = getUtils ().createShadowedLabel (MomUIConstants.SILVER.darker (), MomUIConstants.SILVER, getLargeFont ());
		contentPane.add (magicPowerPerTurn, "frmMagicPowerBase");

		// Column headings
		overlandEnchantmentsTitle = getUtils ().createShadowedLabel (MomUIConstants.DULL_AQUA.darker (), MomUIConstants.DULL_AQUA, getLargeFont ());
		contentPane.add (overlandEnchantmentsTitle, "frmMagicOverlandEnchantmentsLabel");
		
		manaTitle = getUtils ().createShadowedLabel (MomUIConstants.DULL_GOLD.darker (), MomUIConstants.DULL_GOLD, getLargeFont ());
		contentPane.add (manaTitle, "frmMagicManaLabel");
		
		researchTitle = getUtils ().createShadowedLabel (MomUIConstants.GREEN.darker (), MomUIConstants.GREEN, getLargeFont ());
		contentPane.add (researchTitle, "frmMagicResearchLabel");
		
		skillTitle = getUtils ().createShadowedLabel (MomUIConstants.RED.darker (), MomUIConstants.RED, getLargeFont ());
		contentPane.add (skillTitle, "frmMagicSkillLabel");
		
		// The actual sliders
		final MagicPowerDistribution dist = getClient ().getOurPersistentPlayerPrivateKnowledge ().getMagicPowerDistribution ();
		
		manaSlider = getUiComponentFactory ().createMagicSlider ();
		manaSlider.init ("mana", lockAction, dist.getManaRatio ());
		contentPane.add (manaSlider, "frmMagicManaSlider");
		sliders.add (manaSlider);
		
		researchSlider = getUiComponentFactory ().createMagicSlider ();
		researchSlider.init ("research", lockAction, dist.getResearchRatio ());
		contentPane.add (researchSlider, "frmMagicResearchSlider");
		sliders.add (researchSlider);

		skillSlider = getUiComponentFactory ().createMagicSlider ();
		skillSlider.init ("skill", lockAction, dist.getSkillRatio ());
		contentPane.add (skillSlider, "frmMagicSkillSlider");
		sliders.add (skillSlider);
		
		for (final MagicSlider slider : sliders)
			slider.getSlider ().addChangeListener (sliderChanged);

		// Progress bars underneath.
		manaPerTurn = new JProgressBar ();
		manaPerTurn.setStringPainted (true);
		manaPerTurn.setFont (getSmallFont ());
		manaPerTurn.setForeground (MomUIConstants.DULL_GOLD);
		manaPerTurn.setBackground (MomUIConstants.TRANSPARENT);
		contentPane.add (manaPerTurn, "frmMagicManaProgress");
		
		researchPerTurn = new JProgressBar ();
		researchPerTurn.setStringPainted (true);
		researchPerTurn.setFont (getSmallFont ());
		researchPerTurn.setForeground (MomUIConstants.GREEN);
		researchPerTurn.setBackground (MomUIConstants.TRANSPARENT);
		contentPane.add (researchPerTurn, "frmMagicResearchProgress");

		skillPerTurn = new JProgressBar ();
		skillPerTurn.setStringPainted (true);
		skillPerTurn.setFont (getSmallFont ());
		skillPerTurn.setForeground (MomUIConstants.RED);
		skillPerTurn.setBackground (MomUIConstants.TRANSPARENT);
		contentPane.add (skillPerTurn, "frmMagicSkillProgress");

		// Change the colour of the progress bars.
		// This is horrid that it can't be done against the individual progress bars, and we have to do it to ALL progress bars in the application,
		// but there appears to be no way to do it.  The underlying ProgressBarPainter is final, package private, with an awkward constructor.
		UIManager.put ("nimbusOrange", MomUIConstants.LIGHT_BROWN);
		
		// Labels underneath
		manaLabel = getUtils ().createLabel (MomUIConstants.DULL_GOLD, getSmallFont ());
		contentPane.add (manaLabel, "frmMagicManaCastingNowHeading");
		
		researchLabel = getUtils ().createLabel (MomUIConstants.GREEN, getSmallFont ());
		contentPane.add (researchLabel, "frmMagicResearchingHeading");
		
		skillLabel = getUtils ().createLabel (MomUIConstants.RED, getSmallFont ());
		contentPane.add (skillLabel, "frmMagicCastingSkillHeading");

		currentlyCasting = getUtils ().createLabel (MomUIConstants.DULL_GOLD, getSmallFont ());
		contentPane.add (currentlyCasting, "frmMagicCastingSpellName");
		
		currentlyResearching = getUtils ().createLabel (MomUIConstants.GREEN, getSmallFont ());
		contentPane.add (currentlyResearching, "frmMagicResearchingSpellName");
		
		castingSkill = getUtils ().createLabel (MomUIConstants.RED, getSmallFont ());
		contentPane.add (castingSkill, "frmMagicCastingSkill");

		// Buttons
		contentPane.add (getUtils ().createImageButton (alchemyAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmMagicAlchemy");
		
		contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmMagicOK");

		contentPane.add (getUtils ().createImageButton (applyAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmMagicApply");
		
		// Overland enchantments grid
		spellsTableModel = new SpellsTableModel ();
		final JTable spellsTable = new JTable ();
		spellsTable.setOpaque (false);
		spellsTable.setModel (spellsTableModel);
		spellsTable.setDefaultRenderer (MemoryMaintainedSpell.class, getMemoryMaintainedSpellTableCellRenderer ());
		spellsTable.setRowHeight (getUtils ().loadImage ("/momime.client.graphics/ui/mirror/mirror.png").getHeight ());
		spellsTable.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		spellsTable.setTableHeader (null);

		final JScrollPane spellsScrollPane = getUtils ().createTransparentScrollPane (spellsTable);
		contentPane.add (spellsScrollPane, "frmMagicOverlandEnchantments");

		spellsChanged ();
		
		// Clicking a spell asks about cancelling it
		final MouseListener spellSelectionListener = new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				final int col = spellsTable.columnAtPoint (ev.getPoint ());
				final int row = spellsTable.rowAtPoint (ev.getPoint ());
				final int index = (row * 2) + col;
				
				if ((index >= 0) && (index < spellsTableModel.getSpells ().size ()))
					try
					{
						final MemoryMaintainedSpell spell = spellsTableModel.getSpells ().get (index);
						if (SwingUtilities.isRightMouseButton (ev))
						{
							// Right clicking on spells gets help text about them
							getHelpUI ().showSpellID (spell.getSpellID (),
								getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), spell.getCastingPlayerID (), "OverlandEnchantmentHelpText"));
						}
						else if (spell.getCastingPlayerID () == getClient ().getOurPlayerID ())
						{
							final String spellName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell (spell.getSpellID (), "MagicSlidersUI").getSpellName ());
							
							final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
							msg.setLanguageTitle (getLanguages ().getSpellCasting ().getSwitchOffSpellTitle ());
							msg.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellCasting ().getSwitchOffSpell ()).replaceAll ("SPELL_NAME", spellName));
							msg.setSwitchOffSpell (spell);
							msg.setVisible (true);
						}
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
			}
		};
		spellsTable.addMouseListener (spellSelectionListener);
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		
		// Shortcut keys
		contentPane.getActionMap ().put (Shortcut.ALCHEMY, alchemyAction);
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		getFrame ().setTitle (getLanguageHolder ().findDescription (getLanguages ().getMagicSlidersScreen ().getTitle ()));
		
		manaTitle.setText		(getLanguageHolder ().findDescription (getLanguages ().getMagicSlidersScreen ().getManaTitle ()));
		researchTitle.setText	(getLanguageHolder ().findDescription (getLanguages ().getMagicSlidersScreen ().getResearchTitle ()));
		skillTitle.setText			(getLanguageHolder ().findDescription (getLanguages ().getMagicSlidersScreen ().getSkillTitle ()));
		manaLabel.setText		(getLanguageHolder ().findDescription (getLanguages ().getMagicSlidersScreen ().getManaLabel ()) + ":");
		researchLabel.setText	(getLanguageHolder ().findDescription (getLanguages ().getMagicSlidersScreen ().getResearchLabel ()) + ":");
		skillLabel.setText		(getLanguageHolder ().findDescription (getLanguages ().getMagicSlidersScreen ().getSkillLabel ()) + ":");
		
		overlandEnchantmentsTitle.setText (getLanguageHolder ().findDescription (getLanguages ().getMagicSlidersScreen ().getOverlandEnchantments ()));
		alchemyAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getMagicSlidersScreen ().getAlchemy ()));
		okAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getOk ()));
		applyAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getMagicSlidersScreen ().getApply ()));
		
		updateProductionLabels ();
		
		// Shortcut keys
		getLanguageHolder ().configureShortcutKeys (contentPane);
	}
	
	/**
	 * Shows the actual mana/search/skill points we'll get according to the current slider positions and our current power base
	 */
	public final void updateProductionLabels ()
	{
		// This can be called before the screen has been opened and the UI components exist
		if (manaPerTurn != null)
			try
			{
				// Use the real calc routine to work out how much MP/RP/SP we'll actually get, because this takes everything into
				// account such as getting a bonus to research if we've got a lot of spell books in the magic realm of the spell currently
				// being researched; and retorts like Archmage that give a bonus to mana spent on improving skill
				final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "updatePerTurnLabels");
				final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ourPlayer.getPersistentPlayerPublicKnowledge ();
		
				final int manaPerTurnValue = getResourceValueUtils ().calculateAmountPerTurnForProductionType
					(getClient ().getOurPersistentPlayerPrivateKnowledge (), pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());

				final int researchPerTurnValue = getResourceValueUtils ().calculateAmountPerTurnForProductionType
					(getClient ().getOurPersistentPlayerPrivateKnowledge (), pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());

				final int skillPerTurnValue = getResourceValueUtils ().calculateAmountPerTurnForProductionType
					(getClient ().getOurPersistentPlayerPrivateKnowledge (), pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());

				final int magicPowerPerTurnValue = getResourceValueUtils ().calculateAmountPerTurnForProductionType
					(getClient ().getOurPersistentPlayerPrivateKnowledge (), pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());
			
				// Update the per turn labels
				final ProductionType manaProduction = getClient ().getClientDB ().findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "updateProductionLabels");
				manaPerTurn.setString (getTextUtils ().intToStrCommas (manaPerTurnValue) + " " + getLanguageHolder ().findDescription (manaProduction.getProductionTypeSuffix ()));
		
				final ProductionType researchProduction = getClient ().getClientDB ().findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, "updateProductionLabels");
				researchPerTurn.setString (getTextUtils ().intToStrCommas (researchPerTurnValue) + " " + getLanguageHolder ().findDescription (researchProduction.getProductionTypeSuffix ()));
		
				final ProductionType skillProduction = getClient ().getClientDB ().findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, "updateProductionLabels");
				skillPerTurn.setString (getTextUtils ().intToStrCommas (skillPerTurnValue) + " " + getLanguageHolder ().findDescription (skillProduction.getProductionTypeSuffix ()));

				magicPowerPerTurn.setText (getLanguageHolder ().findDescription (getLanguages ().getMagicSlidersScreen ().getPowerBase ()).replaceAll
					("AMOUNT_PER_TURN", getTextUtils ().intToStrCommas (magicPowerPerTurnValue)));				
			
				// Update casting skill label
				final int currentSkill = getResourceValueUtils ().calculateCastingSkillOfPlayer (getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue ());
				castingSkill.setText (getTextUtils ().intToStrCommas (currentSkill));
			
				// Skill progress bar
				skillPerTurn.setMaximum (getSkillCalculations ().getSkillPointsRequiredToImproveSkillFrom (currentSkill));
				skillPerTurn.setValue (getResourceValueUtils ().findAmountStoredForProductionType
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT) -
					getSkillCalculations ().getSkillPointsRequiredForCastingSkill (currentSkill));		// i.e. current skill pts - what we needed to attain our current skill level
			
				// Spell being researched
				if (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched () == null)
				{
					researchPerTurn.setMaximum (100);
					researchPerTurn.setValue (0);
					currentlyResearching.setText (getLanguageHolder ().findDescription (getLanguages ().getMagicSlidersScreen ().getResearchingNothing ()));
				}
				else
				{
					final Spell spell = getClient ().getClientDB ().findSpell
						(getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched (), "updateProductionLabels (r)");
					final String spellName = getLanguageHolder ().findDescription (spell.getSpellName ());
					currentlyResearching.setText (spellName);
				
					final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellResearchStatus (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ());
					final int totalResearchCost = spell.getResearchCost ();
				
					researchPerTurn.setMaximum (totalResearchCost);
					researchPerTurn.setValue (totalResearchCost - researchStatus.getRemainingResearchCost ());
				}
				
				// Spell being cast
				if (getClient ().getOurPersistentPlayerPrivateKnowledge ().getQueuedSpell ().size () == 0)
				{
					manaPerTurn.setMaximum (100);
					manaPerTurn.setValue (0);
					currentlyCasting.setText (getLanguageHolder ().findDescription (getLanguages ().getMagicSlidersScreen ().getResearchingNothing ()));
				}
				else
				{
					final QueuedSpell queued = getClient ().getOurPersistentPlayerPrivateKnowledge ().getQueuedSpell ().get (0);
					final Spell spellBeingCast = getClient ().getClientDB ().findSpell (queued.getQueuedSpellID (), "updateProductionLabels (c)");
					
					if (queued.getHeroItem () != null)
						currentlyCasting.setText (queued.getHeroItem ().getHeroItemName ());
					else
						currentlyCasting.setText (getLanguageHolder ().findDescription (spellBeingCast.getSpellName ()));

					manaPerTurn.setMaximum (getSpellUtils ().getReducedOverlandCastingCost
						(spellBeingCast, queued.getHeroItem (), queued.getVariableDamage (),
							pub.getPick (), getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ()));
					manaPerTurn.setValue (getClient ().getOurPersistentPlayerPrivateKnowledge ().getManaSpentOnCastingCurrentSpell ());					
				}
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
	}	

	/**
	 * Update the list of overland enchantments whenever they change
	 */
	public final void spellsChanged ()
	{
		if (spellsTableModel != null)
		{
			spellsTableModel.getSpells ().clear ();
			for (final MemoryMaintainedSpell spell : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ())
				if ((spell.getCityLocation () == null) && (spell.getUnitURN () == null))
					spellsTableModel.getSpells ().add (spell);
			
			spellsTableModel.fireTableDataChanged ();
		}
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getMagicSlidersLayout ()
	{
		return magicSlidersLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setMagicSlidersLayout (final XmlLayoutContainerEx layout)
	{
		magicSlidersLayout = layout;
	}

	/**
	 * @return UI component factory
	 */
	public final UIComponentFactory getUiComponentFactory ()
	{
		return uiComponentFactory;
	}

	/**
	 * @param factory UI component factory
	 */
	public final void setUiComponentFactory (final UIComponentFactory factory)
	{
		uiComponentFactory = factory;
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
	 * @return Text utils
	 */
	public final TextUtils getTextUtils ()
	{
		return textUtils;
	}

	/**
	 * @param tu Text utils
	 */
	public final void setTextUtils (final TextUtils tu)
	{
		textUtils = tu;
	}

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param util Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils util)
	{
		resourceValueUtils = util;
	}

	/**
	 * @return Skill calculations
	 */
	public final SkillCalculations getSkillCalculations ()
	{
		return skillCalculations;
	}

	/**
	 * @param calc Skill calculations
	 */
	public final void setSkillCalculations (final SkillCalculations calc)
	{
		skillCalculations = calc;
	}

	/**
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
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
	 * @return Alchemy UI
	 */
	public final AlchemyUI getAlchemyUI ()
	{
		return alchemyUI;
	}

	/**
	 * @param ui Alchemy UI
	 */
	public final void setAlchemyUI (final AlchemyUI ui)
	{
		alchemyUI = ui;
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

	/**
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}

	/**
	 * @return Renderer for the enchantments table
	 */
	public final MemoryMaintainedSpellTableCellRenderer getMemoryMaintainedSpellTableCellRenderer ()
	{
		return memoryMaintainedSpellTableCellRenderer;
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

	/**
	 * @return Help text scroll
	 */
	public final HelpUI getHelpUI ()
	{
		return helpUI;
	}

	/**
	 * @param ui Help text scroll
	 */
	public final void setHelpUI (final HelpUI ui)
	{
		helpUI = ui;
	}
	
	/**
	 * @param renderer Renderer for the enchantments table
	 */
	public final void setMemoryMaintainedSpellTableCellRenderer (final MemoryMaintainedSpellTableCellRenderer renderer)
	{
		memoryMaintainedSpellTableCellRenderer = renderer;
	}
	
	/**
	 * Table model for displaying the overland enchantments grid
	 */
	private class SpellsTableModel extends AbstractTableModel
	{
		/** Underlying storage */
		private List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		/**
		 * @return Underlying storage
		 */
		public final List<MemoryMaintainedSpell> getSpells ()
		{
			return spells;
		}
		
		/**
		 * @return Enough rows to fit however many spells we have to draw, rounding up
		 */
		@Override
		public final int getRowCount ()
		{
			return (spells.size () + 1) / 2;
		}

		/**
		 * @return Fixed at 2 columns
		 */
		@Override
		public final int getColumnCount ()
		{
			return 2;
		}

		/**
		 * @return Spell to display at the specified grid location
		 */
		@Override
		public final Object getValueAt (final int rowIndex, final int columnIndex)
		{
			final int index = (rowIndex * 2) + columnIndex;
			return ((index < 0) || (index >= spells.size ())) ? null : spells.get (index);
		}

		/**
		 * @return Columns are all spells
		 */
		@Override
		public final Class<?> getColumnClass (@SuppressWarnings ("unused") final int columnIndex)
		{
			return MemoryMaintainedSpell.class;
		}
	}
}