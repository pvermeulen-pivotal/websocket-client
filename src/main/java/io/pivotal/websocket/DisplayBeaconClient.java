package io.pivotal.websocket;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

import java.awt.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;

import io.pivotal.gemfire.domain.BeaconResponse;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.Box;

@SpringBootApplication
public class DisplayBeaconClient extends JPanel {
	private static final long serialVersionUID = -6569896959119825687L;
	private static final Logger logger = LoggerFactory.getLogger(DisplayBeaconClient.class);

	private AtomicBoolean noPopUp = new AtomicBoolean(false);
	private AtomicInteger popScreenBeacon = new AtomicInteger(0);
	
	private int screenNumber = 0;
	private int customerNumber;

	private List<ImageIcon> bgBackgrounds = new ArrayList<>();
	private List<IconText> icons = new ArrayList<>();
	private List<Transport> transports;

	private JFrame mainFrame;
	private JFrame bgFrame;
	
	private JPanel bgPanel;
	private JPanel mainPanel;
	private JPanel mainImagePanel;
	private JPanel mainTextPanel;
	private JLabel mainImageLabel;
	private JLabel mainBeaconImageLabel;
	private JLabel bgImageLabel;
	
	private JButton bgButton;

	private SockJsClient sockJsClient;
	private static String[] customer1BeaconRequests = {
			"{ \"customerId\": \"12345678\", \"deviceId\" : \"TTH12S\", \"uuid\": \"ABCDEF0123456789\", \"major\": \"1000\", \"minor\": \"0\", \"signalPower\": \"-30\" }",
			"{ \"customerId\": \"12345678\", \"deviceId\" : \"TTH12S\", \"uuid\": \"ABCDEF0123456789\", \"major\": \"1000\", \"minor\": \"1\", \"signalPower\": \"-30\" }",
			"{ \"customerId\": \"12345678\", \"deviceId\" : \"TTH12S\", \"uuid\": \"ABCDEF0123456789\", \"major\": \"1000\", \"minor\": \"5\", \"signalPower\": \"-30\" }",
			"{ \"customerId\": \"12345678\", \"deviceId\" : \"TTH12S\", \"uuid\": \"ABCDEF0123456789\", \"major\": \"1000\", \"minor\": \"3\", \"signalPower\": \"-30\" }",
			"{ \"customerId\": \"12345678\", \"deviceId\" : \"TTH12S\", \"uuid\": \"ABCDEF0123456789\", \"major\": \"1000\", \"minor\": \"99\", \"signalPower\": \"-30\" }", };

	private static String[] customer2BeaconRequests = {
			"{ \"customerId\": \"987654321\", \"deviceId\" : \"RDD100Z\", \"uuid\": \"ABCDEF0123456789\", \"major\": \"1000\", \"minor\": \"0\", \"signalPower\": \"-30\" }",
			"{ \"customerId\": \"987654321\", \"deviceId\" : \"RDD100Z\", \"uuid\": \"ABCDEF0123456789\", \"major\": \"1000\", \"minor\": \"4\", \"signalPower\": \"-30\" }",
			"{ \"customerId\": \"987654321\", \"deviceId\" : \"RDD100Z\", \"uuid\": \"ABCDEF0123456789\", \"major\": \"1000\", \"minor\": \"2\", \"signalPower\": \"-30\" }",
			"{ \"customerId\": \"987654321\", \"deviceId\" : \"RDD100Z\", \"uuid\": \"ABCDEF0123456789\", \"major\": \"1000\", \"minor\": \"6\", \"signalPower\": \"-30\" }",
			"{ \"customerId\": \"987654321\", \"deviceId\" : \"RDD100Z\", \"uuid\": \"ABCDEF0123456789\", \"major\": \"1000\", \"minor\": \"99\", \"signalPower\": \"-30\" }", };

	private static DisplayBeaconWebSocketTransport displayBeaconWebSocketTransport;
	
	public AtomicBoolean getNoPopUp() {
		return noPopUp;
	}

	public void setNoPopUp(AtomicBoolean noPopUp) {
		this.noPopUp = noPopUp;
	}

	public AtomicInteger getPopScreenBeacon() {
		return popScreenBeacon;
	}

	public void setPopScreenBeacon(AtomicInteger popScreenBeacon) {
		this.popScreenBeacon = popScreenBeacon;
	}

	public DisplayBeaconClient() {
		super(new GridLayout(1, 0));
	}

	public void createBackground() {
		bgFrame = new JFrame();
		bgFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		bgFrame.setUndecorated(false);
		ImageIcon icon = createImageIcon("/images/pivotal-icon.gif", "pivotal icon", false);
		bgFrame.setIconImage(icon.getImage());
		bgFrame.setResizable(false);
		bgBackgrounds.add(createImageIcon("/images/supermarket.jpg", "sm icon", false));
		bgBackgrounds.add(createImageIcon("/images/supermarket-b1.jpg", "sm b1 icon", false));
		bgBackgrounds.add(createImageIcon("/images/supermarket-b2.jpg", "sm b2 icon", false));
		bgBackgrounds.add(createImageIcon("/images/supermarket-b3.jpg", "sm b3 icon", false));
		bgBackgrounds.add(createImageIcon("/images/supermarket-b4.jpg", "sm b4 icon", false));
		bgBackgrounds.add(createImageIcon("/images/supermarket-b5.jpg", "sm b5 icon", false));

		bgPanel = new JPanel();
		bgImageLabel = new JLabel(bgBackgrounds.get(screenNumber));
		bgPanel.add(bgImageLabel);
		bgPanel.setOpaque(true);
		bgPanel.setVisible(true);

		bgButton = new JButton("Start");
		bgButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				screenNumber++;
				if (screenNumber == bgBackgrounds.size())
					screenNumber = 0;
				if (screenNumber == 0) {
					mainFrame.setVisible(false);
					mainFrame.revalidate();
					mainFrame.repaint();
				} else {
					mainFrame.setVisible(true);
					mainFrame.revalidate();
					mainFrame.repaint();
					mainFrame.toFront();
				}
				JLabel tempLabel = new JLabel(bgBackgrounds.get(screenNumber));
				bgPanel.remove(bgImageLabel);
				bgImageLabel = tempLabel;
				bgPanel.add(bgImageLabel, 0);
				if (screenNumber == 0) {
					customerNumber++;
					if (customerNumber > 2)
						customerNumber = 0;
					bgButton.setText("Start");
				} else {
					bgButton.setText("Next");
				}
				bgPanel.revalidate();
				bgPanel.repaint();
				if (screenNumber > 0) {
					noPopUp.getAndSet(true);
					popScreenBeacon.set(screenNumber);
				} else {
					noPopUp.getAndSet(false);
					popScreenBeacon.set(0);
				}
			}
		});

		bgPanel.add(bgButton, Component.RIGHT_ALIGNMENT);
		bgFrame.add(bgPanel);
		bgFrame.pack();
		bgFrame.setVisible(true);
	}

	private String createBeaconRequest(int beacon) {
		if (customerNumber == 1) {
			return customer1BeaconRequests[beacon - 1];
		} else {
			return customer2BeaconRequests[beacon - 1];
		}
	}

	public void createPanel() {
		mainFrame = new JFrame("Mobile Device");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setUndecorated(false);
		ImageIcon icon = createImageIcon("/images/pivotal-icon.gif", "pivotal icon", false);
		mainFrame.setIconImage(icon.getImage());
		mainFrame.setPreferredSize(new Dimension(280, 480));
		mainFrame.setResizable(false);

		mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBackground(Color.BLACK);
		mainPanel.setOpaque(true);
		mainImagePanel = new JPanel(new BorderLayout());
		mainImagePanel.setBackground(Color.BLACK);

		icons.add(new IconText(createImageIcon("/images/306.gif", "beacon icon", true), "No activity"));
		icons.add(new IconText(createImageIcon("/images/302.gif", "beacon1 icon", true), "Beacon 1"));
		icons.add(new IconText(createImageIcon("/images/303.gif", "beacon2 icon", true), "Beacon 2"));
		icons.add(new IconText(createImageIcon("/images/304.gif", "beacon3 icon", true), "Beacon 3"));

		mainImageLabel = new JLabel(icons.get(0).icon);
		mainImageLabel.setVisible(true);
		mainImageLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		mainImagePanel.add(mainImageLabel, BorderLayout.EAST);

		mainBeaconImageLabel = new JLabel(createImageIcon("/images/602.png", "BLE icon", true));
		mainBeaconImageLabel.setVisible(false);
		mainImagePanel.add(mainBeaconImageLabel, BorderLayout.WEST);
		mainImagePanel.setBackground(Color.LIGHT_GRAY);

		mainPanel.add(Box.createRigidArea(new Dimension(0, 10)), BorderLayout.NORTH);
		mainPanel.add(Box.createRigidArea(new Dimension(10, 0)), BorderLayout.WEST);
		mainPanel.add(Box.createRigidArea(new Dimension(10, 0)), BorderLayout.EAST);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 10)), BorderLayout.SOUTH);
		mainPanel.add(mainImagePanel, BorderLayout.NORTH);

		JLabel mainIphoneImageLabel = new JLabel(createImageIcon("/images/iphone.gif", "iphone icon", false));
		mainPanel.add(mainIphoneImageLabel, BorderLayout.CENTER);

		mainFrame.add(mainPanel);

		mainFrame.pack();
		mainFrame.setVisible(false);
	}

	protected ImageIcon createImageIcon(String path, String description, boolean scale) {
		java.net.URL imgURL = DisplayBeaconClient.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	private void createAndShowGUI() throws InterruptedException, URISyntaxException {
		displayBeaconWebSocketTransport = new DisplayBeaconWebSocketTransport(new StandardWebSocketClient(),
				new DisplayBeaconClientWebSocketHandler(), this);
		transports = new ArrayList<>();
		transports.add(displayBeaconWebSocketTransport);
		transports.add(new DisplayBeaconRestTemplateXhrTransport());
		sockJsClient = new SockJsClient(transports);
		URI uri = new URI("ws://localhost:9292/websocket");
		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.add("connection", "websocket");
		headers.add("upgrade", "websocket");
		headers.add("server", uri.toString());
		sockJsClient.doHandshake(displayBeaconWebSocketTransport.getDisplayBeaconClientWebSocketHandler(), headers,
				uri);
	}

	public Timer startBeacon() {
		Timer timer = new Timer("BeaconTimer");
		long delay = 100L;
		timer.schedule(new TimerTask() {
			public void run() {
				int count = 0;
				int lastScreen = 0;
				while (1 > 0) {
					if (noPopUp.get()) {
						count = 0;
						BeaconResponse response = null;
						if (popScreenBeacon.get() != lastScreen) {
							lastScreen = popScreenBeacon.get();
							try {
								displayBeaconWebSocketTransport.getSession()
										.sendMessage(new TextMessage(createBeaconRequest(lastScreen)));
							} catch (IOException e) {
								logger.error("Error sending message to websocket server: Exception: ", e);
							}
						}
						while (noPopUp.get() && popScreenBeacon.get() != 0 && popScreenBeacon.get() == lastScreen) {
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
									count++;
								}
								if (count > 1)
									mainBeaconImageLabel.setVisible(true);
								mainFrame.revalidate();
								mainFrame.repaint();
								if (count > 2) {
									response = displayBeaconWebSocketTransport.getDisplayBeaconClientWebSocketHandler()
											.getInboundMessageQueue().poll();
									if (response != null) {
										updateTextPanel(response);
										mainFrame.revalidate();
										mainFrame.repaint();
									}
								}
								if (popScreenBeacon.get() != lastScreen)
									break;
								try {
									Thread.sleep(500L);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
								}
							}
						}
						mainTextPanel.setVisible(false);
					} else {
						mainFrame.setVisible(false);
						do {
							try {
								Thread.sleep(500L);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
							}

						} while (!noPopUp.get() && popScreenBeacon.get() == 0);

					}
				}
			}
		}, delay);
		return timer;
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

	private Image getImage(String urlStr) {
		Image image = null;
		try {
			URL url = new URL(urlStr);
			image = ImageIO.read(url);
		} catch (Exception e) {
			logger.error("Error reading image from file " + urlStr, e);
		}
		return image;
	}

	private void updateTextPanel(BeaconResponse response) {
		int len = 1;
		if (mainTextPanel != null)
			mainPanel.remove(mainTextPanel);
		mainTextPanel = new JPanel(new BorderLayout());
		JLabel label;
		JTextArea tf = new JTextArea(1, 30);

		if (response.getMessage() != null && response.getMessage().length() > 0) {
			len = response.getMessage().length();
			int mod = len % 30;
			if (mod > 0) {
				len = (len / 30) + 1;
			} else {
				len = (len / 30);
			}
			tf = new JTextArea(len, 30);
			tf.setText(response.getMessage());
		}

		if (response.getUrl() != null && response.getUrl().length() > 0) {
			BufferedImage bImage = (BufferedImage) getImage(response.getUrl());
			if (len == 1) {
				len = 60;
			} else {
				len = len * 30;
			}
			Image image = bImage.getScaledInstance(len, len, Image.SCALE_DEFAULT);
			label = new JLabel(new ImageIcon(image));
		} else {
			label = new JLabel(new ImageIcon());
		}

		tf.setEditable(false);
		tf.setFont(new Font("Serif", Font.BOLD, 16));
		tf.setLineWrap(true);
		tf.setWrapStyleWord(true);
		tf.setVisible(true);
		label.setOpaque(true);
		label.setBackground(tf.getBackground());
		mainTextPanel.add(label, BorderLayout.WEST);
		mainTextPanel.add(new Label());
		mainTextPanel.add(tf, BorderLayout.CENTER);
		mainTextPanel.setVisible(true);
		mainPanel.add(mainTextPanel, BorderLayout.SOUTH);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				DisplayBeaconClient drawBeaconImages = new DisplayBeaconClient();
				try {
					drawBeaconImages.createAndShowGUI();
				} catch (InterruptedException | URISyntaxException e) {
					logger.error("Main method exception: " + e);
				}
			}
		});
	}

}
