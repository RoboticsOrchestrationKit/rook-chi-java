package run.rook.chi.ws;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import run.rook.chi.Driver;

public class ChiWebSocketCreator implements WebSocketCreator {

	private final JsonWebSocket jsonWebSocket;
	
	public ChiWebSocketCreator(Driver hardware) {
		this.jsonWebSocket = new JsonWebSocket(hardware);
	}
	
	@Override
	public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
		for (String protocol : req.getSubProtocols()) {
			switch (protocol) {
			case JsonWebSocket.PROTOCOL:
				resp.setAcceptedSubProtocol(protocol);
				return jsonWebSocket;
			}
		}
		return null;
	}

}