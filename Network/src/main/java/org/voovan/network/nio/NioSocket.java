package org.voovan.network.nio;

import org.voovan.Global;
import org.voovan.network.ConnectModel;
import org.voovan.network.EventTrigger;
import org.voovan.network.SocketContext;
import org.voovan.network.exception.ReadMessageException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.network.handler.SynchronousHandler;
import org.voovan.network.messagesplitter.TrasnferSplitter;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
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
public class NioSocket extends SocketContext{
	private SelectorProvider provider;
	private Selector selector;
	private SocketChannel socketChannel;
	private NioSession session;
	private NioSelector nioSelector;

	/**
	 * socket 连接
	 * 		默认不会触发空闲事件
	 * @param host      监听地址
	 * @param port		监听端口
	 * @param readTimeout   超时时间, 单位: 毫秒
	 * @throws IOException	IO异常
	 */
	public NioSocket(String host,int port,int readTimeout) throws IOException{
		super(host, port, readTimeout);
		init();
	}

	/**
	 * socket 连接
	 * @param host      监听地址
	 * @param port		监听端口
     * @param idleInterval	空闲事件触发时间, 单位: 秒
	 * @param readTimeout   超时时间, 单位: 毫秒
	 * @throws IOException	IO异常
	 */
	public NioSocket(String host,int port,int readTimeout, int idleInterval) throws IOException{
		super(host, port, readTimeout, idleInterval);
		init();
	}

	private void init() throws IOException {
		provider = SelectorProvider.provider();
		socketChannel = provider.openSocketChannel();
		socketChannel.socket().setSoTimeout(this.readTimeout);
		session = new NioSession(this);
		connectModel = ConnectModel.CLIENT;
	}

	/**
	 * 构造函数
	 * @param parentSocketContext 父 SocketChannel 对象
	 * @param socketChannel SocketChannel 对象
	 */
	protected NioSocket(SocketContext parentSocketContext,SocketChannel socketChannel){
		try {
			provider = SelectorProvider.provider();
			this.host = socketChannel.socket().getLocalAddress().getHostAddress();
			this.port = socketChannel.socket().getLocalPort();
			this.socketChannel = socketChannel;
			socketChannel.configureBlocking(false);
			this.copyFrom(parentSocketContext);
			this.socketChannel().socket().setSoTimeout(this.readTimeout);
			session = new NioSession(this);
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
	public <T> void setOption(SocketOption<T> name, T value) throws IOException {
		socketChannel.setOption(name, value);
	}
	
	
	/**
	 * 获取 SocketChannel 对象
	 * @return SocketChannel 对象
	 */
	public SocketChannel socketChannel(){
		return this.socketChannel;
	}
	
	/**
	 * 初始化函数
	 */
	private void registerSelector()  {
		try{
			selector = provider.openSelector();
			socketChannel.register(selector, SelectionKey.OP_READ);
		}catch(IOException e){
			Logger.error("init SocketChannel failed by openSelector",e);
		}
	}

	/**
	 * 获取 Session 对象
	 * @return Session 对象
     */
	public NioSession getSession(){
		return session;
	}

	/**
	 * 启动同步的上下文连接,
	 * 		阻塞方法
	 * @throws IOException IO 异常
	 */
	public void start() throws IOException  {
		initSSL(session);

		socketChannel.connect(new InetSocketAddress(this.host, this.port));
		socketChannel.configureBlocking(false);

		registerSelector();

		if(socketChannel!=null && socketChannel.isOpen()){
			nioSelector = new NioSelector(selector,this);
			nioSelector.eventChose();
		}
	}

	/**
	 * 启动同步的上下文连接
	 * 		非阻塞方法
	 */
	public void syncStart(){

		Global.getThreadPool().execute(new Runnable(){
			public void run() {
				try {
					start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		waitConnected(session);
	}

	protected void acceptStart() throws IOException {
		final NioSocket nioSocket = this;

		Global.getThreadPool().execute(new Runnable() {
			@Override
			public void run() {
				try {
					initSSL(session);

					registerSelector();

					if (socketChannel != null && socketChannel.isOpen()) {
						nioSelector = new NioSelector(selector, nioSocket);
						nioSelector.eventChose();
					}
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * 重连当前连接
	 * @throws IOException IO 异常
	 */
	public void reStart() throws IOException {
		init();
		this.start();
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
			if (socketChannel.getRemoteAddress() != null) {
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

		if(socketChannel!=null){
			try{
				socketChannel.close();

				//如果有未读数据等待数据处理完成
				session.wait(this.getReadTimeout());

				nioSelector.release();
				session.getByteBufferChannel().release();
				if(session.getSSLParser()!=null){
					session.getSSLParser().release();
				}
				return true;
			} catch(IOException e){
				Logger.error("Close SocketChannel failed",e);
				return false;
			}
		}else{
			return true;
		}
	}

}
