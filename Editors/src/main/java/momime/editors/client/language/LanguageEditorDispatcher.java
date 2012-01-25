package momime.editors.client.language;

import java.io.IOException;
import java.util.List;

import momime.editors.client.language.building.BuildingGrid;
import momime.editors.client.language.heroName.HeroNameGrid;
import momime.editors.client.language.spell.SpellGrid;
import momime.editors.client.language.unit.UnitGrid;
import momime.editors.server.ServerEditorDatabaseConstants;

import org.jdom.Element;

import com.ndg.xmleditor.constants.XsdConstants;
import com.ndg.xmleditor.editor.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlDocument;
import com.ndg.xmleditor.editor.XmlEditorDispatcher;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.editor.XmlEditorMain;

/**
 * Provides specialised forms for certain grids and single record displays
 */
public final class LanguageEditorDispatcher extends XmlEditorDispatcher
{
	/**
	 * Creates the specialised main window
	 * @param xmlDocuments A list of the main XML document being edited, plus any referenced documents
	 * @return Created main editor window
	 * @throws XmlEditorException If there are syntax problems with the XSD
	 */
	@Override
	public XmlEditorMain createMainEditorWindow (final List<XmlDocument> xmlDocuments)
		throws XmlEditorException
	{
		return new LanguageEditorMain (xmlDocuments, this);
	}

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

		// building
		if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_BUILDING))
			new BuildingGrid (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor);

		// unit
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_UNIT))
			new UnitGrid (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor);

		// spell
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_SPELL))
			new SpellGrid (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor);

		// hero
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_HERO))
			new HeroNameGrid (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor);

		else
			super.showGrid (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor);
	}
}
