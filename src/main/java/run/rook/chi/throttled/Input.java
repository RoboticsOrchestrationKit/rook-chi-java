package run.rook.chi.throttled;

import run.rook.chi.data.DataType;

class Input {
	public final String name;
	public final DataType type;

	public Input(String name, DataType type) {
		this.name = name;
		this.type = type;
	}
}