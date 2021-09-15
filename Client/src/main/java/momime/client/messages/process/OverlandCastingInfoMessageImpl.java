package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.ui.frames.WizardsUI;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.messages.servertoclient.OverlandCastingInfo;
import momime.common.messages.servertoclient.OverlandCastingInfoMessage;

/**
 * Server telling us what spells all wizards are currently casting overland
 */
public final class OverlandCastingInfoMessageImpl extends OverlandCastingInfoMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** Wizards UI */
	private WizardsUI wizardsUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		getWizardsUI ().getOverlandCastingInfo ().clear ();
		
		for (final OverlandCastingInfo info : getOverlandCastingInfo ())
			getWizardsUI ().getOverlandCastingInfo ().put (info.getPlayerID (), info);
		
		if (getOurSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SPELL_BLAST))
		{
			final Spell spell = getClient ().getClientDB ().findSpell (getOurSpellID (), "OverlandCastingInfoMessageImpl");
			getWizardsUI ().setTargetingSpell (spell);
		}
		
		getWizardsUI ().setVisible (true);
		getWizardsUI ().updateWizards (false);
	}

	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}
	
	/**
	 * @return Wizards UI
	 */
	public final WizardsUI getWizardsUI ()
	{
		return wizardsUI;
	}

	/**
	 * @param ui Wizards UI
	 */
	public final void setWizardsUI (final WizardsUI ui)
	{
		wizardsUI = ui;
	}
}