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
import momime.common.MomException;
import momime.common.database.LanguageText;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.DamageCalculationConfusionData;
import momime.common.utils.UnitUtils;

/**
 * Lets players involved in a combat know what confusion effect a unit rolled this turn
 */
public final class DamageCalculationConfusionDataEx extends DamageCalculationConfusionData implements DamageCalculationText
{
	/** Confused unit */
	private MemoryUnit unit;
	
	/** Player who owns the confused unit */
	private PlayerPublicDetails owningPlayer;
	
	/** Player who owns the confusion spell */
	private PlayerPublicDetails castingPlayer;

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
	 * Perform some pre-processing action
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void preProcess () throws IOException
	{
		setUnit (getUnitUtils ().findUnitURN (getUnitURN (),
	    	getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "DamageCalculationConfusionDataEx (U)"));
	
	    setOwningPlayer (getMultiplayerSessionUtils ().findPlayerWithID
    		(getClient ().getPlayers (), getUnit ().getOwningPlayerID (), "DamageCalculationConfusionDataEx (CP)"));

	    setCastingPlayer (getMultiplayerSessionUtils ().findPlayerWithID
    		(getClient ().getPlayers (), getCastingPlayerID (), "DamageCalculationConfusionDataEx (CP)"));
	    
	    // Also actually record the effect
	    getUnit ().setConfusionEffect (getConfusionEffect ());
	}

	/**
	 * @return Text to display for this breakdown line
	 * @throws IOException If there is a problem
	 */
	@Override
	public final String getText () throws IOException
	{
		final List<LanguageText> languageText;
		switch (getConfusionEffect ())
		{
			case DO_NOTHING:
				languageText = getLanguages ().getCombatDamage ().getConfusionDoNothing ();
				break;
				
			case MOVE_RANDOMLY:
				languageText = getLanguages ().getCombatDamage ().getConfusionMoveRandomly ();
				break;
				
			case OWNER_CONTROLLED:
				languageText = getLanguages ().getCombatDamage ().getConfusionOwnerControlled ();
				break;
				
			case CASTER_CONTROLLED:
				languageText = getLanguages ().getCombatDamage ().getConfusionCasterControlled ();
				break;
				
			default:
				throw new MomException ("DamageCalculationConfusionDataEx encountered an unknown confusion effect: " + getConfusionEffect ());
		}
		
		return getLanguageHolder ().findDescription (languageText).replaceAll
			("PLAYER_NAME", getWizardClientUtils ().getPlayerName (getOwningPlayer ())).replaceAll
			("CASTER_NAME", getWizardClientUtils ().getPlayerName (getCastingPlayer ())).replaceAll
			("RACE_UNIT_NAME", getUnitClientUtils ().getUnitName (getUnit (), UnitNameType.RACE_UNIT_NAME));
	}

	/**
	 * @return Confused unit
	 */
	public final MemoryUnit getUnit ()
	{
		return unit;
	}

	/**
	 * @param u Confused unit
	 */
	public final void setUnit (final MemoryUnit u)
	{
		unit = u;
	}
	
	/**
	 * @return Player who owns the confused unit
	 */
	public final PlayerPublicDetails getOwningPlayer ()
	{
		return owningPlayer;
	}
	
	/**
	 * @param o Player who owns the confused unit
	 */
	public final void setOwningPlayer (final PlayerPublicDetails o)
	{
		owningPlayer = o;
	}
	
	/**
	 * @return Player who owns the confusion spell
	 */
	public final PlayerPublicDetails getCastingPlayer ()
	{
		return castingPlayer;
	}

	/**
	 * @param c Player who owns the confusion spell
	 */
	public final void setCastingPlayer (final PlayerPublicDetails c)
	{
		castingPlayer = c;
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