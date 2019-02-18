/*
 * Created by Ibrahim Althomali on 12/15/2017
 * 
 ********************************************/


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;




public class Workbench
{
	static String filePath = "/Users/redecheck/fault-examples";
	static String reportsPath = "/Users/redecheck/reports";
	//static String filePath = "C:\\GitSpace\\BitBucket\\fault-examples";
	//static String reportsPath = "C:\\GitSpace\\BitBucket\\reports";
	static Scanner reader = new Scanner(System.in);  
	static ArrayList<Webpage> subsetPages = new ArrayList<Webpage>(); //for testing just specific sites
	static ArrayList<Webpage> allPages;
	static boolean featureHiddenDetection = false; //detect hidden collision elements behind another collision element with background color. 
	public static void main(String[] args) throws IOException
	{
//	     DriverManager dm = new DriverManager("chrome");
//	     dm.navigate("file:///C:/GitSpace/BitBucket/fault-examples/Duolingo/index.html");
//	     dm.measureScreen();
//	     dm.screenshot();
//	     pause("driver open");
//	     dm.shutdown(); 
        String fileName = "." + File.separator + Assist.date + File.separator + "ProgramRuntime.csv";

        PrintWriter writer = null;
        String runTimeCSV ="";
        long startTime = System.nanoTime();
        Assist.startTime();
		InputManager inM = new InputManager(filePath);
		inM.findFile("fault-report.txt", new File(reportsPath));
		inM.parseRedecheckOutputFiles();		
		allPages = inM.webpages;
		removeSiteWithZeroFailures();
		//PrintPages(allPages);
		PrintPages(allPages);
		Categorizer inv = new Categorizer(reportsPath, featureHiddenDetection);
		new File("." + File.separator +Assist.date).mkdir();
		inv.setWebpages(allPages); //All pages found by input manager
		String site = "pocket";
		String page = "index.html";
		
	//	addPage(site, page);
	//	firstRun();
	//	PrintPages(subsetPages);
	//  inv.setWebpages(subsetPages); //Subset of pages found by input manager

		Assist.endTime();
		runTimeCSV = runTimeCSV + Assist.getDurationMilli() + ",";
		Assist.startTime();
		inv.lookForNOI2();
		Assist.endTime();
		runTimeCSV = runTimeCSV + Assist.getDurationMilli() + ",";
		Assist.startTime();
		inv.outputHTMLSite();
		Assist.endTime();
		runTimeCSV = runTimeCSV + Assist.getDurationMilli() + ",";
		Assist.startTime();
		inv.writeReport();
		Assist.endTime();
		runTimeCSV = runTimeCSV + Assist.getDurationMilli() + ",";
		inv.dm.shutdown();
		Assist.reader.close(); 
		long endTime = System.nanoTime();
		runTimeCSV = runTimeCSV + ((endTime - startTime)/1000000);
        try 
        {
        		writer = new PrintWriter(fileName);
            writer.println("Input Millisec,Verify Millisec,Output HTML Millisec,Output CSV Millesec,Program Total Runtime Millesec");
    			writer.println(runTimeCSV);
        }catch (IOException e) 
        {    
            System.out.println("could not save runtime report to file ("+fileName+")... exiting");
            e.printStackTrace();
            System.exit(0);
        }
		writer.close();
	}
	private static void removeSite(String removeSiteName)
	{
		for(int i =0; i < allPages.size(); i++)
		{
			if(allPages.get(i).siteName.equals(removeSiteName))
			{
				allPages.remove(i);
				break;
			}
		}
	}
	private static void removeSiteWithZeroFailures()
	{
		for(int i =0; i < allPages.size(); i++)
		{
			if(allPages.get(i).Failures.size()==0)
			{
				allPages.remove(i);
				i=0;
			}
		}
	}
	public static void addPage(String site, String page) {
		for(int i=0; i<allPages.size(); i++)
		{
			Webpage tmpPage = allPages.get(i);
			if(tmpPage.siteName.toLowerCase().equals(site.toLowerCase()) && tmpPage.wPageName.toLowerCase().equals(page.toLowerCase()))
			{
				subsetPages.add(tmpPage);
				i=allPages.size(); //finds the first instance of Webpage with the same name
			}
		}
	}
	public static void firstRun() {
		ArrayList<String> runs = new ArrayList<String>();
		for(int i=0; i<allPages.size(); i++)
		{
			Webpage tmpPage = allPages.get(i);
			if(runs.isEmpty() || !runs.contains(tmpPage.siteName))
			{
				subsetPages.add(tmpPage);
				runs.add(tmpPage.siteName);
			}
		}
	}
	public static void lastRun() {
		ArrayList<String> runs = new ArrayList<String>();
		for(int i=allPages.size()-1; i>=0; i--)
		{
			Webpage tmpPage = allPages.get(i);
			if(runs.isEmpty() || !runs.contains(tmpPage.siteName))
			{
				subsetPages.add(tmpPage);
				runs.add(tmpPage.siteName);
			}
		}
	}
	public static void PrintPages(ArrayList<Webpage> pages) {
		System.out.println("Total Sites: " + pages.size());
		System.out.println("---------------------------------");
		for(Webpage wp: pages)
		{
			System.out.println("Site   :" + wp.siteName);
			System.out.println("Page   :" + wp.wPageName);
			System.out.println("Path   :" + wp.wPagePath);
			System.out.println("Run    :" + wp.uniqueRunName);
			System.out.println("Faults :" + wp.Failures.size());
			System.out.println("---------------------------------");
		}
	}

}
