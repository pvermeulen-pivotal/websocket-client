package io.pivotal.websocket;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.awt.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.Box;

public class DrawBeaconImages extends JPanel {
	private static final long serialVersionUID = 3765567846172558688L;
	private static final Logger logger = LoggerFactory.getLogger(DrawBeaconImages.class);
	private static AtomicBoolean killTimer = new AtomicBoolean(false);
	private static int screenNumber = 0;
	private static java.util.List<ImageIcon> bgBackgrounds = new ArrayList<>();
	private static java.util.List<IconText> icons = new ArrayList<>();
	private static JFrame mainFrame;
	private static JFrame bgFrame;
	private static JPanel bgPanel;
	private static JPanel mainPanel;
	private static JPanel mainImagePanel;
	private static JPanel mainTextPanel;
	private static JLabel mainImageLabel;
	private static JLabel mainBeaconImageLabel;
	private static JTextArea mainTextArea;
	private static JLabel bgImageLabel;
	private static JButton bgButton;
	private static Timer timer;
	private static SockJsClient sockJsClient;
	private static ClientWebSocketHandler clientWebSocketHandler;
	private static WebSocketSession session;
	private static List<Transport> transports;
	private static String[] beaconRequests = {
			"{ \"customerId\": \"12345678\", \"deviceId\" : \"TTH12S\", \"uuid\": \"ABCDEF0123456789\", \"major\": \"1000\", \"minor\": \"0\", \"signalPower\": \"-30\" }",
			"{ \"customerId\": \"12345678\", \"deviceId\" : \"TTH12S\", \"uuid\": \"ABCDEF0123456789\", \"major\": \"1000\", \"minor\": \"1\", \"signalPower\": \"-30\" }",
			"{ \"customerId\": \"12345678\", \"deviceId\" : \"TTH12S\", \"uuid\": \"ABCDEF0123456789\", \"major\": \"1000\", \"minor\": \"2\", \"signalPower\": \"-30\" }",
			"{ \"customerId\": \"12345678\", \"deviceId\" : \"TTH12S\", \"uuid\": \"ABCDEF0123456789\", \"major\": \"1000\", \"minor\": \"3\", \"signalPower\": \"-30\" }",
			"{ \"customerId\": \"12345678\", \"deviceId\" : \"TTH12S\", \"uuid\": \"ABCDEF0123456789\", \"major\": \"1000\", \"minor\": \"99\", \"signalPower\": \"-30\" }", };

	public DrawBeaconImages() {
		super(new GridLayout(1, 0));
	}

	private void createBackground() {
		bgFrame = new JFrame();
		bgFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		bgFrame.setUndecorated(false);
		ImageIcon icon = createImageIcon("/images/pivotal-icon.gif", "pivotal icon");
		bgFrame.setIconImage(icon.getImage());
		bgFrame.setResizable(false);
		bgBackgrounds.add(createImageIcon("/images/supermarket.jpg", "sm icon"));
		bgBackgrounds.add(createImageIcon("/images/supermarket-b1.jpg", "sm b1 icon"));
		bgBackgrounds.add(createImageIcon("/images/supermarket-b2.jpg", "sm b2 icon"));
		bgBackgrounds.add(createImageIcon("/images/supermarket-b3.jpg", "sm b3 icon"));
		bgBackgrounds.add(createImageIcon("/images/supermarket-b4.jpg", "sm b4 icon"));
		bgBackgrounds.add(createImageIcon("/images/supermarket-b5.jpg", "sm b5 icon"));

		bgPanel = new JPanel();
		bgImageLabel = new JLabel(bgBackgrounds.get(screenNumber));
		bgPanel.add(bgImageLabel);
		bgPanel.setOpaque(true);
		bgPanel.setVisible(true);

		bgButton = new JButton("Start");
		bgButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				killTimer.getAndSet(true);
				screenNumber++;
				if (screenNumber == bgBackgrounds.size())
					screenNumber = 0;
				JLabel tempLabel = new JLabel(bgBackgrounds.get(screenNumber));
				bgPanel.remove(bgImageLabel);
				bgImageLabel = tempLabel;
				bgPanel.add(bgImageLabel, 0);
				if (screenNumber == 0) {
					bgButton.setText("Start");
				} else {
					bgButton.setText("Next");
				}
				bgPanel.revalidate();
				bgPanel.repaint();
				if (screenNumber == 0) {
					mainFrame.setVisible(false);
					mainFrame.revalidate();
					mainFrame.repaint();
				} else {
					mainFrame.setVisible(true);
					mainFrame.revalidate();
					mainFrame.repaint();
					mainFrame.toFront();
					clientWebSocketHandler.getInboundMessageQueue()
							.add("This is a test for beacon: " + String.valueOf(screenNumber));
				}
				if (screenNumber > 0)
					timer = startBeacon(createBeaconRequest(screenNumber));
			}
		});

		bgPanel.add(bgButton, Component.RIGHT_ALIGNMENT);
		bgFrame.add(bgPanel);
		bgFrame.pack();
		bgFrame.setVisible(true);
	}

	private String createBeaconRequest(int beacon) {
		return beaconRequests[beacon - 1];
	}

	private void createPanel() {
		mainFrame = new JFrame("Mobile Device");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setUndecorated(true);
		ImageIcon icon = createImageIcon("/images/pivotal-icon.gif", "pivotal icon");
		mainFrame.setIconImage(icon.getImage());
		mainFrame.setPreferredSize(new Dimension(320, 500));
		mainFrame.setResizable(false);

		mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBackground(Color.BLACK);
		mainPanel.setOpaque(true);
		mainImagePanel = new JPanel(new BorderLayout());
		mainTextPanel = new JPanel(new BorderLayout());

		icons.add(new IconText(createImageIcon("/images/306.gif", "beacon icon"), "No activity"));
		icons.add(new IconText(createImageIcon("/images/302.gif", "beacon1 icon"), "Beacon 1"));
		icons.add(new IconText(createImageIcon("/images/303.gif", "beacon2 icon"), "Beacon 2"));
		icons.add(new IconText(createImageIcon("/images/304.gif", "beacon3 icon"), "Beacon 3"));

		mainImageLabel = new JLabel(icons.get(0).icon);
		mainImageLabel.setVisible(true);
		mainImagePanel.add(mainImageLabel, BorderLayout.EAST);

		mainBeaconImageLabel = new JLabel(createImageIcon("/images/602.png", "BLE icon"));
		mainBeaconImageLabel.setVisible(false);
		mainImagePanel.add(mainBeaconImageLabel, BorderLayout.WEST);

		mainTextArea = new JTextArea(10, 50);
		mainTextArea.setEditable(false);
		mainTextArea.setFont(new Font("Serif", Font.BOLD, 18));
		mainTextArea.setLineWrap(true);
		mainTextArea.setWrapStyleWord(true);
		mainTextArea.setVisible(true);

		JScrollPane scroll = new JScrollPane(mainTextArea);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setVisible(true);
		mainTextPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		mainTextPanel.add(scroll);

		mainPanel.add(Box.createRigidArea(new Dimension(0, 10)), BorderLayout.NORTH);
		mainPanel.add(Box.createRigidArea(new Dimension(10, 0)), BorderLayout.WEST);
		mainPanel.add(Box.createRigidArea(new Dimension(10, 0)), BorderLayout.EAST);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 10)), BorderLayout.SOUTH);
		mainPanel.add(mainImagePanel, BorderLayout.AFTER_LAST_LINE);
		mainPanel.add(mainTextPanel, BorderLayout.CENTER);

		mainFrame.add(mainPanel);
		mainFrame.pack();
		mainFrame.setVisible(false);
	}

	void addCompForTitledBorder(TitledBorder border, String description, int justification, int position,
			Container container) {
		border.setTitleJustification(justification);
		border.setTitlePosition(position);
		addCompForBorder(border, description, container);
	}

	void addCompForBorder(Border border, String description, Container container) {
		JPanel comp = new JPanel(new GridLayout(5, 5), false);
		JLabel label = new JLabel(description, JLabel.CENTER);
		comp.add(label);
		comp.setBorder(border);

		container.add(Box.createRigidArea(new Dimension(0, 20)));
		container.add(comp);
	}

	protected static ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = DrawBeaconImages.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	private static void createAndShowGUI() throws InterruptedException, URISyntaxException {
		// transports = new ArrayList<>();
		// transports.add(new WebSocketTransport(new StandardWebSocketClient()));
		// transports.add(new RestTemplateXhrTransport());
		// sockJsClient = new SockJsClient(transports);
		clientWebSocketHandler = new ClientWebSocketHandler();
		// URI uri = new URI("ws://192.168.1.8:9292/websocket");
		// ListenableFuture<WebSocketSession> future =
		// sockJsClient.doHandshake(clientWebSocketHandler, null, uri);
		// future.addCallback(new MyListenableFutureCallback());
		DrawBeaconImages db = new DrawBeaconImages();
		db.createBackground();
		db.createPanel();
	}

	public static Timer startBeacon(String beaconRequest) {
		Timer timer = new Timer("BeaconTimer");
		long delay = 1000L;
		mainTextArea.setText("");
		timer.schedule(new TimerTask() {
			public void run() {
				if (killTimer.get())
					killTimer.getAndSet(false);
				int count = 0;
				String response = null;
				// try {
				// session.sendMessage(new TextMessage(beaconRequest));
				// } catch (IOException e1) {
				// // TODO Auto-generated catch block
				// e1.printStackTrace();
				// }
				do {
					for (IconText icon : icons) {
						JLabel tempLabel = new JLabel(icon.getIcon());
						mainImagePanel.remove(mainImageLabel);
						mainImageLabel = tempLabel;
						mainImagePanel.add(mainImageLabel, BorderLayout.EAST);
						if (count == 0 || count == icons.size()) {
							mainBeaconImageLabel.setVisible(false);
							if (count == icons.size()) {
								count = 0;
							} else {
								count++;
							}
						} else {
							mainBeaconImageLabel.setVisible(true);
							count++;
						}
						mainFrame.revalidate();
						mainFrame.repaint();
						try {
							Thread.sleep(1000L);
							if (count == 1) {
								response = clientWebSocketHandler.getInboundMessageQueue().poll();
								if (response != null) {
									mainTextArea.setText(response);
								}
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
						}
						if (killTimer.get())
							break;
					}
					try {
						if (!killTimer.get()) {
							Thread.sleep(500L);
							count = 0;
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
					}
				} while (!killTimer.get());
				killTimer.getAndSet(false);
			}
		}, delay);
		return timer;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					createAndShowGUI();
				} catch (InterruptedException | URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	public class IconText {
		private ImageIcon icon;
		private String text;

		public IconText(ImageIcon icon, String text) {
			super();
			this.icon = icon;
			this.text = text;
		}

		public ImageIcon getIcon() {
			return icon;
		}

		public String getText() {
			return text;
		}
	}

	private static class MyListenableFutureCallback implements ListenableFutureCallback<WebSocketSession> {

		@Override
		public void onSuccess(WebSocketSession arg0) {
			session = arg0;
			DrawBeaconImages db = new DrawBeaconImages();
			db.createBackground();
			db.createPanel();
		}

		@Override
		public void onFailure(Throwable arg0) {
			logger.error("Unable to connect to websocket- Exception: " + arg0.getMessage(), arg0);
		}
	}
}
