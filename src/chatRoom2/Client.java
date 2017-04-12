package chatRoom2;

/**
 * @author Yasiru Dahanayake
 */

import java.awt.EventQueue;
import javax.swing.JFrame;
import java.awt.CardLayout;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JProgressBar;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client
{

	private static JFrame frmClient;
	private static JTextField msg_text;
	private static JTextPane msg_area;
	private JButton nameSubmit, btnExit;
	private static JButton msg_send;
	private static String name;
	private static Socket socket;
	private static JPanel getNameScreen, clientScreen;
	private static JTextField nameInput;
	private JLabel lblEnterYourName;
	private static JLabel countDownLabel;
	private JScrollPane scrollPane;
	private static JProgressBar progressBar;
	private static int count = 60;
	private static Timer t;
	long lastClicked = System.currentTimeMillis();
	final long threshold = 500; // 500msec = half second
	private static Pattern p = Pattern.compile("[^A-Za-z0-9]");

	/**
	 * Launch the application.
	 */
	public static void main(String[] args)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					Client window = new Client();
					// closeFrame();
					window.frmClient.setVisible(true);
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});

		try
		{
			socket = new Socket("127.0.0.1", 1234);
			ReadFromServer(socket);

		} catch (Exception e)
		{
			e.printStackTrace();

			JOptionPane.showMessageDialog(frmClient, "Cannot establish connection to server, cliet will now exit ");
			System.exit(0);
			try
			{
				socket.close();
			} catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}

	}

	/*
	 * displays the progress-bar, which ticks down according to the timer does
	 * specific functions once the count get to a certain number;
	 */
	private static void StartProgressBar()
	{
		progressBar.setVisible(true);
		MessageUtils.appendToPane(msg_area, "Server has initiates a shutdown \n", Color.LIGHT_GRAY);
		MessageUtils.appendToPane(msg_area, "you will be logged out in "+ count + " seconds \n", Color.LIGHT_GRAY);
		t = new Timer(1000, new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				// TODO Auto-generated method stub

				count--;
				// decrease the value of the progress bar as the as the
				// count is decreasing
				progressBar.setValue((int) (count * (1.6)));
				
				if (count%10 == 0 && count >10)
				{
					MessageUtils.appendToPane(msg_area, "Logging out in " + count + "\n", Color.LIGHT_GRAY);
				} else if (count <= 10)
				{
					MessageUtils.appendToPane(msg_area, "Logging out in " + count + "\n", Color.LIGHT_GRAY);
				}

			}

		});
		t.start();
	}

	/*
	 * Takes the name and validates it, according to set validation
	 */
	private static void GetName()
	{
		name = nameInput.getText().replaceAll("\\s+", "");
		Matcher m = p.matcher(name);

		if ((name.isEmpty() == false) && (m.find() == false))
		{
			writemessage(name, socket);
			frmClient.setTitle(name);
			getNameScreen.setVisible(false);
			clientScreen.setVisible(true);
			writemessage(ClientRequestsCommands.getClientconnectionrequest(), socket);
		} else
		{
			JOptionPane.showMessageDialog(frmClient, "please enter a valid name");
		}
	}

	/*
	 * reads from the socket Input stream carries out tasks depending on what is
	 * send by the server
	 */
	private static void ReadFromServer(Socket sockett) throws IOException
	{
		String message = null;

		BufferedReader fromServer = new BufferedReader(new InputStreamReader(sockett.getInputStream()));
		// String nameTagColourname = fromServer.readLine().replaceAll("\\s+",
		// "");
		// Color nameTagColor = MessageUtils.checkcolor(nameTagColourname);
		// System.out.println(nameTagColourname);

		// if there is something in the stream de-crypt it.
		String UsrJoin = fromServer.readLine();
		MessageUtils.appendToPane(msg_area, MessageEncryption.decrypt(UsrJoin) + "\n", Color.gray);
		
			while ((message = MessageEncryption.decrypt(fromServer.readLine())) != null)
			// while ((message = fromServer.readLine()) != null)
			{

				// System.out.println(message);

				if (message.equals((ServerCommands.getTerminateclient())))
				{
					socket.getOutputStream().flush();
					socket.close();
					System.exit(0);

				} else if (message.equals((ServerCommands.getServershutdownrequest())))
				{
					StartProgressBar();

				} else if (message.equals((ServerCommands.getServerkickrequest())))
				{

					JOptionPane.showMessageDialog(frmClient, "you have been removed from the server");
					socket.getOutputStream().flush();
					socket.close();
					System.exit(0);

				} else if (message.equals(ServerCommands.getCloseinstantly()))
				{
					JOptionPane.showMessageDialog(frmClient, "Server has shutdown, you have been removed");
					socket.getOutputStream().flush();
					socket.close();
					System.exit(0);

				} else if (message.equals(ServerCommands.getAbortshutdown()))
				{
					MessageUtils.appendToPane(msg_area, "Server had Aborted shutdown \n", Color.LIGHT_GRAY);
					progressBar.setVisible(false);
					countDownLabel.setVisible(false);
					count = 60;
					t.stop();

				} else
				{

					// System.out.println(message);
					MessageUtils.appendToPane(msg_area, message + "\n", Color.BLACK);
				}

			}
		
	}

	/*
	 * writes a an encrypted string to server output stream
	 */
	private static void writemessage(String message, Socket sockett)
	{
		try
		{
			PrintWriter writer = new PrintWriter(sockett.getOutputStream(), true);

			writer.println(MessageEncryption.encrypt(message));

		} catch (Exception E)
		{
			E.printStackTrace();
		}

	}

	/**
	 * Create the application.
	 */
	public Client()
	{
		initialize();
	}

	/*
	 * sets limilations so the user is not able to spam messages
	 */
	private void AntiSpamWriteMessage()
	{
		long now = System.currentTimeMillis();
		if ((msg_text.getText().trim()).isEmpty())
		{

		} else if (now - lastClicked > threshold)
		{

			writemessage(msg_text.getText().trim(), socket);
			System.out.println(msg_text.getText() + " from client ");
			lastClicked = now;
			msg_text.setText(null);

		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize()
	{
		frmClient = new JFrame();
		frmClient.setBounds(100, 100, 450, 300);
		frmClient.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmClient.getContentPane().setLayout(new CardLayout(0, 0));
		frmClient.setResizable(false);
		frmClient.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		getNameScreen = new JPanel();
		frmClient.getContentPane().add(getNameScreen, "name_414462615211513");
		getNameScreen.setLayout(null);

		nameInput = new JTextField();
		nameInput.setBounds(110, 154, 130, 26);
		getNameScreen.add(nameInput);
		nameInput.setColumns(10);

		nameSubmit = new JButton("Submit");
		nameSubmit.setBounds(241, 154, 117, 29);
		getNameScreen.add(nameSubmit);
		getNameScreen.add(nameSubmit);

		lblEnterYourName = new JLabel("Enter your name below");
		lblEnterYourName.setBounds(161, 66, 155, 16);
		getNameScreen.add(lblEnterYourName);

		clientScreen = new JPanel();
		frmClient.getContentPane().add(clientScreen, "name_355561592104539");
		clientScreen.setLayout(null);

		msg_text = new JTextField();
		msg_text.setBounds(29, 193, 230, 26);
		clientScreen.add(msg_text);
		msg_text.setColumns(10);

		msg_send = new JButton("send");
		msg_send.setBounds(289, 193, 117, 29);
		clientScreen.add(msg_send);

		scrollPane = new JScrollPane();
		scrollPane.setBounds(19, 6, 401, 175);
		clientScreen.add(scrollPane);

		msg_area = new JTextPane();
		scrollPane.setViewportView(msg_area);

		btnExit = new JButton("Exit");
		btnExit.setBounds(289, 243, 117, 29);
		clientScreen.add(btnExit);

		progressBar = new JProgressBar();
		progressBar.setBounds(29, 243, 230, 20);
		progressBar.setVisible(false);
		clientScreen.add(progressBar);

		countDownLabel = new JLabel("");
		countDownLabel.setBounds(29, 231, 217, 16);
		clientScreen.add(countDownLabel);

		/*
		 * Used to collect the name from the initial screen
		 */
		nameSubmit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				GetName();
			}
		});

		/*
		 * Used to collect the name from the initial screen
		 */
		nameInput.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					GetName();
				}
			}
		});

		/*
		 * creates an exit request if the socket s open, if not close
		 */
		btnExit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					if (socket.isClosed() == false)
					{
						writemessage(ClientRequestsCommands.getClientexitrequest(), socket);
					} else
					{
						socket.close();
						System.exit(0);
					}
				} catch (IOException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
		});

		/**
		 * send message by clicking button.
		 * 
		 */
		msg_send.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				AntiSpamWriteMessage();
			}
		});

		/**
		 * send message by pressing enter on the tet field
		 */
		msg_text.addKeyListener(new KeyAdapter()
		{

			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					AntiSpamWriteMessage();
				}
			}
		});
	}
}
