package com.cristiano.isw2.isw2_deliverable_two.milestone_one;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;


public class GetReleaseInfo {

	private static Map<LocalDateTime, String> releaseNames;
	private static List<LocalDateTime> releases;
	
	private GetReleaseInfo() {
		/* to avoid SonarCloud smell */
	}
   
	public static int createOutputFile(String projectName) throws IOException, JSONException {
		//Fills the array list with releases dates and orders them
		//Ignores releases with missing dates
		releases = new ArrayList<>();
		int i;
		int numOfReleases;
		String url = "https://issues.apache.org/jira/rest/api/2/project/" + projectName;
		JSONObject json = readJsonFromUrl(url);
		JSONArray versions = json.getJSONArray("versions");
		releaseNames = new HashMap<>();
		for (i = 0; i < versions.length(); i++ ) {
			String name = "";
		    if(versions.getJSONObject(i).has("releaseDate")) {
		    	if (versions.getJSONObject(i).has("name"))
		    		name = versions.getJSONObject(i).get("name").toString();

		    	addRelease(versions.getJSONObject(i).get("releaseDate").toString(), name);
			}
	 	}
		numOfReleases = releases.size();
		// order releases by date
		Collections.sort(releases, (LocalDateTime o1, LocalDateTime o2)->o1.compareTo(o2));
		//Name of CSV for output
		String outname = projectName + "VersionInfo.csv";
		// create file CSV
    	File out = new File(outname);
    	Files.deleteIfExists(out.toPath());
    	if (!out.createNewFile())
    		throw new IOException();
		try (
				FileWriter fileWriter = new FileWriter(outname);
			) {
	    	// file CSV initialization
	        fileWriter.append("Version Name,Date");
	        fileWriter.append("\n");
	        for (i = 0; i < numOfReleases; i++) {
	        	fileWriter.append(releaseNames.get(releases.get(i)));
	        	fileWriter.append(",");
	        	fileWriter.append(releases.get(i).toString());
	        	fileWriter.append("\n");
		    	}
	 	} catch (Exception e) {
	 		throw new IOException();
	 	}
		
		return numOfReleases;
	}
	
	   public static void addRelease(String strDate, String name) {
		   LocalDate date = LocalDate.parse(strDate);
		   LocalDateTime dateTime = date.atStartOfDay();
		   if (!releases.contains(dateTime))
			   releases.add(dateTime);
		   releaseNames.put(dateTime, name);
	   }

	   public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {    
		   try (
				   InputStream is = new URL(url).openStream();
				   BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			   ) {
			   String jsonText = readAll(rd);
		        
			   return new JSONObject(jsonText);
		   }
		}
	   
	   private static String readAll(Reader rd) throws IOException {
		   StringBuilder sb = new StringBuilder();
		   int cp;
		   while ((cp = rd.read()) != -1) {
			   sb.append((char) cp);
		   }
		   return sb.toString();
	   }

	
}