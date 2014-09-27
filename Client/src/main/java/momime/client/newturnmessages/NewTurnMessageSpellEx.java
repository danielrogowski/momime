package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Spell;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.SpellBookUI;
import momime.common.messages.v0_9_5.NewTurnMessageSpell;

/**
 * NTM about a spell, either one we've researched or need to pick a target for
 */
public final class NewTurnMessageSpellEx extends NewTurnMessageSpell
	implements NewTurnMessageExpiration, NewTurnMessageSimpleUI, NewTurnMessageClickable, NewTurnMessageMusic
{
	/** Current status of this NTM */
	private NewTurnMessageStatus status;
	
	/** Small font */
	private Font smallFont;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Spell book */
	private SpellBookUI spellBookUI;
	
	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	@Override
	public final NewTurnMessageSortOrder getSortOrder ()
	{
		return NewTurnMessageSortOrder.SORT_ORDER_SPELLS;
	}

	/**
	 * @return Name of music file on the classpath to play when this NTM is displayed; null if this message has no music associated
	 */
	@Override
	public final String getMusicResourceName ()
	{
		return "/momime.client.music/MUSIC_040 - ResearchedASpell-DetectMagic-Awareness-GreatUnsummoning-CharmOfLife.mp3";
	}
	
	/**
	 * @return Image to draw for this NTM, or null to display only text
	 */
	@Override
	public final Image getImage ()
	{
		return null;
	}
	
	/**
	 * @return Text to display for this NTM
	 */
	@Override
	public final String getText ()
	{
		final String languageEntryID;
		if (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched () == null)
			languageEntryID = "ResearchNotChosen";
		else
			languageEntryID = "ResearchChosen";
		
		final Spell oldSpell = getLanguage ().findSpell (getSpellID ());
		final String oldSpellName = (oldSpell != null) ? oldSpell.getSpellName () : null;
		
		final String newSpellID = (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched () == null) ? "" : getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ();
		final Spell newSpell = (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched () == null) ? null : getLanguage ().findSpell
			(getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ());
		final String newSpellName = (newSpell != null) ? newSpell.getSpellName () : null;
		
		return getLanguage ().findCategoryEntry ("NewTurnMessages", languageEntryID).replaceAll
			("OLD_SPELL_NAME", (oldSpellName != null) ? oldSpellName : getSpellID ()).replaceAll
			("NEW_SPELL_NAME", (newSpellName != null) ? newSpellName : newSpellID);
	}
	
	/**
	 * @return Font to display the text in
	 */
	@Override
	public final Font getFont ()
	{
		return getSmallFont ();
	}
	
	/**
	 * @return Colour to display the text in
	 */
	@Override
	public final Color getColour ()
	{
		return MomUIConstants.SILVER;
	}
	
	/**
	 * Clicking on population changes brings up the city screen
	 * @throws Exception If there is a problem
	 */
	@Override
	public final void clicked () throws Exception
	{
		getSpellBookUI ().setVisible (true);
	}
	
	/**
	 * @return Current status of this NTM
	 */
	@Override
	public final NewTurnMessageStatus getStatus ()
	{
		return status;
	}
	
	/**
	 * @param newStatus New status for this NTM
	 */
	@Override
	public final void setStatus (final NewTurnMessageStatus newStatus)
	{
		status = newStatus;
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
	 * @return Spell book
	 */
	public final SpellBookUI getSpellBookUI ()
	{
		return spellBookUI;
	}

	/**
	 * @param ui Spell book
	 */
	public final void setSpellBookUI (final SpellBookUI ui)
	{
		spellBookUI = ui;
	}
}