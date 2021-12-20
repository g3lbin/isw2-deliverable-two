package com.cristiano.isw2.isw2_deliverable_two.milestone_two;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;

public class WekaAnalyzer {
	
	private static final String PROJECT1 = "BOOKKEEPER";
	private static final String PROJECT2 = "ZOOKEEPER";
	private static final String RESULTS = "results/weka-results.csv";
	public static final int NO_SAMPLING = 0;
	public static final int UNDERSAMPLING = 1;
	public static final int OVERSAMPLING = 2;
	public static final int SMOTE = 3;
	private static final int CFP = 1;
	private static final int CFN = 10*CFP;
	public static final int NO_COST_SENSITIVE = 0;
	public static final int SENSITIVE_THRESHOLD = 1;
	public static final int SENSITIVE_LEARNING = 2;
	public static final int NAIVE_BAYES = 0;
	public static final int IBK = 1;
	public static final int RANDOM_FOREST = 2;
	private static final Logger LOGGER = LoggerFactory.getLogger(WekaAnalyzer.class);

	public static void main(String[] args) {
		
		try {
	    	// create file CSV
	    	File out = new File(RESULTS);
	    	Files.deleteIfExists(out.toPath());
	    	if (!out.createNewFile())
	    		throw new IOException();
    	} catch (IOException e) {
    		System.exit(1);
    	}
		
		try (
    			FileWriter fileWriter = new FileWriter(RESULTS);
    		) {
    		// file CSV initialization
		    fileWriter.append("dataset,#TrainingRelease,%training,%Defective in training,%Defective in testing,classifier," +
		    		"balancing,Feature Selection,Sensitivity,TP,FP,TN,FN,Precision,Recall,AUC,Kappa");
		    fileWriter.append("\n");
		    
		    walkForwardValidation(PROJECT1, fileWriter);
		    walkForwardValidation(PROJECT2, fileWriter);
		    
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			System.exit(1);
		}
	}
	
	private static void walkForwardValidation(String project, FileWriter fileWriter) throws Exception {
		DataSource source = new DataSource("results/" + project + "-ds.csv");
		Instances instances = source.getDataSet();
		
		int numOfReleases = instances.attribute("Version").numValues();
		if (numOfReleases < 2) {
			LOGGER.error("Not enough releases in the project");
			System.exit(1);
		}
		Attribute versionAttr = instances.attribute("Version");
		for (int i = 0; i < numOfReleases - 1; i++) {
			List<AnalysisResult> results = executeRun(project, instances, versionAttr, i);
			for (AnalysisResult res : results) {
				fileWriter.append(res.getCsvRow());
				fileWriter.append("\n");
				fileWriter.flush();
			}
		}
	}

	private static List<AnalysisResult> executeRun(String dataset, Instances instances, Attribute versionAttr, int lastTrainingReleaseIndx) throws Exception {
		int i;
		int buggyIndx = instances.numAttributes() - 1;

		String testingRelease = versionAttr.value(lastTrainingReleaseIndx + 1);
		List<String> trainingReleases = new ArrayList<>();
		for (i = 0; i <= lastTrainingReleaseIndx; i++) {
			trainingReleases.add(versionAttr.value(i));
		}
		
		int numOfInstances = instances.numInstances();
		int firstTestingInstanceIndx = 0;
		// create training set
		Instances trainingSet = new Instances(instances, 0);
		int defectiveInTraining = 0;
		for (i = 0; i < numOfInstances; i++) {
			Instance instance = instances.instance(i);
			defectiveInTraining += instance.stringValue(buggyIndx).equals("yes")  ? 1 : 0;
			
			String release = instance.stringValue(versionAttr);
			if (trainingReleases.contains(release)) {
				trainingSet.add(instance);
			} else {
				firstTestingInstanceIndx = i;
				break;
			}
		}
		trainingSet.setClassIndex(buggyIndx);
		// create testing set
		Instances testingSet = new Instances(instances, 0);
		int defectiveInTesting = 0;
		for (i = 0; i < numOfInstances - firstTestingInstanceIndx; i++) {
			Instance instance = instances.instance(firstTestingInstanceIndx + i);
			defectiveInTesting += instance.stringValue(buggyIndx).equals("yes")  ? 1 : 0;
			
			String release = instance.stringValue(versionAttr);
			if (release.equals(testingRelease)) {
				testingSet.add(instance);
			} else {
				break;
			}
		}
		testingSet.setClassIndex(buggyIndx);

		return doWork(
				dataset,
				trainingSet, 
				testingSet, 
				trainingReleases.size(), 
				instances.size(), 
				defectiveInTraining, 
				defectiveInTesting);
	}

	private static Classifier getClassifier(int which) {
		switch(which) {
		case NAIVE_BAYES:
			return new NaiveBayes();
		case IBK:
			return new IBk();
		case RANDOM_FOREST:
			return new RandomForest();
			
		default:
			return null;
		}
	}

	private static List<AnalysisResult> doWork(
			String dataset,
			Instances trainingSet, 
			Instances testingSet, 
			int numTrReleases, 
			int numTotalInstances,
			int defectiveInTraining, 
			int defectiveInTesting) throws Exception {
		
		float trainingPercent = (float) 100*trainingSet.size() / numTotalInstances;
		float defectiveInTrainingPercent = (float) 100*defectiveInTraining / trainingSet.size();
		float defectiveInTestingPercent = (float) 100*defectiveInTesting / testingSet.size();
		
		Instances filteredTrSet;
		Instances filteredTestSet = testingSet;
		List<AnalysisResult> results = new ArrayList<>();
		// sampling
		for (int i = 0; i < 4; i++) {
			filteredTrSet = applySampling(i, trainingSet);
				// feature selection
				for (int j = 0; j < 2; j++) {
					AttributeSelection filter = selectFeatures(filteredTrSet);
					if (j == 0) {
						filteredTrSet = Filter.useFilter(trainingSet, filter);
						filteredTestSet = Filter.useFilter(testingSet, filter);
					}
					for (int k = 0; k < 3; k++) {
						// cost sensitive
						Classifier cl = getClassifier(k);
						for (int h = 0; h < 3 && cl != null; h++) {
							AnalysisResult res = new AnalysisResult(
									dataset, 
									numTrReleases, 
									trainingPercent, 
									defectiveInTrainingPercent, 
									defectiveInTestingPercent,
									cl.getClass().getName());
							res.setSampling(i);
							res.setFeatureSelection(j != 0);
							res.setCostSensitiveClassifier(k);
							
							costSensitiveClassification(cl, filteredTrSet, filteredTestSet, h, res);
							results.add(res);
						}
					}
				}
		}
		
		return results;
	}

	private static Instances applySampling(int sampling, Instances trainingSet) throws Exception {
		String[] opts;
		Instances filteredDataset;
		Float createdInstancesPercent;
		
		float majorityNum;
		float minorityNum = 0;
		int buggyIndx = trainingSet.numAttributes() - 1;
		for (Instance instance: trainingSet){
		    minorityNum += instance.stringValue(buggyIndx).equals("yes")  ? 1 : 0;
		}
		majorityNum = trainingSet.numInstances() - minorityNum;
		if (minorityNum > majorityNum) {
			// swap values
			float temp;
			temp = minorityNum;
			minorityNum = majorityNum;
			majorityNum = temp;
		}
		if (minorityNum == 0 && majorityNum == 0) {
			LOGGER.info("returned zero instances");
			return new Instances(trainingSet, 0);
		}
		
		switch(sampling) {
		case UNDERSAMPLING:
			SpreadSubsample  spreadSubsample = new SpreadSubsample();
			opts = new String[]{ "-M", "1.0"};
			spreadSubsample.setOptions(opts);
			spreadSubsample.setInputFormat(trainingSet);
			filteredDataset = Filter.useFilter(trainingSet, spreadSubsample);
			break;
		case OVERSAMPLING:
			createdInstancesPercent = 100*(majorityNum - minorityNum) / (majorityNum + minorityNum);
			Resample resample = new Resample();
			Float outputSizePercent = 100 + createdInstancesPercent;
			opts = new String[]{ "-B", "1.0", "-Z", outputSizePercent.toString()};
			resample.setOptions(opts);
			resample.setInputFormat(trainingSet);
			filteredDataset = Filter.useFilter(trainingSet, resample);
			break;
		case SMOTE:
			if (minorityNum == 0) {
				minorityNum++;
			}
			createdInstancesPercent = 100*(majorityNum - minorityNum) / minorityNum;
			SMOTE smote = new SMOTE();
			// specifies percentage of SMOTE instances to create
			opts = new String[]{ "-P", createdInstancesPercent.toString()};
			smote.setOptions(opts);
			smote.setInputFormat(trainingSet);
			filteredDataset = Filter.useFilter(trainingSet, smote);
			break;
		default:
			return trainingSet;
		
		}
		
		return filteredDataset;
	}
	
	private static AttributeSelection selectFeatures(Instances trainingSet) throws Exception{
		AttributeSelection filter = new AttributeSelection();
	    CfsSubsetEval eval = new CfsSubsetEval();
	    BestFirst search = new BestFirst();
	    String[] opts = new String[]{ "-D", "1", "-N", "11"};
	    search.setOptions(opts);
	    filter.setEvaluator(eval);
	    filter.setSearch(search);
		filter.setInputFormat(trainingSet);
		
		return filter;
	}

	private static void costSensitiveClassification(Classifier cl, Instances trainingSet, Instances testingSet, int type, AnalysisResult result) throws Exception {
		Evaluation eval;
		
		int buggyIndx = testingSet.numAttributes() - 1;
		trainingSet.setClassIndex(buggyIndx);
		testingSet.setClassIndex(buggyIndx);
		
		switch(type) {
		case NO_COST_SENSITIVE:
			cl.buildClassifier(trainingSet);
			eval = new Evaluation(trainingSet);
			eval.evaluateModel(cl, testingSet);
			break;
		case SENSITIVE_THRESHOLD:
		case SENSITIVE_LEARNING:
			CostSensitiveClassifier costSensCl = new CostSensitiveClassifier();
			costSensCl.setClassifier(cl);
			costSensCl.setCostMatrix(createCostMatrix(CFP, CFN));
			costSensCl.buildClassifier(trainingSet);
			costSensCl.setMinimizeExpectedCost(type == SENSITIVE_THRESHOLD);
			
			eval = new Evaluation(trainingSet, costSensCl.getCostMatrix());
			eval.evaluateModel(costSensCl, testingSet);
			break;
			
		default:
			return;
		}
		
		result.setTp(eval.numTrueNegatives(0));
		result.setFp(eval.numFalseNegatives(0));
		result.setTn(eval.numTruePositives(0));
		result.setFn(eval.numFalsePositives(0));
		result.setPrecision(eval.precision(0));
		result.setRecall(eval.recall(0));
		result.setAuc(eval.areaUnderROC(0));
		result.setKappa(eval.kappa());
	}
	
	private static CostMatrix createCostMatrix(double weightFalsePositive, double weightFalseNegative) {
		/*
		 *  ----------------------------
		 *  | True "no"  | False "yes" |
		 *  ----------------------------
		 *  | False "no" | True "yes"  |
		 *  ----------------------------
		 */
		CostMatrix costMatrix = new CostMatrix(2);
		costMatrix.setCell(0, 0, 0.0);
		costMatrix.setCell(0, 1, weightFalsePositive);
		costMatrix.setCell(1, 0, weightFalseNegative);
		costMatrix.setCell(1, 1, 0.0);
		
		return costMatrix;
	}
	
}
