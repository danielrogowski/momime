package momime.client.ui.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.utils.swing.GridBagConstraintsNoFill;
import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.actions.LoggingAction;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import jakarta.xml.bind.JAXBException;
import momime.client.MomClient;
import momime.client.calculations.ClientCityCalculations;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.MomUIConstants;
import momime.client.ui.PlayerColourImageGenerator;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.frames.CitiesListUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.utils.TextUtils;
import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.CityProductionCalculations;
import momime.common.database.Building;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.LanguageText;
import momime.common.database.RaceEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.database.UnitEx;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.ChangeCityConstructionMessage;
import momime.common.messages.clienttoserver.ChangeOptionalFarmersMessage;
import momime.common.messages.clienttoserver.SellBuildingMessage;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.ResourceValueUtils;

/**
 * Renderer for drawing the details about each city on the cities list screen
 */
public final class CitiesListCellRenderer extends JPanel implements ListCellRenderer<CitiesListEntry>
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (CitiesListCellRenderer.class);
	
	/** Typical inset used on this screen layout */
	private final static int INSET = 0;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** XML layout */
	private XmlLayoutContainerEx citiesListEntryLayout;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Small font */
	private Font smallFont;
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Client city calculations */
	private ClientCityCalculations clientCityCalculations;
	
	/** Cities list */
	private CitiesListUI citiesListUI;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Player colour image generator */
	private PlayerColourImageGenerator playerColourImageGenerator;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** City production calculations */
	private CityProductionCalculations cityProductionCalculations;
	
	/** Background image */
	private BufferedImage background;

	/** Label showing the city name */
	private JLabel cityName;
	
	/** Images showing all the civilian population; clickable to set number of optional farmers */
	private JPanel civilianPanel;
	
	/** Images showing units garrisoned in the city */
	private JPanel unitsPanel;
	
	/** Icon showing the weapon grade units constructed in this city will get */
	private JLabel cityWeaponGrade;
	
	/** Count of how many enchantments are cast on this city */
	private JLabel cityEnchantments;
	
	/** Count of how many curses are cast on this city */
	private JLabel cityCurses;
	
	/** Icon to open popup to select a building to sell */
	private JLabel sellIcon;

	/** Icon to rush buy current construction */
	private JLabel rushBuyIcon;
	
	/** Label showing what's currently being constructed in the city */
	private JLabel cityCurrentlyConstructing;
	
	/** Label showing how many turns what's currently being constructed in the city is going to take */
	private JLabel cityCurrentlyConstructingTurns;
	
	/**
	 * Loads the background image for the panel
	 * @throws IOException If there is a problem
	 */
	public final void init () throws IOException
	{
		background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/citiesListRow.png");
		final BufferedImage rushBuyImage = getUtils ().loadImage ("/momime.client.graphics/production/gold/1.png");
		final BufferedImage pendingSaleImage = getUtils ().loadImage ("/momime.client.graphics/cityView/spellEffects/SE145.png");
		
		setLayout (new XmlLayoutManager (getCitiesListEntryLayout ()));

		cityName = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (cityName, "frmCitiesListRowName");

		civilianPanel = new JPanel ();
		civilianPanel.setOpaque (false);
		civilianPanel.setLayout (new GridBagLayout ());
		add (civilianPanel, "frmCitiesListRowPopulation");

		unitsPanel = new JPanel ();
		unitsPanel.setOpaque (false);
		unitsPanel.setLayout (new GridBagLayout ());
		add (unitsPanel, "frmCitiesListRowUnits");

		cityWeaponGrade = new JLabel ();
		add (cityWeaponGrade, "frmCitiesListRowWeaponGrade");

		cityEnchantments = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		cityEnchantments.setHorizontalAlignment (SwingConstants.CENTER);
		add (cityEnchantments, "frmCitiesListRowEnchantments");

		cityCurses = getUtils ().createLabel (Color.RED, getSmallFont ());
		cityCurses.setHorizontalAlignment (SwingConstants.CENTER);
		add (cityCurses, "frmCitiesListRowCurses");

		sellIcon = getUtils ().createImage (pendingSaleImage);
		add (sellIcon, "frmCitiesListRowSell");

		cityCurrentlyConstructing = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (cityCurrentlyConstructing, "frmCitiesListRowCurrentlyConstructing");
		
		rushBuyIcon = getUtils ().createImage (rushBuyImage);
		add (rushBuyIcon, "frmCitiesListRowRushBuy");
		
		cityCurrentlyConstructingTurns = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (cityCurrentlyConstructingTurns, "frmCitiesListRowCurrentlyConstructingTurns");
	}

	/**
	 * Return this panel to draw itself
	 */
	@SuppressWarnings ("unused")
	@Override
	public final Component getListCellRendererComponent (final JList<? extends CitiesListEntry> list,
		final CitiesListEntry city, final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		cityName.setText (city.getCityName ());

		cityEnchantments.setText (Integer.valueOf (city.getEnchantmentCount ()).toString ());
		cityEnchantments.setVisible (city.getEnchantmentCount () > 0);

		cityCurses.setText (Integer.valueOf (city.getCurseCount ()).toString ());
		cityCurses.setVisible (city.getCurseCount () > 0);
		
		final String buildingID = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(city.getCityLocation ().getZ ()).getRow ().get (city.getCityLocation ().getY ()).getCell ().get (city.getCityLocation ().getX ()).getBuildingIdSoldThisTurn ();
		sellIcon.setVisible (buildingID != null);
		
		civilianPanel.removeAll ();
		unitsPanel.removeAll ();
		try
		{
			cityWeaponGrade.setIcon (new ImageIcon (getUtils ().loadImage (city.getWeaponGradeImageFile ())));

			final RaceEx race = getClient ().getClientDB ().findRace (city.getCityRaceID (), "CitiesListCellRenderer");
			
			// Start with farmers
			BufferedImage civilianImage = getUtils ().loadImage (race.findCivilianImageFile (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER, "CitiesListCellRenderer"));
			final int civvyCount = city.getCityPopulation () / 1000;
			int x = 0;
			for (int civvyNo = 1; civvyNo <= civvyCount; civvyNo++)
			{
				// Is this the first rebel?
				if (civvyNo == civvyCount - city.getNumberOfRebels () + 1)
					civilianImage = getUtils ().loadImage (race.findCivilianImageFile (CommonDatabaseConstants.POPULATION_TASK_ID_REBEL, "CitiesListCellRenderer"));
				
				// Is this the first worker?
				else if (civvyNo == city.getMinimumFarmers () + city.getOptionalFarmers () + 1)
					civilianImage = getUtils ().loadImage (race.findCivilianImageFile (CommonDatabaseConstants.POPULATION_TASK_ID_WORKER, "CitiesListCellRenderer"));
				
				// Left justify all the civilians
				final GridBagConstraints imageConstraints = getUtils ().createConstraintsNoFill (x, 0, 1, 1, INSET, GridBagConstraintsNoFill.SOUTHWEST);
				if (civvyNo == civvyCount)
					imageConstraints.weightx = 1;
				
				civilianPanel.add (getUtils ().createImage (civilianImage), imageConstraints);
				x++;
				
				// If this is the last farmer who's necessary to feed the population (& so we cannot convert them to a worker) then leave a gap to show this
				if (civvyNo == city.getMinimumFarmers ())
				{
					civilianPanel.add (Box.createRigidArea (new Dimension (6, 0)), getUtils ().createConstraintsNoFill (x, 0, 1, 1, INSET, GridBagConstraintsNoFill.SOUTHWEST));
					x++;
				}
			}
			
			// Units
			final int unitCount = (int) getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ().stream ().filter
				(mu -> (city.getCityLocation ().equals (mu.getUnitLocation ())) && (mu.getOwningPlayerID () == getClient ().getOurPlayerID ()) &&
					(mu.getStatus () == UnitStatusID.ALIVE)).count ();
			
			x = 0;
			for (final MemoryUnit mu : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
				if ((city.getCityLocation ().equals (mu.getUnitLocation ())) && (mu.getOwningPlayerID () == getClient ().getOurPlayerID ()) &&
					(mu.getStatus () == UnitStatusID.ALIVE))
				{
					final UnitEx unitDef = getClient ().getClientDB ().findUnit (mu.getUnitID (), "CitiesListCellRenderer");
					final Image image = getPlayerColourImageGenerator ().getOverlandUnitImage (unitDef, getClient ().getOurPlayerID (), false);
					
					// Left justify all the units
					final GridBagConstraints imageConstraints = getUtils ().createConstraintsNoFill (x, 0, 1, 1, INSET, GridBagConstraintsNoFill.SOUTHWEST);
					x++;
					if (x == unitCount)
						imageConstraints.weightx = 1;
					
					unitsPanel.add (getUtils ().createImage (image), imageConstraints);
				}
			
			// Name of what's currently being constructed
			if (city.getCurrentlyConstructingBuildingID () != null)
				cityCurrentlyConstructing.setText (getLanguageHolder ().findDescription
					(getClient ().getClientDB ().findBuilding (city.getCurrentlyConstructingBuildingID (), "CitiesListCellRenderer").getBuildingName ()));
			else
				cityCurrentlyConstructing.setText (getLanguageHolder ().findDescription
					(getClient ().getClientDB ().findUnit (city.getCurrentlyConstructingUnitID (), "CitiesListCellRenderer").getUnitName ()));
			
			// Check if we can rush buy it
			rushBuyIcon.setVisible (isRushBuyAllowed (city));
			
			// Turns to complete construction
			cityCurrentlyConstructingTurns.setText ((city.getConstructionTurns () == null) ? "" : city.getConstructionTurns ().toString ());
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		return this;
	}

	/**
	 * @param city City to test
	 * @return Whether rush buy should be enabled for this city or not
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	private final boolean isRushBuyAllowed (final CitiesListEntry city)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		// Name of what's currently being constructed
		final Integer productionCost = getCityProductionCalculations ().calculateProductionCost (getClient ().getPlayers (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), city.getCityLocation (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (),
			getClient ().getGeneralPublicKnowledge ().getConjunctionEventID (), getClient ().getClientDB (), null);
		
		// Check if we can rush buy it
		boolean rushBuyEnabled = false;
		if (productionCost != null)
		{
			final int goldToRushBuy = getCityCalculations ().goldToRushBuy (productionCost, (city.getProductionSoFar () == null) ? 0 : city.getProductionSoFar ());
			rushBuyEnabled = (goldToRushBuy > 0) && (goldToRushBuy <= getResourceValueUtils ().findAmountStoredForProductionType
				(getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD));
		}
		
		return rushBuyEnabled;
	}
	
	/**
	 * Handles left mouse clicks in a list row
	 * 
	 * @param ev Click event
	 * @param city Which city was clicked on 
	 * @param x X coordinate within the row that was clicked
	 * @param y Y coordinate within the row that was clicked
	 * @throws IOException If there is a problem
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public final void handleClick (final MouseEvent ev, final CitiesListEntry city, final int x, final int y)
		throws IOException, JAXBException, XMLStreamException
	{
		final XmlLayoutComponent cell = getCitiesListEntryLayout ().findComponentAt (x, y);
		if (cell != null)
			switch (cell.getName ())
			{
				// Clicking the name of the city opens up the city screen
				case "frmCitiesListRowName":
					CityViewUI cityView = getClient ().getCityViews ().get (city.getCityLocation ().toString ());
					if (cityView == null)
					{
						cityView = getPrototypeFrameCreator ().createCityView ();
						cityView.setCityLocation (city.getCityLocation ());
						getClient ().getCityViews ().put (city.getCityLocation ().toString (), cityView);
					}
				
					cityView.setVisible (true);
					
					// Show flashing white dot for location of the clicked on city
					getCitiesListUI ().regenerateMiniMapBitmaps ();
					break;
					
				// Clicking the population sets the number of optional farmers, but first we have to figure out exactly which figure was clicked on
				case "frmCitiesListRowPopulation":
					final RaceEx race = getClient ().getClientDB ().findRace (city.getCityRaceID (), "CitiesListCellRenderer");
					final BufferedImage civilianImage = getUtils ().loadImage (race.findCivilianImageFile (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER, "CitiesListCellRenderer"));
					final int minimumFarmersWidth = (civilianImage.getWidth () * city.getMinimumFarmers ()) + 6;
					if (x - cell.getLeft () >= minimumFarmersWidth)
					{
						int optionalFarmers = ((x - cell.getLeft () - minimumFarmersWidth) / civilianImage.getWidth ()) + 1;
						if (optionalFarmers + city.getMinimumFarmers () + city.getNumberOfRebels () <= city.getCityPopulation () / 1000)
						{
							// Clicking on the same number toggles it, so we can turn the last optional farmer into a worker
							if (optionalFarmers == city.getOptionalFarmers ())
								optionalFarmers--;
							
							log.debug ("Requesting optional farmers in city " + city.getCityLocation () + " to be set to " + optionalFarmers + " (from cities list)");
							
							final ChangeOptionalFarmersMessage msg = new ChangeOptionalFarmersMessage ();
							msg.setCityLocation (city.getCityLocation ());
							msg.setOptionalFarmers (optionalFarmers);
							getClient ().getServerConnection ().sendMessageToServer (msg);
						}						
					}
					break;
					
				// Clicking the units column selects those units to move
				case "frmCitiesListRowUnits":
					getOverlandMapProcessing ().showSelectUnitBoxes (city.getCityLocation ());
					getOverlandMapUI ().scrollTo (city.getCityLocation ().getX (), city.getCityLocation ().getY (), city.getCityLocation ().getZ (), true);
					break;
					
				// Clicking the enchantments column brings up a popup list of enchantments we can switch off
				case "frmCitiesListRowEnchantments":
					if ((city.getEnchantmentCount () > 0) && (getClient ().isPlayerTurn ()) && (!getCombatUI ().isVisible ()))
					{
						final JPopupMenu popup = new JPopupMenu ();
						
						for (final MemoryMaintainedSpell spell : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ())
							if ((city.getCityLocation ().equals (spell.getCityLocation ())) && (spell.getCastingPlayerID () == getClient ().getOurPlayerID ()) &&
								(spell.getCitySpellEffectID () != null))
							{
								final String effectName = getLanguageHolder ().findDescription
									(getClient ().getClientDB ().findCitySpellEffect (spell.getCitySpellEffectID (), "CitiesListCellRenderer").getCitySpellEffectName ());
								
								final JMenuItem item = new JMenuItem (new LoggingAction (effectName, (ev2) ->
								{
									final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
									msg.setLanguageTitle (getLanguages ().getSpellCasting ().getSwitchOffSpellTitle ());
									msg.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellCasting ().getSwitchOffSpell ()).replaceAll ("SPELL_NAME", effectName));
									msg.setSwitchOffSpell (spell);
									msg.setVisible (true);
								}));
								
								item.setFont (getSmallFont ());
								popup.add (item);								
							}
						
						popup.show (ev.getComponent (), ev.getX (), ev.getY ());
					}
					break;
					
				// Clicking the sell column brings up a popup list so we can pick a building to sell, maybe
				case "frmCitiesListRowSell":
					final String soldBuildingID = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
						(city.getCityLocation ().getZ ()).getRow ().get (city.getCityLocation ().getY ()).getCell ().get (city.getCityLocation ().getX ()).getBuildingIdSoldThisTurn ();
					if (soldBuildingID != null)
					{
						// There's already a building been sold / being sold
						if (getClient ().getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS)
						{
							final SellBuildingMessage msg = new SellBuildingMessage ();
							msg.setCityLocation (city.getCityLocation ());
							getClient ().getServerConnection ().sendMessageToServer (msg);
						}
					}
					else
					{
						// Build popup menu listing everything that can be sold
						final JPopupMenu popup = new JPopupMenu ();
						boolean anythingCanBeSold = false;
						
						for (final MemoryBuilding building : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding ())
							if (city.getCityLocation ().equals (building.getCityLocation ()))
							{
								final Building buildingDef = getClient ().getClientDB ().findBuilding (building.getBuildingID (), "CitiesListCellRenderer");
								final int goldValue = getMemoryBuildingUtils ().goldFromSellingBuilding (buildingDef);
								if ((goldValue > 0)	&&	// Stop trying to sell Summoning Circle or something else that isn't really a building
									(getMemoryBuildingUtils ().doAnyBuildingsDependOn (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (),
										city.getCityLocation (), building.getBuildingID (), getClient ().getClientDB ()) == null))		// Trying to sell Granary when we have a Farmers' Market
								{
									final String buildingName = getLanguageHolder ().findDescription (buildingDef.getBuildingName ());
									
									final JMenuItem item = new JMenuItem (new LoggingAction
										((buildingName != null) ? buildingName : building.getBuildingID (), (ev2) ->
									{
										// OK - but first check if current construction project depends on the one we're selling
										// If so, then we can still sell it, but it will cancel our current construction project
										final List<LanguageText> languageText;
										String prerequisiteBuildingName = null;
										
										if (((city.getCurrentlyConstructingBuildingID () != null) &&
												(getMemoryBuildingUtils ().isBuildingAPrerequisiteForBuilding (building.getBuildingID (), city.getCurrentlyConstructingBuildingID (), getClient ().getClientDB ()))) ||
											((city.getCurrentlyConstructingUnitID () != null) &&
												(getMemoryBuildingUtils ().isBuildingAPrerequisiteForUnit (building.getBuildingID (), city.getCurrentlyConstructingUnitID (), getClient ().getClientDB ()))))
										{
											languageText = getLanguages ().getBuyingAndSellingBuildings ().getSellPromptPrerequisite ();
											if (city.getCurrentlyConstructingBuildingID () != null)
												prerequisiteBuildingName = getLanguageHolder ().findDescription
													(getClient ().getClientDB ().findBuilding (city.getCurrentlyConstructingBuildingID (), "CitiesListCellRenderer").getBuildingName ());
											else if (city.getCurrentlyConstructingUnitID () != null)
												prerequisiteBuildingName = getLanguageHolder ().findDescription
													(getClient ().getClientDB ().findUnit (city.getCurrentlyConstructingUnitID (), "CitiesListCellRenderer").getUnitName ());
										}
										else
											languageText = getLanguages ().getBuyingAndSellingBuildings ().getSellPromptNormal ();
										
										// Work out the text for the message box
										String text = getLanguageHolder ().findDescription (languageText).replaceAll
											("BUILDING_NAME", getLanguageHolder ().findDescription (buildingDef.getBuildingName ())).replaceAll
											("PRODUCTION_VALUE", getTextUtils ().intToStrCommas (goldValue));
										
										if (prerequisiteBuildingName != null)
											text = text.replaceAll ("PREREQUISITE_NAME", prerequisiteBuildingName);
										
										// Show message box
										final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
										msg.setLanguageTitle (getLanguages ().getBuyingAndSellingBuildings ().getSellTitle ());
										msg.setText (text);
										msg.setCityLocation (city.getCityLocation ());
										msg.setBuildingURN (building.getBuildingURN ());
										msg.setVisible (true);
									}));
									
									item.setFont (getSmallFont ());
									popup.add (item);
									anythingCanBeSold = true;
								}
							}
						
						if (anythingCanBeSold)
							popup.show (ev.getComponent (), ev.getX (), ev.getY ());
					}
					
					break;
					
				// Clicking in the construction column brings up a popup list so we can change the construction of a city
				case "frmCitiesListRowCurrentlyConstructing":
					final MapCoordinates3DEx cityLocation = city.getCityLocation ();
					final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
						(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
					if (cityData.getCityPopulation () >= 1000)
					{
						// Build popup menu listing everything this city can construct
						final JPopupMenu popup = new JPopupMenu ();
	
						for (final Building building : getClientCityCalculations ().listBuildingsCityCanConstruct (cityLocation))
						{
							final String buildingName = getLanguageHolder ().findDescription (building.getBuildingName ());
							
							final JCheckBoxMenuItem item = new JCheckBoxMenuItem (new LoggingAction
								((buildingName != null) ? buildingName : building.getBuildingID (), (ev2) ->
							{
								// Tell server that we want to change our construction
								// Note we don't update our own copy of it on the client - the server will confirm back to us that the choice was OK
								final ChangeCityConstructionMessage msg = new ChangeCityConstructionMessage ();
								msg.setBuildingID (building.getBuildingID ());
								msg.setCityLocation (cityLocation);
								getClient ().getServerConnection ().sendMessageToServer (msg);
							}));
							
							item.setSelected (building.getBuildingID ().equals (cityData.getCurrentlyConstructingBuildingID ()));
							item.setFont (getSmallFont ());
							popup.add (item);
						}
	
						popup.addSeparator ();
						
						for (final Unit unitDef : getCityCalculations ().listUnitsCityCanConstruct (cityLocation, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
							getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getClient ().getClientDB ()))									
						{
							final String unitName = getLanguageHolder ().findDescription (unitDef.getUnitName ());
	
							final JCheckBoxMenuItem item = new JCheckBoxMenuItem (new LoggingAction (unitName, (ev2) ->
							{
								// Tell server that we want to change our construction
								// Note we don't update our own copy of it on the client - the server will confirm back to us that the choice was OK
								final ChangeCityConstructionMessage msg = new ChangeCityConstructionMessage ();
								msg.setUnitID (unitDef.getUnitID ());
								msg.setCityLocation (cityLocation);
								getClient ().getServerConnection ().sendMessageToServer (msg);
							}));
	
							item.setSelected (unitDef.getUnitID ().equals (cityData.getCurrentlyConstructingUnitID ()));
							item.setFont (getSmallFont ());
							popup.add (item);
						}
						
						popup.show (ev.getComponent (), ev.getX (), ev.getY ());
					}
					break;
					
				// Rush buy
				case "frmCitiesListRowRushBuy":
					if (isRushBuyAllowed (city))
						getClientCityCalculations ().showRushBuyPrompt (city.getCityLocation ());
					break;
			}
	}

	/**
	 * Paint the panel background
	 */
	@Override
	protected final void paintComponent (final Graphics g)
	{
		g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
	}
	
	/**
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
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
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getCitiesListEntryLayout ()
	{
		return citiesListEntryLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setCitiesListEntryLayout (final XmlLayoutContainerEx layout)
	{
		citiesListEntryLayout = layout;
	}
	
	/**
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final NdgUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final NdgUIUtils util)
	{
		utils = util;
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
	 * @return City calculations
	 */
	public final CityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final CityCalculations calc)
	{
		cityCalculations = calc;
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
	 * @return Cities list
	 */
	public final CitiesListUI getCitiesListUI ()
	{
		return citiesListUI;
	}

	/**
	 * @param list Cities list
	 */
	public final void setCitiesListUI (final CitiesListUI list)
	{
		citiesListUI = list;
	}

	/**
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param mbu Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils mbu)
	{
		memoryBuildingUtils = mbu;
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
	 * @return Combat UI
	 */
	public final CombatUI getCombatUI ()
	{
		return combatUI;
	}

	/**
	 * @param cui Combat UI
	 */
	public final void setCombatUI (final CombatUI cui)
	{
		combatUI = cui;
	}

	/**
	 * @return Player colour image generator
	 */
	public final PlayerColourImageGenerator getPlayerColourImageGenerator ()
	{
		return playerColourImageGenerator;
	}

	/**
	 * @param gen Player colour image generator
	 */
	public final void setPlayerColourImageGenerator (final PlayerColourImageGenerator gen)
	{
		playerColourImageGenerator = gen;
	}

	/**
	 * @return Turn sequence and movement helper methods
	 */
	public final OverlandMapProcessing getOverlandMapProcessing ()
	{
		return overlandMapProcessing;
	}

	/**
	 * @param proc Turn sequence and movement helper methods
	 */
	public final void setOverlandMapProcessing (final OverlandMapProcessing proc)
	{
		overlandMapProcessing = proc;
	}
	
	/**
	 * @return Overland map UI
	 */
	public final OverlandMapUI getOverlandMapUI ()
	{
		return overlandMapUI;
	}

	/**
	 * @param u Overland map UI
	 */
	public final void setOverlandMapUI (final OverlandMapUI u)
	{
		overlandMapUI = u;
	}

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param r Resource value  utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils r)
	{
		resourceValueUtils = r;
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