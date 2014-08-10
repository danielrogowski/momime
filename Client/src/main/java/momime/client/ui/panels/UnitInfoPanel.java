package momime.client.ui.panels;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import momime.client.MomClient;
import momime.client.calculations.MomClientCityCalculations;
import momime.client.calculations.MomClientUnitCalculations;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.RangedAttackTypeEx;
import momime.client.graphics.database.UnitAttributeEx;
import momime.client.graphics.database.v0_9_5.CityViewElement;
import momime.client.graphics.database.v0_9_5.UnitSkill;
import momime.client.ui.MomUIConstants;
import momime.client.ui.renderer.CellRendererFactory;
import momime.client.ui.renderer.UnitSkillListCellRenderer;
import momime.client.utils.AnimationController;
import momime.client.utils.ResourceValueClientUtils;
import momime.client.utils.TextUtils;
import momime.common.MomException;
import momime.common.calculations.MomUnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_5.Building;
import momime.common.database.v0_9_5.BuildingPopulationProductionModifier;
import momime.common.database.v0_9_5.Unit;
import momime.common.database.v0_9_5.UnitAttribute;
import momime.common.database.v0_9_5.UnitHasSkill;
import momime.common.database.v0_9_5.UnitUpkeep;
import momime.common.messages.v0_9_5.AvailableUnit;
import momime.common.messages.v0_9_5.MemoryBuilding;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.utils.MomUnitAttributeComponent;
import momime.common.utils.MomUnitAttributePositiveNegative;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.GridBagConstraintsNoFill;

/**
 * Unit info screen; used both for displaying real units already on the map, or when changing
 * construction at a city to preview the stats of units we can potentially build.
 * 
 * For the change construction screen, this also has to be able to display buildings, in which
 * case the two areas showing unit attributes + skills and replaced by two text areas
 * showing a description of the building and what it allows.  This switching is done via two CardLayouts.
 */
public final class UnitInfoPanel extends MomClientPanelUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UnitInfoPanel.class);

	/** There's a lot of pixel precision positionining going on here so the panel typically uses no insets or custom insets per component */
	private final static int INSET = 0;
	
	/** Card layout key for when we're displaying a unit */
	private final static String KEY_UNITS = "U";
	
	/** Card layout key for when we're displaying a building */
	private final static String KEY_BUILDINGS = "B";
	
	/** Darkening colour drawn over the top of attributes that are being reduced by a negative effect, e.g. Black Prayer */
	private final static Color COLOUR_NEGATIVE_ATTRIBUTES = new Color (0, 0, 0, 0xA0);
	
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
	private MomClientCityCalculations clientCityCalculations;
	
	/** Multiplayer client */
	private MomClient client;

	/** Unit utils */
	private UnitUtils unitUtils;

	/** Unit calculations */
	private MomUnitCalculations unitCalculations;

	/** Client unit calculations */
	private MomClientUnitCalculations clientUnitCalculations;
	
	/** Factory for creating cell renderers */
	private CellRendererFactory cellRendererFactory;
	
	/** Overall background image */
	private BufferedImage background;
	
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
	
	/** Card layout for top section */
	private CardLayout topCardLayout;
	
	/** Panel that the top card layout is the layout manager for */
	private JPanel topCards;

	/** Panel that the bottom card layout is the layout manager for */
	private JPanel bottomCards;

	/** Card layout for bottom section */
	private CardLayout bottomCardLayout;
	
	/** Long description of building */
	private JTextArea currentlyConstructingDescription;
	
	/** List of what building allows us to construct once we complete it */
	private JTextArea currentlyConstructingAllows;
	
	/** List model of unit skills */
	private DefaultListModel<UnitHasSkill> unitSkillsItems;
	
	/** Map containing all the unit attribute labels */
	private Map<String, JLabel> unitAttributeLabels = new HashMap<String, JLabel> ();
	
	/** Building being displayed */
	private MemoryBuilding building;
	
	/** Unit being displayed */
	private AvailableUnit unit;
	
	/** Cell renderer for drawing the unit skill icons and generating the correct descriptions (some, notably the experience 'skill', aren't straightforward static text) */
	private UnitSkillListCellRenderer unitSkillListCellRenderer;
	
	/**
	 * Sets up the panel once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	public final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/unitDetails.png");
		
		// Fix the size of the panel to be the same as the background
		final Dimension backgroundSize = new Dimension (background.getWidth (), background.getHeight ());
		getPanel ().setMinimumSize (backgroundSize);
		getPanel ().setMaximumSize (backgroundSize);
		getPanel ().setPreferredSize (backgroundSize);

		// Set up layout
		getPanel ().setLayout (new GridBagLayout ());
	
		final Dimension currentlyConstructingImageSize = new Dimension (62, 60);
		
		currentlyConstructingImage = new JPanel ()
		{
			private static final long serialVersionUID = -8785208582910019708L;

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
						final CityViewElement buildingImage = getGraphicsDB ().findBuilding (building.getBuildingID (), "currentlyConstructingBuilding");
						final BufferedImage image = getAnim ().loadImageOrAnimationFrame
							((buildingImage.getCityViewAlternativeImageFile () != null) ? buildingImage.getCityViewAlternativeImageFile () : buildingImage.getCityViewImageFile (),
							buildingImage.getCityViewAnimation ());
					
						g.drawImage (image, (getSize ().width - image.getWidth ()) / 2, (getSize ().height - image.getHeight ()) / 2, null);
					}
					
					// Draw unit
					if (unit != null)
					{
					}
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		currentlyConstructingImage.setOpaque (false);
		currentlyConstructingImage.setMinimumSize (currentlyConstructingImageSize);
		currentlyConstructingImage.setMaximumSize (currentlyConstructingImageSize);
		currentlyConstructingImage.setPreferredSize (currentlyConstructingImageSize);
		
		getPanel ().add (currentlyConstructingImage, getUtils ().createConstraintsNoFill (0, 0, 1, 5, new Insets (0, 0, 0, 6), GridBagConstraintsNoFill.CENTRE));
		
		currentlyConstructingName = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		getPanel ().add (currentlyConstructingName, getUtils ().createConstraintsNoFill (1, 0, 2, 1, INSET, GridBagConstraintsNoFill.WEST));

		costLabel = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		getPanel ().add (costLabel, getUtils ().createConstraintsNoFill (1, 1, 1, 1, new Insets (0, 0, 0, 4), GridBagConstraintsNoFill.WEST));
		
		upkeepLabel = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		getPanel ().add (upkeepLabel, getUtils ().createConstraintsNoFill (1, 2, 1, 1, new Insets (0, 0, 0, 4), GridBagConstraintsNoFill.WEST));
		
		movesLabel = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		getPanel ().add (movesLabel, getUtils ().createConstraintsNoFill (1, 3, 1, 1, new Insets (0, 0, 0, 4), GridBagConstraintsNoFill.WEST));
		
		currentlyConstructingProductionCost = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		getPanel ().add (currentlyConstructingProductionCost, getUtils ().createConstraintsNoFill (2, 1, 1, 1, INSET, GridBagConstraintsNoFill.WEST));
		
		currentlyConstructingUpkeep = new JLabel ();
		getPanel ().add (currentlyConstructingUpkeep, getUtils ().createConstraintsNoFill (2, 2, 1, 1, INSET, GridBagConstraintsNoFill.WEST));
		
		currentlyConstructingMoves = new JLabel ();
		getPanel ().add (currentlyConstructingMoves, getUtils ().createConstraintsNoFill (2, 3, 1, 1, INSET, GridBagConstraintsNoFill.WEST));
		
		// Card layouts
		topCardLayout = new CardLayout ();
		
		topCards = new JPanel (topCardLayout);
		topCards.setOpaque (false);
		getPanel ().add (topCards, getUtils ().createConstraintsNoFill (0, 5, 3, 1, new Insets (6, 0, 0, 0), GridBagConstraintsNoFill.CENTRE));

		bottomCardLayout = new CardLayout ();
		
		bottomCards = new JPanel (bottomCardLayout);
		bottomCards.setOpaque (false);
		getPanel ().add (bottomCards, getUtils ().createConstraintsNoFill (0, 6, 3, 1, new Insets (6, 0, 0, 0), GridBagConstraintsNoFill.CENTRE));
		
		// Top card - buildings
		final Dimension topCardSize = new Dimension (360, 119);

		currentlyConstructingAllows = getUtils ().createWrappingLabel (MomUIConstants.AQUA, getMediumFont ());
		currentlyConstructingAllows.setMinimumSize (topCardSize);
		currentlyConstructingAllows.setMaximumSize (topCardSize);
		currentlyConstructingAllows.setPreferredSize (topCardSize);

		topCards.add (currentlyConstructingAllows, KEY_BUILDINGS);

		// Bottom card - buildings
		final Dimension bottomCardSize = new Dimension (360, 71);

		currentlyConstructingDescription = getUtils ().createWrappingLabel (MomUIConstants.AQUA, getMediumFont ());
		currentlyConstructingDescription.setMinimumSize (bottomCardSize);
		currentlyConstructingDescription.setMaximumSize (bottomCardSize);
		currentlyConstructingDescription.setPreferredSize (bottomCardSize);
		
		bottomCards.add (currentlyConstructingDescription, KEY_BUILDINGS);
		
		// Top card - units
		final JPanel unitAttributesPanel = new JPanel ();
		
		unitAttributesPanel.setOpaque (false);
		unitAttributesPanel.setMinimumSize (topCardSize);
		unitAttributesPanel.setMaximumSize (topCardSize);
		unitAttributesPanel.setPreferredSize (topCardSize);
		
		unitAttributesPanel.setLayout (new GridBagLayout ());
		
		final Dimension attrValuePanelSize = new Dimension (289, 15);
		
		int y = 0;
		for (final UnitAttribute attr : getClient ().getClientDB ().getUnitAttribute ())
		{
			// Label
			final JLabel attrLabel = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
			
			final GridBagConstraints attrConstraints = getUtils ().createConstraintsNoFill (0, y, 1, 1, INSET, GridBagConstraintsNoFill.WEST);
			attrConstraints.weightx = 1;
			unitAttributesPanel.add (attrLabel, attrConstraints);
			unitAttributeLabels.put (attr.getUnitAttributeID (), attrLabel);
			
			// Value
			final JPanel attrValue = new JPanel ()
			{
				private static final long serialVersionUID = -8840801258402387568L;

				/**
				 * Draws icons representing a particular unit attribute
				 */
				@Override
				protected final void paintComponent (final Graphics g)
				{
					try
					{
						// Work out the icon to use to display this type of unit attribute
						final String attributeImageName;
						if (attr.getUnitAttributeID ().equals (CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK))
						{
							// Ranged attacks have their own special rules, so we select the appropriate
							// type of range attack icon, e.g. bow, rock, blue blast.
							final Unit unitInfo = getClient ().getClientDB ().findUnit (unit.getUnitID (), "unitInfoPanel.paintComponent");
							if (unitInfo.getRangedAttackType () == null)
								attributeImageName = null;
							else
							{
								// If there is only a single image then just use it; if there are multiple, then select the right one by weapon grade
								final RangedAttackTypeEx rat = getGraphicsDB ().findRangedAttackType (unitInfo.getRangedAttackType (), "unitInfoPanel.paintComponent");
								if ((unit.getWeaponGrade () == null) || (rat.getRangedAttackTypeWeaponGrade ().size () == 1))
									attributeImageName = rat.getRangedAttackTypeWeaponGrade ().get (0).getUnitDisplayRangedImageFile ();
								else
									attributeImageName = rat.findWeaponGradeImageFile (unit.getWeaponGrade ());
							}
						}
						else
						{
							// Some attribute other than ranged attack; same behaviour as above with weapon grades
							final UnitAttributeEx attrGfx = getGraphicsDB ().findUnitAttribute (attr.getUnitAttributeID (), "unitInfoPanel.paintComponent");
							if ((unit.getWeaponGrade () == null) || (attrGfx.getUnitAttributeWeaponGrade ().size () == 1))
								attributeImageName = attrGfx.getUnitAttributeWeaponGrade ().get (0).getAttributeImageFile ();
							else
								attributeImageName = attrGfx.findWeaponGradeImageFile (unit.getWeaponGrade ());
						}
						
						final BufferedImage attributeImage = (attributeImageName == null) ? null : getUtils ().loadImage (attributeImageName);

						// Do we need to draw any icons faded, due to negative spells (e.g. Black Prayer) or losing hitpoints?
						final int attributeValueIncludingNegatives;
						if (attr.getUnitAttributeID ().equals (CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_HIT_POINTS))
							attributeValueIncludingNegatives = getUnitCalculations ().calculateHitPointsRemainingOfFirstFigure
								(unit, getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
								getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ());
						else
							attributeValueIncludingNegatives = getUnitUtils ().getModifiedAttributeValue (unit, attr.getUnitAttributeID (),
								MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, getClient ().getPlayers (),
								getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
								getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ());
						
						// Calculate and draw each component separately
						int drawnAttributeCount = 0;
						for (final MomUnitAttributeComponent attrComponent : MomUnitAttributeComponent.values ())
							if (attrComponent != MomUnitAttributeComponent.ALL)
							{
								// Work out the total value (without negative effects), and our actual current value (after negative effects),
								// so we can show stats knocked off by e.g. Black Prayer as faded.
								// Simiarly we fade icons for hit points/hearts lost due to damage we've taken.
								final int totalValue = getUnitUtils ().getModifiedAttributeValue (unit, attr.getUnitAttributeID (), attrComponent,
									attr.getUnitAttributeID ().equals (CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_HIT_POINTS) ? MomUnitAttributePositiveNegative.BOTH : MomUnitAttributePositiveNegative.POSITIVE,
									getClient ().getPlayers (),
									getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
									getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ());
								
								if (totalValue > 0)
								{
									// Work out background image according to the component that the bonus is coming from
									final String backgroundImageName;
									switch (attrComponent)
									{
										case BASIC:
											backgroundImageName = "/momime.client.graphics/unitAttributes/basic.png";
											break;
											
										case WEAPON_GRADE:
											backgroundImageName = "/momime.client.graphics/unitAttributes/weaponGrade.png";
											break;
											
										case EXPERIENCE:
											backgroundImageName = "/momime.client.graphics/unitAttributes/experience.png";
											break;
											
										case HERO_SKILLS:
											backgroundImageName = "/momime.client.graphics/unitAttributes/heroSkills.png";
											break;
											
										case COMBAT_AREA_EFFECTS:
											backgroundImageName = "/momime.client.graphics/unitAttributes/combatAreaEffect.png";
											break;
										
										default:
											throw new MomException ("Don't know unit attribute background image to use for component " + attrComponent);
									}
									
									final BufferedImage backgroundImage = getUtils ().loadImage (backgroundImageName);
									
									// Draw right number of attribute icons
									for (int n = 0; n < totalValue; n++)
									{
										final int attrX = ((drawnAttributeCount % 5) * (backgroundImage.getWidth () + 1)) +
											
											// Leave a slightly bigger gap each 5
											(((drawnAttributeCount / 5) % 4) * ((backgroundImage.getWidth () * 5) + 7)) +
													
											// Indent 2nd, 3rd rows (i.e. after 15 or 20) slightly
											((drawnAttributeCount / 20) * 4);
										
										final int attrY = (drawnAttributeCount / 20) * 3;
										
										g.drawImage (backgroundImage, attrX, attrY, null);
										g.drawImage (attributeImage, attrX, attrY, null);
										
										// Dark hit points when we have lost health
										drawnAttributeCount++;
										if (drawnAttributeCount > attributeValueIncludingNegatives)
										{
											g.setColor (COLOUR_NEGATIVE_ATTRIBUTES);
											g.fillRect (attrX, attrY, backgroundImage.getWidth (), backgroundImage.getHeight ());
										}
									}
								}
							}
					}
					catch (final IOException e)
					{
						log.error (e, e);
					}
				}
			};
			
			attrValue.setOpaque (false);
			attrValue.setMinimumSize (attrValuePanelSize);
			attrValue.setMaximumSize (attrValuePanelSize);
			attrValue.setPreferredSize (attrValuePanelSize);
			
			unitAttributesPanel.add (attrValue, getUtils ().createConstraintsNoFill (1, y, 1, 1, new Insets (1, 0, 1, 0), GridBagConstraintsNoFill.EAST));
			y++;
		}

		topCards.add (unitAttributesPanel, KEY_UNITS);
		
		// Bottom card - units
		unitSkillListCellRenderer = getCellRendererFactory ().createUnitSkillListCellRenderer ();
		unitSkillListCellRenderer.setFont (getSmallFont ());
		unitSkillListCellRenderer.setForeground (MomUIConstants.AQUA);
		unitSkillListCellRenderer.init ();
		
		unitSkillsItems = new DefaultListModel<UnitHasSkill> ();
		
		final JList<UnitHasSkill> unitSkillsList = new JList<UnitHasSkill>  ();		
		unitSkillsList.setOpaque (false);
		unitSkillsList.setModel (unitSkillsItems);
		unitSkillsList.setCellRenderer (unitSkillListCellRenderer);
		unitSkillsList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		
		final JScrollPane unitSkillsScrollPane = getUtils ().createTransparentScrollPane (unitSkillsList);
		
		unitSkillsScrollPane.setMinimumSize (bottomCardSize);
		unitSkillsScrollPane.setMaximumSize (bottomCardSize);
		unitSkillsScrollPane.setPreferredSize (bottomCardSize);

		bottomCards.add (unitSkillsScrollPane, KEY_UNITS);
		
		log.trace ("Exiting init");
	}
	
	/**
	 * This is called by the windowClosed handler of ChangeConstructionUI to close down all animations when the panel closes
	 */
	public final void unitInfoPanelClosing ()
	{
		log.trace ("Entering unitInfoPanelClosing");
		
		getAnim ().unregisterRepaintTrigger (null, currentlyConstructingImage);

		log.trace ("Exiting unitInfoPanelClosing");
	}
	
	/**
	 * @param showBuilding Building to show info about
	 * @throws IOException If there is a problem
	 */
	public final void showBuilding (final MemoryBuilding showBuilding) throws IOException
	{
		log.trace ("Entering showBuilding");

		topCardLayout.show (topCards, KEY_BUILDINGS);
		bottomCardLayout.show (bottomCards, KEY_BUILDINGS);

		// Find details about this kind of building
		building = showBuilding;
		unit = null;
		final Building buildingInfo = getClient ().getClientDB ().findBuilding (building.getBuildingID (), "showBuilding");
		
		// Update language independant labels
		currentlyConstructingProductionCost.setText ((buildingInfo.getProductionCost () == null) ? null : getTextUtils ().intToStrCommas (buildingInfo.getProductionCost ()));
		costLabel.setVisible (buildingInfo.getProductionCost () != null);
		movesLabel.setVisible (false);
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
	 * @param showUnit Unit to show info about
	 * @throws IOException If there is a problem
	 */
	public final void showUnit (final AvailableUnit showUnit) throws IOException
	{
		log.trace ("Entering showUnit");
		
		topCardLayout.show (topCards, KEY_UNITS);
		bottomCardLayout.show (bottomCards, KEY_UNITS);

		// Find details about this kind of building
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
			upkeep.setUpkeepValue (getUnitUtils ().getModifiedUpkeepValue (unit, upkeep.getProductionTypeID (), getClient ().getPlayers (), getClient ().getClientDB ()));
			upkeeps.add (upkeep);
		}

		// Generate an image from the upkeeps
		final BufferedImage upkeepImage = getResourceValueClientUtils ().generateUpkeepImage (upkeeps, false);
		currentlyConstructingUpkeep.setIcon ((upkeepImage == null) ? null : new ImageIcon (upkeepImage));
		upkeepLabel.setVisible (upkeepImage != null);
		
		// Generate an image showing movement
		final BufferedImage singleMovementImage = getUtils ().loadImage (getClientUnitCalculations ().findPreferredMovementSkillGraphics (unit).getMovementIconImageFile ());
		final int movementCount = unitInfo.getDoubleMovement () / 2;
		
		final BufferedImage movementImage;
		if (movementCount <= 0)
			movementImage = null;
		else if (movementCount == 1)
			movementImage = singleMovementImage;
		else
		{
			// Create a merged image showing, 2,3, etc movement icons side-by-side
			movementImage = new BufferedImage ((singleMovementImage.getWidth () * movementCount) + movementCount - 1,
				singleMovementImage.getHeight (), BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = movementImage.createGraphics ();
			try
			{
				for (int movementNo = 0; movementNo < movementCount; movementNo++)
					g.drawImage (singleMovementImage, (singleMovementImage.getWidth () + 1) * movementNo, 0, null);
			}
			finally
			{
				g.dispose ();
			}
		}

		currentlyConstructingMoves.setIcon ((movementImage == null) ? null : new ImageIcon (movementImage));
		movesLabel.setVisible (movementImage != null);
		currentlyConstructingMoves.setVisible (movementImage != null);
		
		// Find all skills to show in the list box
		final List<UnitHasSkill> mergedSkills;
		if (unit instanceof MemoryUnit)
			mergedSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), (MemoryUnit) unit);
		else
			mergedSkills = unit.getUnitHasSkill ();
		
		unitSkillsItems.clear ();
		unitSkillListCellRenderer.setUnit (unit);
		for (final UnitHasSkill thisSkill : mergedSkills)
		{
			final UnitSkill skillGfx = getGraphicsDB ().findUnitSkill (thisSkill.getUnitSkillID (), "showUnit");
			
			// Only add skills with images - some don't have, e.g. Flying, since this shows up on the movement section of the form
			if (skillGfx.getUnitSkillImageFile () != null)
			{
				final UnitHasSkill listSkill = new UnitHasSkill ();
				listSkill.setUnitSkillID (thisSkill.getUnitSkillID ());
				listSkill.setUnitSkillValue (getUnitUtils ().getModifiedSkillValue (unit, mergedSkills, thisSkill.getUnitSkillID (), getClient ().getPlayers (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ()));
				unitSkillsItems.addElement (listSkill);
			}
		}
		
		// Update language dependant labels
		currentConstructionChanged ();
		
		log.trace ("Entering showUnit");
	}

	/**
	 * Draws the overall background behind all the other components
	 */
	@Override
	protected final void paintComponent (final Graphics g)
	{
		g.drawImage (background, 0, 0, null);
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
			final momime.client.language.database.v0_9_5.UnitAttribute unitAttrLang = getLanguage ().findUnitAttribute (unitAttr.getKey ());
			unitAttr.getValue ().setText ((unitAttrLang != null) ? unitAttrLang.getUnitAttributeDescription () : unitAttr.getKey ());
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
			final momime.client.language.database.v0_9_5.Building buildingLang = getLanguage ().findBuilding (building.getBuildingID ());
			currentlyConstructingName.setText ((buildingLang != null) ? buildingLang.getBuildingName () : building.getBuildingID ());
			currentlyConstructingDescription.setText ((buildingLang != null) ? buildingLang.getBuildingHelpText () : null);
			currentlyConstructingAllows.setText (getClientCityCalculations ().describeWhatBuildingAllows (building.getBuildingID (), (MapCoordinates3DEx) building.getCityLocation ()));
		}
		
		// Labels if showing a unit
		if (unit != null)
		{
			// Derive name of unit - this might include the race as a prefix if requested; e.g. "Wraiths" or "Demon Lord" or "Klackon Spearmen"
			// N.B. This used to be a separate function in Delphi (UnitName in MomClientDBUtils)
			final MemoryUnit mu = (unit instanceof MemoryUnit) ? (MemoryUnit) unit : null;
			String unitName;
			
			// Is it a hero with a specifically assigned name?
			if ((mu != null) && (mu.getUnitName () != null))
				unitName = mu.getUnitName ();
			
			// Does it use hero names?
			else if ((mu != null) && (mu.getHeroNameID () != null))
				unitName = getLanguage ().findHeroName (mu.getHeroNameID ());
			
			else
			{
				// Regular unit name
				unitName = getLanguage ().findUnit (unit.getUnitID ()).getUnitName ();
			
				// Do we need to prefix the unit name with the name of the race?
				try
				{
					final Unit unitInfo = getClient ().getClientDB ().findUnit (unit.getUnitID (), "currentConstructionChanged");
					if ((unitInfo.isIncludeRaceInUnitName () != null) && (unitInfo.isIncludeRaceInUnitName ()))
						unitName = getLanguage ().findRace (unitInfo.getUnitRaceID ()).getRaceName () + " " + unitName;
				}
				catch (final RecordNotFoundException e)
				{
					// Log the error, but its only in generating the name, so keep going
					log.error (e, e);
				}
			}
			
			currentlyConstructingName.setText (unitName);
		}
		
		log.trace ("Exiting currentConstructionChanged");
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
	public final MomClientCityCalculations getClientCityCalculations ()
	{
		return clientCityCalculations;
	}

	/**
	 * @param calc Client city calculations
	 */
	public final void setClientCityCalculations (final MomClientCityCalculations calc)
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
	 * @return Unit calculations
	 */
	public final MomUnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final MomUnitCalculations calc)
	{
		unitCalculations = calc;
	}

	/**
	 * @return Client unit calculations
	 */
	public final MomClientUnitCalculations getClientUnitCalculations ()
	{
		return clientUnitCalculations;
	}

	/**
	 * @param calc Client unit calculations
	 */
	public final void setClientUnitCalculations (final MomClientUnitCalculations calc)
	{
		clientUnitCalculations = calc;
	}

	/**
	 * @return Factory for creating cell renderers
	 */
	public final CellRendererFactory getCellRendererFactory ()
	{
		return cellRendererFactory;
	}

	/**
	 * @param fac Factory for creating cell renderers
	 */
	public final void setCellRendererFactory (final CellRendererFactory fac)
	{
		cellRendererFactory = fac;
	}
}