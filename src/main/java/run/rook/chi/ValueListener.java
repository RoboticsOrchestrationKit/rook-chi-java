package run.rook.chi;

import org.agrona.DirectBuffer;

import run.rook.chi.data.DataType;

public interface ValueListener {
	void handle(String name, DataType dataType, long value);
	void handle(String name, DataType dataType, DirectBuffer value, int length);
}
