/**
 * A frame to be sent in an STPLP MAC protocol simulator
 * @author Lucas Stuyvesant
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
    
    public Frame(){    //defaults to a token
        this(0,0,0,"");
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
        for(int i = 0; i < s.length(); i += 8){
            tmp[i/8] = (char)Integer.parseInt(s.substring(i,i+8),2);
        }
        
        Data = String.valueOf(tmp);
    }
    
    /**
     * Constructs a Frame object from a binary frame
     * @param frame
     */
    public Frame(Integer sa, String init){
    	
    	SA = sa;
    	String [] s = init.split(":");
    	DA = Integer.parseInt(s[0]);
    	Data = s[1];
    	SIZE = Data.length();
    }
    
    /**
     * @param sA
     * @param dA
     * @param sIZE
     * @param data
     */
    public Frame(int sA, int dA, int sIZE, String data) {
        
        // If the size byte does not match the length of the data, populate the frame erroneously
        if(sIZE != data.length())
        {
            SA = null;
            DA = null;
            SIZE = null;
            Data = null;
        }
        else
        {
            SA = sA;
            DA = dA;
            SIZE = sIZE;
            Data = data;
        }
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
    
    @Override
    public String toString(){
        if(SIZE == 0){    //size == 0 indicates the frame is an ACK
            return "ACK";
        }
        return DA + "," + SIZE + "," + Data;
    }
}
