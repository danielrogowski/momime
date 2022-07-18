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
 * Grid for displaying and editing diplomacy messages
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

		// These need to be in the order they're in the XSD or they won't import correctly
		importDiplomacyMessages (list, 630, LanguageEditorDatabaseConstants.TAG_ENTITY_NORMAL_GREETING_PHRASE, language);
		importDiplomacyMessages (list, 645, LanguageEditorDatabaseConstants.TAG_ENTITY_IMPATIENT_GREETING_PHRASE, language);
		importDiplomacyMessages (list, 660, LanguageEditorDatabaseConstants.TAG_ENTITY_REFUSE_GREETING_PHRASE, language);
		importDiplomacyMessages (list, 855, LanguageEditorDatabaseConstants.TAG_ENTITY_GENERIC_REFUSE_PHRASE, language);
		importDiplomacyMessages (list, 675, LanguageEditorDatabaseConstants.TAG_ENTITY_PROPOSE_WIZARD_PACT_PHRASE, language);
		importDiplomacyMessages (list, 765, LanguageEditorDatabaseConstants.TAG_ENTITY_ACCEPT_WIZARD_PACT_PHRASE, language);
		importDiplomacyMessages (list, 690, LanguageEditorDatabaseConstants.TAG_ENTITY_PROPOSE_ALLIANCE_PHRASE, language);
		importDiplomacyMessages (list, 780, LanguageEditorDatabaseConstants.TAG_ENTITY_ACCEPT_ALLIANCE_PHRASE, language);
		importDiplomacyMessages (list, 450, LanguageEditorDatabaseConstants.TAG_ENTITY_BREAK_PACT_PHRASE, language);
		importDiplomacyMessages (list, 75, LanguageEditorDatabaseConstants.TAG_ENTITY_BROKEN_PACT_PHRASE, language);
		importDiplomacyMessages (list, 480, LanguageEditorDatabaseConstants.TAG_PACT_BROKEN_UNITS_PHRASE, language);
		importDiplomacyMessages (list, 495, LanguageEditorDatabaseConstants.TAG_PACT_BROKEN_CITY_PHRASE, language);
		importDiplomacyMessages (list, 60, LanguageEditorDatabaseConstants.TAG_DECLARE_WAR_CITY_PHRASE, language);
		importDiplomacyMessages (list, 1065, LanguageEditorDatabaseConstants.TAG_ENTITY_GROWN_IMPATIENT_PHRASE, language);
		importDiplomacyMessages (list, 15, LanguageEditorDatabaseConstants.TAG_THANKS_FOR_GOLD_PHRASE, language);
		importDiplomacyMessages (list, 45, LanguageEditorDatabaseConstants.TAG_THANKS_FOR_SPELL_PHRASE, language);
		importDiplomacyMessages (list, 720, LanguageEditorDatabaseConstants.TAG_EXCHANGE_SPELL_OURS_PHRASE, language);
		importDiplomacyMessages (list, 1080, LanguageEditorDatabaseConstants.TAG_REFUSE_EXCHANGE_SPELL_PHRASE, language);
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