import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Switch {
	
	ServerSocket ss;
	ArrayList<ServerSocket> serverlist;
	ArrayList<Frame> buffer;
	boolean termflag;
	HashMap <Integer, Integer> switchTable ;
	

	
	
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


