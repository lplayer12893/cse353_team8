import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;


public class Switch {
	
	ServerSocket listener;	// listens for and accepts new clients
	
	ArrayList<ServerSocket> serverList;	// resources devoted to accepted clients
	ArrayList<Socket> clientList;	// list of client information
	
	boolean termflag;	// used to synchronize termination of the network
	
	HashMap <Integer, Integer> switchTable;	// key is node number, value is index in clientList
	
	ArrayDeque<Frame> buffer;	// buffer of frames, FIFO

	Switch() {	
		
		// Initialize listener server socket
		try {
			listener = new ServerSocket(49152);
		} catch (IOException e) {
			System.err.println("Listener server socket failed to initialize");
			e.printStackTrace();
		}
		
		// Initialize frame buffer
		buffer = new ArrayDeque<Frame>();
		
		// Initialize switching table
		switchTable = new HashMap <Integer , Integer>();

		// Initialize termination flag
		termflag = true;
		
		/**
		 * Handles accepting connections, reassigning the connections, and closing connections on termination
		 */
		Thread a = new Thread(){	
			public void run(){
				int startingPort = 49153;
				int i = 0;
				BufferedWriter writer = null;
				Socket client;
				
				while(termflag){

					try {
						client = listener.accept();
						
						writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));	//get socket outputStream
						
						serverList.add(new ServerSocket(startingPort + i));
						
						writer.write(Integer.toString(startingPort + i));
						
						writer.close();
						client.close();
						
						clientList.add(serverList.get(serverList.size() - 1).accept());					
						
					} catch (IOException e) {
						System.err.println("Failed to establish reassigned connection to client");
						e.printStackTrace();
					}
				}
				
				for(ServerSocket ss : serverList)
				{
					try {
						ss.close();
					} catch (IOException e) {
						System.err.println("Failed to close server socket");
						e.printStackTrace();
					}
				}
				
				for(Socket s : clientList)
				{
					try {
						s.close();
					} catch (IOException e) {
						System.err.println("Failed to close server socket");
						e.printStackTrace();
					}
				}
			}
		};
		a.start();

		chat();
	}
	
	
	/**
	 * Sends data from dataOut to the socket that accepts it when made
	 */
	private void sendData(){
		
		Thread send = new Thread() {
			public void run() {
				
				try {
					BufferedWriter writer = null;
					Frame f = null;
					
					while(termflag)
					{
						if(!buffer.isEmpty())
						{
							f = buffer.pop();
							if(!switchTable.containsKey(f.getDA()))
							{
								// Flood to all clients except the source
								for(int i = 0; i < clientList.size(); i++)
								{
									if(i != switchTable.get(f.getSA()))
									{
										writer = new BufferedWriter(new OutputStreamWriter(clientList.get(i).getOutputStream()));
										writer.write(f.toBinFrame());
									}
								}
							}
							else
							{
								writer = new BufferedWriter(new OutputStreamWriter(clientList.get(switchTable.get(f.getDA())).getOutputStream()));
								writer.write(f.toBinFrame());
							}
						}
					}
					
					writer.close();	//close socket and writer
					
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("ERROR: There is a port conflict");
					return;
				}
				
				return;
			}
		};
		send.start();	//begin thread
		return;
	}
	
	/**
	 * Reads data from the socket when the "server" accepts a client
	 * The data is then printed
	 */
	private void receiveData(){
		Thread rec = new Thread() {
			public void run() {
				try {
					BufferedReader reader = null;
					String s = null;
					Frame f = null;
					
					while(termflag)
					{
						for(int i = 0; i < clientList.size(); i++)
						{
							reader = new BufferedReader(new InputStreamReader(clientList.get(i).getInputStream()));	//get reader from socket
							if((s = reader.readLine()) != null){	//if there is data to receive, buffer it
								f = new Frame(s);
								
								if(!switchTable.containsKey(f.getSA()))
								{
									switchTable.put(f.getSA(), i);
								}
								
								buffer.addLast(f);
							}
							reader.close();	//close reader 
						}
					}
					
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("ERROR: There is a port conflict");
					return;
				}
			}
		};
		rec.start();
		
		return;
	}

	private void chat() {
		sendData();
		receiveData();
	}
}


