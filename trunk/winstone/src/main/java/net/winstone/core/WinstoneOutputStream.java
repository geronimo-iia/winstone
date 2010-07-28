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
import net.winstone.log.Logger;
import net.winstone.log.LoggerFactory;
import net.winstone.util.StringUtils;

/**
 * Matches the socket output stream to the servlet output.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneOutputStream.java,v 1.20 2008/02/28 00:01:38 rickknowles Exp $
 */
public class WinstoneOutputStream extends javax.servlet.ServletOutputStream {

    private static Logger logger = LoggerFactory.getLogger(WinstoneOutputStream.class);
    
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final byte[] CR_LF = "\r\n".getBytes();
    protected OutputStream outStream;
    protected int bufferSize;
    protected int bufferPosition;
    protected int bytesCommitted;
    protected final ByteArrayOutputStream buffer;
    protected boolean committed;
    protected final boolean bodyOnly;
    protected WinstoneResponse owner;
    protected boolean disregardMode = false;
    protected boolean closed = false;
    protected Stack<ByteArrayOutputStream> includeByteStreams;
    private int contentLengthFromHeader = -1;

    /**
     * Constructor
     */
    public WinstoneOutputStream(OutputStream out, boolean bodyOnlyForInclude) {
        this.outStream = out;
        this.bodyOnly = bodyOnlyForInclude;
        this.bufferSize = DEFAULT_BUFFER_SIZE;
        this.committed = false;
        // this.headersWritten = false;
        this.buffer = new ByteArrayOutputStream();
    }

    public void setResponse(WinstoneResponse response) {
        this.owner = response;
    }

    public int getBufferSize() {
        return this.bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        if (this.owner.isCommitted()) {
            throw new IllegalStateException("OutputStream already committed");
        }
        this.bufferSize = bufferSize;
    }

    public boolean isCommitted() {
        return this.committed;
    }

    public int getOutputStreamLength() {
        return this.bytesCommitted + this.bufferPosition;
    }

    public int getBytesCommitted() {
        return this.bytesCommitted;
    }

    public void setDisregardMode(boolean disregard) {
        this.disregardMode = disregard;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    @Override
    public synchronized void write(int oneChar) throws IOException {
        if (this.disregardMode || this.closed) {
            return;
        } else if ((this.contentLengthFromHeader != -1) && (this.bytesCommitted >= this.contentLengthFromHeader)) {
            return;
        }
        // System.out.println("Out: " + this.bufferPosition + " char=" + (char)oneChar);
        this.buffer.write(oneChar);
        this.bufferPosition++;
        // if (this.headersWritten)
        if (this.bufferPosition >= this.bufferSize) {
            commit();
        } else if ((this.contentLengthFromHeader != -1) && ((this.bufferPosition + this.bytesCommitted) >= this.contentLengthFromHeader)) {
            commit();
        }
    }

    @Override
    public synchronized void write(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        } else if (this.disregardMode || this.closed) {
            return;
        } else if ((this.contentLengthFromHeader != -1) && (this.bytesCommitted >= this.contentLengthFromHeader)) {
            return;
        }
        int actualLength = Math.min(len, this.bufferSize - this.bufferPosition);
        this.buffer.write(b, off, actualLength);
        this.bufferPosition += actualLength;
        // if (this.headersWritten)
        if (this.bufferPosition >= this.bufferSize) {
            commit();
        } else if ((this.contentLengthFromHeader != -1) && ((this.bufferPosition + this.bytesCommitted) >= this.contentLengthFromHeader)) {
            commit();
        }

        // Write the remainder
        if (actualLength < len) {
            write(b, off + actualLength, len - actualLength);
        }
    }

    public void commit() throws IOException {
        this.buffer.flush();

        // If we haven't written the headers yet, write them out
        if (!this.committed && !this.bodyOnly) {
            this.owner.validateHeaders();
            this.committed = true;
            String contentLengthHeader = this.owner.getHeader(WinstoneConstant.CONTENT_LENGTH_HEADER);
            if (contentLengthHeader != null) {
                this.contentLengthFromHeader = Integer.parseInt(contentLengthHeader);
            }
            logger.debug("Committing response body");

            int statusCode = this.owner.getStatus();
            HttpProtocole reason = HttpProtocole.valueOf("HTTP_" + Integer.toString(statusCode));
            String statusLine = this.owner.getProtocol() + " " + statusCode + " " + (reason == null ? "No reason" : reason.toString());
            this.outStream.write(statusLine.getBytes("8859_1"));
            this.outStream.write(CR_LF);
            logger.debug("Response: " + statusLine);

            // Write headers and cookies
            for (Iterator<String> i = this.owner.getHeaders().iterator(); i.hasNext();) {
                String header = (String) i.next();
                this.outStream.write(header.getBytes("8859_1"));
                this.outStream.write(CR_LF);
                logger.debug("Header: " + header);
            }

            if (!this.owner.getHeaders().isEmpty()) {
                for (Iterator<Cookie> i = this.owner.getCookies().iterator(); i.hasNext();) {
                    Cookie cookie = (Cookie) i.next();
                    String cookieText = this.owner.writeCookie(cookie);
                    this.outStream.write(cookieText.getBytes("8859_1"));
                    this.outStream.write(CR_LF);
                    logger.debug("Header: " + cookieText);
                }
            }
            this.outStream.write(CR_LF);
            this.outStream.flush();
            // Logger.log(Logger.FULL_DEBUG,
            // Launcher.RESOURCES.getString("HttpProtocol.OutHeaders") + out.toString());
        }
        byte content[] = this.buffer.toByteArray();
        // winstone.ajp13.Ajp13Listener.packetDump(content, content.length);
        // this.buffer.writeTo(this.outStream);
        int commitLength = content.length;
        if (this.contentLengthFromHeader != -1) {
            commitLength = Math.min(this.contentLengthFromHeader - this.bytesCommitted, content.length);
        }
        if (commitLength > 0) {
            this.outStream.write(content, 0, commitLength);
        }
        this.outStream.flush();
        logger.debug("WinstoneOutputStream.CommittedBytes", Integer.toString(this.bytesCommitted + commitLength));

        this.bytesCommitted += commitLength;
        this.buffer.reset();
        this.bufferPosition = 0;
    }

    public void reset() {
        if (isCommitted()) {
            throw new IllegalStateException("OutputStream already committed");
        } else {
            logger.debug("WinstoneOutputStream.ResetBuffer", Integer.toString(this.bufferPosition));
            this.buffer.reset();
            this.bufferPosition = 0;
            this.bytesCommitted = 0;
        }
    }

    public void finishResponse() throws IOException {
        this.outStream.flush();
        this.outStream = null;
    }

    @Override
    public void flush() throws IOException {
        if (this.disregardMode) {
            return;
        }
        logger.debug("ServletOutputStream flushed");
        this.buffer.flush();
        this.commit();
    }

    @Override
    public void close() throws IOException {
        if (!isCommitted() && !this.disregardMode && !this.closed && (this.owner.getHeader(WinstoneConstant.CONTENT_LENGTH_HEADER) == null)) {
            if ((this.owner != null) && !this.bodyOnly) {
                this.owner.setContentLength(getOutputStreamLength());
            }
        }
        flush();
    }

    // Include related buffering
    public boolean isIncluding() {
        return (this.includeByteStreams != null && !this.includeByteStreams.isEmpty());
    }

    public void startIncludeBuffer() {
        synchronized (this.buffer) {
            if (this.includeByteStreams == null) {
                this.includeByteStreams = new Stack<ByteArrayOutputStream>();
            }
        }
        this.includeByteStreams.push(new ByteArrayOutputStream());
    }

    public void finishIncludeBuffer() throws IOException {
        if (isIncluding()) {
            ByteArrayOutputStream body = (ByteArrayOutputStream) this.includeByteStreams.pop();
            OutputStream topStream = this.outStream;
            if (!this.includeByteStreams.isEmpty()) {
                topStream = (OutputStream) this.includeByteStreams.peek();
            }
            byte bodyArr[] = body.toByteArray();
            if (bodyArr.length > 0) {
                topStream.write(bodyArr);
            }
            body.close();
        }
    }

    public void clearIncludeStackForForward() throws IOException {
        if (isIncluding()) {
            for (Iterator<ByteArrayOutputStream> i = this.includeByteStreams.iterator(); i.hasNext();) {
                ((ByteArrayOutputStream) i.next()).close();
            }
            this.includeByteStreams.clear();
        }
    }
}
