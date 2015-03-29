package momime.client.calculations.damage;

import java.io.IOException;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.utils.TextUtils;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.client.utils.WizardClientUtils;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.DamageCalculationDefenceData;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Breakdown about how damage from a number of potential hits was applied to a unit
 */
public final class DamageCalculationDefenceDataEx extends DamageCalculationDefenceData implements DamageCalculationText
{
	/** Class logger */
	private final Log log = LogFactory.getLog (DamageCalculationDefenceDataEx.class);
	
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
		log.trace ("Entering preProcess");
		
	    setDefenderUnit (getUnitUtils ().findUnitURN (getDefenderUnitURN (),
	    	getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "DamageCalculationDefenceDataEx-du"));
		    
	    setDefenderPlayer (getMultiplayerSessionUtils ().findPlayerWithID
	    	(getClient ().getPlayers (), getDefenderUnit ().getOwningPlayerID (), "DamageCalculationDefenceDataEx-dp"));

		log.trace ("Exiting preProcess");
	}
	
	/**
	 * @return Text to display for this breakdown line
	 * @throws IOException If there is a problem
	 */
	@Override
	public final String getText () throws IOException
	{
		return "          " + getLanguage ().findCategoryEntry ("CombatDamage", "DefenceStatistics").replaceAll
			("DEFENDER_NAME", getWizardClientUtils ().getPlayerName (getDefenderPlayer ())).replaceAll
			("DEFENDER_RACE_UNIT_NAME", getUnitClientUtils ().getUnitName (getDefenderUnit (), UnitNameType.RACE_UNIT_NAME)).replaceAll
			("DEFENDER_FIGURES", new Integer (getDefenderFigures ()).toString ()).replaceAll
			("DEFENCE_STRENGTH", getDefenceStrength ().toString ()).replaceAll
			("CHANCE_TO_HIT", new Integer (getChanceToHit () * 10).toString ()).replaceAll
			("CHANCE_TO_DEFEND", new Integer (getChanceToDefend () * 10).toString ()).replaceAll
			("AVERAGE_DAMAGE", getTextUtils ().insertDecimalPoint (getTenTimesAverageDamage (), 1)).replaceAll
			("ACTUAL_HITS", getActualHits ().toString ()).replaceAll
			("AVERAGE_BLOCK", getTextUtils ().insertDecimalPoint (getTenTimesAverageBlock (), 1)).replaceAll
			("FINAL_HITS", new Integer (getFinalHits ()).toString ());
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