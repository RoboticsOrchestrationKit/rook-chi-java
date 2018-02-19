package run.rook.chi.throttled;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.agrona.DirectBuffer;

class OutputManager {

	private final List<Output> outputs = new ArrayList<>();
	private final Map<String, Integer> outputIndexes = new HashMap<>();
	private boolean initialized = false;
	
	public synchronized void init() {
		initialized = true;
	}
	
	public synchronized void addOutput(String name, OutputType type) {
		if(initialized) {
			throw new IllegalStateException("Cannot add output after init");
		}
		int idx = outputs.size();
		outputIndexes.put(name, idx);
		outputs.add(new Output(name, type));
	}
	
	public void setOutput(String name, long value) {
		Output output = getOutput(name);
		if(output != null) {
			aquire(output);
			output.longValue.set(value);
			output.state.set(Output.NEEDS_FLUSH);
		}
	}
	
	public void setOutput(String name, DirectBuffer buffer, int length) {
		Output output = getOutput(name);
		if(output != null) {
			aquire(output);
			if(output.bufferValue.capacity() < length) {
				output.bufferValue.wrap(new byte[length]);
			}
			output.bufferValue.putBytes(0, buffer, 0, length);
			output.bufferSize.set(length);
			output.state.set(Output.NEEDS_FLUSH);
		}
	}
	
	private void aquire(Output output) {
		while(!output.state.compareAndSet(Output.FLUSHED, Output.POPULATING)) {
			if(output.state.compareAndSet(Output.NEEDS_FLUSH, Output.POPULATING)) {
				break;
			} else {
				Thread.yield();
			}
		}
	}

	private Output getOutput(String name) {
		Integer idx = outputIndexes.get(name);
		if(idx == null) {
			return null;
		} else {
			return outputs.get((int)idx);
		}
	}
	
	public int numOutputs() {
		return outputs.size();
	}
	
	public Output getOutput(int idx) {
		return outputs.get(idx);
	}
	
}