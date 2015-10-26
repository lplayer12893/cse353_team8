import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Sends data between nodes through a socket connection
 * @author Lucas Stuyvesant
 */
public class SocketManager {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		ArrayList<String> dataA = readData("confA.txt");

		Integer sendPortA = Integer.valueOf(dataA.get(0));
		ArrayList<String> dataB = readData("confB.txt");

		Integer sendPortB = Integer.valueOf(dataB.get(1));
		Integer recievePortB = Integer.valueOf(dataB.get(0));
		
		if(sendPortA.intValue() == sendPortB.intValue() || sendPortB.intValue() == recievePortB.intValue()){	//checks if a port is being used for another Node (other than the one shared)
			System.err.println("ERROR: There is a port conflict");
			return;
		}
		
		if(sendPortA.intValue() != recievePortB.intValue()){	//checks if port number is consistent for Nodes A and B
			System.err.println("ERROR: Node A is not sending on the port Node B is listening to");
		}
		
		Node B = new Node("B",true,true,sendPortB,recievePortB,new ArrayList<String>(),new ArrayList<String>(dataB.subList(2, dataB.size())));
		B.sendData();
		B.recieveData();
		
		Node A = new Node("A",true,false,sendPortA,null,new ArrayList<String>(),new ArrayList<String>(dataA.subList(1, dataA.size())));
		A.sendData();
		
		Node C = new Node("C",false,true,null,sendPortB,new ArrayList<String>(),new ArrayList<String>());
		C.recieveData();
	}

	/**
	 * @param filename
	 * @return data the data read from file [filename].txt
	 */
	public static ArrayList<String> readData(String filename){
		BufferedReader in = null;
		try {	//open file
			in = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		
		String s = null;
		ArrayList<String> data = new ArrayList<String>();
		
		try {
			while((s = in.readLine()) != null){	//read data
				data.add(s);
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return data;
	}
}
