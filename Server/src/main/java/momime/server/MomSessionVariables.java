package momime.server;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.calculations.MomCityCalculations;
import momime.common.messages.v0_9_4.MomGeneralPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.UnitUtils;
import momime.server.calculations.MomServerResourceCalculations;
import momime.server.calculations.MomServerUnitCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.mapgenerator.OverlandMapGenerator;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;
import momime.server.process.CityProcessing;
import momime.server.process.CombatProcessing;
import momime.server.process.PlayerMessageProcessing;
import momime.server.process.SpellProcessing;
import momime.server.utils.CityServerUtils;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.utils.PlayerPickServerUtils;
import momime.server.utils.SpellServerUtils;
import momime.server.utils.UnitServerUtils;

import com.ndg.map.CoordinateSystem;
import com.ndg.multiplayer.server.MultiplayerServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Container for all the values held against a session
 * Declared as an interface mainly so test methods can mock this without needing to actually create instances of MomSessionThread
 */
public interface MomSessionVariables
{
	/**
	 * @return Session description, typecasted to MoM specific type
	 */
	public MomSessionDescription getSessionDescription ();
	
	/**
	 * @return Combat map coordinate system, expect this to be merged into session desc once client is also in Java
	 */
	public CoordinateSystem getCombatMapCoordinateSystem ();

	/**
	 * @return Server XML in use for this session
	 */
	public ServerDatabaseEx getServerDB ();

	/**
	 * @return Server general knowledge, typecasted to MoM specific type
	 */
	public MomGeneralServerKnowledge getGeneralServerKnowledge ();
	
	/**
	 * @return Public knowledge structure, typecasted to MoM specific type
	 */
	public MomGeneralPublicKnowledge getGeneralPublicKnowledge ();
	
	/**
	 * @return All information about all players in this session - some may be AI players or disconnected
	 */
	public List<PlayerServerDetails> getPlayers ();
	
	/**
	 * @return Server-side multiplayer utils
	 */
	public MultiplayerServerUtils getMultiplayerServerUtils ();
	
	/**
	 * @return Logger for logging key messages relating to this session
	 */
	public Logger getSessionLogger ();
	
	/**
	 * @return Methods for updating true map + players' memory
	 */
	public FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ();

	/**
	 * @return Methods for dealing with player msgs
	 */
	public PlayerMessageProcessing getPlayerMessageProcessing ();
	
	/**
	 * @return Spell processing methods
	 */
	public SpellProcessing getSpellProcessing ();

	/**
	 * @return City processing methods
	 */
	public CityProcessing getCityProcessing ();
	
	/**
	 * @return Combat processing
	 */
	public CombatProcessing getCombatProcessing ();

	/**
	 * @return City calculcations
	 */
	public MomCityCalculations getCityCalculations ();
	
	/**
	 * @return Resource calculations
	 */
	public MomServerResourceCalculations getServerResourceCalculations ();
	
	/**
	 * @return Resource value utils
	 */
	public ResourceValueUtils getResourceValueUtils ();
	
	/**
	 * @return Player pick utils
	 */
	public PlayerPickUtils getPlayerPickUtils ();
	
	/**
	 * @return Unit utils
	 */
	public UnitUtils getUnitUtils ();
	
	/**
	 * @return Spell utils
	 */
	public SpellUtils getSpellUtils ();

	/**
	 * @return Maintained spell utils 
	 */
	public MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ();

	/**
	 * @return Builiding utils 
	 */
	public MemoryBuildingUtils getMemoryBuildingUtils ();
	
	/**
	 * @return Pending movement utils
	 */
	public PendingMovementUtils getPendingMovementUtils (); 
	
	/**
	 * @return Server-only city utils
	 */
	public CityServerUtils getCityServerUtils ();

	/**
	 * @return Server-only unit utils
	 */
	public UnitServerUtils getUnitServerUtils ();
	
	/**
	 * @return Server-only pick utils
	 */
	public PlayerPickServerUtils getPlayerPickServerUtils ();
	
	/**
	 * @return Server-only spell utils
	 */
	public SpellServerUtils getSpellServerUtils ();
	
	/**
	 * @return Server-only unit calculations
	 */
	public MomServerUnitCalculations getServerUnitCalculations ();
	
	/**
	 * @return Overland map generator for this session
	 */
	public OverlandMapGenerator getOverlandMapGenerator ();
	
	/**
	 * @return Server-only overland map utils
	 */
	public OverlandMapServerUtils getOverlandMapServerUtils ();
	
	/**
	 * Adds an AI player
	 *
	 * @param newPlayer Details of player who requested new session
	 * @return Newly created player
	 * @throws JAXBException If there is a problem converting the reply into XML
	 * @throws XMLStreamException If there is a problem writing the reply to the XML stream
	 */
	public PlayerServerDetails addComputerPlayer (final PlayerDescription newPlayer)
		throws JAXBException, XMLStreamException;
}
