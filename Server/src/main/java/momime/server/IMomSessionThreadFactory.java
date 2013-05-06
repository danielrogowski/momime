package momime.server;

/**
 * Factory interface for creating MomSessionThreads
 */
public interface IMomSessionThreadFactory
{
	/**
	 * @return Newly created thread
	 */
	public MomSessionThread createThread ();
}
