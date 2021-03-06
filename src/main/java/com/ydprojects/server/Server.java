package com.ydprojects.server;

/**
 * 
 * @author Yasiru Dahanayake
 * 
 */

import com.ydprojects.message.Message;
import com.ydprojects.utils.MessageEncryption;
import com.ydprojects.utils.MessageUtils;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.CardLayout;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JLabel;

public class Server
{
	private static JFrame frmServer;
	private JLabel lblNewLabel;
	private static JScrollPane scrollPane, clientListScrollPane;
	private static int count = 60;
	static ServerSocket serverSocket;
	static Socket socket;
	private static final int PORT = 1234;
	private static ArrayList<ServerThread> clients;
	private static List<String> ClientNames = new ArrayList<String>();
	private static JButton btnExit, instantClose, btnStopShutdown;
	private static Timer t;
	private static JTextField getClientName;
	private JButton btnRemoveUsr;
	private JPanel panel;
	private static JTextPane msg_area2;
	private static JTextPane clientList;

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

		setUpClients();

	}

	/**
	 * sets up the the socket for client and adds the individual client to the
	 * ServerThread List
	 */
	private static void setUpClients()
	{
		try
		{
			serverSocket = new ServerSocket(PORT);
			while (true)
			{
				socket = serverSocket.accept();
				ServerThread rc = new ServerThread(socket);
				clients.add(rc);
				rc.start();
				// clients.get(clients.size() - 1).start()
				;

			}

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/*
	 * used to remove the thread (client) from the clients list
	 */
	private static void removeThread(String clientName)
	{
		
		clients.removeIf(client -> client.name.equals(clientName));

	}

	
	/**
	 * gets the socket socket output stream for all client instance's and writs
	 * the message
	 */
	private static void writeToAllClients(String message)
	{
		clients.forEach(client -> client.writeToClient(new Message(message)));
	}

	/*
	 * write a specific message to a single client
	 */
	private static void writeToAllClients(Message message) {
		clients.forEach(client -> client.writeToClient(message));
	}

	/*
	 * Compares the the name of the current thread with others if exists
	 * terminates the current thread (close client instance)
	 */
	private static boolean checkIfNameAlreadyExist(ServerThread thread, String name)
	{

		for (ServerThread client : clients)
		{
			if (client.name != null && client.name.equalsIgnoreCase(name))
			{

				JOptionPane.showMessageDialog(frmServer, "The name already exists");
				client.writeToClient(new Message(""));
				client.writeToClient(new Message(ServerCommands.TERMINATE_CLIENT));

				return false;
			}
		}
		return true;
	}

	/*
	 * Similar to close client connection, but also displays additional text on
	 * the client side.
	 */
	private static void removeUserFromServer()
	{
		for (ServerThread client : clients)
		{
			if (getClientName.getText().equalsIgnoreCase(client.name))
			{
				client.writeToClient(new Message(ServerCommands.SERVER_KICK_REQUEST));
				JOptionPane.showMessageDialog(frmServer, "User " + client.name + " Removed");
				clients.remove(client);
				displayClients();
			}
		}
	}

	/*
	 * used to display server-threads that are now running.
	 */
	private static void displayClients()
	{
		clientList.setText(null);
		clients.forEach(client -> MessageUtils.appendToPane(clientList, client.name + "\n", Color.RED));
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
	 * If the initial server shutdown has started the user would have the option
	 * to override the count-down.
	 */
	private static void immediateServerShutdown()
	{
		writeToAllClients(new Message(ServerCommands.TERMINATE_CLIENT));
		System.exit(0); // closes the server window
		if (socket != null)
		{
			try
			{
				socket.close();
			} catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
	}

	/*
	 * starts a count-down and when count reaches 0 closes the server
	 */
	private static void closeServer()
	{
		// starts the timer for the progressbar
		writeToAllClients(new Message(ServerCommands.SERVER_SHUTDOWN_REQUEST));
		btnExit.setVisible(false);
		instantClose.setVisible(true);
		btnStopShutdown.setVisible(true);

		t = new Timer(1000, new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{

				count--;

				MessageUtils.appendToPane(msg_area2, "Server shutting down in " + count + "\n", Color.LIGHT_GRAY);

				if (count == 0)
				{
					try
					{

						writeToAllClients(new Message(ServerCommands.TERMINATE_CLIENT));

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
								e1.printStackTrace();
							}
						}
					}

				}
			}

		});
		t.start();

	}

	/*
	 * Stops the server from shutting down and sends a command to reset the
	 * progress-bar and timer in client.
	 */
	private static void stopServerShutdown()
	{
		t.stop();
		count = 60;
		writeToAllClients(new Message(ServerCommands.ABORT_SHUTDOWN));
		btnExit.setVisible(true);
		instantClose.setVisible(false);
		btnStopShutdown.setVisible(false);
		MessageUtils.appendToPane(msg_area2, "Shutdown Aborted \n", Color.GRAY);
	}


	/**
	 * Create the application.
	 */
	public Server()
	{
		initialize();
	}

	/*
	 * creates the inner class server thread which is used for multi client
	 * connection/handling
	 */
	private static class ServerThread extends Thread
	{

		Socket socket;
		String name = null;
		Color nameColor;
		ObjectOutputStream objectOutputStream;


		ServerThread(Socket socket)
		{
			this.socket = socket;
		}

		public void run()
		{
			try
			{
				Message msgin = null;
				String message = "";
				String msgout = "";
				String namee = "";

				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

				namee = MessageEncryption.decrypt(((Message)ois.readObject()).getMessage());

				// connecting
				if (checkIfNameAlreadyExist(this, namee))
				{

					// if true then set he class variable = to local variable.
					name = namee;
					ClientNames.add(name);
					displayClients();

					nameColor = MessageUtils.randomColor();
					writeToAllClients(name + " has now joined the room ");
					MessageUtils.appendToPane(msg_area2, name + " is now connected.. \n", Color.LIGHT_GRAY);

					while ((msgin = (Message)ois.readObject()) != null)
					{

						System.out.println(msgin + " server checkpoint 1");

						if (msgin.getSeverCommand() == ServerCommands.TERMINATE_CLIENT)
						{
							writeToClient(new Message(ServerCommands.TERMINATE_CLIENT));
							clients.remove(this);
							displayClients();
							writeToAllClients(name + " has left the room \n");

						} else {
							message = MessageEncryption.decrypt(msgin.getMessage());
							MessageUtils.appendToPane(msg_area2, getTime() + ": ", Color.LIGHT_GRAY);
							MessageUtils.appendToPane(msg_area2, name + ": ", nameColor);
							MessageUtils.appendToPane(msg_area2, message + "\n", Color.BLACK);

							msgout = message;
							writeToAllClients((getTime() + ": " + name + ": " + msgout));
						}

					}

					MessageUtils.appendToPane(msg_area2, name + " has left the room \n", Color.LIGHT_GRAY);
					writeToAllClients(name + " has disconnected \n");
					removeThread(name);
					displayClients();

					// socket.close();

				}

			} catch (IOException | ClassNotFoundException e)
			{
				e.printStackTrace();
			}
		}

		public void writeToClient(Message message)
		{
			try
			{

				if(objectOutputStream==null) {
					objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
				}
				objectOutputStream.writeObject(message);
				objectOutputStream.flush();
				objectOutputStream.reset();

			} catch (Exception E)
			{
				E.printStackTrace();
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
		frmServer.setBounds(100, 100, 607, 378);
		frmServer.setResizable(false);
		frmServer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmServer.getContentPane().setLayout(new CardLayout(0, 0));

		panel = new JPanel();
		frmServer.getContentPane().add(panel, "name_355565354289191");
		panel.setLayout(null);

		msg_area2 = new JTextPane();
		scrollPane = new JScrollPane(msg_area2);
		scrollPane.setBounds(232, 44, 354, 167);
		panel.add(scrollPane);

		btnExit = new JButton("Start Shutdown");
		btnExit.setBounds(456, 247, 130, 29);
		panel.add(btnExit);

		getClientName = new JTextField();
		getClientName.setBounds(43, 247, 130, 26);
		panel.add(getClientName);
		getClientName.setColumns(10);

		btnRemoveUsr = new JButton("Remove usr");
		btnRemoveUsr.setBounds(53, 279, 117, 29);
		panel.add(btnRemoveUsr);

		instantClose = new JButton("Shutdown Immediatley");
		instantClose.setBounds(414, 279, 172, 29);
		instantClose.setVisible(false);
		panel.add(instantClose);

		lblNewLabel = new JLabel("Users Connected");
		lblNewLabel.setBounds(37, 21, 115, 16);
		panel.add(lblNewLabel);

		clientListScrollPane = new JScrollPane();
		clientListScrollPane.setBounds(33, 44, 140, 167);
		panel.add(clientListScrollPane);

		clientList = new JTextPane();
		clientListScrollPane.setViewportView(clientList);

		btnStopShutdown = new JButton("Stop Shutdown");
		btnStopShutdown.setBounds(456, 247, 130, 29);
		btnStopShutdown.setVisible(false);
		panel.add(btnStopShutdown);

		/*
		 * clicking on the buttons stops the shutdown sequence
		 */
		btnStopShutdown.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				stopServerShutdown();
			}
		});

		/*
		 * Immediately shuts down the server and all clients connected.
		 */
		instantClose.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				immediateServerShutdown();
			}
		});

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
					removeUserFromServer();
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

				removeUserFromServer();

			}
		});

		/*
		 * to close the server
		 */
		btnExit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{

				closeServer();

			}
		});

	}
}
