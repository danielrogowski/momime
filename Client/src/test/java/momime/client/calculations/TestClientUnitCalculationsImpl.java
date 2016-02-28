package momime.client.calculations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.UnitSkillGfx;
import momime.common.MomException;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.UnitSkillUtils;

import org.junit.Test;

import com.ndg.multiplayer.session.PlayerPublicDetails;

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
		// Mock entries from the graphics XML
		final List<UnitSkillGfx> skills = new ArrayList<UnitSkillGfx> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkillGfx skill = new UnitSkillGfx ();
			skill.setUnitSkillID ("US0" + n);
			
			if ((n >= 2) || (n <= 4))
				skill.setMovementIconImagePreference (5-n);  // So US02,3,4 have preference 3,2,1
			
			skills.add (skill);
		}
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.getUnitSkills ()).thenReturn (skills);
		
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
		final UnitSkillUtils unitSkillUtils = mock (UnitSkillUtils.class);
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US01", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (-1);
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US02", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (0);
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US03", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (0);
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US04", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (-1);
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US05", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (-1);
		
		// Set up object to test
		final ClientUnitCalculationsImpl calc = new ClientUnitCalculationsImpl ();
		calc.setGraphicsDB (gfx);
		calc.setUnitSkillUtils (unitSkillUtils);
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
		final List<UnitSkillGfx> skills = new ArrayList<UnitSkillGfx> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkillGfx skill = new UnitSkillGfx ();
			skill.setUnitSkillID ("US0" + n);
			
			if ((n >= 2) || (n <= 4))
				skill.setMovementIconImagePreference (5-n);  // So US02,3,4 have preference 3,2,1
			
			skills.add (skill);
		}
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.getUnitSkills ()).thenReturn (skills);
		
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
		final UnitSkillUtils unitSkillUtils = mock (UnitSkillUtils.class);
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US01", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (-1);
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US02", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (-1);
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US03", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (-1);
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US04", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (-1);
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US05", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (-1);
		
		// Set up object to test
		final ClientUnitCalculationsImpl calc = new ClientUnitCalculationsImpl ();
		calc.setGraphicsDB (gfx);
		calc.setUnitSkillUtils (unitSkillUtils);
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
		final List<UnitSkillGfx> skills = new ArrayList<UnitSkillGfx> ();
		final UnitSkillGfx skill = new UnitSkillGfx ();
		skill.setUnitSkillID ("US01");
		skill.setMovementIconImagePreference (1);
		skill.setStandActionID ("XXX");
		skill.setMoveActionID ("YYY");
		skills.add (skill);
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.getUnitSkills ()).thenReturn (skills);

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
		final UnitSkillUtils unitSkillUtils = mock (UnitSkillUtils.class);
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US01", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (0);
		
		// Set up object to test
		final ClientUnitCalculationsImpl calc = new ClientUnitCalculationsImpl ();
		calc.setGraphicsDB (gfx);
		calc.setUnitSkillUtils (unitSkillUtils);
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
		final List<UnitSkillGfx> skills = new ArrayList<UnitSkillGfx> ();
		final UnitSkillGfx skill = new UnitSkillGfx ();
		skill.setUnitSkillID ("US01");
		skill.setMovementIconImagePreference (1);
		skills.add (skill);
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.getUnitSkills ()).thenReturn (skills);

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
		final UnitSkillUtils unitSkillUtils = mock (UnitSkillUtils.class);
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US01", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (0);
		
		// Set up object to test
		final ClientUnitCalculationsImpl calc = new ClientUnitCalculationsImpl ();
		calc.setGraphicsDB (gfx);
		calc.setUnitSkillUtils (unitSkillUtils);
		calc.setClient (client);
		
		// Run test
		calc.determineCombatActionID (unit, false);
	}
}