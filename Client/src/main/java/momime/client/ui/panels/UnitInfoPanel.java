package momime.client.ui.panels;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutManager;
import com.ndg.utils.swing.zorder.ZOrderGraphicsImmediateImpl;

import momime.client.MomClient;
import momime.client.calculations.ClientCityCalculations;
import momime.client.config.MomImeClientConfig;
import momime.client.graphics.AnimationContainer;
import momime.client.ui.MomUIConstants;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.HelpUI;
import momime.client.ui.frames.HeroItemInfoUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.renderer.UnitAttributeListCellRenderer;
import momime.client.ui.renderer.UnitAttributeWithBreakdownImage;
import momime.client.ui.renderer.UnitSkillListCellRenderer;
import momime.client.utils.AnimationController;
import momime.client.utils.ResourceValueClientUtils;
import momime.client.utils.TextUtils;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.MomException;
import momime.common.calculations.CityProductionCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.Building;
import momime.common.database.BuildingPopulationProductionModifier;
import momime.common.database.CityViewElement;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.Spell;
import momime.common.database.UnitCanCast;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSkillTypeID;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.NumberedHeroItem;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerPickUtils;

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
	private final static Log log = LogFactory.getLog (UnitInfoPanel.class);

	/** XML layout */
	private XmlLayoutContainerEx unitInfoLayout;

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
	
	/** Client city calculations */
	private ClientCityCalculations clientCityCalculations;

	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Multiplayer client */
	private MomClient client;

	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;

	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** 0-3 actions to set up red buttons for; NB. this must be set prior to init () being called */
	private List<Action> actions = new ArrayList<Action> ();
	
	/** If true then buttons appear on the right; if false then buttons appear underneath; NB. this must be set prior to init () being called */
	private boolean buttonsPositionRight;
	
	/** If true, will calculate unit cost reduction from Iron Ore or Coal */
	private boolean showReducedUnitCost;
	
	/** Main background image */
	private BufferedImage backgroundMain;

	/** Frame for hero portraits */
	private BufferedImage heroPortraitFrame;
	
	/** Extension to the background image according to the number and location of buttons to add */
	private BufferedImage backgroundButtons;
	
	/** Upkeep label */
	private JLabel upkeepLabel;
	
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

	/** Head count label for Hydra */
	private JLabel headsLabel;
	
	/** Head count label for Hydra */
	private JLabel headsValue;
	
	/** URN label */
	private JLabel urnLabel;
	
	/** URN value */
	private JLabel urnValue;
	
	/** Long description of building */
	private JTextArea currentlyConstructingDescription;
	
	/** List of what building allows us to construct once we complete it */
	private JTextArea currentlyConstructingAllows;

	/** List model of unit attributes */
	private DefaultListModel<UnitAttributeWithBreakdownImage> unitAttributesItems;
	
	/** List model of unit skills */
	private DefaultListModel<UnitSkillOrHeroItemSlot> unitSkillsItems;

	/** Scroll pane containing the list of unit attributes */
	private JScrollPane unitAttributesScrollPane;
	
	/** Scroll pane containing the list of unit skills */
	private JScrollPane unitSkillsScrollPane;
	
	/** Building being displayed */
	private MemoryBuilding building;
	
	/** Unit being displayed */
	private ExpandedUnitDetails unit;

	/** List of shading colours to apply to the unit images */
	private List<String> shadingColours;
	
	/** Cell renderer for drawing the unit attribute text and component breakdowns */
	private UnitAttributeListCellRenderer unitAttributeListCellRenderer;
	
	/** Cell renderer for drawing the unit skill icons and generating the correct descriptions (some, notably the experience 'skill', aren't straightforward static text) */
	private UnitSkillListCellRenderer unitSkillListCellRenderer;

	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Client config, containing various combat map settings */
	private MomImeClientConfig clientConfig;
	
	/** Help text scroll */
	private HelpUI helpUI;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** City production calculations */
	private CityProductionCalculations cityProductionCalculations;
	
	/**
	 * Sets up the panel once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	public final void init () throws IOException
	{
		// Load images
		backgroundMain = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/unitDetails.png");
		heroPortraitFrame = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/unitDetailsHeroFrame.png");
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
						final CityViewElement buildingImage = getClient ().getClientDB ().findCityViewElementBuilding (building.getBuildingID (), "currentlyConstructingBuilding");
						final BufferedImage image = getAnim ().loadImageOrAnimationFrame
							((buildingImage.getCityViewAlternativeImageFile () != null) ? buildingImage.getCityViewAlternativeImageFile () : buildingImage.getCityViewImageFile (),
							buildingImage.getCityViewAnimation (), true, AnimationContainer.COMMON_XML);
					
						g.drawImage (image, (getSize ().width - image.getWidth ()) / 2, (getSize ().height - image.getHeight ()) / 2, null);
					}
					
					// Draw unit
					if (getUnit () != null)
					{
						final UnitEx UnitEx = getClient ().getClientDB ().findUnit (getUnit ().getUnitID (), "heroPortrait");
						if ((UnitEx.getHeroPortraitImageFile () != null) && (getClientConfig ().isShowHeroPortraits ()))
						{
							// Show static hero portrait
							int x = (getSize ().width - heroPortraitFrame.getWidth ()) / 2;
							int y = (getSize ().height - heroPortraitFrame.getHeight ()) / 2;
							g.drawImage (heroPortraitFrame, x, y, null);
							g.drawImage (getUtils ().loadImage (UnitEx.getHeroPortraitImageFile ()), x+1, y+1,
								heroPortraitFrame.getWidth () - 2, heroPortraitFrame.getHeight () - 2, null);
						}
						else
						{
							// Show combat anim of unit 
							zOrderGraphics.setGraphics (g);
							final String movingActionID = getUnitCalculations ().determineCombatActionID (getUnit (), true, getClient ().getClientDB ());
							getUnitClientUtils ().drawUnitFigures (getUnit (), movingActionID, 4, zOrderGraphics, 1, 26, true, true, 0, shadingColours, null, false, true);
						}
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
		
		currentlyConstructingProductionCost = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		getPanel ().add (currentlyConstructingProductionCost, "frmUnitInfoCurrentlyConstructingCost");
		
		currentlyConstructingUpkeep = new JLabel ();
		getPanel ().add (currentlyConstructingUpkeep, "frmUnitInfoCurrentlyConstructingUpkeep");
		
		headsLabel = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		getPanel ().add (headsLabel, "frmUnitInfoHeadsLabel");
		
		headsValue = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		getPanel ().add (headsValue, "frmUnitInfoHeadsValue");
		
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
		getUnitAttributeListCellRenderer ().setFont (getSmallFont ());
		getUnitAttributeListCellRenderer ().setForeground (MomUIConstants.AQUA);
		getUnitAttributeListCellRenderer ().init ();
		
		unitAttributesItems = new DefaultListModel<UnitAttributeWithBreakdownImage> ();
		
		final JList<UnitAttributeWithBreakdownImage> unitAttributesList = new JList<UnitAttributeWithBreakdownImage> ();
		unitAttributesList.setOpaque (false);
		unitAttributesList.setModel (unitAttributesItems);
		unitAttributesList.setCellRenderer (getUnitAttributeListCellRenderer ());
		unitAttributesList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		
		unitAttributesScrollPane = getUtils ().createTransparentScrollPane (unitAttributesList);
		unitAttributesScrollPane.setHorizontalScrollBarPolicy (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		getPanel ().add (unitAttributesScrollPane, "frmUnitInfoAttributes");
		
		// Bottom area - units
		getUnitSkillListCellRenderer ().setFont (getSmallFont ());
		getUnitSkillListCellRenderer ().setForeground (MomUIConstants.AQUA);
		getUnitSkillListCellRenderer ().init ();
		
		unitSkillsItems = new DefaultListModel<UnitSkillOrHeroItemSlot> ();
		
		final JList<UnitSkillOrHeroItemSlot> unitSkillsList = new JList<UnitSkillOrHeroItemSlot>  ();		
		unitSkillsList.setOpaque (false);
		unitSkillsList.setModel (unitSkillsItems);
		unitSkillsList.setCellRenderer (getUnitSkillListCellRenderer ());
		unitSkillsList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		
		unitSkillsScrollPane = getUtils ().createTransparentScrollPane (unitSkillsList);
		unitSkillsScrollPane.setHorizontalScrollBarPolicy (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		getPanel ().add (unitSkillsScrollPane, "frmUnitInfoSkills");
		
		// Clicking a unit skill from a spell asks about cancelling it
		unitSkillsList.addListSelectionListener ((ev) ->
		{
			if ((getUnit ().isMemoryUnit ()) && (unitSkillsList.getSelectedIndex () >= 0))
			{
				final UnitSkillOrHeroItemSlot skill = unitSkillsItems.get (unitSkillsList.getSelectedIndex ());
				
				// We want to ignore clicks on regular skills, and only do something about clicks on skills granted by spells.
				// So search through maintained spells looking for this unitSkillID on this unit and see if we find anything.
				if (skill.getUnitSkillID () != null)
					try
					{
						final MemoryMaintainedSpell spell = getMemoryMaintainedSpellUtils ().findMaintainedSpell
							(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
							null, null, getUnit ().getUnitURN (), skill.getUnitSkillID (), null, null);
						
						if (spell != null)
						{
							// Its a spell - but make sure it isn't permanent
							final Spell spellDef = getClient ().getClientDB ().findSpell (spell.getSpellID (), "SwitchOffUnitSpell");
							if ((spellDef.isPermanent () == null) || (!spellDef.isPermanent ()))
							{
								// All looks ok - check they are really sure
								final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
								msg.setLanguageTitle (getLanguages ().getSpellCasting ().getSwitchOffSpellTitle ());
		
								final String spellName = getLanguageHolder ().findDescription (spellDef.getSpellName ());
								
								if (spell.getCastingPlayerID () != getClient ().getOurPlayerID ())
									msg.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellCasting ().getSwitchOffSpellNotOurs ()).replaceAll
										("SPELL_NAME", (spellName != null) ? spellName : spell.getSpellID ()));
								else if (!getClient ().isPlayerTurn ())
									msg.setLanguageText (getLanguages ().getSpellCasting ().getSwitchOffSpellNotYourTurn ());
								else if (getCombatUI ().isVisible ())
									msg.setLanguageText (getLanguages ().getSpellCasting ().getSwitchOffSpellInCombat ());
								else
								{
									msg.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellCasting ().getSwitchOffSpell ()).replaceAll
										("SPELL_NAME", (spellName != null) ? spellName : spell.getSpellID ()));
									msg.setSwitchOffSpell (spell);
								}
		
								msg.setVisible (true);
							}
						}
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
			}
		});
		
		// Right clicking on attributes shows help text about them
		unitAttributesList.addMouseListener (new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				if (SwingUtilities.isRightMouseButton (ev))
				{
					final int row = unitAttributesList.locationToIndex (ev.getPoint ());
					if ((row >= 0) && (row < unitAttributesItems.size ()))
					{
						final UnitAttributeWithBreakdownImage unitAttribute = unitAttributesItems.get (row);
						try
						{
							getHelpUI ().showUnitSkillID (unitAttribute.getUnitSkillID (), getUnit ());
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
					}
				}
			}
		});
		
		// Right clicking on skills, slots or item hero items shows help text about them
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
						final UnitSkillOrHeroItemSlot skill = unitSkillsItems.get (row);
						try
						{
							if (skill.getUnitSkillID () != null)
								getHelpUI ().showUnitSkillID (skill.getUnitSkillID (), getUnit ());
							else if (skill.getSpellID () != null)
								getHelpUI ().showSpellID (skill.getSpellID (), getUnit ().getOwningPlayer ());
							else if (skill.getHeroItemSlotTypeID () != null)
								getHelpUI ().showHeroItemSlotTypeID (skill.getHeroItemSlotTypeID ());
							else if (skill.getHeroItem () != null)
							{
								// Is there an item info screen already open for this item?
								HeroItemInfoUI itemInfo = getClient ().getHeroItemInfos ().get (skill.getHeroItem ().getHeroItemURN ());
								if (itemInfo == null)
								{
									itemInfo = getPrototypeFrameCreator ().createHeroItemInfo ();
									itemInfo.setItem (skill.getHeroItem ());
									getClient ().getHeroItemInfos ().put (skill.getHeroItem ().getHeroItemURN (), itemInfo);
								}
								itemInfo.setVisible (true);
							}
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
					}
				}
			}
		});
	}
	
	/**
	 * This is called by the windowClosed handler of ChangeConstructionUI to close down all animations when the panel closes
	 * @throws MomException If unregisterRepaintTrigger is passed a null component
	 */
	public final void unitInfoPanelClosing () throws MomException
	{
		getAnim ().unregisterRepaintTrigger (null, currentlyConstructingImage);
	}
	
	/**
	 * Clears out old list items
	 */
	private final void clear ()
	{
		unitAttributesItems.clear ();
		unitSkillsItems.clear ();
	}
	
	/**
	 * @param showBuilding Building to show info about
	 * @throws IOException If there is a problem
	 */
	public final void showBuilding (final MemoryBuilding showBuilding) throws IOException
	{
		clear ();
		unitAttributesScrollPane.setVisible (false);
		unitSkillsScrollPane.setVisible (false);
		currentlyConstructingAllows.setVisible (true);
		currentlyConstructingDescription.setVisible (true);
		headsLabel.setVisible (false);
		headsValue.setVisible (false);

		// Find details about this kind of building
		building = showBuilding;
		unit = null;
		shadingColours = null;
		final Building buildingInfo = getClient ().getClientDB ().findBuilding (building.getBuildingID (), "showBuilding");
		
		// Update language independant labels
		currentlyConstructingProductionCost.setText ((buildingInfo.getProductionCost () == null) ? null : getTextUtils ().intToStrCommas (buildingInfo.getProductionCost ()));
		costLabel.setVisible (buildingInfo.getProductionCost () != null);

		// Search for upkeep values (i.e. no population task specified, and the value is negative)
		final List<ProductionTypeAndUndoubledValue> upkeeps = new ArrayList<ProductionTypeAndUndoubledValue> ();
		for (final BuildingPopulationProductionModifier upkeepValue : buildingInfo.getBuildingPopulationProductionModifier ())
			if ((upkeepValue.getPopulationTaskID () == null) && (upkeepValue.getDoubleAmount () != null) && (upkeepValue.getDoubleAmount () < 0))
			{
				if (upkeepValue.getDoubleAmount () % 2 != 0)
					throw new MomException ("Building " + building.getBuildingID () + " has an upkeep of type " + upkeepValue.getProductionTypeID () + " which is not a multiple of 2");
			
				final ProductionTypeAndUndoubledValue upkeep = new ProductionTypeAndUndoubledValue ();
				upkeep.setProductionTypeID (upkeepValue.getProductionTypeID ());
				upkeep.setUndoubledProductionValue (-upkeepValue.getDoubleAmount () / 2);
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
		getAnim ().registerRepaintTrigger (getClient ().getClientDB ().findCityViewElementBuilding
			(building.getBuildingID (), "showBuilding").getCityViewAnimation (), currentlyConstructingImage, AnimationContainer.COMMON_XML);
		
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
	public final ExpandedUnitDetails getUnit ()
	{
		return unit;
	}

	/**
	 * Shortcut method for re-displaying the same unit again
	 * @throws IOException If there is a problem
	 */
	public final void refreshUnitDetails () throws IOException
	{
		showUnit (getUnit ().getUnit ());
	}
	
	/**
	 * @param showUnit Unit to show info about
	 * @throws IOException If there is a problem
	 */
	public final void showUnit (final AvailableUnit showUnit) throws IOException
	{
		clear ();
		currentlyConstructingAllows.setVisible (false);
		currentlyConstructingDescription.setVisible (false);
		unitAttributesScrollPane.setVisible (true);
		unitSkillsScrollPane.setVisible (true);
		
		// Find details about this kind of unit
		building = null;
		shadingColours = new ArrayList<String> ();
		unit = getExpandUnitDetails ().expandUnitDetails (showUnit, null, null, null, getClient ().getPlayers (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());

		// Update language independant labels
		if (getUnit ().getUnitDefinition ().getProductionCost () == null)
			currentlyConstructingProductionCost.setText (null);
		else
		{
			final Integer reducedCost = isShowReducedUnitCost () ? getCityProductionCalculations ().calculateProductionCost (getClient ().getPlayers (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), unit.getUnitLocation (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (),
				getClient ().getGeneralPublicKnowledge ().getConjunctionEventID (), getClient ().getClientDB (), getUnit ().getUnitID ()) : null;
			
			if ((reducedCost != null) && (reducedCost < getUnit ().getUnitDefinition ().getProductionCost ()))
				currentlyConstructingProductionCost.setText (getTextUtils ().intToStrCommas (reducedCost) + " (" + getTextUtils ().intToStrCommas (getUnit ().getUnitDefinition ().getProductionCost ()) + ")");
			else
				currentlyConstructingProductionCost.setText (getTextUtils ().intToStrCommas (getUnit ().getUnitDefinition ().getProductionCost ()));
		}
		costLabel.setVisible (getUnit ().getUnitDefinition ().getProductionCost () != null);
		
		// Search for upkeeps of the unit
		final Map<String, Integer> upkeepsMap = new HashMap<String, Integer> ();
		unit.listModifiedUpkeepProductionTypeIDs ().forEach (productionTypeID -> upkeepsMap.put (productionTypeID, getUnit ().getModifiedUpkeepValue (productionTypeID)));
		
		// Search for upkeeps from spells cast on the unit
		if (getUnit ().isMemoryUnit ())
			for (final MemoryMaintainedSpell spell : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ())
				if ((spell.getCastingPlayerID () == getUnit ().getOwningPlayerID ()) && (spell.getUnitURN () != null) && (getUnit ().getUnitURN () == spell.getUnitURN ()))
				{
					final Spell spellDef = getClient ().getClientDB ().findSpell (spell.getSpellID (), "showUnit");
					for (final ProductionTypeAndUndoubledValue upkeepValue : spellDef.getSpellUpkeep ())
					{
						Integer value = upkeepsMap.get (upkeepValue.getProductionTypeID ());
						if (value == null)
							value = 0;
						
						value = value + upkeepValue.getUndoubledProductionValue ();
						
						upkeepsMap.put (upkeepValue.getProductionTypeID (), value);
					}
				}					
		
		// Turn the map back into a list
		final List<ProductionTypeAndUndoubledValue> upkeeps = upkeepsMap.entrySet ().stream ().sorted ((e1, e2) -> e1.getKey ().compareTo (e2.getKey ())).map (e ->
		{
			final ProductionTypeAndUndoubledValue upkeepValue = new ProductionTypeAndUndoubledValue ();
			upkeepValue.setProductionTypeID (e.getKey ());
			upkeepValue.setUndoubledProductionValue (e.getValue ());
			return upkeepValue;
		}).collect (Collectors.toList ());

		// Generate an image from the upkeeps
		final BufferedImage upkeepImage = getResourceValueClientUtils ().generateUpkeepImage (upkeeps,
			getPlayerPickUtils ().getQuantityOfPick (getUnit ().getOwningWizard ().getPick (), CommonDatabaseConstants.RETORT_ID_CHANNELER) >= 1);
		currentlyConstructingUpkeep.setIcon ((upkeepImage == null) ? null : new ImageIcon (upkeepImage));
		upkeepLabel.setVisible (upkeepImage != null);
		
		// Add each skill
		// Note we need both basic skills (so that we display skills even if they're being negated, like Resist Elements and Elemental Armour together)
		// and modified skills (so that we display skills that we gain from bonuses even without having the original skill, for +to hit/block)
		final Set<String> basicAndModifiedSkillIDs = new HashSet<String> ();
		basicAndModifiedSkillIDs.addAll (getUnit ().listBasicSkillIDs ());
		basicAndModifiedSkillIDs.addAll (getUnit ().listModifiedSkillIDs ());
		
		// Don't show unit caster skill for heroes - which comes from hero items with +spell skill on them.
		// This can't be done in expandUnitDetails itself, as calculateManaTotal relies on the unit caster skill being there with its correctly calculated value.
		if (getUnit ().isHero ())
			basicAndModifiedSkillIDs.remove (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT);
		
		getUnitSkillListCellRenderer ().setUnit (getUnit ());
		for (final String unitSkillID : basicAndModifiedSkillIDs.stream ().sorted ().collect (Collectors.toList ()))
		{
			// Which list do we display it in?
			final UnitSkillEx unitSkillGfx = getClient ().getClientDB ().findUnitSkill (unitSkillID, "UnitInfoPanel");
			
			if ((unitSkillGfx.getUnitSkillTypeID () == UnitSkillTypeID.ATTRIBUTE) ||
				((unitSkillGfx.getUnitSkillTypeID () == UnitSkillTypeID.MODIFYABLE) && ((getClientConfig ().getDisplayUnitSkillsAsAttributes () == UnitSkillTypeID.MODIFYABLE) ||
																															(getClientConfig ().getDisplayUnitSkillsAsAttributes () == UnitSkillTypeID.FIXED))) ||
				((unitSkillGfx.getUnitSkillTypeID () == UnitSkillTypeID.FIXED) && (getClientConfig ().getDisplayUnitSkillsAsAttributes () == UnitSkillTypeID.FIXED)))
			{
				// Display as unit attribute - these have to display modified values, so we show curses and bonuses as extra or greyed out icons
				if (getUnit ().hasModifiedSkill (unitSkillID))
					unitAttributesItems.addElement (new UnitAttributeWithBreakdownImage (unitSkillID,
						getUnitClientUtils ().generateAttributeImage (getUnit (), unitSkillID)));
			}
			
			// Display as unit skill.				
			// Only add skills with images - some don't have, e.g. Flying, since this shows up on the movement section of the form.
			// Experience is an exception since its images are derived differently.
			else if ((unitSkillID.equals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)) ||
				(getClient ().getClientDB ().findUnitSkill (unitSkillID, "showUnit").getUnitSkillImageFile () != null))
			{
				final UnitSkillOrHeroItemSlot skill = new UnitSkillOrHeroItemSlot ();
				skill.setUnitSkillID (unitSkillID);
				skill.setUnitSkillValue (getUnit ().hasModifiedSkill (unitSkillID) ? getUnit ().getModifiedSkillValue (unitSkillID) : getUnit ().getBasicSkillValue (unitSkillID));
				skill.setDarkened (!getUnit ().hasModifiedSkill (unitSkillID));
				unitSkillsItems.addElement (skill);
			}
			
			// Does this skill mean we should colour the unit image differently?
			if (unitSkillGfx.getUnitSkillCombatColour () != null)
				shadingColours.add (unitSkillGfx.getUnitSkillCombatColour ());
		}
		
		// Add ability to cast fixed spells
		for (final UnitCanCast unitCanCast : getUnit ().getUnitDefinition ().getUnitCanCast ())
			
			// Ignore heroes having spells available to cast from their MP pool - we only want to show spells that are free to cast
			if ((unitCanCast.getNumberOfTimes () != null) && (unitCanCast.getNumberOfTimes () > 0))
			{
				final UnitSkillOrHeroItemSlot skill = new UnitSkillOrHeroItemSlot ();
				skill.setSpellID (unitCanCast.getUnitSpellID ());
				unitSkillsItems.addElement (skill);
			}
		
		// Add hero item slots
		int slotNumber = 0;
		for (final String slotTypeID : getUnit ().getUnitDefinition ().getHeroItemSlot ())
		{
			// Is there an item in this slot?
			NumberedHeroItem item = null;
			if (getUnit ().isMemoryUnit ())
				if (slotNumber < getUnit ().getMemoryUnit ().getHeroItemSlot ().size ())
					item = getUnit ().getMemoryUnit ().getHeroItemSlot ().get (slotNumber).getHeroItem ();
			
			if (item != null)
			{
				// Add item image
				final UnitSkillOrHeroItemSlot skill = new UnitSkillOrHeroItemSlot ();
				skill.setHeroItem (item);
				unitSkillsItems.addElement (skill);
			}
			else
			{
				// Add empty slot image
				final UnitSkillOrHeroItemSlot skill = new UnitSkillOrHeroItemSlot ();
				skill.setHeroItemSlotTypeID (slotTypeID);
				unitSkillsItems.addElement (skill);
			}
			
			slotNumber++;
		}
		
		// Update language dependant labels
		languageChanged ();

		// Show the image of the selected unit
		getAnim ().unregisterRepaintTrigger (null, currentlyConstructingImage);
		final String movingActionID = getUnitCalculations ().determineCombatActionID (getUnit (), true, getClient ().getClientDB ());
		getUnitClientUtils ().registerUnitFiguresAnimation (getUnit ().getUnitID (), movingActionID, 4, currentlyConstructingImage); 
		
		// Show URN?
		if ((unit.isMemoryUnit ()) && (getUnit ().getUnitURN () > 0) && (getClientConfig ().isDebugShowURNs ()))
		{
			urnValue.setText (getTextUtils ().intToStrCommas (getUnit ().getUnitURN ()));
			urnLabel.setVisible (true);
			urnValue.setVisible (true);
		}
		else
		{
			urnLabel.setVisible (false);
			urnValue.setVisible (false);
		}
		
		// Special head count label for hydras
		if (unit.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_DRAW_ONE_FIGURE))
		{
			headsLabel.setVisible (true);
			headsValue.setVisible (true);
			headsValue.setText (Integer.valueOf (getUnit ().calculateAliveFigureCount ()).toString ());
		}
		else
		{
			headsLabel.setVisible (false);
			headsValue.setVisible (false);
		}
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
		// Fixed labels
		upkeepLabel.setText	(getLanguageHolder ().findDescription (getLanguages ().getChangeConstructionScreen ().getUpkeep ()));
		costLabel.setText		(getLanguageHolder ().findDescription (getLanguages ().getChangeConstructionScreen ().getCost ()));

		headsLabel.setText ("Heads");

		// Update text about unit or building being displayed
		currentConstructionChanged ();
	}

	/**
	 * This is called when either the language or building/unit being previewed for construction changes,
	 * so we can update the name and description of it
	 */
	private final void currentConstructionChanged ()
	{
		try
		{
			// Labels if showing a building
			if (building != null)
			{
				final Building buildingDef = getClient ().getClientDB ().findBuilding (getBuilding ().getBuildingID (), "currentConstructionChanged");
				
				currentlyConstructingName.setText (getLanguageHolder ().findDescription (buildingDef.getBuildingName ()));
				currentlyConstructingDescription.setText (getLanguageHolder ().findDescription (buildingDef.getBuildingHelpText ()));
				currentlyConstructingAllows.setText (getClientCityCalculations ().describeWhatBuildingAllows (building.getBuildingID (), (MapCoordinates3DEx) getBuilding ().getCityLocation ()));
				urnLabel.setText (getLanguageHolder ().findDescription (getLanguages ().getChangeConstructionScreen ().getBuildingURN ()));
			}
			
			// Labels if showing a unit
			else if (getUnit () != null)
			{
				currentlyConstructingName.setText (getUnitClientUtils ().getUnitName (getUnit ().getUnit (), UnitNameType.RACE_UNIT_NAME));
				urnLabel.setText (getLanguageHolder ().findDescription (getLanguages ().getChangeConstructionScreen ().getUnitURN ()));
			}
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
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
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
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
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}

	/**
	 * @return Cell renderer for drawing the unit attribute text and component breakdowns
	 */
	public final UnitAttributeListCellRenderer getUnitAttributeListCellRenderer ()
	{
		return unitAttributeListCellRenderer;
	}

	/**
	 * @param rend Cell renderer for drawing the unit attribute text and component breakdowns
	 */
	public final void setUnitAttributeListCellRenderer (final UnitAttributeListCellRenderer rend)
	{
		unitAttributeListCellRenderer = rend;
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
	 * @return If true, will calculate unit cost reduction from Iron Ore or Coal
	 */
	public final boolean isShowReducedUnitCost ()
	{
		return showReducedUnitCost;
	}

	/**
	 * @param r If true, will calculate unit cost reduction from Iron Ore or Coal
	 */
	public final void setShowReducedUnitCost (final boolean r)
	{
		showReducedUnitCost = r;
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
	public final MomImeClientConfig getClientConfig ()
	{
		return clientConfig;
	}

	/**
	 * @param config Client config, containing various combat map settings
	 */
	public final void setClientConfig (final MomImeClientConfig config)
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

	/**
	 * @return Combat UI
	 */
	public final CombatUI getCombatUI ()
	{
		return combatUI;
	}

	/**
	 * @param ui Combat UI
	 */
	public final void setCombatUI (final CombatUI ui)
	{
		combatUI = ui;
	}

	/**
	 * @return City production calculations
	 */
	public final CityProductionCalculations getCityProductionCalculations ()
	{
		return cityProductionCalculations;
	}

	/**
	 * @param c City production calculations
	 */
	public final void setCityProductionCalculations (final CityProductionCalculations c)
	{
		cityProductionCalculations = c;
	}
}