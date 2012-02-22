/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The request stream management class.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneInputStream.java,v 1.4 2006/02/28 07:32:47 rickknowles
 *          Exp $
 */
public class WinstoneInputStream extends javax.servlet.ServletInputStream {

	protected static Logger logger = LoggerFactory.getLogger(WinstoneInputStream.class);
	final int BUFFER_SIZE = 4096;
	private final InputStream inData;
	private final ByteArrayOutputStream dump;
	private Integer contentLength;
	private int readSoFar;

	/**
	 * Constructor
	 */
	public WinstoneInputStream(final InputStream inData) {
		super();
		this.inData = inData;
		dump = new ByteArrayOutputStream();
	}

	public WinstoneInputStream(final byte inData[]) {
		this(new ByteArrayInputStream(inData));
	}

	public InputStream getRawInputStream() {
		return inData;
	}

	public void setContentLength(final int length) {
		contentLength = new Integer(length);
		readSoFar = 0;
	}

	@Override
	public int read() throws IOException {
		if (contentLength == null) {
			final int data = inData.read();
			dump.write(data);
			// System.out.println("Char: " + (char) data);
			return data;
		} else if (contentLength.intValue() > readSoFar) {
			readSoFar++;
			final int data = inData.read();
			dump.write(data);
			// System.out.println("Char: " + (char) data);
			return data;
		} else {
			return -1;
		}
	}

	public void finishRequest() {
		// this.inData = null;
		// byte content[] = this.dump.toByteArray();
		// com.rickknowles.winstone.ajp13.Ajp13Listener.packetDump(content,
		// content.length);
	}

	@Override
	public int available() throws IOException {
		return inData.available();
	}

	/**
	 * Wrapper for the servletInputStream's readline method
	 */
	public byte[] readLine() throws IOException {
		// System.out.println("ReadLine()");
		final byte buffer[] = new byte[BUFFER_SIZE];
		final int charsRead = super.readLine(buffer, 0, BUFFER_SIZE);
		if (charsRead == -1) {
			WinstoneInputStream.logger.debug("End of stream");
			return new byte[0];
		}
		final byte outBuf[] = new byte[charsRead];
		System.arraycopy(buffer, 0, outBuf, 0, charsRead);
		return outBuf;
	}
}
