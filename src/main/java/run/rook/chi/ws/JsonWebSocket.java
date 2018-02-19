package run.rook.chi.ws;

import static run.rook.chi.ws.JsonConst.TYPE_INBOUND_SUBSCRIBE;
import static run.rook.chi.ws.JsonConst.TYPE_INBOUND_UNSUBSCRIBE;
import static run.rook.chi.ws.JsonConst.TYPE_INBOUND_WRITE_VALUE;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.agrona.concurrent.UnsafeBuffer;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import run.rook.chi.Driver;
import run.rook.chi.data.DataType;
import run.rook.chi.data.DataTypeUtil;

@WebSocket
public class JsonWebSocket {

	public static final String PROTOCOL = "json";

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Gson gson = new Gson();
	private final Map<Session, JsonSessionContext> sessionContexts = Collections.synchronizedMap(new HashMap<>());
	private final Driver hardware;

	public JsonWebSocket(Driver hardware) {
		this.hardware = hardware;
	}

	@OnWebSocketConnect
	public void onWebSocketConnect(Session session) {
		logger.info("WebSocket Connect: " + session.getRemote().getInetSocketAddress());
		sessionContexts.put(session, new JsonSessionContext(session, hardware));
	}

	@OnWebSocketClose
	public void onWebSocketClose(Session session, int code, String reason) {
		logger.info("WebSocket Close: " + session.getRemote().getInetSocketAddress());
		JsonSessionContext context = sessionContexts.remove(session);
		if (context != null) {
			context.close();
		}
	}

	@OnWebSocketMessage
	public void onText(Session session, String message) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug(
					"Handling Request: remote=" + session.getRemote().getInetSocketAddress() + " message=" + message);
		}
		JsonInboundMessage req = gson.fromJson(message, JsonInboundMessage.class);
		if (req.type.equals(TYPE_INBOUND_SUBSCRIBE)) {
			subscribe(session, req.name);
		} else if (req.type.equals(TYPE_INBOUND_UNSUBSCRIBE)) {
			unsubscribe(session, req.name);
		} else if (req.type.equals(TYPE_INBOUND_WRITE_VALUE)) {
			setValue(session, req.name, req.dataType, req.value);
		}
	}

	private void subscribe(Session session, String name) {
		JsonSessionContext context = sessionContexts.get(session);
		if (context == null) {
			logger.warn("Received message from unknown session: " + session.getRemote().getInetSocketAddress());
			return;
		}
		context.subscribe(name);
	}

	private void unsubscribe(Session session, String name) {
		JsonSessionContext context = sessionContexts.get(session);
		if (context == null) {
			logger.warn("Received message from unknown session: " + session.getRemote().getInetSocketAddress());
			return;
		}
		context.unsubscribe(name);
	}

	private void setValue(Session session, String name, DataType dataType, String value) {
		if (logger.isDebugEnabled()) {
			logger.debug("Publishing Output: session=" + session.getRemote().getInetSocketAddress() + " name=" + name
					+ " dataType=" + dataType + " value=" + value);
		}
		
		byte[] bytes;
		
		switch(dataType) {
		case UTF8:
			bytes = value.getBytes(DataTypeUtil.UTF8);
			hardware.write(name, new UnsafeBuffer(bytes), bytes.length);
			break;
		case BLOB:
			bytes = Base64.getDecoder().decode(value);
			hardware.write(name, new UnsafeBuffer(bytes), bytes.length);
			break;
		case I8:
		case I16:
		case I32:
		case I64:
		case U8:
		case U16:
		case U32:
		case U64:
			hardware.write(name, Long.parseLong(value));
			break;
		default:
			logger.error("Unknown DataType: " + dataType);
			break;
		}
	}
}