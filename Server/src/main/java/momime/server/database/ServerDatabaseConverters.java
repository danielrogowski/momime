package momime.server.database;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import momime.client.database.v0_9_4.ClientDatabase;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.servertoclient.v0_9_4.NewGameDatabaseMessage;

/**
 * Converters for building derivative XML files from the server XML file
 * Old Delphi unit: MomServerDB.pas
 */
public interface ServerDatabaseConverters
{
	/**
	 * Finds all the compatible (i.e. correct namespace) XML databases on the server and extracts a small portion of each needed for setting up new games
	 * @param xmlFolder Folder in which to look for server XML files, e.g. F:\Workspaces\Delphi\Master of Magic\XML Files\Server\
	 * @param serverDatabaseUnmarshaller JAXB Unmarshaller for loading server XML files
	 * @return Info extracted from all available XML databases
	 * @throws JAXBException If there is a problem creating the server XML unmarshaller
	 * @throws MomException If there are no compatible server XML databases
	 * @throws IOException If there is a problem reading the XML databases
	 */
	public NewGameDatabaseMessage buildNewGameDatabase (final File xmlFolder, final Unmarshaller serverDatabaseUnmarshaller)
		throws JAXBException, MomException, IOException;

	/**
	 * @param src Server side database loaded from XML
	 * @param humanSpellPicks Number of picks human players get in this game, as per session description
	 * @return Info extracted from server XML
	 * @throws RecordNotFoundException If one of the wizards does not have picks for the specified number of human picks defined
	 */
	public ClientDatabase buildClientDatabase (final ServerDatabaseEx src, final int humanSpellPicks) throws RecordNotFoundException;
}
