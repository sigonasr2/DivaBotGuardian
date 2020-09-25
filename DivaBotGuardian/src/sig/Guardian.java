package sig;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.imageio.ImageIO;

import sig.utils.FileUtils;

public class Guardian {
	public static int USERID = -1;
	public static long streamLastModified = -1;
	public static STAGE currentStage = STAGE.STARTING;
	public static int FRAMECOUNT = 0;
	
	public static Point UPPERLEFTCORNER = null;
	public static Point LOWERRIGHTCORNER = null;
	
	enum STAGE{
		STARTING,
		CALIBRATING,
		RUNNING,
		CLEANUP;
	}
	
	public static BufferedImage CropFutureToneImage(BufferedImage img) throws IOException {
		Point crop1 = null;
		Point crop2 = null;
		
		Color col = new Color(img.getRGB(0, 0));
		if (col.getRed()<=5&&col.getGreen()<=5&&col.getBlue()<=5) {
			boolean done=false;
			for (int x=img.getWidth()-1;x>=img.getWidth()*(7f/8);x--) {
				for (int y=0;y<img.getHeight()/8;y++) {
					col = new Color(img.getRGB(x, y));
					if (col.getRed()>=5&&col.getGreen()>=5&&col.getBlue()>=5) {
						crop1 = new Point(x,y);
						done=true;
						break;
					}
				}
				if (done) {
					break;
				}
			}
			done=false;
			for (int x=0;x<img.getWidth()/8;x++) {
				for (int y=img.getHeight()-1;y>=img.getHeight()*(7f/8);y--) {
					col = new Color(img.getRGB(x, y));
					if (col.getRed()>=5&&col.getGreen()>=5&&col.getBlue()>=5) {
						crop2 = new Point(x,y);
						done=true;
						break;
					}
				}
				if (done) {
					break;
				}
			}
			UPPERLEFTCORNER=new Point(crop2.x,crop1.y);
			LOWERRIGHTCORNER=new Point(crop1.x,crop2.y);
			//img = img.getSubimage(crop2.x, crop1.y, crop1.x-crop2.x, crop2.y-crop1.y);
		}
		//System.out.println("Future Tone? "+MyRobot.FUTURETONE);
		return img;
	}
	
	public static int scale1280(int original) {
		//772,175
		//UPPERLEFTCORNER[x=37,y=22],LOWERRIGHTCORNER[x=1239,y=692]
		return (int)(original*((double)(LOWERRIGHTCORNER.x-UPPERLEFTCORNER.x)/1280))+UPPERLEFTCORNER.x;
	}
	public static int scale720(int original) {
		return (int)(original*((double)(LOWERRIGHTCORNER.y-UPPERLEFTCORNER.y)/720))+UPPERLEFTCORNER.y;
	}
	
	public static void main(String[] args) throws InterruptedException, IOException {
		USERID = Integer.parseInt(args[0]);
		File f = new File("streams","output"+USERID+".png");
		if (f.exists()) {
			f.delete();
		}
		File[] tempf = new File[10];
		
		for (int i=0;i<tempf.length;i++) {
			tempf[i] = new File("streams","tempoutput"+i+"_"+USERID+".png");
		}
		
 		program:
		while (true) {
			switch (currentStage) {
				case STARTING:{
					while (currentStage==STAGE.STARTING) {
						if (f.exists()) {
							streamLastModified = f.lastModified();
							currentStage=STAGE.CALIBRATING;
						}
						Thread.sleep(100);
					}
				}break;
				case CALIBRATING:{
					while (currentStage==STAGE.CALIBRATING) {
						try {
							HandleStreamFile(f, tempf[FRAMECOUNT]);
							CropFutureToneImage(ImageIO.read(tempf[FRAMECOUNT]));
							currentStage=STAGE.RUNNING;
						} catch (IOException | InvocationTargetException | NullPointerException e) {
						}
						Thread.sleep(100);
					}
				}break;
				case RUNNING:{
					while (currentStage==STAGE.RUNNING) {
						long startTime = System.currentTimeMillis();
						try {
							HandleStreamFile(f, tempf[FRAMECOUNT]);
							BufferedImage img = ImageIO.read(tempf[FRAMECOUNT]);
							//1227x690    //1.04319478402607986960065199674
							//1280x720
							ColorRegion cr = new ColorRegion(img,new Rectangle(scale1280(772),scale720(175),5,5));
							System.out.println(UPPERLEFTCORNER+","+LOWERRIGHTCORNER+"///"+"Is Song select? "+cr.getAllRange(160,200,0,15,170,200)+" "+cr+" ("+(System.currentTimeMillis()-startTime)+"ms)");
						} catch (IOException | InvocationTargetException | NullPointerException e) {
							System.out.println("Skip error frame.("+(System.currentTimeMillis()-startTime)+"ms)");
						}
						FRAMECOUNT=(FRAMECOUNT+1)%10;
						Thread.sleep(100);
					}
				}break;
				case CLEANUP:{
					f.delete();
					System.exit(0);
					break program;
				}
			}
		}
	}

	private static void HandleStreamFile(File f, File tempf) throws IOException, InvocationTargetException {
		FileUtils.copyFile(f, tempf);
		System.out.println(System.currentTimeMillis()+"/"+streamLastModified);
		if (System.currentTimeMillis()>=streamLastModified+5000) {
			currentStage=STAGE.CLEANUP;
			System.out.println("Stream is no longer being updated! Shutting down!");
		} else {
			streamLastModified=f.lastModified();
		}
	}
}
