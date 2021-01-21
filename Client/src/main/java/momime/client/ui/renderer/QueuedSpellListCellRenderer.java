package momime.client.ui.renderer;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.QueuedSpell;
import momime.common.utils.SpellUtils;

/**
 * Renderer for writing spell names and remaining casting costs onto the queued spells screen
 */
public final class QueuedSpellListCellRenderer extends JPanel implements ListCellRenderer<QueuedSpell>
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (QueuedSpellListCellRenderer.class);
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** Spell utils */
	private SpellUtils spellUtils;

	/** Multiplayer client */
	private MomClient client;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Label containing the left portion*/
	private JLabel leftLabel;
	
	/** Label containing the right portion */
	private JLabel rightLabel;
	
	/**
	 * Sets up the layout of the panel
	 */
	public final void init ()
	{
		setLayout (new BorderLayout ());
		
		leftLabel = new JLabel ();
		add (leftLabel, BorderLayout.WEST);
		
		rightLabel = new JLabel ();
		add (rightLabel, BorderLayout.EAST);
		
		setOpaque (false);
	}
	
	/**
	 * Output spell name
	 */
	@SuppressWarnings ("unused")
	@Override
	public final Component getListCellRendererComponent (final JList<? extends QueuedSpell> list, final QueuedSpell queued,
		final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		leftLabel.setFont (getFont ());
		leftLabel.setForeground (getForeground ());
		rightLabel.setFont (getFont ());
		rightLabel.setForeground (getForeground ());
		
		// Get spell cost
		String castingCostText = null;
		try
		{
			// Get spell name
			final Spell spell = getClient ().getClientDB ().findSpell (queued.getQueuedSpellID (), "QueuedSpellListCellRenderer");
			if (queued.getHeroItem () != null)
				leftLabel.setText (queued.getHeroItem ().getHeroItemName ());
			else
				leftLabel.setText (getLanguageHolder ().findDescription (spell.getSpellName ()));

			// Work out text
			final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "QueuedSpellListCellRenderer");
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ourPlayer.getPersistentPlayerPublicKnowledge ();
			
			final int castingCost = getSpellUtils ().getReducedOverlandCastingCost (spell, queued.getHeroItem (),
				pub.getPick (), getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());
			
			final String suffix = " " + getLanguageHolder ().findDescription
				(getClient ().getClientDB ().findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "QueuedSpellListCellRenderer").getProductionTypeSuffix ());
			castingCostText = castingCost + suffix; 
			
			// If this is the top spell in the list, show how much we've put towards casting it so far
			if (index == 0)
				castingCostText = getClient ().getOurPersistentPlayerPrivateKnowledge ().getManaSpentOnCastingCurrentSpell () + suffix + " / " + castingCostText; 
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		rightLabel.setText (castingCostText);
		
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
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
	}

	/**
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
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