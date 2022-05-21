package com.redis.sidecar.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.List;

import javax.sql.RowSet;
import javax.sql.rowset.CachedRowSet;

import com.redis.sidecar.core.Config;
import com.redis.sidecar.core.Config.Rule;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;

public class SidecarStatement implements Statement {

	protected final SidecarConnection connection;
	private final Statement statement;

	protected String sql;
	protected ResultSet resultSet;

	public SidecarStatement(SidecarConnection connection, Statement statement) {
		this.connection = connection;
		this.statement = statement;
	}

	protected SidecarStatement(SidecarConnection connection, Statement statement, String sql) {
		this(connection, statement);
		this.sql = sql;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return statement.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return statement.isWrapperFor(iface);
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		this.sql = sql;
		this.resultSet = get(sql);
		if (resultSet == null) {
			resultSet = cache(sql, () -> statement.executeQuery(sql));
		}
		return resultSet;
	}

	protected ResultSet get(String sql) throws SQLException {
		return connection.getCache().get(key(sql));
	}

	protected String key(String sql) {
		return sql;
	}

	protected interface ResultSetProvider {

		ResultSet get() throws SQLException;
	}

	protected RowSet cache(String sql, ResultSetProvider provider) throws SQLException {
		CachedRowSet rowSet = connection.createCachedRowSet();
		rowSet.populate(provider.get());
		put(sql, rowSet);
		rowSet.beforeFirst();
		return rowSet;
	}

	protected void put(String sql, CachedRowSet rowSet) throws SQLException {
		connection.getCache().put(key(sql), ttl(sql), rowSet);
	}

	private long ttl(String sql) throws SQLException {
		net.sf.jsqlparser.statement.Statement statement;
		try {
			statement = CCJSqlParserUtil.parse(sql);
		} catch (JSQLParserException e) {
			throw new SQLException("Could not parse SQL", e);
		}
		Select selectStatement = (Select) statement;
		TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
		List<String> tableList = tablesNamesFinder.getTableList(selectStatement);
		for (Rule rule : connection.getConfig().getRules()) {
			if (rule.getTable() == null || tableList.contains(rule.getTable())) {
				return rule.getTtl();
			}
		}
		return Config.TTL_NO_CACHE;
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		return statement.executeUpdate(sql);
	}

	@Override
	public void close() throws SQLException {
		statement.close();
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		return statement.getMaxFieldSize();
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException {
		statement.setMaxFieldSize(max);
	}

	@Override
	public int getMaxRows() throws SQLException {
		return statement.getMaxRows();
	}

	@Override
	public void setMaxRows(int max) throws SQLException {
		statement.setMaxRows(max);
	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException {
		statement.setEscapeProcessing(enable);
	}

	@Override
	public int getQueryTimeout() throws SQLException {
		return statement.getQueryTimeout();
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		statement.setQueryTimeout(seconds);
	}

	@Override
	public void cancel() throws SQLException {
		statement.cancel();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return statement.getWarnings();
	}

	@Override
	public void clearWarnings() throws SQLException {
		statement.clearWarnings();
	}

	@Override
	public void setCursorName(String name) throws SQLException {
		statement.setCursorName(name);
	}

	@Override
	public boolean execute(String sql) throws SQLException {
		this.sql = sql;
		resultSet = get(sql);
		if (resultSet == null) {
			return statement.execute(sql);
		}
		return true;
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		if (resultSet == null) {
			resultSet = cache(sql, statement::getResultSet);
		}
		return resultSet;
	}

	@Override
	public int getUpdateCount() throws SQLException {
		return statement.getUpdateCount();
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		return statement.getMoreResults();
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		statement.setFetchDirection(direction);
	}

	@Override
	public int getFetchDirection() throws SQLException {
		return statement.getFetchDirection();
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		statement.setFetchSize(rows);
	}

	@Override
	public int getFetchSize() throws SQLException {
		return statement.getFetchSize();
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		return statement.getResultSetConcurrency();
	}

	@Override
	public int getResultSetType() throws SQLException {
		return statement.getResultSetType();
	}

	@Override
	public void addBatch(String sql) throws SQLException {
		statement.addBatch(sql);
	}

	@Override
	public void clearBatch() throws SQLException {
		statement.clearBatch();
	}

	@Override
	public int[] executeBatch() throws SQLException {
		return statement.executeBatch();
	}

	@Override
	public Connection getConnection() throws SQLException {
		return connection;
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException {
		return statement.getMoreResults();
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		return statement.getGeneratedKeys();
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		this.sql = sql;
		return statement.executeUpdate(sql, autoGeneratedKeys);
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		this.sql = sql;
		return statement.executeUpdate(sql, columnIndexes);
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		this.sql = sql;
		return statement.executeUpdate(sql, columnNames);
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		this.sql = sql;
		resultSet = get(sql);
		if (resultSet == null) {
			return statement.execute(sql, autoGeneratedKeys);
		}
		return true;
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		this.sql = sql;
		resultSet = get(sql);
		if (resultSet == null) {
			return statement.execute(sql, columnIndexes);
		}
		return true;
	}

	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException {
		this.sql = sql;
		resultSet = get(sql);
		if (resultSet == null) {
			return statement.execute(sql, columnNames);
		}
		return true;
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return statement.getResultSetHoldability();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return statement.isClosed();
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException {
		statement.setPoolable(poolable);
	}

	@Override
	public boolean isPoolable() throws SQLException {
		return statement.isPoolable();
	}

	@Override
	public void closeOnCompletion() throws SQLException {
		statement.closeOnCompletion();
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		return statement.isCloseOnCompletion();
	}

}
