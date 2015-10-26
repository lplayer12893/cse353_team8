import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.ArrayList;

/**
 * Contains flags and functions to control server and client sockets
 * @author Lucas Stuyvesant
 */
public class Node {
	
	private Integer destAddr;
	private final boolean sendable;	//permission to send data to a socket
	private Integer sendPort;
	private final boolean recievable;	//permission to recieve data from socket
	private Integer recievePort;
	private ArrayList<String> dataIn;	//data read from socket
	private ArrayList<String> dataOut;	//data written to socket

	public Node(){
		this("",false,false,0,0,null,null);
	}
	
	public Node(String str, boolean s, boolean r, Integer sp, Integer rp, ArrayList<String> di, ArrayList<String> dt){
		name = str;
		sendable = s;
		sendPort = sp;
		recievable = r;
		recievePort = rp;
		dataIn = di;
		dataOut = dt;
	}
	
	/**
	 * @return the sendable
	 */
	public boolean isSendable() {
		return sendable;
	}

	/**
	 * @return the recievable
	 */
	public boolean isRecievable() {
		return recievable;
	}

	/**
	 * prints data recieved from socket
	 */
	public void printData(){
		for(String s: dataIn){
			System.out.println("Node " + name + " received: " + s);
		}
	}
 
	/**
	 * Sends data from dataOut to the socket that accepts it when made
	 */
	public void sendData(){
		
		Thread send = new Thread() {
			public void run() {
				
				if(sendable == false){	//check permission 
					System.err.println("ERROR: This socket cannot send data");
					return;
				}
				
				try {
					if(dataOut == null){	//if there is no data to write to socket
						return;
					}
					
					Socket client = new Socket((String)null, sendPort);	//connect to socket server
					
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));	//get socket outputStream
					
					for(String s: dataOut){	//write data to socket
						writer.write(s);
						writer.newLine();
					}
					
					writer.write("terminate");	//write terminate command
					
					writer.close();	//close socket and writer
					client.close();
					
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
				
				if(recievable == false){	//check permission 
					System.err.println("ERROR: This socket cannot recieve data");
					return;
				}
				
				try {
					ServerSocket server = new ServerSocket(recievePort);	//establish a server
					Socket client = server.accept();	//accept a socket request
					
					BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));	//get reader from socket
					String s = null;
					while((s = reader.readLine()) == null){	//sleep, periodically checking for data
						sleep(500);
					}
					while(true){	//read data until terminate is found
						if(s != null){
							if(s.equals("terminate")){	//if found terminate
								break;
							}

							dataIn.add(s);
							s = reader.readLine();
						}
					}
					
					printData();
					
					reader.close();	//close reader and server
					server.close();
					
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
}
	
