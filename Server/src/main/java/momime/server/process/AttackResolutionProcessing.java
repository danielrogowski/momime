package momime.server.process;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.AttackResolution;
import momime.common.database.AttackResolutionStep;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.utils.ExpandedUnitDetails;
import momime.server.MomSessionVariables;

/**
 * Methods for processing attack resolutions.  This would all just be part of DamageProcessor, these methods
 * are just moved out so they can be mocked separately in unit tests.
 */
public interface AttackResolutionProcessing
{
	/**
	 * When one unit initiates a basic attack in combat against another, determines the most appropriate attack resolution rules to deal with processing the attack.
	 * 
	 * @param attacker Unit making the attack (may be owned by the player that is defending in combat)
	 * 	Note the attacker stats must be calculated listing the defender as the opponent, in order for Negate First Strike to work correctly 
	 * @param defender Unit being attacked (may be owned by the player that is attacking in combat)
	 * @param attackSkillID Which skill they are attacking with (melee or ranged)
	 * @param db Lookup lists built over the XML database
	 * @return Chosen attack resolution
	 * @throws RecordNotFoundException If the unit skill or so on can't be found in the XML database
	 * @throws MomException If no attack resolutions are appropriate, or if there are errors checking unit skills
	 */
	public AttackResolution chooseAttackResolution (final ExpandedUnitDetails attacker, final ExpandedUnitDetails defender, final String attackSkillID,
		final CommonDatabase db) throws RecordNotFoundException, MomException;
	
	/**
	 * @param steps Steps in one continuous list
	 * @return Same list as input, but segmented into sublists where all steps share the same step number; also wraps each step in the container class
	 * @throws MomException If the steps in the input list aren't in stepNumber order
	 */
	public List<List<AttackResolutionStepContainer>> splitAttackResolutionStepsByStepNumber (final List<AttackResolutionStep> steps)
		throws MomException;
	
	/**
	 * Executes all of the steps of an attack sequence that have the same step number, i.e. all those steps such that damage is calculated and applied simultaneously.
	 * 
	 * @param attacker Unit making the attack; or null if the attack isn't coming from a unit 
	 * @param defender Unit being attacked
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param combatLocation Location the combat is taking place; null if its damage from an overland spell
	 * @param steps The steps to take, i.e. all of the steps defined under the chosen attackResolution that have the same stepNumber
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	public void processAttackResolutionStep (final AttackResolutionUnit attacker, final AttackResolutionUnit defender,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final MapCoordinates3DEx combatLocation,
		final List<AttackResolutionStepContainer> steps, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
}