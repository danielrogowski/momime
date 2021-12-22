package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.common.messages.servertoclient.PlaneShiftUnitStackMessage;

/**
 * A unit stack tried to jump from one plane to the other, maybe successfully or not.  If failed then moveTo = moveFrom.
 */
public final class PlaneShiftUnitStackMessageImpl extends PlaneShiftUnitStackMessage implements BaseServerToClientMessage
{
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		if (getMoveTo ().equals (getMoveFrom ()))
		{
			final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
			msg.setLanguageTitle (getLanguages ().getOverlandMapScreen ().getPlaneShiftFailedTitle ());
			msg.setLanguageText (getLanguages ().getOverlandMapScreen ().getPlaneShiftFailedText ());
			msg.setVisible (true);
		}
		
		getOverlandMapProcessing ().selectNextUnitToMoveOverland ();
	}

	/**
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
	}

	/**
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
	}

	/**
	 * @return Turn sequence and movement helper methods
	 */
	public final OverlandMapProcessing getOverlandMapProcessing ()
	{
		return overlandMapProcessing;
	}

	/**
	 * @param proc Turn sequence and movement helper methods
	 */
	public final void setOverlandMapProcessing (final OverlandMapProcessing proc)
	{
		overlandMapProcessing = proc;
	}
}