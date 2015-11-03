import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.ArrayList;

/**
 * Contains flags and functions to control server and client sockets
 * @author Lucas Stuyvesant, Joshua Garcia, Nizal Alshammry
 */
public class Node {
	
	private Integer address;
	private boolean termFlag;
	private ArrayList<Integer> unAcked;	//list of unacknowledged destination addresses
	private ArrayList<Frame> dataIn;	//data read from socket
	private ArrayList<Frame> dataOut;	//data written to socket
	private Integer sendPort;
	private Integer receivePort;

	public Node(){
		this(0,null,null);
	}
	
	
	public Node(Integer i, ArrayList<Frame> di, ArrayList<Frame> dt){
		
		address = i;
		termFlag = true;
		unAcked = new ArrayList<Integer>();
		dataIn = di;
		dataOut = dt;
		sendPort = 0;
		receivePort = 0;
		
		BufferedReader in = null;
		String s = null;
		Frame f;
		
		try {
			in = new BufferedReader(new FileReader("node"+i+".txt"));
			
			while((s = in.readLine()) != null)
			{
				f = new Frame(address,s);
				dataOut.add(f);
			}
		} catch (IOException e) {
			System.err.println("Unable to read from file" + e.getMessage());
			e.printStackTrace();
		}
			
		Thread chatter = new Thread() {
			public void run() {
				
				BufferedReader rdr = null;
				Socket sock = null;
				
				try {
					sock = new Socket((String)null, 49152);	//connect to switch
				} catch (UnknownHostException e) {
					System.err.println("Host port does not exist");
					e.printStackTrace();
				} catch (IOException e) {
					System.err.println("Could not connect to host port");
					e.printStackTrace();
				}
				
				try {
					rdr = new BufferedReader(new InputStreamReader(sock.getInputStream()));	//get reader from socket
					while(!rdr.ready())	//read new port number from socket
					{
						System.out.println("Node " + address + " is sleeping, waiting to be reassigned");

						Thread.sleep(500);
					}
					
					sendPort = Integer.valueOf(rdr.readLine());
					
					while(!rdr.ready())	//read new port number from socket
					{
						System.out.println("Node " + address + " is sleeping, waiting to be reassigned");

						Thread.sleep(500);
					}
					receivePort = Integer.valueOf(rdr.readLine());
					
					System.out.println("Node " + address + " recPort: " + receivePort + " sendPort: " + sendPort);
					
					rdr.close();
					sock.close();	//close old socket
										
				} catch (IOException e) {
					// TODO Auto-generated catch block stub
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
					
				sendData();
				recieveData();
			}
		};
		chatter.start();
	}
	
	/**
	 * prints data received from socket
	 */
	public void printData(){
		FileWriter w = null;
		String addr = String.valueOf(address);
		
		try {
			w = new FileWriter("node" + addr + "output.txt");
			
			for(Frame s: dataIn){
				w.append(s.getSA() + ":" + s.getData());
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("Could not create file node" + addr + ".txt");
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
 
	/**
	 * Sends data from dataOut to the socket that accepts it when made
	 */
	public void sendData(){
		Thread t = new Thread() {
			public void run() {
				try {
					while(sendPort == 0)
					{
						System.out.println("Waiting for sendPort to change in " + address);
						sleep(500);
					}
					Socket sock = null;
					BufferedWriter writer = null;
					while(termFlag) {
						sock = new Socket((String) null, sendPort);
						
						if(dataOut.isEmpty()){	//if there is no data to write to socket
							System.out.println("Node " + address + " is sleeping because dataOut is empty");
							sleep(500);
						}
						else {
							writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));	//get socket outputStream

							for(int i = 0; i < dataOut.size(); i++)
							{
								Frame s = dataOut.get(i);
								if((!unAcked.contains(s.getDA())) || s.isAck()) {
									System.out.println("Node " + address + " is sending " + s.toString());
									
									writer.write(s.toBinFrame());
									writer.newLine();
									unAcked.add(s.getDA());
									dataOut.remove(i);
								}
							}
							writer.close();
						}
						sock.close();
					}
					
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("ERROR: There is a port conflict with Node " + address + " while writing");
					System.exit(-1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return;
			}
		};
		t.start();
	}
	
	/**
	 * Reads data from the socket when the "server" accepts a client
	 * The data is then printed
	 */
	public void recieveData(){
		Thread t = new Thread() {
			public void run() {
				try {
					while(receivePort == 0)
					{
						System.out.println("Waiting for receivePort to change in " + address);
						sleep(500);
					}
					
					String s = null;
					Socket client = null;
					ServerSocket ss = new ServerSocket(receivePort);
					BufferedReader reader = null;
					Frame f = null;
					while(termFlag) {
						client = ss.accept();
						reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
						while(!reader.ready()){	//sleep, periodically checking for data
							System.out.println("Node " + address + " is sleeping in receive");
							Thread.sleep(500);
						}
						
						s = reader.readLine();
						
						if(s != null){

							f = new Frame(s);
							System.out.println("Node " + address + " has received a not-null string " + f.toString());

							if(f.isAck()) {
								unAcked.remove(f.getSA());
							}
							else if(f.toString().equals("terminate")){	//if found terminate
								termFlag = false;
							}
							else
							{
								dataOut.add(new Frame(address,f.getSA()));
							}
			
							dataIn.add(f);
						}
						else
						{
							System.out.println("string read from client is null in Node " + address);
						}
						
						reader.close();	//close reader
						client.close();
					}
					
					ss.close();
					
					printData();
					
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("ERROR: There is a port conflict with Node " + address + " while reading");
					System.exit(-1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			return;
			}
		};
		t.start();
	}
}
	
