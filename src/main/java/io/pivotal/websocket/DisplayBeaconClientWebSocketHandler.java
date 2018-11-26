package io.pivotal.websocket;

import java.net.InetSocketAddress;
import java.util.PriorityQueue;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.sockjs.client.WebSocketClientSockJsSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.pivotal.gemfire.domain.BeaconResponse;

public class DisplayBeaconClientWebSocketHandler extends TextWebSocketHandler {
	private static final Logger logger = LoggerFactory.getLogger(DisplayBeaconClientWebSocketHandler.class);
	private WebSocketSession session;
	private Queue<BeaconResponse> inboundMessageQueue = new PriorityQueue<BeaconResponse>();
	private WebSocketClientSockJsSession clientSession;

	public DisplayBeaconClientWebSocketHandler() {
	}

	public WebSocketClientSockJsSession getClientSession() {
		return clientSession;
	}

	public void setClientSession(WebSocketClientSockJsSession clientSession) {
		this.clientSession = clientSession;
	}

	public DisplayBeaconClientWebSocketHandler(WebSocketClientSockJsSession clientSession) {
		this.clientSession = clientSession;
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		BeaconResponse beaconResponse = convertJsonToObject(message.getPayload());
		if (beaconResponse.getUrl() == null || beaconResponse.getUrl().isEmpty()) {
			// beaconResponse.setUrl("http://www.arrms.org/media/blogs/blog/banana.jpg?mtime=1417645870");
			if (beaconResponse.getMessage() != null && beaconResponse.getMessage().length() > 0)
				beaconResponse.setUrl(
						"https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTH6RlXHjREjEc1dzHigqQC2Ng2pkBgSBgmoz1DPDojpRwItfGW");
		}
		if (beaconResponse.getMessage() == null || beaconResponse.getMessage().length() == 0) {
			beaconResponse.setMessage(beaconResponse.getError());
		}
		inboundMessageQueue.add(beaconResponse);
	}

	private BeaconResponse convertJsonToObject(String json) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(json, BeaconResponse.class);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		InetSocketAddress clientAddress = session.getRemoteAddress();
		logger.info("Accepted connection from: {}:{}", clientAddress.getHostString(), clientAddress.getPort());
		this.session = session;
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		logger.info("Connection closed by {}:{}", session.getRemoteAddress().getHostString(),
				session.getRemoteAddress().getPort());
		super.afterConnectionClosed(session, status);
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		logger.info("Connection closed by {}:{}", session.getRemoteAddress().getHostString(),
				session.getRemoteAddress().getPort());
	}

	public WebSocketSession getSession() {
		return session;
	}

	public Queue<BeaconResponse> getInboundMessageQueue() {
		return inboundMessageQueue;
	}

}
