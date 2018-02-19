package run.rook.chi.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import run.rook.chi.data.DataType;

class ValueManager {

	private final List<Value> values = Collections.synchronizedList(new ArrayList<>());
	private final Map<String, Integer> valueIndexes = Collections.synchronizedMap(new HashMap<>());
	
	public void setValue(String name, DataType dataType, long value) {
		Value output = getOrCreateValue(name, ValueType.LONG);
		if(output != null) {
			aquire(output);
			output.dataType = dataType;
			output.longValue.set(value);
			output.state.set(Value.NEEDS_FLUSH);
		}
	}
	
	public void setValue(String name, DataType dataType, DirectBuffer buffer, int length) {
		Value output = getOrCreateValue(name, ValueType.BUFFER);
		if(output != null) {
			aquire(output);
			output.dataType = dataType;
			if(output.bufferValue.capacity() < length) {
				output.bufferValue.wrap(new byte[length]);
			}
			output.bufferValue.putBytes(0, buffer, 0, length);
			output.bufferSize.set(length);
			output.state.set(Value.NEEDS_FLUSH);
		}
	}
	
	private Value getOrCreateValue(String name, ValueType type) {
		Integer idx = valueIndexes.get(name);
		if(idx == null) {
			synchronized (values) {
				idx = valueIndexes.get(name);
				if(idx == null) {
					// index still null, create value holder
					idx = values.size();
					valueIndexes.put(name, idx);
					values.add(new Value(name, type));
				} else {
					// race condition prior to synchronized block, no need to do anything
				}
			}
		}
		return values.get((int)idx);
	}
	
	private void aquire(Value output) {
		while(!output.state.compareAndSet(Value.FLUSHED, Value.POPULATING)) {
			if(output.state.compareAndSet(Value.NEEDS_FLUSH, Value.POPULATING)) {
				break;
			} else {
				Thread.yield();
			}
		}
	}

	public int numValues() {
		return values.size();
	}
	
	public Value getValue(int idx) {
		return values.get(idx);
	}
	
}

class Value {
	public static final int FLUSHED = 0;
	public static final int NEEDS_FLUSH = 1;
	public static final int POPULATING = 2;
	public static final int FLUSHING = 3;

	public AtomicInteger state = new AtomicInteger(FLUSHED);
	public AtomicInteger bufferSize = new AtomicInteger();
	public AtomicLong longValue = new AtomicLong();
	public final String name;
	public final ValueType type;
	public final AtomicBuffer bufferValue;
	public DataType dataType;
	
	public Value(String name, ValueType type) {
		this.name = name;
		this.type = type;
		this.bufferValue = new UnsafeBuffer(new byte[256]);
	}
}

enum ValueType {
	LONG, BUFFER;
}