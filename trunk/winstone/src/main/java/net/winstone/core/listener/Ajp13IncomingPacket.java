/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core.listener;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import net.winstone.WinstoneException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Models a single incoming ajp13 packet. Fixes by Cory Osborn 2007/4/3 - IIS
 * related. Thanks
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Ajp13IncomingPacket.java,v 1.7 2008/02/17 07:44:01 rickknowles
 *          Exp $
 */
public class Ajp13IncomingPacket {
	// Server originated packet types

	private static Logger logger = LoggerFactory.getLogger(Ajp13IncomingPacket.class);
	byte SERVER_FORWARD_REQUEST = 0x02;
	// public static byte SERVER_SHUTDOWN = 0x07; //not implemented
	// public static byte SERVER_PING = 0x08; //not implemented
	// public static byte SERVER_CPING = 0x10; //not implemented
	private final int packetLength;
	private final byte packetBytes[];
	private byte packetType;
	private String method;
	private String protocol;
	private String uri;
	private String remoteAddr;
	private String remoteHost;
	private String serverName;
	private int serverPort;
	private boolean isSSL;
	private String headers[];
	private Map<String, String> attributes;

	/**
	 * Constructor
	 */
	public Ajp13IncomingPacket(final InputStream in, final RequestHandlerThread handler) throws IOException {
		DataInputStream din = new DataInputStream(in);
		// Get the incoming packet flag
		final byte headerBuffer[] = new byte[4];
		din.readFully(headerBuffer);
		handler.setRequestStartTime();
		if ((headerBuffer[0] != 0x12) || (headerBuffer[1] != 0x34)) {
			throw new WinstoneException("Invalid AJP header");
		}

		// Read in the whole packet
		packetLength = ((headerBuffer[2] & 0xFF) << 8) + (headerBuffer[3] & 0xFF);
		packetBytes = new byte[packetLength];
		din.readFully(packetBytes);
		// Ajp13Listener.packetDump(packetBytes, packetBytesRead);
	}

	/**
	 * Pares packet with specified encoding.
	 * 
	 * @param encoding
	 * @return packet type
	 * @throws IOException
	 */
	public byte parsePacket(final String encoding) throws IOException {
		final DataInputStream di = new DataInputStream(new ByteArrayInputStream(packetBytes));
		packetType = di.readByte();

		if (packetType != SERVER_FORWARD_REQUEST) {
			throw new WinstoneException("Unknown AJP packet type - " + packetType);
		}
		// Check for terminator
		if (packetBytes[packetLength - 1] != (byte) 255) {
			throw new WinstoneException("Invalid AJP packet terminator");
		}

		method = decodeMethodType(di.readByte());
		Ajp13IncomingPacket.logger.debug("Method: {}", method);

		// Protocol
		protocol = readString(di, encoding);
		Ajp13IncomingPacket.logger.debug("Protocol: {}", protocol);

		// URI
		uri = readString(di, encoding);
		Ajp13IncomingPacket.logger.debug("URI: {}", uri);

		// Remote addr
		remoteAddr = readString(di, encoding);
		Ajp13IncomingPacket.logger.debug("Remote address: {}", remoteAddr);

		// Remote host
		remoteHost = readString(di, encoding);
		Ajp13IncomingPacket.logger.debug("RemoteHost: {}", remoteHost);

		// Server name
		serverName = readString(di, encoding);
		Ajp13IncomingPacket.logger.debug("Server name: {}", serverName);

		serverPort = di.readShort();
		Ajp13IncomingPacket.logger.debug("Server port: {}", "" + serverPort);

		isSSL = di.readBoolean();
		Ajp13IncomingPacket.logger.debug("SSL: {}", "" + isSSL);

		// Read headers
		final int headerCount = di.readShort();
		Ajp13IncomingPacket.logger.debug("Header Count: {}", "" + headerCount);

		headers = new String[headerCount];
		for (int n = 0; n < headerCount; n++) {
			// Header name
			final int headerTypeOrLength = di.readShort();
			String headerName;
			if ((headerTypeOrLength & 0xFF00) == 0xA000) {
				headerName = decodeHeaderType(headerTypeOrLength & 0xFFFF);
			} else {
				headerName = readString(di, encoding, headerTypeOrLength);
			}
			// Header value
			headers[n] = headerName + ": " + readString(di, encoding);
			Ajp13IncomingPacket.logger.debug("Header: {}", headers[n]);
		}

		// Attribute parsing
		attributes = new HashMap<String, String>();
		while (true) {
			final byte type = di.readByte();
			if (type == -1) {
				break; // end of attributes
			}
			String attName = decodeAttributeType(type);
			if (type == 0x0A) {
				attName = readString(di, encoding);
			}
			final String attValue = readString(di, encoding);
			attributes.put(attName, attValue);
			Ajp13IncomingPacket.logger.debug("Attribute: {}={}", attName, attValue);
		}
		Ajp13IncomingPacket.logger.debug("Successfully read AJP13 packet - length={}", "" + packetLength);
		return packetType;
	}

	public int getPacketLength() {
		return packetLength;
	}

	public String getMethod() {
		return method;
	}

	public String getProtocol() {
		return protocol;
	}

	public String getURI() {
		return uri;
	}

	public String getRemoteAddress() {
		return remoteAddr;
	}

	public String getRemoteHost() {
		return remoteHost;
	}

	public String getServerName() {
		return serverName;
	}

	public int getServerPort() {
		return serverPort;
	}

	public boolean isSSL() {
		return isSSL;
	}

	public String[] getHeaders() {
		return headers;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	/**
	 * Read a single string from the stream
	 */
	private String readString(final DataInput di, final String encoding) throws IOException {
		// System.out.println("Reading string length: " + length +
		// " position=" + position + " packetLength=" + packet.length);
		return readString(di, encoding, di.readShort());
	}

	private String readString(final DataInput di, final String encoding, final int length) throws IOException {
		// System.out.println("Reading string length: " + length +
		// " position=" + position + " packetLength=" + packet.length);
		if (length == -1) {
			return null;
		}
		if (length == 0) {
			di.readByte(); // skip over the null terminator
			return "";
		}

		final byte[] buf = new byte[length];
		di.readFully(buf);
		di.readByte(); // skip over the null terminator
		return new String(buf, encoding);
	}

	/**
	 * Decodes the method types into Winstone HTTP method strings
	 */
	private String decodeMethodType(final byte methodType) {
		switch (methodType) {
		case 1:
			return "OPTIONS";
		case 2:
			return "GET";
		case 3:
			return "HEAD";
		case 4:
			return "POST";
		case 5:
			return "PUT";
		case 6:
			return "DELETE";
		case 7:
			return "TRACE";
		case 8:
			return "PROPFIND";
		case 9:
			return "PROPPATCH";
		case 10:
			return "MKCOL";
		case 11:
			return "COPY";
		case 12:
			return "MOVE";
		case 13:
			return "LOCK";
		case 14:
			return "UNLOCK";
		case 15:
			return "ACL";
		case 16:
			return "REPORT";
		case 17:
			return "VERSION-CONTROL";
		case 18:
			return "CHECKIN";
		case 19:
			return "CHECKOUT";
		case 20:
			return "UNCHECKOUT";
		case 21:
			return "SEARCH";
		case 22:
			return "MKWORKSPACE";
		case 23:
			return "UPDATE";
		case 24:
			return "LABEL";
		case 25:
			return "MERGE";
		case 26:
			return "BASELINE_CONTROL";
		case 27:
			return "MKACTIVITY";
		default:
			return "UNKNOWN";
		}
	}

	/**
	 * Decodes the header types into Winstone HTTP header strings
	 */
	private String decodeHeaderType(final int headerType) {
		switch (headerType) {
		case 0xA001:
			return "Accept";
		case 0xA002:
			return "Accept-Charset";
		case 0xA003:
			return "Accept-Encoding";
		case 0xA004:
			return "Accept-Language";
		case 0xA005:
			return "Authorization";
		case 0xA006:
			return "Connection";
		case 0xA007:
			return "Content-Type";
		case 0xA008:
			return "Content-Length";
		case 0xA009:
			return "Cookie";
		case 0xA00A:
			return "Cookie2";
		case 0xA00B:
			return "Host";
		case 0xA00C:
			return "Pragma";
		case 0xA00D:
			return "Referer";
		case 0xA00E:
			return "User-Agent";
		default:
			return null;
		}
	}

	/**
	 * Decodes the header types into Winstone HTTP header strings
	 */
	private String decodeAttributeType(final byte attributeType) {
		switch (attributeType) {
		case 0x01:
			return "context";
		case 0x02:
			return "servlet_path";
		case 0x03:
			return "remote_user";
		case 0x04:
			return "auth_type";
		case 0x05:
			return "query_string";
		case 0x06:
			return "jvm_route";
		case 0x07:
			return "ssl_cert";
		case 0x08:
			return "ssl_cipher";
		case 0x09:
			return "ssl_session";
		case 0x0A:
			return "req_attribute";
		case 0x0B:
			return "ssl_key_size";
		case 0x0C:
			return "secret";
		case 0x0D:
			return "stored_method";
		default:
			return null;
		}
	}
}
