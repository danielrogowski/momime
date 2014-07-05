package momime.editors.server.unit;

import java.awt.Component;
import java.util.Iterator;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import momime.editors.server.ServerEditorDatabaseConstants;

import org.jdom.Element;

import com.ndg.swing.UneditableArrayTableModel;
import com.ndg.xml.JdomUtils;
import com.ndg.xmleditor.single.EditXmlRecord;

/**
 * Adds a grid underneath the record details showing the calculated movement rates for this unit over every type of terrain
 */
public final class EditUnit extends EditXmlRecord
{
	/**
	 * Adds a grid underneath the record details showing the calculated movement rates for this unit over every type of terrain
	 */
	@Override
	protected final void initialize ()
	{
		if ((getRecordBeingEdited () != null) && (!isRecordBeingCopied ()))
		{
			// Now create the table
			final String [] columnHeadings = {"Tile Type ID", "Description", "2x Movement Rate", "Rate from which Skill"};

			final JTable table = new JTable (new UneditableArrayTableModel (calculateMovementRates (), columnHeadings));
			table.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);		// So it actually pays attention to the preferred widths
			table.getColumnModel ().getColumn (0).setPreferredWidth (80);
			table.getColumnModel ().getColumn (1).setPreferredWidth (200);
			table.getColumnModel ().getColumn (2).setPreferredWidth (110);
			table.getColumnModel ().getColumn (3).setPreferredWidth (150);

			// Put the grid into a scrolling area
			final JScrollPane scrollPane = new JScrollPane (table);
			scrollPane.setAlignmentY (Component.TOP_ALIGNMENT);
			getContentPane ().add (scrollPane, getUtils ().createConstraintsBothFill (0, 2, 1, 1, INSET));
		}
	}

	/**
	 * Leave space for the grid
	 * @return The height to lock the form at
	 */
	@Override
	public final int getFormHeight ()
	{
		int value = super.getFormHeight ();

		if ((getRecordBeingEdited () != null) && (!isRecordBeingCopied ()))
			value = value + 350;

		return value;
	}

	/**
	 * @return Array containing details of this unit's movement over each type of terrain
	 */
	@SuppressWarnings ("rawtypes")
	private final String [] [] calculateMovementRates ()
	{
		// Find how many tile types there are so we can size the array
		final List tileTypes = getMdiEditor ().getXmlDocuments ().get (0).getXml ().getChildren (ServerEditorDatabaseConstants.TAG_ENTITY_TILE_TYPE);
		final String [] [] movementRates = new String [tileTypes.size ()] [4];

		// Get the list of movement rules, since we need these every time
		final List movementRateRules = getMdiEditor ().getXmlDocuments ().get (0).getXml ().getChildren (ServerEditorDatabaseConstants.TAG_ENTITY_MOVEMENT_RATE_RULE);

		// Calculate for each tile type
		for (int tileTypeNo = 0; tileTypeNo < tileTypes.size (); tileTypeNo++)
		{
			final Element thisTileType = (Element) tileTypes.get (tileTypeNo);
			final String tileTypeId = thisTileType.getAttributeValue (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_TILE_TYPE_ID);
			final String tileTypeDescription = thisTileType.getChildText (ServerEditorDatabaseConstants.TAG_VALUE_TILE_TYPE_DESCRIPTION);

			// Search through all the rules
			String doubleMovementRate = null;
			String ruleUnitSkillDescription = null;

			final Iterator movementRateRulesIter = movementRateRules.iterator ();
			while ((doubleMovementRate == null) && (ruleUnitSkillDescription == null) && (movementRateRulesIter.hasNext ()))
			{
				final Element thisRule = (Element) movementRateRulesIter.next ();
				final String ruleUnitSkillId = thisRule.getChildText (ServerEditorDatabaseConstants.TAG_VALUE_MOVEMENT_RATE_RULE_UNIT_SKILL);

				// Does this unit actually have this skill?
				if (JdomUtils.findDomChildNodeWithTextAttribute (getRecordBeingEdited (), ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_UNIT_HAS_SKILL,
					ServerEditorDatabaseConstants.TAG_ATTRIBUTE_UNIT_HAS_SKILL_ID, ruleUnitSkillId) != null)
				{
					final String ruleTileTypeId = thisRule.getChildText (ServerEditorDatabaseConstants.TAG_VALUE_MOVEMENT_RATE_RULE_TILE_TYPE);

					// Does the tile type match?
					if ((ruleTileTypeId == null) || (ruleTileTypeId.equals (tileTypeId)))
					{
						// Found a match!
						doubleMovementRate = thisRule.getChildText (ServerEditorDatabaseConstants.TAG_VALUE_MOVEMENT_RATE_RULE_DOUBLE_MOVEMENT);

						final Element ruleUnitSkillNode = JdomUtils.findDomChildNodeWithTextAttribute (getMdiEditor ().getXmlDocuments ().get (0).getXml (),
							ServerEditorDatabaseConstants.TAG_ENTITY_UNIT_SKILL, ServerEditorDatabaseConstants.TAG_ATTRIBUTE_UNIT_SKILL_ID, ruleUnitSkillId);
						ruleUnitSkillDescription = ruleUnitSkillNode.getChildText (ServerEditorDatabaseConstants.TAG_VALUE_UNIT_SKILL_DESCRIPTION);
					}
				}
			}

			// If we got no matches at all, then this terrain is impassable to this unit
			if ((doubleMovementRate == null) && (ruleUnitSkillDescription == null))
			{
				doubleMovementRate = "Impassable";
				ruleUnitSkillDescription = "";
			}

			// Copy values into array
			movementRates [tileTypeNo] [0] = tileTypeId;
			movementRates [tileTypeNo] [1] = tileTypeDescription;
			movementRates [tileTypeNo] [2] = doubleMovementRate;
			movementRates [tileTypeNo] [3] = ruleUnitSkillDescription;
		}

		return movementRates;
	}
}
