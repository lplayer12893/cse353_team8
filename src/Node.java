import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
	private ArrayList<String> dataIn;	//data read from socket
	private ArrayList<Frame> dataOut;	//data written to socket
	private Socket sock;
	private BufferedReader reader;
	private BufferedWriter writer;

	public Node(){
		this(0,null,null);
	}
	
	
	public Node(Integer i, ArrayList<String> di, ArrayList<Frame> dt){
		
		address = i;
		termFlag = true;
		unAcked = new ArrayList<Integer>();
		dataIn = di;
		dataOut = dt;
		reader = null;
		writer = null;
		
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
				String s = null;
				
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
					while((s = rdr.readLine()) == null)	//read new port number from socket
					{
						System.out.println("Node " + address + " is sleeping, waiting to be reassigned");

						Thread.sleep(500);
					}
					
					rdr.close();
					sock.close();	//close old socket
					
					
					sock = new Socket((String)null, Integer.valueOf(s));	//connect to new port number
					
					writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));	//get socket outputStream
					reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));	//get reader from socket
										
				} catch (IOException e) {
					// TODO Auto-generated catch block stub
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				while(termFlag) {
					
					sendData();
					recieveData();
				}
			}
		};
		chatter.start();
	}
	
	/**
	 * prints data recieved from socket
	 */
	public void printData(Integer sa){
		FileWriter w = null;
		String addr = String.valueOf(address);
		
		try {
			w = new FileWriter("node" + addr + "output.txt");
			
			for(String s: dataIn){
				w.write(sa + ":" + s);
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
		
		try {
			if(dataOut == null){	//if there is no data to write to socket
				return;
			}
						
			for(Frame s: dataOut){	//write data to socket
				if(!unAcked.contains(s.getDA()) || s.isAck()) {
					
					System.out.println(s.toString());
					
					writer.write(s.toBinFrame());
					writer.newLine();
					unAcked.add(s.getDA());
				}
			}
			
			//writer.close();	//close socket and writer
			
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("ERROR: There is a port conflict with Node " + address);
			System.exit(-1);
			return;
		}
		
		return;
	}
	
	/**
	 * Reads data from the socket when the "server" accepts a client
	 * The data is then printed
	 */
	public void recieveData(){
		
		try {

			String s = null;
			
			while(!reader.ready()){	//sleep, periodically checking for data
				System.out.println("Node " + address + " is sleeping in receive");
				Thread.sleep(500);
			}
			
			s = reader.readLine();
			Frame f = null;
			
			if(s != null){
				f = new Frame(s);
				if(f.isAck()) {
					unAcked.remove(f.getDA());
				}
				else if(f.toString().equals("terminate")){	//if found terminate
					termFlag = false;
				}
				else
				{
					dataOut.add(new Frame(address,f.getSA()));
				}

				dataIn.add(s);
				s = reader.readLine();
			}
			
			printData(f.getSA());
			
			//reader.close();	//close reader
			
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("ERROR: There is a port conflict with Node " + address);
			System.exit(-1);
			return;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return;
	}
}
	
