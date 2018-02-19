package run.rook.chi.throttled;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import run.rook.chi.Driver;
import run.rook.chi.ValueListener;
import run.rook.chi.data.DataType;

/**
 * Abstract implementation of {@link Driver} that reads/writes at a specified
 * interval
 * 
 * @author Eric Thill
 *
 */
public abstract class ThrottledDriver implements Driver {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Set<ValueListener> listeners = new LinkedHashSet<>();
	private final InputManager inputManager = new InputManager();
	private final OutputManager outputManager = new OutputManager();
	private final long throttleInterval;

	private final Queue<RegisterEvent> registerQueue = new ManyToOneConcurrentLinkedQueue<>();

	public ThrottledDriver(long throttleInterval) {
		this.throttleInterval = throttleInterval;
	}

	public void addInput(String name, DataType type) {
		inputManager.addInput(name, type);
	}

	public void addOutput(String name, OutputType type) {
		outputManager.addOutput(name, type);
	}

	public void init() {
		inputManager.init();
		outputManager.init();
		new Thread(this::throttleLoop).start();
	}

	protected abstract long doReadLong(String name) throws IOException;

	protected abstract int doReadBuffer(String name, DirectBuffer buffer) throws IOException;

	protected abstract void doWrite(String name, long value) throws IOException;

	protected abstract void doWrite(String name, DirectBuffer value, int length) throws IOException;

	private void throttleLoop() {
		int outputIdx = 0;
		int inputIdx = 0;
		int numOutputs = outputManager.numOutputs();
		int numInputs = inputManager.numInputs();
		AtomicBuffer buffer = new UnsafeBuffer(new byte[256]);
		while (true) {
			if(numInputs > 0) {
				processInput(inputIdx++, buffer);
				if (inputIdx == numInputs) {
					inputIdx = 0;
				}
			}
			if(numOutputs > 0) {
				processOutput(outputIdx++);
				if (outputIdx == numOutputs) {
					outputIdx = 0;
				}
			}
			processRegisterEvent(registerQueue.poll());
		}
	}

	private void processInput(int inputIdx, AtomicBuffer buffer) {
		try {
			Input input = inputManager.getInput(inputIdx);
			switch (input.type) {
			case I8:
			case I16:
			case I32:
			case I64:
			case U8:
			case U16:
			case U32:
			case U64:
				dispatch(input.name, input.type, doReadLong(input.name));
				break;
			case BLOB:
			case UTF8:
				int bufferLength = doReadBuffer(input.name, buffer);
				dispatch(input.name, input.type, buffer, bufferLength);
				break;
			}
		} catch (IOException e) {
			// a bad read can happen, not an actionable event, so debug level
			logger.debug("Failure reading input", e);
		}
		throttle();
	}

	private void processOutput(int outputIdx) {
		// try up to 3 times
		for(int i = 0; i < 3; i++) {
			try {
				Output output = outputManager.getOutput(outputIdx);
				if (output.state.compareAndSet(Output.NEEDS_FLUSH, Output.FLUSHING)) {
					if (output.type == OutputType.LONG) {
						doWrite(output.name, output.longValue.get());
					} else if (output.type == OutputType.BUFFER) {
						doWrite(output.name, output.bufferValue, output.bufferSize.get());
					}
					output.state.set(Output.FLUSHED);
					throttle();
				}
				// success, break out of retry loop
				break;
			} catch (IOException e) {
				logger.warn("Failure writing output", e);
			}
		}
	}

	private void throttle() {
		trySleep(throttleInterval);
	}

	private void trySleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void processRegisterEvent(RegisterEvent event) {
		if (event != null) {
			if (event.type == RegisterEventType.REGISTER) {
				listeners.add(event.listener);
			} else if (event.type == RegisterEventType.DEREGISTER) {
				listeners.remove(event.listener);
			}
		}
	}

	private void dispatch(String name, DataType dataType, DirectBuffer value, int length) {
		for (ValueListener listener : listeners) {
			listener.handle(name, dataType, value, length);
		}
	}

	private void dispatch(String name, DataType dataType, long value) {
		for (ValueListener listener : listeners) {
			listener.handle(name, dataType, value);
		}
	}

	@Override
	public void registerListener(ValueListener listener) {
		registerQueue.add(new RegisterEvent(RegisterEventType.REGISTER, listener));
	}

	@Override
	public void deregisterListener(ValueListener listener) {
		registerQueue.add(new RegisterEvent(RegisterEventType.DEREGISTER, listener));
	}

	@Override
	public void write(String name, long value) {
		outputManager.setOutput(name, value);
	}

	@Override
	public void write(String name, DirectBuffer value, int valueLength) {
		outputManager.setOutput(name, value, valueLength);
	}

}