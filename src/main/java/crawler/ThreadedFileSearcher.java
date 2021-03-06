package crawler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class ThreadedFileSearcher implements Runnable {

	public void run() {
		try {
			boolean finishedWork = false;
			File directoryToSearchAt = null;
			do {
				try {
					directoryToSearchAt = HyperScraper.localDirectoryQueue.take();
				} catch (InterruptedException e) {
					HyperScraper.fileErrorHandler(e);
				}
				if (directoryToSearchAt.exists()) {
					try {
						searchForFiles(directoryToSearchAt);
					} catch (Exception e1) {
						e1.printStackTrace((new PrintWriter(String.format("logs/%s/%s/ErrorLogs/FILEErrorLogs.txt",
								HyperScraper.dateString, HyperScraper.hourString))));
					}
				}
				else {
					finishedWork = true;
				}
			} while (!finishedWork);
		} catch (FileNotFoundException e) {
			HyperScraper.fileErrorHandler(e);
		}
	}

	private static void searchForFiles(File i_DirectoryToSearchAt) {
		File[] innerFiles = i_DirectoryToSearchAt.listFiles();
		if (innerFiles != null) {
			for (File file : innerFiles) {
				if (file.isFile() && file.getName().contains("css")) {
					if (!HyperScraper.localFileMap.containsKey(file)) {
						addFileToFileQueue(file);
					} else if (HyperScraper.localFileMap.get(file)) {

						// In this case the file was already uploaded and there
						// exists a duplicate
						file.delete();
					}
				}
			}
		}
	}


	/**
	 * Adds the given file to the file queue. The assumption is that this happens only if the file was not already uploaded.
	 * @param i_File - The given file to upload.
	 */
	private static void addFileToFileQueue(File i_File) {
		try {
			HyperScraper.localFileQueue.put(i_File);
		} catch (InterruptedException e) {
			HyperScraper.fileErrorHandler(e);
		}
		HyperScraper.localFileMap.put(i_File, false);
	}
}