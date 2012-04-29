package mveritym.cashflow.database;

import java.sql.SQLException;

import lib.Mitsugaru.SQLibrary.Database.Query;
import lib.Mitsugaru.SQLibrary.MySQL;
import lib.Mitsugaru.SQLibrary.SQLite;
import mveritym.cashflow.CashFlow;
import mveritym.cashflow.Config;

public class DBHandler {
	// Class Variables
	private CashFlow plugin;
	private Config config;
	private SQLite sqlite;
	private MySQL mysql;
	private boolean useMySQL;

	public DBHandler(CashFlow ks, Config conf) {
		plugin = ks;
		config = conf;
		useMySQL = config.useMySQL;
		checkTables();
		if (config.importSQL)
		{
			if (useMySQL)
			{
				importSQL();
			}
			config.set("mysql.import", false);
		}
	}

	private void checkTables() {
		if (useMySQL)
		{
			// Connect to mysql database
			mysql = new MySQL(plugin.log, plugin.prefix, config.host,
					config.port, config.database, config.user, config.password);
			// Check if master table exists
			if (!mysql.checkTable(config.tablePrefix + "cashflow"))
			{
				plugin.log.info(plugin.prefix + " Created master list table");
				// Master table
				mysql.createTable("CREATE TABLE "
						+ config.tablePrefix
						+ "cashflow (`playername` varchar(32) NOT NULL, `laston` REAL, `check` INTEGER, UNIQUE(`playername`));");
			}
			if (!mysql.checkTable(config.tablePrefix + "buffer"))
			{
				plugin.log.info(plugin.prefix + " Created buffer table");
				// Table to save buffer items
				mysql.createTable("CREATE TABLE "
						+ config.tablePrefix
						+ "buffer (`name` varchar(32) NOT NULL, `contract` TEXT NOT NULL, `tax` INTEGER NOT NULL);");
			}
		}
		else
		{
			// Connect to sql database
			sqlite = new SQLite(plugin.log, plugin.prefix, "database", plugin
					.getDataFolder().getAbsolutePath());
			// Check if master table exists
			if (!sqlite.checkTable(config.tablePrefix + "cashflow"))
			{
				plugin.log.info(plugin.prefix + " Created master list table");
				// Master table
				sqlite.createTable("CREATE TABLE "
						+ config.tablePrefix
						+ "cashflow (`playername` varchar(32) NOT NULL, `laston` REAL, `check` INTEGER, UNIQUE(`playername`));");
			}
			if (!sqlite.checkTable(config.tablePrefix + "buffer"))
			{
				plugin.log.info(plugin.prefix + " Created buffer table");
				// Table to save buffer items
				sqlite.createTable("CREATE TABLE "
						+ config.tablePrefix
						+ "buffer (`name` varchar(32) NOT NULL, `contract` TEXT NOT NULL, `tax` INTEGER NOT NULL);");
			}
		}
	}

	private void importSQL() {
		// Connect to sql database
		try
		{
			StringBuilder sb = new StringBuilder();
			// Grab local SQLite database
			sqlite = new SQLite(plugin.log, plugin.prefix, "database", plugin
					.getDataFolder().getAbsolutePath());
			// Copy items
			Query rs = sqlite.select("SELECT * FROM " + config.tablePrefix
					+ "cashflow;");
			if (rs.getResult().next())
			{
				plugin.log.info(plugin.prefix + " Importing master table...");
				do
				{
					boolean hasLast = false;
					final String name = rs.getResult().getString("playername");
					long laston = 0;
					try
					{
						laston = rs.getResult().getLong("laston");
						if (!rs.getResult().wasNull())
						{
							hasLast = true;
						}
					}
					catch (SQLException noColumn)
					{
						// Ignore
					}
					sb.append("INSERT INTO " + config.tablePrefix
							+ "cashflow (playername");
					if (hasLast)
					{
						sb.append(",laston");
					}
					sb.append(") VALUES('" + name + "'");
					if (hasLast)
					{
						sb.append(",'" + laston + "'");
					}
					sb.append(");");
					final String query = sb.toString();
					mysql.standardQuery(query);
					sb = new StringBuilder();
				}
				while (rs.getResult().next());
			}
			rs.closeQuery();
			sb = new StringBuilder();
			// Copy players
			rs = sqlite.select("SELECT * FROM " + config.tablePrefix
					+ "buffer;");
			if (rs.getResult().next())
			{
				plugin.log.info(plugin.prefix + " Importing buffer...");
				do
				{
					final String name = rs.getResult().getString("name");
					final String contract = rs.getResult().getString("contract");
					final int tax = rs.getResult().getInt("tax");
					sb.append("INSERT INTO " + config.tablePrefix
							+ "buffer (name,contract,tax) VALUES('" + name
							+ "','" + contract + "','" + tax + "');");
					final String query = sb.toString();
					mysql.standardQuery(query);
					sb = new StringBuilder();
				}
				while (rs.getResult().next());
			}
			rs.closeQuery();
			plugin.log
					.info(plugin.prefix + " Done importing SQLite into MySQL");
		}
		catch (SQLException e)
		{
			plugin.log.warning(plugin.prefix + " SQL Exception on Import");
			e.printStackTrace();
		}

	}

	public boolean checkConnection() {
		boolean connected = false;
		if (useMySQL)
		{
			connected = mysql.checkConnection();
		}
		else
		{
			connected = sqlite.checkConnection();
		}
		return connected;
	}

	public void close() {
		if (useMySQL)
		{
			mysql.close();
		}
		else
		{
			sqlite.close();
		}
	}

	public Query select(String query) {
		if (useMySQL)
		{
			return mysql.select(query);
		}
		else
		{
			return sqlite.select(query);
		}
	}

	public void standardQuery(String query) {
		if (useMySQL)
		{
			mysql.standardQuery(query);
		}
		else
		{
			sqlite.standardQuery(query);
		}
	}

	public void createTable(String query) {
		if (useMySQL)
		{
			mysql.createTable(query);
		}
		else
		{
			sqlite.createTable(query);
		}
	}
}
