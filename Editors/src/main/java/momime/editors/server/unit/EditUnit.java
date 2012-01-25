package momime.editors.server.unit;

import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import momime.editors.server.ServerEditorDatabaseConstants;

import org.jdom.Element;

import com.ndg.swing.SwingLayoutConstants;
import com.ndg.swing.UneditableArrayTableModel;
import com.ndg.xml.JdomUtils;
import com.ndg.xmleditor.editor.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.editor.XmlEditorMain;
import com.ndg.xmleditor.single.EditXmlRecord;

/**
 * Adds a grid underneath the record details showing the calculated movement rates for this unit over every type of terrain
 */
public class EditUnit extends EditXmlRecord
{
	/**
	 * Does nothing other than call the super constructor
	 * @param entityElement The xsd:element node of the entity being edited from the XSD, i.e. for a top level entity, this will be a the entry under the xsd:sequence under the database complex type
	 * @param aTypeDefinition The xsd:complexType node of the entity being edited from the XSD
	 * @param aMdiEditor The main MDI window
	 * @param aRecordBeingEdited The record being edited, or null if we're adding a new record
	 * @param recordBeingCopied True if we're copying the specified record rather than editing it
	 * @param aParentRecord The parent record which contains this child record, or null if this is a top level record (it most cases you can get this from recordBeingEdited.getParent (), except when creating a new record)
	 * @param parentEntityElements Array of xsd:element entries that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @param aParentTypeDefinitions Array of type definitions that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 * @throws IOException If there is a problem loading the button images
	 */
	public EditUnit (final Element entityElement, final ComplexTypeReference aTypeDefinition, final XmlEditorMain aMdiEditor,
		final Element aRecordBeingEdited, final boolean recordBeingCopied, final Element aParentRecord, final Element [] parentEntityElements, final ComplexTypeReference [] aParentTypeDefinitions)
		throws XmlEditorException, IOException
	{
		super (entityElement, aTypeDefinition, aMdiEditor, aRecordBeingEdited, recordBeingCopied, aParentRecord, parentEntityElements, aParentTypeDefinitions);
	}

	/**
	 * Adds a grid underneath the record details showing the calculated movement rates for this unit over every type of terrain
	 */
	@Override
	protected void initialize ()
	{
		if ((getRecordBeingEdited () != null) && (!isRecordBeingCopied ()))
		{
			// Leave a gap between the unit details and the movement grid
			getLeftPanel ().add (Box.createRigidArea (new Dimension (0, SwingLayoutConstants.SPACE_BETWEEN_CONTROLS)));

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
			getLeftPanel ().add (scrollPane);
		}
	}

	/**
	 * Leave space for the grid
	 * @return The height to lock the form at
	 */
	@Override
	public int getFormHeight ()
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
	private String [] [] calculateMovementRates ()
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
