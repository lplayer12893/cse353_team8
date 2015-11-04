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
		
		for(int i = 1; i < 4; i++)
		{
			new Node(i, new ArrayList<Frame>(), new ArrayList<Frame>());
		}
		
		new Switch();
		System.out.println("Main thread termination");
	}
}
