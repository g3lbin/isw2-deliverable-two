package com.cristiano.isw2.isw2_deliverable_two;

import java.time.LocalDateTime;

import org.eclipse.jgit.lib.Repository;

public class JavaClass {

	private String name;
	public static final String LOC_touched = "Sum over revisions of LOC added+deleted+modified";
	public static final String NR = "Number of revisions";
	public static final String NAuth = "Number of Authors";
	public static final String MAX_LOC_added = "Maximum over revisions of LOC added";
	public static final String AVG_LOC_added = "Average LOC added per revision";
	public static final String Churn = "Sum over revisions of added-deleted LOC";
	public static final String MAX_CHURN = "MAX_CHURN over revisions";
	public static final String AVG_Churn = "Average CHURN per revision";
	public static final String ChgSetSize = "Number of files committed togheter with it";
	public static final String MAX_ChgSet = "Maximum of ChgSet_Size over revisions";
	public static final String AVG_ChgSet = "Average of ChgSet_Size over revisions";
	
	public JavaClass(String name) {
		this.name = name;
	}
	
	public String compute(String metric, LocalDateTime releaseStart, LocalDateTime releaseEnd) {
		
		return null;
	}

}
