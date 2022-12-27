package momime.client.utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.utils.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.config.SpellBookViewMode;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.PlayerColourImageGenerator;
import momime.client.ui.frames.CombatUI;
import momime.common.MomException;
import momime.common.database.AnimationEx;
import momime.common.database.CityViewElement;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.Unit;
import momime.common.database.UnitCanCast;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.ValidUnitTarget;
import momime.common.messages.PlayerPick;
import momime.common.messages.SpellResearchStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;

/**
 * Client side only helper methods for dealing with spells
 */
public final class SpellClientUtilsImpl implements SpellClientUtils
{
	/** How many spells we show on each page in standard view */
	private final static int SPELLS_PER_PAGE_STANDARD = 4;

	/** How many spells we show on each page in compact view */
	private final static int SPELLS_PER_PAGE_COMPACT = 11;
	
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
	
	/** Player colour image generator */
	private PlayerColourImageGenerator playerColourImageGenerator;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/**
	 * NB. This can't work from a MaintainedSpell, since we must be able to right click spells not cast yet,
	 * e.g. during free spell selection at game startup, or spells in spell book. 
	 * 
	 * @param spell Spell to list upkeeps of
	 * @param picks Picks owned by the player who is casting the spell
	 * @return Descriptive list of all the upkeeps of the specified spell; null if the spell has no upkeep
	 * @throws RecordNotFoundException If one of the upkeeps can't be found
	 */
	@Override
	public final String listUpkeepsOfSpell (final Spell spell, final List<PlayerPick> picks) throws RecordNotFoundException
	{
		String upkeepList = null;
		for (final ProductionTypeAndUndoubledValue upkeep : spell.getSpellUpkeep ())
		{
			if (upkeepList == null)
				upkeepList = "";
			else
				upkeepList = upkeepList + ", ";
			
			final String productionTypeDescription = getLanguageHolder ().findDescription
				(getClient ().getClientDB ().findProductionType (upkeep.getProductionTypeID (), "listUpkeepsOfSpell").getProductionTypeDescription ());

			// Channeler?
			final String thisUpkeep;
			if ((picks != null) && (getPlayerPickUtils ().getQuantityOfPick (picks, CommonDatabaseConstants.RETORT_ID_CHANNELER) > 0))
				thisUpkeep = getLanguageHolder ().findDescription (getLanguages ().getHelpScreen ().getSpellUpkeepWithChanneler ());
			else
				thisUpkeep = getLanguageHolder ().findDescription (getLanguages ().getHelpScreen ().getSpellUpkeepWithoutChanneler ());
				
			upkeepList = upkeepList + thisUpkeep.replaceAll
				("PRODUCTION_TYPE", (productionTypeDescription != null) ? productionTypeDescription : upkeep.getProductionTypeID ()).replaceAll
				("HALF_UPKEEP_VALUE", getTextUtils ().halfIntToStr (upkeep.getUndoubledProductionValue ())).replaceAll
				("UPKEEP_VALUE", Integer.valueOf (upkeep.getUndoubledProductionValue ()).toString ());
		}
		
		// Did we find any?
		final String result = (upkeepList == null) ? null :
			getLanguageHolder ().findDescription (getLanguages ().getHelpScreen ().getSpellUpkeepFixed ()).replaceAll ("UPKEEP_LIST", upkeepList);
		
		return result;
	}

	/**
	 * @param spell Spell to list valid Magic realm/Lifeform type targets of
	 * @return Descriptive list of all the valid Magic realm/Lifeform types for this spell; always returns some text, never null
	 * @throws RecordNotFoundException If one of the magic realms can't be found
	 */
	@Override
	public final String listValidMagicRealmLifeformTypeTargetsOfSpell (final Spell spell) throws RecordNotFoundException
	{
		final StringBuilder magicRealms = new StringBuilder ();
		for (final ValidUnitTarget target : spell.getSpellValidUnitTarget ())
		{
			final String magicRealmPlural = getLanguageHolder ().findDescription
				(getClient ().getClientDB ().findPick (target.getTargetMagicRealmID (), "listValidMagicRealmLifeformTypeTargetsOfSpell").getUnitMagicRealmPlural ());
			
			magicRealms.append (System.lineSeparator () + magicRealmPlural);
		}
		
		final String result = magicRealms.toString ();
		return result;
	}
	
	
	/**
	 * @param spell Spell to list saving throws of
	 * @return Descriptive list of all the saving throws of the specified curse spell; always returns some text, never null
	 * @throws MomException If there are multiple saving throws listed, but against different unit skills
	 * @throws RecordNotFoundException If an expected data item can't be found
	 */
	@Override
	public final String listSavingThrowsOfSpell (final Spell spell) throws MomException, RecordNotFoundException
	{
		final List<Integer> additionalSavingThrowModifiers = new ArrayList<Integer> ();

		// See if spell has any additional saving throw modifiers vs certain magic realms, e.g. Dispel Evil or Holy Word
		for (final ValidUnitTarget target : spell.getSpellValidUnitTarget ())
		{
			final int additionalSavingThrowModifier = (target.getMagicRealmAdditionalSavingThrowModifier () == null) ? 0 : target.getMagicRealmAdditionalSavingThrowModifier ();
			if (!additionalSavingThrowModifiers.contains (additionalSavingThrowModifier))
				additionalSavingThrowModifiers.add (additionalSavingThrowModifier);
		}
		
		final String result;
		
		// Spell allows no saving throw at all, e.g. Web
		if (spell.getCombatBaseDamage () == null)
			result = getLanguageHolder ().findDescription (getLanguages ().getHelpScreen ().getSpellBookNoSavingThrow ());
		
		// Resistance roll, but there's no modifiers at all
		else if ((spell.getCombatBaseDamage () == 0) &&
			((additionalSavingThrowModifiers.size () == 0) || ((additionalSavingThrowModifiers.size () == 1) && (additionalSavingThrowModifiers.get (0) == 0))))
			
			result = getLanguageHolder ().findDescription (getLanguages ().getHelpScreen ().getSpellBookNoSavingThrowModifier ());
		
		// Resistance roll with some penalty, but the penalty is always the same
		else if (additionalSavingThrowModifiers.size () <= 1)
		{
			int savingThrowModifier = spell.getCombatBaseDamage ();
			if (additionalSavingThrowModifiers.size () == 1)
				savingThrowModifier = savingThrowModifier + additionalSavingThrowModifiers.get (0);
			
			result = getLanguageHolder ().findDescription (getLanguages ().getHelpScreen ().getSpellBookSingleSavingThrowModifier ()).replaceAll
				("SAVING_THROW_MODIFIER", getTextUtils ().intToStrPlusMinus (-savingThrowModifier));
		}
		
		// Resistance roll with penalty that varies depending on the magic realm/lifeform type of the target
		else
		{
			Collections.sort (additionalSavingThrowModifiers);
			
			final int savingThrowModifierMin = spell.getCombatBaseDamage () + additionalSavingThrowModifiers.get (0);
			final int savingThrowModifierMax = spell.getCombatBaseDamage () + additionalSavingThrowModifiers.get (additionalSavingThrowModifiers.size () - 1);
			
			result = getLanguageHolder ().findDescription (getLanguages ().getHelpScreen ().getSpellBookMultipleSavingThrowModifiers ()).replaceAll
				("SAVING_THROW_MODIFIER_MINIMUM", getTextUtils ().intToStrPlusMinus (-savingThrowModifierMin)).replaceAll
				("SAVING_THROW_MODIFIER_MAXIMUM", getTextUtils ().intToStrPlusMinus (-savingThrowModifierMax));
		}
		
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
				case SPECIAL_SPELLS:
				{
					final String imageName = spell.getOverlandEnchantmentImageFile ();
					if (imageName != null)
					{
						final BufferedImage spellImage = getUtils ().loadImage (imageName);
						if (castingPlayerID == null)
							image = spellImage;
						else
						{
							// Now that we got the spell image OK, get the coloured mirror for the caster
							final BufferedImage mirrorImage = getPlayerColourImageGenerator ().getModifiedImage (GraphicsDatabaseConstants.OVERLAND_ENCHANTMENTS_MIRROR,
								true, null, null, null, castingPlayerID, null);
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
					for (final String summonedUnitID : spell.getSummonedUnit ())
					{
						final String imageName = getClient ().getClientDB ().findUnit (summonedUnitID, "findImageForSpell").getUnitSummonImageFile ();
						if ((imageName != null) && (!imageFilenames.contains (imageName)))
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
					for (final UnitSkillAndValue unitSpellEffect : spell.getUnitSpellEffect ())
					{
						final String imageName = getClient ().getClientDB ().findUnitSkill (unitSpellEffect.getUnitSkillID (), "findImageForSpell").getUnitSkillImageFile ();
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
					for (final String citySpellEffectID : spell.getSpellHasCityEffect ())
					{
						final CityViewElement cityViewElement = getClient ().getClientDB ().findCityViewElementSpellEffect (citySpellEffectID);
						if (cityViewElement != null)
							cityViewElements.add (cityViewElement);
					}
					
					// Spells that create buildings, like Wall of Stone
					if (spell.getBuildingID () != null)
						cityViewElements.add (getClient ().getClientDB ().findCityViewElementBuilding (spell.getBuildingID (), "findImageForSpell"));
					
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
							// set up repaint timers, and there's only a handful of spells this actually affects
							// e.g. Dark Rituals, Altar of Battle, Move Fortress
							final AnimationEx anim = getClient ().getClientDB ().findAnimation (cityViewElement.getCityViewAnimation (), "findImageForSpell");
							if (anim.getFrame ().size () > 0)
								imageFilenames.add (anim.getFrame ().get (0).getImageFile ());
						}
					
					break;
				}
					
				// Combat enchantments
				case COMBAT_ENCHANTMENTS:
				{
					for (final String combatSpellEffectID : spell.getSpellHasCombatEffect ())
					{
						final String imageName = getClient ().getClientDB ().findCombatAreaEffect (combatSpellEffectID, "findImageForSpell").getCombatAreaEffectImageFile ();
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
				final Graphics2D g = image.createGraphics ();
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
		
		return resizedImage;
	}

	/**
	 * Used by OverlandEnchantmentsUI to merge the pics of the mirror fading in/out
	 * 
	 * @param sourceImage Source image to start from
	 * @param fadeAnimFrame One frame from the fading animation
	 * @param xOffset How much to offset the sourceImage by
	 * @param yOffset How much to offset the sourceImage by
	 * @return Image which will draw only pixels from sourceImage where the matching pixels in fadeAnimFrame are transparent
	 */
	@Override
	public final BufferedImage mergeImages (final Image sourceImage, final BufferedImage fadeAnimFrame, final int xOffset, final int yOffset)
	{
		final BufferedImage mergedImage = new BufferedImage (fadeAnimFrame.getWidth () * 2, fadeAnimFrame.getHeight () * 2, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2 = mergedImage.createGraphics ();
		try
		{
			g2.drawImage (sourceImage, xOffset, yOffset, null);
		}
		finally
		{
			g2.dispose ();
		}
		
		for (int x = 0; x < fadeAnimFrame.getWidth (); x++)
			for (int y = 0; y < fadeAnimFrame.getHeight (); y++)
				if (fadeAnimFrame.getRGB (x, y) != 0)
					for (int x2 = 0; x2 < 2; x2++)
						for (int y2 = 0; y2 < 2; y2++)
							mergedImage.setRGB ((x*2) + x2, (y*2) + y2, 0);
		
		return mergedImage;
	}
	
	/**
	 * @param viewMode Current view mode of the spell book UI
	 * @return How many spells can appear on each logical page 
	 */
	@Override
	public final int getSpellsPerPage (final SpellBookViewMode viewMode)
	{
		return (viewMode == SpellBookViewMode.COMPACT) ? SPELLS_PER_PAGE_COMPACT : SPELLS_PER_PAGE_STANDARD;
	}
	
	/**
	 * When we learn a new spell, updates the spells in the spell book to include it.
	 * That may involve shuffling pages around if a page is now full, or adding new pages if the spell is a kind we didn't previously have.
	 * 
	 * Unlike the original MoM and earlier MoM IME versions, because the spell book can be left up permanently now, it will always
	 * draw all spells - so combat spells are shown when on the overland map, and overland spells are shown in combat, just greyed out.
	 * So here we don't need to pay any attention to the cast type (except that in combat, heroes can make additional spells appear
	 * in the spell book if they know any spells that their controlling wizard does not)
	 * 
	 * @param viewMode Current view mode of the spell book UI
	 * @param castType Whether to generate the spell book for overland or combat casting
	 * @return List of spell book, broken into pages for the UI
	 * @throws MomException If we encounter an unknown research unexpected status
	 * @throws RecordNotFoundException If we can't find a research status for a particular spell
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 */
	@Override
	public final List<SpellBookPage> generateSpellBookPages (final SpellBookViewMode viewMode, final SpellCastType castType)
		throws MomException, RecordNotFoundException, PlayerNotFoundException
	{
		// If it is a unit casting, rather than the wizard
		final List<String> heroKnownSpellIDs = new ArrayList<String> ();
		String overridePickID = null;
		int overrideMaximumMP = -1;
		if ((castType == SpellCastType.COMBAT) && (getCombatUI ().getCastingSource () != null) &&
			(getCombatUI ().getCastingSource ().getCastingUnit () != null) && (getCombatUI ().getCastingSource ().getHeroItemSlotNumber () == null) &&
			(getCombatUI ().getCastingSource ().getFixedSpellNumber () == null))
		{
			// Units with the caster skill (Archangels, Efreets and Djinns) cast spells from their magic realm, totally ignoring whatever spells their controlling wizard knows.
			// Using getModifiedUnitMagicRealmLifeformTypeID makes this account for them casting Death spells instead if you get an undead Archangel or similar.
			// overrideMaximumMP isn't essential, but there's no point us listing spells in the spell book that the unit doesn't have enough MP to cast.
			final ExpandedUnitDetails castingUnit = getCombatUI ().getCastingSource ().getCastingUnit ();
			
			// Heroes can get the caster unit skill from + Spell Skill items
			// Check unit type rather than caster hero skill, just in case we put a + spell skill sword on a non-caster sword hero
			if ((castingUnit.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT)) && (!castingUnit.isHero ()))
				overrideMaximumMP = castingUnit.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT);
			
			if (overrideMaximumMP > 0)
			{
				overridePickID = castingUnit.getModifiedUnitMagicRealmLifeformType ().getCastSpellsFromPickID ();
				if (overridePickID == null)
					overridePickID = castingUnit.getModifiedUnitMagicRealmLifeformType ().getPickID ();
			}
			
			if (overridePickID == null)
			{
				// Get a list of any spells this hero knows, in addition to being able to cast spells from their controlling wizard
				final Unit unitDef = getClient ().getClientDB ().findUnit (getCombatUI ().getCastingSource ().getCastingUnit ().getUnitID (), "generateSpellBookPages");
				for (final UnitCanCast knownSpell : unitDef.getUnitCanCast ())
					if (knownSpell.getNumberOfTimes () == null)
						heroKnownSpellIDs.add (knownSpell.getUnitSpellID ());
			}
		}
			
		// Get a list of all spells we know, and all spells we can research now; grouped by section
		final Map<SpellBookSectionID, List<Spell>> sections = new HashMap<SpellBookSectionID, List<Spell>> ();
		for (final Spell spell : getClient ().getClientDB ().getSpell ())
		{
			final SpellResearchStatusID researchStatus;
			
			// Units can cast spells from a specific magic realm, up to a their maximum MP
			if (overridePickID != null)
				researchStatus = ((overridePickID.equals (spell.getSpellRealm ())) && (spell.getCombatCastingCost () != null) && (spell.getCombatCastingCost () <= overrideMaximumMP)) 
					? SpellResearchStatusID.AVAILABLE : SpellResearchStatusID.UNAVAILABLE;
			
			// Heroes knowing their own spells in addition to spells from their controlling wizard
			else if (heroKnownSpellIDs.contains (spell.getSpellID ()))
				researchStatus = SpellResearchStatusID.AVAILABLE;
			
			// Normal situation of wizard casting, or hero casting spells their controlling wizard knows
			else
				researchStatus = getSpellUtils ().findSpellResearchStatus (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellResearchStatus (), spell.getSpellID ()).getStatus ();
			
			final SpellBookSectionID sectionID = getSpellUtils ().getModifiedSectionID (spell, researchStatus, true);
			if (sectionID != null)
			{
				// Do we have this section already?
				List<Spell> section = sections.get (sectionID);
				if (section == null)
				{
					section = new ArrayList<Spell> ();
					sections.put (sectionID, section);
				}
				
				section.add (spell);
			}
		}
		
		// Sort them into sections
		final List<SpellBookSectionID> sortedSections = new ArrayList<SpellBookSectionID> ();
		sortedSections.addAll (sections.keySet ());
		Collections.sort (sortedSections);
		
		// Go through each section
		final List<SpellBookPage> pages = new ArrayList<SpellBookPage> ();
		for (final SpellBookSectionID sectionID : sortedSections)
		{
			final List<Spell> spells = sections.get (sectionID);
			
			// Sort the spells within this section
			Collections.sort (spells, new SpellSorter (sectionID));
			
			// Divide them into pages with up to SPELLS_PER_PAGE on each page
			boolean first = true;
			while (spells.size () > 0)
			{
				final SpellBookPage page = new SpellBookPage ();
				page.setSectionID (sectionID);
				page.setFirstPageOfSection (first);
				pages.add (page);
				
				while ((spells.size () > 0) && (page.getSpells ().size () < getSpellsPerPage (viewMode)))
				{
					page.getSpells ().add (spells.get (0));
					spells.remove (0);
				}
				
				first = false;
			}
		}
		
		return pages;
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
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
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
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param u Spell utils
	 */
	public final void setSpellUtils (final SpellUtils u)
	{
		spellUtils = u;
	}
}