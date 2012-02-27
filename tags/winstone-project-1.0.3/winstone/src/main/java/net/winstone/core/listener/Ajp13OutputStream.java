/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core.listener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;

import net.winstone.WinstoneException;
import net.winstone.core.WinstoneOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends the winstone output stream, so that the ajp13 protocol requirements
 * can be fulfilled.
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Ajp13OutputStream.java,v 1.7 2007/05/05 00:52:50 rickknowles
 *          Exp $
 */
public class Ajp13OutputStream extends WinstoneOutputStream {
	// Container originated packet types

	protected static Logger logger = LoggerFactory.getLogger(Ajp13OutputStream.class);

	byte CONTAINER_SEND_BODY_CHUNK = 0x03;
	byte CONTAINER_SEND_HEADERS = 0x04;
	byte CONTAINER_END_RESPONSE = 0x05;
	// byte CONTAINER_GET_BODY_CHUNK = 0x06;
	// byte CONTAINER_CPONG_REPLY = 0x09;
	final static Map<String, byte[]> headerCodes;

	static {
		headerCodes = new HashMap<String, byte[]>();
		Ajp13OutputStream.headerCodes.put("content-type", new byte[] { (byte) 0xA0, 0x01 });
		Ajp13OutputStream.headerCodes.put("content-language", new byte[] { (byte) 0xA0, 0x02 });
		Ajp13OutputStream.headerCodes.put("content-length", new byte[] { (byte) 0xA0, 0x03 });
		Ajp13OutputStream.headerCodes.put("date", new byte[] { (byte) 0xA0, 0x04 });
		Ajp13OutputStream.headerCodes.put("last-modified", new byte[] { (byte) 0xA0, 0x05 });
		Ajp13OutputStream.headerCodes.put("location", new byte[] { (byte) 0xA0, 0x06 });
		Ajp13OutputStream.headerCodes.put("set-cookie", new byte[] { (byte) 0xA0, 0x07 });
		Ajp13OutputStream.headerCodes.put("set-cookie2", new byte[] { (byte) 0xA0, 0x08 });
		Ajp13OutputStream.headerCodes.put("servlet-engine", new byte[] { (byte) 0xA0, 0x09 });
		Ajp13OutputStream.headerCodes.put("server", new byte[] { (byte) 0xA0, 0x09 });
		Ajp13OutputStream.headerCodes.put("status", new byte[] { (byte) 0xA0, 0x0A });
		Ajp13OutputStream.headerCodes.put("www-authenticate", new byte[] { (byte) 0xA0, 0x0B });
	}
	private final String headerEncoding;

	/**
	 * Build a new instance of Ajp13OutputStream.
	 * 
	 * @param outStream
	 * @param headerEncoding
	 */
	public Ajp13OutputStream(final OutputStream outStream, final String headerEncoding) {
		super(outStream, Boolean.FALSE);
		this.headerEncoding = headerEncoding;
	}

	@Override
	public void commit() throws IOException {
		Ajp13OutputStream.logger.trace("Written {} bytes to response body", "" + bytesCommitted);
		buffer.flush();
		// If we haven't written the headers yet, write them out
		if (!committed) {
			owner.validateHeaders();
			committed = Boolean.TRUE;
			final ByteArrayOutputStream headerArrayStream = new ByteArrayOutputStream();

			for (final String header : owner.getHeaders()) {
				final int colonPos = header.indexOf(':');
				if (colonPos == -1) {
					throw new WinstoneException("No colon header: " + header);
				}
				final String headerName = header.substring(0, colonPos).trim();
				final String headerValue = header.substring(colonPos + 1).trim();
				final byte headerCode[] = Ajp13OutputStream.headerCodes.get(headerName.toLowerCase());
				if (headerCode == null) {
					headerArrayStream.write(getStringBlock(headerName));
				} else {
					headerArrayStream.write(headerCode);
				}
				headerArrayStream.write(getStringBlock(headerValue));
			}

			for (final Cookie cookie : owner.getCookies()) {
				final String cookieText = owner.writeCookie(cookie);
				final int colonPos = cookieText.indexOf(':');
				if (colonPos == -1) {
					throw new WinstoneException("No colon header: " + cookieText);
				}
				final String headerName = cookieText.substring(0, colonPos).trim();
				final String headerValue = cookieText.substring(colonPos + 1).trim();
				final byte headerCode[] = Ajp13OutputStream.headerCodes.get(headerName.toLowerCase());
				if (headerCode == null) {
					headerArrayStream.write(getStringBlock(headerName));
				} else {
					headerArrayStream.write(headerCode);
				}
				headerArrayStream.write(getStringBlock(headerValue));
			}

			// Write packet header + prefix + status code + status msg + header
			// count
			final byte headerArray[] = headerArrayStream.toByteArray();
			final byte headerPacket[] = new byte[12];
			headerPacket[0] = (byte) 0x41;
			headerPacket[1] = (byte) 0x42;
			Ajp13OutputStream.setIntBlock(headerArray.length + 8, headerPacket, 2);
			headerPacket[4] = CONTAINER_SEND_HEADERS;
			Ajp13OutputStream.setIntBlock(owner.getStatus(), headerPacket, 5);
			Ajp13OutputStream.setIntBlock(0, headerPacket, 7); // empty msg
			headerPacket[9] = (byte) 0x00;
			Ajp13OutputStream.setIntBlock(owner.getHeaders().size() + owner.getCookies().size(), headerPacket, 10);

			// Ajp13Listener.packetDump(headerPacket, headerPacket.length);
			// Ajp13Listener.packetDump(headerArray, headerArray.length);

			outStream.write(headerPacket);
			outStream.write(headerArray);
		}

		// Write out the contents of the buffer in max 8k chunks
		final byte bufferContents[] = buffer.toByteArray();
		int position = 0;
		while (position < bufferContents.length) {
			final int packetLength = Math.min(bufferContents.length - position, 8184);
			final byte responsePacket[] = new byte[packetLength + 8];
			responsePacket[0] = 0x41;
			responsePacket[1] = 0x42;
			Ajp13OutputStream.setIntBlock(packetLength + 4, responsePacket, 2);
			responsePacket[4] = CONTAINER_SEND_BODY_CHUNK;
			Ajp13OutputStream.setIntBlock(packetLength, responsePacket, 5);
			System.arraycopy(bufferContents, position, responsePacket, 7, packetLength);
			responsePacket[packetLength + 7] = 0x00;
			position += packetLength;

			// Ajp13Listener.packetDump(responsePacket, responsePacket.length);
			outStream.write(responsePacket);
		}

		buffer.reset();
		bufferPosition = 0;
	}

	@Override
	public void finishResponse() throws IOException {
		// Send end response packet
		final byte endResponse[] = new byte[] { 0x41, 0x42, 0x00, 0x02, CONTAINER_END_RESPONSE, 1 };
		// Ajp13Listener.packetDump(endResponse, endResponse.length);
		outStream.write(endResponse);
	}

	/**
	 * Useful generic method for getting ajp13 format integers in a packet.
	 */
	public byte[] getIntBlock(final int integer) {
		final byte hi = (byte) (0xFF & (integer >> 8));
		final byte lo = (byte) (0xFF & (integer - (hi << 8)));
		return new byte[] { hi, lo };
	}

	/**
	 * Useful generic method for setting ajp13 format integers in a packet.
	 */
	public static void setIntBlock(final int integer, final byte packet[], final int offset) {
		final byte hi = (byte) (0xFF & (integer >> 8));
		final byte lo = (byte) (0xFF & (integer - (hi << 8)));
		packet[offset] = hi;
		packet[offset + 1] = lo;
	}

	/**
	 * Useful generic method for getting ajp13 format strings in a packet.
	 */
	public byte[] getStringBlock(final String text) throws UnsupportedEncodingException {
		final byte textBytes[] = text.getBytes(headerEncoding);
		final byte outArray[] = new byte[textBytes.length + 3];
		System.arraycopy(getIntBlock(textBytes.length), 0, outArray, 0, 2);
		System.arraycopy(textBytes, 0, outArray, 2, textBytes.length);
		outArray[textBytes.length + 2] = 0x00;
		return outArray;
	}
}
