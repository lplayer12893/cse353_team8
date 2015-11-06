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
import java.util.Random;

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
				f = new Frame(address,s);	//create frames from data
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
				int i = 1;
				Random r = new Random();
				long sleep = r.nextInt(100);
				
				while(true)
				{
					try {
						sock = new Socket((String)null, 65535);	//connect to switch
						break;
					} catch (UnknownHostException e) {
						System.err.println("Host port does not exist");
						e.printStackTrace();
					} catch (IOException e) {
						try {
							if(i > 2)
							{
								sleep(sleep);	//wait for socket to connect to switch
							}
							else
							{
								sleep((long)Math.pow(sleep, i));	//exponential back off
								i++;
							}
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
				}
				
				
				try {
					rdr = new BufferedReader(new InputStreamReader(sock.getInputStream()));	//get reader from socket
					while(!rdr.ready())
					{

						Thread.sleep(500);
					}
					
					sendPort = Integer.valueOf(rdr.readLine());		//read sending port
					
					while(!rdr.ready())
					{

						Thread.sleep(500);
					}
					receivePort = Integer.valueOf(rdr.readLine());		//read receiving port
										
					rdr.close();
					sock.close();	//close old socket
										
				} catch (IOException e) {
					e.printStackTrace();
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}				
				
				sendData();
				recieveData();
				return;
			}
		};
		chatter.start();
		return;
	}
	
	/**
	 * prints data received from socket
	 */
	public void printData(){
		FileWriter w = null;
		String addr = String.valueOf(address);
		
		System.out.println("Node " + address + " is trying to print");
		
		try {
			w = new FileWriter("node" + addr + "output.txt");	//create output files
			
			for(Frame s: dataIn){
				w.write(s.getSA() + ":" + s.getData() + "\n");	//print data read from socket
			}
			
			w.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("Could not create file node" + addr + ".txt");
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
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
						sleep(500);
					}
					Socket sock = null;
					BufferedWriter writer = null;
					Frame s = null;
					boolean hasTerminated = false;
					while(termFlag) {
						
						if(dataOut.isEmpty())	//if there is no data to write to socket
						{
							if(unAcked.isEmpty())	//if all acknowledgments received
							{
								if(!hasTerminated)	//check for termination frame
								{
									hasTerminated = true;
									sock = new Socket((String) null, sendPort);
									writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));	//get socket outputStream
									
									s = new Frame();
											
									System.out.println(address);
	
									writer.write(s.toBinFrame());	//send termination
									writer.newLine();
									
									writer.close();		//close writer and socket
									sock.close();
								}
							}
							sleep(500);
						}
						else
						{
							if(canSend())	//checks if socket can send data
							{
								sock = new Socket((String) null, sendPort);
								writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));	//get socket outputStream
								int size = dataOut.size();
								int i = 0;
								int prtyCount = 0;
								
								for(Frame prty : dataOut)
								{
									if(prty.isPrioritized())	//check for frame priority
									{
										prtyCount++;
									}
								}
								while(true)
								{
									if(i >= size)
									{
										break;
									}
									
									s = dataOut.get(i);
									
									if(prtyCount > 0)	//check if some frames have priority and send first
									{
										if(s.isPrioritized())
										{
											prtyCount--;
											if((!unAcked.contains(s.getDA())) || s.isAck()) {	//if priority, send the data
																								
												writer.write(s.toBinFrame());
												writer.newLine();
												
												if(!s.isAck())
												{
													unAcked.add(s.getDA());
												}
												
												dataOut.remove(i);
												i--;
												size--;
											}
										}
										i++;
									}
									else
									{
										if((!unAcked.contains(s.getDA())) || s.isAck()) {	//Send all other frames after
																						
											writer.write(s.toBinFrame());
											writer.newLine();
											
											if(!s.isAck())
											{
												unAcked.add(s.getDA());
											}
											
											dataOut.remove(i);
											i--;
											size--;
										}
										i++;
									}
								}
								
								writer.close();
								sock.close();
							}
							else	//if socket can't send, than wait until it can
							{
								sleep(500);
							}
						}
					}
					
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("ERROR: There is a port conflict with Node " + address + " while writing");
					System.exit(-1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				System.out.println("Node send is returning");

				return;
			}
			
			/*
			 * Checks if destination is available (ACK) and data can be sent
			 */
			private boolean canSend()
			{
				Frame s = null;
				for(int i = 0; i < dataOut.size(); i++)
				{
					s = dataOut.get(i);
					if((!unAcked.contains(s.getDA())) || s.isAck())
					{
						return true;
					}
				}
				
				return false;
			}
		};
		t.start();
		return;
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
						sleep(500);
					}
					
					String s = null;
					Socket client = null;
					
					ServerSocket ss;
					while(true)	//Keep trying port until a connection is established
					{
						try {
							ss = new ServerSocket(receivePort);		//Try to connect to port
							System.out.println("Node " + address + " can receive");
							break;
						} catch(BindException e) {		//If no connection is established wait
							sleep(500);
							continue;
						}
					}
					
					BufferedReader reader = null;
					Frame f = null;
					while(termFlag) {
						
						client = ss.accept();	//start a client to receive data
						reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
						
						while(!reader.ready()){	//sleep, periodically checking for data
							Thread.sleep(500);
						}
						
						s = reader.readLine();
						
						if(s != null){

							f = new Frame(s);

							if(f.isTerm()){		//if terminate frame found
								termFlag = false;
								dataIn.add(f);
							}
							else if(f.isAck()) {	//if ACK frame found
								for(int i = 0; i < unAcked.size(); i++)
								{
									if(unAcked.get(i) == f.getSA())
									{
										unAcked.remove(i);
									}
								}
								dataIn.add(f);
							}
							
							else if(address == f.getDA())	//Data frame found
							{
								dataOut.add(new Frame(address,f.getSA()));
								dataIn.add(f);
							}
						}
						else
						{
							System.err.println("string read from client is null in Node " + address);
						}
						
						reader.close();	//close reader
						client.close();
					}
										
					ss.close();	//close server socket

				} catch (IOException e) {
					System.err.println("ERROR: There is a port conflict with Node " + address + " while reading");
					e.printStackTrace();
					System.exit(-1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			
				System.out.println("Node receive is returning");

				printData();	//write output files after node finishes reading and writing
				return;
			}
		};
		t.start();
		return;
	}
}
	
