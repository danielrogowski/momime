package momime.server.utils;

import javax.xml.stream.XMLStreamException;

import jakarta.xml.bind.JAXBException;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;

/**
 * Process for making sure one wizard has met another wizard
 */
public interface KnownWizardServerUtils
{
	/**
	 * @param metWizardID The wizard who has become known
	 * @param meetingWizardID The wizard who now knows them; if null then everybody now knows them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param showAnimation Whether to show animation popup of wizard announcing themselves to you
	 * @throws RecordNotFoundException If we can't find the wizard we are meeting
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void meetWizard (final int metWizardID, final Integer meetingWizardID, final boolean showAnimation, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
}