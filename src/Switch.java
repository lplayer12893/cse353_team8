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
	ArrayList<BufferedReader> readers;
	ArrayList<BufferedWriter> writers;
	
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
		
		serverList = new ArrayList<ServerSocket>();
		clientList = new ArrayList<Socket>();
		readers = new ArrayList<BufferedReader>();
		writers = new ArrayList<BufferedWriter>();
		
		// Initialize termination flag
		termflag = true;
		
		// Initialize switching table
		switchTable = new HashMap <Integer , Integer>();
		
		// Initialize frame buffer
		buffer = new ArrayDeque<Frame>();
		
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
						client = listener.accept();	//accept connection
						writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));	//get socket outputStream
						
						serverList.add(new ServerSocket(startingPort + i));	//open new connection to which the client will be reassigned
						
						writer.write(Integer.toString(startingPort + i));	//reassign the client
						
						writer.close();
						client.close();
												
						clientList.add(serverList.get(serverList.size() - 1).accept());
						readers.add(new BufferedReader(new InputStreamReader(clientList.get(clientList.size() - 1).getInputStream())));
						writers.add(new BufferedWriter(new OutputStreamWriter(clientList.get(clientList.size() - 1).getOutputStream())));
						
						System.out.println("switch has reassigned a client to socket " + clientList.get(clientList.size() - 1).getPort());
						
						i++;
						
					} catch (IOException e) {
						System.err.println("Failed to establish reassigned connection to client");
						e.printStackTrace();
					}
				}
				for(BufferedReader s : readers)
				{
					try {
						s.close();
					} catch (IOException e) {
						System.err.println("Failed to close server socket");
						e.printStackTrace();
					}
				}
				
				for(BufferedWriter s : writers)
				{
					try {
						s.close();
					} catch (IOException e) {
						System.err.println("Failed to close server socket");
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
										
										writers.get(i).write(f.toBinFrame());
									}
								}
							}
							else
							{
								writers.get(switchTable.get(f.getDA())).write(f.toBinFrame());
							}
						}
					}
										
				} catch (IOException e) {
					System.err.println("ERROR: There is a port conflict");
					System.exit(-1);
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
					String s = null;
					Frame f = null;
					int i = 0;
					
					while(termflag)
					{
						sleep(5000);
						System.out.println("Client list size: " + clientList.size());
						for(i = 0; i < clientList.size(); i++)
						{							
							System.out.println("Switch reading from " + clientList.get(i).getPort());
							
							if(readers.get(i) != null)
							{
								System.out.println("Switch reader " + i + " is not null");
								
								if(readers.get(i).ready()){	//if there is data to receive, buffer it
									
									System.out.println("Switch reader " + i + " is ready");
									s = readers.get(i).readLine();
									f = new Frame(s);
									
									System.out.println("Switch received message " + f.toString());
									
									if(!switchTable.containsKey(f.getSA()))
									{
										switchTable.put(f.getSA(), i);
									}
									
									buffer.addLast(f);
								}
							}
						}
					}
					
				} catch (IOException e) {
					System.err.println("ERROR: There is a port conflict");
					System.exit(-1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		rec.start();
		
		return;
	}

	private void chat() {
		receiveData();
		sendData();
	}
}


