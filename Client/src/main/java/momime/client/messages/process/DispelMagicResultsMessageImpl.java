package momime.client.messages.process;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.utils.TextUtils;
import momime.client.utils.WizardClientUtils;
import momime.common.database.LanguageText;
import momime.common.database.Spell;
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
		// What type of dispelling spell is it
		final Spell dispelSpell = getClient ().getClientDB ().findSpell (getSpellID (), "DispelMagicResultsMessageImpl (C)");
		final boolean isSpellBinding = (dispelSpell.getOverlandMaxDamage () == null) && (dispelSpell.getCombatMaxDamage () == null);
		
		// Work out main text
		final StringBuilder text = new StringBuilder ();
		
		if (isSpellBinding)
		{
			// Spell binding
			if (getCastingPlayerID () == getClient ().getOurPlayerID ())
				text.append (getLanguageHolder ().findDescription (getLanguages ().getDispelMagic ().getOurSpellBindingHeading ()));
			else
				text.append (getLanguageHolder ().findDescription (getLanguages ().getDispelMagic ().getTheirSpellBindingHeading ()).replaceAll
					("PLAYER_NAME", getWizardClientUtils ().getPlayerName (getMultiplayerSessionUtils ().findPlayerWithID
						(getClient ().getPlayers (), getCastingPlayerID (), "DispelMagicResultsMessageImpl (C)"))));
		}
		else
		{
			// Normal dispel spell
			if (getCastingPlayerID () == getClient ().getOurPlayerID ())
				text.append (getLanguageHolder ().findDescription (getLanguages ().getDispelMagic ().getOurDispelMagicHeading ()));
			else
				text.append (getLanguageHolder ().findDescription (getLanguages ().getDispelMagic ().getTheirDispelMagicHeading ()).replaceAll
					("PLAYER_NAME", getWizardClientUtils ().getPlayerName (getMultiplayerSessionUtils ().findPlayerWithID
						(getClient ().getPlayers (), getCastingPlayerID (), "DispelMagicResultsMessageImpl (C)"))));
		}
		
		for (final DispelMagicResult result : getDispelMagicResult ())
		{
			final List<LanguageText> languageText;
			if (isSpellBinding)
			{
				// Spell binding
				if (getCastingPlayerID () == getClient ().getOurPlayerID ())
					languageText = result.isDispelled () ? getLanguages ().getDispelMagic ().getOurSpellBindingSuccess () : getLanguages ().getDispelMagic ().getOurSpellBindingFail ();
				else
					languageText = result.isDispelled () ? getLanguages ().getDispelMagic ().getTheirSpellBindingSuccess () : getLanguages ().getDispelMagic ().getTheirSpellBindingFail ();
			}
			else
			{
				// Normal dispel spell
				if (getCastingPlayerID () == getClient ().getOurPlayerID ())
					languageText = result.isDispelled () ? getLanguages ().getDispelMagic ().getOurDispelMagicSuccess () : getLanguages ().getDispelMagic ().getOurDispelMagicFail ();
				else
					languageText = result.isDispelled () ? getLanguages ().getDispelMagic ().getTheirDispelMagicSuccess () : getLanguages ().getDispelMagic ().getTheirDispelMagicFail ();
			}
				
			// This is the spell that was dispelled (or not)
			final String spellName;
			if (result.getSpellID () != null)
				spellName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell
					(result.getSpellID (), "DispelMagicResultsMessageImpl (R)").getSpellName ());
			else
				spellName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findCombatAreaEffect
					(result.getCombatAreaEffectID (), "DispelMagicResultsMessageImpl (S)").getCombatAreaEffectDescription ());
			
			String line = getLanguageHolder ().findDescription (languageText).replaceAll
				("SPELL_NAME", spellName).replaceAll
				("CASTING_COST", getTextUtils ().intToStrCommas (result.getCastingCost ())).replaceAll
				("PERCENTAGE", Integer.valueOf ((int) (result.getChance () * 100d)).toString ());
			
			if (getCastingPlayerID () == getClient ().getOurPlayerID ())
				line = line.replaceAll ("PLAYER_NAME", getWizardClientUtils ().getPlayerName (getMultiplayerSessionUtils ().findPlayerWithID
					(getClient ().getPlayers (), result.getOwningPlayerID (), "DispelMagicResultsMessageImpl (O)")));
			
			text.append (System.lineSeparator () + line);
		}
		
		// Set up message box
		final String spellName = getLanguageHolder ().findDescription (dispelSpell.getSpellName ());
		
		final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
		msg.setTitle (spellName);
		msg.setText (text.toString ());
		msg.setVisible (true);
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
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
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