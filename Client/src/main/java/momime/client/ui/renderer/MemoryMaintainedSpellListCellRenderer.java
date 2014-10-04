package momime.client.ui.renderer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Spell;
import momime.client.ui.MomUIConstants;
import momime.common.messages.v0_9_5.MemoryMaintainedSpell;
import momime.common.messages.v0_9_5.MomTransientPlayerPublicKnowledge;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Renderer for writing spell names onto the city screen, and colouring spell names according to the wizard who cast them
 */
public final class MemoryMaintainedSpellListCellRenderer extends JLabel implements ListCellRenderer<MemoryMaintainedSpell>
{
	/** Unique value for serialization */
	private static final long serialVersionUID = 9188194078229760332L;

	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** Multiplayer client */
	private MomClient client;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;

	/**
	 * Output spell name in caster wizard's colour
	 */
	@Override
	public final Component getListCellRendererComponent (final JList<? extends MemoryMaintainedSpell> list, final MemoryMaintainedSpell spell,
		final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		// Get spell name
		final Spell spellLang = getLanguage ().findSpell (spell.getSpellID ());
		final String spellName = (spellLang != null) ? spellLang.getSpellName () : null;
		setText ((spellName != null) ? spellName : spell.getSpellID ());
		
		// Get wizard colour
		final PlayerPublicDetails pub = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), spell.getCastingPlayerID ());
		final MomTransientPlayerPublicKnowledge trans = (pub == null) ? null : (MomTransientPlayerPublicKnowledge) pub.getTransientPlayerPublicKnowledge ();
		final String flagColour = (trans == null) ? null : trans.getFlagColour ();
		setForeground ((flagColour == null) ? MomUIConstants.SILVER : new Color (Integer.parseInt (flagColour, 16)));
		
		return this;
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
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}
}