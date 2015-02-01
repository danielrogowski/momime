package momime.client.utils;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import momime.client.MomClient;
import momime.client.graphics.database.AnimationEx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.CityViewElement;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.language.database.v0_9_5.UnitAttribute;
import momime.client.ui.PlayerColourImageGenerator;
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.database.SpellHasCityEffect;
import momime.common.database.SpellHasCombatEffect;
import momime.common.database.SpellUpkeep;
import momime.common.database.SpellValidUnitTarget;
import momime.common.database.SummonedUnit;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.PlayerPick;
import momime.common.utils.PlayerPickUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

/**
 * Client side only helper methods for dealing with spells
 */
public final class SpellClientUtilsImpl implements SpellClientUtils
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SpellClientUtilsImpl.class);

	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Player colour image generator */
	private PlayerColourImageGenerator playerColourImageGenerator;
	
	/**
	 * NB. This can't work from a MaintainedSpell, since we must be able to right click spells not cast yet,
	 * e.g. during free spell selection at game startup, or spells in spell book. 
	 * 
	 * @param spell Spell to list upkeeps of
	 * @param picks Picks owned by the player who is casting the spell
	 * @return Descriptive list of all the upkeeps of the specified spell; null if the spell has no upkeep
	 */
	@Override
	public final String listUpkeepsOfSpell (final Spell spell, final List<PlayerPick> picks)
	{
		log.trace ("Entering listUpkeepsOfSpell: " + spell.getSpellID ());
		
		String upkeepList = null;
		for (final SpellUpkeep upkeep : spell.getSpellUpkeep ())
		{
			if (upkeepList == null)
				upkeepList = "";
			else
				upkeepList = upkeepList + ", ";
			
			final ProductionType productionType = getLanguage ().findProductionType (upkeep.getProductionTypeID ());
			final String productionTypeDescription = (productionType == null) ? null : productionType.getProductionTypeDescription ();
			
			// Channeler?
			final String thisUpkeep;
			if ((picks != null) && (getPlayerPickUtils ().getQuantityOfPick (picks, CommonDatabaseConstants.RETORT_ID_CHANNELER) > 0))
				thisUpkeep = getLanguage ().findCategoryEntry ("frmHelp", "SpellUpkeepWithChanneler");
			else
				thisUpkeep = getLanguage ().findCategoryEntry ("frmHelp", "SpellUpkeepWithoutChanneler");
				
			upkeepList = upkeepList + thisUpkeep.replaceAll
				("PRODUCTION_TYPE", (productionTypeDescription != null) ? productionTypeDescription : upkeep.getProductionTypeID ()).replaceAll
				("HALF_UPKEEP_VALUE", getTextUtils ().halfIntToStr (upkeep.getUpkeepValue ())).replaceAll
				("UPKEEP_VALUE", new Integer (upkeep.getUpkeepValue ()).toString ());
		}
		
		// Did we find any?
		final String result = (upkeepList == null) ? null :
			getLanguage ().findCategoryEntry ("frmHelp", "SpellUpkeepList").replaceAll ("UPKEEP_LIST", upkeepList);
		
		log.trace ("Exiting listUpkeepsOfSpell = \"" + result + "\"");
		return result;
	}

	/**
	 * @param spell Spell to list saving throws of
	 * @return Descriptive list of all the saving throws of the specified curse spell; always returns some text, never null
	 * @throws MomException If there are multiple saving throws listed, but against different unit attributes
	 */
	@Override
	public final String listSavingThrowsOfSpell (final Spell spell) throws MomException
	{
		log.trace ("Entering listSavingThrowsOfSpell: " + spell.getSpellID ());
		
		final List<Integer> savingThrowModifiers = new ArrayList<Integer> ();
		String unitAttributeID = null;
		
		// Just because there are some ValidUnitTarget entries doesn't necessarily mean the spell gives a saving throw,
		// the records may just be indicating that the spell can only be used on e.g. chaos+death creatures
		for (final SpellValidUnitTarget target : spell.getSpellValidUnitTarget ())
		{
			// unitAttributeID must be the same for all of them
			if (unitAttributeID == null)
				unitAttributeID = target.getSavingThrowAttributeID ();
			else if (!unitAttributeID.equals (target.getSavingThrowAttributeID ()))
				throw new MomException ("listSavingThrowsOfSpell can't generate text for spell " + spell.getSpellID () +
					" because it has saving throws defined against different unit attributes");
			
			if (target.getSavingThrowModifier () != null)
				savingThrowModifiers.add (target.getSavingThrowModifier ());
		}
		
		final String result;
		if (unitAttributeID == null)
			result = getLanguage ().findCategoryEntry ("frmHelp", "SpellBookNoSavingThrow");
		else
		{
			final UnitAttribute unitAttribute = getLanguage ().findUnitAttribute (unitAttributeID);
			final String unitAttributeDescription = (unitAttribute == null) ? null : unitAttribute.getUnitAttributeDescription ();
			if (savingThrowModifiers.size () == 0)
				result = getLanguage ().findCategoryEntry ("frmHelp", "SpellBookNoSavingThrowModifier").replaceAll
					("UNIT_ATTRIBUTE", (unitAttributeDescription != null) ? unitAttributeDescription : unitAttributeID);
			else if (savingThrowModifiers.size () == 1)
				result = getLanguage ().findCategoryEntry ("frmHelp", "SpellBookSingleSavingThrowModifier").replaceAll
					("UNIT_ATTRIBUTE", (unitAttributeDescription != null) ? unitAttributeDescription : unitAttributeID).replaceAll
					("SAVING_THROW_MODIFIER", getTextUtils ().intToStrPlusMinus (savingThrowModifiers.get (0)));
			else
			{
				Collections.sort (savingThrowModifiers);
				result = getLanguage ().findCategoryEntry ("frmHelp", "SpellBookMultipleSavingThrowModifiers").replaceAll
					("UNIT_ATTRIBUTE", (unitAttributeDescription != null) ? unitAttributeDescription : unitAttributeID).replaceAll
					("SAVING_THROW_MODIFIER_MINIMUM", getTextUtils ().intToStrPlusMinus (savingThrowModifiers.get (0))).replaceAll
					("SAVING_THROW_MODIFIER_MAXIMUM", getTextUtils ().intToStrPlusMinus (savingThrowModifiers.get (savingThrowModifiers.size () - 1)));
			}
		}
		
		log.trace ("Exiting listSavingThrowsOfSpell = \"" + result + "\"");
		return result;
	}
	
	/**
	 * Spell images are derived all sorts of ways, depending on the kind of spell.  This method deals with all that.
	 * Some spells have multiple images, e.g. Summon Hero/Champion doesn't know which actual unit will be
	 * summoned, and Chaos Channels doesn't know which bonus we'll get, so in that case this generates a
	 * merged image showing all of them.
	 * 
	 * @param spellID Spell to find image for
	 * @param castingPlayerID Player who is casting it; can pass as null if desired - the player only affects the colour of the mirror around overland enchantments
	 * @return Image to draw for spell, or null if there isn't one
	 * @throws IOException If a necessary record or image is not found
	 */
	@Override
	public final Image findImageForSpell (final String spellID, final Integer castingPlayerID) throws IOException
	{
		log.trace ("Entering findImageForSpell: " + spellID);
		
		// Get the details about the spell
		final Spell spell = getClient ().getClientDB ().findSpell (spellID, "findImageForSpell");
		
		// Make a list of all applicable images (0..many) and size divisor
		// Alternatively, the image may just get filled in directly
		final List<String> imageFilenames = new ArrayList<String> ();
		BufferedImage image = null;
		int sizeDivisor = 1;
		
		// Deal with spells of different sections appropriately
		if (spell.getSpellBookSectionID () != null)
			switch (spell.getSpellBookSectionID ())
			{
				// Overland enchantments
				case OVERLAND_ENCHANTMENTS:
				{
					final String imageName = getGraphicsDB ().findSpell (spellID, "findImageForSpell").getOverlandEnchantmentImageFile ();
					if (imageName != null)
					{
						final BufferedImage spellImage = getUtils ().loadImage (imageName);
						if (castingPlayerID == null)
							image = spellImage;
						else
						{
							// Now that we got the spell image OK, get the coloured mirror for the caster
							final BufferedImage mirrorImage = getPlayerColourImageGenerator ().getOverlandEnchantmentMirror (castingPlayerID);
							image = new BufferedImage (mirrorImage.getWidth (), mirrorImage.getHeight (), BufferedImage.TYPE_INT_ARGB);
							final Graphics2D g = image.createGraphics ();
							try
							{
								g.drawImage (spellImage, GraphicsDatabaseConstants.IMAGE_MIRROR_X_OFFSET, GraphicsDatabaseConstants.IMAGE_MIRROR_Y_OFFSET, null);
								g.drawImage (mirrorImage, 0, 0, null);
							}
							finally
							{
								g.dispose ();
							}
						}
						
						// Mirrors are really large so show at half size
						sizeDivisor = 2;
					}				
					break;
				}
					
				// Summoning
				case SUMMONING:
				{
					for (final SummonedUnit summonedUnit : spell.getSummonedUnit ())
					{
						final String imageName = getGraphicsDB ().findUnit (summonedUnit.getSummonedUnitID (), "findImageForSpell").getUnitSummonImageFile ();
						if (!imageFilenames.contains (imageName))
							imageFilenames.add (imageName);
					}
					
					// Unit summoning pictures are really large so show at half size
					sizeDivisor = 2;
					break;
				}
					
				// Unit enchantments
				case UNIT_ENCHANTMENTS:
				case UNIT_CURSES:
				{
					for (final UnitSpellEffect unitSpellEffect : spell.getUnitSpellEffect ())
					{
						final String imageName = getGraphicsDB ().findUnitSkill (unitSpellEffect.getUnitSkillID (), "findImageForSpell").getUnitSkillImageFile ();
						if (!imageFilenames.contains (imageName))
							imageFilenames.add (imageName);
					}
					break;
				}
					
				// City enchantments
				case CITY_ENCHANTMENTS:
				case CITY_CURSES:
				{
					final List<CityViewElement> cityViewElements = new ArrayList<CityViewElement> ();
					
					// Spells that create city effects, like Altar of Battle
					for (final SpellHasCityEffect citySpellEffect : spell.getSpellHasCityEffect ())
						cityViewElements.add (getGraphicsDB ().findCitySpellEffect (citySpellEffect.getCitySpellEffectID (), "findImageForSpell"));
					
					// Spells that create buildings, like Wall of Stone
					if (spell.getBuildingID () != null)
						cityViewElements.add (getGraphicsDB ().findBuilding (spell.getBuildingID (), "findImageForSpell"));
					
					// Find the image for each
					for (final CityViewElement cityViewElement : cityViewElements)
						if (cityViewElement.getCityViewAlternativeImageFile () != null)
							imageFilenames.add (cityViewElement.getCityViewAlternativeImageFile ());
						else if (cityViewElement.getCityViewImageFile () != null)
							imageFilenames.add (cityViewElement.getCityViewImageFile ());
						else if (cityViewElement.getCityViewAnimation () != null)
						{
							// Just pick the first animation frame.  Sure it'd be nice to actually have the animations displayed in
							// the help scrolls and anywhere else this is used, but it complicates things enormously having to
							// set up repaint timers, and there's only a handful of spells this actually effects
							// e.g. Dark Rituals, Altar of Battle, Move Fortress
							final AnimationEx anim = getGraphicsDB ().findAnimation (cityViewElement.getCityViewAnimation (), "findImageForSpell");
							if (anim.getFrame ().size () > 0)
								imageFilenames.add (anim.getFrame ().get (0));
						}
					
					break;
				}
					
				// Combat enchantments
				case COMBAT_ENCHANTMENTS:
				{
					for (final SpellHasCombatEffect combatSpellEffect : spell.getSpellHasCombatEffect ())
					{
						final String imageName = getGraphicsDB ().findCombatAreaEffect (combatSpellEffect.getCombatAreaEffectID (), "findImageForSpell").getCombatAreaEffectImageFile ();
						if (!imageFilenames.contains (imageName))
							imageFilenames.add (imageName);
					}
					break;
				}
				
				// No spell image
				default:
					break;
			}
		
		// Merge the images together
		if ((image == null) && (imageFilenames.size () > 0))
		{
			if (imageFilenames.size () == 1)
				image = getUtils ().loadImage (imageFilenames.get (0));
			else
			{
				int totalWidth = 0;
				int maxHeight = 0;
				for (final String filename : imageFilenames)
				{
					final BufferedImage thisImage = getUtils ().loadImage (filename);
					totalWidth = totalWidth + thisImage.getWidth ();
					maxHeight = Math.max (maxHeight, thisImage.getHeight ());
				}
				
				image = new BufferedImage (totalWidth + (sizeDivisor * (imageFilenames.size () - 1)), maxHeight, BufferedImage.TYPE_INT_ARGB);
				final Graphics g = image.getGraphics ();
				try
				{
					int x = 0;
					for (final String filename : imageFilenames)
					{
						final BufferedImage thisImage = getUtils ().loadImage (filename);
						g.drawImage (thisImage, x, maxHeight - thisImage.getHeight (), null);
						x = x + thisImage.getWidth () + sizeDivisor;		// Leave a 1 pixel gap between each image
					}
				}
				finally
				{
					g.dispose ();
				}
			}
		}
		
		// Resize the image down
		final Image resizedImage;
		if ((image == null) || (sizeDivisor == 1))
			resizedImage = image;
		else
			resizedImage = image.getScaledInstance (image.getWidth () / sizeDivisor, image.getHeight () / sizeDivisor, Image.SCALE_SMOOTH);
		
		log.trace ("Exiting findImageForSpell = " + resizedImage);
		return resizedImage;
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
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final LanguageDatabaseEx getLanguage ()
	{
		return languageHolder.getLanguage ();
	}

	/**
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param util Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils util)
	{
		playerPickUtils = util;
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
}