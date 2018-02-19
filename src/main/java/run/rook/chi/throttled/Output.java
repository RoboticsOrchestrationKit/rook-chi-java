package run.rook.chi.throttled;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;

class Output {
	public static final int FLUSHED = 0;
	public static final int NEEDS_FLUSH = 1;
	public static final int POPULATING = 2;
	public static final int FLUSHING = 3;

	public AtomicInteger state = new AtomicInteger(FLUSHED);
	public AtomicInteger bufferSize = new AtomicInteger();
	public AtomicLong longValue = new AtomicLong();
	public final String name;
	public final OutputType type;
	public final AtomicBuffer bufferValue;

	public Output(String name, OutputType type) {
		this.name = name;
		this.type = type;
		this.bufferValue = new UnsafeBuffer(new byte[256]);
	}
}