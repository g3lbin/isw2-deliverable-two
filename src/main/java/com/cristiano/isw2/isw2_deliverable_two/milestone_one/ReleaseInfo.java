package com.cristiano.isw2.isw2_deliverable_two.milestone_one;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReleaseInfo {
	private String name;
	private LocalDateTime start;
	private LocalDateTime end;
	private Map<String, String> classesBugginess;
	private MetricsCalculator mc;
	private Set<String> classesAtTheEnd;
	
	public ReleaseInfo(String name, LocalDateTime start, LocalDateTime end) {
		this.name = name;
		this.start = start;
		this.end = end;
		classesBugginess = new HashMap<>();
		mc = new MetricsCalculator();
		classesAtTheEnd = new HashSet<>();
	}
	
	public String getName() {
		return name;
	}
	
	public LocalDateTime getStart() {
		return start;
	}

	public LocalDateTime getEnd() {
		return end;
	}
	
	public void addData(String className, Float linesAdded, Float linesDeleted, Float chgSetSize, String author) {	
		classesBugginess.computeIfAbsent(className, v -> "no");
		mc.addData(className, linesAdded, linesDeleted, chgSetSize, author);
	}
	
	public void renameClass(String oldName, String newName) {
		String old = "no";
		if (classesBugginess.containsKey(oldName)) {
			old = classesBugginess.remove(oldName);
		}
		classesBugginess.put(newName, old);
		mc.renameClass(oldName, newName);
	}
	
	public void setBugginess(String className) {
		if (!classesBugginess.containsKey(className)) {
			classesBugginess.put(className, "yes");
		} else {
			classesBugginess.replace(className, "yes");
		}
	}
	
	public Set<String> getClassesAtTheEnd() {
		return classesAtTheEnd;
	}

	public void setClassesAtTheEnd(Set<String> classesAtTheEnd) {
		this.classesAtTheEnd = classesAtTheEnd;
	}
	
	public String getCsvRow(String className) {
		classesBugginess.computeIfAbsent(className, v -> "no");
		
		Float m1 = mc.getComputedMetric(className, MetricsCalculator.NR);
		Float m2 = mc.getComputedMetric(className, MetricsCalculator.NAUTH);
		Float m3 = mc.getComputedMetric(className, MetricsCalculator.LOC_ADDED);
		Float m4 = mc.getComputedMetric(className, MetricsCalculator.MAX_LOC_ADDED);
		Float m5 = mc.getComputedMetric(className, MetricsCalculator.AVG_LOC_ADDED);
		Float m6 = mc.getComputedMetric(className, MetricsCalculator.CHURN);
		Float m7 = mc.getComputedMetric(className, MetricsCalculator.MAX_CHURN);
		Float m8 = mc.getComputedMetric(className, MetricsCalculator.AVG_CHURN);
		Float m9 = mc.getComputedMetric(className, MetricsCalculator.MAX_CHG_SET);
		Float m10 = mc.getComputedMetric(className, MetricsCalculator.AVG_CHG_SET);
		
		return name + "," + className + "," + m1 + "," + m2 + "," + m3 + "," + m4 + "," + m5 + "," + m6 + 
				"," + m7 + "," + m8 + "," + m9 + "," + m10 + "," + classesBugginess.get(className) + "\n";
	}

}
