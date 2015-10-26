package com.cse353_Lucas_Stuyvesant_p2.STPLP_Sim;

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
	private Integer AC;
	private final Integer FC;
	private final Integer DA;
	private final Integer SA;
	private final Integer SIZE;
	private final String Data;
	private Integer FS;
	
	public Frame(){	//defaults to a token
		this(0,0,0,0,0,"",0);
	}
	
	/**
	 * Constructs a Frame object from a binary frame
	 * @param frame
	 */
	public Frame(String frame){
		AC = Integer.parseInt(frame.substring(0,8),2);
		FC = Integer.parseInt(frame.substring(8,16),2);
		DA = Integer.parseInt(frame.substring(16,24),2);
		SA = Integer.parseInt(frame.substring(24,32),2);
		SIZE = Integer.parseInt(frame.substring(32,40),2);
		
		String s = frame.substring(40,40 + SIZE * 8);	
		char[] tmp = new char[SIZE];
		for(int i=0; i<s.length(); i+=8){
			tmp[i/8] = (char)Integer.parseInt(s.substring(i,i+8),2);
		}
		Data = String.valueOf(tmp);
		
		FS = Integer.parseInt(frame.substring(frame.length() - 8),2);;
	}
	
	/**
	 * @param aC
	 * @param fC
	 * @param dA
	 * @param sA
	 * @param sIZE
	 * @param data
	 * @param fS
	 */
	public Frame(int aC, int fC, int dA, int sA, int sIZE,
			String data, int fS) {
		AC = aC;
		FC = fC;
		DA = dA;
		SA = sA;
		SIZE = sIZE;
		Data = data;
		FS = fS;
	}

	/**
	 * @return the fS
	 */
	public int getFS() {
		return FS;
	}

	/**
	 * @param fS the fS to set
	 */
	public void setFS(int fS) {
		FS = fS;
	}

	/**
	 * @return the aC
	 */
	public int getAC() {
		return AC;
	}

	/**
	 * @param aC the aC to set
	 */
	public void setAC(int aC){
		AC = aC;
	}
	
	/**
	 * @return the fC
	 */
	public int getFC() {
		return FC;
	}

	/**
	 * @return the dA
	 */
	public int getDA() {
		return DA;
	}

	/**
	 * @return the sA
	 */
	public int getSA() {
		return SA;
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
		String s = String.format("%8s", Integer.toBinaryString(AC)).replace(' ', '0');	//formating and replacing maintains leading 0's
		s = s + String.format("%8s", Integer.toBinaryString(FC)).replace(' ', '0');
		s = s + String.format("%8s", Integer.toBinaryString(DA)).replace(' ', '0');
		s = s + String.format("%8s", Integer.toBinaryString(SA)).replace(' ', '0');
		s = s + String.format("%8s", Integer.toBinaryString(SIZE)).replace(' ', '0');
		
		Integer n;	//convert the data to binary one byte at a time
		for(int i=0; i<SIZE; i++){
			n = Integer.valueOf(Data.charAt(i));
			s = s + String.format("%8s", Integer.toBinaryString(n)).replace(' ', '0');
		}
		
		s = s + String.format("%8s", Integer.toBinaryString(FS)).replace(' ', '0');
		return s; 
	}
	
	/**
	 * @return true if Frame is a token, false if not
	 */
	public Boolean isToken(){
		if(FC == 0){
			return true;
		}
		return false;
	}
	
	/**
	 * @return true if Frame indicates network is shutting down, false if not
	 */
	public Boolean isTerminate(){
		if(FS == 8){
			return true;
		}
		return false;
	}

	@Override
	public String toString(){
		if(SIZE == 0){	//size == 0 indicates the frame is a token
			return "token";
		}
		return DA + "," + SIZE + "," + Data;
	}
}
