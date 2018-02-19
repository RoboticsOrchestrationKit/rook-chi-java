package run.rook.chi.client;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import run.rook.chi.ValueListener;
import run.rook.chi.data.DataType;

/**
 * Received values are cached atomically and the latest values are dispatched in
 * a separate thread, effectively eliminating issues due to user-created
 * back-pressure.
 * 
 * @author Eric Thill
 *
 */
abstract class AtomicClient implements Client {

	public static final IdleStrategy DEFAULT_IDLE_STRATEGY = new BackoffIdleStrategy(10, 100, 1000, 1000000);
	private static final AtomicLong UNIQUE_THREAD_NUMBER = new AtomicLong();

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final ValueManager valueManager = new ValueManager();
	private final ValueListener listener;
	private final IdleStrategy idleStrategy;
	private volatile boolean keepRunning;
	private volatile boolean running;

	public AtomicClient(ValueListener listener, IdleStrategy idleStrategy) {
		this.listener = listener;
		this.idleStrategy = idleStrategy;
	}

	@Override
	public final void start() throws IOException {
		keepRunning = true;
		running = true;
		new Thread(this::dispatchLoop, getClass().getSimpleName() + "_" + UNIQUE_THREAD_NUMBER.incrementAndGet())
				.start();
		onStart();
	}

	protected abstract void onStart() throws IOException;

	@Override
	public final void shutdown() throws IOException {
		keepRunning = false;
		while (running) {
			idleStrategy.idle();
		}
		onShutdown();
	}

	protected abstract void onShutdown() throws IOException;

	protected void handleValue(String name, DataType dataType, long value) {
		valueManager.setValue(name, dataType, value);

	}

	protected void handleValue(String name, DataType dataType, DirectBuffer value, int length) {
		valueManager.setValue(name, dataType, value, length);
	}

	private void dispatchLoop() {
		AtomicBuffer buffer = new UnsafeBuffer(new byte[256]);
		while (keepRunning) {
			int numValues = valueManager.numValues();
			boolean didSomething = false;
			for (int i = 0; i < numValues; i++) {
				Value value = valueManager.getValue(i);
				if (value.state.compareAndSet(Value.NEEDS_FLUSH, Value.FLUSHING)) {
					// locked
					String name = value.name;
					ValueType type = value.type;
					DataType dataType = value.dataType;
					long longValue = 0;
					int bufferLength = 0;
					if (value.type == ValueType.LONG) {
						longValue = value.longValue.get();
					} else if (value.type == ValueType.BUFFER) {
						bufferLength = value.bufferSize.get();
						if (bufferLength > buffer.capacity()) {
							buffer.wrap(new byte[bufferLength]);
						}
						buffer.putBytes(0, value.bufferValue, 0, bufferLength);
					}
					
					// unlock
					value.state.set(Value.FLUSHED);

					// dispatch outside of lock so callback does not cause
					// excess back-pressure, which would lead to OS-level TCP
					// queueing, which leads to stale data in callbacks, which
					// leads to a robot that seems to have lost its mind
					try {
						if(type == ValueType.LONG) {
							listener.handle(name, dataType, longValue);
						} else if(type == ValueType.BUFFER) {
							listener.handle(name, dataType, buffer, bufferLength);
						}
					} catch(Throwable t) {
						// user errors should not kill this thread, catch and log them
						logger.error("Error from userland", t);
					}
					didSomething = true;
				}
			}
			if (!didSomething) {
				idleStrategy.idle();
			}
		}
		running = false;
	}

}
