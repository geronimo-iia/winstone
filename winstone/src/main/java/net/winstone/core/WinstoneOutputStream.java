/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Stack;

import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Matches the socket output stream to the servlet output.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneOutputStream.java,v 1.20 2008/02/28 00:01:38
 *          rickknowles Exp $
 */
public class WinstoneOutputStream extends javax.servlet.ServletOutputStream {

	private static Logger logger = LoggerFactory.getLogger(WinstoneOutputStream.class);

	private static final int DEFAULT_BUFFER_SIZE = 8192;
	private static final byte[] CR_LF = "\r\n".getBytes();
	protected OutputStream outStream;
	protected int bufferSize;
	protected int bufferPosition;
	protected long bytesCommitted;
	protected final ByteArrayOutputStream buffer;
	protected boolean committed;
	protected final boolean bodyOnly;
	protected WinstoneResponse owner;
	protected boolean disregardMode = false;
	protected boolean closed = false;
	protected Stack<ByteArrayOutputStream> includeByteStreams;
	private long contentLengthFromHeader = -1;

	/**
	 * Constructor
	 */
	public WinstoneOutputStream(final OutputStream out, final boolean bodyOnlyForInclude) {
		outStream = out;
		bodyOnly = bodyOnlyForInclude;
		bufferSize = WinstoneOutputStream.DEFAULT_BUFFER_SIZE;
		committed = false;
		// this.headersWritten = false;
		buffer = new ByteArrayOutputStream();
	}

	public void setResponse(final WinstoneResponse response) {
		owner = response;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(final int bufferSize) {
		if (owner.isCommitted()) {
			throw new IllegalStateException("OutputStream already committed");
		}
		this.bufferSize = bufferSize;
	}

	public boolean isCommitted() {
		return committed;
	}

	public long getOutputStreamLength() {
		return bytesCommitted + bufferPosition;
	}

	public long getBytesCommitted() {
		return bytesCommitted;
	}

	public void setDisregardMode(final boolean disregard) {
		disregardMode = disregard;
	}

	public void setClosed(final boolean closed) {
		this.closed = closed;
	}

	@Override
	public synchronized void write(final int oneChar) throws IOException {
		if (disregardMode || closed) {
			return;
		} else if ((contentLengthFromHeader != -1) && (bytesCommitted >= contentLengthFromHeader)) {
			return;
		}
		// System.out.println("Out: " + this.bufferPosition + " char=" +
		// (char)oneChar);
		buffer.write(oneChar);
		bufferPosition++;
		// if (this.headersWritten)
		if (bufferPosition >= bufferSize) {
			commit();
		} else if ((contentLengthFromHeader != -1) && ((bufferPosition + bytesCommitted) >= contentLengthFromHeader)) {
			commit();
		}
	}

	@Override
	public synchronized void write(final byte b[], final int off, final int len) throws IOException {
		if (b == null) {
			throw new NullPointerException();
		} else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return;
		} else if (disregardMode || closed) {
			return;
		} else if ((contentLengthFromHeader != -1) && (bytesCommitted >= contentLengthFromHeader)) {
			return;
		}
		final int actualLength = Math.min(len, bufferSize - bufferPosition);
		buffer.write(b, off, actualLength);
		bufferPosition += actualLength;
		// if (this.headersWritten)
		if (bufferPosition >= bufferSize) {
			commit();
		} else if ((contentLengthFromHeader != -1) && ((bufferPosition + bytesCommitted) >= contentLengthFromHeader)) {
			commit();
		}

		// Write the remainder
		if (actualLength < len) {
			write(b, off + actualLength, len - actualLength);
		}
	}

	public void commit() throws IOException {
		buffer.flush();

		// If we haven't written the headers yet, write them out
		if (!committed && !bodyOnly) {
			owner.validateHeaders();
			committed = true;
			final String contentLengthHeader = owner.getHeader(WinstoneConstant.CONTENT_LENGTH_HEADER);
			if (contentLengthHeader != null) {
				contentLengthFromHeader = Long.parseLong(contentLengthHeader);
			}
			WinstoneOutputStream.logger.debug("Committing response body");

			final int statusCode = owner.getStatus();
			final HttpProtocole reason = HttpProtocole.valueOf("HTTP_" + Integer.toString(statusCode));
			final String statusLine = owner.getProtocol() + " " + statusCode + " " + (reason == null ? "No reason" : reason.toString());
			outStream.write(statusLine.getBytes("8859_1"));
			outStream.write(WinstoneOutputStream.CR_LF);
			WinstoneOutputStream.logger.debug("Response: " + statusLine);

			// Write headers and cookies
			for (final Iterator<String> i = owner.getHeaders().iterator(); i.hasNext();) {
				final String header = i.next();
				outStream.write(header.getBytes("8859_1"));
				outStream.write(WinstoneOutputStream.CR_LF);
				WinstoneOutputStream.logger.debug("Header: " + header);
			}

			if (!owner.getHeaders().isEmpty()) {
				for (final Iterator<Cookie> i = owner.getCookies().iterator(); i.hasNext();) {
					final Cookie cookie = i.next();
					final String cookieText = owner.writeCookie(cookie);
					outStream.write(cookieText.getBytes("8859_1"));
					outStream.write(WinstoneOutputStream.CR_LF);
					WinstoneOutputStream.logger.debug("Header: " + cookieText);
				}
			}
			outStream.write(WinstoneOutputStream.CR_LF);
			outStream.flush();
			// Logger.log(Logger.FULL_DEBUG,
			// Launcher.RESOURCES.getString("HttpProtocol.OutHeaders") +
			// out.toString());
		}
		final byte content[] = buffer.toByteArray();
		// winstone.ajp13.Ajp13Listener.packetDump(content, content.length);
		// this.buffer.writeTo(this.outStream);
		int commitLength = content.length;
		if (contentLengthFromHeader != -1) {
			final long delta = contentLengthFromHeader - bytesCommitted;
			if (delta < Integer.MAX_VALUE) {
				commitLength = Math.min((int) delta, content.length);
			} else {
				commitLength = content.length;
			}
		}
		if (commitLength > 0) {
			outStream.write(content, 0, commitLength);
		}
		outStream.flush();
		WinstoneOutputStream.logger.debug("Written {} bytes to response body", Long.toString(bytesCommitted + commitLength));

		bytesCommitted += commitLength;
		buffer.reset();
		bufferPosition = 0;
	}

	public void reset() {
		if (isCommitted()) {
			throw new IllegalStateException("OutputStream already committed");
		} else {
			WinstoneOutputStream.logger.debug("WResetting buffer - discarding {} bytes", Integer.toString(bufferPosition));
			buffer.reset();
			bufferPosition = 0;
			bytesCommitted = 0;
		}
	}

	public void finishResponse() throws IOException {
		outStream.flush();
		outStream = null;
	}

	@Override
	public void flush() throws IOException {
		if (disregardMode) {
			return;
		}
		WinstoneOutputStream.logger.debug("ServletOutputStream flushed");
		buffer.flush();
		commit();
	}

	@Override
	public void close() throws IOException {
		if (!isCommitted() && !disregardMode && !closed && (owner.getHeader(WinstoneConstant.CONTENT_LENGTH_HEADER) == null)) {
			if ((owner != null) && !bodyOnly) {
				owner.setContentLength(getOutputStreamLength());
			}
		}
		flush();
	}

	// Include related buffering
	public boolean isIncluding() {
		return ((includeByteStreams != null) && !includeByteStreams.isEmpty());
	}

	public void startIncludeBuffer() {
		synchronized (buffer) {
			if (includeByteStreams == null) {
				includeByteStreams = new Stack<ByteArrayOutputStream>();
			}
		}
		includeByteStreams.push(new ByteArrayOutputStream());
	}

	public void finishIncludeBuffer() throws IOException {
		if (isIncluding()) {
			final ByteArrayOutputStream body = includeByteStreams.pop();
			OutputStream topStream = outStream;
			if (!includeByteStreams.isEmpty()) {
				topStream = includeByteStreams.peek();
			}
			final byte bodyArr[] = body.toByteArray();
			if (bodyArr.length > 0) {
				topStream.write(bodyArr);
			}
			body.close();
		}
	}

	public void clearIncludeStackForForward() throws IOException {
		if (isIncluding()) {
			for (final Iterator<ByteArrayOutputStream> i = includeByteStreams.iterator(); i.hasNext();) {
				i.next().close();
			}
			includeByteStreams.clear();
		}
	}
}
