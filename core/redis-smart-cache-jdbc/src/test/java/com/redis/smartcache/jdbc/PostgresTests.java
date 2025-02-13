package com.redis.smartcache.jdbc;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import com.redis.smartcache.Driver;
import com.redis.smartcache.core.Config;
import com.redis.testcontainers.junit.RedisTestContext;
import com.redis.testcontainers.junit.RedisTestContextsSource;

class PostgresTests extends AbstractIntegrationTests {

	private static final DockerImageName POSTGRE_DOCKER_IMAGE_NAME = DockerImageName.parse(PostgreSQLContainer.IMAGE)
			.withTag(PostgreSQLContainer.DEFAULT_TAG);

	@Container
	private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>(POSTGRE_DOCKER_IMAGE_NAME);

	@BeforeAll
	public void setupAll() throws SQLException, IOException {
		Connection backendConnection = connection(POSTGRESQL);
		runScript(backendConnection, "postgres/northwind.sql");
		runScript(backendConnection, "postgres/employee.sql");
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void testSimpleStatement(RedisTestContext redis) throws Exception {
		testSimpleStatement(POSTGRESQL, redis, "SELECT * FROM orders");
		Config config = bootstrapConfig();
		String cacheKeyPattern = config.key(Driver.CACHE_KEY_PREFIX, "*");
		List<String> keys = redis.sync().keys(cacheKeyPattern);
		Assertions.assertEquals(1, keys.size());
		testSimpleStatement(POSTGRESQL, redis, "SELECT * FROM employees");
		keys = redis.sync().keys(cacheKeyPattern);
		Assertions.assertEquals(2, keys.size());
		testSimpleStatement(POSTGRESQL, redis, "SELECT * FROM products");
		keys = redis.sync().keys(cacheKeyPattern);
		Assertions.assertEquals(3, keys.size());
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void testUpdateAndGetResultSet(RedisTestContext redis) throws Exception {
		testUpdateAndGetResultSet(POSTGRESQL, redis, "SELECT * FROM orders");
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void testPreparedStatement(RedisTestContext redis) throws Exception {
		testPreparedStatement(POSTGRESQL, redis, "SELECT * FROM orders WHERE employee_id = ?", 8);
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void testSimpleCallableStatement(RedisTestContext redis) throws Exception {
		testCallableStatement(POSTGRESQL, redis, "SELECT * FROM orders WHERE employee_id = ?", 8);
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void testCallableStatementParams(RedisTestContext redis) throws Exception {
		String runFunction = "{ ? = call hello( ? ) }";
		try (Connection connection = connection(POSTGRESQL, redis);
				Statement statement = connection.createStatement();
				CallableStatement callableStatement = connection.prepareCall(runFunction)) {
			callableStatement.registerOutParameter(1, Types.VARCHAR);
			callableStatement.setString(2, "julien");
			callableStatement.executeUpdate();
			Assertions.assertEquals("hello julien", callableStatement.getString(1));
		}
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void testCallableStatementRefCursor(RedisTestContext redis) throws Exception {
		String runFunction = "{? = call getUsers()}";
		try (Connection connection = connection(POSTGRESQL, redis);
				Statement statement = connection.createStatement();
				CallableStatement cs = connection.prepareCall(runFunction)) {
			// We must be inside a transaction for cursors to work.
			connection.setAutoCommit(false);
			// register output
			cs.registerOutParameter(1, Types.REF_CURSOR);
			// run function
			cs.execute();
			// get refcursor and convert it to ResultSet
			ResultSet resultSet = (ResultSet) cs.getObject(1);
			while (resultSet.next()) {
				Assertions.assertEquals("test", resultSet.getString("usename"));
				Assertions.assertEquals("********", resultSet.getString("passwd"));
			}

		}
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void testCallableStatementGetResultSet(RedisTestContext redis) throws Exception {
		testCallableStatementGetResultSet(POSTGRESQL, redis, "SELECT * FROM orders WHERE employee_id = 8");
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void testResultSetMetadata(RedisTestContext redis) throws Exception {
		testResultSetMetaData(POSTGRESQL, redis, "SELECT * FROM orders");
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void testConnect(RedisTestContext redis) throws SQLException, IOException {
		Config config = new Config();
		config.getDriver().setClassName(POSTGRESQL.getDriverClassName());
		config.getDriver().setUrl(POSTGRESQL.getJdbcUrl());
		Properties info = Driver.properties(config);
		info.setProperty("user", POSTGRESQL.getUsername());
		info.setProperty("password", POSTGRESQL.getPassword());
		java.sql.Driver driver = DriverManager.getDriver("jdbc:" + redis.getRedisURI());
		Connection connection = driver.connect("jdbc:" + redis.getRedisURI(), info);
		Assertions.assertInstanceOf(SmartConnection.class, connection);
	}

}
