package com.redis.sidecar;

import java.nio.ByteBuffer;
import java.sql.SQLException;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import com.redis.sidecar.codec.ExplicitResultSetCodec;
import com.redis.sidecar.codec.JdkSerializationResultSetCodec;
import com.redis.sidecar.test.RowSetBuilder;

@State(Scope.Benchmark)
public class CodecExecutionPlan {

	private static final int BYTE_BUFFER_CAPACITY = 100000000;

	@Param({ "10", "100" })
	private int columns;
	@Param({ "10", "100", "1000" })
	private int rows;

	private RowSetFactory rowSetFactory;
	private ExplicitResultSetCodec explicitCodec;
	private JdkSerializationResultSetCodec jdkCodec;
	private ByteBuffer explicitByteBuffer;
	private ByteBuffer jdkByteBuffer;
	private CachedRowSet rowSet;

	@Setup(Level.Trial)
	public void setUpTrial() throws SQLException {
		this.rowSetFactory = RowSetProvider.newFactory();
		this.explicitCodec = ExplicitResultSetCodec.builder(rowSetFactory).maxByteBufferCapacity(BYTE_BUFFER_CAPACITY)
				.build();
		this.jdkCodec = new JdkSerializationResultSetCodec(rowSetFactory, BYTE_BUFFER_CAPACITY);
	}

	@Setup(Level.Invocation)
	public void setUpInvocation() throws SQLException {
		RowSetBuilder rowSetBuilder = new RowSetBuilder();
		this.rowSet = rowSetBuilder.build(rowSetBuilder.metaData(columns, RowSetBuilder.SUPPORTED_TYPES), rows);
		rowSet.beforeFirst();
		this.explicitByteBuffer = explicitCodec.encodeValue(rowSet);
		rowSet.beforeFirst();
		this.jdkByteBuffer = jdkCodec.encodeValue(rowSet);
		rowSet.beforeFirst();
	}

	public int getColumns() {
		return columns;
	}

	public int getRows() {
		return rows;
	}

	public CachedRowSet getRowSet() {
		return rowSet;
	}

	public ExplicitResultSetCodec getExplicitCodec() {
		return explicitCodec;
	}

	public ByteBuffer getExplicitByteBuffer() {
		return explicitByteBuffer;
	}

	public JdkSerializationResultSetCodec getJdkCodec() {
		return jdkCodec;
	}

	public ByteBuffer getJdkByteBuffer() {
		return jdkByteBuffer;
	}
}