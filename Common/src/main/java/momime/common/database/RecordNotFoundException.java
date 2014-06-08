package momime.common.database;

import java.io.IOException;

/**
 * Type of exception thrown when we look for a particular record that we expect to be present in the XML file and we can't find it
 */
public final class RecordNotFoundException extends IOException
{
	/** Unique value for serialization */
	private static final long serialVersionUID = -955521547372783138L;

	/**
	 * Creates an exception for failing to find a record in a table with a single String primary key
	 * @param table The table (or XML record type) where we're searching for a particular record
	 * @param keyValue The value of the primary key we were expecting to find
	 * @param caller The routine that was looking for the value
	 */
	public RecordNotFoundException (final String table, final String keyValue, final String caller)
	{
		super ("\"" + table + "\" record with key value \"" + keyValue + "\" not found by \"" + caller + "\"");
	}

	/**
	 * Creates an exception for failing to find a record in a table with a single integer primary key
	 * @param table The table (or XML record type) where we're searching for a particular record
	 * @param keyValue The value of the primary key we were expecting to find
	 * @param caller The routine that was looking for the value
	 */
	public RecordNotFoundException (final String table, final int keyValue, final String caller)
	{
		this (table, new Integer (keyValue).toString (), caller);
	}

	/**
	 * Creates an exception for failing to find a record in a table with a single String primary key
	 * @param table The table (or XML record type) where we're searching for a particular record
	 * @param keyValue The value of the primary key we were expecting to find
	 * @param caller The routine that was looking for the value
	 */
	public RecordNotFoundException (final Class<?> table, final String keyValue, final String caller)
	{
		this (table.getName (), keyValue, caller);
	}

	/**
	 * Creates an exception for failing to find a record in a table with a single integer primary key
	 * @param table The table (or XML record type) where we're searching for a particular record
	 * @param keyValue The value of the primary key we were expecting to find
	 * @param caller The routine that was looking for the value
	 */
	public RecordNotFoundException (final Class<?> table, final int keyValue, final String caller)
	{
		this (table.getName (), keyValue, caller);
	}
}
