package momime.server;

/**
 * Factory interface for creating MomSessionThreads
 */
public interface MomSessionThreadFactory
{
	/**
	 * @return Newly created thread
	 */
	public MomSessionThread createThread ();
}
