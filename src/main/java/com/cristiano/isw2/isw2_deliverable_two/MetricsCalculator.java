package com.cristiano.isw2.isw2_deliverable_two;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsCalculator {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetricsCalculator.class);
	
	public static final String NR = "Number of revisions";
	public static final String NAUTH = "Number of Authors";
	public static final String LOC_ADDED = "Sum over revisions of LOC added";
	public static final String MAX_LOC_ADDED = "Maximum over revisions of LOC added";
	public static final String AVG_LOC_ADDED = "Average LOC added per revision";
	public static final String CHURN = "Sum over revisions of added-deleted LOC";
	public static final String MAX_CHURN = "MAX_CHURN over revisions";
	public static final String AVG_CHURN = "Average CHURN per revision";
	public static final String CHG_SET_SIZE = "Number of files committed togheter with it";
	public static final String MAX_CHG_SET = "Maximum of ChgSet_Size over revisions";
	public static final String AVG_CHG_SET = "Average of ChgSet_Size over revisions";
	
	private Set<String> setOfClasses;
	private Map<String, Float> numOfRev;
	private Map<String, Float> numOfAuth;
	private Map<String, Float> maxLocAdded;
	private Map<String, Float> totLocAdded;
	private Map<String, Float> churnMetric;
	private Map<String, Float> maxChurn;
	private Map<String, Float> totChurn;
	private Map<String, Float> chgSetSize;
	private Map<String, Float> maxChgSet;
	private Map<String, Float> totChgSet;
	
	public MetricsCalculator() {
		this.setOfClasses = new HashSet<>();
		this.numOfRev = new HashMap<>();
		this.numOfAuth = new HashMap<>();
		this.maxLocAdded = new HashMap<>();
		this.totLocAdded = new HashMap<>();
		this.churnMetric = new HashMap<>();
		this.maxChurn = new HashMap<>();
		this.totChurn = new HashMap<>();
		this.chgSetSize = new HashMap<>();
		this.maxChgSet = new HashMap<>();
		this.totChgSet = new HashMap<>();
	}
	
	public void addData(String className, Float linesAdded, Float linesDeleted, Float chgSetSize) {
		Float oldVal;
		Float newVal;
		
		if (!this.setOfClasses.contains(className)) {
			initMaps(className);
		}
//		LOGGER.info("numOfRev: {}, numOfAuth: {}, maxLocAdded: {}, totLocAdded: {}, churnMetric: {}, maxChurn: {}, totChurn: {}, chgSetSize: {}, maxChgSet: {}, totChgSet: {}",
//				this.numOfRev.get(className), this.numOfAuth.get(className), this.maxLocAdded.get(className), this.totLocAdded.get(className), this.churnMetric.get(className), this.maxChurn.get(className), this.totChurn.get(className), this.chgSetSize.get(className), this.maxChgSet.get(className), this.totChgSet.get(className));
		
		// TO DO: number of authors
		
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
		
		oldVal = this.chgSetSize.get(className);
		newVal = (float) chgSetSize - 1;
		this.chgSetSize.replace(className, oldVal + newVal);
		
		oldVal = this.maxChgSet.get(className);
		if (newVal > oldVal) {
			this.maxChgSet.replace(className, newVal);
		}
		
		oldVal = this.totChgSet.get(className);
		this.totChgSet.replace(className, oldVal + newVal);
	}

	private void initMaps(String className) {
		this.setOfClasses.add(className);
		this.numOfRev.put(className, (float) 0);
		this.numOfAuth.put(className, (float) 0);
		this.maxLocAdded.put(className, (float) 0);
		this.totLocAdded.put(className, (float) 0);
		this.churnMetric.put(className, (float) 0);
		this.maxChurn.put(className, (float) 0);
		this.totChurn.put(className, (float) 0);
		this.chgSetSize.put(className, (float) 0);
		this.maxChgSet.put(className, (float) 0);
		this.totChgSet.put(className, (float) 0);
	}
	
	public void renameClass(String oldName, String newName) {
		Float val;
		
		setOfClasses.remove(oldName);
		setOfClasses.add(newName);
		
		val = this.numOfRev.remove(oldName);
		this.numOfRev.put(newName, val);
		val = this.numOfAuth.remove(oldName);
		this.numOfAuth.put(newName, val);
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
		val = this.chgSetSize.remove(oldName);
		this.chgSetSize.put(newName, val);
		val = this.maxChgSet.remove(oldName);
		this.maxChgSet.put(newName, val);
		val = this.totChgSet.remove(oldName);
		this.totChgSet.put(newName, val);
	}
	
	public void addRevision(String className) {
		if (!this.setOfClasses.contains(className)) {
			initMaps(className);
		}
		Float old = this.numOfRev.get(className);
		this.numOfRev.replace(className, old + 1);
	}
	
	public Float getComputedMetric(String className, String metric) {
		switch (metric) {
		case NR:
			return this.numOfRev.get(className);
		case NAUTH:
			return this.numOfAuth.get(className);
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
		case CHG_SET_SIZE:
			return this.chgSetSize.get(className);
		case MAX_CHG_SET:
			return this.maxChgSet.get(className);
		case AVG_CHG_SET:
			return this.totChgSet.get(className) / this.numOfRev.get(className);

		default:
			return (float) -1;
		}
	}
	
	public Set<String> getClasses() {
		return this.setOfClasses;
	}

}
