package run.rook.chi.throttled;

import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;

class OutputBufferUpdate {
	public final AtomicBuffer buffer;
	public int length;

	public OutputBufferUpdate(int maxValueSize) {
		buffer = new UnsafeBuffer(new byte[maxValueSize]);
	}
}