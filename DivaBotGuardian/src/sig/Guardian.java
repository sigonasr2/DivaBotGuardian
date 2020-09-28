package sig;
import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import sig.utils.FileUtils;
import sig.utils.ImageUtils;

public class Guardian {
	public static int USERID = -1;
	public static long streamLastModified = -1;
	public static STAGE currentStage = STAGE.STARTING;
	public static int FRAMECOUNT = 0;
	
	public static Point UPPERLEFTCORNER = null;
	public static Point LOWERRIGHTCORNER = null;
	public static boolean RESULTSSCREEN = false;
	
	public static TypeFace2 typeface;
	
	enum STAGE{
		STARTING,
		CALIBRATING,
		RUNNING,
		SUBMIT,
		CLEANUP;
	}
	
	public static BufferedImage CropFutureToneImage(BufferedImage img) throws IOException {
		Point crop1 = null;
		Point crop2 = null;
		
		boolean done=false;
		for (int x=img.getWidth()-1;x>=img.getWidth()*(5f/8);x--) {
			for (int y=272;y<273;y++) {
				Color col = new Color(img.getRGB(x, y));
				Color lastcol = col;
				if (col.getRed()>=100&&col.getGreen()>=100&&col.getBlue()>=100) {
					while (col.getRed()+col.getGreen()+col.getBlue()>30||(col.getRed()+col.getGreen()+col.getBlue()>10&&(col.getRed()!=col.getGreen()||col.getGreen()!=col.getBlue()||col.getRed()!=col.getBlue()))) {
						lastcol = col;
						y--;
						if (y>=0) {
							col = new Color(img.getRGB(x, y));
						} else {
							y++;
							break;
						}
					}
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
		for (int x=0;x<img.getWidth()*(3f/8);x++) {
			for (int y=530;y>=529;y--) {
				Color col = new Color(img.getRGB(x, y));
				Color lastcol = col;
				if (col.getRed()>=100&&col.getGreen()>=100&&col.getBlue()>=100) {
					while (col.getRed()+col.getGreen()+col.getBlue()>30||(col.getRed()+col.getGreen()+col.getBlue()>10&&(col.getRed()!=col.getGreen()||col.getGreen()!=col.getBlue()||col.getRed()!=col.getBlue()))) {
						lastcol = col;
						y++;
						if (y<img.getHeight()-1) {
							col = new Color(img.getRGB(x, y));
						} else {
							y--;
							break;
						}
					}
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
		img = img.getSubimage(crop2.x, crop1.y, crop1.x-crop2.x, crop2.y-crop1.y);
		ImageIO.write(img,"png",new File("/var/www/html/divar/cropped/cropped"+USERID+".png"));
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
		
		typeface = new TypeFace2(ImageIO.read(new File("./DivaBotGuardian/DivaBotGuardian/typeface.png")),
				 ImageIO.read(new File("./DivaBotGuardian/DivaBotGuardian/typeface2.png")),
				ImageIO.read(new File("./DivaBotGuardian/DivaBotGuardian/typeface3.png")),
				 ImageIO.read(new File("./DivaBotGuardian/DivaBotGuardian/typeface4.png")),
				 ImageIO.read(new File("./DivaBotGuardian/DivaBotGuardian/typeface5.png")));
		
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
							if (isOnSongSelect(img)) {
								RESULTSSCREEN = false;
							} else {
								//607,365  92x19
								//ColorRegion(Region: java.awt.Rectangle[x=608,y=374,width=92,height=19],R:149,G:107,B:183)
								//System.out.println(cr);
								if (!RESULTSSCREEN) {
									if (OnResultsScreen(img)) {
										if (ReadytoSubmit(img)) {
											//Thread.sleep(500);
											MyRobot.FUTURETONE=true;
											Result r = typeface.getAllData(CropFutureToneImage(img));
											if (!(r.cool==-1 || r.fine==-1 || r.safe==-1 || r.sad==-1 || r.worst==-1 || r.percent<0f || r.percent>110f || r.combo==-1 || r.score==-1)) {
												currentStage=STAGE.SUBMIT;
											} else {
												System.out.println("Waiting for results to populate... "+r);
											}
										}
									}
								}
							}
							//System.out.println(UPPERLEFTCORNER+","+LOWERRIGHTCORNER+"///"+"Is Song select? "+cr.getAllRange(160,200,0,15,170,200)+" "+cr+" ("+(System.currentTimeMillis()-startTime)+"ms)");
						} catch (Exception e) {
							System.out.println("Skip error frame.  " +e.getMessage()+  " ("+(System.currentTimeMillis()-startTime)+"ms)");
						}
						FRAMECOUNT=(FRAMECOUNT+1)%10;
						Thread.sleep(100);
					}
				}break;
				case SUBMIT:{
					while (currentStage==STAGE.SUBMIT) {
						long startTime = System.currentTimeMillis();
						try {
							HttpClient httpclient = HttpClients.createDefault();
							HttpPost httppost = new HttpPost("http://45.33.13.215:4501/getUserAuthData");
							List<NameValuePair> params = new ArrayList<NameValuePair>();
							params.add(new BasicNameValuePair("password", FileUtils.readFromFile(".guardian_env")[0]));
							params.add(new BasicNameValuePair("userId", Integer.toString(USERID)));
							try {
								httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
							//Execute and get the response.
							HttpResponse response = null;
							try {
								response = httpclient.execute(httppost);
							} catch (IOException e) {
								e.printStackTrace();
							}
							HttpEntity entity = response.getEntity();
							
							JSONObject report = null;

							if (entity != null) {
							    try (InputStream instream = entity.getContent()) {
							    	Scanner s = new Scanner(instream).useDelimiter("\\A");
							    	String result = s.hasNext() ? s.next() : "";
							    	report=new JSONObject(result);
							    	instream.close();
							    } catch (UnsupportedOperationException | IOException e) {
									e.printStackTrace();
								}
							}
							File scoreFile = new File("streams","score"+USERID+".png");
							HandleStreamFile(f, scoreFile);
							ImageIO.write(ImageIO.read(scoreFile).getSubimage(UPPERLEFTCORNER.x, UPPERLEFTCORNER.y, LOWERRIGHTCORNER.x-UPPERLEFTCORNER.x, LOWERRIGHTCORNER.y-UPPERLEFTCORNER.y),"png",scoreFile);
							Process proc2 = Runtime.getRuntime().exec("mkdir ../server/files/plays/"+USERID);
							BufferedReader stdInput = new BufferedReader(new 
								     InputStreamReader(proc2.getInputStream()));

							BufferedReader stdError = new BufferedReader(new 
							     InputStreamReader(proc2.getErrorStream()));

							String output = null;
							while ((output = stdInput.readLine()) != null) {
							    System.out.println(output);
							}
							System.out.println("------------");
							while ((output = stdError.readLine()) != null) {
							    System.out.println(output);
							}
							System.out.println("------------");
							Process proc = Runtime.getRuntime().exec("cp streams/score"+USERID+".png ../server/files/plays/"+USERID+"/"+report.getInt("uploads"));
							stdInput = new BufferedReader(new 
								     InputStreamReader(proc.getInputStream()));

							stdError = new BufferedReader(new 
							     InputStreamReader(proc.getErrorStream()));
							
							output = null;
							while ((output = stdInput.readLine()) != null) {
							    System.out.println(output);
							}
							System.out.println("------------");
							while ((output = stdError.readLine()) != null) {
							    System.out.println(output);
							}
							//HandleStreamFile(f, new File(userFolder,Integer.toString(report.getInt("uploads"))));
							
							httpclient = HttpClients.createDefault();
							httppost = new HttpPost("http://projectdivar.com/passImageData");
							params = new ArrayList<NameValuePair>();
							params.add(new BasicNameValuePair("user", report.getString("username")));
							params.add(new BasicNameValuePair("auth", report.getString("authentication_token")));
							params.add(new BasicNameValuePair("url", "http://projectdivar.com/files/plays/"+USERID+"/"+report.getInt("uploads")));
							try {
								httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
							//Execute and get the response.
							response = null;
							try {
								response = httpclient.execute(httppost);
							} catch (IOException e) {
								e.printStackTrace();
							}
							entity = response.getEntity();
							
							report = null;

							if (entity != null) {
							    try (InputStream instream = entity.getContent()) {
							    	Scanner s = new Scanner(instream).useDelimiter("\\A");
							    	String result = s.hasNext() ? s.next() : "";
							    	report=new JSONObject(result);
							    	instream.close();
							    } catch (UnsupportedOperationException | IOException e) {
									e.printStackTrace();
								}
							}
							

							RESULTSSCREEN=true;
							System.out.println(report);
							
							currentStage=STAGE.RUNNING;
						} catch (IOException | NullPointerException | InvocationTargetException | JSONException e) {
							e.printStackTrace();
							System.out.println("Skip error frame.("+(System.currentTimeMillis()-startTime)+"ms)");
						}
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

	private static boolean ReadytoSubmit(BufferedImage img) {
		IgnoredColorRegion cr = new IgnoredColorRegion(img,new Rectangle(scale1280(606),scale720(371),92,19));
		IgnoredColorRegion cr2 = new IgnoredColorRegion(img,new Rectangle(scale1280(607),scale720(335),65,21));
		IgnoredColorRegion cr3 = new IgnoredColorRegion(img,new Rectangle(scale1280(607),scale720(302),79,21));
		IgnoredColorRegion cr4 = new IgnoredColorRegion(img,new Rectangle(scale1280(607),scale720(267),68,21));
		IgnoredColorRegion cr5 = new IgnoredColorRegion(img,new Rectangle(scale1280(607),scale720(234),80,20));
		// ColorRegion(Region: java.awt.Rectangle[x=608,y=374,width=92,height=19],R:149,G:107,B:183)
		System.out.println("  "+cr);
		System.out.println("  "+cr2);
		System.out.println("  "+cr3);
		System.out.println("  "+cr4);
		System.out.println("  "+cr5);
		return cr.getAllRange(115,170,60,130,150,210)&&
				cr2.getAllRange(40,150,75,150,100,200)&&
				cr3.getAllRange(50,170,150,240,60,170)&&
				cr4.getAllRange(95,160,135,200,140,200)&&
				cr5.getAllRange(150,220,150,220,80,180);
	}

	private static boolean OnResultsScreen(BufferedImage img) {
		ColorRegion ft_results = new ColorRegion(img,new Rectangle(scale1280(81),scale720(35),80,37));
		System.out.println(" "+ft_results);
		return ft_results.getAllRange(30,150,60,180,60,180);
	}

	private static boolean isOnSongSelect(BufferedImage img) {
		ColorRegion cr = new ColorRegion(img,new Rectangle(scale1280(772),scale720(175),5,5));
		return cr.getAllRange(160,200,0,15,170,200);
	}

	private static void HandleStreamFile(File f, File tempf) throws IOException, InvocationTargetException {
		FileUtils.copyFile(f, tempf);
		//System.out.println(System.currentTimeMillis()+"/"+streamLastModified);
		if (System.currentTimeMillis()>=streamLastModified+5000) {
			currentStage=STAGE.CLEANUP;
			System.out.println("Stream is no longer being updated! Shutting down!");
		} else {
			streamLastModified=f.lastModified();
		}
	}
}
