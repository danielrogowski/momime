package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.language.replacer.DamageCalculationBreakdown;
import momime.client.ui.frames.DamageCalculationsUI;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.servertoclient.DamageCalculationMessage;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Server telling the two players involved in a combat how damage was calculated.
 * Either attackSkillID or attackAttributeID will be filled in, but not both:
 *		attackSkillID will be filled in if the attack is is a special skill like First Strike, Fire Breath etc.
 *		attackAttributeID will be filled in (UA01 or UA02) if the attack is a standard melee or ranged attack.
 */
public final class DamageCalculationMessageImpl extends DamageCalculationMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (DamageCalculationMessageImpl.class);

	/** UI for displaying damage calculations */
	private DamageCalculationsUI damageCalculationsUI;
	
	/** Unit utils */
	private UnitUtils unitUtils;

	/** Multiplayer client */
	private MomClient client;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start");
		
		switch (getMessageType ())
		{
			case ATTACK_AND_DEFENCE_STATISTICS:
			{
				final DamageCalculationBreakdown breakdown1 = createBreakdown ();
				breakdown1.setLanguageEntryID ("AttackStatistics");
				breakdown1.setIndent ("    ");
				getDamageCalculationsUI ().addBreakdown (breakdown1);

				// Don't even bother showing blocked hits if we didn't score any hits
				if (getActualDamage () > 0)
				{
					final DamageCalculationBreakdown breakdown2 = createBreakdown ();
					breakdown2.setLanguageEntryID ("DefenceStatistics");
					breakdown2.setIndent ("    ");
					getDamageCalculationsUI ().addBreakdown (breakdown2);
				}
				break;
			}
				
			case MELEE_ATTACK:
			{
				final DamageCalculationBreakdown breakdown = createBreakdown ();
				breakdown.setLanguageEntryID ("MeleeAttack");
				getDamageCalculationsUI ().addBreakdown (breakdown);
				break;
			}
				
			case RANGED_ATTACK:
			{
				final DamageCalculationBreakdown breakdown = createBreakdown ();
				breakdown.setLanguageEntryID ("RangedAttack");
				getDamageCalculationsUI ().addBreakdown (breakdown);
				break;
			}
		}
		
		log.trace ("Exiting start");
	}
	
	/**
	 * @return New breakdown object, with all its values copied from this message
	 * @throws RecordNotFoundException If one of the unitURNs can't be found
	 * @throws PlayerNotFoundException If one of the players who owns one of the units can't be found
	 */
	private final DamageCalculationBreakdown createBreakdown () throws RecordNotFoundException, PlayerNotFoundException
	{
		final DamageCalculationBreakdown breakdown = new DamageCalculationBreakdown ();
		
	    breakdown.setMessageType					(getMessageType ());
	    breakdown.setAttackerUnitURN				(getAttackerUnitURN ());
	    breakdown.setDefenderUnitURN				(getDefenderUnitURN ());
	    breakdown.setAttackSkillID						(getAttackSkillID ());
	    breakdown.setAttackAttributeID				(getAttackAttributeID ());
	    breakdown.setAttackerFigures					(getAttackerFigures ());
	    breakdown.setDefenderFigures				(getDefenderFigures ());
	    breakdown.setAttackStrength					(getAttackStrength ());
	    breakdown.setDefenceStrength				(getDefenceStrength ());
	    breakdown.setPotentialDamage				(getPotentialDamage ());
	    breakdown.setChanceToHit						(getChanceToHit ());
	    breakdown.setChanceToDefend				(getChanceToDefend ());
	    breakdown.setTenTimesAverageDamage	(getTenTimesAverageDamage ());
	    breakdown.setTenTimesAverageBlock		(getTenTimesAverageBlock ());
	    breakdown.setActualDamage					(getActualDamage ());
	    breakdown.getActualBlockedHits ().addAll	(getActualBlockedHits ());
	    
	    // These damage calculations can live in the history for a long time, easily after the unit(s) in question have been destroyed
	    // and potentially even after the player(s) who own the unit(s) have been kicked out of the game.
	    // So find and record them up front, so we can't fail to find the unitURNs or playerIDs later.
	    breakdown.setAttackerUnit (getUnitUtils ().findUnitURN (getAttackerUnitURN (),
	    	getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "DamageCalculationMessageImpl-a"));

	    breakdown.setDefenderUnit (getUnitUtils ().findUnitURN (getDefenderUnitURN (),
	    	getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "DamageCalculationMessageImpl-d"));
	    
	    breakdown.setAttackingPlayer (getMultiplayerSessionUtils ().findPlayerWithID
	    	(getClient ().getPlayers (), breakdown.getAttackerUnit ().getOwningPlayerID (), "DamageCalculationMessageImpl-a"));

	    breakdown.setDefenderPlayer (getMultiplayerSessionUtils ().findPlayerWithID
	    	(getClient ().getPlayers (), breakdown.getDefenderUnit ().getOwningPlayerID (), "DamageCalculationMessageImpl-d"));
	    
		return breakdown;
	}

	/**
	 * @return UI for displaying damage calculations
	 */
	public final DamageCalculationsUI getDamageCalculationsUI ()
	{
		return damageCalculationsUI;
	}

	/**
	 * @param ui UI for displaying damage calculations
	 */
	public final void setDamageCalculationsUI (final DamageCalculationsUI ui)
	{
		damageCalculationsUI = ui;
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
}