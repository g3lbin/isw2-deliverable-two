package com.cristiano.isw2.isw2_deliverable_two.milestone_one;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Classe utilizzata per estrarre gli ID dei tickets mediante la REST API di Jira
 */
public class RetrieveTicketsFromJira {
	
	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
	
	private RetrieveTicketsFromJira() {
		throw new IllegalStateException("Utility class");
	}

	private static String readAll(Reader rd) throws IOException {
		int cp;
		StringBuilder sb = new StringBuilder();
		
		while ((cp = rd.read()) != -1)
			sb.append((char) cp);
		
		return sb.toString();
	}
	
	private static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
	      
		try (
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			) {
	        String jsonText = readAll(rd);
	        
	        return new JSONObject(jsonText);
		} finally {
	         is.close();
	    }
	}
	
	public static List<JiraIssue> findIssues(String project, List<ReleaseInfo> releases) throws IOException, JSONException {
		float proportion = 0; 	// P of proportion method
		String jiraURL = "https://issues.apache.org/jira/rest/api/2/search?jql=project%20=%20" + 
				project.toUpperCase() + "%20AND%20issuetype%20=%20Bug%20AND%20status%20in%20(Resolved,%20Closed)" + 
		        "%20AND%20resolution%20=%20Fixed%20ORDER%20BY%20created%20ASC&fields=" +
				"key,fixVersions,versions,created,updated,resolutiondate&maxResults=1000";
		List<JiraIssue> issuesList;
		Map<String, Integer> versionsNum = new HashMap<>();
		Set<String> releaseNames = new HashSet<>();
		// assign an index to each release
		int idx = 0;
		for (ReleaseInfo r : releases) {
			versionsNum.put(r.getName(), idx);
			releaseNames.add(r.getName());
			idx++;
		}

		int total = 1;
		int i = 0;
		int j = 0;

		try {
			issuesList = new ArrayList<>();
			int iteration = 1;
			do {
				JSONObject json = readJsonFromUrl(jiraURL);
			    JSONArray issues = json.getJSONArray("issues");
			    total = json.getInt("total");
			    
			    j = i + 1000;
			    for (; i < total && i < j; i++) {
			    	//Iterate through each ticket
			        JiraIssue issue = new JiraIssue(issues.getJSONObject(i%1000));
			        if (checkGoodness(issue, releases, releaseNames)) {
			        	float res = computeAffectedVersions(issue, releases, versionsNum, proportion, iteration);
			        	if (res != -1) {
			        		proportion = res;
				        	issuesList.add(issue);
				        	iteration++;
			        	}
			        }
			    }  
		    } while (i < total);
		} catch (IOException | JSONException e) {
			throw new JSONException("findIssueIds failed");
		}
		return issuesList;
	}
	
	private static float computeAffectedVersions(JiraIssue issue, List<ReleaseInfo> releases, Map<String, Integer> versionsNum, float p, int iteration) {
		List<String> versions = issue.getVersions();
		List<String> fixVersions = issue.getFixVersions();
		String ov;
		String fv;
		String iv = null;
		boolean applyProportion = false;
		
		ov = getOV(issue.getCreated(), releases);
		if (versions.isEmpty()) {
			applyProportion = true;
		} else {
			iv = getMinVersion(versions, versionsNum);
			issue.setAv(versions);
		}
		if (fixVersions.isEmpty()) {
			fv = getFV(issue.getResolutiondate(), issue.getUpdated(), releases);
		} else {
			fv = getMaxVersion(fixVersions, versionsNum);
		}
		
		if (applyProportion) {
			int predictedIV = (int) (versionsNum.get(fv) - (versionsNum.get(fv) - versionsNum.get(ov))*p);
			for (Map.Entry<String,Integer> entry : versionsNum.entrySet()) {
				if (entry.getValue() == predictedIV) {
					iv = entry.getKey();
				}
			}
		}
		if (!versionsNum.containsKey(iv) || !versionsNum.containsKey(ov) || !versionsNum.containsKey(fv)) {
			return -1;
		}
		issue.setIv(iv);
		issue.setOv(ov);
		issue.setFv(fv);
		if (applyProportion) {
			issue.setAv(getAV(iv, fv, releases, versionsNum));
		}
		if (versionsNum.get(fv) <= (versionsNum.get(ov)) || versionsNum.get(iv) > versionsNum.get(ov) || issue.getAv().contains(fv)) {
			return -1;
		}
		
		float currP = (float) (versionsNum.get(fv) - versionsNum.get(iv)) / (float) (versionsNum.get(fv) - versionsNum.get(ov));
		
		return p + (currP - p) / iteration;		// update avg with Welford algorithm: avg_i = avg_{i-1} + (x_i - avg_{i-1})/i
	}

	private static List<String> getAV(String iv, String fv, List<ReleaseInfo> releases, Map<String, Integer> versionsNum) {
		List<String> av = new ArrayList<>();
		int numAV = versionsNum.get(fv) - versionsNum.get(iv);
		int first = versionsNum.get(iv);
		for (int i = first; i < first + numAV; i++) {
			av.add(releases.get(i).getName());
		}
		
		return av;
	}

	private static String getFV(String resolutiondate, String updated, List<ReleaseInfo> releases) {
		String fv = null;
		int checkStart;
		int checkEnd;
		LocalDateTime dateTime;
		if (resolutiondate == null) {
			dateTime = LocalDateTime.parse(updated, formatter);
		} else {
			dateTime = LocalDateTime.parse(resolutiondate, formatter);
		}
		for (int i = 0; i < releases.size(); i++) {
			ReleaseInfo release = releases.get(i);
			checkStart = dateTime.compareTo(release.getStart());
			checkEnd = dateTime.compareTo(release.getEnd());
	        if(checkStart >= 0 && checkEnd < 0) {
	        	// date >= start_release && date < start_end
	        	fv = release.getName();
	        	break;
	        }
		}
		
		return fv;
	}

	private static String getOV(String created, List<ReleaseInfo> releases) {
		String ov = null;
		int checkStart;
		int checkEnd;
		LocalDateTime dateCreated = LocalDateTime.parse(created, formatter);
		
		for (int i = 0; i < releases.size(); i++) {
			ReleaseInfo release = releases.get(i);
			checkStart = dateCreated.compareTo(release.getStart());
			checkEnd = dateCreated.compareTo(release.getEnd());
	        if(checkStart >= 0 && checkEnd < 0) {
	        	// dateCreated >= start_release && dateCreated < start_end
	        	ov = release.getName();
	        	break;
	        }
		}
		
		return ov;
	}
		
	private static boolean checkGoodness(JiraIssue issue, List<ReleaseInfo> releases, Set<String> releaseNames) {
		// check if the basic fields are present
		if (issue.getCreated() == null) {
			return false;
		}
		if (issue.getResolutiondate() == null && issue.getUpdated() == null) {
			return false;
		}
		if (!releaseNames.containsAll(issue.getVersions()))
			return false;
		if (!releaseNames.containsAll(issue.getFixVersions()))
			return false;
		// check if the bug is too old
		LocalDateTime dateCreated = LocalDateTime.parse(issue.getCreated(), formatter);
		if (dateCreated.compareTo(releases.get(0).getStart()) < 0) {
			return false;
		}
		// check if the bug is too new
		ReleaseInfo lastRelease = releases.get(releases.size() - 1);
		return dateCreated.compareTo(lastRelease.getEnd()) < 0;
	}

	private static String getMaxVersion(List<String> versions, Map<String, Integer> versionsNum) {
		int max = -1;
		String maxVer = "";
		
		for (String version : versions) {
			if (versionsNum.get(version) > max) {
				max = versionsNum.get(version);
				maxVer = version;
			}
		}
		
		return maxVer;
	}
	
	private static String getMinVersion(List<String> versions, Map<String, Integer> versionsNum) {
		int min = 999999999; // max representable value
		String minVer = "";
		
		for (String version : versions) {
			if (versionsNum.get(version) < min) {
				min = versionsNum.get(version);
				minVer = version;
			}
		}
		
		return minVer;
	}
}
