package run.rook.chi.ws;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import run.rook.chi.Driver;

public class ChiWebServer {

	private final int port;
	private final Driver driver;
	private Server server;

	public ChiWebServer(int port, Driver driver) {
		this.port = port;
		this.driver = driver;
	}

	public void start() throws Exception {
		WebSocketCreator wsCreator = new ChiWebSocketCreator(driver);
		WebSocketHandler wsHandler = new WebSocketHandler() {
			@Override
			public void configure(WebSocketServletFactory factory) {
				factory.setCreator(wsCreator);
			}
		};
		
		ResourceHandler htmlHandler = new ResourceHandler();
		htmlHandler.setDirectoriesListed(false);
		htmlHandler.setWelcomeFiles(new String[] { "index.html" });
		htmlHandler.setResourceBase(getClass().getResource("/html").toExternalForm());
		ContextHandler htmlContext = new ContextHandler();
		htmlContext.setContextPath("/");
		htmlContext.setHandler(htmlHandler);
		
		HandlerList handlerList = new HandlerList();
	    handlerList.setHandlers(new Handler[] { wsHandler, htmlHandler, new DefaultHandler() });
	    
	    server = new Server(port);
	    server.setHandler(handlerList);
	    server.start();
	}

	public void stop() throws Exception {
		server.stop();
	}
}
