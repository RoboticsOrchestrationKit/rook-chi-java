package run.rook.chi;

import org.agrona.DirectBuffer;

public interface Driver {
	void registerListener(ValueListener listener);
	void deregisterListener(ValueListener listener);
	void write(String name, long value);
	void write(String name, DirectBuffer value, int valueLength);
}
