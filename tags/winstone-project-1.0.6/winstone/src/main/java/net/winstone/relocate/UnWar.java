package net.winstone.relocate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import net.winstone.util.FileUtils;

/**
 * Explode war file format in a specified directory.
 * 
 * @author Jerome Guibert
 */
public class UnWar {

	private final File warDirectory;

	/**
	 * @param webappDir
	 *            the directory where "unwar" webapplication. if this parameter
	 *            is not specified, directory will be a
	 *            'user.dir/localhost/webapps'
	 * @throws IllegalStateException
	 *             if webappDir is not a directory.
	 */
	public UnWar(final String webappDir) throws IllegalStateException {
		super();
		// set default directory
		if (webappDir == null) {
			warDirectory = new File(System.getProperty("user.dir") + File.separator + "localhost" + File.separator + "webapps");
		} else {
			warDirectory = new File(webappDir);
		}
		// check for directory existence
		if (!warDirectory.exists()) {
			warDirectory.mkdirs();
		}
		if (!warDirectory.isDirectory()) {
			throw new IllegalStateException(String.format("WarDirectory %s is not a directory!", warDirectory.getAbsolutePath()));
		}
	}

	/**
	 * Deploy the specified war file.
	 * 
	 * @param warFile
	 * @throws IllegalStateException
	 */
	public void deploy(final File warFile) throws IllegalStateException {
		// simple test
		if (!warFile.exists()) {
			throw new IllegalStateException(String.format("War file %s did not exists.", warFile.getAbsolutePath()));
		}
		// build context name
		String context = warFile.getName();
		final int index = context.lastIndexOf(".");
		if (index > 0) {
			context = context.substring(0, index);
		}

		ZipFile zipFile = null;
		try {
			// build target directory
			final File targetDirectory = new File(warDirectory, context);
			if (!targetDirectory.exists()) {
				targetDirectory.mkdirs();
			}
			if (!targetDirectory.isDirectory()) {
				throw new IllegalStateException(String.format("Target deployement directory %s is not a directory!", targetDirectory.getAbsolutePath()));
			}
			zipFile = new ZipFile(warFile);
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry ze = entries.nextElement();
				String en = ze.getName();
				// adapt path
				if (File.separatorChar == '/') {
					en = en.replace('\\', File.separatorChar);
				}
				final File outFile = new File(targetDirectory, en);
				if (ze.isDirectory()) {
					// entry is a directory
					outFile.mkdirs();
				} else {
					// entry is a file
					OutputStream os = null;
					InputStream is = null;
					final File parentFile = outFile.getParentFile();
					if (parentFile.exists() == Boolean.FALSE) {
						parentFile.mkdirs();
					}
					// let local modification in place
					if (outFile.exists() && (outFile.lastModified() >= ze.getTime())) {
						continue;
					}
					// copy to output file
					try {
						os = new FileOutputStream(outFile);
						is = zipFile.getInputStream(ze);
						FileUtils.copyStream(is, os);
						outFile.setLastModified(ze.getTime());
					} catch (final IOException ioe2) {
						// server.log("Problem in extracting " + en + " " +
						// ioe2);
					} finally {
						try {
							os.close();
						} catch (final Exception e2) {

						}
						try {
							is.close();
						} catch (final Exception e2) {

						}
					}
				}
			}
		} catch (final ZipException e) {
			throw new IllegalStateException("Invalid .war format", e);
		} catch (final IOException e) {
			throw new IllegalStateException("Can't read " + warFile + "/ " + e.getMessage(), e);
		} finally {
			try {
				zipFile.close();
			} catch (final Exception e) {
			}
			zipFile = null;
		}

	}
}
