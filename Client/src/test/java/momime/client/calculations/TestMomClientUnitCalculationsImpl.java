package momime.client.calculations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.UnitSkill;
import momime.common.MomException;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.UnitUtils;

import org.junit.Test;

import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Tests the MomClientUnitCalculationsImpl class
 */
public final class TestMomClientUnitCalculationsImpl
{
	/**
	 * Tests the findPreferredMovementSkillGraphics method when we find a match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindPreferredMovementSkillGraphics_Found () throws Exception
	{
		// Mock entries from the graphics XML
		final List<UnitSkill> skills = new ArrayList<UnitSkill> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkill skill = new UnitSkill ();
			skill.setUnitSkillID ("US0" + n);
			
			if ((n >= 2) || (n <= 4))
				skill.setMovementIconImagePreference (5-n);  // So US02,3,4 have preference 3,2,1
			
			skills.add (skill);
		}
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.getUnitSkill ()).thenReturn (skills);
		
		// Mock things accessed from MomClient
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);

		// Unit to test
		final AvailableUnit unit = new AvailableUnit ();

		// Give the unit skills US02 and US03
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US01", players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (-1);
		when (unitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US02", players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (0);
		when (unitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US03", players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (0);
		when (unitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US04", players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (-1);
		when (unitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US05", players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (-1);
		
		// Set up object to test
		final MomClientUnitCalculationsImpl calc = new MomClientUnitCalculationsImpl ();
		calc.setGraphicsDB (gfx);
		calc.setUnitUtils (unitUtils);
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
		// Mock entries from the graphics XML
		final List<UnitSkill> skills = new ArrayList<UnitSkill> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkill skill = new UnitSkill ();
			skill.setUnitSkillID ("US0" + n);
			
			if ((n >= 2) || (n <= 4))
				skill.setMovementIconImagePreference (5-n);  // So US02,3,4 have preference 3,2,1
			
			skills.add (skill);
		}
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.getUnitSkill ()).thenReturn (skills);
		
		// Mock things accessed from MomClient
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);

		// Unit to test
		final AvailableUnit unit = new AvailableUnit ();

		// Give the unit skills US02 and US03
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US01", players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (-1);
		when (unitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US02", players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (-1);
		when (unitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US03", players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (-1);
		when (unitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US04", players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (-1);
		when (unitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US05", players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (-1);
		
		// Set up object to test
		final MomClientUnitCalculationsImpl calc = new MomClientUnitCalculationsImpl ();
		calc.setGraphicsDB (gfx);
		calc.setUnitUtils (unitUtils);
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
		// Mock entries from the graphics XML
		final List<UnitSkill> skills = new ArrayList<UnitSkill> ();
		final UnitSkill skill = new UnitSkill ();
		skill.setUnitSkillID ("US01");
		skill.setMovementIconImagePreference (1);
		skill.setStandActionID ("XXX");
		skill.setMoveActionID ("YYY");
		skills.add (skill);
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.getUnitSkill ()).thenReturn (skills);

		// Mock things accessed from MomClient
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);
		
		// Unit to test
		final AvailableUnit unit = new AvailableUnit ();

		// Give the unit skills US02 and US03
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US01", players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (0);
		
		// Set up object to test
		final MomClientUnitCalculationsImpl calc = new MomClientUnitCalculationsImpl ();
		calc.setGraphicsDB (gfx);
		calc.setUnitUtils (unitUtils);
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
		// Mock entries from the graphics XML
		final List<UnitSkill> skills = new ArrayList<UnitSkill> ();
		final UnitSkill skill = new UnitSkill ();
		skill.setUnitSkillID ("US01");
		skill.setMovementIconImagePreference (1);
		skills.add (skill);
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.getUnitSkill ()).thenReturn (skills);

		// Mock things accessed from MomClient
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);
		
		// Unit to test
		final AvailableUnit unit = new AvailableUnit ();

		// Give the unit skills US02 and US03
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US01", players, fow.getMaintainedSpell (), fow.getCombatAreaEffect (), db)).thenReturn (0);
		
		// Set up object to test
		final MomClientUnitCalculationsImpl calc = new MomClientUnitCalculationsImpl ();
		calc.setGraphicsDB (gfx);
		calc.setUnitUtils (unitUtils);
		calc.setClient (client);
		
		// Run test
		calc.determineCombatActionID (unit, false);
	}
}