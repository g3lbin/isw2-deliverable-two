package com.cristiano.isw2.isw2_deliverable_two.milestone_one;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateDataset {

	private static List<JiraIssue> issues;
	private static List<ReleaseInfo> releases;
	private static final String PROJECT = "BOOKKEEPER";
	private static final String DATASET = "results/" + PROJECT + "-ds.csv";
	private static final Logger LOGGER = LoggerFactory.getLogger(CreateDataset.class);
	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
	
	private static void analyzeCommits(Repository repository, LocalDateTime firstConsiderableDate, LocalDateTime lastConsiderableDate) {
		try (
	    		RevWalk revWalk = new RevWalk(repository);
	    		) {
			revWalk.setRevFilter(CommitTimeRevFilter.after(Date.from(firstConsiderableDate.atZone(ZoneId.systemDefault()).toInstant())));
			revWalk.markStart(revWalk.parseCommit(repository.resolve(Constants.HEAD)));
	    	revWalk.sort(RevSort.REVERSE); // chronological order
	    	RevCommit lastCommit = null;
	    	ReleaseInfo lastRelease = releases.get(0);
	    	for (;;) {
		    	RevCommit commit = revWalk.next();
		    	if (commit == null) {
		    		break;
		    	} else if (commit.getParentCount() > 0) {
		    		// retrieve commit data
		    		PersonIdent authorIdent = commit.getAuthorIdent();
		    		String author = authorIdent.getName();
		    		TimeZone authorTimeZone = authorIdent.getTimeZone();
		    		LocalDateTime commitDate = Instant.ofEpochSecond(commit.getCommitTime()).atZone(authorTimeZone.toZoneId()).toLocalDateTime();
		    		
		    		boolean computeMetrics = false;
		    		if (commitDate.compareTo(lastConsiderableDate) < 0) {
						computeMetrics = true;
					}
		    		// get commit appertaining release
		    		ReleaseInfo release = null;
		    		int releaseIndx = getCommitRelease(commitDate);
		    		if (releaseIndx != -1) {
			    		release = releases.get(releaseIndx);
		    		}
		    		// analyze diffs from commit
			    	RevCommit parent = revWalk.parseCommit(commit.getParent(0).getId());
					DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
					df.setRepository(repository);
					df.setDiffComparator(RawTextComparator.DEFAULT);
					df.setDetectRenames(true);
					List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());
					analyzeDiffs(df, diffs, releaseIndx, author, getAVfromFixedBugs(commit, commitDate), computeMetrics);
					df.close();
					// check if this commit is the first of the release
					if (lastRelease != null && !lastRelease.equals(release)) {
						// get the final classes tree (which will be included in the dataset) from last commit of previous release
			    		lastRelease.setClassesAtTheEnd(getListOfClasses(lastCommit, repository));
			    	}
			    	lastCommit = commit;
			    	lastRelease = release;
		    	}
	    	}
    	} catch (IOException | JSONException e) {
			System.exit(1);
		}
	}
	
	private static Set<String> getAVfromFixedBugs(RevCommit commit, LocalDateTime commitDate) {
		Set<String> av = new HashSet<>();
		String msg = commit.getFullMessage();
		for (JiraIssue issue : issues) {
			LocalDateTime dateCreated = LocalDateTime.parse(issue.getCreated(), formatter);
			if (commitDate.compareTo(dateCreated) < 0) {
				break;
			}
			if (msg.contains(issue.getKey())) {
				av.addAll(issue.getAv());
			}	
		}
		
		return av;
	}
	
	private static void analyzeDiffs(DiffFormatter df, List<DiffEntry> diffs, int releaseIndx, String author, Set<String> av, boolean computeMetrics) throws IOException {
		for (DiffEntry diff : diffs) {
			String path = diff.getNewPath();
			String extension = FilenameUtils.getExtension(path);
			if (!extension.equals("java")) {
				continue;
			}
			if (computeMetrics) {
				String oldPath = diff.getOldPath();
				if (!path.equals(oldPath) && FilenameUtils.getExtension(oldPath).equals("java")) {
					releases.get(releaseIndx).renameClass(oldPath, path);
				}
				int linesAdded = 0;
				int linesDeleted = 0;
				for (Edit edit : df.toFileHeader(diff).toEditList()) {
		            linesDeleted += edit.getEndA() - edit.getBeginA();
		            linesAdded += edit.getEndB() - edit.getBeginB();
		        }
				int chgSetSize = diffs.size();
				releases.get(releaseIndx).addData(path, (float)linesAdded, (float)linesDeleted, (float)chgSetSize, author);
			}
			// set the bugginess of the class in all affected versions
			if (!av.isEmpty()) {
				setBugginessInAV(path, av);
			}
		}
	}
	
	private static void setBugginessInAV(String className, Set<String> av) {
		for (ReleaseInfo r : releases) {
			if (av.contains(r.getName())) {
				r.setBugginess(className);
			}
		}
	}
	
	private static Set<String> getListOfClasses(RevCommit commit, Repository repository) {
		Set<String> classes = new HashSet<>();
		// get only existing classes at the end of the release
		try (
	    		TreeWalk treeWalk = new TreeWalk(repository);
			) {
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
	    	    	classes.add(treeWalk.getPathString());
	    	    }
	    	}
		} catch (IOException | JSONException e) {
			System.exit(1);
		}
		
		return classes;
	}
	
	private static int getCommitRelease(LocalDateTime commitDate) {
		int checkStart;
		int checkEnd;
		
		for (int i = 0; i < releases.size(); i++) {
			ReleaseInfo release = releases.get(i);
			checkStart = commitDate.compareTo(release.getStart());
			checkEnd = commitDate.compareTo(release.getEnd());
	        if(checkStart >= 0 && checkEnd < 0) {
	        	// dateCreated >= start_release && dateCreated < start_end
	        	return i;
	        }
		}
		
		return -1;
	}

	private static void writeOnFile(FileWriter fileWriter, ReleaseInfo release, Set<String> classes) throws IOException {
		for (String cl : classes) {
			fileWriter.append(release.getCsvRow(cl));
			fileWriter.flush();
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
		LocalDateTime firstConsiderableDate = null;
		LocalDateTime lastConsiderableDate = null;
		final String URL = "https://github.com/apache/" + PROJECT + ".git";
		final String ROOT = "jgit/";
		
		try {
			// check number of release
			numOfReleases = GetReleaseInfo.createOutputFile(PROJECT);
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
    			BufferedReader br = new BufferedReader(new FileReader(PROJECT + "VersionInfo.csv"));
    			FileWriter fileWriter = new FileWriter(DATASET);
    		) {
    		// take the repository
	    	Repository repository = git.getRepository();
    		// file CSV initialization
		    fileWriter.append("Version,File Name,NR,NAuth,LOC_added,MAX_LOC_added,AVG_LOC_added,Churn,MAX_Churn,AVG_Churn,MAX_ChgSet,AVG_ChgSet,Buggy");
		    fileWriter.append("\n");

			// discard the column names
			prev = br.readLine();
			prev = br.readLine();
			
			// get the date of the first release
			fields = prev.split(",");
			releaseDate = fields[1];
			firstConsiderableDate = LocalDateTime.parse(releaseDate);

			releases = new ArrayList<>();
			for (i = 0; i < numOfReleases; i++) {
				fields = prev.split(",");
				releaseName = fields[0];
				releaseDate = fields[1];
				releaseStart = LocalDateTime.parse(releaseDate);
				
				if (i == numOfReleases - 1) {
					// assume last release has end date = start date + 1 day
					ReleaseInfo release = new ReleaseInfo(releaseName, releaseStart, releaseStart.plusDays(1));
					releases.add(release);
					
					break;
				}
				next = br.readLine();
				
				fields = next.split(",");
				releaseDate = fields[1];
				releaseEnd = LocalDateTime.parse(releaseDate);
				
				prev = next;

				ReleaseInfo release = new ReleaseInfo(releaseName, releaseStart, releaseEnd);
				releases.add(release);
				
				if (i == (numOfReleases / 2) - 1) {
					lastConsiderableDate = releaseEnd;
				}
			}
			// get all issue related to bugs from Jira
    		issues = RetrieveTicketsFromJira.findIssues(PROJECT, releases);
    		analyzeCommits(repository, firstConsiderableDate, lastConsiderableDate);
    		createDataset(fileWriter, numOfReleases);
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

	private static void createDataset(FileWriter fileWriter, int numOfReleases) throws IOException {
		Set<String> lastNonEmptySet = releases.get(0).getClassesAtTheEnd();
		for (int i = 0; i < numOfReleases / 2; i++) {
			ReleaseInfo release = releases.get(i);
			Set<String> classesInRelease = release.getClassesAtTheEnd();
			if (classesInRelease.isEmpty() && i != 0) {
				classesInRelease = lastNonEmptySet;
			} else if (classesInRelease.isEmpty()) {
				continue;
			}
			writeOnFile(fileWriter, release, classesInRelease);
			lastNonEmptySet = classesInRelease;
		}
	}

}
