/*
 * Created by Ibrahim Althomali on 28/11/2017
 * 
 ********************************************/


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;



public class InputManager
{
	ArrayList<File>	inputFiles;
	ArrayList<Webpage>	webpages;
	String localSitesDirectory;
	int RedecheckRuns;
	public InputManager(String localSitesDirectory)
	{
		this.localSitesDirectory = localSitesDirectory;
		inputFiles = new ArrayList<File>();
		webpages = new ArrayList<Webpage>();
	}
	public void findFile(String name,File file)
    {
        if (file.isDirectory())
        {
            File[] subFiles = file.listFiles();
            if(subFiles!=null)
            for (File fileInSubFiles : subFiles)
            {
                findFile(name,fileInSubFiles);
            }
        }else if (name.equalsIgnoreCase(file.getName()))
        {
        	inputFiles.add(file);
        }
    }
	public void parseRedecheckOutputFiles()
	{
		int count = 1;
		for(File f: inputFiles)
		{
			Webpage wp = new Webpage();
			int from = f.getParentFile().getParentFile().getPath().lastIndexOf(File.separator);
			wp.siteName = f.getParentFile().getParentFile().getPath().substring(from+1);
			if(!wp.siteName.equals("Ninite")) //exclude this site since it did not report failures in the published paper.
			{
				from = f.getParentFile().getPath().lastIndexOf(File.separator);
				wp.uniqueRunName = f.getParentFile().getPath().substring(from+1);
				//from = f.getPath().lastIndexOf(File.separator);
				wp.wPageName = "index.html";
				wp.wPagePath = localSitesDirectory+File.separator + wp.siteName + File.separator + wp.wPageName;
				try 
				{
					String line;
					BufferedReader bReader = new BufferedReader(new FileReader(f));
					while((line = bReader.readLine()) != null)
					{
						String lineFragments [] = line.split(" ");
						if(line.contains("ARE OVERLAPPING"))
						{
							Failure lf = new Failure();
							lf.type = "collision";
							lf.ID = count;
							count++;
							if(lineFragments[1].trim().contains(lineFragments[3].trim()+"/"))
							{
								lf.addXpath(lineFragments[3].trim());
								lf.addXpath(lineFragments[1].trim());
							}
							else
							{
								lf.addXpath(lineFragments[1].trim());
								lf.addXpath(lineFragments[3].trim());
							}
							if(lineFragments[1].trim().contains(lineFragments[3].trim()+"/") || lineFragments[3].trim().contains(lineFragments[1].trim()+"/"))
							{
								lf.HTMLParentRelationship = true;
								lf.titles.add("HTML ancestor-descendant relationship:"); 
								lf.notes.add("Yes");
							}
							lf.viewMin = Integer.parseInt(lineFragments[lineFragments.length-3].trim());
							lf.viewMax = Integer.parseInt(lineFragments[lineFragments.length-1].trim());
							lf.setCaptureView();
							wp.Failures.add(lf);
						}
						else if(line.contains("OVERFLOWED ITS PARENT"))
						{
							Failure lf = new Failure();
							lf.type = "protrusion";
							lf.ID = count;
							count++;
							String overflowedElement = lineFragments[0].trim();
							lf.viewMin = Integer.parseInt(lineFragments[lineFragments.length-3].trim());
							lf.viewMax = Integer.parseInt(lineFragments[lineFragments.length-1].trim());
							line = bReader.readLine();
							lineFragments = line.split(",");
							String parent = "";
							if(!lineFragments[0].trim().equals(overflowedElement))
							{
								parent = lineFragments[0].trim();
								lf.addXpath(lineFragments[0].trim());
								lf.addXpath(overflowedElement);
								if(overflowedElement.length() <= lineFragments[1].length() && lineFragments[0].trim().contains(overflowedElement+"/"))
								{
									System.out.println("Input Warning for ID " +lf.ID);
									lf.titles.add("Input Warning:");
									lf.notes.add(overflowedElement + " was reported as overflowing its descendant element in the HTML structure. Will not work using this prototype.");
								}
							}
							else if(!lineFragments[1].trim().equals(overflowedElement))
							{
								parent = lineFragments[1].trim();
								lf.addXpath(lineFragments[1].trim());
								lf.addXpath(overflowedElement);
								if(overflowedElement.length() <= lineFragments[1].length() && lineFragments[1].trim().contains(overflowedElement+"/"))
								{
									System.out.println("Input Warning for ID " +lf.ID);
									lf.titles.add("Input Warning:");
									lf.notes.add(overflowedElement + " was reported as overflowing its descendant element in the HTML structure. Will not work using this prototype.");
								}
							}
							if(overflowedElement.contains(parent.trim()+"/")|| parent.trim().contains(overflowedElement.trim()+"/"))
							{
								lf.HTMLParentRelationship = true;
								lf.titles.add("HTML ancestor-descendant relationship:"); 
								lf.notes.add("Yes");
							}
							lf.setCaptureView();
							wp.Failures.add(lf);
						}
						else if(line.contains("overflowed the viewport"))
						{
							Failure lf = new Failure();
							lf.type = "viewport";
							lf.ID = count;
							count++;
							lf.HTMLParentRelationship = true;
							lf.titles.add("HTML ancestor-descendant relationship:"); 
							lf.notes.add("Yes");
							lf.addXpath("/HTML/BODY");
							lf.addXpath(lineFragments[0].trim());
							lf.viewMin = Integer.parseInt(lineFragments[lineFragments.length-3].trim());
							lf.viewMax = Integer.parseInt(lineFragments[lineFragments.length-1].trim());
							lf.setCaptureView();
							wp.Failures.add(lf);
						}

					}
					bReader.close();
				} catch (IOException e) 
				{
					System.out.println("problem reading input file ("+ f.getPath()+") exiting..");
					e.printStackTrace();
					System.exit(0);
				}
				webpages.add(wp);
			}
		}
	}
//	public void parseInputFiles()
//	{
//		int count = 1;
//		for(File f: inputFiles)
//		{
//			Webpage wp = new Webpage();
//			int from = f.getParentFile().getParentFile().getPath().lastIndexOf(File.separator);
//			wp.siteName = f.getParentFile().getParentFile().getPath().substring(from+1);
//			from = f.getParentFile().getPath().lastIndexOf(File.separator);
//			wp.uniqueRunName = f.getParentFile().getPath().substring(from+1);
//			//from = f.getPath().lastIndexOf(File.separator);
//			wp.wPageName = "index.html";
//			wp.wPagePath = localSitesDirectory+File.separator + wp.siteName + File.separator + wp.wPageName;
//			try 
//			{
//				String line;
//				BufferedReader bReader = new BufferedReader(new FileReader(f));
//				while((line = bReader.readLine()) != null)
//				{
//					Failure lf = new Failure();
//					if(line.toLowerCase().equals("wrapping"))
//					{
//						lf.type = "wrapping";
//						lf.ID = 0;
//						lf.addXpath(bReader.readLine());
//						int pathCount = Integer.parseInt(bReader.readLine().trim());
//						for(int i=0; i < pathCount; i++)
//						{
//							lf.addXpath(bReader.readLine());
//						}
//						lf.viewMin = Integer.parseInt(bReader.readLine().trim());
//						lf.viewMax = Integer.parseInt(bReader.readLine().trim());
//						lf.setCaptureView();
//					}
//					else if(line.toLowerCase().equals("viewport"))
//					{
//						lf.type = "viewport";
//						lf.ID = count;
//						count++;
//					}
//					else if(line.toLowerCase().equals("collision"))
//					{
//						lf.type = "collision";
//						lf.ID = count;
//						count++;
//					}
//					else if(line.toLowerCase().equals("protrusion"))
//					{
//						lf.type = "protrusion";
//						lf.ID = count;
//						count++;
//					}
//					else if(line.toLowerCase().equals("small-range"))
//					{
//						lf.type = "small-range";
//						lf.ID = 0;
//					}
//					if(line.toLowerCase().equals("viewport") || line.toLowerCase().equals("collision") || line.toLowerCase().equals("protrusion") || line.toLowerCase().equals("small-range"))
//					{
//						lf.addXpath(bReader.readLine());
//						lf.addXpath(bReader.readLine());
//						lf.viewMin = Integer.parseInt(bReader.readLine().trim());
//						lf.viewMax = Integer.parseInt(bReader.readLine().trim());
//						lf.setCaptureView();
//					}
//					wp.Failures.add(lf);
//				}
//				bReader.close();
//			} catch (IOException e) 
//			{
//				System.out.println("problem reading input file ("+ f.getPath()+") exiting..");
//				e.printStackTrace();
//				System.exit(0);
//			}
//			webpages.add(wp);
//		}
//	}
	public ArrayList<Webpage> getWebpages() {
		return webpages;
	}
	public void print()
	{
		for(Webpage wp : webpages )
		{
			System.out.println(wp);
		}
	}
	
}
