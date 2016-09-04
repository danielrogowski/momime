package momime.server.knowledge;

import momime.server.messages.v0_9_8.MomGeneralServerKnowledge;
import momime.server.messages.v0_9_8.ObjectFactory;
import momime.server.messages.v0_9_8.ServerGridCell;

/**
 * Object factory necessary to create correct extensions of classes when reloading saved games
 */
public final class MomServerKnowledgeObjectFactory extends ObjectFactory
{
	/**
	 * @return Extended server knowledge structure
	 */
	@Override
	public final MomGeneralServerKnowledge createMomGeneralServerKnowledge ()
	{
		return new MomGeneralServerKnowledgeEx ();
	}

	/**
	 * @return Extended grid cell
	 */
	@Override
	public final ServerGridCell createServerGridCell ()
	{
		return new ServerGridCellEx ();
	}	
}