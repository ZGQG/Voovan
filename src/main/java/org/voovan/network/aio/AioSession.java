package org.voovan.network.aio;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.WritePendingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * NIO 会话连接对象
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AioSession extends IoSession<AioSocket>  {

	private AsynchronousSocketChannel	socketChannel;


	/**
	 * 构造函数
	 * 
	 * @param socket
	 */
	AioSession(AioSocket socket) {
		super(socket);
		if (socket != null) {
			this.socketChannel = socket.socketChannel();
		} else {
			Logger.error("SocketChannel is null, please check it.");
		}
	}

	@Override
	public String loaclAddress() {
		if (this.isConnected()) {
			try {
				InetSocketAddress socketAddress = TObject.cast(socketChannel.getLocalAddress());
				return socketAddress.getHostName();
			} catch (IOException e) {
				Logger.error("Get SocketChannel local address failed.",e);
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public int loaclPort() {
		if (this.isConnected()) {
			try {
				InetSocketAddress socketAddress = TObject.cast(socketChannel.getLocalAddress());
				return socketAddress.getPort();
			} catch (IOException e) {
				Logger.error("Get SocketChannel local port failed.",e);
				return -1;
			}
		} else {
			return -1;
		}
	}

	@Override
	public String remoteAddress() {
		if (this.isConnected()) {
			try {
				InetSocketAddress socketAddress = TObject.cast(socketChannel.getRemoteAddress());
				return socketAddress.getHostString();
			} catch (IOException e) {
				Logger.error("Get SocketChannel remote address failed.",e);
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public int remotePort() {
		if (this.isConnected()) {
			try {
				InetSocketAddress socketAddress = TObject.cast(socketChannel.getRemoteAddress());
				return socketAddress.getPort();
			} catch (IOException e) {
				Logger.error("Get SocketChannel remote port failed.",e);
				return -1;
			}
		} else {
			return -1;
		}
	}

	@Override
	protected int read(ByteBuffer buffer) throws IOException {
		int readSize = 0;
		if (buffer != null) {
			try {
				readSize = this.getByteBufferChannel().readHead(buffer);
			} catch (Exception e) {
				Logger.error("Read socketChannel failed.",e);
				// 如果出现异常则返回-1,表示读取通道结束
				readSize = -1;
			}
		}
		return readSize;
	}

	@Override
	public int send(ByteBuffer buffer) throws IOException {
		int totalSendByte = 0;
		if (isConnected() && buffer != null) {
			//循环发送直到全不内容发送完毕
			while(isConnected() && buffer.remaining()!=0){
				try {
					Future<Integer> sendResult = socketChannel.write(buffer);
					if(sendResult==null){
						break;
					}

					try {
						totalSendByte += sendResult.get();
					} catch (InterruptedException | ExecutionException e) {
						throw new IOException("Get send byte count error: "+e.getMessage(), e);
					}

				}catch(WritePendingException e){
					continue;
				}
			}
		}
		return totalSendByte;
	}

	@Override
	protected MessageSplitter getMessagePartition() {
		return this.socketContext().messageSplitter();
	}

	@Override
	public boolean isConnected() {
		return this.socketContext().isConnected();
	}

	/**
	 * 会话是否打开
	 *
	 * @return true: 打开,false: 关闭
	 */
	@Override
	public boolean isOpen() {
		return this.socketContext().isOpen();
	}

	@Override
	public boolean close() {
		// 关闭 socket
		return this.socketContext().close();
	}

	@Override
	public String toString() {
		return "[" + this.loaclAddress() + ":" + this.loaclPort() + "] -> [" + this.remoteAddress() + ":" + this.remotePort() + "]";
	}
}
