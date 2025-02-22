package momime.client.calculations.damage;

import java.io.IOException;
import java.util.List;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.client.utils.WizardClientUtils;
import momime.common.database.Event;
import momime.common.database.LanguageText;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.DamageCalculationHeaderData;
import momime.common.utils.UnitUtils;

/**
 * Header about how the source and target of an attack, before any attacks or counterattacks are rolled
 */
public final class DamageCalculationHeaderDataEx extends DamageCalculationHeaderData implements DamageCalculationText
{
	/** Attacking unit */
	private MemoryUnit attackerUnit;
	
	/** Unit being attacked */
	private MemoryUnit defenderUnit;
	
	/** Player who owns the attacking unit */
	private PlayerPublicDetails attackingPlayer;
	
	/** Player who owns unit being attacked */
	private PlayerPublicDetails defenderPlayer;
	
	/** Event that caused the attack */
	private Event event;

	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Unit utils */
	private UnitUtils unitUtils;

	/** Multiplayer client */
	private MomClient client;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/**
	 * These damage calculations can live in the history for a long time, easily after the unit(s) in question have been destroyed
	 * and potentially even after the player(s) who own the unit(s) have been kicked out of the game.
	 * So find and record them up front, so we can't fail to find the unitURNs or playerIDs later.
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void preProcess () throws IOException
	{
		// Find attacking unit and player
		if (getAttackerUnitURN () != null)
		{
			setAttackerUnit (getUnitUtils ().findUnitURN (getAttackerUnitURN (),
			   	getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "DamageCalculationHeaderDataEx-au"));
		    
		    setAttackingPlayer (getMultiplayerSessionUtils ().findPlayerWithID
		    	(getClient ().getPlayers (), getAttackerUnit ().getOwningPlayerID (), "DamageCalculationHeaderDataEx-aup"));
		}
		else if (getAttackerPlayerID () != null)
		    setAttackingPlayer (getMultiplayerSessionUtils ().findPlayerWithID
		    	(getClient ().getPlayers (), getAttackerPlayerID (), "DamageCalculationHeaderDataEx-ap"));

		// Find defending unit and player
		if (getDefenderUnitURN () != null)
		{
		    setDefenderUnit (getUnitUtils ().findUnitURN (getDefenderUnitURN (),
			   	getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "DamageCalculationHeaderDataEx-du"));
		    
		    setDefenderPlayer (getMultiplayerSessionUtils ().findPlayerWithID
		    	(getClient ().getPlayers (), getDefenderUnit ().getOwningPlayerID (), "DamageCalculationHeaderDataEx-dup"));
		}
		
		// Was the attack caused by an event?
		if (getEventID () != null)
			setEvent (getClient ().getClientDB ().findEvent (getEventID (), "DamageCalculationHeaderDataEx"));
	}
	
	/**
	 * @return Text to display for this breakdown line
	 * @throws IOException If there is a problem
	 */
	@Override
	public final String getText () throws IOException
	{
		// Either a unit skill ID or a unit attribute ID
		final String attackType;
		if (getAttackSpellID () != null)
			attackType = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell (getAttackSpellID (), "getText").getSpellName ());
		else
			attackType = getLanguageHolder ().findDescription (getClient ().getClientDB ().findUnitSkill (getAttackSkillID (), "getText").getUnitSkillDescription ());

		// Now work out the rest of the text
		final List<LanguageText> languageText;
		if ((getDefenderUnit () == null) && (getAttackerUnit () == null))
			languageText = getLanguages ().getCombatDamage ().getHeaderWithoutEitherUnit ();

		else if ((getDefenderUnit () != null) && (getAttackerUnit () != null))
			languageText = getLanguages ().getCombatDamage ().getHeaderWithBothUnits ();
		
		else if (getAttackerUnit () != null)
			languageText = getLanguages ().getCombatDamage ().getHeaderWithAttackerOnly ();
		
		else if ((isExistingCurse () != null) && (isExistingCurse ()))
			languageText = getLanguages ().getCombatDamage ().getHeaderWithDefenderOnlyExistingCurse ();
		
		else
			languageText = getLanguages ().getCombatDamage ().getHeaderWithDefenderOnly (); 
		
		String text = getLanguageHolder ().findDescription (languageText).replaceAll
			("ATTACK_TYPE", attackType);
		
		if (getAttackingPlayer () != null)
			text = text.replaceAll ("ATTACKER_NAME", getWizardClientUtils ().getPlayerName (getAttackingPlayer ()));
		else if (getEvent () != null)
			text = text.replaceAll ("ATTACKER_NAME", getLanguageHolder ().findDescription (getEvent ().getEventName ()));

		if (getAttackerUnit () != null)
			text = text.replaceAll ("ATTACKER_RACE_UNIT_NAME", getUnitClientUtils ().getUnitName (getAttackerUnit (), UnitNameType.RACE_UNIT_NAME));

		if (getDefenderPlayer () != null)
			text = text.replaceAll ("DEFENDER_NAME", getWizardClientUtils ().getPlayerName (getDefenderPlayer ()));
		
		if (getDefenderUnit () != null)
			text = text.replaceAll ("DEFENDER_RACE_UNIT_NAME", getUnitClientUtils ().getUnitName (getDefenderUnit (), UnitNameType.RACE_UNIT_NAME));
		
		return text;
	}
	
	/**
	 * @return Attacking unit
	 */
	public final MemoryUnit getAttackerUnit ()
	{
		return attackerUnit;
	}

	/**
	 * @param attacker Attacking unit
	 */
	public final void setAttackerUnit (final MemoryUnit attacker)
	{
		attackerUnit = attacker;
	}
	
	/**
	 * @return Unit being attacked
	 */
	public final MemoryUnit getDefenderUnit ()
	{
		return defenderUnit;
	}
	
	/**
	 * @param defender Unit being attacked
	 */
	public final void setDefenderUnit (final MemoryUnit defender)
	{
		defenderUnit = defender;
	}

	/**
	 * @return Player who owns the attacking unit
	 */
	public final PlayerPublicDetails getAttackingPlayer ()
	{
		return attackingPlayer;
	}

	/**
	 * @param player Player who owns the attacking unit
	 */
	public final void setAttackingPlayer (final PlayerPublicDetails player)
	{
		attackingPlayer = player;
	}
	
	/**
	 * @return Player who owns unit being attacked
	 */
	public final PlayerPublicDetails getDefenderPlayer ()
	{
		return defenderPlayer;
	}
	
	/**
	 * @param player Player who owns unit being attacked
	 */
	public final void setDefenderPlayer (final PlayerPublicDetails player)
	{
		defenderPlayer = player;
	}

	/**
	 * @return Event that caused the attack
	 */
	public final Event getEvent ()
	{
		return event;
	}

	/**
	 * @param e Event that caused the attack
	 */
	public final void setEvent (final Event e)
	{
		event = e;
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
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
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

	/**
	 * @return Client-side unit utils
	 */
	public final UnitClientUtils getUnitClientUtils ()
	{
		return unitClientUtils;
	}

	/**
	 * @param util Client-side unit utils
	 */
	public final void setUnitClientUtils (final UnitClientUtils util)
	{
		unitClientUtils = util;
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
}