package momime.server.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.CommonDatabase;
import momime.common.database.Spell;
import momime.common.database.SwitchResearch;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.utils.SpellUtils;

/**
 * Tests the SpellServerUtilsImpl class
 */
public final class TestSpellServerUtilsImpl
{
	/**
	 * Tests the validateResearch method when we're researching nothing, and make a valid choice
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateResearch () throws Exception
	{
		// Spell details
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		// Research status
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);

		// Set up object to test
		final SpellServerUtilsImpl utils = new SpellServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNull (utils.validateResearch (player, "SP001", SwitchResearch.DISALLOWED, db));
	}

	/**
	 * Tests the validateResearch method when we try to research something that isn't available to us yet
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateResearch_NotAvailable () throws Exception
	{
		// Spell details
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		// Research status
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE);		// <---
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);

		// Set up object to test
		final SpellServerUtilsImpl utils = new SpellServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNotNull (utils.validateResearch (player, "SP001", SwitchResearch.DISALLOWED, db));
	}
	
	/**
	 * Tests the validateResearch method switching to the spell we're already researching 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateResearch_ResearchingSame () throws Exception
	{
		// Spell details
		final Spell spell = new Spell ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP001", "validateResearch")).thenReturn (spell);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setSpellIDBeingResearched ("SP001");
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		// Research status
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);

		// Set up object to test
		final SpellServerUtilsImpl utils = new SpellServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNull (utils.validateResearch (player, "SP001", SwitchResearch.DISALLOWED, db));
	}

	/**
	 * Tests the validateResearch method trying to swap research when it isn't allowed 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateResearch_SwitchDisallowed () throws Exception
	{
		// Spell details
		final Spell oldSpell = new Spell ();
		oldSpell.setResearchCost (80);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP002", "validateResearch")).thenReturn (oldSpell);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setSpellIDBeingResearched ("SP002");
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		// Research status
		final SpellResearchStatus oldResearchStatus = new SpellResearchStatus ();
		oldResearchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		oldResearchStatus.setRemainingResearchCost (40);

		final SpellResearchStatus newResearchStatus = new SpellResearchStatus ();
		newResearchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (newResearchStatus);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP002")).thenReturn (oldResearchStatus);

		// Set up object to test
		final SpellServerUtilsImpl utils = new SpellServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNotNull (utils.validateResearch (player, "SP001", SwitchResearch.DISALLOWED, db));
	}
	
	/**
	 * Tests the validateResearch method trying to swap research when it its fine 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateResearch_SwitchFreely () throws Exception
	{
		// Spell details
		final Spell oldSpell = new Spell ();
		oldSpell.setResearchCost (80);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP002", "validateResearch")).thenReturn (oldSpell);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setSpellIDBeingResearched ("SP002");
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		// Research status
		final SpellResearchStatus oldResearchStatus = new SpellResearchStatus ();
		oldResearchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		oldResearchStatus.setRemainingResearchCost (40);

		final SpellResearchStatus newResearchStatus = new SpellResearchStatus ();
		newResearchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (newResearchStatus);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP002")).thenReturn (oldResearchStatus);

		// Set up object to test
		final SpellServerUtilsImpl utils = new SpellServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNull (utils.validateResearch (player, "SP001", SwitchResearch.FREE, db));
	}
	
	/**
	 * Tests the validateResearch method trying to swap research when it its fine but we'll lose previous research (that's dealt with elsewhere, we just validate that its OK)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateResearch_SwitchButLose () throws Exception
	{
		// Spell details
		final Spell oldSpell = new Spell ();
		oldSpell.setResearchCost (80);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP002", "validateResearch")).thenReturn (oldSpell);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setSpellIDBeingResearched ("SP002");
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		// Research status
		final SpellResearchStatus oldResearchStatus = new SpellResearchStatus ();
		oldResearchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		oldResearchStatus.setRemainingResearchCost (40);

		final SpellResearchStatus newResearchStatus = new SpellResearchStatus ();
		newResearchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (newResearchStatus);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP002")).thenReturn (oldResearchStatus);

		// Set up object to test
		final SpellServerUtilsImpl utils = new SpellServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNull (utils.validateResearch (player, "SP001", SwitchResearch.LOSE_CURRENT_RESEARCH, db));
	}
	
	/**
	 * Tests the validateResearch method trying to swap research when its set to "only if not started" but we had started 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateResearch_SwitchAfterStarted () throws Exception
	{
		// Spell details
		final Spell oldSpell = new Spell ();
		oldSpell.setResearchCost (80);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP002", "validateResearch")).thenReturn (oldSpell);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setSpellIDBeingResearched ("SP002");
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		// Research status
		final SpellResearchStatus oldResearchStatus = new SpellResearchStatus ();
		oldResearchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		oldResearchStatus.setRemainingResearchCost (40);

		final SpellResearchStatus newResearchStatus = new SpellResearchStatus ();
		newResearchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (newResearchStatus);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP002")).thenReturn (oldResearchStatus);

		// Set up object to test
		final SpellServerUtilsImpl utils = new SpellServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNotNull (utils.validateResearch (player, "SP001", SwitchResearch.ONLY_IF_NOT_STARTED, db));
	}

	/**
	 * Tests the validateResearch method trying to swap research when its set to "only if not started" but we hadn't started 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateResearch_SwitchBeforeStarted () throws Exception
	{
		// Spell details
		final Spell oldSpell = new Spell ();
		oldSpell.setResearchCost (80);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findSpell ("SP002", "validateResearch")).thenReturn (oldSpell);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setSpellIDBeingResearched ("SP002");
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		// Research status
		final SpellResearchStatus oldResearchStatus = new SpellResearchStatus ();
		oldResearchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		oldResearchStatus.setRemainingResearchCost (80);		// <---

		final SpellResearchStatus newResearchStatus = new SpellResearchStatus ();
		newResearchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (newResearchStatus);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP002")).thenReturn (oldResearchStatus);

		// Set up object to test
		final SpellServerUtilsImpl utils = new SpellServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNull (utils.validateResearch (player, "SP001", SwitchResearch.ONLY_IF_NOT_STARTED, db));
	}
}