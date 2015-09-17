package momime.client.ui.panels;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;
import com.ndg.zorder.ZOrderGraphicsImmediateImpl;

import momime.client.MomClient;
import momime.client.calculations.ClientCityCalculations;
import momime.client.calculations.ClientUnitCalculations;
import momime.client.config.MomImeClientConfigEx;
import momime.client.graphics.database.CityViewElementGfx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.database.BuildingLang;
import momime.client.language.database.SpellLang;
import momime.client.language.database.UnitSkillLang;
import momime.client.ui.MomUIConstants;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.frames.HelpUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.renderer.UnitSkillListCellRenderer;
import momime.client.utils.AnimationController;
import momime.client.utils.ResourceValueClientUtils;
import momime.client.utils.TextUtils;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.Building;
import momime.common.database.BuildingPopulationProductionModifier;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.database.UnitHasSkill;
import momime.common.database.UnitSkillTypeID;
import momime.common.database.UnitUpkeep;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.UnitSkillUtils;
import momime.common.utils.UnitUtils;

/**
 * Unit info screen; used both for displaying real units already on the map, or when changing
 * construction at a city to preview the stats of units we can potentially build.
 * 
 * For the change construction screen, this also has to be able to display buildings, in which
 * case the two areas showing unit attributes + skills and replaced by two text areas
 * showing a description of the building and what it allows.
 */
public final class UnitInfoPanel extends MomClientPanelUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UnitInfoPanel.class);

	/** XML layout */
	private XmlLayoutContainerEx unitInfoLayout;

	/** There's a lot of pixel precision positionining going on here so the panel typically uses no insets or custom insets per component */
	private final static int INSET = 0;
	
	/** How many pixels of the buttons backgrounds overlap the main background */
	private final int BUTTONS_OVERLAP = 5;
	
	/** Names of numbers, for generating button panel filename */
	private final String [] NUMBER_NAMES = new String [] {"Zero", "One", "Two", "Three"};
	
	/** Medium font */
	private Font mediumFont;

	/** Small font */
	private Font smallFont;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Resource value client utils */
	private ResourceValueClientUtils resourceValueClientUtils;
	
	/** Animation controller */
	private AnimationController anim;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Client city calculations */
	private ClientCityCalculations clientCityCalculations;

	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Multiplayer client */
	private MomClient client;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit skill utils */
	private UnitSkillUtils unitSkillUtils;

	/** Unit calculations */
	private UnitCalculations unitCalculations;

	/** Client unit calculations */
	private ClientUnitCalculations clientUnitCalculations;
	
	/** 0-3 actions to set up red buttons for; NB. this must be set prior to init () being called */
	private List<Action> actions = new ArrayList<Action> ();
	
	/** If true then buttons appear on the right; if false then buttons appear underneath; NB. this must be set prior to init () being called */
	private boolean buttonsPositionRight;
	
	/** Main background image */
	private BufferedImage backgroundMain;

	/** Extension to the background image according to the number and location of buttons to add */
	private BufferedImage backgroundButtons;
	
	/** Upkeep label */
	private JLabel upkeepLabel;
	
	/** Moves label */
	private JLabel movesLabel;
	
	/** Cost label */
	private JLabel costLabel;

	/** Image of what's currently being constructed */
	private JPanel currentlyConstructingImage;
	
	/** Name of what's currently being constructed */
	private JLabel currentlyConstructingName;
	
	/** The actual cost figure */
	private JLabel currentlyConstructingProductionCost;

	/** Image of upkeep in coins */
	private JLabel currentlyConstructingUpkeep;

	/** Image of unit movement */
	private JLabel currentlyConstructingMoves;

	/** URN label */
	private JLabel urnLabel;
	
	/** URN value */
	private JLabel urnValue;
	
	/** Long description of building */
	private JTextArea currentlyConstructingDescription;
	
	/** List of what building allows us to construct once we complete it */
	private JTextArea currentlyConstructingAllows;
	
	/** List model of unit skills */
	private DefaultListModel<UnitHasSkill> unitSkillsItems;
	
	/** Unit attributes panel */
	private JPanel unitAttributesPanel;
	
	/** Map containing all the unit attribute labels */
	private Map<String, JLabel> unitAttributeLabels = new HashMap<String, JLabel> ();
	
	/** List containing all the unit attribute images */
	private List<JLabel> unitAttributeImages = new ArrayList<JLabel> ();
	
	/** Scroll pane containing the list of unit skills */
	private JScrollPane unitSkillsScrollPane;
	
	/** Building being displayed */
	private MemoryBuilding building;
	
	/** Unit being displayed */
	private AvailableUnit unit;
	
	/** Cell renderer for drawing the unit skill icons and generating the correct descriptions (some, notably the experience 'skill', aren't straightforward static text) */
	private UnitSkillListCellRenderer unitSkillListCellRenderer;

	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Client config, containing various combat map settings */
	private MomImeClientConfigEx clientConfig;
	
	/** Help text scroll */
	private HelpUI helpUI;
	
	/**
	 * Sets up the panel once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	public final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		backgroundMain = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/unitDetails.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button49x12redNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button49x12redPressed.png");
		
		if (getActions ().size () > 0)
		{
			if (isButtonsPositionRight ())
				backgroundButtons = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/unitDetailsButtonsRight" +
					NUMBER_NAMES [getActions ().size ()] + ".png");
			else
				backgroundButtons = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/unitDetailsButtonsBelow.png");
		}
		
		// Fix the size of the panel to be the same as the background.
		// Add on space for the buttons background as appropriate.
		final Dimension backgroundSize = new Dimension
			(backgroundMain.getWidth () + (((backgroundButtons != null) && (isButtonsPositionRight ())) ? backgroundButtons.getWidth () - BUTTONS_OVERLAP : 0),
			backgroundMain.getHeight () + (((backgroundButtons != null) && (!isButtonsPositionRight ())) ? backgroundButtons.getHeight () - BUTTONS_OVERLAP : 0));
		getPanel ().setMinimumSize (backgroundSize);
		getPanel ().setMaximumSize (backgroundSize);
		getPanel ().setPreferredSize (backgroundSize);

		// Set up layout
		getPanel ().setLayout (new XmlLayoutManager (getUnitInfoLayout ()));
		getPanel ().setOpaque (false);
	
		final ZOrderGraphicsImmediateImpl zOrderGraphics = new ZOrderGraphicsImmediateImpl ();		
		currentlyConstructingImage = new JPanel ()
		{
			/**
			 * Draws whatever is currently selected to construct
			 */
			@Override
			protected final void paintComponent (final Graphics g)
			{
				try
				{
					// Draw building
					if (building != null)
					{
						final CityViewElementGfx buildingImage = getGraphicsDB ().findBuilding (building.getBuildingID (), "currentlyConstructingBuilding");
						final BufferedImage image = getAnim ().loadImageOrAnimationFrame
							((buildingImage.getCityViewAlternativeImageFile () != null) ? buildingImage.getCityViewAlternativeImageFile () : buildingImage.getCityViewImageFile (),
							buildingImage.getCityViewAnimation (), true);
					
						g.drawImage (image, (getSize ().width - image.getWidth ()) / 2, (getSize ().height - image.getHeight ()) / 2, null);
					}
					
					// Draw unit
					if (unit != null)
					{
						zOrderGraphics.setGraphics (g);
						final String movingActionID = getClientUnitCalculations ().determineCombatActionID (unit, true);
						getUnitClientUtils ().drawUnitFigures (unit, movingActionID, 4, zOrderGraphics, 1, 26, true, true, 0);
					}
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		currentlyConstructingImage.setOpaque (false);
		getPanel ().add (currentlyConstructingImage, "frmUnitInfoCurrentlyConstructing");
		
		currentlyConstructingName = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		getPanel ().add (currentlyConstructingName, "frmUnitInfoCurrentlyConstructingName");

		costLabel = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		getPanel ().add (costLabel, "frmUnitInfoCost");
		
		upkeepLabel = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		getPanel ().add (upkeepLabel, "frmUnitInfoUpkeep");
		
		movesLabel = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		getPanel ().add (movesLabel, "frmUnitInfoMoves");
		
		currentlyConstructingProductionCost = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		getPanel ().add (currentlyConstructingProductionCost, "frmUnitInfoCurrentlyConstructingCost");
		
		currentlyConstructingUpkeep = new JLabel ();
		getPanel ().add (currentlyConstructingUpkeep, "frmUnitInfoCurrentlyConstructingUpkeep");
		
		currentlyConstructingMoves = new JLabel ();
		getPanel ().add (currentlyConstructingMoves, "frmUnitInfoCurrentlyConstructingMoves");

		urnLabel = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		getPanel ().add (urnLabel, "frmUnitInfoURNLabel");

		urnValue = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		getPanel ().add (urnValue, "frmUnitInfoURNValue");
		
		// Mini panel at the bottom or right containing the 2 red buttons.
		if (getActions ().size () > 0)
		{
			if (isButtonsPositionRight ())
			{
				for (int n = 0; n < getActions ().size (); n++)
					getPanel ().add (getUtils ().createImageButton (getActions ().get (n), MomUIConstants.DULL_GOLD, MomUIConstants.GOLD, getSmallFont (),
						buttonNormal, buttonPressed, buttonNormal), "frmUnitInfoRedButtonsRight" + (n + (4 - getActions ().size ())));
			}
			else
			{
				for (int n = 0; n < getActions ().size (); n++)
					getPanel ().add (getUtils ().createImageButton (getActions ().get (n), MomUIConstants.DULL_GOLD, MomUIConstants.GOLD, getSmallFont (),
							buttonNormal, buttonPressed, buttonNormal), "frmUnitInfoRedButtonsBottom" + (n+1));
			}
		}
		
		// Top area - buildings
		currentlyConstructingAllows = getUtils ().createWrappingLabel (MomUIConstants.AQUA, getMediumFont ());
		getPanel ().add (currentlyConstructingAllows, "frmUnitInfoCurrentlyConstructingAllows");

		// Bottom area - buildings
		currentlyConstructingDescription = getUtils ().createWrappingLabel (MomUIConstants.AQUA, getMediumFont ());
		getPanel ().add (currentlyConstructingDescription, "frmUnitInfoCurrentlyConstructingDescription");
		
		// Top area  - units
		unitAttributesPanel = new JPanel ();
		unitAttributesPanel.setOpaque (false);
		unitAttributesPanel.setLayout (new GridBagLayout ());
		getPanel ().add (unitAttributesPanel, "frmUnitInfoAttributes");
		
		// Bottom area - units
		getUnitSkillListCellRenderer ().setFont (getSmallFont ());
		getUnitSkillListCellRenderer ().setForeground (MomUIConstants.AQUA);
		getUnitSkillListCellRenderer ().init ();
		
		unitSkillsItems = new DefaultListModel<UnitHasSkill> ();
		
		final JList<UnitHasSkill> unitSkillsList = new JList<UnitHasSkill>  ();		
		unitSkillsList.setOpaque (false);
		unitSkillsList.setModel (unitSkillsItems);
		unitSkillsList.setCellRenderer (getUnitSkillListCellRenderer ());
		unitSkillsList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		
		unitSkillsScrollPane = getUtils ().createTransparentScrollPane (unitSkillsList);
		getPanel ().add (unitSkillsScrollPane, "frmUnitInfoSkills");
		
		// Clicking a unit skill from a spell asks about cancelling it
		unitSkillsList.addListSelectionListener (new ListSelectionListener ()
		{
			@Override
			public final void valueChanged (final ListSelectionEvent ev)
			{
				if ((unit instanceof MemoryUnit) && (unitSkillsList.getSelectedIndex () >= 0))
				{
					final MemoryUnit memoryUnit = (MemoryUnit) unit;
					final UnitHasSkill skill = unitSkillsItems.get (unitSkillsList.getSelectedIndex ());
					
					// We want to ignore clicks on regular skills, and only do something about clicks on skills granted by spells.
					// So search through maintained spells looking for this unitSkillID on this unit and see if we find anything.
					final MemoryMaintainedSpell spell = getMemoryMaintainedSpellUtils ().findMaintainedSpell
						(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
						null, null, memoryUnit.getUnitURN (), skill.getUnitSkillID (), null, null);
					
					if (spell != null)
						try
						{
							final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
							msg.setTitleLanguageCategoryID ("SpellCasting");
							msg.setTitleLanguageEntryID ("SwitchOffSpellTitle");
	
							final SpellLang spellLang = getLanguage ().findSpell (spell.getSpellID ());
							final String spellName = (spellLang != null) ? spellLang.getSpellName () : null;
							
							if (spell.getCastingPlayerID () != getClient ().getOurPlayerID ())
								msg.setText (getLanguage ().findCategoryEntry ("SpellCasting", "SwitchOffSpellNotOurs").replaceAll
									("SPELL_NAME", (spellName != null) ? spellName : spell.getSpellID ()));
							else
							{
								msg.setText (getLanguage ().findCategoryEntry ("SpellCasting", "SwitchOffSpell").replaceAll
									("SPELL_NAME", (spellName != null) ? spellName : spell.getSpellID ()));
								msg.setSwitchOffSpell (spell);
							}
	
							msg.setVisible (true);
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
				}
			}
		});
		
		// Right clicking on skills shows help text about them
		unitSkillsList.addMouseListener (new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				if (SwingUtilities.isRightMouseButton (ev))
				{
					final int row = unitSkillsList.locationToIndex (ev.getPoint ());
					if ((row >= 0) && (row < unitSkillsItems.size ()))
					{
						final UnitHasSkill unitSkill = unitSkillsItems.get (row);
						try
						{
							getHelpUI ().showUnitSkillID (unitSkill.getUnitSkillID (), getUnit ());
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
					}
				}
			}
		});
		
		log.trace ("Exiting init");
	}
	
	/**
	 * This is called by the windowClosed handler of ChangeConstructionUI to close down all animations when the panel closes
	 * @throws MomException If unregisterRepaintTrigger is passed a null component
	 */
	public final void unitInfoPanelClosing () throws MomException
	{
		log.trace ("Entering unitInfoPanelClosing");
		
		getAnim ().unregisterRepaintTrigger (null, currentlyConstructingImage);

		log.trace ("Exiting unitInfoPanelClosing");
	}
	
	/**
	 * Clears old dynamically created controls
	 */
	private final void clear ()
	{
		for (final JLabel attrLabel : unitAttributeLabels.values ())
			unitAttributesPanel.remove (attrLabel);
		
		for (final JLabel attrImage : unitAttributeImages)
			unitAttributesPanel.remove (attrImage);
		
		unitAttributeLabels.clear ();
		unitAttributeImages.clear ();
		unitSkillsItems.clear ();
	}
	
	/**
	 * @param showBuilding Building to show info about
	 * @throws IOException If there is a problem
	 */
	public final void showBuilding (final MemoryBuilding showBuilding) throws IOException
	{
		log.trace ("Entering showBuilding");

		clear ();
		unitAttributesPanel.setVisible (false);
		unitSkillsScrollPane.setVisible (false);
		currentlyConstructingAllows.setVisible (true);
		currentlyConstructingDescription.setVisible (true);

		// Find details about this kind of building
		building = showBuilding;
		unit = null;
		final Building buildingInfo = getClient ().getClientDB ().findBuilding (building.getBuildingID (), "showBuilding");
		
		// Update language independant labels
		currentlyConstructingProductionCost.setText ((buildingInfo.getProductionCost () == null) ? null : getTextUtils ().intToStrCommas (buildingInfo.getProductionCost ()));
		costLabel.setVisible (buildingInfo.getProductionCost () != null);
		movesLabel.setText (" ");		// Space ensures the line where the movement goes for units is still occupied so the URN line goes in the right place
		currentlyConstructingMoves.setVisible (false);

		// Search for upkeep values (i.e. no population task specified, and the value is negative)
		final List<UnitUpkeep> upkeeps = new ArrayList<UnitUpkeep> ();
		for (final BuildingPopulationProductionModifier upkeepValue : buildingInfo.getBuildingPopulationProductionModifier ())
			if ((upkeepValue.getPopulationTaskID () == null) && (upkeepValue.getDoubleAmount () != null) && (upkeepValue.getDoubleAmount () < 0))
			{
				if (upkeepValue.getDoubleAmount () % 2 != 0)
					throw new MomException ("Building " + building.getBuildingID () + " has an upkeep of type " + upkeepValue.getProductionTypeID () + " which is not a multiple of 2");
			
				final UnitUpkeep upkeep = new UnitUpkeep ();
				upkeep.setProductionTypeID (upkeepValue.getProductionTypeID ());
				upkeep.setUpkeepValue (-upkeepValue.getDoubleAmount () / 2);
				upkeeps.add (upkeep);
			}
	
		// Generate an image from the upkeeps
		final BufferedImage upkeepImage = getResourceValueClientUtils ().generateUpkeepImage (upkeeps, false);
		currentlyConstructingUpkeep.setIcon ((upkeepImage == null) ? null : new ImageIcon (upkeepImage));
		upkeepLabel.setVisible (upkeepImage != null);

		// Update language dependant labels
		currentConstructionChanged ();
	
		// Show the image of the selected building
		getAnim ().unregisterRepaintTrigger (null, currentlyConstructingImage);
		getAnim ().registerRepaintTrigger (getGraphicsDB ().findBuilding (building.getBuildingID (), "showBuilding").getCityViewAnimation (), currentlyConstructingImage);
		
		// Show URN?
		if ((showBuilding.getBuildingURN () > 0) && (getClientConfig ().isDebugShowURNs ()))
		{
			urnValue.setText (getTextUtils ().intToStrCommas (showBuilding.getBuildingURN ()));
			urnLabel.setVisible (true);
			urnValue.setVisible (true);
		}
		else
		{
			urnLabel.setVisible (false);
			urnValue.setVisible (false);
		}
		
		log.trace ("Entering showBuilding");
	}
	
	/**
	 * @return Building that we're displaying info about, or null if we're displaying info about a unit
	 */
	public final MemoryBuilding getBuilding ()
	{
		return building;
	}

	/**
	 * @return Unit that we're displaying info about, or null if we're displaying info about a building
	 */
	public final AvailableUnit getUnit ()
	{
		return unit;
	}

	/**
	 * @param showUnit Unit to show info about
	 * @throws IOException If there is a problem
	 */
	public final void showUnit (final AvailableUnit showUnit) throws IOException
	{
		log.trace ("Entering showUnit");
		
		clear ();
		currentlyConstructingAllows.setVisible (false);
		currentlyConstructingDescription.setVisible (false);
		unitAttributesPanel.setVisible (true);
		unitSkillsScrollPane.setVisible (true);

		// Find details about this kind of unit
		unit = showUnit;
		building = null;
		final Unit unitInfo = getClient ().getClientDB ().findUnit (unit.getUnitID (), "showUnit");

		// Update language independant labels
		currentlyConstructingProductionCost.setText ((unitInfo.getProductionCost () == null) ? null : getTextUtils ().intToStrCommas (unitInfo.getProductionCost ()));
		costLabel.setVisible (unitInfo.getProductionCost () != null);
		
		// Search for upkeep values
		final List<UnitUpkeep> upkeeps = new ArrayList<UnitUpkeep> ();
		for (final UnitUpkeep upkeepValue : unitInfo.getUnitUpkeep ())
		{
			final UnitUpkeep upkeep = new UnitUpkeep ();
			upkeep.setProductionTypeID (upkeepValue.getProductionTypeID ());
			upkeep.setUpkeepValue (getUnitSkillUtils ().getModifiedUpkeepValue (unit, upkeep.getProductionTypeID (), getClient ().getPlayers (), getClient ().getClientDB ()));
			upkeeps.add (upkeep);
		}

		// Generate an image from the upkeeps
		final BufferedImage upkeepImage = getResourceValueClientUtils ().generateUpkeepImage (upkeeps, false);
		currentlyConstructingUpkeep.setIcon ((upkeepImage == null) ? null : new ImageIcon (upkeepImage));
		upkeepLabel.setVisible (upkeepImage != null);
		
		// Generate an image showing movement
		final BufferedImage movementImage = getUnitClientUtils ().generateMovementImage (getUnit ());

		currentlyConstructingMoves.setIcon ((movementImage == null) ? null : new ImageIcon (movementImage));
		movesLabel.setVisible (movementImage != null);
		currentlyConstructingMoves.setVisible (movementImage != null);
		
		// Create labels and icons to display unit attributes
		final List<UnitHasSkill> mergedSkills;
		if (unit instanceof MemoryUnit)
			mergedSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList
				(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), (MemoryUnit) unit, getClient ().getClientDB ());
		else
			mergedSkills = unit.getUnitHasSkill ();
		
		final Dimension attrValuePanelSize = new Dimension (289, 15);
		
		int y = 0;
		for (final UnitHasSkill thisSkill : mergedSkills)
			if (getGraphicsDB ().findUnitSkill (thisSkill.getUnitSkillID (), "UnitInfoPanel").getUnitSkillTypeID () == UnitSkillTypeID.ATTRIBUTE)
			{
				// Label
				final JLabel attrLabel = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
				
				final GridBagConstraints attrConstraints = getUtils ().createConstraintsNoFill (0, y, 1, 1, INSET, GridBagConstraintsNoFill.WEST);
				attrConstraints.weightx = 1;
				unitAttributesPanel.add (attrLabel, attrConstraints);
				unitAttributeLabels.put (thisSkill.getUnitSkillID (), attrLabel);
				
				// Value
				final BufferedImage attrImage = getUnitClientUtils ().generateAttributeImage (getUnit (), thisSkill.getUnitSkillID ());
				final JLabel attrValue = new JLabel ((attrImage == null) ? null : new ImageIcon (attrImage));
				
				attrValue.setOpaque (false);
				attrValue.setMinimumSize (attrValuePanelSize);
				attrValue.setMaximumSize (attrValuePanelSize);
				attrValue.setPreferredSize (attrValuePanelSize);
				attrValue.setHorizontalAlignment (SwingConstants.LEFT);
				attrValue.setVerticalAlignment (SwingConstants.TOP);
				
				unitAttributesPanel.add (attrValue, getUtils ().createConstraintsNoFill (1, y, 1, 1, new Insets (1, 0, 1, 0), GridBagConstraintsNoFill.EAST));
				unitAttributeImages.add (attrValue);
				y++;
				
				// Right clicking on unit attribute labels or icon area gives help about that attribute
				final MouseListener unitAttributeHelpListener = new MouseAdapter ()
				{
					@Override
					public final void mouseClicked (final MouseEvent ev)
					{
						if (SwingUtilities.isRightMouseButton (ev))
							try
							{
								getHelpUI ().showUnitSkillID (thisSkill.getUnitSkillID (), getUnit ());
							}
							catch (final Exception e)
							{
								log.error (e, e);
							}
					}
				};
				
				attrLabel.addMouseListener (unitAttributeHelpListener);
				attrValue.addMouseListener (unitAttributeHelpListener);
			}
		
		// Find all skills to show in the list box
		getUnitSkillListCellRenderer ().setUnit (unit);
		for (final UnitHasSkill thisSkill : mergedSkills)
		{
			// Only add skills with images - some don't have, e.g. Flying, since this shows up on the movement section of the form.
			// Experience is an exception since its images are derived differently.
			if ((thisSkill.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)) ||
				(getGraphicsDB ().findUnitSkill (thisSkill.getUnitSkillID (), "showUnit").getUnitSkillImageFile () != null))

				unitSkillsItems.addElement (thisSkill);
		}
		
		// Update language dependant labels
		languageChanged ();

		// Show the image of the selected unit
		getAnim ().unregisterRepaintTrigger (null, currentlyConstructingImage);
		final String movingActionID = getClientUnitCalculations ().determineCombatActionID (unit, true);
		getUnitClientUtils ().registerUnitFiguresAnimation (unit.getUnitID (), movingActionID, 4, currentlyConstructingImage); 
		
		// Show URN?
		if ((showUnit instanceof MemoryUnit) && (((MemoryUnit) showUnit).getUnitURN () > 0) && (getClientConfig ().isDebugShowURNs ()))
		{
			urnValue.setText (getTextUtils ().intToStrCommas (((MemoryUnit) showUnit).getUnitURN ()));
			urnLabel.setVisible (true);
			urnValue.setVisible (true);
		}
		else
		{
			urnLabel.setVisible (false);
			urnValue.setVisible (false);
		}
		
		log.trace ("Entering showUnit");
	}

	/**
	 * Draws the overall background behind all the other components
	 */
	@Override
	protected final void paintComponent (final Graphics g)
	{
		g.drawImage (backgroundMain, 0, 0, null);
		
		if (backgroundButtons != null)
		{
			if (isButtonsPositionRight ())
				g.drawImage (backgroundButtons, backgroundMain.getWidth () - BUTTONS_OVERLAP, backgroundMain.getHeight () - backgroundButtons.getHeight (), null);
			else
				g.drawImage (backgroundButtons, (backgroundMain.getWidth () - backgroundButtons.getWidth ()) / 2, backgroundMain.getHeight () - BUTTONS_OVERLAP, null);
		}
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		// Fixed labels
		upkeepLabel.setText	(getLanguage ().findCategoryEntry ("frmChangeConstruction", "Upkeep"));
		movesLabel.setText	(getLanguage ().findCategoryEntry ("frmChangeConstruction", "Moves"));
		costLabel.setText		(getLanguage ().findCategoryEntry ("frmChangeConstruction", "Cost"));

		// Unit attribute labels
		for (final Entry<String, JLabel> unitAttr : unitAttributeLabels.entrySet ())
		{
			final UnitSkillLang unitAttrLang = getLanguage ().findUnitSkill (unitAttr.getKey ());
			unitAttr.getValue ().setText ((unitAttrLang != null) ? unitAttrLang.getUnitSkillDescription () : unitAttr.getKey ());
		}
		
		// Update text about unit or building being displayed
		currentConstructionChanged ();
		
		log.trace ("Exiting languageChanged");
	}

	/**
	 * This is called when either the language or building/unit being previewed for construction changes,
	 * so we can update the name and description of it
	 */
	private final void currentConstructionChanged ()
	{
		log.trace ("Entering currentConstructionChanged");
		
		// Labels if showing a building
		if (building != null)
		{
			final BuildingLang buildingLang = getLanguage ().findBuilding (building.getBuildingID ());
			currentlyConstructingName.setText ((buildingLang != null) ? buildingLang.getBuildingName () : building.getBuildingID ());
			currentlyConstructingDescription.setText ((buildingLang != null) ? buildingLang.getBuildingHelpText () : null);
			currentlyConstructingAllows.setText (getClientCityCalculations ().describeWhatBuildingAllows (building.getBuildingID (), (MapCoordinates3DEx) building.getCityLocation ()));
			urnLabel.setText (getLanguage ().findCategoryEntry ("frmChangeConstruction", "BuildingURN"));
		}
		
		// Labels if showing a unit
		else if (unit != null)
		{
			String unitName = null;
			try
			{
				unitName = getUnitClientUtils ().getUnitName (unit, UnitNameType.RACE_UNIT_NAME);
			}
			catch (final RecordNotFoundException e)
			{
				// Log the error, but its only in generating the name, so keep going
				log.error (e, e);
			}
			
			currentlyConstructingName.setText (unitName);
			urnLabel.setText (getLanguage ().findCategoryEntry ("frmChangeConstruction", "UnitURN"));
		}
		
		log.trace ("Exiting currentConstructionChanged");
	}

	/**
	 * @return Width added to the main panel to accomodate any action button(s)
	 */
	public final int getBackgroundButtonsWidth ()
	{
		final int width;
		if (backgroundButtons == null)
			width = 0;
		else
			width = isButtonsPositionRight () ? backgroundButtons.getWidth () - BUTTONS_OVERLAP : backgroundButtons.getWidth ();
			
		return width;
	}	
	
	/**
	 * @return Height added to the main panel to accomodate any action button(s)
	 */
	public final int getBackgroundButtonsHeight ()
	{
		final int height;
		if (backgroundButtons == null)
			height = 0;
		else
			height = isButtonsPositionRight () ? backgroundButtons.getHeight () : backgroundButtons.getHeight () - BUTTONS_OVERLAP;
			
		return height;
	}	
	
	/**
	 * @return Medium font
	 */
	public final Font getMediumFont ()
	{
		return mediumFont;
	}

	/**
	 * @param font Medium font
	 */
	public final void setMediumFont (final Font font)
	{
		mediumFont = font;
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
	 * @return Resource value client utils
	 */
	public final ResourceValueClientUtils getResourceValueClientUtils ()
	{
		return resourceValueClientUtils;
	}

	/**
	 * @param utils Resource value client utils
	 */
	public final void setResourceValueClientUtils (final ResourceValueClientUtils utils)
	{
		resourceValueClientUtils = utils;
	}

	/**
	 * @return Animation controller
	 */
	public final AnimationController getAnim ()
	{
		return anim;
	}

	/**
	 * @param controller Animation controller
	 */
	public final void setAnim (final AnimationController controller)
	{
		anim = controller;
	}

	/**
	 * @return Graphics database
	 */
	public final GraphicsDatabaseEx getGraphicsDB ()
	{
		return graphicsDB;
	}

	/**
	 * @param db Graphics database
	 */
	public final void setGraphicsDB (final GraphicsDatabaseEx db)
	{
		graphicsDB = db;
	}

	/**
	 * @return Client city calculations
	 */
	public final ClientCityCalculations getClientCityCalculations ()
	{
		return clientCityCalculations;
	}

	/**
	 * @param calc Client city calculations
	 */
	public final void setClientCityCalculations (final ClientCityCalculations calc)
	{
		clientCityCalculations = calc;
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
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}

	/**
	 * @return Unit skill utils
	 */
	public final UnitSkillUtils getUnitSkillUtils ()
	{
		return unitSkillUtils;
	}

	/**
	 * @param utils Unit skill utils
	 */
	public final void setUnitSkillUtils (final UnitSkillUtils utils)
	{
		unitSkillUtils = utils;
	}
	
	/**
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
	}

	/**
	 * @return Client unit calculations
	 */
	public final ClientUnitCalculations getClientUnitCalculations ()
	{
		return clientUnitCalculations;
	}

	/**
	 * @param calc Client unit calculations
	 */
	public final void setClientUnitCalculations (final ClientUnitCalculations calc)
	{
		clientUnitCalculations = calc;
	}

	/**
	 * @return Client-side unit utils
	 */
	public final UnitClientUtils getUnitClientUtils ()
	{
		return unitClientUtils;
	}

	/**
	 * @param util Client-side unit utils
	 */
	public final void setUnitClientUtils (final UnitClientUtils util)
	{
		unitClientUtils = util;
	}
	
	/**
	 * @return Cell renderer for drawing the unit skill icons and generating the correct descriptions (some, notably the experience 'skill', aren't straightforward static text)
	 */
	public final UnitSkillListCellRenderer getUnitSkillListCellRenderer ()
	{
		return unitSkillListCellRenderer;
	}

	/**
	 * @param rend Cell renderer for drawing the unit skill icons and generating the correct descriptions (some, notably the experience 'skill', aren't straightforward static text)
	 */
	public final void setUnitSkillListCellRenderer (final UnitSkillListCellRenderer rend)
	{
		unitSkillListCellRenderer = rend;
	}

	/**
	 * @return 0-3 actions to set up red buttons for; NB. this must be set prior to init () being called
	 */
	public final List<Action> getActions ()
	{
		return actions;
	}

	/**
	 * @return If true then buttons appear on the right; if false then buttons appear underneath; NB. this must be set prior to init () being called
	 */
	public final boolean isButtonsPositionRight ()
	{
		return buttonsPositionRight;
	}

	/**
	 * @param pos If true then buttons appear on the right; if false then buttons appear underneath; NB. this must be set prior to init () being called
	 */
	public final void setButtonsPositionRight (final boolean pos)
	{
		buttonsPositionRight = pos;
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
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtils)
	{
		memoryMaintainedSpellUtils = spellUtils;
	}

	/**
	 * @return Client config, containing various combat map settings
	 */	
	public final MomImeClientConfigEx getClientConfig ()
	{
		return clientConfig;
	}

	/**
	 * @param config Client config, containing various combat map settings
	 */
	public final void setClientConfig (final MomImeClientConfigEx config)
	{
		clientConfig = config;
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
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getUnitInfoLayout ()
	{
		return unitInfoLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setUnitInfoLayout (final XmlLayoutContainerEx layout)
	{
		unitInfoLayout = layout;
	}
}