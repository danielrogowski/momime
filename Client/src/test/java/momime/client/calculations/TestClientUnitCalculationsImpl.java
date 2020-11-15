package momime.client.calculations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.UnitSkillEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Tests the ClientUnitCalculationsImpl class
 */
public final class TestClientUnitCalculationsImpl
{
	/**
	 * Tests the findPreferredMovementSkillGraphics method when we find a match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindPreferredMovementSkillGraphics_Found () throws Exception
	{
		// Mock database
		final List<UnitSkillEx> skills = new ArrayList<UnitSkillEx> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkillEx skill = new UnitSkillEx ();
			skill.setUnitSkillID ("US0" + n);
			
			if ((n >= 2) || (n <= 4))
				skill.setMovementIconImagePreference (5-n);  // So US02,3,4 have preference 3,2,1
			
			skills.add (skill);
		}
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitSkills ()).thenReturn (skills);
		
		// Mock things accessed from MomClient
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);

		// Unit to test
		final ExpandedUnitDetails unit = mock (ExpandedUnitDetails.class);

		// Give the unit skills US02 and US03
		when (unit.hasModifiedSkill ("US01")).thenReturn (false);
		when (unit.hasModifiedSkill ("US02")).thenReturn (true);
		when (unit.hasModifiedSkill ("US03")).thenReturn (true);
		when (unit.hasModifiedSkill ("US04")).thenReturn (false);
		when (unit.hasModifiedSkill ("US05")).thenReturn (false);
		
		// Set up object to test
		final ClientUnitCalculationsImpl calc = new ClientUnitCalculationsImpl ();
		calc.setClient (client);
		
		// Run test
		assertEquals ("US03", calc.findPreferredMovementSkillGraphics (unit).getUnitSkillID ());
	}

	/**
	 * Tests the findPreferredMovementSkillGraphics method when we don't find a match
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testFindPreferredMovementSkillGraphics_NotFound () throws Exception
	{
		// Mock database
		final List<UnitSkillEx> skills = new ArrayList<UnitSkillEx> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkillEx skill = new UnitSkillEx ();
			skill.setUnitSkillID ("US0" + n);
			
			if ((n >= 2) || (n <= 4))
				skill.setMovementIconImagePreference (5-n);  // So US02,3,4 have preference 3,2,1
			
			skills.add (skill);
		}
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitSkills ()).thenReturn (skills);
		
		// Mock things accessed from MomClient
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);

		// Unit to test
		final ExpandedUnitDetails unit = mock (ExpandedUnitDetails.class);

		// Give the unit skills US02 and US03
		when (unit.hasModifiedSkill ("US01")).thenReturn (false);
		when (unit.hasModifiedSkill ("US02")).thenReturn (false);
		when (unit.hasModifiedSkill ("US03")).thenReturn (false);
		when (unit.hasModifiedSkill ("US04")).thenReturn (false);
		when (unit.hasModifiedSkill ("US05")).thenReturn (false);
		
		// Set up object to test
		final ClientUnitCalculationsImpl calc = new ClientUnitCalculationsImpl ();
		calc.setClient (client);
		
		// Run test
		calc.findPreferredMovementSkillGraphics (unit);
	}
	
	/**
	 * Tests the determineCombatActionID method when a combatActionID is defined 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineCombatActionID_Defined () throws Exception
	{
		// Mock database
		final List<UnitSkillEx> skills = new ArrayList<UnitSkillEx> ();
		final UnitSkillEx skill = new UnitSkillEx ();
		skill.setUnitSkillID ("US01");
		skill.setMovementIconImagePreference (1);
		skill.setStandActionID ("XXX");
		skill.setMoveActionID ("YYY");
		skills.add (skill);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitSkills ()).thenReturn (skills);

		// Mock things accessed from MomClient
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);
		
		// Unit to test
		final ExpandedUnitDetails unit = mock (ExpandedUnitDetails.class);

		// Give the unit skills US01 only
		when (unit.hasModifiedSkill ("US01")).thenReturn (true);
		
		// Set up object to test
		final ClientUnitCalculationsImpl calc = new ClientUnitCalculationsImpl ();
		calc.setClient (client);
		
		// Run test
		assertEquals ("XXX", calc.determineCombatActionID (unit, false));
		assertEquals ("YYY", calc.determineCombatActionID (unit, true));
	}
	
	/**
	 * Tests the determineCombatActionID method when a combatActionID isn't defined 
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testDetermineCombatActionID_Undefined () throws Exception
	{
		// Mock database
		final List<UnitSkillEx> skills = new ArrayList<UnitSkillEx> ();
		final UnitSkillEx skill = new UnitSkillEx ();
		skill.setUnitSkillID ("US01");
		skill.setMovementIconImagePreference (1);
		skills.add (skill);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitSkills ()).thenReturn (skills);

		// Mock things accessed from MomClient
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();		
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);
		
		// Unit to test
		final ExpandedUnitDetails unit = mock (ExpandedUnitDetails.class);

		// Give the unit skills US01 only
		when (unit.hasModifiedSkill ("US01")).thenReturn (true);
		
		// Set up object to test
		final ClientUnitCalculationsImpl calc = new ClientUnitCalculationsImpl ();
		calc.setClient (client);
		
		// Run test
		calc.determineCombatActionID (unit, false);
	}
}