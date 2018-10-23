package io.pivotal.websocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

//import com.google.gson.Gson;

public class ClientWebSocketHandler extends TextWebSocketHandler {
	private static final Logger logger = LoggerFactory.getLogger(ClientWebSocketHandler.class);
	private WebSocketSession session;
	private Queue<String> inboundMessageQueue = new PriorityQueue<String>();

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message)
			throws InterruptedException, IOException {
		inboundMessageQueue.add(message.getPayload());
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

	public Queue<String> getInboundMessageQueue() {
		return inboundMessageQueue;
	}
	
}
