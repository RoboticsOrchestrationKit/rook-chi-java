package run.rook.chi.throttled;

import java.util.ArrayList;
import java.util.List;

import run.rook.chi.data.DataType;

class InputManager {

	private final List<Input> inputs = new ArrayList<>();
	private boolean initialized = false;

	public synchronized void init() {
		initialized = true;
	}

	public synchronized void addInput(String name, DataType type) {
		if (initialized) {
			throw new IllegalStateException("Cannot add output after init");
		}
		inputs.add(new Input(name, type));
	}

	public int numInputs() {
		return inputs.size();
	}

	public Input getInput(int idx) {
		return inputs.get(idx);
	}
}