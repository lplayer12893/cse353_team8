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
	
	//ArrayList<ServerSocket> serverList;	// resources devoted to accepted clients
	//ArrayList<Socket> clientList;	// list of client information
	//ArrayList<BufferedReader> readers;
	//ArrayList<BufferedWriter> writers;
	
	ArrayList<Integer> receivePorts;
	ArrayList<Integer> sendPorts;
	
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
		
		receivePorts = new ArrayList<Integer>();
		sendPorts = new ArrayList<Integer>();

		//serverList = new ArrayList<ServerSocket>();
		//clientList = new ArrayList<Socket>();
		//readers = new ArrayList<BufferedReader>();
		//writers = new ArrayList<BufferedWriter>();
		
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
						
						//serverList.add(new ServerSocket(startingPort + i));	//open new connection to which the client will be reassigned
						
						writer.write(Integer.toString(startingPort + i) + "\n");	//reassign the client
						writer.write(Integer.toString(startingPort + i + 256));
						
						receivePorts.add(startingPort + i);
						sendPorts.add(startingPort + i + 256);
						
						writer.close();
						client.close();
												
						//clientList.add(serverList.get(serverList.size() - 1).accept());
						//readers.add(new BufferedReader(new InputStreamReader(clientList.get(clientList.size() - 1).getInputStream())));
						//writers.add(new BufferedWriter(new OutputStreamWriter(clientList.get(clientList.size() - 1).getOutputStream())));
						
						//System.out.println("switch has reassigned a client to socket " + clientList.get(clientList.size() - 1).getPort());
						
						i++;
						
					} catch (IOException e) {
						System.err.println("Failed to establish reassigned connection to client");
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
					Socket s = null;
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
								for(int i = 0; i < sendPorts.size(); i++)
								{
									if(i != switchTable.get(f.getSA()))
									{
										s = new Socket((String)null, sendPorts.get(i));
										writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
										writer.write(f.toBinFrame());
										
										writer.close();
										s.close();
									}
								}
							}
							else
							{
								s = new Socket((String)null, sendPorts.get(switchTable.get(f.getDA())));
								writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
								writer.write(f.toBinFrame());
							}
						}
						sleep(500);
					}
										
				} catch (IOException e) {
					System.err.println("ERROR: There is a port conflict");
					System.exit(-1);
					return;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
	private void receiveData(final Integer recPort){
		Thread rec = new Thread() {
			public void run() {
				try {
					String s = null;
					Frame f = null;
					int i = 0;
					ServerSocket listen = new ServerSocket(recPort);
					Socket c = null;
					BufferedReader reader = null;
					while(termflag)
					{
						c = listen.accept();
						reader = new BufferedReader(new InputStreamReader(c.getInputStream()));
							
						if(reader != null)
						{
							while(!reader.ready())
							{
								sleep(500);
							}
								
							System.out.println("Switch reader " + i + " is ready");
							s = reader.readLine();
							f = new Frame(s);
							
							System.out.println("Switch received message " + f.toString());
							
							if(!switchTable.containsKey(f.getSA()))
							{
								switchTable.put(f.getSA(), i);
							}
							
							buffer.addLast(f);
						}
						reader.close();
						c.close();
					}
					listen.close();
					
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
		
		for(Integer i : receivePorts)
		{
			receiveData(i);
		}
		sendData();
	}
}


