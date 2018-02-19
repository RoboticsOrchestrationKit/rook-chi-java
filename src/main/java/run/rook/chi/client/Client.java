package run.rook.chi.client;

import java.io.IOException;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import run.rook.chi.data.DataType;
import run.rook.chi.data.DataTypeUtil;

public interface Client {

	void start() throws IOException;

	void shutdown() throws IOException;

	/**
	 * Subscribe to input data.
	 * 
	 * @param name
	 *            The input to register. null will receive all inputs.
	 */
	void subscribe(String name) throws IOException;

	/**
	 * Unsubscribe from input data.
	 * 
	 * @param name
	 *            The input to unsubscribe. 
	 * 
	 */
	void unsubscribe(String name) throws IOException;

	void write(String name, DataType dataType, long value) throws IOException;

	void write(String name, DataType dataType, DirectBuffer value, int offset, int length) throws IOException;

	default void write(String name, DataType dataType, byte[] value, int offset, int length) throws IOException {
		write(name, dataType, new UnsafeBuffer(value), offset, length);
	}

	default void write(String name, byte value) throws IOException {
		write(name, DataType.I8, value);	
	}
	
	default void write(String name, short value) throws IOException {
		write(name, DataType.I16, value);
	}
	
	default void write(String name, int value) throws IOException {
		write(name, DataType.I32, value);
	}
	
	default void write(String name, long value) throws IOException {
		write(name, DataType.I64, value);
	}
	
	default void write(String name, String value) throws IOException {
		byte[] buffer = value.getBytes(DataTypeUtil.UTF8);
		write(name, DataType.UTF8, buffer, 0, buffer.length);
	}
}
