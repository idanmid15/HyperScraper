package crawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import com.jcraft.jsch.ChannelSftp;

public class ThreadedUploader extends SftpCommunicator implements Runnable {
	static String SFTPWORKINGDIR = "/home/ct_dummytest/dummytest.clicktale-samples.com/HyperScraper/";

	public ThreadedUploader(Configuration config) {
		setupProperties(config);
	}
	/**
	 * Starts the thread.
	 */
	public void run() {
		connect();
		ChannelSftp channelSftp = (ChannelSftp) channel;
		File localFile = null;

		// Wait for the first files to arrive.
		do {
			try {
				localFile = HyperScraper.localFileQueue.take();
			} catch (InterruptedException e1) {
				HyperScraper.fileErrorHandler(e1);
			}
			if (HyperScraper.localFileQueue.size() == 0) {
				synchronized (HyperScraper.localSearcherWakeLock) {
					HyperScraper.localSearcherWakeLock.notify();
				}
			}
			if (!localFile.getName().contains("endthis")) {
				try {
					uploadFile(channelSftp, localFile);
				} catch (IOException e) {
					handleUploadError(e, "Connection to SFTP Error");
				}
			}
		} while (!localFile.getName().contains("endthis"));
		disconnect();
	}

	/**
	 * Uploads a single file to the SFTP (synced)
	 * 
	 * @param channelSftp
	 *            - The channelSftp.
	 * @param i_LocalFile
	 *            - The local file to upload.
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	protected void uploadFile(ChannelSftp channelSftp, File i_LocalFile) throws IOException, FileNotFoundException {
		String ftpPath = null;
		boolean validUpload = true;
		String localSFTPWORKINGDIR = (SFTPWORKINGDIR + i_LocalFile.getPath().substring(0, 10)).replace('\\', '/');
		String localPath = i_LocalFile.toString().substring(i_LocalFile.toString().indexOf("\\companies")).replace('\\',
				'/');
		ftpPath = (localSFTPWORKINGDIR + localPath).substring(0, (localSFTPWORKINGDIR + localPath).lastIndexOf('/'));
		InputStream iStream = new FileInputStream(i_LocalFile);
		try {
			synchronized (HyperScraper.sftpLock) {
				channelSftp.cd(ftpPath);
				channelSftp.put(iStream, i_LocalFile.getName());
				HyperScraper.totalTransferredSizeBytes += i_LocalFile.length();
			}
		} catch (Exception e) {
			validUpload = false;
			handleUploadError(e, i_LocalFile.toString());
		}
		iStream.close();
		if (validUpload) {
			HyperScraper.localFileMap.put(i_LocalFile, true);
			writeStatusToUploadLogFile(String.format("%s: File %d: %s Size = %.4fMb\n",
					new Date().toString().substring(11, 19), ++HyperScraper.uploadFileCount, i_LocalFile,
					(double) i_LocalFile.length() / Math.pow(2, 20)));
			i_LocalFile.delete();
		}
	}
	
	@Override
	String[] connectionMessages() {
		return new String[] {"Upload thread connecting to SFTP server...\n", "Upload thread's connection established!\n"};
	}
	
	@Override
	String disconnectionMessage() {
		return "Upload thread terminated!\n";
	}
}
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    