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
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
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
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateDataset {

	private static final String PROJECT = "BOOKKEEPER";
	private static final String DATASET = PROJECT + "-ds.csv";
	private static final Logger LOGGER = LoggerFactory.getLogger(CreateDataset.class);
	
	private static Set<String> analyzeRevisions(MetricsCalculator mc, FileWriter fileWriter, String releaseName, LocalDateTime releaseStart, LocalDateTime releaseEnd, Repository repository) {
		try (
	    		RevWalk revWalk = new RevWalk(repository);
	    		) {
			RevFilter between = CommitTimeRevFilter.between(Date.from(releaseStart.atZone(ZoneId.systemDefault()).toInstant()),
					Date.from(releaseEnd.atZone(ZoneId.systemDefault()).toInstant()));
			revWalk.setRevFilter(between);
			revWalk.markStart(revWalk.parseCommit(repository.resolve(Constants.HEAD)));
	    	revWalk.sort(RevSort.REVERSE); // ordine cronologico
	    	RevCommit lastCommit = null;
	    	for (;;) {
		    	RevCommit commit = revWalk.next();
		    	if (commit == null) {
		    		break;
		    	}
		    	String author = commit.getAuthorIdent().getName();
		    	
		    	RevCommit parent = revWalk.parseCommit(commit.getParent(0).getId());
				DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
				df.setRepository(repository);
				df.setDiffComparator(RawTextComparator.DEFAULT);
				df.setDetectRenames(true);
				List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());
				int chgSetSize = diffs.size();
				for (DiffEntry diff : diffs) {
					String path = diff.getNewPath();
					String extension = FilenameUtils.getExtension(path);
					if (!extension.equals("java")) {
						continue;
					}
					String oldPath = diff.getOldPath();
					if (!path.equals(oldPath) && FilenameUtils.getExtension(oldPath).equals("java")) {
						mc.renameClass(oldPath, path);
					}
					int linesAdded = 0;
					int linesDeleted = 0;
					for (Edit edit : df.toFileHeader(diff).toEditList()) {
			            linesDeleted += edit.getEndA() - edit.getBeginA();
			            linesAdded += edit.getEndB() - edit.getBeginB();
			        }
					mc.addData(path, (float)linesAdded, (float)linesDeleted, (float)chgSetSize, author);
				}
				df.close();
		    	lastCommit = commit;
	    	}
	    	if (lastCommit != null) {
	    		return getListOfClasses(lastCommit, repository);
	    	}
    	} catch (IOException | JSONException e) {
			System.exit(1);
		}
		
		return new HashSet<>();
	}
	
	private static Set<String> getListOfClasses(RevCommit lastCommit, Repository repository) {
		Set<String> classes = new HashSet<>();
		// get only existing classes at the end of the release
		try (
	    		TreeWalk treeWalk = new TreeWalk(repository);
			) {
	    	RevTree tree = lastCommit.getTree();
	    	TreeFilter treeFilter = PathSuffixFilter.create(".java");
	    	treeWalk.addTree(tree);
	    	treeWalk.setRecursive(false);
	    	treeWalk.setFilter(treeFilter);
	    	while (treeWalk.next()) {
	    	    if (treeWalk.isSubtree()) {
	    	        treeWalk.enterSubtree();
	    	    } else {
	    	    	// get class name (path)
	    	    	classes.add(treeWalk.getPathString());
	    	    }
	    	}
		} catch (IOException | JSONException e) {
			System.exit(1);
		}
		
		return classes;
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
		MetricsCalculator mc;
		
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
		    fileWriter.append("Version,File Name,NR,NAuth,LOC_added,MAX_LOC_added,AVG_LOC_added,Churn,MAX_Churn,AVG_Churn,ChgSetSize,MAX_ChgSet,AVG_ChgSet");
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

				mc = new MetricsCalculator();
				Set<String> classesInRelease = analyzeRevisions(mc, fileWriter, releaseName, releaseStart, releaseEnd, repository);
				writeOnFile(fileWriter, releaseName, mc, classesInRelease);
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

	private static void writeOnFile(FileWriter fileWriter, String release, MetricsCalculator mc, Set<String> classes) throws IOException {
		for (String cl : classes) {
			Float m1 = mc.getComputedMetric(cl, MetricsCalculator.NR);
			Float m2 = mc.getComputedMetric(cl, MetricsCalculator.NAUTH);
			Float m3 = mc.getComputedMetric(cl, MetricsCalculator.LOC_ADDED);
			Float m4 = mc.getComputedMetric(cl, MetricsCalculator.MAX_LOC_ADDED);
			Float m5 = mc.getComputedMetric(cl, MetricsCalculator.AVG_LOC_ADDED);
			Float m6 = mc.getComputedMetric(cl, MetricsCalculator.CHURN);
			Float m7 = mc.getComputedMetric(cl, MetricsCalculator.MAX_CHURN);
			Float m8 = mc.getComputedMetric(cl, MetricsCalculator.AVG_CHURN);
			Float m9 = mc.getComputedMetric(cl, MetricsCalculator.MAX_CHG_SET);
			Float m10 = mc.getComputedMetric(cl, MetricsCalculator.AVG_CHG_SET);
			
			fileWriter.append(release + "," + cl + "," + m1 + "," + m2 + "," + m3 + "," + m4 + "," + m5 + "," + m6 + "," + m7 + "," + m8 + "," + m9 + "," + m10 + "," + "\n");
			fileWriter.flush();
		}
	}
}
