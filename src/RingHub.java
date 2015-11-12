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

/**
 * Contains the implementation for a switch in a tcp/ip network
 * @author Lucas Stuyvesant, Joshua Garcia, Nizal Alshammry
 */
public class RingHub {
	
	
	private final int numNodes;
	ServerSocket Ringlistener;	// listens for and accepts new clients
	ArrayList<Integer> receivePorts;
	ArrayList<Integer> sendPorts;
	ArrayList<Integer> usedPorts;
	
	boolean termflag; // used to synchronize termination of the network
	
	int terminated;
	
	ArrayDeque<Frame> Nodesbuffer;	// buffer of frames, FIFO
	
	
	RingHub(int num) {
		
		numNodes = num;
		// Initialize Ringlistener server socket. REQUIRES PORT 65535 BE OPEN
		while(true)
		{
			try {
				Ringlistener = new ServerSocket(65534);
				break;
			} catch (IOException e) {
				System.err.println("Listener server socket failed to initialize");
				continue;
			}
		}
	
	
		receivePorts = new ArrayList<Integer>();
		sendPorts = new ArrayList<Integer>();
		usedPorts = new ArrayList<Integer>();
		terminated = 0;
		
		// Initialize termination flag
		termflag = true;
		
		Nodesbuffer = new ArrayDeque<Frame>();
		
		/**
		 * Handles accepting connections, reassigning the connections, and closing connections on termination
		 */
		Thread aa = new Thread(){	// begin listener thread, reassigns nodes to open ports
			public void run(){

				int startingPort = 49665;
				BufferedWriter writer = null;
				
				Socket c;
				
						while(termflag)
						{
							c = null;
							try {
								Ringlistener.setSoTimeout(5000);
								c = Ringlistener.accept();	//accept connection
								
								writer = new BufferedWriter(new OutputStreamWriter(c.getOutputStream()));	//get socket outputStream
								
								for(; startingPort < 65534; startingPort++)	// iterates until a free port is found
								{
									try {
										ServerSocket tmp = new ServerSocket(startingPort);
										tmp.close();
										if(usedPorts.contains(startingPort))
										{
											throw new IOException();
										}
										usedPorts.add(startingPort);
										receivePorts.add(startingPort);
										break;
									} catch (IOException e) {
										continue;
									}
								}
								
								receiveData(startingPort);	//set up new receiver for reassigned connection
		
								writer.write(Integer.toString(startingPort) + "\n");	//reassign the client
		
								startingPort++;
								for(; startingPort < 65535; startingPort ++)	//iterates until a second free port is found
								{
									try {
										ServerSocket tmp = new ServerSocket(startingPort);
										tmp.close();
										if(usedPorts.contains(startingPort))
										{
											throw new IOException();
										}
										usedPorts.add(startingPort);
										sendPorts.add(startingPort);
										break;
									} catch (IOException e) {
										continue;
									}
								}
								writer.write(Integer.toString(startingPort) + "\n");
		
								startingPort++;
								
								writer.close();
								c.close();
								
								
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
	
			aa.start();
		
			sendData();
	
			try {
				aa.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	
	
	
	/**
	 * Sends data from dataOut to the socket that accepts it when made
	 */
	private void sendData(){
		
		Thread send = new Thread() {
			public void run()  {
				try {
					Socket s = null;
					BufferedWriter writer = null;
					Frame f = null;
					
					ArrayList<Integer> badIndices = new ArrayList<Integer>();
					while(termflag)	// until time to terminate
					{
						
						badIndices.clear();
						f = null;
						
						if(!Nodesbuffer.isEmpty())	// if there is data to be sent
						{
								f = Nodesbuffer.pop();
							}
						
						
						
					}
					
					
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.exit(-1);
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
	 * @param recPort the port which the switch is receiving upon, one thread per node
	 */
	private void receiveData(final Integer recPort){
		Thread rec = new Thread() {
			public void run() {
				
				try {
					String ss = null;
					Frame ff = null;
					ServerSocket listen1 = new ServerSocket(recPort);
					Socket cc = null;
					BufferedReader reader1 = null;
					
					while(termflag)	// until time to terminate
					{
						
						try {	// wait for connection, periodically checking termination status
							listen1.setSoTimeout(10000);
							cc = listen1.accept();
						} catch (SocketTimeoutException e) {
							continue;
						}
						
						reader1 = new BufferedReader(new InputStreamReader(cc.getInputStream()));
						if(reader1 != null)
						{
							while(!reader1.ready())	//wait until reader is ready
							{
								sleep(500);
							}
							
							while(reader1.ready())	//read all data and process the data
							{
								ss = reader1.readLine();
								ff = new Frame(ss);
							
								for(int i = 0; i < sendPorts.size(); i++)
								{
									if (recPort(i)== recPort)
								
								}
								if(ff.isTerm())	// if is a termination frame, increment count of terminated nodes
								{
									terminated++;
									System.out.println("terminated = " + terminated);
									if(terminated == numNodes)
									{
										System.out.println("termflagRing is set to false");
										termflag = false;
										listen1.close();
										System.out.println("RingHub receive is returning");
										return;
									}
								}
								
							}
						}
						reader1.close();
						cc.close();
						reader1 = null;
						cc = null;

					}


				} catch (IOException e) {
					System.err.println("ERROR: There is a port conflict in switch receive, port " + recPort);
					e.printStackTrace();
					System.exit(-1);
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(-1);
				}

				return;
			}
		};
		rec.start();
		return;
	}
	
	}
	
	
	
