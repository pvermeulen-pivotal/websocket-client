package io.pivotal.websocket;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.sockjs.client.TransportRequest;
import org.springframework.web.socket.sockjs.client.WebSocketClientSockJsSession;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

public class DisplayBeaconWebSocketTransport extends WebSocketTransport {
	private static final Log logger = LogFactory.getLog(WebSocketTransport.class);

	private final WebSocketClient webSocketClient;
	private WebSocketSession session;
	private DisplayBeaconClientWebSocketHandler displayBeaconClientWebSocketHandler;
	private DisplayBeaconClient displayBeaconClient;

	public WebSocketSession getSession() {
		return session;
	}

	public DisplayBeaconClientWebSocketHandler getDisplayBeaconClientWebSocketHandler() {
		return displayBeaconClientWebSocketHandler;
	}

	public DisplayBeaconWebSocketTransport(WebSocketClient webSocketClient, DisplayBeaconClientWebSocketHandler displayBeaconClientWebSocketHandler,
			DisplayBeaconClient displayBeaconClient) {
		super(webSocketClient);
		Assert.notNull(webSocketClient, "WebSocketClient is required");
		this.webSocketClient = webSocketClient;
		this.displayBeaconClientWebSocketHandler = displayBeaconClientWebSocketHandler;
		this.displayBeaconClient = displayBeaconClient;
	}

	@Override
	public ListenableFuture<WebSocketSession> connect(TransportRequest request, WebSocketHandler handler) {
		final SettableListenableFuture<WebSocketSession> future = new SettableListenableFuture<WebSocketSession>();
		WebSocketClientSockJsSession clientSession = new WebSocketClientSockJsSession(request, handler, future);
		displayBeaconClientWebSocketHandler.setClientSession(clientSession);
		// request.addTimeoutTask(session.getTimeoutTask());

		URI url = request.getTransportUrl();
		WebSocketHttpHeaders headers = new WebSocketHttpHeaders(request.getHandshakeHeaders());
		if (logger.isDebugEnabled()) {
			logger.debug("Starting WebSocket session url=" + url);
		}
		this.webSocketClient.doHandshake(displayBeaconClientWebSocketHandler, headers, url)
				.addCallback(new ListenableFutureCallback<WebSocketSession>() {
					@Override
					public void onSuccess(WebSocketSession webSocketSession) {
						session = webSocketSession;
						DisplayBeaconClient db = new DisplayBeaconClient();
						db.createBackground();
						db.createPanel();
						displayBeaconClient.getNoPopUp().set(false);
						displayBeaconClient.getPopScreenBeacon().set(0);
						db.startBeacon();
						logger.info("on Success DisplayBeaconWebSocketTransport");
					}

					@Override
					public void onFailure(Throwable ex) {
						future.setException(ex);
					}
				});
		return future;
	}

}
