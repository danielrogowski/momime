package momime.client.utils;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.client.config.SpellBookViewMode;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.PlayerPick;
import momime.common.utils.SpellCastType;

/**
 * Client side only helper methods for dealing with spells
 */
public interface SpellClientUtils
{
	/**
	 * NB. This can't work from a MaintainedSpell, since we must be able to right click spells not cast yet,
	 * e.g. during free spell selection at game startup, or spells in spell book. 
	 * 
	 * @param spell Spell to list upkeeps of
	 * @param picks Picks owned by the player who is casting the spell
	 * @return Descriptive list of all the upkeeps of the specified spell; null if the spell has no upkeep
	 * @throws RecordNotFoundException If one of the upkeeps can't be found
	 */
	public String listUpkeepsOfSpell (final Spell spell, final List<PlayerPick> picks) throws RecordNotFoundException;
	
	/**
	 * @param spell Spell to list valid Magic realm/Lifeform type targets of
	 * @return Descriptive list of all the valid Magic realm/Lifeform types for this spell; always returns some text, never null
	 * @throws RecordNotFoundException If one of the magic realms can't be found
	 */
	public String listValidMagicRealmLifeformTypeTargetsOfSpell (final Spell spell) throws RecordNotFoundException;
	
	/**
	 * @param spell Spell to list saving throws of
	 * @return Descriptive list of all the saving throws of the specified curse spell; always returns some text, never null
	 * @throws MomException If there are multiple saving throws listed, but against different unit attributes
	 * @throws RecordNotFoundException If an expected data item can't be found
	 */
	public String listSavingThrowsOfSpell (final Spell spell) throws MomException, RecordNotFoundException;
	
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
	public Image findImageForSpell (final String spellID, final Integer castingPlayerID) throws IOException;

	/**
	 * Used by OverlandEnchantmentsUI to merge the pics of the mirror fading in/out
	 * 
	 * @param sourceImage Source image to start from
	 * @param fadeAnimFrame One frame from the fading animation
	 * @param xOffset How much to offset the sourceImage by
	 * @param yOffset How much to offset the sourceImage by
	 * @return Image which will draw only pixels from sourceImage where the matching pixels in fadeAnimFrame are transparent
	 */
	public BufferedImage mergeImages (final Image sourceImage, final BufferedImage fadeAnimFrame, final int xOffset, final int yOffset);
	
	/**
	 * @param viewMode Current view mode of the spell book UI
	 * @return How many spells can appear on each logical page 
	 */
	public int getSpellsPerPage (final SpellBookViewMode viewMode);
	
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
	public List<SpellBookPage> generateSpellBookPages (final SpellBookViewMode viewMode, final SpellCastType castType)
		throws MomException, RecordNotFoundException, PlayerNotFoundException;
}