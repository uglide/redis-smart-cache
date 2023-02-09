package com.redis.smartcache;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.rowset.CachedRowSet;

import io.lettuce.core.internal.LettuceAssert;
import io.trino.sql.parser.ParsingException;
import io.trino.sql.parser.ParsingOptions;
import io.trino.sql.parser.SqlParser;
import io.trino.sql.tree.AstVisitor;
import io.trino.sql.tree.Node;
import io.trino.sql.tree.QualifiedName;
import io.trino.sql.tree.Table;

public class CachingConnection implements Connection {

	private final Connection connection;
	private final ConnectionContext context;
	private final SqlParser parser = new SqlParser();
	private final ParsingOptions options = new ParsingOptions();

	public CachingConnection(Connection connection) {
		this(connection, new ConnectionContext());
	}

	public CachingConnection(Connection connection, ConnectionContext context) {
		LettuceAssert.notNull(context, "Connection context is required");
		this.connection = connection;
		this.context = context;
	}

	public ConnectionContext getContext() {
		return context;
	}

	@Override
	public void close() throws SQLException {
		connection.close();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return connection.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return connection.isWrapperFor(iface);
	}

	@Override
	public Statement createStatement() throws SQLException {
		Statement statement = connection.createStatement();
		return new CachingStatement(this, statement, context.getMeterRegistry());
	}

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		PreparedStatement statement = connection.prepareStatement(sql);
		return new CachingPreparedStatement(this, statement, context.getMeterRegistry(), sql);
	}

	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		CallableStatement statement = connection.prepareCall(sql);
		return new CachingCallableStatement(this, statement, context.getMeterRegistry(), sql);
	}

	@Override
	public String nativeSQL(String sql) throws SQLException {
		return connection.nativeSQL(sql);
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		connection.setAutoCommit(autoCommit);
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		return connection.getAutoCommit();
	}

	@Override
	public void commit() throws SQLException {
		connection.commit();
	}

	@Override
	public void rollback() throws SQLException {
		connection.rollback();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return connection.isClosed();
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		return connection.getMetaData();
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		connection.setReadOnly(readOnly);
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return connection.isReadOnly();
	}

	@Override
	public void setCatalog(String catalog) throws SQLException {
		connection.setCatalog(catalog);
	}

	@Override
	public String getCatalog() throws SQLException {
		return connection.getCatalog();
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		connection.setTransactionIsolation(level);
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		return connection.getTransactionIsolation();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return connection.getWarnings();
	}

	@Override
	public void clearWarnings() throws SQLException {
		connection.clearWarnings();
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		Statement statement = connection.createStatement(resultSetType, resultSetConcurrency);
		return new CachingStatement(this, statement, context.getMeterRegistry());
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		PreparedStatement statement = connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
		return new CachingPreparedStatement(this, statement, context.getMeterRegistry(), sql);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		CallableStatement statement = connection.prepareCall(sql, resultSetType, resultSetConcurrency);
		return new CachingCallableStatement(this, statement, context.getMeterRegistry(), sql);
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return connection.getTypeMap();
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		connection.setTypeMap(map);
	}

	@Override
	public void setHoldability(int holdability) throws SQLException {
		connection.setHoldability(holdability);
	}

	@Override
	public int getHoldability() throws SQLException {
		return connection.getHoldability();
	}

	@Override
	public Savepoint setSavepoint() throws SQLException {
		return connection.setSavepoint();
	}

	@Override
	public Savepoint setSavepoint(String name) throws SQLException {
		return connection.setSavepoint(name);
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		connection.rollback(savepoint);
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		connection.releaseSavepoint(savepoint);
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		Statement statement = connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
		return new CachingStatement(this, statement, context.getMeterRegistry());
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		PreparedStatement statement = connection.prepareStatement(sql, resultSetType, resultSetConcurrency,
				resultSetHoldability);
		return new CachingPreparedStatement(this, statement, context.getMeterRegistry(), sql);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		CallableStatement statement = connection.prepareCall(sql, resultSetType, resultSetConcurrency,
				resultSetHoldability);
		return new CachingCallableStatement(this, statement, context.getMeterRegistry(), sql);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		PreparedStatement statement = connection.prepareStatement(sql, autoGeneratedKeys);
		return new CachingPreparedStatement(this, statement, context.getMeterRegistry(), sql);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		PreparedStatement statement = connection.prepareStatement(sql, columnIndexes);
		return new CachingPreparedStatement(this, statement, context.getMeterRegistry(), sql);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		PreparedStatement statement = connection.prepareStatement(sql, columnNames);
		return new CachingPreparedStatement(this, statement, context.getMeterRegistry(), sql);
	}

	@Override
	public Clob createClob() throws SQLException {
		return connection.createClob();
	}

	@Override
	public Blob createBlob() throws SQLException {
		return connection.createBlob();
	}

	@Override
	public NClob createNClob() throws SQLException {
		return connection.createNClob();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		return connection.createSQLXML();
	}

	@Override
	public boolean isValid(int timeout) throws SQLException {
		return connection.isValid(timeout);
	}

	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		connection.setClientInfo(name, value);
	}

	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		connection.setClientInfo(properties);
	}

	@Override
	public String getClientInfo(String name) throws SQLException {
		return connection.getClientInfo(name);
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		return connection.getClientInfo();
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		return connection.createArrayOf(typeName, elements);
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		return connection.createStruct(typeName, attributes);
	}

	@Override
	public void setSchema(String schema) throws SQLException {
		connection.setSchema(schema);
	}

	@Override
	public String getSchema() throws SQLException {
		return connection.getSchema();
	}

	@Override
	public void abort(Executor executor) throws SQLException {
		connection.abort(executor);
	}

	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		connection.setNetworkTimeout(executor, milliseconds);
	}

	@Override
	public int getNetworkTimeout() throws SQLException {
		return connection.getNetworkTimeout();
	}

	public ResultSetCache getCache() {
		return context.getCache();
	}

	public CachedRowSet createCachedRowSet() throws SQLException {
		return context.getRowSetFactory().createCachedRowSet();
	}

	public void evaluateRules(CachingStatement statement) {
		context.getRuleSession().fire(statement);
	}

	public Stream<Table> extractTables(String sql) throws ParsingException {
		return parser.createStatement(sql, options).accept(DepthFirstVisitor.by(new TableVisitor()), null);
	}

	public Set<String> extractTableNames(String sql) {
		return extractTables(sql).map(Table::getName).map(QualifiedName::toString).collect(Collectors.toSet());
	}

	static class DepthFirstVisitor<R, C> extends AstVisitor<Stream<R>, C> {

		private final AstVisitor<R, C> visitor;

		public DepthFirstVisitor(AstVisitor<R, C> visitor) {
			this.visitor = visitor;
		}

		public static <R, C> DepthFirstVisitor<R, C> by(AstVisitor<R, C> visitor) {
			return new DepthFirstVisitor<>(visitor);
		}

		@Override
		public final Stream<R> visitNode(Node node, C context) {
			Stream<R> nodeResult = Stream.of(visitor.process(node, context));
			Stream<R> childrenResult = node.getChildren().stream().flatMap(child -> process(child, context));

			return Stream.concat(nodeResult, childrenResult).filter(Objects::nonNull);
		}
	}

	static class TableVisitor extends AstVisitor<Table, Object> {

		@Override
		protected Table visitTable(Table node, Object context) {
			return node;
		}
	}

}