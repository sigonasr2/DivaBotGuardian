package sig;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class IgnoredColorRegion {
	Rectangle region;
	BufferedImage img;
	
	IgnoredColorRegion(BufferedImage img, Rectangle region){
		this.region=region;
		this.img=img;
	}
	
	public boolean getRedRange(int min,int max) {
		int avgRed = getRed();
		return avgRed>=min&&avgRed<=max;
	}
	public boolean getGreenRange(int min,int max) {
		int avgGreen = getGreen();
		return avgGreen>=min&&avgGreen<=max;
	}
	public boolean getBlueRange(int min,int max) {
		int avgBlue = getBlue();
		return avgBlue>=min&&avgBlue<=max;
	}
	
	public boolean getAllRange(int min,int max) {
		return getRedRange(min,max)&&getGreenRange(min,max)&&getBlueRange(min,max);
	}
	public boolean getAllRange(int minRed,int maxRed,int minGreen,int maxGreen,int minBlue,int maxBlue) {
		return getRedRange(minRed,maxRed)&&getGreenRange(minGreen,maxGreen)&&getBlueRange(minBlue,maxBlue);
	}
	
	public int getRed() {
		int total = 0;
		int ignoredRegions=0;
		for (int x=0;x<region.width;x++) {
			for (int y=0;y<region.height;y++) {
				if (region.x+x<0||region.x+x>=region.x+region.width||region.y+y<0||region.y+y>=region.y+region.height) {
					continue;
				}
				Color col = new Color(img.getRGB(region.x+x, region.y+y));
				if (((col.getRed()<=160||col.getRed()>180)&&(col.getGreen()<=180||col.getGreen()>193)&&(col.getBlue()<=170||col.getBlue()>190))||
						((col.getRed()<=190||col.getRed()>220)&&(col.getGreen()<=210||col.getGreen()>230)&&(col.getBlue()<=210||col.getBlue()>220))) {
					total+=new Color(img.getRGB(region.x+x, region.y+y)).getRed();
				} else {
					ignoredRegions++;
				}
			}
		}
		return total/((region.width*region.height)-ignoredRegions);
	}
	public int getGreen() {
		int total = 0;
		int ignoredRegions=0;
		for (int x=0;x<region.width;x++) {
			for (int y=0;y<region.height;y++) {
				if (region.x+x<0||region.x+x>=region.x+region.width||region.y+y<0||region.y+y>=region.y+region.height) {
					continue;
				}
				Color col = new Color(img.getRGB(region.x+x, region.y+y));
				if (((col.getRed()<=160||col.getRed()>180)&&(col.getGreen()<=180||col.getGreen()>193)&&(col.getBlue()<=170||col.getBlue()>190))||
						((col.getRed()<=190||col.getRed()>220)&&(col.getGreen()<=210||col.getGreen()>230)&&(col.getBlue()<=210||col.getBlue()>220))) {
					total+=new Color(img.getRGB(region.x+x, region.y+y)).getGreen();
				} else {
					ignoredRegions++;
				}
			}
		}
		return total/((region.width*region.height)-ignoredRegions);
	}
	public int getBlue() {
		int total = 0;
		int ignoredRegions=0;
		for (int x=0;x<region.width;x++) {
			for (int y=0;y<region.height;y++) {
				if (region.x+x<0||region.x+x>=region.x+region.width||region.y+y<0||region.y+y>=region.y+region.height) {
					continue;
				}
				Color col = new Color(img.getRGB(region.x+x, region.y+y));
				if (((col.getRed()<=160||col.getRed()>180)&&(col.getGreen()<=180||col.getGreen()>193)&&(col.getBlue()<=170||col.getBlue()>190))||
						((col.getRed()<=190||col.getRed()>220)&&(col.getGreen()<=210||col.getGreen()>230)&&(col.getBlue()<=210||col.getBlue()>220))) {
					total+=new Color(img.getRGB(region.x+x, region.y+y)).getBlue();
				} else {
					ignoredRegions++;
				}
			}
		}
		return total/((region.width*region.height)-ignoredRegions);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder("ColorRegion(Region: ");
		return sb.append(region).append(",")
		.append("R:").append(getRed()).append(",")
		.append("G:").append(getGreen()).append(",")
		.append("B:").append(getBlue()).append(")")
		.toString();
	}
}
