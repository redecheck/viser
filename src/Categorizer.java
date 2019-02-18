

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;



public class Categorizer
{
	ArrayList<Webpage> webpages;
	DriverManager dm;
	String reportsPath;
	boolean featureHiddenDetection;
	
	public Categorizer(String reportsPath, boolean featureHiddenDetection)
	{
		dm = new DriverManager("firefox");
		this.reportsPath = reportsPath;
		this.featureHiddenDetection = featureHiddenDetection;
	}
	public void lookForNOI2()
	{
		BufferedImage screenshot;		
		for(Webpage wp: webpages)
		{
				for(Failure lf : wp.Failures)
				{
					if(lf.type.equals("protrusion") || lf.type.equals("viewport") || lf.type.equals("collision"))
					{
						System.out.println("Verifying Failure ID: " + lf.ID);
						if(!dm.getURL().contains(wp.wPagePath.replace("\\", "/"))) //check if web site is already loaded
						{
							dm.navigate("file://" + wp.wPagePath);
						}
						
						dm.setViewport(lf.captureView);
						lf.viewMaxWidth = dm.getWindowWidth();
						lf.viewMaxHeight = dm.getWindowHeight();
						lf.startTime();
						findWebElementsAddRectangles(lf);
						//printSiteThenExit(wp, lf, false);
						String old1 = dm.getOpacity(lf.wbElements.get(0));
						String old2 = dm.getOpacity(lf.wbElements.get(1));
						
						if(lf.rectangles.size() > 1)
						{
						dm.scroll(lf.rectangles.get(1));
						lf.FirstImageScrollOffsetX = dm.scrollX;
						lf.FirstImageScrollOffsetY = dm.scrollY;
						lf.imgs.add(dm.screenshot());
						dm.setOpacity(lf.wbElements.get(1), "0");
						lf.imgs.add(dm.screenshot());
						dm.setOpacity(lf.wbElements.get(1), old2);
						}
						else
						{
							BufferedImage ss = dm.screenshot();
							lf.imgs.add(ss);
							lf.imgs.add(ss);
							lf.endTime();
							continue;
						}
						
						if(lf.xpaths.get(0).equals(lf.xpaths.get(1)))
						{
							lf.titles.add("xpath error:");
							lf.notes.add("The program was asked to compare the same element to self");
							lf.setIgnore(true);
							try 
							{
								lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
								System.out.println("Something went wrong... the program was asked to compare the same element to self");
							} catch (IOException e) {
								e.printStackTrace();
							}
							lf.endTime();
							continue;
						}

						
						lf.findAreasOfConcern();
						if(lf.ignored || lf.falsePositive)
						{

							try {
								lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
							} catch (IOException e) {
								e.printStackTrace();
							}
							if(lf.bestEffort)
							{
								finalReach(lf, wp.siteName);
							}
							lf.endTime();
							continue;
						}
						if(lf.type.equals("collision"))
						{
							if(lf.segregated)
							{
								try {
									lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							if(lf.overlapping)
							{
								//System.out.println("Area Rect: X("+ lf.overlappedArea.area.x + "," + lf.overlappedArea.area.y + ")Y  W("+ lf.overlappedArea.area.width + "," + lf.overlappedArea.area.height + ")H");
								for(TargetArea targetArea: lf.overlappedArea.targetAreas)
								{
									//System.out.println("Targer Area Rect: X("+ targetArea.area.x + "," + targetArea.area.y + ")Y  W("+ targetArea.area.width + "," + targetArea.area.height + ")H");
									dm.scroll(targetArea.area);

									if(lf.HTMLParentRelationship)
									{
										dm.setOpacity(lf.wbElements.get(0), "0");
										dm.setOpacity(lf.wbElements.get(1), "0");
										screenshot = dm.screenshot();
										targetArea.targetImgs.add(Assist.getSubImage(screenshot,targetArea.area,dm.scrollX,dm.scrollY, lf)); //background image meaning both elements not visible
										dm.setOpacity(lf.wbElements.get(0), old1);
										screenshot = dm.screenshot();
										targetArea.targetImgs.add(Assist.getSubImage(screenshot,targetArea.area,dm.scrollX,dm.scrollY, lf)); //parent only visible
										dm.setOpacity(lf.wbElements.get(1), old2);
										screenshot = dm.screenshot();
										targetArea.targetImgs.add(Assist.getSubImage(screenshot,targetArea.area,dm.scrollX,dm.scrollY, lf)); //both elements visible
									}
									else //we dont know which one is on top so display each element in isolation.
									{
										//Change both elements
										dm.setOpacity(lf.wbElements.get(0), "0");
										dm.setOpacity(lf.wbElements.get(1), "0");
										screenshot = dm.screenshot();
										targetArea.targetImgs.add(Assist.getSubImage(screenshot, targetArea.area,dm.scrollX,dm.scrollY,lf));
										dm.setOpacity(lf.wbElements.get(0), old1);
										dm.setOpacity(lf.wbElements.get(1), old2);
										
										//Change first element
										dm.setOpacity(lf.wbElements.get(0), "0");
										screenshot = dm.screenshot();
										targetArea.targetImgs.add(Assist.getSubImage(screenshot, targetArea.area,dm.scrollX,dm.scrollY,lf));
										dm.setOpacity(lf.wbElements.get(0), old1);

										//Change second element
										dm.setOpacity(lf.wbElements.get(1), "0");
										screenshot = dm.screenshot();
										targetArea.targetImgs.add(Assist.getSubImage(screenshot, targetArea.area,dm.scrollX,dm.scrollY,lf));
										dm.setOpacity(lf.wbElements.get(1), old2);
										
										//Both images rendered to make sure the final version shows both image changes
										screenshot = dm.screenshot();
										targetArea.targetImgs.add(Assist.getSubImage(screenshot, targetArea.area,dm.scrollX,dm.scrollY,lf));
									}

									


									
									
					
									
									if(targetArea.checkAreaOfImgs())
									{
//										if(targetArea.imgCollisionCheck())
										if(targetArea.pixelCheck(lf.HTMLParentRelationship,true,featureHiddenDetection))
										{
											lf.NOI = false;
											try 
											{
												lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
											} catch (IOException e) {
												e.printStackTrace();
											}
											break;
										}
									}
									else
									{
										lf.titles.add("Collision Size Error:");
										lf.notes.add("Image dimensions are not the same and cannot compare... ignoring failure..");
										lf.setIgnore(true);
										try 
										{
											lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
											System.out.println("Image dimensions are not the same and cannot compare... ignoring failure..");
										} catch (IOException e) {
											e.printStackTrace();
										}
										break;
									}
								}
							}
							try 
							{
								lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						else if((lf.type.equals("protrusion") || lf.type.equals("viewport")))
						{

							boolean outsideChange = false;
							boolean insideChange = false;




							if(lf.segregated)
							{
								for(TargetArea targetArea: lf.segregatedArea.targetAreas)
								{
									dm.scroll(targetArea.area);
									dm.setOpacity(lf.wbElements.get(1), "0");
									screenshot = dm.screenshot();
									targetArea.targetImgs.add(Assist.getSubImage(screenshot, targetArea.area,dm.scrollX,dm.scrollY,lf));
									dm.setOpacity(lf.wbElements.get(1), old2);
									screenshot = dm.screenshot();
									targetArea.targetImgs.add(Assist.getSubImage(screenshot, targetArea.area,dm.scrollX,dm.scrollY,lf));
									outsideChange = targetArea.anyChange();
									if(outsideChange)
									{
										lf.NOI = false;
										try 
										{
											lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
										} catch (IOException e) {
											e.printStackTrace();
										}
										break;
									}
								}		
							}
							else if(lf.protruding)
							{
								//check if the parent changes the area where both meet
								for(TargetArea insideTargetArea: lf.overlappedArea.targetAreas)
								{

									dm.scroll(insideTargetArea.area);

									
									if(lf.HTMLParentRelationship)
									{
										dm.setOpacity(lf.wbElements.get(1), "0");
										screenshot = dm.screenshot();
										insideTargetArea.targetImgs.add(Assist.getSubImage(screenshot,insideTargetArea.area,dm.scrollX,dm.scrollY, lf)); //parent only visible
										dm.setOpacity(lf.wbElements.get(0), "0");
										screenshot = dm.screenshot();
										insideTargetArea.targetImgs.add(Assist.getSubImage(screenshot,insideTargetArea.area,dm.scrollX,dm.scrollY, lf)); //background image meaning both elements not visible
										dm.setOpacity(lf.wbElements.get(0), old1);
										dm.setOpacity(lf.wbElements.get(1), old2);
										screenshot = dm.screenshot();
										insideTargetArea.targetImgs.add(Assist.getSubImage(screenshot,insideTargetArea.area,dm.scrollX,dm.scrollY, lf)); //both elements visible
									}
									else //we dont know which one is on top so display each element in isolation.
									{
										//Change second element
										dm.setOpacity(lf.wbElements.get(1), "0");
										screenshot = dm.screenshot();
										insideTargetArea.targetImgs.add(Assist.getSubImage(screenshot, insideTargetArea.area,dm.scrollX,dm.scrollY,lf));
										
										//Change both elements
										dm.setOpacity(lf.wbElements.get(0), "0");
										screenshot = dm.screenshot();
										insideTargetArea.targetImgs.add(Assist.getSubImage(screenshot, insideTargetArea.area,dm.scrollX,dm.scrollY,lf));
										dm.setOpacity(lf.wbElements.get(1), old2);
										
										//Change first element
										screenshot = dm.screenshot();
										insideTargetArea.targetImgs.add(Assist.getSubImage(screenshot, insideTargetArea.area,dm.scrollX,dm.scrollY,lf));
										dm.setOpacity(lf.wbElements.get(0), old1);
									}
									
									
									insideChange = insideTargetArea.pixelCheck(lf.HTMLParentRelationship, false, featureHiddenDetection);  
									for(Area area : lf.protrudingArea)
									{
										for(TargetArea outsideTargetArea: area.targetAreas)
										{
											dm.scroll(outsideTargetArea.area);
											dm.setOpacity(lf.wbElements.get(1), "0");
											screenshot = dm.screenshot();
											outsideTargetArea.targetImgs.add(Assist.getSubImage(screenshot, outsideTargetArea.area,dm.scrollX,dm.scrollY,lf));
											dm.setOpacity(lf.wbElements.get(1), old2);
											screenshot = dm.screenshot();
											outsideTargetArea.targetImgs.add(Assist.getSubImage(screenshot, outsideTargetArea.area,dm.scrollX,dm.scrollY,lf));

											outsideChange = outsideTargetArea.anyChange();
											if(outsideChange && insideChange)
											{
												lf.NOI = false;
												break;
											}
										}
										if((outsideChange && insideChange) || (lf.ignored))
										{
											break;
										}

									}
									lf.titles.add("Inside Change Note:");
									if(insideChange)
									{
										lf.notes.add("Yes - Overlapping pixels");
									}
									else
									{
							        	lf.notes.add("NO - Overlapping pixels");
									}
									
									lf.titles.add("Outside Change Note:");
									if(outsideChange)
									{
										lf.notes.add("Yes - There was a change between plain background and after adding the protruding child");
									}
									else
									{
							        	lf.notes.add("NO - There was no change between plain background and after adding the protruding child");
									}
									if(outsideChange && insideChange)
									{
										break;
									}
									
								}
								
							}
							try 
							{
								lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						if(lf.bestEffort)
						{
							finalReach(lf, wp.siteName);
						}
						
						
						
					}
					lf.endTime();
				}
		}

	}

	public void finalReach(Failure lf, String siteName)
	{
		if(lf.bestEffort)//reach only if there was an attempt to categorize without modifying the location of elements.
		{
			if((lf.falsePositive || lf.NOI || lf.ignored) && lf.type.equals("viewport")) //if it is true positive then there is no need to check. 
			{
				lf.unreachableDimensions(dm);
				lf.titles.add("ReCategorization Attempt:");
				lf.notes.add("This failure is a candidate for ReCat");
				for(int xpathID: lf.problemXpathID)
				{			
					if(lf.reachRightDim != null || lf.reachBottomDim != null || lf.reachLeftDim != null || lf.reachTopDim != null)
					{
//						System.out.println("ID        : " + lf.ID);
//						System.out.println("Fault Type: " + lf.type);
//						System.out.println("xpath 0   : " + lf.xpaths.get(0));
//						System.out.println("xpath 1   : " + lf.xpaths.get(1));
//						System.out.println("Problem xpath current: " + lf.xpaths.get(xpathID));
//						System.out.println("Problem xpath count  : " + lf.problemXpathID.size());
						
						WebElement wb = dm.getWebElem(lf.xpaths.get(xpathID));
						WebElement body = dm.getWebElem("/HTML/BODY");
						String wbStyle = wb.getAttribute("style");
						String bodyStyle = body.getAttribute("style");
						Rectangle wbRectangle = new Rectangle(wb.getLocation(),wb.getSize());

						lf.titles.add("ReCat xpath:");
						lf.notes.add(lf.xpaths.get(xpathID));
						lf.titles.add("ReCat Original Rectangle:");
						lf.notes.add("Original XYHW(" + wbRectangle.x + "," + wbRectangle.y + "," + wbRectangle.height + "," + wbRectangle.width + ") ");
						
						

						if(lf.reachRightDim != null && (lf.falsePositive || lf.NOI || lf.ignored))
						{
							lf.titles.add("ReCat Target Rectangle:");
							lf.notes.add("Target XYHW(" + wbRectangle.x + "," + wbRectangle.y + "," + lf.reachRightDim.height + "," + lf.reachRightDim.width + ") ");
							finalReachRight(lf, siteName, wb, body, wbRectangle);
							dm.setStyle(wb, wbStyle);
							dm.setStyle(body, bodyStyle);
						}
						if(lf.reachBottomDim != null && (lf.falsePositive || lf.NOI || lf.ignored))
						{
							lf.titles.add("ReCat Target Rectangle:");
							lf.notes.add("Target XYHW(" + wbRectangle.x + "," + wbRectangle.y + "," + lf.reachBottomDim.height + "," + lf.reachBottomDim.width + ") ");
							finalReachBottom(lf, siteName, wb, body, wbRectangle);
							dm.setStyle(wb, wbStyle);
							dm.setStyle(body, bodyStyle);
						}
						if(lf.reachLeftDim != null && (lf.falsePositive || lf.NOI || lf.ignored))
						{
							lf.titles.add("ReCat Target Rectangle:");
							lf.notes.add("Target XYHW(" + wbRectangle.x + "," + wbRectangle.y + "," + lf.reachLeftDim.height + "," + lf.reachLeftDim.width + ") ");
							finalReachLeft(lf, siteName, wb, body, wbRectangle);
							dm.setStyle(wb, wbStyle);
							dm.setStyle(body, bodyStyle);
							//Assist.pause("Left ID: " + lf.ID);
						}
						if(lf.reachTopDim != null && (lf.falsePositive || lf.NOI || lf.ignored))
						{
							lf.titles.add("ReCat Target Rectangle:");
							lf.notes.add("Target XYHW(" + wbRectangle.x + "," + wbRectangle.y + "," + lf.reachTopDim.height + "," + lf.reachTopDim.width + ") ");
							finalReachTop(lf, siteName, wb, body, wbRectangle);
							dm.setStyle(wb, wbStyle);
							dm.setStyle(body, bodyStyle);
							//Assist.pause("Top ID: " + lf.ID);
						}
					}
				}
			}else
			{
				lf.bestEffort = false; //There was no attempt to move the elements since it is already a TP.
				lf.titles.add("Unscrollable Area Not Needed:");
				if(lf.type.equals("viewport"))
				{
					lf.notes.add("This failure had some content unreachable by scrolling but it was successufully classified as TP without programatically moving the elements.");
				}else
				{
					lf.notes.add("This failure had some content unreachable by scrolling but it is not a viewport failure hence it was ignored.");
				}
			}
		}
	}
	public void finalReachTop(Failure lf, String siteName, WebElement wb, WebElement body, Rectangle wbRectangle) {
		if(lf.falsePositive == false && lf.ignored == false && lf.NOI == false ) //if true positive don't reach
			return;
		String bodyMarginTop = dm.getMarginTop(body);					
		String wbMarginTop = dm.getMarginTop(wb);
		String wbHeight = dm.getHeight(wb);
		String wbWidth = dm.getWidth(wb);
		String wbPosition = dm.getPosition(wb);
		String wbTop = dm.getTop(wb);
		String opacity = dm.getOpacity(wb);
		BufferedImage screenshot;
		
		dm.setWidth(wb, wbWidth);
		dm.setHeight(wb, wbHeight);

		lf.titles.add("ReCat Criteria:");		
		lf.notes.add("Cant reach Top by("+lf.reachTopDim.height+")");
		Rectangle topR = renewTopRectangle(lf, wb);
		String wbMoveBy = Integer.toString(Integer.parseInt(wbMarginTop.substring(0, wbMarginTop.length()-2)) + lf.reachTopDim.height)+"px";
		String wbMoveByTop;
		
		if(wbTop.substring(wbTop.length()-2, wbTop.length()).equals("px"))
		{
			wbMoveByTop = Integer.toString(Integer.parseInt(wbTop.substring(0, wbTop.length()-2)) + lf.reachTopDim.height)+"px";
		}
		else
		{
			wbMoveByTop = lf.reachTopDim.height + "px";
		}
//		if(!wbPosition.equals("static"))
//		{
//			dm.setMarginTop(body, wbMoveByTop);
//			topR = renewTopRectangle(lf, wb);
//			lf.titles.add("ReCat Move:");
//			lf.notes.add("Body moved by ("+ wbMoveByTop +") new coordinates XYHW(" + topR.x + "," + topR.y + "," + topR.height + "," + topR.width + ") ");
//		}
		if(topR.y < 0)
		{
			if(wbPosition.equals("static"))
			{
				dm.setMarginTop(wb, wbMoveBy);
				topR = renewTopRectangle(lf, wb);
				lf.titles.add("ReCat Move:");
				lf.notes.add("Element moved by ("+wbMoveBy+") new coordinates XYHW(" + topR.x + "," + topR.y + "," + topR.height + "," + topR.width + ") ");
			}
			else
			{
				dm.setTop(wb, wbMoveByTop);
				topR = renewTopRectangle(lf, wb);
				lf.titles.add("ReCat Move:");
				lf.notes.add("Element moved by ("+wbMoveByTop+") new coordinates XYHW(" + topR.x + "," + topR.y + "," + topR.height + "," + topR.width + ") ");
			}
		}	
		TargetArea ta = new TargetArea(topR);
		if(topR.y == 0 && wbRectangle.width == wb.getSize().width && wbRectangle.height == wb.getSize().height)
		{
				dm.scroll(ta.area);
				dm.setOpacity(wb, "0");
				screenshot = dm.screenshot();
				ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
				dm.setOpacity(wb, opacity);
				screenshot = dm.screenshot();
				ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
				lf.printFlagT = true;
				if(ta.anyChange())
				{

					lf.newCat("TP");
					lf.saveRecatImages(screenshot, dm, siteName, ta, "TP", "T");
				}
				else
				{
					lf.newCat("NOI");
					lf.saveRecatImages(screenshot, dm, siteName, ta, "NOI", "T");
				}
			
		}
		else
		{
			screenshot = dm.screenshot();
			lf.titles.add("ReCat fail:");
			lf.notes.add("Element coordinates XYHW(" + topR.x + "," + topR.y + "," + topR.height + "," + topR.width + ") "); 
			if(topR.height > 0 && topR.width > 0 && lf.falsePositive)
			{
				lf.newCat("NOI");
				try {
					lf.saveImages(siteName, lf.FirstImageScrollOffsetX, lf.FirstImageScrollOffsetY);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			ta = null;
			lf.saveRecatImages(screenshot, dm, siteName, ta, "FailedReach", "T");
		}

	}
	public void finalReachLeft(Failure lf, String siteName, WebElement wb, WebElement body, Rectangle wbRectangle) {
		if(lf.falsePositive == false && lf.ignored == false && lf.NOI == false ) //if true positive don't reach
			return;
		String bodyMarginLeft = dm.getMarginLeft(body);					
		String wbMarginLeft = dm.getMarginLeft(wb);
		String wbHeight = dm.getHeight(wb);
		String wbWidth = dm.getWidth(wb);
		String wbPosition = dm.getPosition(wb);
		String wbLeft = dm.getLeft(wb);
		String opacity = dm.getOpacity(wb);
		BufferedImage screenshot;
		
		dm.setWidth(wb, wbWidth);
		dm.setHeight(wb, wbHeight);

		lf.titles.add("ReCat Criteria:");		
		lf.notes.add("Cant reach Left by("+lf.reachLeftDim.width+")");
		Rectangle LeftR = renewLeftRectangle(lf, wb);
		String wbMoveBy = Integer.toString(Integer.parseInt(wbMarginLeft.substring(0, wbMarginLeft.length()-2)) + lf.reachLeftDim.width)+"px";
		String wbMoveByLeft;
		
		if(wbLeft.substring(wbLeft.length()-2, wbLeft.length()).equals("px"))
		{
			wbMoveByLeft = Integer.toString(Integer.parseInt(wbLeft.substring(0, wbLeft.length()-2)) + lf.reachLeftDim.width)+"px";
		}
		else
		{
			wbMoveByLeft = lf.reachLeftDim.width + "px";
		}
//		if(!wbPosition.equals("static"))
//		{
//			dm.setMarginLeft(body, wbMoveByLeft);
//			LeftR = renewLeftRectangle(lf, wb);
//			lf.titles.add("ReCat Move:");
//			lf.notes.add("Body moved by ("+ wbMoveByLeft +") new coordinates XYHW(" + LeftR.x + "," + LeftR.y + "," + LeftR.height + "," + LeftR.width + ") ");
//		}
		if(LeftR.x < 0)
		{
			if(wbPosition.equals("static"))
			{
				dm.setMarginLeft(wb, wbMoveBy);
				LeftR = renewLeftRectangle(lf, wb);
				lf.titles.add("ReCat Move:");
				lf.notes.add("Element moved by ("+wbMoveBy+") new coordinates XYHW(" + LeftR.x + "," + LeftR.y + "," + LeftR.height + "," + LeftR.width + ") ");
			}
			else
			{
				dm.setLeft(wb, wbMoveByLeft);
				LeftR = renewLeftRectangle(lf, wb);
				lf.titles.add("ReCat Move:");
				lf.notes.add("Element moved by ("+wbMoveByLeft+") new coordinates XYHW(" + LeftR.x + "," + LeftR.y + "," + LeftR.height + "," + LeftR.width + ") ");
			}
		}	
		TargetArea ta = new TargetArea(LeftR);
		if(LeftR.x == 0 && wbRectangle.width == wb.getSize().width && wbRectangle.height == wb.getSize().height)
		{
				dm.scroll(ta.area);
				dm.setOpacity(wb, "0");
				screenshot = dm.screenshot();
				ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
				dm.setOpacity(wb, opacity);
				screenshot = dm.screenshot();
				ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
				lf.printFlagL = true;
				if(ta.anyChange())
				{

					lf.newCat("TP");
					lf.saveRecatImages(screenshot, dm, siteName, ta, "TP", "L");
				}
				else
				{
					lf.newCat("NOI");
					lf.saveRecatImages(screenshot, dm, siteName, ta, "NOI", "L");
				}
			
		}
		else
		{
			screenshot = dm.screenshot();
			lf.titles.add("ReCat fail:");
			lf.notes.add("Element coordinates XYHW(" + LeftR.x + "," + LeftR.y + "," + LeftR.height + "," + LeftR.width + ") "); 
			if(LeftR.height > 0 && LeftR.width > 0 && lf.falsePositive)
			{
				lf.newCat("NOI");
				try {
					lf.saveImages(siteName, lf.FirstImageScrollOffsetX, lf.FirstImageScrollOffsetY);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			ta = null;
			lf.saveRecatImages(screenshot, dm, siteName, ta, "FailedReach", "L");
		}

	}
	public void finalReachRight(Failure lf, String siteName, WebElement wb, WebElement body, Rectangle wbRectangle) {
		if(lf.falsePositive == false && lf.ignored == false && lf.NOI == false ) //if true positive don't reach
			return;
		String bodyMarginLeft = dm.getMarginLeft(body);					
		String wbMarginLeft = dm.getMarginLeft(wb);
		String wbHeight = dm.getHeight(wb);
		String wbWidth = dm.getWidth(wb);
		String wbPosition = dm.getPosition(wb);
		String wbLeft = dm.getLeft(wb);
		String opacity = dm.getOpacity(wb);
		BufferedImage screenshot;
		
		dm.setWidth(wb, wbWidth);
		dm.setHeight(wb, wbHeight);

		Rectangle rightR = renewRightRectangle(lf, wb);
		String wbMoveBy = Integer.toString(Integer.parseInt(wbMarginLeft.substring(0, wbMarginLeft.length()-2)) - lf.reachRightDim.width)+"px";
		String wbMoveByLeft;
		if(wbLeft.substring(wbLeft.length()-2, wbLeft.length()).equals("px"))
		{
			wbMoveByLeft = Integer.toString(Integer.parseInt(wbLeft.substring(0, wbLeft.length()-2)) - lf.reachRightDim.width)+"px";
		}
		else
		{
			wbMoveByLeft = -lf.reachRightDim.width + "px";
		}

		if(wbPosition.equals("static"))
		{
			dm.setMarginLeft(wb, wbMoveBy);
			rightR = renewRightRectangle(lf, wb);
			lf.titles.add("ReCat Move:");
			lf.notes.add("Element moved by ("+wbMoveBy+") new coordinates XYHW(" + rightR.x + "," + rightR.y + "," + rightR.height + "," + rightR.width + ") ");
		}
		else
		{
			dm.setLeft(wb, wbMoveByLeft);
			rightR = renewRightRectangle(lf, wb);
			lf.titles.add("ReCat Move:");
			lf.notes.add("Element moved by ("+wbMoveByLeft+") new coordinates XYHW(" + rightR.x + "," + rightR.y + "," + rightR.height + "," + rightR.width + ") ");
		}
		dm.scroll(rightR);

		TargetArea ta = new TargetArea(rightR);
		if(dm.scrollX == 0 && wbRectangle.width == wb.getSize().width && wbRectangle.height == wb.getSize().height)
		{
				dm.scroll(ta.area);
				dm.setOpacity(wb, "0");
				screenshot = dm.screenshot();
				ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
				dm.setOpacity(wb, opacity);
				screenshot = dm.screenshot();
				ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
				lf.printFlagR = true;
				if(ta.anyChange())
				{

					lf.newCat("TP");
					lf.saveRecatImages(screenshot, dm, siteName, ta, "TP", "R");
				}
				else
				{
					lf.newCat("NOI");
					lf.saveRecatImages(screenshot, dm, siteName, ta, "NOI", "R");
				}
			
		}
		else
		{
			screenshot = dm.screenshot();
			lf.titles.add("ReCat fail:");
			lf.notes.add("Element coordinates XYHW(" + rightR.x + "," + rightR.y + "," + rightR.height + "," + rightR.width + ") "); 
			if(rightR.height > 0 && rightR.width > 0 && lf.falsePositive)
			{
				lf.newCat("NOI");
				try {
					lf.saveImages(siteName, lf.FirstImageScrollOffsetX, lf.FirstImageScrollOffsetY);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			ta = null;
			lf.saveRecatImages(screenshot, dm, siteName, ta, "FailedReach", "R");
		}

	}
	public void finalReachBottom(Failure lf, String siteName, WebElement wb, WebElement body, Rectangle wbRectangle) {
		if(lf.falsePositive == false && lf.ignored == false && lf.NOI == false ) //if true positive don't reach
			return;
		String wbMarginTop = dm.getMarginTop(wb);
		String wbHeight = dm.getHeight(wb);
		String wbWidth = dm.getWidth(wb);
		String wbPosition = dm.getPosition(wb);
		String wbTop = dm.getTop(wb);
		String opacity = dm.getOpacity(wb);
		BufferedImage screenshot;
		
		dm.setWidth(wb, wbWidth);
		dm.setHeight(wb, wbHeight);

		Rectangle bottomR = renewBottomRectangle(lf, wb);
		String wbMoveBy = Integer.toString(Integer.parseInt(wbMarginTop.substring(0, wbMarginTop.length()-2)) - lf.reachBottomDim.height)+"px";
		String wbMoveByTop;
		if(wbTop.substring(wbTop.length()-2, wbTop.length()).equals("px"))
		{
			wbMoveByTop = Integer.toString(Integer.parseInt(wbTop.substring(0, wbTop.length()-2)) - lf.reachBottomDim.height)+"px";
		}
		else
		{
			wbMoveByTop = -lf.reachBottomDim.height + "px";
		}

		if(wbPosition.equals("static"))
		{
			dm.setMarginTop(wb, wbMoveBy);
			bottomR = renewBottomRectangle(lf, wb);
			lf.titles.add("ReCat Move:");
			lf.notes.add("Element moved by ("+wbMoveBy+") new coordinates XYHW(" + bottomR.x + "," + bottomR.y + "," + bottomR.height + "," + bottomR.width + ") ");
		}
		else
		{
			dm.setTop(wb, wbMoveByTop);
			bottomR = renewBottomRectangle(lf, wb);
			lf.titles.add("ReCat Move:");
			lf.notes.add("Element moved by ("+wbMoveByTop+") new coordinates XYHW(" + bottomR.x + "," + bottomR.y + "," + bottomR.height + "," + bottomR.width + ") ");
		}
		dm.scroll(bottomR);

		TargetArea ta = new TargetArea(bottomR);
		if(dm.scrollY == 0 && wbRectangle.width == wb.getSize().width && wbRectangle.height == wb.getSize().height)
		{
				dm.scroll(ta.area);
				dm.setOpacity(wb, "0");
				screenshot = dm.screenshot();
				ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
				dm.setOpacity(wb, opacity);
				screenshot = dm.screenshot();
				ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
				lf.printFlagB = true;
				if(ta.anyChange())
				{
					lf.newCat("TP");
					lf.saveRecatImages(screenshot, dm, siteName, ta, "TP", "B");
				}
				else
				{
					lf.newCat("NOI");
					lf.saveRecatImages(screenshot, dm, siteName, ta, "NOI", "B");
				}
			
		}
		else
		{
			screenshot = dm.screenshot();
			lf.titles.add("ReCat fail:");
			lf.notes.add("Element coordinates XYHW(" + bottomR.x + "," + bottomR.y + "," + bottomR.height + "," + bottomR.width + ") "); 
			if(bottomR.height > 0 && bottomR.width > 0 && lf.falsePositive)
			{
				lf.newCat("NOI");
				try {
					lf.saveImages(siteName, lf.FirstImageScrollOffsetX, lf.FirstImageScrollOffsetY);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			ta = null;
			lf.saveRecatImages(screenshot, dm, siteName, ta, "FailedReach", "B");
		}

	}
	private Rectangle renewTopRectangle(Failure lf, WebElement wb)
	{
		Rectangle topR;
		topR = new Rectangle(wb.getLocation(),wb.getSize());
		//topR.setY((topR.y+topR.height)-lf.reachTopDim.height);
		topR.setHeight(lf.reachTopDim.height);
		topR.setWidth(lf.reachTopDim.width);
		lf.titles.add("ReCat Target Rectangle:");
		lf.notes.add("Target XYHW(" + topR.x + "," + topR.y + "," + topR.height + "," + topR.width + ") ");
		return topR;
	}
	private Rectangle renewBottomRectangle(Failure lf, WebElement wb)
	{
		Rectangle bottomR;
		bottomR = new Rectangle(wb.getLocation(),wb.getSize());
		bottomR.setY((bottomR.y+bottomR.height)-lf.reachBottomDim.height);
		bottomR.setHeight(lf.reachBottomDim.height);
		bottomR.setWidth(lf.reachBottomDim.width);
		lf.titles.add("ReCat Target Rectangle:");
		lf.notes.add("Target XYHW(" + bottomR.x + "," + bottomR.y + "," + bottomR.height + "," + bottomR.width + ") ");
		return bottomR;
	}
	private Rectangle renewRightRectangle(Failure lf, WebElement wb)
	{
		Rectangle rightR;
		rightR = new Rectangle(wb.getLocation(),wb.getSize());
		rightR.setX((rightR.x+rightR.width)-lf.reachRightDim.width);
		rightR.setHeight(lf.reachRightDim.height);
		rightR.setWidth(lf.reachRightDim.width);
		lf.titles.add("ReCat Target Rectangle:");
		lf.notes.add("Target XYHW(" + rightR.x + "," + rightR.y + "," + rightR.height + "," + rightR.width + ") ");
		return rightR;
	}
	private Rectangle renewLeftRectangle(Failure lf, WebElement wb)
	{
		Rectangle leftR;
		leftR = new Rectangle(wb.getLocation(),wb.getSize());
		//leftR.setY((leftR.y+leftR.height)-lf.reachLeftDim.height);
		leftR.setHeight(lf.reachLeftDim.height);
		leftR.setWidth(lf.reachLeftDim.width);
		lf.titles.add("ReCat Target Rectangle:");
		lf.notes.add("Target XYHW(" + leftR.x + "," + leftR.y + "," + leftR.height + "," + leftR.width + ") ");
		return leftR;
	}

	
	public void outputHTMLSite() throws IOException
	{
		for(Webpage wp: webpages)
		{
				for(Failure lf : wp.Failures)
				{
					lf.writeImages(wp.siteName);
				}
		}
		htmlout html = new htmlout();
		dm.navigate("file://" + new File(".").getCanonicalFile()+ "/"+Assist.date+"/" + html.gallary(webpages));
		dm.maximize();
//		dm.shutdown();
	}
    public void writeReport()
    {
        String fileName = "." + File.separator + Assist.date + File.separator + "report.csv";
        try 
        {
            PrintWriter writer;
            writer = new PrintWriter(fileName);
            writer.println("UID,Failure,Site,Range,Categorization,Prior Cat,Best Effort,Screenshot 1,Screenshot 2,XPath 1,XPath 2,Rect 1 (X*Y*H*W),Rect 2 (X*Y*H*W),Runtime Millisec");
            for(Webpage wp: webpages)
            {
                for(Failure lf : wp.Failures)
                {
                    if(lf.type.equals("collision") || lf.type.equals("viewport") || lf.type.equals("protrusion"))
                    {
                    	String result;
                    	String screenshotFile = "file:///C:/Results/"+Assist.date+"/"; 
                    	String screenshotFile2 = "file:///C:/Results/"+Assist.date+"/"; 
                    	String bestEffort = "No";
                    	String priorCat = lf.priorCat;
                    	String xpaths = "";
                    	for(String xpath:lf.xpaths)
                    	{
                    		xpaths = xpaths + "," + xpath;
                    	}

                    	String rect1 = "("+ lf.orignalRectangles.get(0).x+"*" + lf.orignalRectangles.get(0).y +"*" + lf.orignalRectangles.get(0).height +"*"+ lf.orignalRectangles.get(0).width +")";
                		String rect2 = "("+ lf.orignalRectangles.get(1).x+"*" + lf.orignalRectangles.get(1).y +"*" + lf.orignalRectangles.get(1).height +"*"+ lf.orignalRectangles.get(1).width +")";
                		String rectangles = "," + rect1 + "," + rect2;
                    
                    	if(lf.ignored)
                    	{
                    		result = "Ignored";
                    		screenshotFile = screenshotFile + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_ignored_" +0+ ".png";
                    		screenshotFile2 = screenshotFile2 + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_ignored_" +1+ ".png";

                    	}else if(lf.NOI)
                        {
                    		result = "NOI";
                    		screenshotFile = screenshotFile + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_NOI_" +0+ ".png";
                    		screenshotFile2 = screenshotFile2 + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_NOI_" +1+ ".png";

                        }else if(lf.falsePositive)
                        {
                        	result = "FP";
                        	screenshotFile = screenshotFile + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_FP_" +0+".png";
                        	screenshotFile2 = screenshotFile2 + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_FP_" +1+".png";

                        }else
                        {
                        	result = "TP";
                        	screenshotFile = screenshotFile + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_TP_" +0+".png";
                        	screenshotFile2 = screenshotFile2 + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_TP_" +1+".png";

                        }
    					if(lf.bestEffort == true)
    					{
    						bestEffort = "Yes";
    					}
    					if(lf.priorCat.equals(""))
    					{
    						priorCat = result;
    					}
    					screenshotFile = "=HYPERLINK(\""+screenshotFile+"\")";
    					screenshotFile2 = "=HYPERLINK(\""+screenshotFile2+"\")";
                        writer.println(lf.ID + "," + lf.type + "," + wp.siteName + "," + lf.viewMin + "px-" + lf.viewMax + "px,"+result + "," + priorCat + "," + bestEffort  + "," + screenshotFile+ "," + screenshotFile2 + xpaths + rectangles+","+lf.getDurationMilli());
                    }
                }
            }
            writer.close();
            
        } catch (IOException e) 
        {    
            System.out.println("could not save report to file ("+fileName+")... exiting");
            e.printStackTrace();
            System.exit(0);
        }
    }

	public void printSiteThenExit(Webpage wp, Failure lf, boolean exit) {
		System.out.println("Site   : " + wp.siteName);
		System.out.println("Page   : " + wp.wPageName);
		System.out.println("Path   : " + wp.wPagePath);
		System.out.println("Run    : " + wp.uniqueRunName);
		System.out.println("Capture: " + lf.captureView);
		System.out.println("Range  : " + lf.viewMin + "-" + lf.viewMax + " px");
		System.out.println("Failure: " + lf.type);
		System.out.println("ID     : " + lf.ID);
		for(int i =0; i < lf.xpaths.size(); i++)
		{
			System.out.println("xpath  : " + lf.xpaths.get(i));
		}
		for(int i =0; i < lf.rectangles.size(); i++)
		{
			System.out.println("Rect "+ i +" : " + "xy(" + lf.rectangles.get(i).getX() + "," + lf.rectangles.get(i).getY()+ ")  hw(" + lf.rectangles.get(i).getHeight() + "," + lf.rectangles.get(i).getWidth() + ")  Area(" + (lf.rectangles.get(i).getHeight()*lf.rectangles.get(i).getWidth())+")");
		}
		if(exit)
		{
			System.out.println("Exiting..... ");
			System.exit(0);
		}
	}
	private void findWebElementsAddRectangles(Failure lf)
	{
		for(int i =0; i < lf.xpaths.size(); i++)
		{

			dm.scrollZero();

			WebElement wb = dm.getWebElem(lf.xpaths.get(i));

			lf.wbElements.add(wb);
			Rectangle r = new Rectangle(lf.wbElements.get(i).getLocation(),lf.wbElements.get(i).getSize());
			Rectangle origR = new Rectangle(lf.wbElements.get(i).getLocation(),lf.wbElements.get(i).getSize());
			lf.orignalRectangles.add(origR);
			lf.titles.add("Original Rectangle Information:");
			lf.notes.add(lf.xpaths.get(i) + "   XYHW(" + r.x + "," + r.y + "," + r.height + "," + r.width + ")");
			
			
			dm.scroll(r);

			if(r.x < 0 || r.y < 0 || dm.cantReachX > 0 || dm.cantReachY > 0)
			{
				lf.bestEffort = true;
				lf.problemXpathID.add(i);

				String note = "*Original coordinates XYHW(" + r.x + "," + r.y + "," + r.height + "," + r.width + ") ";

				r.setWidth(r.width - dm.cantReachX);
				r.setHeight(r.height - dm.cantReachY);
				if(r.x < 0)
				{
					r.setWidth(r.width + r.x);
					r.setX(0);
				}
				if(r.y < 0)
				{

					r.setHeight(r.height + r.y);
					r.setY(0);
				}
				note = note + "to XYHW(" + r.x + "," + r.y + "," + r.height + "," + r.width + ") ";
				//System.out.println(note);
				lf.titles.add("Rectangle (WebElemet) Size Warning:");
				lf.notes.add(note);
			}
			lf.rectangles.add(r);
//			lf.rectangles.add(new Rectangle(newX,newY,newH,newW));
			
			if(wb == null)
			{
				lf.setIgnore(true);
				lf.titles.add("Element Not Found:");
				lf.notes.add("Timeout or Could not find... xpath " + lf.xpaths.get(i));
				System.out.println("Timeout or Could not find... xpath " + lf.xpaths.get(i));
			}
			if(r.height <= 0 || r.width <= 0)
			{
				lf.setIgnore(true);
				lf.titles.add("Element Size Error:");
				lf.notes.add(lf.xpaths.get(i) + " Height: " + r.height + " , Width: " + r.width);
				System.out.println("Element Size Error: " + lf.xpaths.get(i) + " Height: " + r.height + " , Width: " + r.width);
			}
			

		}
	}

	
	public void addWebpage(Webpage wp)
	{
		webpages.add(wp);
	}
	public void setWebpages(ArrayList<Webpage> webpages) {
		this.webpages = webpages;
	}
}
