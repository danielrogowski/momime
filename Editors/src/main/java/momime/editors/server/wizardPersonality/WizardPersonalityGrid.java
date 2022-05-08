package momime.editors.server.wizardPersonality;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jdom2.Element;

import com.ndg.xml.JdomUtils;
import com.ndg.xmleditor.editor.XmlEditorException;

import momime.editors.grid.MoMEditorGridWithDiplomacyMessagesImport;
import momime.editors.server.ServerEditorDatabaseConstants;

/**
 * Grid for displaying and editing unit names
 * Allows importing initial meeting phrases from the original MoM DIPLOMSG.LBX
 */
public final class WizardPersonalityGrid extends MoMEditorGridWithDiplomacyMessagesImport
{
	/**
	 * Imports initial meeting phrases from DIPLOMSG.LBX
	 * @param lbxFilename The LBX filename chosen in the file open dialog
	 * @throws IOException If there is a problem reading the LBX file
	 * @throws XmlEditorException If there is a problem using helper methods from the XML editor
	 */
	@Override
	protected final void importFromLbx (final File lbxFilename)
		throws IOException, XmlEditorException
	{
		final String language = getLanguageCombo ().getSelectedItem ().toString ();
		final List<String> list = readDiplomacyMessages (lbxFilename);

		int index = 225;
		for (int personalityNumber = 1; personalityNumber <= 6; personalityNumber++)
		{
			final String personalityID = "WP0" + personalityNumber;
			
			// Personality record should exist already; its a top level entity
			final Element personalityRecord = JdomUtils.findDomChildNodeWithTextAttribute (getContainer (), ServerEditorDatabaseConstants.TAG_ENTITY_WIZARD_PERSONALITY,
				ServerEditorDatabaseConstants.TAG_ATTRIBUTE_WIZARD_PERSONALITY_ID, personalityID);
			
			final List<String> variants = getMessageVariants (list, index);
			int variantNumber = 0;
			for (final String variant : variants)
			{
				variantNumber++;
				
				// Variant number may or may not exist under personality record
				Element variantRecord = JdomUtils.findDomChildNodeWithTextAttribute (personalityRecord, ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_INITIAL_MEETING_PHRASE,
					ServerEditorDatabaseConstants.TAG_ATTRIBUTE_VARIANT_NUMBER, Integer.valueOf (variantNumber).toString ());
				if (variantRecord == null)
				{
					variantRecord = new Element (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_INITIAL_MEETING_PHRASE);
					variantRecord.setAttribute (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_VARIANT_NUMBER, Integer.valueOf (variantNumber).toString ());
					personalityRecord.addContent (variantRecord);
				}
				
				// Add language record under variant record
				final Element languageRecord = new Element (ServerEditorDatabaseConstants.TAG_GRANDCHILD_ENTITY_TEXT_VARIANT);
				languageRecord.setAttribute (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_LANGUAGE, language);
				variantRecord.addContent (languageRecord);
				
				final Element textElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_TEXT);
				textElement.setText (variant);
				languageRecord.addContent (textElement);
			}
			
			index = index + VARIANT_COUNT;
		}
	}
}