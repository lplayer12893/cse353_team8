public class Token extends Frame
{
	public Token()
	{
		super(0,0,FrameType.RING);

		AC = 128;
		FC = 128;
		FS = 0;
		
		setCRC();
	}
}