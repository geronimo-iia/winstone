package net.winstone.boot;

/**
 * Command enumerate control code for manage life cycle of server.
 * 
 * @author <a href="mailto:jguibert@intelligents-ia.com" >Jerome Guibert</a>
 */
public enum Command {

	SHUTDOWN((byte) '0'), RELOAD((byte) '4');

	Command(final byte code) {
		this.code = code;
	}

	/** byte code exchanded on network */
	private final byte code;

	public byte getCode() {
		return code;
	}
}
