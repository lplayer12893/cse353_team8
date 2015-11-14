public class Token extends Frame
{
	public Token(Integer sa, Integer da)
	{
		super(sa,da,FrameType.RING);

		AC = 128;
		FC = 128;
		FS = 0;
		
		setCRC();
	}
}