package run.rook.chi.ws;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import org.agrona.DirectBuffer;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import run.rook.chi.Driver;
import run.rook.chi.ValueListener;
import run.rook.chi.data.DataType;
import run.rook.chi.data.DataTypeUtil;

class JsonSessionContext implements ValueListener {
	private static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.allocate(0);
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Gson gson = new Gson();
	private final Session session;
	private final Driver hardware;
	private final String address;
	
	private final Set<String> registeredNames = new HashSet<>();
	private boolean registeredAll = false;
	private boolean registeredWithHardware = false;

	public JsonSessionContext(Session session, Driver hardware) {
		this.session = session;
		this.hardware = hardware;
		this.address = session.getRemote().getInetSocketAddress().toString();
		new Thread(this::pingLoop, address + " Ping Loop").start();
	}
	
	private void pingLoop() {
		try {
			while(session.isOpen()) {
				Thread.sleep(10000);
				session.getRemote().sendPing(EMPTY_BYTE_BUFFER);
			}
		} catch(Exception e) {
			if(logger.isDebugEnabled()) {
				logger.debug("Ping/Pong Exception", e);
			}
			close();
		}
	}
	
	public void close() {
		hardware.deregisterListener(this);
		session.close();
	}

	public void subscribe(String name) {
		if (name == null) {
			registeredAll = true;
		} else {
			registeredNames.add(name);
		}
		checkRegisterWithHardware();
	}

	private void checkRegisterWithHardware() {
		if (!registeredWithHardware) {
			hardware.registerListener(this);
			registeredWithHardware = true;
		}
	}

	public void unsubscribe(String name) {
		if (name == null) {
			registeredAll = false;
		} else {
			registeredNames.remove(name);
		}
		checkDeregisterWithHardware();
	}

	private void checkDeregisterWithHardware() {
		if (registeredWithHardware && !registeredAll && registeredNames.size() == 0) {
			hardware.deregisterListener(this);
			registeredWithHardware = false;
		}
	}
	
	@Override
	public void handle(String name, DataType dataType, DirectBuffer value, int length) {
		if (registeredAll || registeredNames.contains(name)) {
			NamedValue m = new NamedValue();
			m.setName(name);
			m.setDataType(dataType);
			m.setValue(DataTypeUtil.toString(value, length, dataType));
			String json = gson.toJson(m);
			try {
				session.getRemote().sendString(json);
			} catch (Throwable t) {
				logger.info(toString() + " send failure. Closing Session.");
				close();
			}
		}
	}
	
	@Override
	public void handle(String name, DataType dataType, long value) {
		if (registeredAll || registeredNames.contains(name)) {
			NamedValue m = new NamedValue();
			m.setName(name);
			m.setDataType(dataType);
			m.setValue(Long.toString(value));
			String json = gson.toJson(m);
			try {
				session.getRemote().sendString(json);
			} catch (Throwable t) {
				logger.info(toString() + " send failure. Closing Session.");
				close();
			}
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [address=" + address + "]";
	}
}