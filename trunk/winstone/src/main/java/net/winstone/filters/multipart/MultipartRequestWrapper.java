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
package net.winstone.filters.multipart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;


/**
 * This class is used to encapsulate the decoding of HTTP POST requests
 * using the "multipart/form-data" encoding type.
 * <br/>
 * <br/>
 * It uses Javamail for Mime libraries and the JavaBeans Activation
 * Framework (JAF), so make sure you have activation.jar and mail.jar
 * in the class path before using this class.
 * <br/>
 * <br/>
 * Note: The servlet input stream is empty after the contructor executes.
 * This prevents the use of this class on the same request twice.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: MultipartRequestWrapper.java,v 1.3 2005/09/08 02:42:02 rickknowles Exp $
 */
public class MultipartRequestWrapper extends HttpServletRequestWrapper {
    public final static String MPH_ATTRIBUTE = "MultipartRequestWrapper.reference";

    private Map<String, String[]> stringParameters;
    private Map<String, File> tempFileNames;
    private Map<String, String> mimeTypes;
    private Map<String, String> uploadFileNames;

    /**
     * Constructor - this uses a servlet request, validating it to make
     * sure it's a multipart/form-data request, then reads the
     * ServletInputStream, storing the results after Mime decoding in
     * a member array. Use getParameter etc to retrieve the contents.
     * @param request The Servlet's request object.
     */
    public MultipartRequestWrapper(ServletRequest request)
                     throws IOException {
        super((HttpServletRequest) request);
        String contentType = request.getContentType();

        if (!contentType.toLowerCase().startsWith("multipart/form-data")) {
            throw new IOException("The MIME Content-Type of the Request must be " + 
                    '"' + "multipart/form-data" + '"' + ", not " + '"' + contentType + '"' + ".");
        }
        // If we find a request attribute with an mph already present, copy from that
        else if (request.getAttribute(MPH_ATTRIBUTE) != null) {
            MultipartRequestWrapper oldMPH = (MultipartRequestWrapper) 
                    request.getAttribute(MPH_ATTRIBUTE);

            this.stringParameters = oldMPH.stringParameters;
            this.mimeTypes = oldMPH.mimeTypes;
            this.tempFileNames = oldMPH.tempFileNames;
            this.uploadFileNames = oldMPH.uploadFileNames;

            return;
        }

        try {
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            InputStream inputServlet = request.getInputStream();
            byte buffer[] = new byte[2048];
            int readBytes = inputServlet.read(buffer);
            while (readBytes != -1) {
                byteArray.write(buffer, 0, readBytes);
                readBytes = inputServlet.read(buffer);
            }
            inputServlet.close();
            MimeMultipart parts = new MimeMultipart(
                    new MultipartRequestWrapperDataSource(contentType, 
                            byteArray.toByteArray()));
            byteArray.close();

            Map<String, String[]> stringParameters = new HashMap<String, String[]>();
            Map<String, String> mimeTypes = new HashMap<String, String>();
            Map<String, File> tempFileNames = new HashMap<String, File>();
            Map<String, String> uploadFileNames = new HashMap<String, String>();
            String encoding = request.getCharacterEncoding();
            if (encoding == null) {
                encoding = "8859_1";
            }

            for (int loopCount = 0; loopCount < parts.getCount(); loopCount++) {
                MimeBodyPart current = (MimeBodyPart) parts.getBodyPart(loopCount);
                String headers = current.getHeader("Content-Disposition", "; ");

                // Get the name field
                if (headers.indexOf(" name=" + '"') == -1) {
                    throw new MessagingException("No name header found in " +
                            "Content-Disposition field.");
                } else {
                    // Get the name field
                    String namePart = headers.substring(headers.indexOf(" name=\"") + 7);
                    namePart = namePart.substring(0, namePart.indexOf('"'));
                    String nameField = javax.mail.internet.MimeUtility.decodeText(namePart);

                    InputStream inRaw = current.getInputStream();
                    
                    if (headers.indexOf(" filename=" + '"') != -1) {
                        String fileName = headers.substring(headers.indexOf(" filename=" + '"') + 11);
                        fileName = fileName.substring(0, fileName.indexOf('"'));
                        if (fileName.lastIndexOf('/') != -1) {
                            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                        }
                        if (fileName.lastIndexOf('\\') != -1) {
                            fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);
                        }
                        uploadFileNames.put(nameField, fileName);
                        
                        if (tempFileNames.containsKey(nameField)) {
                            throw new IOException("Multiple parameters named " + nameField);
                        }
                    
                        if (current.getContentType() == null) {
                            mimeTypes.put(nameField, "text/plain");
                        } else {
                            mimeTypes.put(nameField, current.getContentType());
                        }
                        
                        // write out a file in temp space and store it
                        File tempFile = File.createTempFile("mph", ".tmp");
                        OutputStream outStream = new FileOutputStream(tempFile, true);
                        while ((readBytes = inRaw.read(buffer)) != -1) {
                            outStream.write(buffer, 0, readBytes);
                        }
                        inRaw.close();
                        outStream.close();
                        tempFileNames.put(nameField, tempFile.getAbsoluteFile());
                    } else {
                        byte[] stash = new byte[inRaw.available()];
                        inRaw.read(stash);
                        inRaw.close();

                        Object oldParam = stringParameters.get(nameField);
                        if (oldParam == null) {
                            stringParameters.put(nameField, new String[] {
                                    new String(stash, encoding)
                            });
                        } else {
                            String oldParams[] = (String []) oldParam;
                            String newParams[] = new String[oldParams.length + 1];
                            System.arraycopy(oldParams, 0, newParams, 0, oldParams.length);
                            newParams[oldParams.length] = new String(stash, encoding);
                            stringParameters.put(nameField, newParams);
                        }
                    }
                }
            }

            parts = null;
            this.stringParameters = Collections.unmodifiableMap(stringParameters);
            this.mimeTypes = Collections.unmodifiableMap(mimeTypes);
            this.tempFileNames = Collections.unmodifiableMap(tempFileNames);
            this.uploadFileNames = Collections.unmodifiableMap(uploadFileNames);

            // Set this handler into the request attribute set
            request.setAttribute(MPH_ATTRIBUTE, this);
        } catch (MessagingException errMime) {
            throw new IOException(errMime.toString());
        }
    }

    public String getParameter(String name) {
        String parameterValues[] =  getParameterValues(name);
        if ((parameterValues == null) || (parameterValues.length == 0)) {
            return null;
        } else {
            return parameterValues[0];
        }
    }

    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> paramMap = new HashMap<String, String[]>();
        for (Enumeration<String> names = this.getParameterNames(); names.hasMoreElements();) {
            String name = (String) names.nextElement();
            paramMap.put(name, getParameterValues(name));
        }
        return Collections.unmodifiableMap(paramMap);
    }

    @SuppressWarnings("unchecked")
    public Enumeration<String> getParameterNames() {
        Set<String> names = new HashSet<String>(this.stringParameters.keySet());
        names.addAll(this.tempFileNames.keySet());
        Enumeration<String> parent = super.getParameterNames();
        names.addAll(Collections.list(parent));
        return Collections.enumeration(names);
    }

    public ServletInputStream getInputStream() throws IOException {
        throw new IOException("InputStream already parsed by the MultipartRequestWrapper class");
    }

    public String[] getParameterValues(String name) {
        // Try parent first - since after a forward we want that to have precendence
        String parentValue[] = super.getParameterValues(name);
        if (parentValue != null) {
            return parentValue;
        } else if (name == null) {
            return null;
        } else if (name.endsWith(".filename") && isFileUploadParameter(
                name.substring(0, name.length() - 9))) {
            return new String[] {getUploadFileName(name.substring(0, name.length() - 9))};
        } else if (name.endsWith(".content-type") && isFileUploadParameter(
                name.substring(0, name.length() - 13))) {
            return new String[] {getContentType(name.substring(0, name.length() - 13))};
        } else if (isNonFileUploadParameter(name)) {
            return (String []) this.stringParameters.get(name);
        } else if (this.isFileUploadParameter(name)) {
            return new String[] {((File) this.tempFileNames.get(name)).getAbsolutePath()};
        } else {
            return null;
        }
    }

    /**
     * The byte array version of the parameter requested (as an Object).
     * This always returns a byte array, ignoring the mime type of the
     * submitted object.
     * @param name The parameter you wish to retrieve.
     * @return A byte array representation of the supplied parameter.
     */
    public byte[] getRawParameter(String name) throws IOException {
        if (name == null) {
            return null;
        }
        File tempFile = (File) this.tempFileNames.get(name);
        if (tempFile == null) {
            return null;
        }
        InputStream inFile = new FileInputStream(tempFile);
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        byte buffer[] = new byte[2048];
        int readBytes = inFile.read(buffer);
        while (readBytes != -1) {
            byteArray.write(buffer, 0, readBytes);
            readBytes = inFile.read(buffer);
        }
        inFile.close();
        byte output[] = byteArray.toByteArray();
        byteArray.close();
        return output;
    }

    /**
     * Get the MimeType of a particular parameter.
     * @param name The parameter you wish to find the Mime type of.
     * @return The Mime type for the requested parameter (as specified
     * in the Mime header during the post).
     */
    public String getContentType(String name) {
        return (String) this.mimeTypes.get(name);
    }

    /**
     * The local (client) name of the file submitted if this parameter was
     * a file.
     * @param name The parameter you wish to find the file name for.
     * @return The local name for the requested parameter (as specified
     * in the Mime header during the post).
     */
    public String getUploadFileName(String name) {
        return (String) this.uploadFileNames.get(name);
    }

    public boolean isFileUploadParameter(String name) {
        return this.tempFileNames.containsKey(name);
    }

    public boolean isNonFileUploadParameter(String name) {
        return this.stringParameters.containsKey(name);
    }
    
    /**
     * Retrieve a Map of the raw bytes of the parameters supplied in the HTTP POST request.
     */
    public Map<String, byte[]> getRawParameterMap() throws IOException {
        Map<String, byte[]> output = new HashMap<String, byte[]>();
        for (Iterator<String> i = this.uploadFileNames.keySet().iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            output.put(key, getRawParameter(key));
        }
        return output;
    }

    /**
     * Retrieve a Map of the filenames supplied in the HTTP POST request.
     */
    public Map<String, String> getContentTypeMap() {
        return this.mimeTypes;
    }

    /**
     * Retrieve a Map of the filenames supplied in the HTTP POST request.
     */
    public Map<String, String> getUploadFileNameMap() {
        return this.uploadFileNames;
    }
    
    private class MultipartRequestWrapperDataSource implements DataSource {

        private byte mimeByteArray[];
        private String contentType;
        
        private MultipartRequestWrapperDataSource(String contentType, byte mimeByteArray[]) {
            this.mimeByteArray = mimeByteArray;
            this.contentType = contentType;
        }
        
        /**
         * Required for implementation of the DataSource interface.
         * Internal use only.
         */
        public String getName() {
            return "MultipartHandler";
        }

        /**
         * Required for implementation of the DataSource interface.
         * Internal use only.
         */
        public String getContentType() {
            return contentType;
        }

        /**
         * Required for implementation of the DataSource interface.
         * Internal use only.
         */
        public java.io.InputStream getInputStream() throws java.io.IOException {
            return new ByteArrayInputStream(this.mimeByteArray);
        }

        /**
         * Required for implementation of the DataSource interface.
         * Internal use only.
         */
        public java.io.OutputStream getOutputStream() throws java.io.IOException {
            throw new IOException("This is a read-only datasource.");
        }
    }
}
