public class Token extends Frame
{
	public Token()
	{
		super(-1,-1,FrameType.RING);

		AC = 128;
		FC = 128;
		FS = 0;
		
		setCRC();
	}
}