package momime.server.knowledge;

import momime.server.messages.ObjectFactory;
import momime.server.messages.ServerGridCell;

/**
 * Object factory necessary to create correct extensions of classes when reloading saved games
 */
public final class MomServerKnowledgeObjectFactory extends ObjectFactory
{
	/**
	 * @return Extended grid cell
	 */
	@Override
	public final ServerGridCell createServerGridCell ()
	{
		return new ServerGridCellEx ();
	}	
}