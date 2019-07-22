package org.voovan.network.tcp;

import org.voovan.network.*;
import org.voovan.network.exception.ReadMessageException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.network.handler.SynchronousHandler;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * NioSocket 连接
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TcpSocket extends SocketContext<SocketChannel, TcpSession> {
	private SelectorProvider provider;
	private SocketChannel socketChannel;
	private TcpSession session;

	//用来阻塞当前Socket
	private Object waitObj = new Object();


	/**
	 * socket 连接
	 * 		默认不会出发空闲事件, 默认发超时时间: 1s
	 * @param host      监听地址
	 * @param port		监听端口
	 * @param readTimeout   超时时间, 单位: 毫秒
	 * @throws IOException	IO异常
	 */
	public TcpSocket(String host, int port, int readTimeout) throws IOException{
		super(host, port, readTimeout);
	}

	/**
	 * socket 连接
	 *      默认发超时时间: 1s
	 * @param host      监听地址
	 * @param port		监听端口
	 * @param idleInterval	空闲事件触发时间, 单位: 秒
	 * @param readTimeout   超时时间, 单位: 毫秒
	 * @throws IOException	IO异常
	 */
	public TcpSocket(String host, int port, int readTimeout, int idleInterval) throws IOException{
		super(host, port, readTimeout, idleInterval);
	}

	/**
	 * socket 连接
	 * @param host      监听地址
	 * @param port		监听端口
	 * @param idleInterval	空闲事件触发时间, 单位: 秒
	 * @param readTimeout   超时时间, 单位: 毫秒
	 * @param sendTimeout 发超时时间, 单位: 毫秒
	 * @throws IOException	IO异常
	 */
	public TcpSocket(String host, int port, int readTimeout, int sendTimeout, int idleInterval) throws IOException{
		super(host, port, readTimeout, sendTimeout, idleInterval);
	}

	private void init() throws IOException {
		provider = SelectorProvider.provider();
		socketChannel = provider.openSocketChannel();
		socketChannel.socket().setSoTimeout(this.readTimeout);

		session = new TcpSession(this);
		connectModel = ConnectModel.CLIENT;
	}

	/**
	 * 构造函数
	 * @param parentSocketContext 父 SocketChannel 对象
	 * @param socketChannel SocketChannel 对象
	 */
	public TcpSocket(SocketContext parentSocketContext, SocketChannel socketChannel){
		try {
			provider = SelectorProvider.provider();
			this.host = socketChannel.socket().getLocalAddress().getHostAddress();
			this.port = socketChannel.socket().getLocalPort();
			this.socketChannel = socketChannel;
			socketChannel.configureBlocking(false);
			this.copyFrom(parentSocketContext);
			this.socketChannel().socket().setSoTimeout(this.readTimeout);
			session = new TcpSession(this);
			connectModel = ConnectModel.SERVER;
		} catch (IOException e) {
			Logger.error("Create socket channel failed",e);
		}
	}

	@Override
	public void setIdleInterval(int idleInterval) {
		this.idleInterval = idleInterval;
	}

	/**
	 * 设置 Socket 的 Option 选项
	 *
	 * @param name   SocketOption类型的枚举, 参照:SocketChannel.setOption的说明
	 * @param value  SocketOption参数
	 * @param <T> 范型
	 * @throws IOException IO异常
	 */
	@Override
	public <T> void setOption(SocketOption<T> name, T value) throws IOException {
		socketChannel.setOption(name, value);
	}

	/**
	 * 获取 SocketChannel 对象
	 * @return SocketChannel 对象
	 */
	@Override
	public SocketChannel socketChannel(){
		return this.socketChannel;
	}

	/**
	 * 获取 Session 对象
	 * @return Session 对O象
	 */
	public TcpSession getSession(){
		return session;
	}

	/**
	 * 启动同步的上下文连接,
	 * 		阻塞方法
	 * @throws IOException IO 异常
	 */
	public void start() throws IOException  {
		syncStart();

		synchronized (waitObj){
			try {
				waitObj.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 启动同步的上下文连接
	 * 		非阻塞方法
	 */
	public void syncStart() throws IOException {
		init();
		initSSL(session);

		socketChannel.connect(new InetSocketAddress(this.host, this.port));
		socketChannel.configureBlocking(false);
		bindToSocketSelector(SelectionKey.OP_READ);

        waitConnect();
    }

	protected void acceptStart() throws IOException {
		try {
			initSSL(session);

			bindToSocketSelector(SelectionKey.OP_READ);
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	@Override
	public boolean isOpen() {
		if(socketChannel!=null){
			return socketChannel.isOpen();
		}else{
			return false;
		}
	}

	@Override
	public boolean isConnected() {
		try {
			if (socketChannel.isConnected() && socketChannel.getRemoteAddress() != null) {
				return true;
			} else {
				return false;
			}
		}catch(Exception e){
			return false;
		}
	}

	/**
	 * 同步读取消息
	 * @return 读取出的对象
	 * @throws ReadMessageException 读取消息异常
	 */
	public Object synchronouRead() throws ReadMessageException {
		return session.syncRead();
	}

	/**
	 * 同步发送消息
	 * @param obj  要发送的对象
	 * @throws SendMessageException  消息发送异常
	 */
	public void synchronouSend(Object obj) throws SendMessageException {
		session.syncSend(obj);
	}

	@Override
	public boolean close(){
        try {
            if(socketChannel!=null && socketChannel.isOpen()){
                socketChannel.close();
                session.release();
                return true;
            }
        } catch (IOException e) {
        	Logger.error("TcpSocket.close failed", e);
		} finally {
			synchronized (waitObj) {
				waitObj.notify();
			}
		}

		return false;
	}

}
