/*
 * Generator Runtime Servlet Framework
 * Copyright (C) 2004 Rick Knowles
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * Version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License Version 2 for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * Version 2 along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.winstone.filters.gzip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Wraps the response object with an output buffer, and which waits until 
 * the full response is written, after which it commits and writes a 
 * gzip compressed version, with the transfer encoding header set to gzipped.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: GzipResponseWrapper.java,v 1.2 2005/09/10 06:34:35 rickknowles Exp $
 */
public class GzipResponseWrapper extends HttpServletResponseWrapper {

    private ByteArrayServletOutputStream bufferStream;
    private PrintWriter bufferWriter;
    private ServletContext context;
    
    public GzipResponseWrapper(final HttpServletResponse response,final  ServletContext context) {
        super(response);
        this.context = context;
        setHeader("Content-Encoding", "gzip");
    }
    
    public void close() throws IOException {
        
        if (this.bufferWriter != null) {
            this.bufferWriter.flush();
        }
        
        // reset content length, and echo out
        if (this.bufferStream != null) {
            byte content[] = this.bufferStream.getContent();
            long startTime = System.currentTimeMillis(); 
            
            int initialContentLength = content.length;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(buffer);
            gzip.write(content);
            gzip.close();
            content = buffer.toByteArray();
            buffer.close();
            
            context.log("Gzipped " + initialContentLength + " bytes of " +
                    "response content to " + content.length + " bytes in " +
                    (System.currentTimeMillis() - startTime) + "ms");
            
            // Set content length and write out
            super.setContentLength(content.length);
            ServletOutputStream out = null;
            try {
                out = super.getOutputStream();
            } catch (IllegalStateException err) {
                throw new IOException("Can't use a gzip response wrapper on included files");
            }
            out.write(content);
            out.flush();
        }
    }
    
    @Override
    public void setContentLength(int length) {/* do nothing */}
    
    /**
     * Returns a stream wrapper 
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (this.bufferStream != null) {
            return this.bufferStream;
        } else {
            //  Assumes we only map this filter to appropriate objects
            this.bufferStream = new ByteArrayServletOutputStream();
            return this.bufferStream;
        }
    }
    
    /**
     * Returns a writer wrapper
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (this.bufferWriter != null) {
            return this.bufferWriter;
        } else {
            // this is actually against servlet spec, but return a 
            // writer only, if the stream has already been requested
            if (this.bufferStream == null) {
                this.bufferStream = new ByteArrayServletOutputStream();
            }
            this.bufferWriter = new PrintWriter(
                    new OutputStreamWriter(this.bufferStream, 
                            getCharacterEncoding()), true);
            return this.bufferWriter;
        }
    }
}
