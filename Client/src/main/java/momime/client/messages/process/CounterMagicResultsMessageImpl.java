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
import momime.common.messages.servertoclient.CounterMagicResult;
import momime.common.messages.servertoclient.CounterMagicResultsMessage;

/**
 * Lists the results of attempting to cast a spell in the presence of CAEs that may counter and block it.
 * Includes a list of each CAE that attmpted to counter it and whether the counter succeeded or not for each.  This gets sent to both players in combat
 * regardless of which one was actually trying to cast, or whether the counter is coming from a Counter Magic spell they cast or a node aura.
 */
public final class CounterMagicResultsMessageImpl extends CounterMagicResultsMessage implements BaseServerToClientMessage
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
		// What type of spell tried to be cast?
		final Spell spell = getClient ().getClientDB ().findSpell (getSpellID (), "CounterMagicResultsMessageImpl (C)");
		
		// Work out main text
		final StringBuilder text = new StringBuilder ();
		
		if (getCastingPlayerID () == getClient ().getOurPlayerID ())
			text.append (getLanguageHolder ().findDescription (getLanguages ().getDispelMagic ().getOurCounterMagicHeading ()).replaceAll
				("SPELL_NAME", getLanguageHolder ().findDescription (spell.getSpellName ())));
		else
			text.append (getLanguageHolder ().findDescription (getLanguages ().getDispelMagic ().getTheirCounterMagicHeading ()).replaceAll
				("SPELL_NAME", getLanguageHolder ().findDescription (spell.getSpellName ())).replaceAll
				("PLAYER_NAME", getWizardClientUtils ().getPlayerName (getMultiplayerSessionUtils ().findPlayerWithID
					(getClient ().getPlayers (), getCastingPlayerID (), "CounterMagicResultsMessageImpl (C)"))));
		
		for (final CounterMagicResult result : getCounterMagicResult ())
		{
			final List<LanguageText> languageText;
			if (result.getOwningPlayerID () == null)
				languageText = result.isDispelled () ? getLanguages ().getDispelMagic ().getNodeCounterMagicSuccess () : getLanguages ().getDispelMagic ().getNodeCounterMagicFail ();
			else if (result.getOwningPlayerID ().equals (getClient ().getOurPlayerID ()))
				languageText = result.isDispelled () ? getLanguages ().getDispelMagic ().getOurCounterMagicSuccess () : getLanguages ().getDispelMagic ().getOurCounterMagicFail ();
			else
				languageText = result.isDispelled () ? getLanguages ().getDispelMagic ().getTheirCounterMagicSuccess () : getLanguages ().getDispelMagic ().getTheirCounterMagicFail ();

			// This is the spell or CAE that is doing the dispelling
			final String caeName;
			if (result.getSpellID () != null)
				caeName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell
					(result.getSpellID (), "CounterMagicResultsMessageImpl (R)").getSpellName ());
			else
				caeName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findCombatAreaEffect
					(result.getCombatAreaEffectID (), "CounterMagicResultsMessageImpl (S)").getCombatAreaEffectDescription ());

			// Replace vars
			String line = getLanguageHolder ().findDescription (languageText).replaceAll
				("COMBAT_AREA_EFFECT_NAME", caeName).replaceAll
				("DISPELLING_POWER", getTextUtils ().intToStrCommas (result.getDispellingPower ())).replaceAll
				("PERCENTAGE", Integer.valueOf ((int) (result.getChance () * 100d)).toString ());
			
			if (result.getOwningPlayerID () != null)
				line = line.replaceAll ("PLAYER_NAME", getWizardClientUtils ().getPlayerName (getMultiplayerSessionUtils ().findPlayerWithID
					(getClient ().getPlayers (), result.getOwningPlayerID (), "CounterMagicResultsMessageImpl (O)")));
			
			text.append (System.lineSeparator () + line);
		}
		
		// Set up message box
		final String spellName = getLanguageHolder ().findDescription (spell.getSpellName ());
		
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