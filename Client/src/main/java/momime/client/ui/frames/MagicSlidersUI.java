package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import momime.client.MomClient;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.language.database.v0_9_5.Spell;
import momime.client.ui.MomUIConstants;
import momime.client.ui.components.MagicSlider;
import momime.client.ui.components.UIComponentFactory;
import momime.client.utils.TextUtils;
import momime.common.calculations.MomSkillCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.clienttoserver.v0_9_5.UpdateMagicPowerDistributionMessage;
import momime.common.messages.v0_9_5.MagicPowerDistribution;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.SpellResearchStatus;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.GridBagConstraintsHorizontalFill;
import com.ndg.swing.GridBagConstraintsNoFill;

/**
 * Screen allowing setting magic power distribution into mana/research/skill, and viewing overland enchantments
 */
public final class MagicSlidersUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MagicSlidersUI.class);

	/** Typical inset used on this screen layout */
	private final static int INSET = 0;
	
	/** Inset used to space the sliders correctly */
	private final static int TINY_INSET = 1;
	
	/** The width of the progress bars */
	private final static int PROGRESS_BAR_WIDTH = 92;
	
	/** The width of the main 3 columns */
	private final static int COLUMN_WIDTH = PROGRESS_BAR_WIDTH + TINY_INSET + TINY_INSET;
	
	/** How much we chop off the side of the numeric labels either side of the name of the spell currently being researched */
	private final static int RESEARCH_NAME_SPACERS = 30;
	
	/** How wide the MP/SP labels are, either side of the research name */
	private final static int RESEARCH_NAME_OUTER_LABELS_WIDTH = COLUMN_WIDTH - RESEARCH_NAME_SPACERS - RESEARCH_NAME_SPACERS;
	
	/** How wide the research name itself is */
	private final static int RESEARCH_NAME_WIDTH = COLUMN_WIDTH + RESEARCH_NAME_SPACERS + RESEARCH_NAME_SPACERS;
	
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
	private MomSkillCalculations skillCalculations;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Alchemy UI */
	private AlchemyUI alchemyUI;
	
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
	
	/** Mana label below the slider */
	private JLabel manaLabel;
	
	/** Research progress bar below the slider */
	private JLabel researchLabel;
	
	/** Skill progress bar below the slider */
	private JLabel skillLabel;

	/** Mana stored */
	private JLabel manaStored;
	
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
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/magicSliders/background.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldPressed.png");
		
		// Actions
		alchemyAction = new AbstractAction ()
		{
			private static final long serialVersionUID = 1986507984605842240L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getAlchemyUI ().setVisible (true);
				}
				catch (final IOException e)
				{
					log.error (e, e);
				}
			}
		}; 
		
		applyAction = new AbstractAction ()
		{
			private static final long serialVersionUID = 3281074290844172318L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
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
					try
					{
						getClient ().getServerConnection ().sendMessageToServer (msg);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				}
			}
		};
		
		okAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -5463992577360555129L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				applyAction.actionPerformed (ev);
				
				// Hide the screen
				getFrame ().setVisible (false);
			}
		};				
				
		// Set whether the 3 slides are enabled.
		// Even if a slider is unlocked, we only want to enable it if at least two sliders are unlocked.
		final List<MagicSlider> sliders = new ArrayList<MagicSlider> ();
		final Action lockAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -9035882679704578423L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				int unlocked = 0;
				for (final MagicSlider slider : sliders)
					if (!slider.isLocked ())
						unlocked++;

				for (final MagicSlider slider : sliders)
					slider.getSlider ().setEnabled ((!slider.isLocked ()) && (unlocked >= 2));
			}
		};

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
		final JPanel contentPane = getUtils ().createPanelWithBackgroundImage (background);
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
		// Column headings
		magicPowerPerTurn = getUtils ().createShadowedLabel (MomUIConstants.SILVER.darker (), MomUIConstants.SILVER, getLargeFont ());
		contentPane.add (magicPowerPerTurn, getUtils ().createConstraintsNoFill (0, 0, 3, 1, new Insets (0, 0, 4, 0), GridBagConstraintsNoFill.CENTRE));

		overlandEnchantmentsTitle = getUtils ().createShadowedLabel (MomUIConstants.DULL_AQUA.darker (), MomUIConstants.DULL_AQUA, getLargeFont ());
		contentPane.add (overlandEnchantmentsTitle, getUtils ().createConstraintsNoFill (3, 0, 1, 1, new Insets (0, 35, 4, 0), GridBagConstraintsNoFill.NORTHWEST));
		
		manaTitle = getUtils ().createShadowedLabel (MomUIConstants.DULL_GOLD.darker (), MomUIConstants.DULL_GOLD, getLargeFont ());
		contentPane.add (manaTitle, getUtils ().createConstraintsNoFill (0, 1, 1, 1, new Insets (13, 0, 0, 0), GridBagConstraintsNoFill.NORTH));
		
		researchTitle = getUtils ().createShadowedLabel (MomUIConstants.GREEN.darker (), MomUIConstants.GREEN, getLargeFont ());
		contentPane.add (researchTitle, getUtils ().createConstraintsNoFill (1, 1, 1, 1, new Insets (13, 0, 0, 0), GridBagConstraintsNoFill.NORTH));
		
		skillTitle = getUtils ().createShadowedLabel (MomUIConstants.RED.darker (), MomUIConstants.RED, getLargeFont ());
		contentPane.add (skillTitle, getUtils ().createConstraintsNoFill (2, 1, 1, 1, new Insets (13, 0, 0, 0), GridBagConstraintsNoFill.NORTH));
		
		// The actual sliders
		final MagicPowerDistribution dist = getClient ().getOurPersistentPlayerPrivateKnowledge ().getMagicPowerDistribution ();
		
		manaSlider = getUiComponentFactory ().createMagicSlider ();
		manaSlider.init ("mana", lockAction, dist.getManaRatio ());
		contentPane.add (manaSlider, getUtils ().createConstraintsNoFill (0, 2, 1, 1, new Insets (6, 0, 7, 0), GridBagConstraintsNoFill.CENTRE));
		sliders.add (manaSlider);
		
		researchSlider = getUiComponentFactory ().createMagicSlider ();
		researchSlider.init ("research", lockAction, dist.getResearchRatio ());
		contentPane.add (researchSlider, getUtils ().createConstraintsNoFill (1, 2, 1, 1, new Insets (6, 0, 7, 0), GridBagConstraintsNoFill.CENTRE));
		sliders.add (researchSlider);

		skillSlider = getUiComponentFactory ().createMagicSlider ();
		skillSlider.init ("skill", lockAction, dist.getSkillRatio ());
		contentPane.add (skillSlider, getUtils ().createConstraintsNoFill (2, 2, 1, 1, new Insets (6, 0, 7, 0), GridBagConstraintsNoFill.CENTRE));
		sliders.add (skillSlider);
		
		for (final MagicSlider slider : sliders)
			slider.getSlider ().addChangeListener (sliderChanged);

		// Progress bars underneath.
		// We don't actually display a progress bar for manaPerTurn, but defined it as one to make the spacing consistent
		final Dimension progressBarSize = new Dimension (PROGRESS_BAR_WIDTH, 22);
		
		manaPerTurn = new JProgressBar ();
		manaPerTurn.setStringPainted (true);
		manaPerTurn.setFont (getSmallFont ());
		manaPerTurn.setForeground (MomUIConstants.DULL_GOLD);
		manaPerTurn.setBackground (MomUIConstants.TRANSPARENT);
		manaPerTurn.setMinimumSize (progressBarSize);
		manaPerTurn.setMaximumSize (progressBarSize);
		manaPerTurn.setPreferredSize (progressBarSize);
		contentPane.add (manaPerTurn, getUtils ().createConstraintsNoFill (0, 3, 1, 1, TINY_INSET, GridBagConstraintsNoFill.CENTRE));
		
		researchPerTurn = new JProgressBar ();
		researchPerTurn.setStringPainted (true);
		researchPerTurn.setFont (getSmallFont ());
		researchPerTurn.setForeground (MomUIConstants.GREEN);
		researchPerTurn.setValue (100);
		researchPerTurn.setBackground (MomUIConstants.TRANSPARENT);
		researchPerTurn.setMinimumSize (progressBarSize);
		researchPerTurn.setMaximumSize (progressBarSize);
		researchPerTurn.setPreferredSize (progressBarSize);
		contentPane.add (researchPerTurn, getUtils ().createConstraintsNoFill (1, 3, 1, 1, TINY_INSET, GridBagConstraintsNoFill.CENTRE));

		skillPerTurn = new JProgressBar ();
		skillPerTurn.setStringPainted (true);
		skillPerTurn.setFont (getSmallFont ());
		skillPerTurn.setForeground (MomUIConstants.RED);
		skillPerTurn.setBackground (MomUIConstants.TRANSPARENT);
		skillPerTurn.setMinimumSize (progressBarSize);
		skillPerTurn.setMaximumSize (progressBarSize);
		skillPerTurn.setPreferredSize (progressBarSize);
		contentPane.add (skillPerTurn, getUtils ().createConstraintsNoFill (2, 3, 1, 1, TINY_INSET, GridBagConstraintsNoFill.CENTRE));

		// Make the progress bars gold coloured.
		// This is horrid that it can't be done against the individual progress bars, and we have to do it to ALL progress bars in the application,
		// but there appears to be no way to do it.  The underlying ProgressBarPainter is final, package private, with an awkward constructor.
		UIManager.put ("nimbusOrange", MomUIConstants.DULL_GOLD);
		
		// Labels underneath
		manaLabel = getUtils ().createLabel (MomUIConstants.DULL_GOLD, getSmallFont ());
		contentPane.add (manaLabel, getUtils ().createConstraintsNoFill (0, 4, 1, 1, new Insets (12, 0, 2, 0), GridBagConstraintsNoFill.CENTRE));
		
		researchLabel = getUtils ().createLabel (MomUIConstants.GREEN, getSmallFont ());
		contentPane.add (researchLabel, getUtils ().createConstraintsNoFill (1, 4, 1, 1, new Insets (12, 0, 2, 0), GridBagConstraintsNoFill.CENTRE));
		
		skillLabel = getUtils ().createLabel (MomUIConstants.RED, getSmallFont ());
		contentPane.add (skillLabel, getUtils ().createConstraintsNoFill (2, 4, 1, 1, new Insets (12, 0, 2, 0), GridBagConstraintsNoFill.CENTRE));

		// Some of the spell names are long enough to overflow into the numeric labels on either side, and we want that to happen rather than
		// displaying "..." and chopping the label off, so the only way to do it is to swap out that row of 3 columns with a special panel and
		// fix the sizes so that everything still lines up centred under the main columns, so its a bit of a hack but is the only way I could think to make this look right
		final JPanel researchNamePanel = new JPanel ();
		researchNamePanel.setLayout (new GridBagLayout ());
		researchNamePanel.setOpaque (false);
		contentPane.add (researchNamePanel, getUtils ().createConstraintsNoFill (0, 5, 3, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		final Dimension outerLabelsSize = new Dimension (RESEARCH_NAME_OUTER_LABELS_WIDTH, 22);
		final Dimension researchNameLabelsSize = new Dimension (RESEARCH_NAME_WIDTH, 22);
		
		researchNamePanel.add (Box.createRigidArea (new Dimension (RESEARCH_NAME_SPACERS, 0)),
			getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		manaStored = getUtils ().createLabel (MomUIConstants.DULL_GOLD, getSmallFont ());
		manaStored.setMinimumSize (outerLabelsSize);
		manaStored.setMaximumSize (outerLabelsSize);
		manaStored.setPreferredSize (outerLabelsSize);
		manaStored.setHorizontalAlignment (SwingConstants.CENTER);
		researchNamePanel.add (manaStored, getUtils ().createConstraintsNoFill (1, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		currentlyResearching = getUtils ().createLabel (MomUIConstants.GREEN, getSmallFont ());
		currentlyResearching.setMinimumSize (researchNameLabelsSize);
		currentlyResearching.setMaximumSize (researchNameLabelsSize);
		currentlyResearching.setPreferredSize (researchNameLabelsSize);
		currentlyResearching.setHorizontalAlignment (SwingConstants.CENTER);
		researchNamePanel.add (currentlyResearching, getUtils ().createConstraintsNoFill (2, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		castingSkill = getUtils ().createLabel (MomUIConstants.RED, getSmallFont ());
		castingSkill.setMinimumSize (outerLabelsSize);
		castingSkill.setMaximumSize (outerLabelsSize);
		castingSkill.setPreferredSize (outerLabelsSize);
		castingSkill.setHorizontalAlignment (SwingConstants.CENTER);
		researchNamePanel.add (castingSkill, getUtils ().createConstraintsNoFill (3, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		researchNamePanel.add (Box.createRigidArea (new Dimension (RESEARCH_NAME_SPACERS, 0)),
			getUtils ().createConstraintsNoFill (4, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		// The power base label and buttons are also 3 items, but there's no reason for them to be lined up over the above columns, so put them into their own panel
		final JPanel lowerPanel = new JPanel ();
		lowerPanel.setLayout (new GridBagLayout ());
		lowerPanel.setOpaque (false);
		contentPane.add (lowerPanel, getUtils ().createConstraintsHorizontalFill (0, 6, 3, 1, INSET, GridBagConstraintsHorizontalFill.CENTRE));

		lowerPanel.add (getUtils ().createImageButton (alchemyAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.WEST));
		
		// Put all the spare space in the middle
		final GridBagConstraints spaceConstraints = getUtils ().createConstraintsHorizontalFill (1, 0, 1, 1, INSET, GridBagConstraintsHorizontalFill.CENTRE);
		spaceConstraints.weightx = 1;
		
		lowerPanel.add (Box.createRigidArea (new Dimension (0, 0)), spaceConstraints);
		
		lowerPanel.add (getUtils ().createImageButton (okAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), getUtils ().createConstraintsNoFill (2, 0, 1, 1, INSET, GridBagConstraintsNoFill.EAST));

		lowerPanel.add (getUtils ().createImageButton (applyAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), getUtils ().createConstraintsNoFill (3, 0, 1, 1, new Insets (0, 6, 0, 0), GridBagConstraintsNoFill.EAST));
		
		// Leave a space for the overland enchantments grid when we implement it
		final Dimension overlandEnchantmentsSize = new Dimension (272, 430);
		
		final JPanel overlandEnchantments = new JPanel ();
		overlandEnchantments.setOpaque (false);
		overlandEnchantments.setMinimumSize (overlandEnchantmentsSize);
		overlandEnchantments.setMaximumSize (overlandEnchantmentsSize);
		overlandEnchantments.setPreferredSize (overlandEnchantmentsSize);
		
		contentPane.add (overlandEnchantments, getUtils ().createConstraintsNoFill (3, 1, 1, 6, new Insets (0, 35, 6, 4), GridBagConstraintsNoFill.CENTRE));		

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
		log.trace ("Entering languageChanged");
		
		getFrame ().setTitle (getLanguage ().findCategoryEntry ("frmMagicSliders", "Title"));
		
		manaTitle.setText		(getLanguage ().findCategoryEntry ("frmMagicSliders", "ManaTitle"));
		researchTitle.setText	(getLanguage ().findCategoryEntry ("frmMagicSliders", "ResearchTitle"));
		skillTitle.setText			(getLanguage ().findCategoryEntry ("frmMagicSliders", "SkillTitle"));
		manaLabel.setText		(getLanguage ().findCategoryEntry ("frmMagicSliders", "ManaLabel") + ":");
		researchLabel.setText	(getLanguage ().findCategoryEntry ("frmMagicSliders", "ResearchLabel") + ":");
		skillLabel.setText		(getLanguage ().findCategoryEntry ("frmMagicSliders", "SkillLabel") + ":");
		
		overlandEnchantmentsTitle.setText (getLanguage ().findCategoryEntry ("frmMagicSliders", "OverlandEnchantments"));
		alchemyAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMagicSliders", "Alchemy"));
		okAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMagicSliders", "OK"));
		applyAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMagicSliders", "Apply"));
		
		updateProductionLabels ();
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * Shows the actual mana/search/skill points we'll get according to the current slider positions and our current power base
	 */
	public final void updateProductionLabels ()
	{
		log.trace ("Entering updateProductionLabels");
		
		// This can be called before the screen has been opened and the UI components exist
		if (manaPerTurn != null)
			try
			{
				// Use the real calc routine to work out how much MP/RP/SP we'll actually get, because this takes everything into
				// account such as getting a bonus to research if we've got a lot of spell books in the magic realm of the spell currently
				// being researched; and retorts like Archmage that give a bonus to mana spent on improving skill
				final PlayerPublicDetails ourPlayer = MultiplayerSessionUtils.findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "updatePerTurnLabels");
				final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ourPlayer.getPersistentPlayerPublicKnowledge ();
		
				final int manaPerTurnValue = getResourceValueUtils ().calculateAmountPerTurnForProductionType
					(getClient ().getOurPersistentPlayerPrivateKnowledge (), pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());

				final int researchPerTurnValue = getResourceValueUtils ().calculateAmountPerTurnForProductionType
					(getClient ().getOurPersistentPlayerPrivateKnowledge (), pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());

				final int skillPerTurnValue = getResourceValueUtils ().calculateAmountPerTurnForProductionType
					(getClient ().getOurPersistentPlayerPrivateKnowledge (), pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());

				final int magicPowerPerTurnValue = getResourceValueUtils ().calculateAmountPerTurnForProductionType
					(getClient ().getOurPersistentPlayerPrivateKnowledge (), pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());
			
				// Update the per turn labels
				final ProductionType manaProduction = getLanguage ().findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA);
				manaPerTurn.setString (getTextUtils ().intToStrCommas (manaPerTurnValue) + " " +
					((manaProduction != null) ? manaProduction.getProductionTypeSuffix () : CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA));
		
				final ProductionType researchProduction = getLanguage ().findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH);
				researchPerTurn.setString (getTextUtils ().intToStrCommas (researchPerTurnValue) + " " +
					((researchProduction != null) ? researchProduction.getProductionTypeSuffix () : CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH));
		
				final ProductionType skillProduction = getLanguage ().findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT);
				skillPerTurn.setString (getTextUtils ().intToStrCommas (skillPerTurnValue) + " " +
					((skillProduction != null) ? skillProduction.getProductionTypeSuffix () : CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT));

				magicPowerPerTurn.setText (getLanguage ().findCategoryEntry ("frmMagicSliders", "PowerBase").replaceAll
					("AMOUNT_PER_TURN", getTextUtils ().intToStrCommas (magicPowerPerTurnValue)));				
			
				// Update amount stored labels
				final int manaStoredValue = getResourceValueUtils ().findAmountStoredForProductionType
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA);
			
				manaStored.setText (getTextUtils ().intToStrCommas (manaStoredValue));
			
				final int currentSkill = getResourceValueUtils ().calculateCastingSkillOfPlayer (getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue ());
				castingSkill.setText (getTextUtils ().intToStrCommas (currentSkill));
			
				// Skill progress bar
				skillPerTurn.setMaximum (getSkillCalculations ().getSkillPointsRequiredToImproveSkillFrom (currentSkill));
				skillPerTurn.setValue (getResourceValueUtils ().findAmountStoredForProductionType
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT) -
					getSkillCalculations ().getSkillPointsRequiredForCastingSkill (currentSkill));		// i.e. current skill pts - what we needed to attain our current skill level
			
				// Spell being researched
				if (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched () == null)
				{
					researchPerTurn.setMaximum (100);
					researchPerTurn.setValue (0);
					currentlyResearching.setText (getLanguage ().findCategoryEntry ("frmMagicSliders", "ResearchingNothing"));
				}
				else
				{
					final Spell spell = getLanguage ().findSpell (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ());
					currentlyResearching.setText ((spell != null) ? spell.getSpellName () : getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ());
				
					final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellResearchStatus (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ());
					final int totalResearchCost = getClient ().getClientDB ().findSpell
						(getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched (), "updateProductionLabels").getResearchCost ();
				
					researchPerTurn.setMaximum (totalResearchCost);
					researchPerTurn.setValue (totalResearchCost - researchStatus.getRemainingResearchCost ());
				}
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
		
		log.trace ("Exiting updateProductionLabels");
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
	public final MomSkillCalculations getSkillCalculations ()
	{
		return skillCalculations;
	}

	/**
	 * @param calc Skill calculations
	 */
	public final void setSkillCalculations (final MomSkillCalculations calc)
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
}