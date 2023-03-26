package momime.client;

import java.util.Map;

import com.ndg.multiplayer.client.MultiplayerSessionClient;

import momime.client.database.NewGameDatabase;
import momime.client.ui.frames.ChangeConstructionUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.HeroItemInfoUI;
import momime.client.ui.frames.OfferUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.common.database.CommonDatabase;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;

/**
 * A lot of data structures, especially the db, are accessed from the client, but in ways that can't easily be
 * mocked out in unit tests.  So instead this interface exists as the reference point for any places that
 * obtain data structures from the client so we don't have to use the real MomClientImpl in unit tests.
 */
public interface MomClient extends MultiplayerSessionClient
{
	/**
	 * @return Public knowledge structure, typecasted to MoM specific type
	 */
	@Override
	public MomGeneralPublicKnowledge getGeneralPublicKnowledge ();

	/**
	 * @return Client XML in use for this session
	 */
	public CommonDatabase getClientDB ();
	
	/**
	 * @return Session description, typecasted to MoM specific type
	 */
	@Override
	public MomSessionDescription getSessionDescription ();
	
	/**
	 * @return Private knowledge about our player that is persisted to save game files,  typecasted to MoM specific type
	 */
	@Override
	public MomPersistentPlayerPrivateKnowledge getOurPersistentPlayerPrivateKnowledge ();
	
	/**
	 * @return Private knowledge about our player that is not persisted to save game files,  typecasted to MoM specific type
	 */
	@Override
	public MomTransientPlayerPrivateKnowledge getOurTransientPlayerPrivateKnowledge ();

	/**
	 * @return List of all city views currently open, keyed by coordinates.toString ()
	 */
	public Map<String, CityViewUI> getCityViews ();
	
	/**
	 * @return List of all change constructions currently open, keyed by coordinates.toString ()
	 */
	public Map<String, ChangeConstructionUI> getChangeConstructions ();
	
	/**
	 * @return List of all unit info screens currently open, keyed by Unit URN
	 */
	public Map<Integer, UnitInfoUI> getUnitInfos ();
	
	/**
	 * @return List of all hero item info screens currently open, keyed by Hero Item URN
	 */
	public Map<Integer, HeroItemInfoUI> getHeroItemInfos ();

	/**
	 * @return List of all offer screens currently open, keyed by Offer URN
	 */
	public Map<Integer, OfferUI> getOffers ();
	
	/**
	 * @return Info we need in order to create games; sent from server
	 */
	public NewGameDatabase getNewGameDatabase ();

	/**
	 * @param db Info we need in order to create games; sent from server
	 */
	public void setNewGameDatabase (final NewGameDatabase db);
	
	/**
	 * Used to stop players taking action when its someone else's turn (one at a time turns) or after they hit Next Turn (simultaneous turns)
	 * 
	 * @return Whether its currently this player's turn or not
	 */
	public boolean isPlayerTurn ();
}