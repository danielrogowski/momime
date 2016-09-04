package momime.client.calculations.damage;

import java.io.IOException;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.SpellLang;
import momime.client.language.database.UnitSkillLang;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.client.utils.WizardClientUtils;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.DamageCalculationAttackData;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Breakdown about how a number of potential hits was calculated
 */
public final class DamageCalculationAttackDataEx extends DamageCalculationAttackData implements DamageCalculationText
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (DamageCalculationAttackDataEx.class);
	
	/** Attacking unit */
	private MemoryUnit attackerUnit;
	
	/** Player who owns the attacking unit */
	private PlayerPublicDetails attackingPlayer;
	
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
	
	/**
	 * These damage calculations can live in the history for a long time, easily after the unit(s) in question have been destroyed
	 * and potentially even after the player(s) who own the unit(s) have been kicked out of the game.
	 * So find and record them up front, so we can't fail to find the unitURNs or playerIDs later.
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void preProcess () throws IOException
	{
		log.trace ("Entering preProcess");
		
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
		else
		    setAttackingPlayer (getMultiplayerSessionUtils ().findPlayerWithID
			    (getClient ().getPlayers (), getAttackerPlayerID (), "DamageCalculationAttackDataEx-ap"));

		log.trace ("Exiting preProcess");
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
		{
			final SpellLang spell = getLanguage ().findSpell (getAttackSpellID ());
			final String spellName = (spell == null) ? null : spell.getSpellName ();
			attackType = (spellName != null) ? spellName : getAttackSpellID ();
		}
		else
		{
			final UnitSkillLang unitSkill = getLanguage ().findUnitSkill (getAttackSkillID ());
			final String unitSkillDescription = (unitSkill == null) ? null : unitSkill.getUnitSkillDescription ();
			attackType = (unitSkillDescription != null) ? unitSkillDescription : getAttackSkillID ();
		}

		// Now work out the rest of the text
		final String languageEntryID;
		switch (getDamageResolutionTypeID ())
		{
			case CHANCE_OF_DEATH:
				languageEntryID = "AttackChanceOfDeath";
				break;

			case ZEROES_AMMO:
				languageEntryID = "AttackZeroesAmmo";
				break;

			// Leave saving throw modifier as positive on here, so the text can say "destroys any with resistance 9 + 2 or less"
			case DISINTEGRATE:
				languageEntryID = "AttackDisintegrate";
				break;

			// Potential hits here is the number of rolls, not a saving throw modifier, so leave positive
			case RESISTANCE_ROLLS:
				languageEntryID = "AttackResistanceRolls";
				break;

			case FEAR:
				languageEntryID = "AttackFear";
				if (getPotentialHits () != null)
					setPotentialHits (-getPotentialHits ());
				break;
				
			case EACH_FIGURE_RESIST_OR_DIE:
				languageEntryID = "AttackEachFigureResistOrDie";
				if (getPotentialHits () != null)
					setPotentialHits (-getPotentialHits ());
				break;

			case SINGLE_FIGURE_RESIST_OR_DIE:
				languageEntryID = "AttackSingleFigureResistOrDie";
				if (getPotentialHits () != null)
					setPotentialHits (-getPotentialHits ());
				break;

			case RESIST_OR_TAKE_DAMAGE:
				languageEntryID = "AttackResistOrTakeDamage";
				if (getPotentialHits () != null)
					setPotentialHits (-getPotentialHits ());
				break;
				
			default:
				if ((getAttackerUnitURN () != null) && (getAttackerFigures () != null))
					languageEntryID = "AttackWithUnit";
				else
					languageEntryID = "AttackWithoutUnit";
		}
		
		String text = "     " + getLanguage ().findCategoryEntry ("CombatDamage", languageEntryID).replaceAll
			("ATTACKER_NAME", getWizardClientUtils ().getPlayerName (getAttackingPlayer ())).replaceAll
			("ATTACK_TYPE", attackType).replaceAll
			("POTENTIAL_HITS", (getPotentialHits () == null) ? "0" : new Integer (getPotentialHits ()).toString ());
		
		if (getAttackerUnit () != null)
			text = text.replaceAll ("ATTACKER_RACE_UNIT_NAME", getUnitClientUtils ().getUnitName (getAttackerUnit (), UnitNameType.RACE_UNIT_NAME));
		
		if (getAttackerFigures () != null)
			text = text.replaceAll ("ATTACKER_FIGURES", getAttackerFigures ().toString ());
		
		if (getAttackStrength () != null)
			text = text.replaceAll ("ATTACK_STRENGTH", getAttackStrength ().toString ());

		if (getDamageTypeID () != null)
			text = text.replaceAll ("DAMAGE_TYPE", getLanguage ().findDamageTypeName (getDamageTypeID ()));
		
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