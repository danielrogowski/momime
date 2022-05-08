package momime.editors.client.language.diplomacyScreen;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jdom2.Element;

import com.ndg.xml.JdomUtils;
import com.ndg.xmleditor.editor.XmlEditorException;

import momime.editors.client.language.LanguageEditorDatabaseConstants;
import momime.editors.grid.MoMEditorGridWithDiplomacyMessagesImport;
import momime.editors.server.ServerEditorDatabaseConstants;

/**
 * Grid for displaying and editing unit names
 * Allows importing initial meeting phrases from the original MoM DIPLOMSG.LBX
 */
public final class DiplomacyScreenGrid extends MoMEditorGridWithDiplomacyMessagesImport
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

		importDiplomacyMessages (list, 630, LanguageEditorDatabaseConstants.TAG_ENTITY_NORMAL_GREETING_PHRASE, language);
		importDiplomacyMessages (list, 645, LanguageEditorDatabaseConstants.TAG_ENTITY_IMPATIENT_GREETING_PHRASE, language);
		importDiplomacyMessages (list, 660, LanguageEditorDatabaseConstants.TAG_ENTITY_REFUSE_GREETING_PHRASE, language);
	}
	
	/**
	 * @param list Messages read from DIPLOMSG.LBX 
	 * @param index Index into list to start extracting messages from
	 * @param tag Tag to output messages to
	 * @param language Language to output messages as
	 */
	private final void importDiplomacyMessages (final List<String> list, final int index, final String tag, final String language)
	{
		// diplomacyScreen record must exist for us to have accessed this editor
		final Element diplomacyScreenRecord = getContainer ().getChild (getEntityTag ());
		
		final List<String> variants = getMessageVariants (list, index);
		int variantNumber = 0;
		for (final String variant : variants)
		{
			variantNumber++;
			
			// Variant number may or may not exist under diplomacyScreen record
			Element variantRecord = JdomUtils.findDomChildNodeWithTextAttribute (diplomacyScreenRecord, tag,
				ServerEditorDatabaseConstants.TAG_ATTRIBUTE_VARIANT_NUMBER, Integer.valueOf (variantNumber).toString ());
			if (variantRecord == null)
			{
				variantRecord = new Element (tag);
				variantRecord.setAttribute (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_VARIANT_NUMBER, Integer.valueOf (variantNumber).toString ());
				diplomacyScreenRecord.addContent (variantRecord);
			}
			
			// Add language record under variant record
			final Element languageRecord = new Element (ServerEditorDatabaseConstants.TAG_GRANDCHILD_ENTITY_TEXT_VARIANT);
			languageRecord.setAttribute (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_LANGUAGE, language);
			variantRecord.addContent (languageRecord);
			
			final Element textElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_TEXT);
			textElement.setText (variant);
			languageRecord.addContent (textElement);
		}
	}
}