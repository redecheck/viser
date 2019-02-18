/*
 * Created by Ibrahim Althomali on 12/17/2017
 * 
 ********************************************/


import java.util.ArrayList;


public class Webpage 
{
	String siteName;
	String wPageName;
	String wPagePath;
	String uniqueRunName;
	ArrayList<String>	modHTMLPaths; 
	ArrayList<Failure>	Failures;
	public Webpage(String wPagePath)
	{
		this.wPagePath = wPagePath;
		modHTMLPaths = new ArrayList<String>();
		Failures = new ArrayList<Failure>();
	}
	public Webpage()
	{
		modHTMLPaths = new ArrayList<String>();
		Failures = new ArrayList<Failure>();
	}

}
