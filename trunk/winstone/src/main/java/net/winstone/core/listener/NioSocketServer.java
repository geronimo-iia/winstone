package net.winstone.core.listener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: NioSocketServer.java,v 1.1 2006/08/27 14:22:32 rickknowles Exp
 *          $
 */
public class NioSocketServer implements Runnable {

	private final static int LISTEN_PORT = 6475;

	private Thread thread;
	private Selector selector;

	private ServerSocket serverSocket;

	public NioSocketServer(final boolean useNIO) throws IOException {
		if (useNIO) {
			final ServerSocketChannel ssc = ServerSocketChannel.open();
			ssc.configureBlocking(Boolean.FALSE);
			final ServerSocket ss = ssc.socket();
			ss.bind(new InetSocketAddress(NioSocketServer.LISTEN_PORT));

			selector = Selector.open();
			ssc.register(selector, SelectionKey.OP_ACCEPT);
		} else {
			serverSocket = new ServerSocket(NioSocketServer.LISTEN_PORT);
			serverSocket.setSoTimeout(500);
		}

		thread = new Thread(this);
		thread.setDaemon(Boolean.TRUE);
		thread.start();
	}

	@Override
	public void run() {
		boolean interrupted = Boolean.FALSE;
		while (!interrupted) {
			try {
				if (selector != null) {
					nioLoop();
				} else {
					jioLoop();
				}
				interrupted = Thread.interrupted();
			} catch (final IOException err) {
				interrupted = Boolean.TRUE;
			}
		}
		thread = null;
	}

	private void nioLoop() throws IOException {
		selector.select(500);
		final Set<SelectionKey> selectedKeys = selector.selectedKeys();
		final Iterator<SelectionKey> i = selectedKeys.iterator();
		while (i.hasNext()) {
			final SelectionKey key = i.next();
			if (key.isAcceptable()) {
				final ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
				final SocketChannel sc = ssc.accept();
				sc.configureBlocking(Boolean.FALSE);
				sc.register(selector, SelectionKey.OP_READ);
			} else if (key.isReadable()) {
				final SocketChannel sc = (SocketChannel) key.channel();
				final ByteBuffer buffer = ByteBuffer.allocate(10);
				buffer.clear();
				sc.read(buffer);
				buffer.flip();
				sc.write(buffer);
				sc.close();
			}
			i.remove();
		}
	}

	private void jioLoop() throws IOException {
		Socket socket = null;
		try {
			socket = serverSocket.accept();
		} catch (final SocketTimeoutException err) {
		}
		if (socket != null) {
			final InputStream in = socket.getInputStream();
			int pos = 0;
			int read = 0;
			final byte buffer[] = new byte[10];
			while ((pos < buffer.length) && ((read = in.read(buffer, pos, buffer.length - pos)) != -1)) {
				pos += read;
			}
			final OutputStream out = socket.getOutputStream();
			out.write(buffer, 0, pos);
			in.close();
			out.close();
			socket.close();
		}
	}

	public void stop() {
		thread.interrupt();
	}

	public static void main(final String argv[]) throws Exception {

		final String iterArg = argv.length > 1 ? argv[1] : "1000";
		final int ITERATION_COUNT = Integer.parseInt(iterArg);
		final boolean useNIO = (argv.length > 0) && argv[0].equals("nio");

		final InetAddress LOCATION = InetAddress.getLocalHost();
		System.out.println("Address: " + LOCATION);

		final NioSocketServer server = new NioSocketServer(useNIO);
		Thread.sleep(1000);

		final long startTime = System.currentTimeMillis();

		final byte TEST_ARRAY[] = "1234567890".getBytes();
		for (int n = 0; n < ITERATION_COUNT; n++) {
			final byte buffer[] = new byte[TEST_ARRAY.length];
			final Socket socket = new Socket(LOCATION, NioSocketServer.LISTEN_PORT);
			socket.setSoTimeout(50);
			final OutputStream out = socket.getOutputStream();
			out.write(TEST_ARRAY);

			final InputStream in = socket.getInputStream();
			int read = 0;
			int pos = 0;
			while ((pos < buffer.length) && ((read = in.read(buffer, pos, buffer.length - pos)) != -1)) {
				pos += read;
			}
			in.close();
			out.close();
			socket.close();
			// if (!Arrays.equals(TEST_ARRAY, buffer)) {
			// throw new RuntimeException("in and out arrays are not equal");
			// }
			if ((n % 500) == 0) {
				System.out.println("Completed " + n + " iterations in " + (System.currentTimeMillis() - startTime) + "ms");
			}
		}
		System.out.println("Completed " + ITERATION_COUNT + " iterations in " + (System.currentTimeMillis() - startTime) + "ms");
		server.stop();
	}
}
