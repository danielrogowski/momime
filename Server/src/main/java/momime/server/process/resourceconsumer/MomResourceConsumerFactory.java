package momime.server.process.resourceconsumer;

/**
 * Factory interface to allow spring to create resource consumers
 */
public interface MomResourceConsumerFactory
{
	/**
	 * @return Newly created building consumer
	 */
	public MomResourceConsumerBuilding createBuildingConsumer ();

	/**
	 * @return Newly created spell consumer
	 */
	public MomResourceConsumerSpell createSpellConsumer ();

	/**
	 * @return Newly created unit consumer
	 */
	public MomResourceConsumerUnit createUnitConsumer ();
}
