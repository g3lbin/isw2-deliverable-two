package com.cristiano.isw2.isw2_deliverable_two.milestone_one;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MetricsCalculator {
	
	public static final String NR = "Number of revisions";
	public static final String NAUTH = "Number of Authors";
	public static final String LOC_ADDED = "Sum over revisions of LOC added";
	public static final String MAX_LOC_ADDED = "Maximum over revisions of LOC added";
	public static final String AVG_LOC_ADDED = "Average LOC added per revision";
	public static final String CHURN = "Sum over revisions of added-deleted LOC";
	public static final String MAX_CHURN = "MAX_CHURN over revisions";
	public static final String AVG_CHURN = "Average CHURN per revision";
	public static final String MAX_CHG_SET = "Maximum of ChgSet_Size over revisions";
	public static final String AVG_CHG_SET = "Average of ChgSet_Size over revisions";
	
	private Set<String> setOfClasses;
	private Map<String, Float> numOfRev;
	private Map<String, Set<String>> authors;
	private Map<String, Float> maxLocAdded;
	private Map<String, Float> totLocAdded;
	private Map<String, Float> churnMetric;
	private Map<String, Float> maxChurn;
	private Map<String, Float> totChurn;
	private Map<String, Float> maxChgSet;
	private Map<String, Float> totChgSet;
	
	public MetricsCalculator() {
		this.setOfClasses = new HashSet<>();
		this.numOfRev = new HashMap<>();
		this.authors = new HashMap<>();
		this.maxLocAdded = new HashMap<>();
		this.totLocAdded = new HashMap<>();
		this.churnMetric = new HashMap<>();
		this.maxChurn = new HashMap<>();
		this.totChurn = new HashMap<>();
		this.maxChgSet = new HashMap<>();
		this.totChgSet = new HashMap<>();
	}
	
	public void addData(String className, Float linesAdded, Float linesDeleted, Float chgSetSize, String author) {
		Float oldVal;
		Float newVal;
		
		if (!this.setOfClasses.contains(className)) {
			initClassMetrics(className);
		}
		
		oldVal = this.numOfRev.get(className);
		this.numOfRev.replace(className, oldVal + 1);
		
		this.authors.get(className).add(author);
		
		oldVal = this.maxLocAdded.get(className);
		if (linesAdded > oldVal) {
			this.maxLocAdded.replace(className, linesAdded);
		}
			
		oldVal = this.totLocAdded.get(className);
		this.totLocAdded.replace(className, oldVal + linesAdded);
		
		oldVal = this.churnMetric.get(className);
		newVal = oldVal + linesAdded - linesDeleted;
		this.churnMetric.replace(className, newVal);
		
		oldVal = this.maxChurn.get(className);
		newVal = linesAdded - linesDeleted;
		if (newVal > oldVal) {
			this.maxChurn.replace(className, newVal);
		}
		
		oldVal = this.totChurn.get(className);
		this.totChurn.replace(className, oldVal + newVal);
		
		oldVal = this.maxChgSet.get(className);
		if (chgSetSize > oldVal) {
			this.maxChgSet.replace(className, newVal);
		}
		
		oldVal = this.totChgSet.get(className);
		this.totChgSet.replace(className, oldVal + chgSetSize);
	}

	private void initClassMetrics(String className) {
		this.setOfClasses.add(className);
		this.numOfRev.put(className, (float) 0);
		this.authors.put(className, new HashSet<>());
		this.maxLocAdded.put(className, (float) 0);
		this.totLocAdded.put(className, (float) 0);
		this.churnMetric.put(className, (float) 0);
		this.maxChurn.put(className, (float) 0);
		this.totChurn.put(className, (float) 0);
		this.maxChgSet.put(className, (float) 0);
		this.totChgSet.put(className, (float) 0);
	}
	
	public void renameClass(String oldName, String newName) {
		Float val;
		Set<String> set;
		
		if (!this.setOfClasses.contains(oldName)) {
			initClassMetrics(newName);
			
			return;
		}
		
		setOfClasses.remove(oldName);
		setOfClasses.add(newName);
		
		val = this.numOfRev.remove(oldName);
		this.numOfRev.put(newName, val);
		set = this.authors.remove(oldName);
		this.authors.put(newName, set);
		val = this.maxLocAdded.remove(oldName);
		this.maxLocAdded.put(newName, val);
		val = this.totLocAdded.remove(oldName);
		this.totLocAdded.put(newName, val);
		val = this.churnMetric.remove(oldName);
		this.churnMetric.put(newName, val);
		val = this.maxChurn.remove(oldName);
		this.maxChurn.put(newName, val);
		val = this.totChurn.remove(oldName);
		this.totChurn.put(newName, val);
		val = this.maxChgSet.remove(oldName);
		this.maxChgSet.put(newName, val);
		val = this.totChgSet.remove(oldName);
		this.totChgSet.put(newName, val);
	}
	
	public Float getComputedMetric(String className, String metric) {
		if (!this.setOfClasses.contains(className)) {
			return (float) 0;
		}
		
		switch (metric) {
		case NR:
			return this.numOfRev.get(className);
		case NAUTH:
			return (float) this.authors.get(className).size();
		case LOC_ADDED:
			return this.totLocAdded.get(className);
		case MAX_LOC_ADDED:
			return this.maxLocAdded.get(className);
		case AVG_LOC_ADDED:
			return this.totLocAdded.get(className) / this.numOfRev.get(className);
		case CHURN:
			return this.churnMetric.get(className);
		case MAX_CHURN:
			return this.maxChurn.get(className);
		case AVG_CHURN:
			return this.totChurn.get(className) / this.numOfRev.get(className);
		case MAX_CHG_SET:
			return this.maxChgSet.get(className);
		case AVG_CHG_SET:
			return this.totChgSet.get(className) / this.numOfRev.get(className);

		default:
			return (float) -1;
		}
	}

}
