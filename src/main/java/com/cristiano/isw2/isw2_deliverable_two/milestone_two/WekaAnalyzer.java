package com.cristiano.isw2.isw2_deliverable_two.milestone_two;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;

public class WekaAnalyzer {
	
	private static final String PROJECT1 = "BOOKKEEPER";
	private static final String PROJECT2 = "ZOOKEEPER";
	private static final String RESULTS = "results.csv";
	private static final int UNDERSAMPLING = 0;
	private static final int OVERSAMPLING = 1;
	private static final int SMOTE = 2;
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
		    
		    walkForwardValidation(PROJECT1);
		    
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			System.exit(1);
		}
	}
	
	private static void walkForwardValidation(String project) throws Exception {
		DataSource source = new DataSource(project + "-ds.csv");
		Instances instances = source.getDataSet();
		
		int numOfReleases = instances.attribute("Version").numValues();
		if (numOfReleases < 2) {
			LOGGER.error("Not enough releases in the project");
			System.exit(1);
		}
		Attribute versionAttr = instances.attribute("Version");
		for (int i = 0; i < numOfReleases - 1; i++) {
			executeRun(instances, versionAttr, i);
		}
	}

	private static void executeRun(Instances instances, Attribute versionAttr, int lastTrainingReleaseIndx) throws Exception {
		int i;
		String testingRelease = versionAttr.value(lastTrainingReleaseIndx + 1);
		List<String> trainingReleases = new ArrayList<>();
		for (i = 0; i <= lastTrainingReleaseIndx; i++) {
			trainingReleases.add(versionAttr.value(i));
		}
		
		int numOfInstances = instances.numInstances();
		int firstTestingInstanceIndx = 0;
		// create training set
		Instances trainingSet = new Instances(instances, 0);
		for (i = 0; i < numOfInstances; i++) {
			Instance instance = instances.instance(i);
			String release = instance.stringValue(versionAttr);
			if (trainingReleases.contains(release)) {
				trainingSet.add(instance);
			} else {
				firstTestingInstanceIndx = i;
				break;
			}
		}
		trainingSet.setClassIndex(instances.numAttributes() - 1);
		// create testing set
		Instances testingSet = new Instances(instances, 0);
		for (i = 0; i < numOfInstances - firstTestingInstanceIndx; i++) {
			Instance instance = instances.instance(firstTestingInstanceIndx + i);
			String release = instance.stringValue(versionAttr);
			if (release.equals(testingRelease)) {
				testingSet.add(instance);
			} else {
				break;
			}
		}
		testingSet.setClassIndex(instances.numAttributes() - 1);
		
		// sampling
		for (i = 0; i < 3; i++) {
			Instances filteredTrSet = applySampling(i, trainingSet);
			if (filteredTrSet != null) {
				
			}
		}
		
		// feature selection
	}

	private static Instances applySampling(int sampling, Instances trainingSet) throws Exception {
		String[] opts;
		Instances filteredDataset;
		Float createdInstancesPercent;
		
		float majorityNum;
		float minorityNum = 0;
		int buggyIndx = trainingSet.numAttributes() - 1;
		for(Instance instance: trainingSet){
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
		if (minorityNum == 0 || majorityNum == 0) {
			return null;
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
			createdInstancesPercent = 100*(majorityNum - minorityNum) / minorityNum;
			SMOTE smote = new SMOTE();
			// specifies percentage of SMOTE instances to create
			opts = new String[]{ "-P", createdInstancesPercent.toString()};
			smote.setOptions(opts);
			smote.setInputFormat(trainingSet);
			filteredDataset = Filter.useFilter(trainingSet, smote);
			break;
		default:
			return null;
		
		}
		
		return filteredDataset;
	}

}
