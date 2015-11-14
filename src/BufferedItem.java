
/**
 * A single item in the RingHub buffer
 * @author Lucas Stuyvesant, Joshua Garcia, Nizal Alshammry
 */
public class BufferedItem {

	private Frame frame;
	private Integer portIndex;
	
	BufferedItem()
	{
		this(null, null);
	}
	
	BufferedItem(Frame f, Integer i)
	{
		frame = f;
		portIndex = i;
	}

	public Frame getFrame() {
		return frame;
	}

	public Integer getPortIndex() {
		return portIndex;
	}
}
