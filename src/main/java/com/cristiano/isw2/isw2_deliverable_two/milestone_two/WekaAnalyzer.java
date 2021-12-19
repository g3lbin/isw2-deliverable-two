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

public class WekaAnalyzer {
	
	private static final String PROJECT1 = "BOOKKEEPER";
	private static final String PROJECT2 = "ZOOKEEPER";
	private static final String RESULTS = "results.csv";
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
		
		
	}

	

}
