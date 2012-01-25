package momime.editors.server;

import java.io.IOException;

import momime.editors.server.pickTypeCount.PickTypeCountGrid;
import momime.editors.server.raceCannotBuild.RaceCannotBuildGrid;
import momime.editors.server.spell.SpellGrid;
import momime.editors.server.unit.EditUnit;
import momime.editors.server.wizardPickCount.WizardPickCountGrid;

import org.jdom.Element;

import com.ndg.xmleditor.constants.XsdConstants;
import com.ndg.xmleditor.editor.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlEditorDispatcher;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.editor.XmlEditorMain;

/**
 * Provides specialised forms for certain grids and single record displays
 */
public final class ServerEditorDispatcher extends XmlEditorDispatcher
{
	/**
	 * Creates specialised grids for certain record types
	 * @param entityElement The xsd:element node of the entity being edited from the XSD, i.e. for a top level entity, this will be a the entry under the xsd:sequence under the database complex type
	 * @param typeDefinition The xsd:complexType node of the entity being edited from the XSD
	 * @param parentRecord If this is a child entity, this value holds the parent record; if this is a top level entity, this value will be null
	 * @param parentEntityElements Array of xsd:element entries that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @param parentTypeDefinitions Array of type definitions that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @param mdiEditor The main MDI window
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 * @throws IOException If there is a problem loading the button images
	 */
	@Override
	public void showGrid (final Element entityElement, final ComplexTypeReference typeDefinition,
		final Element parentRecord, final Element [] parentEntityElements, final ComplexTypeReference [] parentTypeDefinitions, final XmlEditorMain mdiEditor)
		throws XmlEditorException, IOException
	{
		final String entityTag = entityElement.getAttributeValue (XsdConstants.TAG_ATTRIBUTE_FIELD_NAME);

		// pickType - pickTypeCount
		if (entityTag.equals (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_PICK_TYPE_COUNT))
			new PickTypeCountGrid (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor);

		// wizard - wizardPickCount
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_WIZARD_PICK_COUNT))
			new WizardPickCountGrid (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor);

		// race - raceCannotBuild
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_RACE_CANNOT_BUILD))
			new RaceCannotBuildGrid (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor);

		// spell
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_SPELL))
			new SpellGrid (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor);

		else
			super.showGrid (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor);
	}

	/**
	 * Creates specialised forms for certain record types
	 * @param entityElement The xsd:element node of the entity being edited from the XSD, i.e. for a top level entity, this will be a the entry under the xsd:sequence under the database complex type
	 * @param typeDefinition The xsd:complexType node of the entity being edited from the XSD
	 * @param mdiEditor The main MDI window
	 * @param recordBeingEdited The record being edited, or null if we're adding a new record
	 * @param recordBeingCopied True if we're copying the specified record rather than editing it
	 * @param parentRecord The parent record which contains this child record, or null if this is a top level record (it most cases you can get this from recordBeingEdited.getParent (), except when creating a new record)
	 * @param parentEntityElements Array of xsd:element entries that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @param parentTypeDefinitions Array of type definitions that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 * @throws IOException If there is a problem loading the button images
	 */
	@Override
	public void showRecord (final Element entityElement, final ComplexTypeReference typeDefinition, final XmlEditorMain mdiEditor,
		final Element recordBeingEdited, final boolean recordBeingCopied, final Element parentRecord, final Element [] parentEntityElements, final ComplexTypeReference [] parentTypeDefinitions)
		throws XmlEditorException, IOException
	{
		final String entityTag = entityElement.getAttributeValue (XsdConstants.TAG_ATTRIBUTE_FIELD_NAME);

		// unit
		if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_UNIT))
			new EditUnit (entityElement, typeDefinition, mdiEditor, recordBeingEdited, recordBeingCopied, parentRecord, parentEntityElements, parentTypeDefinitions);

		else
			super.showRecord (entityElement, typeDefinition, mdiEditor, recordBeingEdited, recordBeingCopied, parentRecord, parentEntityElements, parentTypeDefinitions);
	}
}
