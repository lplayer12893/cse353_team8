import java.util.Random;

/**
 * A frame to be sent in an pizza MAC protocol simulator
 * @author Lucas Stuyvesant, Joshua Garcia, Nizal Alshammry
 */
public class Frame {
    	
	protected Integer AC;
	protected Integer FC;
	protected final Integer SA;
	protected final Integer DA;
	protected final Integer SIZE;
	protected final String Data;
	protected Integer CRC;
	protected Integer FS;
	protected FrameType frameType;
    
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
     * Constructs an acknowledgment starter
     * @param sa
     * @param da
     * @param type
     */
    public Frame(Integer sa, Integer da, FrameType type)
    {
    	SA = sa;
		DA = da;
		SIZE = 0;
		Data = "";
		frameType = type;
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
	
	        CRC = Integer.parseInt(frame.substring(24+i,32+i),2);
	        
	        Integer crc = 0;
	        String crc_str = frame.substring(0,24+i);
	        for(int j = 0; j < crc_str.length(); j++)
	        {
	        	if(crc_str.charAt(j) == '1')
	        	{
	        		crc++;
	        	}
	        }
	        
	        crc = crc % 256;
	        
	        if(CRC.equals(crc))
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
    		AC = Integer.parseInt(frame.substring(0,8),2);
			FC = Integer.parseInt(frame.substring(8,16),2);
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
	
	        CRC = Integer.parseInt(frame.substring(40+i,48+i),2);
	        
	        Integer crc = 0;
	        String crc_str = frame.substring(0,40+i);
	        for(int j = 0; j < crc_str.length(); j++)
	        {
	        	if(crc_str.charAt(j) == '1')
	        	{
	        		crc++;
	        	}
	        }
	        
	        crc = crc % 256;        
	        
	        frameType = type;
	        if(CRC.equals(crc))
	        {
	        	valid = true;
	        }
	        else
	        {
	        	valid = false;
	        }
	        
	        FS = Integer.parseInt(frame.substring(i+48,i+56),2);
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
    	if(s.length != 3)
    	{
    		System.err.println("Node " + sa + " has a frame issue, length " + s.length);
    	}
    	DA = Integer.parseInt(s[0]);
    	
    	if(s[1].equals("C"))
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
	        
	        frameType = type;
	        
	        setCRC();
	        
	        FS = null;
        }
        else
        {
    		AC = 0;
    		FC = 0;
	        SA = sA;
	        DA = dA;
	        SIZE = sIZE;
	        Data = data;
	        
	        frameType = type;
	        
	        setCRC();
	        
    		FS = 0;
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
    	String s;
    	if(frameType == FrameType.STAR)
    	{
	    	s = String.format("%8s", Integer.toBinaryString(SA)).replace(' ', '0');    //formating and replacing maintains leading 0's
	        s = s + String.format("%8s", Integer.toBinaryString(DA)).replace(' ', '0');
	        s = s + String.format("%8s", Integer.toBinaryString(SIZE)).replace(' ', '0');
	        
	        Integer n;    //convert the data to binary one byte at a time
	        for(int i = 0; i < SIZE; i++){
	            n = Integer.valueOf(Data.charAt(i));
	            s = s + String.format("%8s", Integer.toBinaryString(n)).replace(' ', '0');
	        }
    	}
    	else
    	{
    		s = String.format("%8s", Integer.toBinaryString(AC)).replace(' ', '0');
        	s = s + String.format("%8s", Integer.toBinaryString(FC)).replace(' ', '0');
            s = s + String.format("%8s", Integer.toBinaryString(DA)).replace(' ', '0');
            s = s + String.format("%8s", Integer.toBinaryString(SA)).replace(' ', '0');
            s = s + String.format("%8s", Integer.toBinaryString(SIZE)).replace(' ', '0');
            
            Integer n;    //convert the data to binary one byte at a time
            for(int i = 0; i < SIZE; i++){
                n = Integer.valueOf(Data.charAt(i));
                s = s + String.format("%8s", Integer.toBinaryString(n)).replace(' ', '0');
            }
    	}
    	
    	CRC = 0;
        for(int j = 0; j < s.length(); j++)
        {
        	if(s.charAt(j) == '1')
        	{
        		CRC++;
        	}
        }
        
        CRC = CRC % 256;        
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
    	if(valid)
    		System.err.println(toString() + " is valid");
    	else
    		System.err.println(toString() + " is NOT valid");

    	return valid;
    }

	/**
     * Combines each field of the Frame into one binary String
     * @return binary String
     */
    public String toBinFrame()
    {
    	String s;
    	setCRC();
    	if(frameType == FrameType.STAR)
    	{
	        s = String.format("%8s", Integer.toBinaryString(SA)).replace(' ', '0');    //formating and replacing maintains leading 0's
	        s = s + String.format("%8s", Integer.toBinaryString(DA)).replace(' ', '0');
	        s = s + String.format("%8s", Integer.toBinaryString(SIZE)).replace(' ', '0');
	        
	        Integer n;    //convert the data to binary one byte at a time
	        for(int i = 0; i < SIZE; i++){
	            n = Integer.valueOf(Data.charAt(i));
	            s = s + String.format("%8s", Integer.toBinaryString(n)).replace(' ', '0');
	        }
	        
	        s = s + String.format("%8s", Integer.toBinaryString(CRC)).replace(' ', '0');
    	}
    	else
    	{
	    	s = String.format("%8s", Integer.toBinaryString(AC)).replace(' ', '0');
	    	s = s + String.format("%8s", Integer.toBinaryString(FC)).replace(' ', '0');
	        s = s + String.format("%8s", Integer.toBinaryString(DA)).replace(' ', '0');
	        s = s + String.format("%8s", Integer.toBinaryString(SA)).replace(' ', '0');
	        s = s + String.format("%8s", Integer.toBinaryString(SIZE)).replace(' ', '0');
	        
	        Integer n;    //convert the data to binary one byte at a time
	        for(int i = 0; i < SIZE; i++){
	            n = Integer.valueOf(Data.charAt(i));
	            s = s + String.format("%8s", Integer.toBinaryString(n)).replace(' ', '0');
	        }
	        
	        s = s + String.format("%8s", Integer.toBinaryString(CRC)).replace(' ', '0');
	        s = s + String.format("%8s", Integer.toBinaryString(FS)).replace(' ', '0');
    	}
        
    	/*Random r = new Random();
    	
    	if(r.nextInt(20) == 0)	// 5% chance to incorrectly send frame
    	{
    		if(s.charAt(0) == '0')
    		{
    			s.replaceFirst("[0]", "1");
    		}
    	}*/
    	
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
     * @return true if the frame is a token, false otherwise
     */
    public boolean isToken()
    {
    	if(SIZE == 0 && Data.length() == 0 && AC == 128 && FC == 128)
    	{
    		return true;
    	}
    	return false;
    }
    
    /**
     * @return true if the frame is an acknowledgment of frame received, false otherwise
     */
    public boolean isAck()
    {
    	switch(frameType)
    	{
    	case STAR:
    		if(SIZE == 0 && Data.length() == 0)
    		{
    			return true;
    		}
    		return false;
    	case RING:
    		    		
    		if(AC != 128 && FC != 128 && FS == 192)
        	{
        		return true;
        	}
    		return false;
    	}
    	return false;
    }
    
    /**
     * @return true if the frame is a termination indicator, false otherwise
     */
    public boolean isTerm()
    {
    	if(frameType == FrameType.RING)
    	{
        	if(DA == 0 && SA == 0 && AC != 128 && FC != 128)
        	{
        		return true;
        	}
    	}
    	else if(DA == 0 && SA == 0)
    	{
    		return true;
    	}
    	return false;
    }
    
    @Override
    public String toString()
    {
        if(isTerm())
        {
        	return "termination";
        }
        else if(isAck())    //size == 0 indicates the frame is an ACK
    	{
            return "SA: " + SA + ", DA: " + DA + " ACK";
    	}
        else if(isToken())
        {
            return "SA: " + SA + ", DA: " + DA + " TOKEN";

        }

    	return "SA: " + SA + ", DA: " + DA + "," + SIZE + "," + Data;
    }
}
