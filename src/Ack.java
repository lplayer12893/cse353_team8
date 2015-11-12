public class Ack extends Frame
{
	public Ack(Integer sa, Integer da, FrameType type)
	{
		super(sa,da,type);
		if(type == FrameType.RING)
		{
			AC = 0;
			FC = 0;
			FS = 192;
		}
		else
		{
			AC = null;
			FC = null;
			FS = null;
		}
		
		setCRC();
	}

}
