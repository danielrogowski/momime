package momime.client.calculations.damage;

import java.io.IOException;
import java.util.List;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.utils.TextUtils;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.client.utils.WizardClientUtils;
import momime.common.database.LanguageText;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.DamageCalculationDefenceData;
import momime.common.utils.UnitUtils;

/**
 * Breakdown about how damage from a number of potential hits was applied to a unit
 */
public final class DamageCalculationDefenceDataEx extends DamageCalculationDefenceData implements DamageCalculationText
{
	/** Unit being attacked */
	private MemoryUnit defenderUnit;
	
	/** Player who owns unit being attacked */
	private PlayerPublicDetails defenderPlayer;

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
	
	/** Text utils */
	private TextUtils textUtils;
	
	/**
	 * These damage calculations can live in the history for a long time, easily after the unit(s) in question have been destroyed
	 * and potentially even after the player(s) who own the unit(s) have been kicked out of the game.
	 * So find and record them up front, so we can't fail to find the unitURNs or playerIDs later.
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void preProcess () throws IOException
	{
	    setDefenderUnit (getUnitUtils ().findUnitURN (getDefenderUnitURN (),
	    	getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "DamageCalculationDefenceDataEx-du"));
		    
	    setDefenderPlayer (getMultiplayerSessionUtils ().findPlayerWithID
	    	(getClient ().getPlayers (), getDefenderUnit ().getOwningPlayerID (), "DamageCalculationDefenceDataEx-dp"));
	}
	
	/**
	 * @return Text to display for this breakdown line
	 * @throws IOException If there is a problem
	 */
	@Override
	public final String getText () throws IOException
	{
		// First check whether defence was reduced or increased
		String defenceStrength = "";
		if (getUnmodifiedDefenceStrength () != null)
		{
			if ((getModifiedDefenceStrength () == null) || (getUnmodifiedDefenceStrength ().equals (getModifiedDefenceStrength ())))
				defenceStrength = getUnmodifiedDefenceStrength ().toString ();
			
			else
			{
				final List<LanguageText> languageText = (getModifiedDefenceStrength () > getUnmodifiedDefenceStrength ()) ?
					getLanguages ().getCombatDamage ().getDefenceStrengthIncreased () : getLanguages ().getCombatDamage ().getDefenceStrengthReduced ();
					
				defenceStrength = getLanguageHolder ().findDescription (languageText).replaceAll
					("UNMODIFIED_DEFENCE_STRENGTH", getUnmodifiedDefenceStrength ().toString ()).replaceAll
					("MODIFIED_DEFENCE_STRENGTH", getModifiedDefenceStrength ().toString ());
			}
		}
		
		// Now work out main text
		final List<LanguageText> languageText;
		if (getDamageResolutionTypeID () == null)
		{
			// This only happens when making a resistance roll
			if (getFinalHits () > 0)
				languageText = getLanguages ().getCombatDamage ().getDefenceCursed ();
			else
				languageText = getLanguages ().getCombatDamage ().getDefenceResistsCurse ();
		}
		else
			switch (getDamageResolutionTypeID ())
			{
				case RESISTANCE_ROLLS:
					languageText = getLanguages ().getCombatDamage ().getDefenceResistanceRolls ();
					break;
				
				case CHANCE_OF_DEATH:
					if (getFinalHits () == 0)
						languageText = getLanguages ().getCombatDamage ().getDefenceChanceOfDeathSurvives ();
					else
						languageText = getLanguages ().getCombatDamage ().getDefenceChanceOfDeathDies ();
					break;
	
				case DISINTEGRATE:
					if (getFinalHits () == 0)
						languageText = getLanguages ().getCombatDamage ().getDefenceDisintegrateSurvives ();
					else
						languageText = getLanguages ().getCombatDamage ().getDefenceDisintegrateDies ();
					break;
					
				case EACH_FIGURE_RESIST_OR_DIE:
					languageText = getLanguages ().getCombatDamage ().getDefenceEachFigureResistOrDie ();
					break;
	
				case SINGLE_FIGURE_RESIST_OR_DIE:
					if (getFinalHits () == 0)
						languageText = getLanguages ().getCombatDamage ().getDefenceSingleFigureResistOrDieSurvives ();
					else
						languageText = getLanguages ().getCombatDamage ().getDefenceSingleFigureResistOrDieDies ();
					break;
	
				case RESIST_OR_TAKE_DAMAGE:
					languageText = getLanguages ().getCombatDamage ().getDefenceResistOrTakeDamage ();
					break;
	
				case FEAR:
					languageText = getLanguages ().getCombatDamage ().getDefenceFear ();
					break;
					
				default:
					if (getModifiedDefenceStrength () == null)
						languageText = getLanguages ().getCombatDamage ().getDefenceStatisticsAutomatic ();		// hits strike automatically, i.e. doom damage
					else
						languageText = getLanguages ().getCombatDamage ().getDefenceStatistics ();
			}
		
		String text = "          " + getLanguageHolder ().findDescription (languageText).replaceAll
			("DEFENDER_NAME", getWizardClientUtils ().getPlayerName (getDefenderPlayer ())).replaceAll
			("DEFENDER_RACE_UNIT_NAME", getUnitClientUtils ().getUnitName (getDefenderUnit (), UnitNameType.RACE_UNIT_NAME)).replaceAll
			("DEFENDER_FIGURES", Integer.valueOf (getDefenderFigures ()).toString ()).replaceAll
			("FINAL_HITS", Integer.valueOf (getFinalHits ()).toString ());

		// All of these are only applicable if we actually make to hit/to defend rolls, so may be null
		if (getActualHits () != null)
			text = text.replaceAll ("ACTUAL_HITS", getActualHits ().toString ());

		if (getUnmodifiedDefenceStrength () != null)
			text = text.replaceAll ("UNMODIFIED_DEFENCE_STRENGTH", getUnmodifiedDefenceStrength ().toString ());
		
		if (getModifiedDefenceStrength () != null)
			text = text.replaceAll ("MODIFIED_DEFENCE_STRENGTH", getModifiedDefenceStrength ().toString ());
		
		if (getChanceToHit () != null)
			text = text.replaceAll ("CHANCE_TO_HIT", Integer.valueOf (getChanceToHit () * 10).toString ());
		
		if (getChanceToDefend () != null)
			text = text.replaceAll ("CHANCE_TO_DEFEND", Integer.valueOf (getChanceToDefend () * 10).toString ());
		
		if (getTenTimesAverageDamage () != null)
			text = text.replaceAll ("AVERAGE_DAMAGE", getTextUtils ().insertDecimalPoint (getTenTimesAverageDamage (), 1));
		
		if (getTenTimesAverageBlock () != null)
			text = text.replaceAll ("AVERAGE_BLOCK", getTextUtils ().insertDecimalPoint (getTenTimesAverageBlock (), 1));
		
		return text.replaceAll ("DEFENCE_STRENGTH", defenceStrength);
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
}