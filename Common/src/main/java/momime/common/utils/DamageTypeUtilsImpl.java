package momime.common.utils;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.DamageType;
import momime.common.database.DamageTypeImmunity;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;

/**
 * Helper methods for dealing with damage types
 */
public final class DamageTypeUtilsImpl implements DamageTypeUtils
{
	/** Class logger */
	private final Log log = LogFactory.getLog (DamageTypeUtilsImpl.class);
	
	/** Unit skill utils */
	private UnitSkillUtils unitSkillUtils;
	
	/**
	 * @param defender Unit being hit
	 * @param damageType Type of damage they are being hit by
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Whether or not the unit is completely immune to this type of damage - so getting a boost to e.g. 50 shields still returns false
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit; or a bonus applies that we cannot determine the amount of
	 */
	@Override
	public final boolean isUnitImmuneToDamageType (final MemoryUnit defender, final DamageType damageType,
		final String attackFromSkillID, final String attackFromMagicRealmID,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
    	log.trace ("Entering isUnitImmuneToDamageType: Unit URN " + defender.getUnitURN () + ", " + damageType.getDamageTypeID ());
		
    	boolean immunity = false;
		final Iterator<DamageTypeImmunity> iter = damageType.getDamageTypeImmunity ().iterator ();
		while ((!immunity) && (iter.hasNext ()))
		{
			final DamageTypeImmunity imm = iter.next ();
			
			// We only want complete immunities - even if it boots defence to 50, its still a valid target
			if ((imm.getBoostsDefenceTo () == null) && (getUnitSkillUtils ().getModifiedSkillValue (defender, defender.getUnitHasSkill (), imm.getUnitSkillID (),
				null, // null seems ok here for enemyUnits, I can't imagine an enemy unit can have a skill that can cancel our immunity to something
   				UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, attackFromSkillID, attackFromMagicRealmID, players, mem, db) >= 0))
					immunity = true;
		}
		
    	log.trace ("Exiting isUnitImmuneToDamageType = " + immunity);
		return immunity;
	}

	/**
	 * @return Unit skill utils
	 */
	public final UnitSkillUtils getUnitSkillUtils ()
	{
		return unitSkillUtils;
	}

	/**
	 * @param utils Unit skill utils
	 */
	public final void setUnitSkillUtils (final UnitSkillUtils utils)
	{
		unitSkillUtils = utils;
	}
}