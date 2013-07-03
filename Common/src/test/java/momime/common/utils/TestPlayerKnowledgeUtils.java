package momime.common.messages;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import momime.common.database.CommonDatabaseConstants;

import org.junit.Test;

/**
 * Tests the PlayerKnowledgeUtils class
 */
public final class TestPlayerKnowledgeUtils
{
	/**
	 * Tests the hasWizardBeenChosen method on a player who hasn't yet chosen a wizard
	 */
	@Test
	public final void testHasWizardBeenChosen_NotChosen ()
	{
		assertFalse (PlayerKnowledgeUtils.hasWizardBeenChosen (null));
	}

	/**
	 * Tests the hasWizardBeenChosen method on a player who has chosen a standard wizard
	 */
	@Test
	public final void testHasWizardBeenChosen_Standard ()
	{
		assertTrue (PlayerKnowledgeUtils.hasWizardBeenChosen ("X"));
	}

	/**
	 * Tests the hasWizardBeenChosen method on a player who has chosen a custom wizard
	 */
	@Test
	public final void testHasWizardBeenChosen_Custom ()
	{
		assertTrue (PlayerKnowledgeUtils.hasWizardBeenChosen (""));
	}

	/**
	 * Tests the hasWizardBeenChosen method on the raiders player
	 */
	@Test
	public final void testHasWizardBeenChosen_Raiders ()
	{
		assertTrue (PlayerKnowledgeUtils.hasWizardBeenChosen (CommonDatabaseConstants.WIZARD_ID_RAIDERS));
	}

	/**
	 * Tests the hasWizardBeenChosen method on the monsters player
	 */
	@Test
	public final void testHasWizardBeenChosen_Monsters ()
	{
		assertTrue (PlayerKnowledgeUtils.hasWizardBeenChosen (CommonDatabaseConstants.WIZARD_ID_MONSTERS));
	}

	/**
	 * Tests the isWizard method on a player who hasn't yet chosen a wizard
	 */
	@Test
	public final void testIsWizard_NotChosen ()
	{
		assertTrue (PlayerKnowledgeUtils.isWizard (null));
	}

	/**
	 * Tests the isWizard method on a player who has chosen a standard wizard
	 */
	@Test
	public final void testIsWizard_Standard ()
	{
		assertTrue (PlayerKnowledgeUtils.isWizard ("X"));
	}

	/**
	 * Tests the isWizard method on a player who has chosen a custom wizard
	 */
	@Test
	public final void testIsWizard_Custom ()
	{
		assertTrue (PlayerKnowledgeUtils.isWizard (""));
	}

	/**
	 * Tests the isWizard method on the raiders player
	 */
	@Test
	public final void testIsWizard_Raiders ()
	{
		assertFalse (PlayerKnowledgeUtils.isWizard (CommonDatabaseConstants.WIZARD_ID_RAIDERS));
	}

	/**
	 * Tests the isWizard method on the monsters player
	 */
	@Test
	public final void testIsWizard_Monsters ()
	{
		assertFalse (PlayerKnowledgeUtils.isWizard (CommonDatabaseConstants.WIZARD_ID_MONSTERS));
	}

	/**
	 * Tests the isCustomWizard method on a player who hasn't yet chosen a wizard
	 */
	@Test
	public final void testIsCustomWizard_NotChosen ()
	{
		assertFalse (PlayerKnowledgeUtils.isCustomWizard (null));
	}

	/**
	 * Tests the isCustomWizard method on a player who has chosen a standard wizard
	 */
	@Test
	public final void testIsCustomWizard_Standard ()
	{
		assertFalse (PlayerKnowledgeUtils.isCustomWizard ("X"));
	}

	/**
	 * Tests the isCustomWizard method on a player who has chosen a custom wizard
	 */
	@Test
	public final void testIsCustomWizard_Custom ()
	{
		assertTrue (PlayerKnowledgeUtils.isCustomWizard (""));
	}

	/**
	 * Tests the isCustomWizard method on the raiders player
	 */
	@Test
	public final void testIsCustomWizard_Raiders ()
	{
		assertFalse (PlayerKnowledgeUtils.isCustomWizard (CommonDatabaseConstants.WIZARD_ID_RAIDERS));
	}

	/**
	 * Tests the isCustomWizard method on the monsters player
	 */
	@Test
	public final void testIsCustomWizard_Monsters ()
	{
		assertFalse (PlayerKnowledgeUtils.isCustomWizard (CommonDatabaseConstants.WIZARD_ID_MONSTERS));
	}
}
