package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import momime.common.database.CommonDatabaseConstants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the PlayerKnowledgeUtils class
 */
@ExtendWith(MockitoExtension.class)
public final class TestPlayerKnowledgeUtilsImpl
{
	/**
	 * Tests the hasWizardBeenChosen method on a player who hasn't yet chosen a wizard
	 */
	@Test
	public final void testHasWizardBeenChosen_NotChosen ()
	{
		final PlayerKnowledgeUtilsImpl utils = new PlayerKnowledgeUtilsImpl ();
		assertFalse (utils.hasWizardBeenChosen (null));
	}

	/**
	 * Tests the hasWizardBeenChosen method on a player who has chosen a standard wizard
	 */
	@Test
	public final void testHasWizardBeenChosen_Standard ()
	{
		final PlayerKnowledgeUtilsImpl utils = new PlayerKnowledgeUtilsImpl ();
		assertTrue (utils.hasWizardBeenChosen ("X"));
	}

	/**
	 * Tests the hasWizardBeenChosen method on a player who has chosen a custom wizard
	 */
	@Test
	public final void testHasWizardBeenChosen_Custom ()
	{
		final PlayerKnowledgeUtilsImpl utils = new PlayerKnowledgeUtilsImpl ();
		assertTrue (utils.hasWizardBeenChosen (""));
	}

	/**
	 * Tests the hasWizardBeenChosen method on the raiders player
	 */
	@Test
	public final void testHasWizardBeenChosen_Raiders ()
	{
		final PlayerKnowledgeUtilsImpl utils = new PlayerKnowledgeUtilsImpl ();
		assertTrue (utils.hasWizardBeenChosen (CommonDatabaseConstants.WIZARD_ID_RAIDERS));
	}

	/**
	 * Tests the hasWizardBeenChosen method on the monsters player
	 */
	@Test
	public final void testHasWizardBeenChosen_Monsters ()
	{
		final PlayerKnowledgeUtilsImpl utils = new PlayerKnowledgeUtilsImpl ();
		assertTrue (utils.hasWizardBeenChosen (CommonDatabaseConstants.WIZARD_ID_MONSTERS));
	}

	/**
	 * Tests the isWizard method on a player who hasn't yet chosen a wizard
	 */
	@Test
	public final void testIsWizard_NotChosen ()
	{
		final PlayerKnowledgeUtilsImpl utils = new PlayerKnowledgeUtilsImpl ();
		assertTrue (utils.isWizard (null));
	}

	/**
	 * Tests the isWizard method on a player who has chosen a standard wizard
	 */
	@Test
	public final void testIsWizard_Standard ()
	{
		final PlayerKnowledgeUtilsImpl utils = new PlayerKnowledgeUtilsImpl ();
		assertTrue (utils.isWizard ("X"));
	}

	/**
	 * Tests the isWizard method on a player who has chosen a custom wizard
	 */
	@Test
	public final void testIsWizard_Custom ()
	{
		final PlayerKnowledgeUtilsImpl utils = new PlayerKnowledgeUtilsImpl ();
		assertTrue (utils.isWizard (""));
	}

	/**
	 * Tests the isWizard method on the raiders player
	 */
	@Test
	public final void testIsWizard_Raiders ()
	{
		final PlayerKnowledgeUtilsImpl utils = new PlayerKnowledgeUtilsImpl ();
		assertFalse (utils.isWizard (CommonDatabaseConstants.WIZARD_ID_RAIDERS));
	}

	/**
	 * Tests the isWizard method on the monsters player
	 */
	@Test
	public final void testIsWizard_Monsters ()
	{
		final PlayerKnowledgeUtilsImpl utils = new PlayerKnowledgeUtilsImpl ();
		assertFalse (utils.isWizard (CommonDatabaseConstants.WIZARD_ID_MONSTERS));
	}

	/**
	 * Tests the isCustomWizard method on a player who hasn't yet chosen a wizard
	 */
	@Test
	public final void testIsCustomWizard_NotChosen ()
	{
		final PlayerKnowledgeUtilsImpl utils = new PlayerKnowledgeUtilsImpl ();
		assertFalse (utils.isCustomWizard (null));
	}

	/**
	 * Tests the isCustomWizard method on a player who has chosen a standard wizard
	 */
	@Test
	public final void testIsCustomWizard_Standard ()
	{
		final PlayerKnowledgeUtilsImpl utils = new PlayerKnowledgeUtilsImpl ();
		assertFalse (utils.isCustomWizard ("X"));
	}

	/**
	 * Tests the isCustomWizard method on a player who has chosen a custom wizard
	 */
	@Test
	public final void testIsCustomWizard_Custom ()
	{
		final PlayerKnowledgeUtilsImpl utils = new PlayerKnowledgeUtilsImpl ();
		assertTrue (utils.isCustomWizard (""));
	}

	/**
	 * Tests the isCustomWizard method on the raiders player
	 */
	@Test
	public final void testIsCustomWizard_Raiders ()
	{
		final PlayerKnowledgeUtilsImpl utils = new PlayerKnowledgeUtilsImpl ();
		assertFalse (utils.isCustomWizard (CommonDatabaseConstants.WIZARD_ID_RAIDERS));
	}

	/**
	 * Tests the isCustomWizard method on the monsters player
	 */
	@Test
	public final void testIsCustomWizard_Monsters ()
	{
		final PlayerKnowledgeUtilsImpl utils = new PlayerKnowledgeUtilsImpl ();
		assertFalse (utils.isCustomWizard (CommonDatabaseConstants.WIZARD_ID_MONSTERS));
	}
}
