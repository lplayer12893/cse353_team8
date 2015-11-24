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
import java.util.Random;

/**
 * Contains the implementation for a RingHub in a tcp/ip network
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

	int timeout;

	ArrayDeque<BufferedItem> nodesbuffer;	// buffer of frames, FIFO


	RingHub(int num) 
	{

		numNodes = num;
		// Initialize Ringlistener server socket. REQUIRES PORT 65534 BE OPEN
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
		timeout = 0;

		// Initialize termination flag
		termflag = true;

		nodesbuffer = new ArrayDeque<BufferedItem>();

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
						System.out.println("Ringhub reassigning client sender to " + startingPort);

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
						System.out.println("Ringhub reassigning client receiver to " + startingPort);

						startingPort++;

						writer.close();
						c.close();

					} catch (SocketTimeoutException e) {
						continue;
					} catch (IOException e) {
						System.err.println("Failed to establish reassigned connection to client");
						e.printStackTrace();
					}
				}
				System.out.println("RingHub listener is returning");
				return;
			}
		};

		aa.start();

		while(sendPorts.size() == 0)
		{
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

		nodesbufferOp(BufferOp.ADD,new BufferedItem(new Token(),0));

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
	private void sendData()
	{
		Thread send = new Thread()
		{
			public void run() 
			{
				Socket s = null;
				BufferedWriter writer = null;
				BufferedItem b = null;
				Frame f = null;

				try
				{
					while(termflag)	// until time to terminate
					{
						b = null;
						f = null;
						if(timeout == sendPorts.size())	// assume token was lost
						{
							System.out.println("Ringhub assuming TOKEN was lost");
							nodesbufferOp(BufferOp.ADD,new BufferedItem(new Token(),0));
							timeout = 0;
						}
						if(!nodesbuffer.isEmpty())	// if there is data to be sent
						{
							b = nodesbufferOp(BufferOp.REM,null);
							f = b.getFrame();

							while(true)
							{
								try {
									s = new Socket((String)null, sendPorts.get(b.getPortIndex()));
									writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
									writer.write(f.toBinFrame());
									
									System.out.println("Ringhub forwarded frame: " + f.toString());

									writer.close();
									s.close();
									break;
								} catch(IOException e) {

									Random r = new Random();
									long sleep = r.nextInt(100);
									int i = 1;

									try
									{
										if(i > 2)
										{
											sleep(sleep);
										}
										else
										{
											sleep((long)Math.pow(sleep, i));
											i++;
										}
									} catch (InterruptedException e1)
									{
										e1.printStackTrace();
										System.exit(-1);
									}
								}
							}
						}

						sleep(500);
					}

					Term term = new Term(FrameType.RING);	// send termination frame to all nodes
					for(Integer k : sendPorts)
					{
						s = new Socket((String)null, k);
						writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
						writer.write(term.toBinFrame());

						writer.close();
						s.close();
					}

				} catch (IOException e) {
					System.err.println("ERROR: There is a port conflict in Hub send");
					System.exit(-1);
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(-1);
				}

				System.out.println("RingHub send is returning");
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
					int reIssueToken;
					String ss = null;
					Frame ff = null;
					ServerSocket listen1 = new ServerSocket(recPort);
					Socket cc = null;
					BufferedReader reader1 = null;
					boolean flag = true;

					while(termflag)	// until time to terminate
					{
						reIssueToken = sendPorts.size() * 10000; 
						try {	// wait for connection, periodically checking termination status
							listen1.setSoTimeout(reIssueToken);
							cc = listen1.accept();
						} catch (SocketTimeoutException e) {
							if(flag)
							{
								timeout++;
								flag = false;
							}
							continue;
						}
						if(!flag)
						{
							flag = true;
							timeout--;
						}

						reader1 = new BufferedReader(new InputStreamReader(cc.getInputStream()));
						if(reader1 != null)
						{
							while(!reader1.ready())	//wait until reader is ready
							{
								System.out.println("ringhub waiting for reader");
								sleep(500);
							}

							while(reader1.ready())	//read all data and process the data
							{
								ss = reader1.readLine();
								ff = new Frame(ss,FrameType.RING);

								System.out.println("Ringhub received frame: " + ff.toString());

								if(ff.isToken())
									System.err.println("is Token, CRC = " + ff.CRC);
								
								if(ff.isValid())
								{
									System.out.println("Ringhub frame is valid");
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
									else
									{
										for(int i = 0; i < receivePorts.size(); i++)
										{
											if(receivePorts.get(i).equals(recPort))
											{
												if(i == (receivePorts.size() - 1))
												{
													nodesbufferOp(BufferOp.ADD,new BufferedItem(ff,0));
												}
												else
												{
													nodesbufferOp(BufferOp.ADD,new BufferedItem(ff,i+1));
												}
												System.out.println("Ringhub has buffered: " + ff.toString());
											}
										}
									}
								}
							}
						}
						reader1.close();
						cc.close();
						reader1 = null;
						cc = null;

					}

					listen1.close();

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

	/**
	 * Thread synchronized method to prevent concurrent modification of nodesbuffer
	 * @param op
	 * @param add
	 * @return BufferedItem being modified
	 */
	private synchronized BufferedItem nodesbufferOp(BufferOp op, BufferedItem add)
	{
		switch(op)
		{
		case ADD:
			nodesbuffer.addLast(add);
			return add;
		//case ADD:
		//	nodesbuffer.addLast(add);
		//	return add;
		case REM:
			return nodesbuffer.pop();
		default:
			break;
		}
		return null;
	}
}