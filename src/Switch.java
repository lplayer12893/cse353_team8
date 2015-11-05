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
import java.util.Random;

/**
 * Contains the implementation for a switch in a tcp/ip network
 * @author Lucas Stuyvesant, Joshua Garcia, Nizal Alshammry
 */
public class Switch {
	
	private final int numNodes;
	
	ServerSocket listener;	// listens for and accepts new clients
	
	ArrayList<Integer> receivePorts;
	ArrayList<Integer> sendPorts;
	ArrayList<Integer> usedPorts;
	int terminated;
	
	boolean termflag;	// used to synchronize termination of the network
	
	HashMap <Integer, Integer> switchTable;	// key is node number, value is index in clientList
	
	ArrayDeque<Frame> buffer;	// buffer of frames, FIFO
	ArrayDeque<Frame> prtyBuffer;	// buffer of prioritized frames, FIFO

	Switch(int num) {
		
		numNodes = num;
		// Initialize listener server socket
		while(true)
		{
			try {
				listener = new ServerSocket(65535);
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
		
		// Initialize switching table
		switchTable = new HashMap <Integer , Integer>();
		
		// Initialize frame buffer
		buffer = new ArrayDeque<Frame>();
		prtyBuffer = new ArrayDeque<Frame>();
		
		/**
		 * Handles accepting connections, reassigning the connections, and closing connections on termination
		 */
		Thread a = new Thread(){	
			public void run(){

				int startingPort = 49153;
				BufferedWriter writer = null;
				
				Socket client;
				
				while(termflag)
				{
					client = null;
					try {
						listener.setSoTimeout(5000);
						client = listener.accept();	//accept connection
												
						writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));	//get socket outputStream
						
						for(; startingPort < 65535; startingPort++)
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
						for(; startingPort < 65536; startingPort ++)
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
						client.close();
												
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
					ArrayList<Integer> badIndices = new ArrayList<Integer>();
					while(termflag)
					{
						badIndices.clear();
						f = null;
						if(!prtyBuffer.isEmpty() || !buffer.isEmpty())
						{
							if(!prtyBuffer.isEmpty())
							{
								f = prtyBuffer.pop();
							}
							else
							{
								f = buffer.pop();
							}
							
							if(!switchTable.containsKey(f.getDA()))
							{
								// Flood to all clients except the source
								for(int i = 0; i < sendPorts.size(); i++)
								{
									if(i != switchTable.get(f.getSA()))
									{
										try {
											s = new Socket((String)null, sendPorts.get(i));
											writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
											writer.write(f.toBinFrame());
											
											writer.close();
											s.close();
										} catch(IOException e) {
											badIndices.add(i);
										}
									}
								}
								if(!badIndices.isEmpty())
								{
									Random r = new Random();
									long sleep = r.nextInt(100);
									int i = 1;
									while(badIndices.size() > 0)
									{
										for(int j = 0; j < badIndices.size(); j++)
										{
											if(badIndices.get(j) != switchTable.get(f.getSA()))
											{
												try {
													s = new Socket((String)null, sendPorts.get(badIndices.get(j)));
													writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
													writer.write(f.toBinFrame());
													
													writer.close();
													s.close();
													
													badIndices.remove(j);
													
												} catch(IOException ex) {
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
														System.exit(-1);
													}
												}
											}
										}
									}
								}
							}
							else
							{
								try {
									s = new Socket((String)null, sendPorts.get(switchTable.get(f.getDA())));
									writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
									writer.write(f.toBinFrame());
									
									writer.close();
									s.close();
								} catch(IOException e) {
									Random r = new Random();
									long sleep = r.nextInt(100);
									int i = 1;
									
									while(true)
									{
										try {
											s = new Socket((String)null, sendPorts.get(switchTable.get(f.getDA())));
											writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
											writer.write(f.toBinFrame());
											
											writer.close();
											s.close();
										} catch(IOException ex) {
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
												System.exit(-1);
											}
										}
									}
								}
								
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
					System.err.println("ERROR: There is a port conflict in switch send");
					System.exit(-1);
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
					while(termflag)
					{
						try {
							listen.setSoTimeout(10000);
							c = listen.accept();
						} catch (SocketTimeoutException e) {
							continue;
						}
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
									terminated++;
									System.out.println("terminated = " + terminated);
									if(terminated == numNodes)
									{
										System.out.println("termflag is set to false");
										termflag = false;
										listen.close();
										System.out.println("Switch receive is returning");
										return;
									}
								}
								else
								{									
									if(!switchTable.containsKey(f.getSA()))
									{
									//	System.out.println("Switch key: " + f.getSA() + " value: " + receivePorts.indexOf(recPort));
										switchTable.put(f.getSA(), receivePorts.indexOf(recPort));
									}
									
									if(f.isPrioritized())
									{
										prtyBuffer.addLast(f);
									}
									else
									{
										buffer.addLast(f);
									}
								}
							}
						}
						reader.close();
						c.close();
						reader = null;
						c = null;
						
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


