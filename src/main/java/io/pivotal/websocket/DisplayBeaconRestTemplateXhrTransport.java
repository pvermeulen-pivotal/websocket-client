package io.pivotal.websocket;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;

public class DisplayBeaconRestTemplateXhrTransport extends RestTemplateXhrTransport {

	private final RestOperations restTemplate;

	public DisplayBeaconRestTemplateXhrTransport() {
		this(new RestTemplate());
	}

	public DisplayBeaconRestTemplateXhrTransport(RestOperations restTemplate) {
		super();
		Assert.notNull(restTemplate, "'restTemplate' is required");
		this.restTemplate = restTemplate;
	}

	@Override
	protected ResponseEntity<String> executeInfoRequestInternal(URI infoUrl, HttpHeaders headers) {
		RequestCallback requestCallback = new XhrRequestCallback(headers);
		return this.restTemplate.execute(infoUrl, HttpMethod.GET, requestCallback, infoResponseExtractor);
	}

	@Override
	public ResponseEntity<String> executeSendRequestInternal(URI url, HttpHeaders headers, TextMessage message) {
		RequestCallback requestCallback = new XhrRequestCallback(headers, message.getPayload());
		return this.restTemplate.execute(url, HttpMethod.POST, requestCallback, textResponseExtractor);
	}

	@Override
	public String executeInfoRequest(URI infoUrl, HttpHeaders headers) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SockJS Info request, url=" + infoUrl);
		}
		HttpHeaders infoRequestHeaders = new HttpHeaders();
		// infoRequestHeaders.putAll(getRequestHeaders());
		if (headers != null) {
			infoRequestHeaders.putAll(headers);
		}
		ResponseEntity<String> response = executeInfoRequestInternal(infoUrl, infoRequestHeaders);
		if ((response.getStatusCode() != HttpStatus.SWITCHING_PROTOCOLS)
				&& (response.getStatusCode() != HttpStatus.OK)) {
			if (logger.isErrorEnabled()) {
				logger.error("SockJS Info request (url=" + infoUrl + ") failed: " + response);
			}
			throw new HttpServerErrorException(response.getStatusCode());
		}
		if (logger.isTraceEnabled()) {
			logger.trace("SockJS Info request (url=" + infoUrl + ") response: " + response);
		}
		if (response.getStatusCode() == HttpStatus.SWITCHING_PROTOCOLS) {
			String body = response.getBody();
			if (body == null) {
				return headers.get("server").get(0);
			} else {
				return body;
			}
		} else {
			return response.getBody();
		}
	}

	private final static ResponseExtractor<ResponseEntity<String>> infoResponseExtractor = new ResponseExtractor<ResponseEntity<String>>() {
		@Override
		public ResponseEntity<String> extractData(ClientHttpResponse response) throws IOException {
			return new ResponseEntity<String>(response.getHeaders(), response.getStatusCode());
		}
	};

	private final static ResponseExtractor<ResponseEntity<String>> textResponseExtractor = new ResponseExtractor<ResponseEntity<String>>() {
		@Override
		public ResponseEntity<String> extractData(ClientHttpResponse response) throws IOException {
			if (response.getBody() == null) {
				return new ResponseEntity<String>(response.getHeaders(), response.getStatusCode());
			} else {
				String body = StreamUtils.copyToString(response.getBody(), SockJsFrame.CHARSET);
				return new ResponseEntity<String>(body, response.getHeaders(), response.getStatusCode());
			}
		}
	};

	private static class XhrRequestCallback implements RequestCallback {

		private final HttpHeaders headers;

		private final String body;

		public XhrRequestCallback(HttpHeaders headers) {
			this(headers, null);
		}

		public XhrRequestCallback(HttpHeaders headers, String body) {
			this.headers = headers;
			this.body = body;
		}

		@Override
		public void doWithRequest(ClientHttpRequest request) throws IOException {
			if (this.headers != null) {
				request.getHeaders().putAll(this.headers);
			}
			if (this.body != null) {
				StreamUtils.copy(this.body, SockJsFrame.CHARSET, request.getBody());
			}
		}
	}

}
