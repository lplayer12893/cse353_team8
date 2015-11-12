/**
 * A frame to be sent in an pizza MAC protocol simulator
 * @author Lucas Stuyvesant, Joshua Garcia, Nizal Alshammry
 */
public class Frame {
    
    public enum FrameType{RING,STAR};
	
	private Integer AC;
	private Integer FC;
    private final Integer SA;
    private final Integer DA;
    private final Integer SIZE;
    private final String Data;
    private Integer CRC;
    private Integer FS;
    private FrameType frameType;
    
    private Boolean valid;
    
    /**
     * Constructs a termination frame
     * @param type
     */
    public Frame(FrameType type)
    {
        this(0,0,0,"",type);
    }
    
    /**
     * Constructs an acknowledgement
     * @param sa
     * @param da
     * @param type
     */
    public Frame(Integer sa, Integer da, FrameType type)
    {
        this(sa,da,0,"",type);
    }
    
    /**
     * Constructs a Frame object from a binary frame
     * @param frame
     * @param type
     */
    public Frame(String frame, FrameType type)
    {
    	if(type == FrameType.STAR)
    	{
			AC = null;
			FC = null;
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
	
	        CRC = Integer.parseInt(frame.substring(i,i+8),2);
	        
	        int crc = 0;
	        for(int j = 0; j < s.length(); j++)
	        {
	        	if(s.charAt(j) == '1')
	        	{
	        		crc++;
	        	}
	        }
	        
	        if(CRC == crc)
	        {
	        	valid = true;
	        }
	        else
	        {
	        	valid = false;
	        }
	        
	        FS = null;
    	}
    	else
    	{
    		AC = Integer.parseInt(frame.substring(0,8),2);;
			FC = Integer.parseInt(frame.substring(8,16),2);;
	        DA = Integer.parseInt(frame.substring(16,24),2);
	        SA = Integer.parseInt(frame.substring(24,32),2);
	        SIZE = Integer.parseInt(frame.substring(32,40),2);
	
	        String s = frame.substring(40,40 + SIZE * 8);    
	        char[] tmp = new char[SIZE];
	        int i = 0;
	        for(; i < s.length(); i += 8){
	            tmp[i/8] = (char)Integer.parseInt(s.substring(i,i+8),2);
	        }
	        
	        Data = String.valueOf(tmp);
	
	        CRC = Integer.parseInt(frame.substring(i,i+8),2);
	        
	        int crc = 0;
	        for(int j = 0; j < s.length(); j++)
	        {
	        	if(s.charAt(j) == '1')
	        	{
	        		crc++;
	        	}
	        }
	        
	        if(CRC == crc)
	        {
	        	valid = true;
	        }
	        else
	        {
	        	valid = false;
	        }
	        
	        FS = Integer.parseInt(frame.substring(i+8,i+16),2);
    	}
    	frameType = type;
    }
    
    /**
     * Constructs a Frame object from the input file
     * @param frame
     * @param init
     */
    public Frame(Integer sa, String init)
    {
    	SA = sa;
    	String [] s = init.split(",",3);
    	if(s.length != 2)
    	{
    		System.err.println("Node " + sa + " has a frame issue, length " + s.length);
    	}
    	DA = Integer.parseInt(s[0]);
    	if(s[1] == "C")
    	{
    		AC = null;
    		FC = null;
    		FS = null;
    		frameType = FrameType.STAR;
    	}
    	else
    	{
    		AC = 0;
    		FC = 0;
    		FS = 0;
    		frameType = FrameType.RING;
    	}
    	
    	Data = s[2];
    	SIZE = Data.length();
    }
    
    /**
     * Constructs a specific frame
     * @param sA
     * @param dA
     * @param sIZE
     * @param data
     * @param type
     */
    public Frame(int sA, int dA, int sIZE, String data, FrameType type)
    {
        if(type == FrameType.STAR)
        {
        	AC = null;
        	FC = null;
        	SA = sA;
	        DA = dA;
	        SIZE = sIZE;
	        Data = data;
	        
	        setCRC();
	        
	        FS = null;
	                
	        frameType = type;
        }
        else
        {
    		AC = 0;
    		FC = 0;
	        SA = sA;
	        DA = dA;
	        SIZE = sIZE;
	        Data = data;
	        
	        setCRC();
	        
    		FS = 0;
	                
	        frameType = type;
        }
    }

    /**
     * @return the AC
     */
    public Integer getAC() {
		return AC;
	}

    /**
     * @return the FC
     */
	public Integer getFC() {
		return FC;
	}

    /**
     * @return the FS
     */
	public Integer getFS() {
		return FS;
	}
    
    /**
     * @return the SA
     */
    public int getSA()
    {
        return SA;
    }
    
    /**
     * @return the DA
     */
    public int getDA()
    {
        return DA;
    }

    /**
     * @return the SIZE
     */
    public int getSIZE()
    {
        return SIZE;
    }

    /**
     * @return the Data
     */
    public String getData()
    {
        return Data;
    }
    
    /**
     * Sets the crc byte according to the contents of the other fields
     */
    public void setCRC()
    {
    	String s = String.format("%8s", Integer.toBinaryString(SA)).replace(' ', '0');    //formating and replacing maintains leading 0's
        s = s + String.format("%8s", Integer.toBinaryString(DA)).replace(' ', '0');
        s = s + String.format("%8s", Integer.toBinaryString(SIZE)).replace(' ', '0');
        
        Integer n;    //convert the data to binary one byte at a time
        for(int i = 0; i < SIZE; i++){
            n = Integer.valueOf(Data.charAt(i));
            s = s + String.format("%8s", Integer.toBinaryString(n)).replace(' ', '0');
        }
        
        CRC = 0;
        for(int j = 0; j < s.length(); j++)
        {
        	if(s.charAt(j) == '1')
        	{
        		CRC++;
        	}
        }
    }
    
    /**
     * @return frameType
     */
    public FrameType getFrameType()
    {
    	return frameType;
    }
    
    /**
     * @return true if the frame is valid (non-erroneous), false otherwise
     */
    public boolean isValid()
    {
    	return valid;
    }

	/**
     * Combines each field of the Frame into one binary String
     * @return binary String
     */
    public String toBinFrame()
    {
    	if(frameType == FrameType.STAR)
    	{
	        String s = String.format("%8s", Integer.toBinaryString(SA)).replace(' ', '0');    //formating and replacing maintains leading 0's
	        s = s + String.format("%8s", Integer.toBinaryString(DA)).replace(' ', '0');
	        s = s + String.format("%8s", Integer.toBinaryString(SIZE)).replace(' ', '0');
	        
	        Integer n;    //convert the data to binary one byte at a time
	        for(int i = 0; i < SIZE; i++){
	            n = Integer.valueOf(Data.charAt(i));
	            s = s + String.format("%8s", Integer.toBinaryString(n)).replace(' ', '0');
	        }
	        
	        CRC = 0;
	        for(int j = 0; j < s.length(); j++)
	        {
	        	if(s.charAt(j) == '1')
	        	{
	        		CRC++;
	        	}
	        }
	        
	        return s;
    	}
    	
    	String s = String.format("%8s", Integer.toBinaryString(AC)).replace(' ', '0');
    	s = s + String.format("%8s", Integer.toBinaryString(FC)).replace(' ', '0');
        s = s + String.format("%8s", Integer.toBinaryString(DA)).replace(' ', '0');
        s = s + String.format("%8s", Integer.toBinaryString(SA)).replace(' ', '0');
        s = s + String.format("%8s", Integer.toBinaryString(SIZE)).replace(' ', '0');
        
        Integer n;    //convert the data to binary one byte at a time
        for(int i = 0; i < SIZE; i++){
            n = Integer.valueOf(Data.charAt(i));
            s = s + String.format("%8s", Integer.toBinaryString(n)).replace(' ', '0');
        }
        
        CRC = 0;
        for(int j = 0; j < s.length(); j++)
        {
        	if(s.charAt(j) == '1')
        	{
        		CRC++;
        	}
        }
        
        s = s + String.format("%8s", Integer.toBinaryString(CRC)).replace(' ', '0');
        s = s + String.format("%8s", Integer.toBinaryString(FS)).replace(' ', '0');
        
        return s;
    }

    /**
     * @return the frame in star format
     */
    public void toStar()
    {
    	if(frameType == FrameType.RING)
    	{
        	frameType = FrameType.STAR;
        	AC = null;
        	FC = null;
        	FS = null;
    	}
    }
    
    /**
     * @return the frame in ring format
     */
    public void toRing()
    {
    	if(frameType == FrameType.STAR)
    	{
    		frameType = FrameType.RING;

			if(isAck())
			{
				AC = 0;
				FC = 0;
				FS = 192;
			}
			//if is unACK?
			else
			{
	        	AC = null;
	        	FC = null;
	        	FS = null;
			}
    	}
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
    public String toString()
    {
        if(DA == 0)
        {
        	return "termination";
        }
        else if(SIZE == 0)    //size == 0 indicates the frame is an ACK
    	{
            return "SA: " + SA + ", DA: " + DA + " ACK";
    	}

    	return "SA: " + SA + ", DA: " + DA + "," + SIZE + "," + Data;
    }
}
