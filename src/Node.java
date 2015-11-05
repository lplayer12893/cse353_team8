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
								sleep(sleep);
							}
							else
							{
								sleep((long)Math.pow(sleep, i));
								i++;
							}
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
				}
				
				
				try {
					rdr = new BufferedReader(new InputStreamReader(sock.getInputStream()));	//get reader from socket
					while(!rdr.ready())	//read new port number from socket
					{

						Thread.sleep(500);
					}
					
					sendPort = Integer.valueOf(rdr.readLine());
					
					while(!rdr.ready())	//read new port number from socket
					{

						Thread.sleep(500);
					}
					receivePort = Integer.valueOf(rdr.readLine());
										
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
			w = new FileWriter("node" + addr + "output.txt");
			
			for(Frame s: dataIn){
				w.write(s.getSA() + ":" + s.getData() + "\n");
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
					//	System.out.println("Waiting for sendPort to change in " + address);
						sleep(500);
					}
					Socket sock = null;
					BufferedWriter writer = null;
					Frame s = null;
					boolean hasTerminated = false;
					while(termFlag) {
						
						if(dataOut.isEmpty())	//if there is no data to write to socket
						{
							if(unAcked.isEmpty())
							{
								if(!hasTerminated)
								{
									hasTerminated = true;
									sock = new Socket((String) null, sendPort);
									writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));	//get socket outputStream
									
									s = new Frame();
											
									//System.out.println("Node " + address + " is sending termination");
									System.out.println(address);
	
									writer.write(s.toBinFrame());
									writer.newLine();
									
									writer.close();
									sock.close();
								}
							}
							sleep(500);
						}
						else
						{
							if(canSend())
							{
								sock = new Socket((String) null, sendPort);
								writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));	//get socket outputStream
								int size = dataOut.size();
								int i = 0;
								int prtyCount = 0;
								
								for(Frame prty : dataOut)
								{
									if(prty.isPrioritized())
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
									
									if(prtyCount > 0)
									{
										if(s.isPrioritized())
										{
											prtyCount--;
											if((!unAcked.contains(s.getDA())) || s.isAck()) {
																								
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
										if((!unAcked.contains(s.getDA())) || s.isAck()) {
																						
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
							else
							{
							//	System.out.println("Node " + address + " is sleeping until data can be sent");
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
					//	System.out.println("Waiting for receivePort to change in " + address);
						sleep(500);
					}
					
					String s = null;
					Socket client = null;
					
					ServerSocket ss;
					while(true)
					{
						try {
							ss = new ServerSocket(receivePort);
							System.out.println("Node " + address + " can receive");
							break;
						} catch(BindException e) {
							sleep(500);
							continue;
						}
					}
					
					BufferedReader reader = null;
					Frame f = null;
					while(termFlag) {
						client = ss.accept();
						reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
						while(!reader.ready()){	//sleep, periodically checking for data
						//	System.out.println("Node " + address + " is sleeping in receive");
							Thread.sleep(500);
						}
						
						s = reader.readLine();
						
						if(s != null){

							f = new Frame(s);

							if(f.isTerm()){	//if found terminate
								termFlag = false;
								dataIn.add(f);
							}
							else if(f.isAck()) {
								for(int i = 0; i < unAcked.size(); i++)
								{
									if(unAcked.get(i) == f.getSA())
									{
										unAcked.remove(i);
									}
								}
								dataIn.add(f);
							}
							
							else if(address == f.getDA())
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
										
					ss.close();

				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("ERROR: There is a port conflict with Node " + address + " while reading");
					System.exit(-1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			
				System.out.println("Node receive is returning");

				printData();
				return;
			}
		};
		t.start();
		return;
	}
}
	
