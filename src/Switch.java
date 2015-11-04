import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;


public class Switch {
	
	ServerSocket listener;	// listens for and accepts new clients
	
	ArrayList<Integer> receivePorts;
	ArrayList<Integer> sendPorts;
	ArrayList<Boolean> terminated;
	
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
		terminated = new ArrayList<Boolean>();
		
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
				
				while(termflag)
				{
					client = null;
					try {
						listener.setSoTimeout(5000);
						client = listener.accept();	//accept connection
						
						receiveData(startingPort + i);	//set up new receiver for reassigned connection
						
						writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));	//get socket outputStream
						
						//serverList.add(new ServerSocket(startingPort + i));	//open new connection to which the client will be reassigned
						
						writer.write(Integer.toString(startingPort + i) + "\n");	//reassign the client
						writer.write(Integer.toString(startingPort + i + 256));
						
						receivePorts.add(startingPort + i);
						sendPorts.add(startingPort + i + 256);
						terminated.add(false);
						
						writer.close();
						client.close();
						
						i++;
						
					} catch (SocketTimeoutException e) {
						
					} catch (IOException e) {
						System.err.println("Failed to establish reassigned connection to client");
						e.printStackTrace();
					}
				}
				System.out.println("Switch listener is returning");
				return;
			}
		};
		a.start();

		sendData();
		try {
			a.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
							System.out.println("Switch buffer is not empty");
							
							//search buffer for priority, if found pop it instead
							
							f = buffer.pop();
							if(!switchTable.containsKey(f.getDA()))
							{
								// Flood to all clients except the source
								System.out.println("Switch is flooding frame '" + f.toString() + "'");
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
								System.out.println("Switch is sending frame '" + f.toString() + "' to port " + sendPorts.get(switchTable.get(f.getDA())));
								s = new Socket((String)null, sendPorts.get(switchTable.get(f.getDA())));
								writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
								writer.write(f.toBinFrame());
								
								writer.close();
								s.close();
							}
						}
						sleep(500);
					}
					Frame term = new Frame();
					for(Integer k : sendPorts)
					{
						s = new Socket((String)null, k);
						writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
						writer.write(term.toBinFrame());
						
						writer.close();
						s.close();
					}
										
				} catch (IOException e) {
					System.err.println("ERROR: There is a port conflict");
					System.exit(-1);
					return;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Switch send is returning");
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
					ServerSocket listen = new ServerSocket(recPort);
					Socket c = null;
					BufferedReader reader = null;
					boolean term = false;
					while(termflag)
					{
						listen.setSoTimeout(5000);
						c = listen.accept();
						reader = new BufferedReader(new InputStreamReader(c.getInputStream()));
							
						if(reader != null)
						{
							while(!reader.ready())
							{
							//	System.out.println("Switch waiting for reader to be ready");
								
								sleep(500);
							}
							while(reader.ready())
							{
								s = reader.readLine();
								f = new Frame(s);
								
								if(f.isTerm())
								{
									terminated.set(receivePorts.indexOf(recPort), true);
								}
								else
								{
									System.out.println("Switch received message " + f.toString());
									
									if(!switchTable.containsKey(f.getSA()))
									{
									//	System.out.println("Switch key: " + f.getSA() + " value: " + receivePorts.indexOf(recPort));
										switchTable.put(f.getSA(), receivePorts.indexOf(recPort));
									}
									
									System.out.println("Adding '" + f.toString() + "' to buffer");
									buffer.addLast(f);
								}
							}
						}
						reader.close();
						c.close();
						reader = null;
						c = null;
						
						term = true;
						for(Boolean b : terminated)
						{
							if(!b)
							{
								term = false;
							}
						}
						if(term)
						{
							termflag = false;
						}
					}
					listen.close();
					
				} catch (SocketTimeoutException e) {
					
				} catch (IOException e) {
					System.err.println("ERROR: There is a port conflict");
					System.exit(-1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Switch receive is returning");

				return;
			}
		};
		rec.start();
		return;
	}
}


