package com.cristiano.isw2.isw2_deliverable_two;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class JiraIssue {
	// issue data (taken from jira)
	private String key;
	private String created;
	private String resolutiondate;
	private String updated;
	private List<String> versions;
	private List<String> fixVersions;
	// bug life cycle
	private String iv;
	private String ov;
	private String fv;
	private List<String> av;
	
	public JiraIssue(JSONObject obj) {
		JSONArray arr;
		
		key = obj.getString("key");
		JSONObject fields = obj.getJSONObject("fields");
		created = fields.getString("created");
		resolutiondate = fields.getString("resolutiondate");
		updated = fields.getString("updated");
		
		arr = fields.getJSONArray("versions");
		versions = new ArrayList<>();
		for (int i = 0; i < arr.length(); i++) {
			versions.add(arr.getJSONObject(i).getString("name"));
		}
		
		arr = fields.getJSONArray("fixVersions");
		fixVersions = new ArrayList<>();
		for (int i = 0; i < arr.length(); i++) {
			fixVersions.add(arr.getJSONObject(i).getString("name"));
		}
	}

	public String getKey() {
		return key;
	}

	public String getCreated() {
		return created;
	}

	public String getResolutiondate() {
		return resolutiondate;
	}

	public String getUpdated() {
		return updated;
	}
	
	public List<String> getVersions() {
		return versions;
	}

	public List<String> getFixVersions() {
		return fixVersions;
	}

	public String getIv() {
		return iv;
	}

	public String getOv() {
		return ov;
	}

	public String getFv() {
		return fv;
	}

	public List<String> getAv() {
		return av;
	}

	public void setIv(String iv) {
		this.iv = iv;
	}

	public void setOv(String ov) {
		this.ov = ov;
	}

	public void setFv(String fv) {
		this.fv = fv;
	}

	public void setAv(List<String> av) {
		this.av = av;
	}
}
