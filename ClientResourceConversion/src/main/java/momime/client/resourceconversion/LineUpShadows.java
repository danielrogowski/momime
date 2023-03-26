package momime.client.resourceconversion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.WindowConstants;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import com.ndg.utils.swing.GridBagConstraintsNoFill;
import com.ndg.utils.swing.JPanelWithConstantRepaints;
import com.ndg.utils.swing.ModifiedImageCache;
import com.ndg.utils.swing.ModifiedImageCacheImpl;
import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.NdgUIUtilsImpl;
import com.ndg.utils.swing.actions.MessageDialogAction;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import momime.common.database.Animation;
import momime.common.database.AnimationFrame;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonXsdResourceResolver;
import momime.common.database.MomDatabase;
import momime.common.database.Unit;
import momime.common.database.UnitCombatAction;
import momime.common.database.UnitShadowOffset;

/**
 * UI to display combat figures and their shadows so the shadows can be edited into the right position
 */
public final class LineUpShadows
{
	/** Location of XML to read/update */
	private final static File XML_LOCATION = new File ("W:\\EclipseHome\\SF\\MoMIME\\Server\\src\\external\\resources\\momime.server.database\\Original Master of Magic 1.31 rules.momime.xml");
	
	/** Readable names for the 8 directions */
	private final static List<String> DIRECTIONS = Arrays.asList ("N", "NE", "E", "SE", "S", "SW", "W", "NW");
	
	/** Space between components */
	private final static int INSET = 2;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** For creating resized images */
	private ModifiedImageCache modifiedImageCache;
	
	/** Parsed database XML */
	private MomDatabase db;
	
	/** Map of animations so they can be found faster */
	private Map<String, Animation> animationsMap;
	
	/** Map of units so they can be found faster */
	private Map<String, Unit> unitsMap;
	
	/** Map of raceIDs to the first unit in that race (usually spearmen) */
	private Map<String, Unit> firstUnitOfEachRace;
	
	/** Index into unit list of unit to display */
	private int unitIndex;
	
	/** Unit to display */
	private Unit currentUnit;
	
	/** Unit's walk action */
	private UnitCombatAction walkAction;
	
	/** Unit's melee attack action */
	private UnitCombatAction meleeAction;
	
	/** Unit's walk animations in each direction */
	private List<Animation> walkAnimations;

	/** Unit's melee animations in each direction */
	private List<Animation> meleeAnimations;
	
	/** Switch to previous unit */
	private Action previousUnit;
	
	/** Switch to next unit */
	private Action nextUnit;
	
	/**
	 * Sets up UI
	 * @throws Exception If there is a problem
	 */
	public final void init () throws Exception
	{
		getUtils ().useNimbusLookAndFeel ();
		
		// Read XML
		final URL xsdResource = getClass ().getResource (CommonDatabaseConstants.COMMON_XSD_LOCATION);

		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));
		
		final Schema schema = schemaFactory.newSchema (xsdResource);

		final Unmarshaller unmarshaller = JAXBContext.newInstance (MomDatabase.class).createUnmarshaller ();		
		unmarshaller.setSchema (schema);

		final Marshaller marshaller = JAXBContext.newInstance (MomDatabase.class).createMarshaller ();		
		marshaller.setSchema (schema);
		marshaller.setProperty (Marshaller.JAXB_FORMATTED_OUTPUT, true);
		
		db = (MomDatabase) unmarshaller.unmarshal (XML_LOCATION);
		animationsMap = db.getAnimation ().stream ().collect (Collectors.toMap (a -> a.getAnimationID (), a -> a));
		unitsMap = db.getUnit().stream ().collect (Collectors.toMap (u -> u.getUnitID (), u -> u));
		
		// Find first unit of each race
		firstUnitOfEachRace = new HashMap<String, Unit> ();
		for (final Unit unitDef : db.getUnit ())
			if ((unitDef.getUnitRaceID () != null) && (!firstUnitOfEachRace.containsKey (unitDef.getUnitRaceID ())))
				firstUnitOfEachRace.put (unitDef.getUnitRaceID (), unitDef);
		
		// Unit to display
		selectUnitIndex (0);
		
		// Direction selection
		final SpinnerListModel model = new SpinnerListModel (DIRECTIONS);
		final JSpinner spinner = new JSpinner (model);
		spinner.setMinimumSize (new Dimension (60, 25));
		spinner.setMaximumSize (new Dimension (60, 25));
		spinner.setPreferredSize (new Dimension (60, 25));
		
		// Actions
		previousUnit = new MessageDialogAction ("Prev", (ev) ->
		{
			selectUnitIndex (unitIndex - 1);

			// Floating island
			if (currentUnit.getUnitID ().equals ("UN191"))
				selectUnitIndex (unitIndex - 1);
			
			previousUnit.setEnabled (unitIndex > 0);
			nextUnit.setEnabled (true);
		});
		previousUnit.setEnabled (false);

		nextUnit = new MessageDialogAction ("Next", (ev) ->
		{
			selectUnitIndex (unitIndex + 1);

			// Floating island
			if (currentUnit.getUnitID ().equals ("UN191"))
				selectUnitIndex (unitIndex + 1);
			
			previousUnit.setEnabled (true);
			nextUnit.setEnabled ((unitIndex + 1) < db.getUnit ().size ());
		});
		
		final Action upAction = new MessageDialogAction ("^", (ev) ->
		{
			final int d = DIRECTIONS.indexOf (spinner.getValue ());
			final UnitShadowOffset offset = currentUnit.getUnitShadowOffset ().get (d);
			offset.setShadowOffsetY (offset.getShadowOffsetY () - 1);
		});
		
		final Action downAction = new MessageDialogAction ("v", (ev) ->
		{
			final int d = DIRECTIONS.indexOf (spinner.getValue ());
			final UnitShadowOffset offset = currentUnit.getUnitShadowOffset ().get (d);
			offset.setShadowOffsetY (offset.getShadowOffsetY () + 1);
		});

		final Action down10Action = new MessageDialogAction ("vv", (ev) ->
		{
			final int d = DIRECTIONS.indexOf (spinner.getValue ());
			final UnitShadowOffset offset = currentUnit.getUnitShadowOffset ().get (d);
			offset.setShadowOffsetY (offset.getShadowOffsetY () + 10);
		});
		
		final Action leftAction = new MessageDialogAction ("<", (ev) ->
		{
			final int d = DIRECTIONS.indexOf (spinner.getValue ());
			final UnitShadowOffset offset = currentUnit.getUnitShadowOffset ().get (d);
			offset.setShadowOffsetX (offset.getShadowOffsetX () - 1);
		});
		
		final Action rightAction = new MessageDialogAction (">", (ev) ->
		{
			final int d = DIRECTIONS.indexOf (spinner.getValue ());
			final UnitShadowOffset offset = currentUnit.getUnitShadowOffset ().get (d);
			offset.setShadowOffsetX (offset.getShadowOffsetX () + 1);
		});
		
		final Action offsetUpAction = new MessageDialogAction ("^", (ev) ->
		{
			if (currentUnit.getNewShadowsUnitOffsetY () == null)
				currentUnit.setNewShadowsUnitOffsetY (0);

			currentUnit.setNewShadowsUnitOffsetY (currentUnit.getNewShadowsUnitOffsetY () - 1);
		});
		
		final Action offsetDownAction = new MessageDialogAction ("v", (ev) ->
		{
			if (currentUnit.getNewShadowsUnitOffsetY () == null)
				currentUnit.setNewShadowsUnitOffsetY (0);

			currentUnit.setNewShadowsUnitOffsetY (currentUnit.getNewShadowsUnitOffsetY () + 1);
		});
		
		final Action saveAction = new MessageDialogAction ("Save", (ev) -> marshaller.marshal (db, XML_LOCATION));
		
		// Panel
		final JPanel buttonsPanel = new JPanel (new GridBagLayout ());
		buttonsPanel.add (new JButton (previousUnit), getUtils ().createConstraintsNoFill (0, 1, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		buttonsPanel.add (new JButton (nextUnit), getUtils ().createConstraintsNoFill (1, 1, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		buttonsPanel.add (spinner, getUtils ().createConstraintsNoFill (2, 1, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		buttonsPanel.add (new JButton (upAction), getUtils ().createConstraintsNoFill (4, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		buttonsPanel.add (new JButton (downAction), getUtils ().createConstraintsNoFill (4, 2, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		buttonsPanel.add (new JButton (down10Action), getUtils ().createConstraintsNoFill (5, 2, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		buttonsPanel.add (new JButton (leftAction), getUtils ().createConstraintsNoFill (3, 1, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		buttonsPanel.add (new JButton (rightAction), getUtils ().createConstraintsNoFill (5, 1, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		buttonsPanel.add (new JButton (offsetUpAction), getUtils ().createConstraintsNoFill (6, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		buttonsPanel.add (new JButton (offsetDownAction), getUtils ().createConstraintsNoFill (6, 2, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		buttonsPanel.add (new JButton (saveAction), getUtils ().createConstraintsNoFill (7, 1, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		final JPanelWithConstantRepaints imagesPanel = new JPanelWithConstantRepaints ()
		{
			@Override
			public final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				g.setColor (Color.WHITE);
				
				int x = 0;
				for (int d = 0; d < 8; d++)
				{
					final Animation walkAnimation = walkAnimations.get (d);
					final Animation meleeAnimation = meleeAnimations.get (d);
					final UnitShadowOffset offset = currentUnit.getUnitShadowOffset ().get (d); 
					
					for (int y = 0; y < 6; y++)
						try
						{
							final AnimationFrame frame;
							switch (y)
							{
								// Walking animation
								case 3:
								{
									final double absoluteFrameNumber = System.nanoTime () / (1000000000d / walkAnimation.getAnimationSpeed ());
									final int frameNumber = ((int) absoluteFrameNumber) % walkAnimation.getFrame ().size ();
									
									frame = walkAnimation.getFrame ().get (frameNumber);
									break;
								}
									
								// Static attack image
								case 4:
									frame = meleeAnimation.getFrame ().get (1);
									break;
									
								// Animated attack
								case 5:
									final double absoluteFrameNumber = System.nanoTime () / (1000000000d / meleeAnimation.getAnimationSpeed ());
									final int frameNumber = ((int) absoluteFrameNumber) % meleeAnimation.getFrame ().size ();
									
									frame = meleeAnimation.getFrame ().get (frameNumber);
									break;
								
								// Stacking walking frames
								default:
									frame = walkAnimation.getFrame ().get (y);
							}

							// Note shadow images are already doubled in size, and the offsets shouldn't be doubled either
							int baseY = y * 100;
							if (currentUnit.getNewShadowsUnitOffsetY () != null)
								baseY = baseY + currentUnit.getNewShadowsUnitOffsetY ();
							
							if (frame.getShadowImageFile () != null)
							{
								final BufferedImage shadowImage = getUtils ().loadImage (frame.getShadowImageFile ());
								g.drawImage (shadowImage, (x * 100) + offset.getShadowOffsetX (), baseY + offset.getShadowOffsetY (),
									shadowImage.getWidth () * 2, shadowImage.getHeight (), null);
							}
							
							final String imageFilename = (frame.getShadowlessImageFile () != null) ? frame.getShadowlessImageFile () : frame.getImageFile ();
							final Image image = getModifiedImageCache ().doubleSize (imageFilename);
							g.drawImage (image, x * 100, baseY, null);
							
							// Circle should be in the centre of the base of the unit
							final int r = 4;
							g.drawOval ((x * 100) + (image.getWidth (null) / 2) - (r / 2), (y * 100) + 47 - (r / 2), r, r);
						}
						catch (final IOException e)
						{
							// Likely its a missing image, don't print entire stack trace
							System.out.println (e.getMessage ());
						}
					
					x++;
				}
			}
		};
		
		imagesPanel.setBackground (new Color (16, 90, 57));
		imagesPanel.setMinimumSize (new Dimension (800, 600));
		imagesPanel.setMaximumSize (new Dimension (800, 600));
		imagesPanel.setPreferredSize (new Dimension (800, 600));
		
		final JPanel contentPane = new JPanel (new BorderLayout ());
		contentPane.add (buttonsPanel, BorderLayout.NORTH);
		contentPane.add (imagesPanel, BorderLayout.CENTER);
		
		// Frame
		final JFrame frame = new JFrame ();
		frame.setDefaultCloseOperation (WindowConstants.EXIT_ON_CLOSE);
		frame.setContentPane (contentPane);
		frame.pack ();
		frame.setLocationRelativeTo (null);
		frame.setVisible (true);
	
		imagesPanel.init ("Repaint shadows UI");
	}
	
	/**
	 * @param newIndex New unit index to select, so 0 = first unit
	 */
	private final void selectUnitIndex (final int newIndex)
	{
		unitIndex = newIndex;
		currentUnit = db.getUnit ().get (unitIndex);
		walkAction = currentUnit.getUnitCombatAction ().stream ().filter (a -> a.getCombatActionID ().equals ("WALK")).findAny ().orElse (null);
		walkAnimations = walkAction.getUnitCombatImage ().stream ().map (i -> animationsMap.get (i.getUnitCombatAnimation ())).collect (Collectors.toList ());
		meleeAction = currentUnit.getUnitCombatAction ().stream ().filter (a -> a.getCombatActionID ().equals ("MELEE")).findAny ().orElse (null);
		meleeAnimations = (meleeAction == null) ? null : meleeAction.getUnitCombatImage ().stream ().map (i -> animationsMap.get (i.getUnitCombatAnimation ())).collect (Collectors.toList ());
		
		if ((currentUnit.getUnitShadowOffset ().isEmpty ()) && (!currentUnit.getUnitID ().equals ("UN191")))
		{
			final String unitName = currentUnit.getUnitName ().get (0).getText ();
			
			// Default shadow offsets from another unit
			final String copyFromUnitID;
			if ((unitName.toLowerCase ().contains ("cavalry")) || (unitName.toLowerCase ().contains ("centaurs")) || (unitName.toLowerCase ().contains ("nightmares")) ||
				(unitName.toLowerCase ().contains ("wolf riders")) || (unitName.toLowerCase ().contains ("elven lords")) || (unitName.toLowerCase ().contains ("paladins")) ||
				(unitName.toLowerCase ().contains ("horse")) || (unitName.toLowerCase ().contains ("hound")) || (unitName.toLowerCase ().contains ("death knights")) ||
				(unitName.toLowerCase ().contains ("unicorns")) || (unitIndex < 35))
				
				copyFromUnitID = "UN001";		// First hero - since all horses should line up the same
			else if (unitName.toLowerCase ().contains ("settlers"))
				copyFromUnitID = "UN045";
			else if (unitName.toLowerCase ().contains ("doom drakes"))
				copyFromUnitID = "UN018";		// Draconian hero
			else if (unitName.toLowerCase ().contains ("colossus"))
				copyFromUnitID = "UN082";		// Golem
			else if (unitName.toLowerCase ().contains ("giant"))
				copyFromUnitID = "UN158";		// Fire Giant
			else if (unitName.toLowerCase ().contains ("sky drake"))
				copyFromUnitID = "UN165";		// Great Drake
			else if ((unitName.toLowerCase ().contains ("galley")) || (unitName.toLowerCase ().contains ("ship")))
				copyFromUnitID = "UN036";		// Trieme
			else if ((currentUnit.getUnitRaceID () != null) && (firstUnitOfEachRace.containsKey (currentUnit.getUnitRaceID ())) &&
				(firstUnitOfEachRace.get (currentUnit.getUnitRaceID ()).getUnitShadowOffset ().size () == 8))
				copyFromUnitID = firstUnitOfEachRace.get (currentUnit.getUnitRaceID ()).getUnitID ();		// Try to find spearmen of the right race
			else
				copyFromUnitID = "UN040";		// Barbarian Spearmen - first infantry figure

			final Unit copyFromUnit = unitsMap.get (copyFromUnitID);
			if ((copyFromUnit == null) || (copyFromUnit.getUnitShadowOffset ().size () < 8))
			{
				// No data to copy from
				for (int d = 1; d <= 8; d++)
				{
					final UnitShadowOffset newOffset = new UnitShadowOffset ();
					newOffset.setDirection (d);
					currentUnit.getUnitShadowOffset ().add (newOffset);
				}
			}
			else
				for (final UnitShadowOffset src : copyFromUnit.getUnitShadowOffset ())
				{
					final UnitShadowOffset dest = new UnitShadowOffset ();
					dest.setDirection (src.getDirection ());
					dest.setShadowOffsetX (src.getShadowOffsetX ());
					dest.setShadowOffsetY (src.getShadowOffsetY ());
					currentUnit.getUnitShadowOffset ().add (dest);
				}
		}
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
	 * @return For creating resized images
	 */
	public final ModifiedImageCache getModifiedImageCache ()
	{
		return modifiedImageCache;
	}

	/**
	 * @param m For creating resized images
	 */
	public final void setModifiedImageCache (final ModifiedImageCache m)
	{
		modifiedImageCache = m;
	}
	
	/**
	 * @param args Command line arguments, ignored
	 */
	public final static void main (final String [] args)
	{
		try
		{
			final NdgUIUtilsImpl utils = new NdgUIUtilsImpl ();
			
			final ModifiedImageCacheImpl cache = new ModifiedImageCacheImpl ();
			cache.setUtils (utils);
			
			final LineUpShadows shad = new LineUpShadows ();
			shad.setUtils (utils);
			shad.setModifiedImageCache (cache);
			shad.init ();
		}
		catch (final Exception e)
		{
			e.printStackTrace ();
		}
	}
}