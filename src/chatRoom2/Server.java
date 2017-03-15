package chatRoom2;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.CardLayout;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * 
 * @author Yasiru Dahanayake
 * 
 */
public class Server
{

	private static JFrame frmServer;
	private static JScrollPane scrollPane;
	private static int count = 60;
	static ServerSocket serverSocket;
	static Socket socket;
	private static final int PORT = 1234;
	private static ArrayList<ServerThread> clients;
	private JButton btnExit;
	private static Timer t;
	private static JTextField getClientName;
	private JButton btnRemoveUsr;
	private JPanel panel;
	private static JTextPane msg_area2;

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
					Server window = new Server();
					window.frmServer.setVisible(true);
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}

		});
		clients = new ArrayList<ServerThread>();

		SetUpClients();

	}

	/**
	 * sets up the the socket for client and adds the individual client to the
	 * ServerThread List
	 */
	private static void SetUpClients()
	{
		try
		{
			serverSocket = new ServerSocket(PORT);
			while (true)
			{
				socket = serverSocket.accept();
				ServerThread rc = new ServerThread(socket);
				clients.add(rc);
				clients.get(clients.size() - 1).start();

			}

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * sets up the socket connection to client.
	 */
	private static void removeName(String namee)
	{
		for (int i = 0; i < clients.size(); i++)
		{
			if (namee.equals(clients.get(i).name))
			{
				clients.get(i).name = null;
			}
		}

	}

	/**
	 * gets the socket socket output stream for all client instance's and writs
	 * the message
	 */
	private static void WriteToAllClients(String message)
	{
		for (int i = 0; i < clients.size(); i++)
		{

			PrintWriter writer;
			try
			{
				writer = new PrintWriter(clients.get(i).socket.getOutputStream(), true);
				writer.println(MessageEncryption.encrypt(message));
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	/*
	 * Compares the the name of the current thread with others if exists
	 * terminates the current thread (close client instance)
	 */
	private static boolean CheckName2(ServerThread thread, String name)
	{

		for (int i = 0; i < clients.size(); i++)
		{
			if (clients.get(i).name != null && clients.get(i).name.equalsIgnoreCase(name))
			{
				JOptionPane.showMessageDialog(frmServer, "The name already exists");
				WriteToclient(ServerCommands.getTerminateclient(), thread.socket);

				return false;

			}
		}
		return true;
	}

	/*
	 * Similar to close client connection, but also displays additional text on
	 * the client side.
	 */
	private static void RemoveUserFromServer()
	{
		for (int i = 0; i < clients.size(); i++)
		{
			if (getClientName.getText().equalsIgnoreCase(clients.get(i).name))
			{
				WriteToclient(ServerCommands.getServerkickrequest(), clients.get(i).socket);
				JOptionPane.showMessageDialog(frmServer, "User " + clients.get(i).name + " Removed");
			}
		}
	}

	/*
	 * write a specific message to a single client
	 */
	private static void WriteToclient(String message, Socket sockett)
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

	/*
	 * gets the time to be used as a time stamp
	 */
	private static String getTime()
	{
		String timeStamp = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());

		return timeStamp;
	}

	/*
	 * starts a count-down and when count reaches 0 closes the server
	 */
	private static void CloseServer()
	{
		// starts the timer for the progressbar
		WriteToAllClients(ServerCommands.getServershutdownrequest());
		t = new Timer(1000, new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				// TODO Auto-generated method stub

				count--;

				MessageUtils.appendToPane(msg_area2, "Server shutting down in " + count + "\n", Color.LIGHT_GRAY);
				if (count == 0)
				{
					try
					{

						for (int i = 0; i < clients.size(); i++)
						{

							WriteToclient(ServerCommands.getTerminateclient(), clients.get(i).socket);

						}
						// socket.close(); // closes the socket
						System.exit(0); // closes the server window
					} finally
					{
						if (socket != null)
						{
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

				}
			}

		});
		t.start();

	}

	/**
	 * Create the application.
	 */
	public Server()
	{
		initialize();
	}

	private static class ServerThread extends Thread
	{

		Socket socket;
		String name = null;
		Color nameColor;

		ServerThread(Socket socket)
		{
			this.socket = socket;
		}

		public void run()
		{
			try
			{
				String msgin = null;
				String msgout = null;
				String namee = null;
				String connectionrequest = null;

				// CheckName(name);
				BufferedReader fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				namee = fromClient.readLine();

				connectionrequest = fromClient.readLine();

				// make sure the client is sending a connection request before
				// connecting
				if (CheckName2(this, namee) && connectionrequest.equals(ClientRequests.getClientconnectionrequest()))
				{
					// if true then set he class variable = to local variable.
					name = namee;
					SqlConnection.AddPeople(namee);
					
					nameColor = MessageUtils.randomColor();
					// WriteToAllClients(nameColor.toString());
					WriteToAllClients(name + " has now joined the room ");
					MessageUtils.appendToPane(msg_area2, name + " is now connected.. \n", Color.LIGHT_GRAY);

					while ((msgin = fromClient.readLine()) != null)
					{

						if (msgin.equals(ClientRequests.getClientconnectionrequest()))
						{

						} else if (msgin
								.equalsIgnoreCase((ClientRequests.getClientexitrequest()).replaceAll("\\s+", "")))
						{

							WriteToclient(ServerCommands.getTerminateclient(), this.socket);

						} else
						{

							MessageUtils.appendToPane(msg_area2, getTime() + ": ", Color.LIGHT_GRAY);
							MessageUtils.appendToPane(msg_area2, name + ": ", nameColor);
							MessageUtils.appendToPane(msg_area2, msgin + "\n", Color.BLACK);

							// doc.insertString(0, (getTime() + ": " + name + ":
							// " + msgin + "\n"), null);
							msgout = msgin;
							WriteToAllClients((getTime() + ": " + name + ": " + msgout));
						}

					}

					MessageUtils.appendToPane(msg_area2, name + " has left the room \n", Color.LIGHT_GRAY);
					WriteToAllClients(name + " has disconnected \n");
					removeName(name);

					// socket.close();

				}

			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize()
	{
		frmServer = new JFrame();
		frmServer.setTitle("SERVER");
		frmServer.setBounds(100, 100, 450, 300);
		frmServer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmServer.getContentPane().setLayout(new CardLayout(0, 0));

		panel = new JPanel();
		frmServer.getContentPane().add(panel, "name_355565354289191");
		panel.setLayout(null);

		msg_area2 = new JTextPane();
		scrollPane = new JScrollPane(msg_area2);
		scrollPane.setBounds(57, 20, 354, 167);
		panel.add(scrollPane);

		btnExit = new JButton("Exit Server");
		btnExit.setBounds(307, 202, 117, 29);
		panel.add(btnExit);

		getClientName = new JTextField();
		getClientName.setBounds(40, 202, 130, 26);
		panel.add(getClientName);
		getClientName.setColumns(10);

		btnRemoveUsr = new JButton("Remove usr");
		btnRemoveUsr.setBounds(162, 202, 117, 29);
		panel.add(btnRemoveUsr);

		/*
		 * clicking enter on the text-field to remove client
		 */
		getClientName.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{

					RemoveUserFromServer();
				}
			}
		});

		/*
		 * clicking on the button to remove client
		 */
		btnRemoveUsr.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{

				RemoveUserFromServer();

			}
		});

		/*
		 * to close the server
		 */
		btnExit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{

				CloseServer();

			}
		});

	}
}
