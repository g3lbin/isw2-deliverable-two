package com.cristiano.isw2.isw2_deliverable_two.milestone_two;

public class AnalysisResult {
	
	private String dataset;
	private int numTrReleases;
	private float trainingPercent;
	private float defectiveInTrainingPercent;
	private float defectiveInTestingPercent;
	private String classifier;
	private String sampling;
	private String featureSelection;
	private String sensitivity;
	private double tp;
	private double fp;
	private double tn;
	private double fn;
	private double precision;
	private double recall;
	private double auc;
	private double kappa;

	public AnalysisResult(String dataset, int numTrReleases, float trainingPercent, float defectiveInTrainingPercent,
			float defectiveInTestingPercent, String classifier) {
		this.dataset = dataset;
		this.numTrReleases = numTrReleases;
		this.trainingPercent = trainingPercent;
		this.defectiveInTrainingPercent = defectiveInTrainingPercent;
		this.defectiveInTestingPercent = defectiveInTestingPercent;
		this.classifier = classifier;
	}
	
	public void setSampling(int sampling) {
		switch(sampling) {
		case WekaAnalyzer.NO_SAMPLING:
			this.sampling = "no sampling";
			break;
		case WekaAnalyzer.UNDERSAMPLING:
			this.sampling = "undersampling";
			break;
		case WekaAnalyzer.OVERSAMPLING:
			this.sampling = "oversampling";
			break;
		case WekaAnalyzer.SMOTE:
			this.sampling = "smote";
			break;
			
		default:
			this.sampling = "undefined";
			break;
		}
	}
	
	public void setFeatureSelection(boolean featureSelection) {
		if (featureSelection) {
			this.featureSelection = "best first";
		} else {
			this.featureSelection = "no selection";
		}
	}
	
	public void setCostSensitiveClassifier(int sensitivity) {
		switch(sensitivity) {
		case WekaAnalyzer.NO_COST_SENSITIVE:
			this.sensitivity = "no cost sensitive";
			break;
		case WekaAnalyzer.SENSITIVE_THRESHOLD:
			this.sensitivity = "sensitive threshold";
			break;
		case WekaAnalyzer.SENSITIVE_LEARNING:
			this.sensitivity = "sensitive learning";
			break;
			
		default:
			this.sensitivity = "undefined";
			break;
		}
	}

	public void setTp(double tp) {
		this.tp = tp;
	}

	public void setFp(double fp) {
		this.fp = fp;
	}

	public void setTn(double tn) {
		this.tn = tn;
	}

	public void setFn(double fn) {
		this.fn = fn;
	}

	public void setPrecision(double precision) {
		this.precision = precision;
	}

	public void setRecall(double recall) {
		this.recall = recall;
	}

	public void setAuc(double auc) {
		this.auc = auc;
	}

	public void setKappa(double kappa) {
		this.kappa = kappa;
	}
	
	public String getCsvRow() {
		return dataset + "," +
		numTrReleases + "," +
		trainingPercent + "," +
		defectiveInTrainingPercent + "," +
		defectiveInTestingPercent + "," +
		classifier + "," +
		sampling + "," +
		featureSelection + "," +
		sensitivity + "," +
		tp + "," +
		fp + "," +
		tn + "," +
		fn + "," +
		precision + "," +
		recall + "," +
		auc + "," +
		kappa;
	}

}
