

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

public class Failure
{
	int viewMin;
	int viewMax;
	int captureView;
	boolean NOI = true;
	boolean ignored = false;
	boolean falsePositive = false;
	boolean overlapping = false;
	boolean protruding = false;
	boolean segregated = false;
	boolean bestEffort = false;
	boolean printFlagR = false; //flag to print recategorization images of right side
	boolean printFlagL = false; //flag to print recategorization images of left side
	boolean printFlagT = false; //flag to print recategorization images of top side
	boolean printFlagB = false; //flag to print recategorization images of bottom side
	boolean HTMLParentRelationship = false; //is there a HTML parent child relationship between the two xpaths.

	String type = null;
	int viewMaxWidth;
	int viewMaxHeight;
	int ID;
	int FirstImageScrollOffsetX;
	int FirstImageScrollOffsetY;

	BufferedImage screenshotP;
	String priorCat = "";
	ArrayList<String> xpaths; //xpaths of web elements
	ArrayList<Integer> problemXpathID;
	ArrayList<WebElement> wbElements; //DOM web elements
	ArrayList<BufferedImage> imgs; 
	ArrayList<Rectangle> rectangles;//DOM Rectangles of web elements (Maybe changed from original to fit to visible coordinates)
	ArrayList<Rectangle> orignalRectangles;//Original DOM Rectangles of web elements to use for printing and assessments
	ArrayList<String> notes; //notes or log if needed
	ArrayList<String> titles; //titles for notes or logs if needed
	
	ArrayList<Area> protrudingArea;
	Area overlappedArea;
	Area segregatedArea;

	Dimension reachRightDim = null;
	Dimension reachLeftDim = null;
	Dimension reachTopDim = null;
	Dimension reachBottomDim = null;
	
	long startTime;
	long endTime;
	long duration;
	public void startTime()
	{
		startTime = System.nanoTime();
	}
	public void endTime()
	{
		endTime = System.nanoTime();
		duration = endTime - startTime;
	}
	public long getDurationNano()
	{
		return duration;
	}
	public long getDurationMilli()
	{
		return duration/1000000;
	}
	public Failure()
	{
		xpaths = new ArrayList<String>();
		wbElements = new ArrayList<WebElement>();
		imgs = new ArrayList<BufferedImage>();
		rectangles = new ArrayList<Rectangle>();
		orignalRectangles = new ArrayList<Rectangle>();
		notes = new ArrayList<String>();
		titles = new ArrayList<String>();
		problemXpathID = new ArrayList<Integer>();
	}
	public void unreachableDimensions(DriverManager dm)
	{
		if(bestEffort && type.equals("viewport"))
		{
			if(falsePositive || NOI || ignored)
			{
				for(int xpathID: problemXpathID)
				{
					WebElement wb = dm.getWebElem(xpaths.get(xpathID));
					Rectangle wbRec = new Rectangle(wb.getLocation(),wb.getSize());
					dm.scroll(wbRec);
					
					if(wbRec.x < 0 || wbRec.y < 0 || dm.cantReachX > 0 || dm.cantReachY > 0)
					{
						if(dm.cantReachX > 0)
						{
							reachRightDim = new Dimension(Math.min(viewMaxWidth, dm.cantReachX), Math.min(viewMaxHeight,wbRec.height));
							titles.add("ReCat Criteria:");
							notes.add("Cant reach X Right by ("+dm.cantReachX+")");
						}
						if(dm.cantReachY > 0)
						{
							reachBottomDim = new Dimension(Math.min(viewMaxWidth,wbRec.width), Math.min(viewMaxHeight,dm.cantReachY));
							titles.add("ReCat Criteria:");
							notes.add("Cant reach Y Bottom by ("+dm.cantReachY+")");
						}
						if(wbRec.x < 0)
						{
							reachLeftDim = new Dimension(Math.min(viewMaxWidth,Math.abs(wbRec.x)), Math.min(viewMaxHeight,wbRec.height));
							titles.add("ReCat Criteria:");
							notes.add("Cant reach X Left by ("+wbRec.x+")");
						}
						if(wbRec.y < 0)
						{
							reachTopDim = new Dimension(Math.min(viewMaxWidth,wbRec.width), Math.min(viewMaxHeight,Math.abs(wbRec.y)));
							titles.add("ReCat Criteria:");
							notes.add("Cant reach Y Top by ("+wbRec.y+")");
						}
						
					}
				}
			}
		}
	}
	public void saveRecategorizedImages(DriverManager dm, String siteName, Area area, String categorization) {
		try {
			ImageIO.write(screenshotP, "png", new File("." + File.separator +Assist.date+ File.separator  + "ID_" + ID + "_" + type+ "_" + siteName + "_" + viewMin + "px_"+ viewMax + "px_"+categorization+"_" +0+".png"));
			if(area == null)
			{
				ImageIO.write(screenshotP, "png", new File("." + File.separator +Assist.date+ File.separator  + "ID_" + ID + "_" + type+ "_" + siteName + "_" + viewMin + "px_"+ viewMax + "px_"+categorization+"_" +1+".png"));
				return;
			}
			saveImages(siteName, dm.scrollX, dm.scrollY);
			Graphics2D g = screenshotP.createGraphics();
			Rectangle r1 = cutRectangleToVisibleArea(dm.scrollX, dm.scrollY, area.area, screenshotP);
			g.setColor(Color.BLACK);
			g.setStroke(Assist.dashed1);
			g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
			g.setColor(Color.ORANGE);
			g.setStroke(Assist.dashed2);
			g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
			ImageIO.write(screenshotP, "png", new File("." + File.separator +Assist.date+ File.separator  + "ID_" + ID + "_" + type+ "_" + siteName + "_" + viewMin + "px_"+ viewMax + "px_"+categorization+"_" +1+".png"));			
			for(TargetArea TA : area.targetAreas)
			{
				for (int i=1; i <= TA.targetImgs.size(); i++)
				{
					try 
					{
						ImageIO.write(TA.targetImgs.get(i-1), "png", new File("." + File.separator +Assist.date+ File.separator  + "ID_" + ID + "_" + type + "_" + siteName + "_"+ viewMin + "_" + viewMax + "_" + "Recategorized_" + i +".png"));
					} catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveRecatImages(BufferedImage screenshot, DriverManager dm, String siteName, TargetArea area, String categorization, String side) {
		try {
			if(area == null)
			{
				ImageIO.write(screenshot, "png", new File("." + File.separator +Assist.date+ File.separator  + "ID_" + ID + "_" + type+ "_" + siteName + "_" + viewMin + "px_"+ viewMax + "px_"+categorization+"_" +1+".png"));
				return;
			}
			for (int i=1; i <= area.targetImgs.size(); i++)
			{
				try 
				{
					ImageIO.write(area.targetImgs.get(i-1), "png", new File("." + File.separator +Assist.date+ File.separator  + "ID_" + ID + "_" + type + "_" + siteName + "_"+ viewMin + "_" + viewMax + "_" + "Recat_Side_"+ side + i +".png"));
				} catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
			saveImages(siteName, dm.scrollX, dm.scrollY);
			Graphics2D g = screenshot.createGraphics();
			Rectangle r1 = cutRectangleToVisibleArea(dm.scrollX, dm.scrollY, area.area, screenshot);
			g.setColor(Color.BLACK);
			g.setStroke(Assist.dashed1);
			g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
			g.setColor(Color.ORANGE);
			g.setStroke(Assist.dashed2);
			g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
			ImageIO.write(screenshot, "png", new File("." + File.separator +Assist.date+ File.separator  + "ID_" + ID + "_" + type+ "_" + siteName + "_" + viewMin + "px_"+ viewMax + "px_"+categorization+"_" +1+".png"));			
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void newCat(String newCat)
	{
		
		if(NOI)
		{
			priorCat = "NOI";			
		}
		if(falsePositive)
		{
			priorCat = "FP";

		}
		if(ignored)
		{
			priorCat = "ignored";

		}
		NOI = false;
		ignored = false;
		falsePositive = false;

		if(newCat.equals("NOI"))
		{
			NOI = true;
		}else if(newCat.equals("FP"))
		{
			falsePositive = true;
		}else if(newCat.equals("ignored"))
		{
			ignored = true;
		}
		titles.add("ReCat Success:");
		notes.add("(" +priorCat + ") confirmed as (" + newCat +")");
	}
	public void findAreasOfConcern()
	{
		protrudingArea = protrudingAreas(rectangles.get(0), rectangles.get(1));
		overlappedArea = overlappedArea(rectangles.get(0), rectangles.get(1));
		segregatedArea = segregatedArea(rectangles.get(0), rectangles.get(1));
		if(protruding)
		{
			for(Area a : protrudingArea)
			{
				if (a.checkSides()) 
				{
					//setIgnore(true);
					titles.add("Zero Size Error:");
					notes.add("A protruding area height or width is zero or negative");
					protrudingArea.remove(a);
				}
			}
			if(protrudingArea.isEmpty())
			{
				 setFalsePositive(true);
				 return;
			}
		}
		if(overlapping)
		{
			if (overlappedArea.checkSides()) 
			{
				setFalsePositive(true);
				titles.add("Zero Size Error:");
				notes.add("Overlapped area height or width is zero or negative");
				return;
			}
		}
		if(segregated)
		{
			if (segregatedArea.checkSides()) 
			{
				setFalsePositive(true);
				titles.add("Zero Size Error:");
				notes.add("segregated area height or width is zero or negative");
				return;
			}
		}
		
		
		if(!falsePositive)
		{
			findFalsePositive();
		}
		
	}

	public void findFalsePositive()
	{
		if(type.equals("collision"))
		{
			if(segregated)
			{
				setFalsePositive(true);
			}
		}
		else if(type.equals("protrusion") || type.equals("viewport"))
		{
			if(overlapping == true && protruding == false)
			{
				setFalsePositive(true);
			}
		}
	}
	
	public ArrayList<Rectangle> fitToView(Rectangle target) //cut the rectangle in to viewable portions
	{
		ArrayList<Rectangle> results = new ArrayList<Rectangle>();
		ArrayList<Rectangle> resultsW = new ArrayList<Rectangle>();
		int area = target.height * target.width;
		if(target.width > viewMaxWidth)
		{
			int portion = target.width / viewMaxWidth;
			for(int i=0; i < portion; i++)
			{
				resultsW.add(new Rectangle((target.x + (viewMaxWidth*i)), target.y, target.height, viewMaxWidth));
			}
			if((portion * viewMaxWidth) < target.width)
			{
				int widthLeftOver = target.width - (portion * viewMaxWidth);
				resultsW.add(new Rectangle((target.x + (viewMaxWidth*portion)), target.y, target.height, widthLeftOver));
			}

		}
		else
		{
			resultsW.add(target);
		}
		if(target.height > viewMaxHeight)
		{
			if(!resultsW.isEmpty())
			{
				for(Rectangle r : resultsW)
				{
					int portion = target.height / viewMaxHeight;
					for(int i=0; i < portion; i++)
					{
						results.add(new Rectangle(r.x, (r.y + (viewMaxHeight*i)), viewMaxHeight, r.width));
					}
					if((portion * viewMaxHeight) < target.height)
					{
						int heightLeftOver = target.height - (portion * viewMaxHeight);
						results.add(new Rectangle(r.x , (r.y + (viewMaxHeight*portion)), heightLeftOver, target.width));
					}
				}
			}
		}
		else
		{
			int newArea = 0;
			for(Rectangle r : resultsW)
			{
				newArea = newArea + (r.height * r.width);
			}
			if(newArea != area)
			{
				System.out.println("Rectangle slicing error: (Original Area, New Area) (" + area + "," + newArea + ")");
				System.exit(5);
			}
			return resultsW;
		}
		int newArea = 0;
		for(Rectangle r : results)
		{
			newArea = newArea + (r.height * r.width);
		}
		if(newArea != area)
		{
			System.out.println("Rectangle slicing error: (Original Area, New Area) (" + area + "," + newArea + ")");
			System.exit(5);
		}
		return results;
	}
	
	public ArrayList<Area> protrudingAreas(Rectangle firstR, Rectangle secondR) //returns protruding rectangles only
	{
		java.awt.Rectangle child;
		java.awt.Rectangle parent;
		parent = new java.awt.Rectangle(firstR.getX(), firstR.getY(), firstR.getWidth(), firstR.getHeight());
		child = new java.awt.Rectangle(secondR.getX(), secondR.getY(), secondR.getWidth(), secondR.getHeight());
		java.awt.Rectangle []protrudingRecAWT = SwingUtilities.computeDifference(child, parent);
		ArrayList<Area>  protrudingArea = new ArrayList<Area>();
		if(protrudingRecAWT.length > 0)
		{

			for(int i = 0; i < protrudingRecAWT.length; i++)
			{
				Rectangle r1 = new Rectangle(protrudingRecAWT[i].x, protrudingRecAWT[i].y, protrudingRecAWT[i].height, protrudingRecAWT[i].width);
				if(r1.height > 0 && r1.width > 0)
				{
					protrudingArea.add(new Area(r1, viewMaxWidth, viewMaxHeight));
				}
				else
				{
					titles.add("Protrusion excluded:");
					notes.add("AWT rectangle if Height,Width("+r1.height+","+r1.width+")");
				}
			}
			if(protrudingArea.size() > 0)
			{
				protruding = true;
				//System.out.println("----------------PROTRUDING");
			}
		}
		return protrudingArea;
	}
	public Area segregatedArea(Rectangle firstR, Rectangle secondR) //must be checked last!!!!!
	{
		if(protruding == false && overlapping == false)
		{
			segregated = true;
			//System.out.println("----------------SEGREGATED");
			if((firstR.height * firstR.width) <= (secondR.height * secondR.width))
			{
				return new Area(firstR, viewMaxWidth, viewMaxHeight);
			}
			else
			{
				return new Area(secondR, viewMaxWidth, viewMaxHeight);
			}
			
		}
		return null;
	}
	public Area overlappedArea(Rectangle firstR, Rectangle secondR)
	{
		if(Assist.intersectingRec(firstR, secondR)) //check if they actually intersect
		{

			Rectangle overlapRec = Assist.intrscRec(rectangles.get(0), rectangles.get(1));
			if(overlapRec.width > 0 && overlapRec.height > 0)
			{
				overlapping = true;
				//System.out.println("----------------OVERLAPPING");
				return new Area(overlapRec, viewMaxWidth, viewMaxHeight);
			}
			else
			{
				titles.add("Overlap excluded:");
				notes.add("AWT rectangle of Height,Width("+overlapRec.height+","+overlapRec.width+")");
			}
		}
		return null;
	}
	public void addXpath(String xpath)
	{
//		if(xpaths.isEmpty())
//		{
			xpaths.add(xpath);
//		}
//		else
//		{
//			if(xpaths.get(0).length() > xpath.length())
//			{
//				xpaths.add(0, xpath);
//			}
//			else
//			{
//				xpaths.add(xpath);
//			}
//		}
	}
	public void setCaptureView() {
		captureView = viewMin;
	}
	public void writeImages(String sitename)
	{
		if(protruding)
		{
			for(int x = 1; x <= protrudingArea.size(); x++)
			{
				for(TargetArea TA : protrudingArea.get(x-1).targetAreas) 
				{
					for (int i=1; i <= TA.targetImgs.size(); i++)
					{
						try 
						{
							ImageIO.write(TA.targetImgs.get(i-1), "png", new File("." + File.separator +Assist.date+ File.separator + "ID_" + ID + "_" + type + "_" + sitename + "_"+ viewMin + "_" + viewMax + "_" + "protruding_" + x + "_TargetArea_"+ i +".png"));
						} catch (IOException e) 
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
		if(overlapping)
		{
			for(TargetArea TA : overlappedArea.targetAreas)
			{
				for (int i=1; i <= TA.targetImgs.size(); i++)
				{
					try 
					{
						ImageIO.write(TA.targetImgs.get(i-1), "png", new File("." + File.separator +Assist.date+ File.separator  + "ID_" + ID + "_" + type + "_" + sitename + "_"+ viewMin + "_" + viewMax + "_" + "overlapping_"+ i +".png"));
					} catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
			}
		}
		if(segregated)
		{
			for(TargetArea TA : segregatedArea.targetAreas)
			{
				for (int i=1; i <= TA.targetImgs.size(); i++)
				{
					try 
					{
						ImageIO.write(TA.targetImgs.get(i-1), "png", new File("." + File.separator +Assist.date+ File.separator  + "ID_" + ID + "_" + type + "_" + sitename + "_"+ viewMin + "_" + viewMax + "_" + "segregated_" + i +".png"));
					} catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
			}
		}
	}
	public void saveImages(String site, int offsetX, int offsetY) throws IOException
	{
		offsetX = FirstImageScrollOffsetX;
		offsetY = FirstImageScrollOffsetY;
		for(int i = 0; i < imgs.size(); i++)
		{
			BufferedImage img = Assist.copyImage(imgs.get(0));
			Graphics2D g = img.createGraphics();
			if(ignored || i == 0)
			{
				Rectangle r1 = cutRectangleToVisibleArea(offsetX, offsetY, rectangles.get(0), img);
				Rectangle r2 = cutRectangleToVisibleArea(offsetX, offsetY, rectangles.get(1), img);
				g.setColor(Color.BLACK);
				g.setStroke(Assist.dashed1);
				g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
				g.setColor(Color.ORANGE);
				g.setStroke(Assist.dashed2);
				g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
				g.setColor(Color.BLACK);
				g.setStroke(Assist.dashed1);
				g.draw(new java.awt.Rectangle(r2.getX() , r2.getY() , r2.getWidth(), r2.getHeight())); //draw around second element
				g.setColor(Color.MAGENTA);
				g.setStroke(Assist.dashed2);                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             
				g.draw(new java.awt.Rectangle(r2.getX() , r2.getY() , r2.getWidth(), r2.getHeight())); //draw around second element

				g.dispose();
			}
			else
			{
				if(protruding && !type.equals("collision"))
				{
					for(Area a : protrudingArea)
					{
						for(TargetArea ta : a.targetAreas)
						{
							Rectangle r1 = cutRectangleToVisibleArea(offsetX, offsetY, ta.area, img);
							g.setColor(Color.BLACK);
							g.setStroke(Assist.dashed1);
							g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
							g.setColor(Color.GREEN);
							g.setStroke(Assist.dashed2);
							g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element

						}
					}
				}
				if(overlapping)
				{
					for(TargetArea ta : overlappedArea.targetAreas)
					{
						Rectangle r1 = cutRectangleToVisibleArea(offsetX, offsetY, ta.area, img);
						g.setColor(Color.BLACK);
						g.setStroke(Assist.dashed1);
						g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
						g.setColor(Color.RED);
						g.setStroke(Assist.dashed2);
						g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
					}
				}
				if(segregated)
				{
					for(TargetArea ta : segregatedArea.targetAreas)
					{
						Rectangle r1 = cutRectangleToVisibleArea(offsetX, offsetY, ta.area, img);
						g.setColor(Color.BLACK);
						g.setStroke(Assist.dashed1);
						g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
						g.setColor(Color.YELLOW);
						g.setStroke(Assist.dashed2);
						g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
					}
				}

				g.dispose();
			}

			if(ignored)
			{
				ImageIO.write(img, "png", new File("." + File.separator +Assist.date+ File.separator  + "ID_" + ID + "_" + type+ "_" + site + "_" + viewMin + "px_"+ viewMax + "px_ignored_" +i+ ".png"));
			}
			else if (falsePositive)
			{
				ImageIO.write(img, "png", new File("." + File.separator +Assist.date+ File.separator  + "ID_" + ID + "_" + type+ "_" + site + "_" + viewMin + "px_"+ viewMax + "px_FP_" +i+".png"));
			}
			else if(NOI)
			{
				ImageIO.write(img, "png", new File("." + File.separator +Assist.date+ File.separator  + "ID_" + ID + "_" + type+ "_" + site + "_" + viewMin + "px_"+ viewMax + "px_NOI_" +i+ ".png"));

			}else
			{
				ImageIO.write(img, "png", new File("." + File.separator +Assist.date+ File.separator  + "ID_" + ID + "_" + type+ "_" + site + "_" + viewMin + "px_"+ viewMax + "px_TP_" +i+".png"));
			}
		}
	}
	public Rectangle cutRectangleToVisibleArea(int offsetX, int offsetY, Rectangle rectangle, BufferedImage img) {
		int newX = rectangle.getX() - offsetX;
		int newY = rectangle.getY() - offsetY;
		int newW = rectangle.getWidth();
		int newH = rectangle.getHeight();
		if(newX < 0 || newY < 0 || newX + newW > img.getWidth() || newY + newH > img.getHeight())
		{
			String note = "Changing coordinates XYHW(" + newX + "," + newY + "," + newH + "," + newW + ") ";
			if(newX < 0)
			{
				newW = newW + newX;
				newX = 0;
			}
			if(newY < 0)
			{
				newH = newH + newY;
				newY = 0;
			}
			if(newX + newW > img.getWidth())
			{
				newW = newW - ((newX + newW) - img.getWidth());
//				newW = img.getWidth() - newX;
			}
			if(newY + newH > img.getHeight())
			{
				newH = newH - ((newY + newH) - img.getHeight());
//				newH = img.getHeight() - newY;
			}
			note = note + "to XYHW(" + newX + "," + newY + "," + newH + "," + newW + ") ";
			//System.out.println(note);
			titles.add("Rectangle (View) Size Warning:");
			notes.add(note);
		}
		Rectangle r = new Rectangle(newX,newY,newH,newW);
		return r;
	}

	public void setFalsePositive(boolean falsePositive) {
		this.falsePositive = falsePositive;
		NOI = false;
	}
	public void setIgnore(boolean bool) {
		this.ignored = bool;
		NOI = false;
	}
}
