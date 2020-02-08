package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.SpellLang;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.utils.TextUtils;
import momime.client.utils.WizardClientUtils;
import momime.common.messages.servertoclient.DispelMagicResult;
import momime.common.messages.servertoclient.DispelMagicResultsMessage;

/**
 * Lists the results of casting a Dispel Magic-type spell.  Includes a list of each spell that the wizard tried to dispel and
 * whether the dispel succeeded or not for each.  This gets sent to the wizard who cast the Dispel Magic-type spell, as well as the owner of each spell.
 * e.g. if wizard A tries to dispel spells cast by both wizards B + C, then wizard A will receive this message listing both results, but wizards B + C
 * will receive this message only containing the 1 result applicable to them.
 * 
 * Note this message is informational only, the client need take no action based on the dispelled spells other than to inform the player.
 * Each successfully dispelled spell will separately send out the usual FOW updates to actually remove it.
 * (That is why I don't put the spellURN here, because the spell would already have been removed before the client receives this message.)
 */
public final class DispelMagicResultsMessageImpl extends DispelMagicResultsMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (DispelMagicResultsMessageImpl.class);
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: Player ID " + getCastingPlayerID () + ", " + getDispelMagicResult ().size () + " results");

		// Work out main text
		final StringBuilder text = new StringBuilder ();
		
		if (getCastingPlayerID () == getClient ().getOurPlayerID ())
			text.append (getLanguage ().findCategoryEntry ("DispelMagic", "OurDispelMagicHeading"));
		else
			text.append (getLanguage ().findCategoryEntry ("DispelMagic", "TheirDispelMagicHeading").replaceAll
				("PLAYER_NAME", getWizardClientUtils ().getPlayerName (getMultiplayerSessionUtils ().findPlayerWithID
					(getClient ().getPlayers (), getCastingPlayerID (), "DispelMagicResultsMessageImpl (C)"))));
		
		for (final DispelMagicResult result : getDispelMagicResult ())
		{
			final String languageEntryID = ((getCastingPlayerID () == getClient ().getOurPlayerID ()) ? "OurDispelMagic" : "TheirDispelMagic") +
				(result.isDispelled () ? "Success" : "Fail");
			
			final SpellLang spell = getLanguage ().findSpell (result.getSpellID ());
			final String spellName = (spell == null) ? null : spell.getSpellName ();
			
			String line = getLanguage ().findCategoryEntry ("DispelMagic", languageEntryID).replaceAll
				("SPELL_NAME", (spellName != null) ? spellName : result.getSpellID ()).replaceAll
				("CASTING_COST", getTextUtils ().intToStrCommas (result.getCastingCost ())).replaceAll
				("PERCENTAGE", Integer.valueOf ((int) (result.getChance () * 100d)).toString ());
			
			if (getCastingPlayerID () == getClient ().getOurPlayerID ())
				line = line.replaceAll ("PLAYER_NAME", getWizardClientUtils ().getPlayerName (getMultiplayerSessionUtils ().findPlayerWithID
					(getClient ().getPlayers (), result.getOwningPlayerID (), "DispelMagicResultsMessageImpl (O)")));
			
			text.append (System.lineSeparator () + line);
		}
		
		// Set up message box
		final SpellLang spell = getLanguage ().findSpell (getSpellID ());
		final String spellName = (spell == null) ? null : spell.getSpellName ();
		
		final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
		msg.setTitle ((spellName != null) ? spellName : getSpellID ());
		msg.setText (text.toString ());
		msg.setVisible (true);

		log.trace ("Exiting start");
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
	 * @return Wizard client utils
	 */
	public final WizardClientUtils getWizardClientUtils ()
	{
		return wizardClientUtils;
	}

	/**
	 * @param util Wizard client utils
	 */
	public final void setWizardClientUtils (final WizardClientUtils util)
	{
		wizardClientUtils = util;
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
}