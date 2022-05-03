package com.redis.sidecar.impl;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CachedResultSetMetaData implements ResultSetMetaData {

	private final List<ColumnMetaData> columns;
	private final Map<String, Integer> columnNames = new HashMap<>();

	public CachedResultSetMetaData(List<ColumnMetaData> columns) {
		this.columns = columns;
		for (int index = 0; index < columns.size(); index++) {
			ColumnMetaData column = columns.get(index);
			columnNames.put(column.getColumnName(), index + 1);
		}
	}

	public List<ColumnMetaData> getColumns() {
		return columns;
	}

	@Override
	public int getColumnCount() {
		return columns.size();
	}

	private ColumnMetaData getColumn(int column) throws SQLException {
		if (column == 0 || column > columns.size())
			throw new SQLException("Wrong column number: " + column);
		return columns.get(column - 1);
	}

	@Override
	public boolean isAutoIncrement(int column) throws SQLException {
		return getColumn(column).isAutoIncrement();
	}

	@Override
	public boolean isCaseSensitive(int column) throws SQLException {
		return getColumn(column).isCaseSensitive();
	}

	@Override
	public boolean isSearchable(int column) throws SQLException {
		return getColumn(column).isSearchable();
	}

	@Override
	public boolean isCurrency(int column) throws SQLException {
		return getColumn(column).isCurrency();
	}

	@Override
	public int isNullable(int column) throws SQLException {
		return getColumn(column).getIsNullable();
	}

	@Override
	public boolean isSigned(int column) throws SQLException {
		return getColumn(column).isSigned();
	}

	@Override
	public int getColumnDisplaySize(int column) throws SQLException {
		return getColumn(column).getColumnDisplaySize();
	}

	@Override
	public String getColumnLabel(int column) throws SQLException {
		return getColumn(column).getColumnLabel();
	}

	@Override
	public String getColumnName(int column) throws SQLException {
		return getColumn(column).getColumnName();
	}

	@Override
	public String getSchemaName(int column) throws SQLException {
		return getColumn(column).getSchemaName();
	}

	@Override
	public int getPrecision(int column) throws SQLException {
		return getColumn(column).getPrecision();
	}

	@Override
	public int getScale(int column) throws SQLException {
		return getColumn(column).getScale();
	}

	@Override
	public String getTableName(int column) throws SQLException {
		return getColumn(column).getTableName();
	}

	@Override
	public String getCatalogName(int column) throws SQLException {
		return getColumn(column).getCatalogName();
	}

	@Override
	public int getColumnType(int column) throws SQLException {
		return getColumn(column).getColumnType();
	}

	@Override
	public String getColumnTypeName(int column) throws SQLException {
		return getColumn(column).getColumnTypeName();
	}

	@Override
	public boolean isReadOnly(int column) throws SQLException {
		return getColumn(column).isReadOnly();
	}

	@Override
	public boolean isWritable(int column) throws SQLException {
		return getColumn(column).isWritable();
	}

	@Override
	public boolean isDefinitelyWritable(int column) throws SQLException {
		return getColumn(column).isDefinitelyWritable();
	}

	@Override
	public String getColumnClassName(int column) throws SQLException {
		return getColumn(column).getColumnClassName();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	public Integer getColumnIndex(String name) {
		return columnNames.get(name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(columns);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CachedResultSetMetaData other = (CachedResultSetMetaData) obj;
		return Objects.equals(columns, other.columns);
	}

}
