package com.cristiano.isw2.isw2_deliverable_two;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateDataset {

	private static final String PROJECT = "BOOKKEEPER";
	private static final String DATASET = PROJECT + "-ds.csv";
	private static final Logger LOGGER = LoggerFactory.getLogger(CreateDataset.class);
	private static HashSet<String> set;
	
	private static void writeRow(FileWriter fileWriter, String releaseName, LocalDateTime releaseStart, LocalDateTime releaseEnd, Repository repository) {
		try (
	    		RevWalk revWalk = new RevWalk(repository);
	    		TreeWalk treeWalk = new TreeWalk(repository);
	    		) {
			RevFilter between = CommitTimeRevFilter.between(Date.from(releaseStart.atZone(ZoneId.systemDefault()).toInstant()),
					Date.from(releaseEnd.atZone(ZoneId.systemDefault()).toInstant()));
			revWalk.setRevFilter(between);
			revWalk.markStart(revWalk.parseCommit(repository.resolve(Constants.HEAD)));
	    	revWalk.sort(RevSort.REVERSE); // ordine cronologico
	    	for (;;) {
		    	RevCommit commit = revWalk.next();
		    	if (commit == null) {
		    		break;
		    	}
		    	RevTree tree = commit.getTree();
		    	TreeFilter treeFilter = PathSuffixFilter.create(".java");
		    	treeWalk.addTree(tree);
		    	treeWalk.setRecursive(false);
		    	treeWalk.setFilter(treeFilter);
		    	while (treeWalk.next()) {
		    	    if (treeWalk.isSubtree()) {
		    	        treeWalk.enterSubtree();
		    	    } else {
		    	    	// get class name (path)
		    	    	String path = treeWalk.getPathString();
		    	    	if (!set.contains(path)) {
		    	    		set.add(path);
		    	    		fileWriter.append(releaseName + "," + path + "\n");
		    	    	}
		    	    }
		    	}
		    	fileWriter.flush();
	    	}
    	} catch (IOException | JSONException e) {
			System.exit(1);
		}
	}
	
	public static void main(String[] args) {
		int i;
		int numOfReleases = 0;
		LocalDateTime releaseStart;
		LocalDateTime releaseEnd;
		String[] fields;
		String prev;
		String next;
		String releaseName;
		String releaseDate;
		final String URL = "https://github.com/apache/" + PROJECT + ".git";
		final String ROOT = "jgit/";
		
		try {
			// check number of release
			numOfReleases = GetReleaseInfo.firstHalf(PROJECT);
		} catch (JSONException | IOException e) {
			LOGGER.error(e.toString(), e);
		}
		if (numOfReleases < 2) {
			LOGGER.error("There are not enough versions for this project!");
			System.exit(1);
		}
		
		try {
	    	// create file CSV
	    	File out = new File(DATASET);
	    	Files.deleteIfExists(out.toPath());
	    	if (!out.createNewFile())
	    		throw new IOException();
	    	// delete a possible existent ROOT directory
	    	if (Files.exists(Path.of(ROOT)))
	    		FileUtils.deleteDirectory(new File(ROOT));
    	} catch (IOException e) {
    		System.exit(1);
    	}
		
    	try (
    			// clone the repository
    			Git git = Git.cloneRepository().setURI(URL).setDirectory(new File(ROOT)).call();
    			// take the repository
    	    	Repository repository = git.getRepository();
    			BufferedReader br = new BufferedReader(new FileReader(PROJECT + "VersionInfo.csv"));
    			FileWriter fileWriter = new FileWriter(DATASET);
    		) {
    		// file CSV initialization
		    fileWriter.append("Version,File Name");
		    fileWriter.append("\n");
	    	
			// discard the column names
			prev = br.readLine();
			prev = br.readLine();

			for (i = 1; i < numOfReleases + 1; i++) {
				next = br.readLine();
				
				fields = prev.split(",");
				releaseName = fields[0];
				releaseDate = fields[1];
				releaseStart = LocalDateTime.parse(releaseDate);
				
				fields = next.split(",");
				releaseDate = fields[1];
				releaseEnd = LocalDateTime.parse(releaseDate);
				
				prev = next;
				set =  new HashSet<>();
				writeRow(fileWriter, releaseName, releaseStart, releaseEnd, repository);
			}
    	} catch (GitAPIException | JSONException | IOException e) {
			LOGGER.error(e.toString(), e);
		}
    	// remove all support files
    	try {
	    	FileUtils.deleteDirectory(new File(ROOT));
	    	Files.deleteIfExists(Path.of(PROJECT + "VersionInfo.csv"));
    	} catch (IOException e) {
    		LOGGER.error(e.toString(), e);
    	}
	}

}
