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
 * @TODO implement the check to set hubstatus and switchstatus
 */
public class Node {
	
	private boolean termFlag;
	private boolean hasToken;
	private ArrayList<Boolean> waitAck;
	private boolean switchStatus;
	private boolean hubStatus;
	private ArrayList<Integer> unAcked;	//list of unacknowledged destination addresses
	private ArrayList<Frame> dataIn;	//data read from socket
	private ArrayList<Frame> dataOut;	//data written to socket
	private ArrayList<Frame> forward;	//frames to be forwarded
	private Integer address;
	private Integer switchSendPort;
	private Integer hubSendPort;
	private Integer switchReceivePort;
	private Integer hubReceivePort;
	private boolean hasSent;

	public Node(){
		this(0,null,null);
	}
	
	
	public Node(Integer i, ArrayList<Frame> di, ArrayList<Frame> dt){

		// init fields
		termFlag = true;
		hasToken = false;
		hasSent = false;
		waitAck = new ArrayList<Boolean>();
		switchStatus = true;
		hubStatus = true;
		unAcked = new ArrayList<Integer>();
		forward = new ArrayList<Frame>();
		dataIn = di;
		dataOut = dt;
		address = i;
		switchSendPort = 0;
		hubSendPort = 0;
		switchReceivePort = 0;
		hubReceivePort = 0;
		
		BufferedReader in = null;
		String s = null;
		Frame f;
		
		try {	//read input file
			in = new BufferedReader(new FileReader("node"+i+".txt"));
			
			while((s = in.readLine()) != null)
			{
				f = new Frame(address,s);	//create frames from data
				dataOutOp(BufferOp.ADD,f,0);
			}
		} catch (IOException e) {
			System.err.println("Unable to read from file" + e.getMessage());
			e.printStackTrace();
		}
			
		Thread chatter = new Thread() {	//thread for beginning chat
			public void run() {
				
				BufferedReader rdr = null;
				Socket switchSock = null;
				Socket hubSock = null;
				int i = 1;
				Random r = new Random();	//exponential backoff
				long sleep = r.nextInt(100);
				
				while(true)	//until successful, tries to connect to switch
				{
					try {
						switchSock = new Socket((String)null, 65535);	//connect to switch
						
						rdr = new BufferedReader(new InputStreamReader(switchSock.getInputStream()));	//get reader from socket
						while(!rdr.ready())
						{
							Thread.sleep(500);
						}
						
						switchSendPort = Integer.valueOf(rdr.readLine());		//read sending port
						
						while(!rdr.ready())
						{
							Thread.sleep(500);
						}
						switchReceivePort = Integer.valueOf(rdr.readLine());		//read receiving port

						rdr.close();
						switchSock.close();	//close old socket
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
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				while(true)	//until successful, tries to connect to ringhub
				{
					try {
						hubSock = new Socket((String)null, 65534);
						
						rdr = new BufferedReader(new InputStreamReader(hubSock.getInputStream()));	//get reader from socket
						while(!rdr.ready())
						{
							Thread.sleep(500);
						}
						
						hubSendPort = Integer.valueOf(rdr.readLine());		//read sending port
						
						while(!rdr.ready())
						{
							Thread.sleep(500);
						}
						hubReceivePort = Integer.valueOf(rdr.readLine());		//read receiving port

						rdr.close();
						switchSock.close();	//close old socket
						hubSock.close();
						
						break;
					} catch (UnknownHostException e) {
						System.err.println("Host port does not exist");
						e.printStackTrace();
					} catch (IOException e) {
						try {
							if(i > 2)
							{
								sleep(sleep);	//wait for socket to connect to ringhub
							}
							else
							{
								sleep((long)Math.pow(sleep, i));	//exponential back off
								i++;
							}
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				System.out.println("Node " + address + " is starting to send and receive");
				
				sendHubData();	//begin send data to switch (threaded)
				sendSwitchData();	//send data over star network
				receiveHubData();	//begin receive data from hub (threaded)
				receiveSwitchData();	//send data over ring network
				
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
	public void sendSwitchData(){
		Thread t = new Thread() {
			public void run() {
				try {
					while(switchSendPort == 0)	//wait for port reassignment to finish
					{
						sleep(500);
					}
					
					System.out.println("Node " + address + " has been reassigned (switch)");
					
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
									sock = new Socket((String) null, switchSendPort);
									writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));	//get socket outputStream
									
									s = new Term(FrameType.STAR);
									
									System.out.println("Node " + address + " has terminated (switch)");

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
								sock = new Socket((String) null, switchSendPort);
								writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));	//get socket outputStream
								int size = dataOut.size();
								int i = 0;

								while(true)
								{
									if(i >= size)
									{
										break;
									}
									
									s = dataOutOp(BufferOp.GET,null,i);
									
									if((!unAcked.contains(s.getDA())) || s.isAck()) 	//Send all other frames after
									{
										if(s.getFrameType() == FrameType.STAR || !hubStatus)	//send only star data, or send all data if star is down
										{
											//System.out.println("Node " + address + " is (SWITCH) sending to Node " + s.getDA() + ": " + s.toString());
											writer.write(s.toBinFrame());
											writer.newLine();
											
											if(!s.isAck())
											{
												unAcked.add(s.getDA());
												waitAck.add(false);
											}
											else if(s.isAck())
											{
												dataOutOp(BufferOp.REM,null,i);	//remove acknowledgment after send
											}

											i--;
											size--;
										}			
									}
									i++;
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
					s = dataOutOp(BufferOp.GET,null,i);

					if(s.frameType == FrameType.STAR)
					{
						if((!unAcked.contains(s.getDA())) || s.isAck())
						{
							return true;
						}
					}
				}
				
				return false;
			}
		};
		t.start();
		return;
	}
	
	/**
	 * Sends data to the ring
	 */
	public void sendHubData() {
		Thread t = new Thread() {
			public void run() {
				try {
					while(hubSendPort == 0)	//wait for port reassignment to finish
					{
						sleep(500);
					}
					
					System.out.println("Node " + address + " has been reassigned (ringhub)");
					
					Socket sock = null;
					BufferedWriter writer = null;
					Frame s = null;
					boolean hasTerminated = false;
					
					while(termFlag) {
						if(forward.size() > 0)
						{
							sock = new Socket((String) null, hubSendPort);
							writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));	//get socket outputStream
							for(int j = forward.size(); j > 0; j--)		//forward all frames first
							{
								s = forward.get(j-1);
								writer.write(s.toBinFrame());
								writer.newLine();
								forward.remove(j-1);
							}
							writer.close();
							sock.close();
						}
						
						else if(dataOut.isEmpty())	//if there is no data to write to socket
						{
							if(hasToken)
							{
								forward.add(new Token());
								hasToken = false;
							}
							if(unAcked.isEmpty() && forward.isEmpty())	//if all acknowledgments received
							{
								if(!hasTerminated)	//check for termination frame
								{
									hasTerminated = true;
									sock = new Socket((String) null, hubSendPort);
									writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));	//get socket outputStream
									
									s = new Term(FrameType.RING);
									
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
							if(canSend() && hasToken && !hasSent)	//checks if socket can send data
							{
								sock = new Socket((String) null, hubSendPort);
								writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));	//get socket outputStream
								int size = dataOut.size();
								int i = 0;

								for(; i < dataOut.size(); i++)
								{
									if(i >= size)
									{
										break;
									}

									s = dataOutOp(BufferOp.GET,null,i);

									if((!unAcked.contains(s.getDA())) || s.isAck()) 
									{	
										if(s.getFrameType() == FrameType.RING || !switchStatus)	//send only ring data, or send all data if star is down
										{
											System.out.println("Node " + address + " is (RINGHUB) sending frame: " + s.toString());
											writer.write(s.toBinFrame());
											writer.newLine();
											hasSent = true;
		
											if(!s.isAck())
											{
												unAcked.add(s.getDA());
												waitAck.add(false);
											}
											else if(s.isAck())
											{
												dataOutOp(BufferOp.REM,null,i);	//remove acknowledgment after it is sent
											}

											i--;
											size--;
											break;
										}
									}
									i++;
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
					s = dataOutOp(BufferOp.GET,null,i);
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
	public void receiveSwitchData(){
		Thread t = new Thread() {
			public void run() {
				try {
					while(switchReceivePort == 0)
					{
						sleep(500);
					}
					
					String s = null;
					Socket client = null;
					
					ServerSocket ss;
					while(true)	//Keep trying port until a connection is established
					{
						try {
							ss = new ServerSocket(switchReceivePort);		//Try to connect to port
							System.out.println("Node " + address + " can receive");
							break;
						} catch(BindException e) {		//If no connection is established wait
							sleep(500);
							continue;
						}
					}
					
					BufferedReader reader = null;
					Frame f = null;
					while(termFlag) 
					{
						try {
							ss.setSoTimeout(2 * 10000);
							client = ss.accept();
						} catch (SocketTimeoutException e) {
							
							for(int i = 0; i < unAcked.size(); i++)
							{
								if(waitAck.get(i) == true)
								{
									unAcked.remove(i);
									waitAck.remove(i);
								}
								else
								{
									waitAck.set(i, true);
								}
							}
							continue;
						}
						
						reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
						
						while(!reader.ready())
						{	//sleep, periodically checking for data
							Thread.sleep(500);
						}
						
						s = reader.readLine();
						
						if(s != null)
						{
						
							f = new Frame(s, FrameType.STAR);
														
							if(f.isTerm())	//if terminate frame found
							{		
								termFlag = false;
								//dataIn.add(f);
							}
							else if(f.isAck())	//if ACK frame found
							{
								//System.out.println("Node " + address + " (SWITCH) received frame: " + f.toString());

								for(int i = 0; i < unAcked.size(); i++)
								{
									if(unAcked.get(i) == f.getSA())
									{
										unAcked.remove(i);
										waitAck.remove(i);
									}
								}
								for (int i = 0; i < dataOut.size(); i++)
								{
									Frame temp = dataOutOp(BufferOp.GET,null,i);
									if (f.getSA() == temp.getDA() && f.getDA() == temp.getSA() && f.getFrameType() == temp.getFrameType())	//find correct data instance to remove
									{
										//System.out.println("Node " + address + " has removed frame: " + temp.toString());
										dataOutOp(BufferOp.REM,null,i);	//remove data after acknowledgment
										break;
									}
								}
								//dataIn.add(f);
							}
								
							else if(address == f.getDA())	//Data frame found
							{
								dataOutOp(BufferOp.ADD,new Ack(address, f.getSA(), FrameType.STAR),0);	//add ACK frame for star
								dataIn.add(f);
								
								//System.out.println("Node " + address + " received frame: " + f.toString());
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

				printData();	//write output files after node finishes reading and writing switch data
				return;
			}
		};
		t.start();
		return;
	}

	public void receiveHubData() {
		Thread t = new Thread() {
			public void run() {
				try {
					while(hubReceivePort == 0)
					{
						sleep(500);
					}
					
					String s = null;
					Socket client = null;
					
					ServerSocket ss;
					while(true)	//Keep trying port until a connection is established
					{
						try {
							ss = new ServerSocket(hubReceivePort);		//Try to connect to port
							System.out.println("Node " + address + " can receive");
							break;
						} catch(BindException e) {		//If no connection is established wait
							sleep(500);
							continue;
						}
					}
					
					BufferedReader reader = null;
					Frame f = null;
					while(termFlag) 
					{
						try {
							ss.setSoTimeout(2 * 10000);
							client = ss.accept();
						} catch (SocketTimeoutException e) {
							
							hasSent = false;
							System.err.println("Node " + address + " has timed out");
							
							for(int i = 0; i < unAcked.size(); i++)
							{
								if(waitAck.get(i) == true)
								{
									unAcked.remove(i);
									waitAck.remove(i);
								}
								else
								{
									waitAck.set(i, true);
								}
							}
							continue;
						}
						
						reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
						
						while(!reader.ready())
						{	//sleep, periodically checking for data
							Thread.sleep(500);
						}
						
						s = reader.readLine();
						
						if(s != null)
						{
						
							f = new Frame(s, FrameType.RING);
							System.out.println("Node " + address + " (RINGHUB) received frame: " + f.toString());
				
							if(f.isTerm())	//if terminate frame found
							{
								System.err.println("Node " + address + " has received termination");
								termFlag = false;
								//dataIn.add(f);
							}
							else if(f.isAck())	//if ACK frame found
							{
								if(address == f.getDA())
								{
									for(int i = 0; i < unAcked.size(); i++)
									{
										if(unAcked.get(i) == f.getSA())
										{
											unAcked.remove(i);
											waitAck.remove(i);
										}
									}
									for (int i = 0; i < dataOut.size(); i++)
									{
										Frame temp = dataOutOp(BufferOp.GET,null,i);
										if (f.getSA() == temp.getDA() && f.getDA() == temp.getSA() && f.getFrameType() == temp.getFrameType())	//find correct data instance to remove
										{
											System.out.println("Node " + address + " has removed frame: " + temp.toString());
											dataOutOp(BufferOp.REM,null,i);	//remove data after acknowledgment
											break;
										}
									}
									//dataIn.add(f);
									
									forward.add(new Token());
									
									hasToken = false;
								}
								else
								{
									forward.add(f);
								}
							}
							else if(f.isToken())
							{
								System.out.println("Node " + address + " received Token");
								hasToken = true;
								hasSent = false;
							}
							else if(address == f.getDA())	// Data frame found
							{
								//dataOutOp(BufferOp.ADD,new Ack(address, f.getSA(), FrameType.RING),0);
								
								forward.add(new Ack(address, f.getSA(), FrameType.RING));
								
								dataIn.add(f);
								
							}
							else
							{
								forward.add(f);
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

				printData();	//write output files after node finishes reading and writing hub data
				return;
			}
		};
		t.start();
		return;
	}
	
	/**
	 * Thread synchronized method to prevent concurrent modification of dataOut
	 * @param op
	 * @param add
	 * @param index
	 * @return Frame being modified
	 */
	private synchronized Frame dataOutOp(BufferOp op, Frame add, int index)
	{
		switch(op)
		{
		case GET:
			return dataOut.get(index);
		case ADD:
			dataOut.add(add);
			return add;
		case REM:
			return dataOut.remove(index);
		default:
			break;
		}
		return null;
	}
}