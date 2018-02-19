package run.rook.chi.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.agrona.DirectBuffer;

import run.rook.chi.ValueListener;
import run.rook.chi.data.DataType;

public class ValueCache implements ValueListener {
	private final Map<String, Long> numbers = Collections.synchronizedMap(new HashMap<>());
	private final Map<String, byte[]> buffers = Collections.synchronizedMap(new HashMap<>());
	
	@Override
	public void handle(String name, DataType dataType, long value) {
		numbers.put(name, value);
	}
	
	@Override
	public void handle(String name, DataType dataType, DirectBuffer value, int length) {
		byte[] b = new byte[length];
		value.getBytes(0, b);
		buffers.put(name, b);
	}
	
	public Long getNumber(String name) {
		return numbers.get(name);
	}
	
	public long getNumber(String name, long defaultValue) {
		return numbers.getOrDefault(name, defaultValue);
	}
	
	public byte[] getBuffer(String name) {
		return buffers.get(name);
	}
	
}
