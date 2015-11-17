import java.util.ArrayList;

/**
 * Sends data between nodes through a socket connection
 * @author Lucas Stuyvesant, Joshua Garcia, Nizal Alshammry
 */
public class SocketManager {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args.length != 1)
		{
			System.err.println("requires 1 argument [number of nodes]");
			return;
		}
		
		int numNodes = Integer.parseInt(args[0]) + 1;
		
		if(numNodes < 3 || numNodes > 256)
		{
			System.err.println("number of nodes must be between 2 and 255 inclusive");
			return;
		}
		
		for(int i = 1; i < numNodes; i++)	// start node processes (threaded part-way through constructor)
		{
			System.out.println("Starting Node " + i);
			new Node(i, new ArrayList<Frame>(), new ArrayList<Frame>());
		}
		
		System.out.println("Starting Switch");
		new Switch(numNodes - 1);	// start switch processes
		
		System.out.println("Starting RingHub");
		new RingHub(numNodes - 1);	// start ringHub processes
		
		System.out.println("Main thread termination");
	}
}
