package momime.editors.client.graphics;

import java.io.IOException;

import momime.editors.server.ServerEditorDatabaseConstants;

import org.jdom.Element;

import com.ndg.xmleditor.constants.XsdConstants;
import com.ndg.xmleditor.editor.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlEditorDispatcher;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.editor.XmlEditorMain;

/**
 * Provides specialised grids which display the images in extra columns
 */
public final class GraphicsEditorDispatcher extends XmlEditorDispatcher
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

		// wizard
		if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_WIZARD))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_WIZARD_PORTRAIT_FILE}, 110);
		
		// productionType - productionTypeImage
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_PRODUCTION_TYPE_IMAGE))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_PRODUCTION_IMAGE_FILE}, 20);
		
		// race - racePopulationTask
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_RACE_POPULATION_TASK))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_CIVILIAN_IMAGE_FILE}, 20);
		
		// pick - bookImage
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_BOOK_IMAGE))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_BOOK_IMAGE_FILE}, 30);
		
		// spell
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_SPELL))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_OVERLAND_ENCHANTMENT_IMAGE_FILE}, 120);
		
		// combatAreaEffect
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_COMBAT_AREA_EFFECT))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_COMBAT_AREA_EFFECT_IMAGE_FILE}, 20);

		// unitAttribute
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_UNIT_ATTRIBUTE))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_UNIT_ATTRIBUTE_IMAGE_FILE}, 20);

		// rangedAttackType
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_RANGED_ATTACK_TYPE))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_UNIT_DISPLAY_RAT_IMAGE}, 20);

		// rangedAttackType - rangedAttackTypeCombatImage
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_RANGED_ATTACK_TYPE_COMBAT_IMAGE))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_RAT_COMBAT_IMAGE_FILE}, 30);
		
		// unit
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_UNIT))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_UNIT_SUMMON_IMAGE,
					ServerEditorDatabaseConstants.TAG_VALUE_UNIT_OVERLAND_IMAGE, ServerEditorDatabaseConstants.TAG_VALUE_UNIT_HERO_IMAGE}, 84);

		// unit - unitCombatAction - unitCombatImage
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_GRANDCHILD_ENTITY_UNIT_COMBAT_IMAGE))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_UNIT_COMBAT_IMAGE_FILE}, 40);

		// unitSkill
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_UNIT_SKILL))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_UNIT_SKILL_IMAGE,
					ServerEditorDatabaseConstants.TAG_VALUE_UNIT_SKILL_MOVE_IMAGE, ServerEditorDatabaseConstants.TAG_VALUE_UNIT_SKILL_SAMPLE_TILE_IMAGE}, 20);
		
		// tileSet - smoothedTileType - smoothedTile
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_GRANDCHILD_ENTITY_UNIT_SMOOTHED_TILE))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_SMOOTHED_TILE_FILE}, 24);

		// tileType
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_TILE_TYPE))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_TILE_TYPE_MONSTER_FOUND_IMAGE}, 70);

		// mapFeature
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_MAP_FEATURE))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_MAP_FEATURE_MONSTER_FOUND_IMAGE,
					ServerEditorDatabaseConstants.TAG_VALUE_MAP_FEATURE_OVERLAND_IMAGE}, 70);

		// cityImage
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_CITY_IMAGE))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_CITY_IMAGE_FILE}, 36);
		
		// combatTileBorderImage
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_COMBAT_TILE_BORDER_IMAGE))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_COMBAT_TILE_BORDER_STANDARD_FILE,
					ServerEditorDatabaseConstants.TAG_VALUE_COMBAT_TILE_BORDER_WRECKED_FILE}, 40);
		
		// cityViewElement
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_ENTITY_CITY_VIEW_ELEMENT))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_CITY_VIEW_IMAGE_FILE,
					ServerEditorDatabaseConstants.TAG_VALUE_CITY_VIEW_ALT_IMAGE_FILE}, 40);
		
		// animation - frame
		else if (entityTag.equals (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_ANIMATION_FRAME))
			new XmlEditorGridWithImages (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor,
				new String [] {ServerEditorDatabaseConstants.TAG_VALUE_FRAME_IMAGE_FILE}, 40);
		
		else
			super.showGrid (entityElement, typeDefinition, parentRecord, parentEntityElements, parentTypeDefinitions, mdiEditor);
	}
}
