package cse353_team8;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import com.cse353_Lucas_Stuyvesant_p2.STPLP_Sim.Frame;

public class Switch {
	
	ServerSocket ss;
	ArrayList<ServerSocket> serverlist;
	ArrayList<Frame> buffer;
	boolean termflag;
	HashMap <Integer, Integer> switchTable ;
	Queue table = null;

	
	
	Switch() {	
		 
		try {
			ss = new ServerSocket(49152);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		 switchTable = new HashMap <Integer , Integer>();
		 buffer = new ArrayList<Frame>();
		 termflag = true;
		 
		 
		 Thread a = new Thread(){	
			 public void run(){
				 while(termflag){
					// TODO Auto-generated method stub
							
					Socket client;
					int portnumber = 50000;
							
					try {
						ss = new ServerSocket(portnumber);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}	
			
			
					try {
						client = ss.accept();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
				 }
			 }
		 };
		 a.start();
		 chat();
	}

	
	public void recieveFrame(Frame frame)
	{
		
		while(frame == null){
			
			Thread.sleep(100);
			
		}
		// thread to insert frame into queue
		if (frame.equals(switchTable) == false){
			
			
		}
		
	
	
	}


	
	private void chat() {
		// TODO Auto-generated method stub
		
		boolean recieving = true;
		while(recieving == false){
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	//	if(recieving )
		
	}
	
}

