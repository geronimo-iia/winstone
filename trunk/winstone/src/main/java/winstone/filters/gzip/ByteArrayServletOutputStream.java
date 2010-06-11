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
package winstone.filters.gzip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;

/**
 * Acts as a simple byte array based output stream for servlet responses.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ByteArrayServletOutputStream.java,v 1.1 2005/08/24 06:43:34 rickknowles Exp $
 */
public class ByteArrayServletOutputStream extends ServletOutputStream {
    
    private ByteArrayOutputStream bodyStream;
    
    public ByteArrayServletOutputStream() {
        this.bodyStream = new ByteArrayOutputStream();
    }
    
    public byte[] getContent() {
        return this.bodyStream.toByteArray();
    }
    
    public void write(int b) throws IOException {
        this.bodyStream.write(b);
    }

    public void flush() throws IOException {
        this.bodyStream.flush();
    }
}
