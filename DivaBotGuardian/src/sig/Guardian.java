package sig;
import java.io.File;
import java.io.IOException;

import sig.utils.FileUtils;

public class Guardian {
	public static int USERID = -1;
	public static long streamLastModified = -1;
	public static STAGE currentStage = STAGE.STARTING;
	
	enum STAGE{
		STARTING,
		RUNNING,
		CLEANUP;
	}
	
	public static void main(String[] args) throws InterruptedException, IOException {
		USERID = Integer.parseInt(args[0]);
		File f = new File("streams","output"+USERID+".png");
		File tempf = new File("streams","tempoutput"+USERID+".png");
		switch (currentStage) {
			case STARTING:{
				while (currentStage==STAGE.STARTING) {
					if (f.exists()) {
						streamLastModified = f.lastModified();
						currentStage=STAGE.RUNNING;
					}
					Thread.sleep(100);
				}
			}break;
			case RUNNING:{
				while (currentStage==STAGE.RUNNING) {
					FileUtils.copyFile(f, tempf);
					if (System.currentTimeMillis()>=streamLastModified+5000) {
						currentStage=STAGE.CLEANUP;
						System.out.println("Stream is no longer being updated! Shutting down!");
					}
					Thread.sleep(100);
				}
			}break;
			case CLEANUP:{
				f.delete();
			}break;
		}
	}
}
