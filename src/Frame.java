import java.util.Random;

/**
 * A frame to be sent in an STPLP MAC protocol simulator
 * @author Lucas Stuyvesant, Joshua Garcia, Nizal Alshammry
 */
public class Frame {
    
    /*
     * MyBytes are used when I need to read/modify
     * individual bits
     * Bytes are used when I do not
     */
    private final Integer SA;
    private final Integer DA;
    private final Integer SIZE;
    private final String Data;
    private final Boolean prioritized;
    
    public Frame(){	// creates a termination notification
        this(0,0,0,"",false);
    }
    
    public Frame(Integer sa, Integer da){	// creates an acknowledgment
        this(sa,da,0,"",false);
    }
    
    /**
     * Constructs a Frame object from a binary frame
     * @param frame
     */
    public Frame(String frame){
        SA = Integer.parseInt(frame.substring(0,8),2);
        DA = Integer.parseInt(frame.substring(8,16),2);
        SIZE = Integer.parseInt(frame.substring(16,24),2);

        String s = frame.substring(24,24 + SIZE * 8);    
        char[] tmp = new char[SIZE];
        int i = 0;
        for(; i < s.length(); i += 8){
            tmp[i/8] = (char)Integer.parseInt(s.substring(i,i+8),2);
        }
        
        Data = String.valueOf(tmp);
        if(frame.charAt(frame.length() - 1) == '1')
        {
        	prioritized = true;
        }
        else
        {
        	prioritized = false;
        }
    }
    
    /**
     * Constructs a Frame object from the input file
     * @param frame
     */
    public Frame(Integer sa, String init){
    	
    	SA = sa;
    	String [] s = init.split(":",2);
    	if(s.length != 2)
    	{
    		System.err.println("Node " + sa + " has a frame issue, length " + s.length);
    	}
    	DA = Integer.parseInt(s[0]);
    	Data = s[1];
    	SIZE = Data.length();
    	Random r = new Random();
    	prioritized = r.nextBoolean();
    }
    
    /**
     * @param sA
     * @param dA
     * @param sIZE
     * @param data
     */
    public Frame(int sA, int dA, int sIZE, String data, Boolean prty) {
        
        // If the size byte does not match the length of the data, populate the frame erroneously
        
        SA = sA;
        DA = dA;
        SIZE = sIZE;
        Data = data;
        prioritized = prty;
    }

    /**
     * @return the sA
     */
    public int getSA() {
        return SA;
    }
    
    /**
     * @return the dA
     */
    public int getDA() {
        return DA;
    }

    /**
     * @return the sIZE
     */
    public int getSIZE() {
        return SIZE;
    }

    /**
     * @return the data
     */
    public String getData() {
        return Data;
    }
    
    public Boolean isPrioritized() {
		return prioritized;
	}

	/**
     * Combines each field of the Frame into one binary String
     * @return binary String
     */
    public String toBinFrame(){
        String s = String.format("%8s", Integer.toBinaryString(SA)).replace(' ', '0');    //formating and replacing maintains leading 0's
        s = s + String.format("%8s", Integer.toBinaryString(DA)).replace(' ', '0');
        s = s + String.format("%8s", Integer.toBinaryString(SIZE)).replace(' ', '0');
        
        Integer n;    //convert the data to binary one byte at a time
        for(int i = 0; i < SIZE; i++){
            n = Integer.valueOf(Data.charAt(i));
            s = s + String.format("%8s", Integer.toBinaryString(n)).replace(' ', '0');
        }
        
        if(prioritized)
        {
        	s = s + "1";
		}
        else
        {
        	s = s + "0";
        }
        
        return s; 
    }

    /**
     * @return true if the frame is an acknowledgement of frame received, false otherwise
     */
    public boolean isAck()
    {
    	if(SIZE == 0 && Data.length() == 0)
    	{
    		return true;
    	}
    	return false;
    }
    
    /**
     * @return true if the frame is a termination indicator, false otherwise
     */
    public boolean isTerm()
    {
    	if(DA == 0)
    	{
    		return true;
    	}
    	return false;
    }
    
    @Override
    public String toString(){
        if(DA == 0)
        {
        	return "termination";
        }
        else if(SIZE == 0)    //size == 0 indicates the frame is an ACK
    	{
            return "SA: " + SA + ", DA: " + DA + " ACK";
    	}

        if(prioritized)
        {
        	return "SA: " + SA + ", DA: " + DA + "," + SIZE + "," + Data + " (prioritized)";
        }
        else
        {
        	return "SA: " + SA + ", DA: " + DA + "," + SIZE + "," + Data;
        }
    }
}
