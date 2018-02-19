package run.rook.chi;

import run.rook.chi.ws.ChiWebServer;

public class CommonHardwareInterface {

	public static final int DEFAULT_WEBSOCKET_PORT = 16182;
	
	private final ChiWebServer server;
	
	public CommonHardwareInterface(Driver driver) {
		this(driver, DEFAULT_WEBSOCKET_PORT);
	}
	
	public CommonHardwareInterface(Driver driver, int websocketPort) {
		server = new ChiWebServer(websocketPort, driver);
	}
	
	public void start() throws Exception {
		server.start();
	}
	
}
