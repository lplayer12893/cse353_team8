import java.io.BufferedWriter;
import java.io.IOException;
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
	
}
