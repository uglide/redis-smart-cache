package com.redis.smartcache.test;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetMetaDataImpl;

public class RowSetBuilder {

	private static final JDBCType[] SUPPORTED_TYPES = { JDBCType.BIT, JDBCType.TINYINT, JDBCType.SMALLINT,
			JDBCType.INTEGER, JDBCType.BIGINT, JDBCType.FLOAT, JDBCType.REAL, JDBCType.DOUBLE, JDBCType.NUMERIC,
			JDBCType.DECIMAL, JDBCType.CHAR, JDBCType.VARCHAR, JDBCType.LONGVARCHAR, JDBCType.NCHAR, JDBCType.NVARCHAR,
			JDBCType.LONGNVARCHAR, JDBCType.DATE, JDBCType.TIME, JDBCType.TIMESTAMP };
	public static final int DEFAULT_ROW_COUNT = 1000;
	public static final int DEFAULT_COLUMN_COUNT = 10;
	private static final List<JDBCType> DEFAULT_TYPES = Arrays.asList(SUPPORTED_TYPES);

	private final Random random = new Random();
	private int leftLimit = 48; // numeral '0'
	private int rightLimit = 122; // letter 'z'
	private int displaySize = 100;
	private int columnNameSize = 10;
	private int columnLabelSize = 5;
	private int scale = 10;
	private int precision = 10;
	private int stringSize = 1000;
	private String catalogName = "";
	private String schemaName = "myschema";
	private String tableName = "mytable";

	private final RowSetFactory rowSetFactory;
	private int rowCount = DEFAULT_ROW_COUNT;
	private int columnCount = DEFAULT_COLUMN_COUNT;
	private List<JDBCType> jdbcTypes = DEFAULT_TYPES;

	public RowSetBuilder(RowSetFactory rowSetFactory) {
		this.rowSetFactory = rowSetFactory;
	}

	public static RowSetBuilder of(RowSetFactory rowSetFactory) {
		return new RowSetBuilder(rowSetFactory);
	}

	public RowSetBuilder rowCount(int count) {
		this.rowCount = count;
		return this;
	}

	public RowSetBuilder columnCount(int count) {
		this.columnCount = count;
		return this;
	}

	public RowSetBuilder jdbcTypes(JDBCType... types) {
		this.jdbcTypes = Arrays.asList(types);
		return this;
	}

	public CachedRowSet build() throws SQLException {
		CachedRowSet rowSet = rowSetFactory.createCachedRowSet();
		rowSet.setMetaData(metaData());
		for (int index = 0; index < rowCount; index++) {
			rowSet.moveToInsertRow();
			for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
				rowSet.updateObject(columnIndex, value(rowSet.getMetaData().getColumnType(columnIndex)));
			}
			rowSet.insertRow();
		}
		rowSet.moveToCurrentRow();
		rowSet.beforeFirst();
		return rowSet;
	}

	public RowSetMetaData metaData() throws SQLException {
		JDBCType[] columnTypes = new JDBCType[columnCount];
		for (int index = 0; index < columnCount; index++) {
			columnTypes[index] = jdbcTypes.get(index % jdbcTypes.size());
		}
		RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
		metaData.setColumnCount(columnTypes.length);
		for (int index = 0; index < columnTypes.length; index++) {
			int columnIndex = index + 1;
			JDBCType columnType = columnTypes[index];
			metaData.setAutoIncrement(columnIndex, false);
			metaData.setCaseSensitive(columnIndex, true);
			metaData.setCatalogName(columnIndex, catalogName);
			metaData.setColumnDisplaySize(columnIndex, displaySize);
			metaData.setColumnLabel(columnIndex, string(columnLabelSize));
			metaData.setColumnName(columnIndex, string(columnNameSize));
			metaData.setColumnType(columnIndex, columnType.getVendorTypeNumber());
			metaData.setColumnTypeName(columnIndex, columnType.getName());
			metaData.setCurrency(columnIndex, false);
			metaData.setNullable(columnIndex,
					index % 3 == 0 ? ResultSetMetaData.columnNoNulls : ResultSetMetaData.columnNullable);
			metaData.setPrecision(columnIndex, precision);
			metaData.setScale(columnIndex, scale);
			metaData.setSchemaName(columnIndex, schemaName);
			metaData.setSearchable(columnIndex, false);
			metaData.setSigned(columnIndex, false);
			metaData.setTableName(columnIndex, tableName);
		}
		return metaData;
	}

	private Object value(int type) {
		switch (type) {
		case Types.BIT:
		case Types.BOOLEAN:
			return true;
		case Types.TINYINT:
			return (byte) 123;
		case Types.SMALLINT:
			return (short) 123;
		case Types.INTEGER:
			return 123;
		case Types.BIGINT:
			return 123456789123L;
		case Types.FLOAT:
		case Types.REAL:
			return 123123123.123123123F;
		case Types.DOUBLE:
			return 123123123123.123123123123D;
		case Types.NUMERIC:
		case Types.DECIMAL:
			return BigDecimal.valueOf(1231231232131231231L);
		case Types.CHAR:
		case Types.VARCHAR:
		case Types.LONGVARCHAR:
		case Types.NCHAR:
		case Types.NVARCHAR:
		case Types.LONGNVARCHAR:
			return string(stringSize);
		case Types.DATE:
			return new java.sql.Date(epochMilli());
		case Types.TIME:
			return new Time(epochMilli());
		case Types.TIMESTAMP:
			return new Timestamp(epochMilli());
		default:
			throw new IllegalArgumentException("Type not supported: " + JDBCType.valueOf(type));
		}
	}

	private long epochMilli() {
		return Instant.now().toEpochMilli();
	}

	private String string(int length) {
		return random.ints(leftLimit, rightLimit + 1).filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
				.limit(length).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();
	}

	public static JDBCType[] supportedTypes() {
		return SUPPORTED_TYPES;
	}
}
