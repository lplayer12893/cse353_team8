import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;

/**
 * Contains flags and functions to control server and client sockets
 * @author Lucas Stuyvesant
 */
public class Node {
	
	private Integer address;
	private boolean termFlag;
	private ArrayList<Integer> unAcked;
	private ArrayList<String> dataIn;	//data read from socket
	private ArrayList<Frame> dataOut;	//data written to socket
	private Socket sock;

	public Node(){
		this(0,null,null);
	}
	
	
	public Node(Integer i, ArrayList<String> di, ArrayList<Frame> dt){
		
			address = i;
			dataIn = di;
			
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
	public void printData(){
		String addr = String.valueOf(address);
		PrintWriter out = null;
		try {
			out = new PrintWriter("node" + addr + "output.txt");
		} catch (FileNotFoundException e) {
			System.out.println("Could not create file node" + addr + ".txt");
			e.printStackTrace();
		}
		for(String s: dataIn){
			out.println((new Frame(s)).getSA() + ":" + s);
		}
	}
 
	/**
	 * Sends data from dataOut to the socket that accepts it when made
	 */
	public void sendData(){
		
		Thread send = new Thread() {
			public void run() {
				
				try {
					if(dataOut == null){	//if there is no data to write to socket
						return;
					}
					
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));	//get socket outputStream
					
					for(Frame s: dataOut){	//write data to socket
						if(!unAcked.contains(s.getDA())) {
							writer.write(s.toBinFrame());
							writer.newLine();
							unAcked.add(s.getDA());
						}
					}
					
					writer.close();	//close socket and writer
					sock.close();
					
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("ERROR: There is a port conflict");
					return;
				}
				
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
	public void recieveData(){
		
		Thread recieve = new Thread() {
			public void run() {
				
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));	//get reader from socket
					String s = null;
					
					while((s = reader.readLine()) == null){	//sleep, periodically checking for data
						sleep(500);
					}
					
					Frame f = null;
					
					if(s != null){
						f = new Frame(s);
						if(f.isAck()) {
							unAcked.remove(f.getDA());
						}
						else if(f.toString().equals("terminate")){	//if found terminate
							termFlag = false;
						}

						dataIn.add(s);
						s = reader.readLine();
					}
					
					printData();
					
					reader.close();	//close reader and server
					
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("ERROR: There is a port conflict");
					return;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				return;
			}
		};
		recieve.start();	//start reciever
		return;
	}
	
	public void chat() {
		BufferedReader reader = null;
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
			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));	//get reader from socket
			while((s = reader.readLine()) == null)	//read new port number from socket
			{
				Thread.sleep(500);
			}
			sock.close();	//close old socket
			sock = new Socket((String)null, Integer.valueOf(s));	//connect to new port number
			
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
}
	
