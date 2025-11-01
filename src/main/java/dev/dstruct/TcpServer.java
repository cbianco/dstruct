package dev.dstruct;

import dev.dstruct.Result.EmptyResult;
import dev.dstruct.Result.Error;
import dev.dstruct.Result.Ok;
import dev.dstruct.Result.Results;
import dev.dstruct.command.Command;
import dev.dstruct.command.Command.Batch;
import dev.dstruct.logging.Log;
import dev.dstruct.logging.LogFactory;
import dev.dstruct.parser.ParseException;
import dev.dstruct.parser.Parser;
import dev.dstruct.parser.Scanner;
import dev.dstruct.parser.Token;
import dev.dstruct.util.Process;
import dev.dstruct.util.ReqRes;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Tcp server that manages request and response as simple text.
 * It is a single thread server, it manages connections by ServerSocketChannel.
 * ServerSocketChannel under the hood is a gateway between java an os file descriptor.
 *
 */
public class TcpServer implements Process {

	private static final Log log = LogFactory.create(TcpServer.class);

	private final int port;
	private Selector selector;
	private ServerSocketChannel serverChannel;
	private final ReqRes reqRes;
	private final ExecutorService tcpServerExecutor = Executors.newSingleThreadExecutor(
		Thread
			.ofPlatform()
			.name("dstruct-tcp-server")
			.factory()
	);

	private final LinkedBlockingDeque<PendingResponse> pendingResponses = new LinkedBlockingDeque<>();

	public TcpServer(int port, ReqRes reqRes) {
		if (port < 1024 || port > 65535) {
			throw new IllegalArgumentException("port is out of range (1024-65535)");
		}
		this.port = port;
		this.reqRes = reqRes;
	}

	private void enqueueResponse(SelectionKey key, Result result) {
		if (!key.isValid()) {
			return;
		}

		SocketChannel channel = (SocketChannel) key.channel();
		if (!channel.isOpen() || !channel.isConnected()) {
			return;
		}
		pendingResponses.offer(new PendingResponse(key, result));
		selector.wakeup();
	}

	private void processPendingResponses() {
		PendingResponse response;
		while ((response = pendingResponses.poll()) != null) {
			SelectionKey key = response.key;

			if (key.isValid()) {
				SocketChannel channel = (SocketChannel) key.channel();
				if (channel.isOpen() && channel.isConnected()) {
					key.attach(response.result);
					key.interestOps(SelectionKey.OP_WRITE);
				}
			}
		}
	}

	public void start() {
		tcpServerExecutor.execute(this::tcpLoop);
	}

	private void tcpLoop() {
		try {
			this.selector = Selector.open();
			this.serverChannel = ServerSocketChannel.open();
			this.serverChannel.bind(new InetSocketAddress(port));
			this.serverChannel.configureBlocking(false);
			this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);

			log.info("Server started at port: " + port);

			while (serverChannel.isOpen() && selector.isOpen()) {
				processPendingResponses();
				if (selector.select() == 0) continue;
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> iterator = selectedKeys.iterator();

				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					iterator.remove();

					if (!key.isValid()) {
						continue;
					}

					if (key.isAcceptable()) {
						handleAccept(key);
					} else if (key.isReadable()) {
						handleRead(key);
					} else if (key.isWritable()) {
						handleWrite(key);
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			cleanup();
		}
	}

	private void handleAccept(SelectionKey key) throws IOException {
		ServerSocketChannel serverChannel = (ServerSocketChannel)key.channel();
		SocketChannel clientChannel = serverChannel.accept();

		if (clientChannel != null) {
			clientChannel.configureBlocking(false);
			clientChannel.register(selector, SelectionKey.OP_READ);
		}
	}

	private void handleWrite(SelectionKey key) {
		SocketChannel clientChannel = (SocketChannel) key.channel();

		try {

			if (!key.isValid() || !clientChannel.isOpen() || !clientChannel.isConnected()) {
				closeChannel(key);
				return;
			}

			Result result = (Result)key.attachment();
			ByteBuffer[] response = toResponseBuffer(result);
			clientChannel.write(response);
			key.attach(null);
			key.interestOps(SelectionKey.OP_READ);
		} catch (IOException e) {
			e.printStackTrace();
			closeChannel(key);
		}
	}

	private void handleRead(SelectionKey key) {
		try {
			SocketChannel channel = (SocketChannel)key.channel();
			if (!channel.isOpen() || !channel.isConnected()) {
				closeChannel(key);
				return;
			}
			Command command = readCommand(channel);
			if (command != null) {
				key.interestOps(0);
				reqRes
					.apply(command)
					.orTimeout(30, TimeUnit.SECONDS)
					.whenComplete((result, throwable) -> {
						if (throwable != null) {
							enqueueResponse(key, new Error(throwable.getMessage()));
						}
						else {
							enqueueResponse(key, result);
						}
					});
			}
			else {
				closeChannel(key);
			}
		}
		catch (ParseException parseException) {
			enqueueResponse(key, new Error(parseException.getMessage()));
		}
	}

	private Command readCommand(SocketChannel client) {
		Scanner scanner = new Scanner(client);
		scanner.parse();
		List<Token> tokens = scanner.getTokens();
		Parser parser = new Parser(tokens);
		parser.parse();
		List<Command> commands = parser.getCommands();
		if (commands.size() > 1) {
			return new Batch(commands);
		} else if (commands.size() == 1) {
			return commands.getFirst();
		} else {
			return null;
		}
	}

	private ByteBuffer[] toResponseBuffer(Result result) {
		ByteBuffer[] arr = new ByteBuffer[2];
		arr[0] = switch (result) {
			case EmptyResult emptyResult -> ByteBuffer.wrap(emptyResult.name().getBytes(StandardCharsets.UTF_8));
			case Ok(byte[] value) -> ByteBuffer.wrap(value);
			case Error(String message) -> ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
			case Results(List<Result> results) -> {
				List<ByteBuffer> byteBuffers = new ArrayList<>(results.size());
				int size = 0;
				for (Result r : results) {
					ByteBuffer[] responseBuffers = toResponseBuffer(r);
					for (ByteBuffer responseBuffer : responseBuffers) {
						size += responseBuffer.capacity();
						byteBuffers.add(responseBuffer);
					}
				}
				ByteBuffer res = ByteBuffer.allocateDirect(size);
				for (ByteBuffer byteBuffer : byteBuffers) {
					res.put(byteBuffer);
				}
				yield res.flip();
			}
		};
		arr[1] = ByteBuffer.allocateDirect(2).put((byte)'\r').put((byte)'\n').flip();
		return arr;

	}

	private void closeChannel(SelectionKey key) {
		try {
			key.channel().close();
			key.cancel();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void cleanup() {
		try {
			if (selector != null) selector.close();
			if (serverChannel != null) serverChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() throws IOException {
		if (this.selector != null) selector.close();
		if (this.serverChannel != null) serverChannel.close();
		tcpServerExecutor.shutdown();
	}

	record PendingResponse(SelectionKey key, Result result) { }

}
