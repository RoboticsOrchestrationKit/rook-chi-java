package run.rook.chi.client;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import run.rook.chi.ValueListener;
import run.rook.chi.data.DataType;
import run.rook.chi.data.DataTypeUtil;

public class JsonWebsocketClient implements Client {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Gson gson = new Gson();
	private final WebSocketClient client = new WebSocketClient();
	private final UnderlyingWebSocket webSocket = new UnderlyingWebSocket();
	private final String url;
	private final ValueListener listener;

	public JsonWebsocketClient(ValueListener listener) {
		this("ws://localhost:16182", listener);
	}
	
	public JsonWebsocketClient(String url, ValueListener listener) {
		this.url = url;
		this.listener = listener;
	}
	
	@Override
	public void start() throws IOException {
		try {
			client.start();
			ClientUpgradeRequest request = new ClientUpgradeRequest();
			request.setSubProtocols("json");
			client.connect(webSocket, new URI(url), request).get();
		} catch(IOException e) {
			throw e;
		} catch(Exception e) {
			throw new IOException("Could not start " + getClass().getSimpleName(), e);
		}
	}

	@Override
	public void shutdown() throws IOException {
		try {
			client.stop();
		} catch(IOException e) {
			throw e;
		} catch(Exception e) {
			throw new IOException("Could not start " + getClass().getSimpleName(), e);
		}
	}

	@Override
	public void subscribe(String name) throws IOException {
		RequestMessage m = new RequestMessage();
		m.type = "subscribe";
		m.name = name;
		send(m);
	}

	@Override
	public void unsubscribe(String name) throws IOException {
		RequestMessage m = new RequestMessage();
		m.type = "unsubscribe";
		m.name = name;
		send(m);
	}

	@Override
	public void write(String name, DataType dataType, long value) throws IOException {
		RequestMessage m = new RequestMessage();
		m.type = "write";
		m.name = name;
		m.dataType = dataType;
		m.value = Long.toString(value);
		send(m);
	}

	@Override
	public void write(String name, DataType dataType, DirectBuffer value, int offset, int length) throws IOException {
		RequestMessage m = new RequestMessage();
		m.type = "write";
		m.name = name;
		m.dataType = dataType;
		if(dataType == DataType.UTF8) {
			byte[] bytes = value.byteArray();
			if(bytes == null) {
				bytes = new byte[length];
				value.getBytes(offset, bytes, 0, length);
				m.value = Base64.getEncoder().encodeToString(bytes);
			}
		} else if(dataType == DataType.BLOB) {
			m.value = value.getStringUtf8(offset, length);
		} else {
			throw new IllegalArgumentException("Unsupported data type for byte array: " + dataType);
		}
		send(m);
	}

	private void send(RequestMessage m) throws IOException {
		String json = gson.toJson(m);
		if(logger.isTraceEnabled()) {
			logger.trace("Sending: " + json);
		}
		try {
			webSocket.getSession().getRemote().sendStringByFuture(json).get();
		} catch (InterruptedException e) {
			throw new IOException(e);
		} catch (ExecutionException e) {
			throw new IOException(e);
		}
	}

	@SuppressWarnings("unused")
	private static class RequestMessage {
		public String type;
		public String name;
		public DataType dataType;
		public String value;
	}
	
	private static class ResponseMessage {
		public String name;
		public DataType dataType;
		public String value;
	}
	
	@WebSocket(maxTextMessageSize = 64 * 1024)
	public class UnderlyingWebSocket
	{
	    private volatile Session session;

	    @OnWebSocketClose
	    public void onClose(int statusCode, String reason)
	    {
	        this.session = null;
	    }

	    @OnWebSocketConnect
	    public void onConnect(Session session)
	    {
	        this.session = session;
	    }

	    @OnWebSocketMessage
	    public void onMessage(String json)
	    {
	    	if(logger.isTraceEnabled()) {
				logger.trace("Received: " + json);
			}
	    	if(listener != null) {
	    		ResponseMessage m = gson.fromJson(json, ResponseMessage.class);
	    		if(m.dataType == null) {
	    			return;
	    		}
	    		switch(m.dataType) {
	    		case I8:
	    		case I16:
	    		case I32:
	    		case I64:
	    		case U8:
	    		case U16:
	    		case U32:
	    		case U64:
	    			dispatchLong(m);
	    			break;
	    		case BLOB:
	    			dispatchBlob(m);
	    			break;
	    		case UTF8:
	    			dispatchUtf8(m);
	    			break;
	    		}
	    	}
	    }
	    
	    private void dispatchLong(ResponseMessage m) {
	    	listener.handle(m.name, m.dataType, Long.parseLong(m.value));
		}

		private void dispatchBlob(ResponseMessage m) {
			byte[] bytes = Base64.getDecoder().decode(m.value);
			listener.handle(m.name, m.dataType, new UnsafeBuffer(bytes), bytes.length);
		}

		private void dispatchUtf8(ResponseMessage m) {
			byte[] bytes = m.value.getBytes(DataTypeUtil.UTF8);
			listener.handle(m.name, m.dataType, new UnsafeBuffer(bytes), bytes.length);
		}

		public Session getSession() {
			return session;
		}
	}
}
