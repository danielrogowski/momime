package momime.client.utils;

import java.awt.Image;
import java.io.IOException;
import java.util.List;

import momime.common.MomException;
import momime.common.database.Spell;
import momime.common.messages.PlayerPick;

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
	 */
	public String listUpkeepsOfSpell (final Spell spell, final List<PlayerPick> picks);
	
	/**
	 * @param spell Spell to list saving throws of
	 * @return Descriptive list of all the saving throws of the specified curse spell; always returns some text, never null
	 * @throws MomException If there are multiple saving throws listed, but against different unit attributes
	 */
	public String listSavingThrowsOfSpell (final Spell spell) throws MomException;
	
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
}