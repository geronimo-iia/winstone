/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package net.winstone.core.listener;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
		// Get the incoming packet flag
		final byte headerBuffer[] = new byte[4];
		int headerBytesRead = 0;
		int thisReadCount = 0;
		while ((headerBytesRead < 4) && ((thisReadCount = in.read(headerBuffer, headerBytesRead, 4 - headerBytesRead)) >= 0)) {
			headerBytesRead += thisReadCount;
		}

		handler.setRequestStartTime();
		if (headerBytesRead != 4) {
			throw new WinstoneException("Invalid AJP header");
		} else if ((headerBuffer[0] != 0x12) || (headerBuffer[1] != 0x34)) {
			throw new WinstoneException("Invalid AJP header");
		}

		// Read in the whole packet
		packetLength = ((headerBuffer[2] & 0xFF) << 8) + (headerBuffer[3] & 0xFF);
		packetBytes = new byte[packetLength];
		int packetBytesRead = 0;
		while ((packetBytesRead < packetLength) && ((thisReadCount = in.read(packetBytes, packetBytesRead, packetLength - packetBytesRead)) >= 0)) {
			packetBytesRead += thisReadCount;
		}

		if (packetBytesRead < packetLength) {
			throw new WinstoneException("Short AJP packet");
		}
		// Ajp13Listener.packetDump(packetBytes, packetBytesRead);
	}

	public byte parsePacket(final String encoding) throws IOException {
		int position = 0;
		packetType = packetBytes[position++];

		if (packetType != SERVER_FORWARD_REQUEST) {
			throw new WinstoneException("Unknown AJP packet type - " + packetType);
		}

		// Check for terminator
		if (packetBytes[packetLength - 1] != (byte) 255) {
			throw new WinstoneException("Invalid AJP packet terminator");
		}

		method = decodeMethodType(packetBytes[position++]);
		Ajp13IncomingPacket.logger.debug("Method: {}", method);

		// Protocol
		final int protocolLength = readInteger(position, packetBytes, true);
		position += 2;
		protocol = (protocolLength > -1) ? readString(position, packetBytes, encoding, protocolLength) : null;
		position += protocolLength + 1;
		Ajp13IncomingPacket.logger.debug("Protocol: {}", protocol);

		// URI
		final int uriLength = readInteger(position, packetBytes, true);
		position += 2;
		uri = (uriLength > -1) ? readString(position, packetBytes, encoding, uriLength) : null;
		position += uriLength + 1;
		Ajp13IncomingPacket.logger.debug("URI: {}", uri);

		// Remote addr
		final int remoteAddrLength = readInteger(position, packetBytes, true);
		position += 2;
		remoteAddr = (remoteAddrLength > -1) ? readString(position, packetBytes, encoding, remoteAddrLength) : null;
		position += remoteAddrLength + 1;
		Ajp13IncomingPacket.logger.debug("Remote address: {}", remoteAddr);

		// Remote host
		final int remoteHostLength = readInteger(position, packetBytes, true);
		position += 2;
		remoteHost = (remoteHostLength > -1) ? readString(position, packetBytes, encoding, remoteHostLength) : null;
		position += remoteHostLength + 1;
		Ajp13IncomingPacket.logger.debug("RemoteHost: {}", remoteHost);

		// Server name
		final int serverNameLength = readInteger(position, packetBytes, true);
		position += 2;
		serverName = (serverNameLength > -1) ? readString(position, packetBytes, encoding, serverNameLength) : null;
		position += serverNameLength + 1;
		Ajp13IncomingPacket.logger.debug("Server name: {}", serverName);

		serverPort = readInteger(position, packetBytes, false);
		position += 2;
		Ajp13IncomingPacket.logger.debug("Server port: {}", "" + serverPort);

		isSSL = readBoolean(position++, packetBytes);
		Ajp13IncomingPacket.logger.debug("SSL: {}", "" + isSSL);

		// Read headers
		final int headerCount = readInteger(position, packetBytes, false);
		Ajp13IncomingPacket.logger.debug("Header Count: {}", "" + headerCount);
		position += 2;
		headers = new String[headerCount];
		for (int n = 0; n < headerCount; n++) {
			// Header name
			final int headerTypeOrLength = readInteger(position, packetBytes, false);
			position += 2;
			String headerName = null;
			if (packetBytes[position - 2] == (byte) 0xA0) {
				headerName = decodeHeaderType(headerTypeOrLength);
			} else {
				headerName = readString(position, packetBytes, encoding, headerTypeOrLength);
				position += headerTypeOrLength + 1;
			}

			// Header value
			final int headerValueLength = readInteger(position, packetBytes, true);
			position += 2;
			headers[n] = headerName + ": " + ((headerValueLength > -1) ? readString(position, packetBytes, encoding, headerValueLength) : "");
			position += headerValueLength + 1;
			Ajp13IncomingPacket.logger.debug("Header: {}", headers[n]);
		}

		// Attribute parsing
		attributes = new HashMap<String, String>();
		while (position < (packetLength - 2)) {
			final String attName = decodeAttributeType(packetBytes[position++]);
			final int attValueLength = readInteger(position, packetBytes, true);
			position += 2;
			final String attValue = (attValueLength > -1) ? readString(position, packetBytes, encoding, attValueLength) : null;
			position += attValueLength + 1;

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
	 * Read a single integer from the stream
	 */
	private int readInteger(final int position, final byte packet[], final boolean forStringLength) {
		if (forStringLength && (packet[position] == (byte) 0xFF) && (packet[position + 1] == (byte) 0xFF)) {
			return -1;
		} else {
			return ((packet[position] & 0xFF) << 8) + (packet[position + 1] & 0xFF);
		}
	}

	/**
	 * Read a single boolean from the stream
	 */
	private boolean readBoolean(final int position, final byte packet[]) {
		return (packet[position] == (byte) 1);
	}

	/**
	 * Read a single string from the stream
	 */
	private String readString(final int position, final byte packet[], final String encoding, final int length) throws UnsupportedEncodingException {
		// System.out.println("Reading string length: " + length +
		// " position=" + position + " packetLength=" + packet.length);
		return (length == 0) || (length > packet.length) ? "" : new String(packet, position, length, encoding);
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
		default:
			return null;
		}
	}
}
