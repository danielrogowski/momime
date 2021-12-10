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
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Event;
import momime.common.database.LanguageText;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitSkillEx;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.DamageCalculationAttackData;
import momime.common.utils.UnitUtils;

/**
 * Breakdown about how a number of potential hits was calculated
 */
public final class DamageCalculationAttackDataEx extends DamageCalculationAttackData implements DamageCalculationText
{
	/** Attacking unit */
	private MemoryUnit attackerUnit;
	
	/** Player who owns the attacking unit */
	private PlayerPublicDetails attackingPlayer;
	
	/** Event that caused the attack */
	private Event event;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Unit utils */
	private UnitUtils unitUtils;

	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** Spell the attack is coming from */
	private Spell spellDef;
	
	/** Unit skill the attack is coming from */
	private UnitSkillEx unitSkillDef;
	
	/**
	 * These damage calculations can live in the history for a long time, easily after the unit(s) in question have been destroyed
	 * and potentially even after the player(s) who own the unit(s) have been kicked out of the game.
	 * So find and record them up front, so we can't fail to find the unitURNs or playerIDs later.
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void preProcess () throws IOException
	{
		if (getAttackerUnitURN () != null)
		{
			setAttackerUnit (getUnitUtils ().findUnitURN (getAttackerUnitURN (),
		    	getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "DamageCalculationAttackDataEx-au"));
	
		    setAttackingPlayer (getMultiplayerSessionUtils ().findPlayerWithID
		    	(getClient ().getPlayers (), getAttackerUnit ().getOwningPlayerID (), "DamageCalculationAttackDataEx-aup"));
		    
		    // If its a ranged attack, then expend ammo
			if (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK.equals (getAttackSkillID ()))
				getUnitCalculations ().decreaseRangedAttackAmmo (getAttackerUnit ());

		}
		else if (getAttackerPlayerID () != null)
		    setAttackingPlayer (getMultiplayerSessionUtils ().findPlayerWithID
			    (getClient ().getPlayers (), getAttackerPlayerID (), "DamageCalculationAttackDataEx-ap"));
		
		// Attack from a spell or unit skill?
		if (getAttackSpellID () != null)
			spellDef = getClient ().getClientDB ().findSpell (getAttackSpellID (), "DamageCalculationAttackDataEx-s");
		else
			unitSkillDef = getClient ().getClientDB ().findUnitSkill (getAttackSkillID (), "DamageCalculationAttackDataEx-u");

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
		if (spellDef != null)
			attackType = getLanguageHolder ().findDescription (spellDef.getSpellName ());
		else
			attackType = getLanguageHolder ().findDescription (unitSkillDef.getUnitSkillDescription ());

		// Now work out the rest of the text
		final List<LanguageText> languageText;
		
		if ((spellDef != null) && (spellDef.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES))
			languageText = getLanguages ().getCombatDamage ().getAttackCurse ();
		
		// Call Chaos casting beneficial spells Healing and Chaos Channels
		else if ((spellDef != null) && ((spellDef.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) ||
			(spellDef.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_UNIT_SPELLS)))
			languageText = getLanguages ().getCombatDamage ().getAttackBeneficial ();
		
		else
			switch (getDamageResolutionTypeID ())
			{
				case CHANCE_OF_DEATH:
					languageText = getLanguages ().getCombatDamage ().getAttackChanceOfDeath ();
					break;
	
				case ZEROES_AMMO:
					languageText = getLanguages ().getCombatDamage ().getAttackZeroesAmmo ();
					break;
	
				// Leave saving throw modifier as positive on here, so the text can say "destroys any with resistance 9 + 2 or less"
				case DISINTEGRATE:
					languageText = getLanguages ().getCombatDamage ().getAttackDisintegrate ();
					break;
	
				// Potential hits here is the number of rolls, not a saving throw modifier, so leave positive
				case RESISTANCE_ROLLS:
					languageText = getLanguages ().getCombatDamage ().getAttackResistanceRolls ();
					break;
	
				case FEAR:
					languageText = getLanguages ().getCombatDamage ().getAttackFear ();
					if (getPotentialHits () != null)
						setPotentialHits (-getPotentialHits ());
					break;
					
				case TERROR:
					languageText = getLanguages ().getCombatDamage ().getAttackTerror ();
					if (getPotentialHits () != null)
						setPotentialHits (-getPotentialHits ());
					break;
					
				case EACH_FIGURE_RESIST_OR_DIE:
					languageText = getLanguages ().getCombatDamage ().getAttackEachFigureResistOrDie ();
					if (getPotentialHits () != null)
						setPotentialHits (-getPotentialHits ());
					break;
	
				case EACH_FIGURE_RESIST_OR_LOSE_1HP:
					languageText = getLanguages ().getCombatDamage ().getAttackEachFigureResistOrLose1HP ();
					if (getPotentialHits () != null)
						setPotentialHits (-getPotentialHits ());
					break;
	
				case SINGLE_FIGURE_RESIST_OR_DIE:
					languageText = getLanguages ().getCombatDamage ().getAttackSingleFigureResistOrDie ();
					if (getPotentialHits () != null)
						setPotentialHits (-getPotentialHits ());
					break;
	
				case UNIT_RESIST_OR_DIE:
					languageText = getLanguages ().getCombatDamage ().getAttackUnitResistOrDie ();
					if (getPotentialHits () != null)
						setPotentialHits (-getPotentialHits ());
					break;
	
				case RESIST_OR_TAKE_DAMAGE:
					languageText = getLanguages ().getCombatDamage ().getAttackResistOrTakeDamage ();
					if (getPotentialHits () != null)
						setPotentialHits (-getPotentialHits ());
					break;
					
				default:
					if ((getAttackerUnitURN () != null) && (getAttackerFigures () != null))
						languageText = getLanguages ().getCombatDamage ().getAttackWithUnit ();
					else
						languageText = getLanguages ().getCombatDamage ().getAttackWithoutUnit ();
			}
		
		String text = "     " + getLanguageHolder ().findDescription (languageText).replaceAll
			("ATTACK_TYPE", attackType).replaceAll
			("POTENTIAL_HITS", (getPotentialHits () == null) ? "0" : Integer.valueOf (getPotentialHits ()).toString ());
		
		if (getAttackingPlayer () != null)
			text = text.replaceAll ("ATTACKER_NAME", getWizardClientUtils ().getPlayerName (getAttackingPlayer ()));
		else if (getEvent () != null)
			text = text.replaceAll ("ATTACKER_NAME", getLanguageHolder ().findDescription (getEvent ().getEventName ()));
		
		if (getAttackerUnit () != null)
			text = text.replaceAll ("ATTACKER_RACE_UNIT_NAME", getUnitClientUtils ().getUnitName (getAttackerUnit (), UnitNameType.RACE_UNIT_NAME));
		
		if (getAttackerFigures () != null)
			text = text.replaceAll ("ATTACKER_FIGURES", getAttackerFigures ().toString ());
		
		if (getAttackStrength () != null)
			text = text.replaceAll ("ATTACK_STRENGTH", getAttackStrength ().toString ());

		if (getDamageTypeID () != null)
			text = text.replaceAll ("DAMAGE_TYPE", getLanguageHolder ().findDescription
				(getClient ().getClientDB ().findDamageType (getDamageTypeID (), "DamageCalculationAttackDataEx").getDamageTypeName ()));
		
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
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
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