package org.voovan.network.nio;

import org.voovan.Global;
import org.voovan.network.*;
import org.voovan.network.exception.IoFilterException;
import org.voovan.network.exception.ReadMessageException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.network.messagesplitter.TimeOutMesssageSplitter;
import org.voovan.tools.log.Logger;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.InetSocketAddress;
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
	
	/**
	 * socket 连接
	 * @param host      监听地址
	 * @param port		监听端口
	 * @param readTimeout   超时事件
	 * @throws IOException	IO异常
	 */
	public NioSocket(String host,int port,int readTimeout) throws IOException{
		super(host, port, readTimeout);
		this.readTimeout = readTimeout;
		provider = SelectorProvider.provider();
		socketChannel = provider.openSocketChannel();
		socketChannel.socket().setSoTimeout(this.readTimeout);
		socketChannel.connect(new InetSocketAddress(this.host,this.port));
		socketChannel.configureBlocking(false);
		session = new NioSession(this);
		connectModel = ConnectModel.CLIENT;
		this.handler = new SynchronousHandler();
		init();
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
			init();
		} catch (IOException e) {
			Logger.error("Create socket channel failed",e);
		}
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
	private void init()  {
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
	
	private void initSSL() throws SSLException{
		if (connectModel == ConnectModel.SERVER && sslManager != null) {
			sslManager.createServerSSLParser(session);
		} else if (connectModel == ConnectModel.CLIENT && sslManager != null) {
			sslManager.createClientSSLParser(session);
		}
	}
	
	/**
	 * 启动
	 * @throws IOException IO 异常
	 */
	public void start() throws IOException  {
		initSSL();
		
		//如果没有消息分割器默认使用读取超时时间作为分割器
		if(messageSplitter == null){
			messageSplitter = new TimeOutMesssageSplitter();
		}
		
		if(socketChannel!=null && socketChannel.isOpen()){
			NioSelector nioSelector = new NioSelector(selector,this);

			//循环放入独立的线程中处理
			Global.getThreadPool().execute( () -> {
				nioSelector.eventChose();
			});
		}
	}

	@Override
	public boolean isConnect() {
		if(socketChannel!=null){
			return socketChannel.isOpen();
		}else{
			return false;
		}
	}

	/**
	 * 同步读取消息
	 * @return 读取出的对象
	 */
	public Object synchronouRead() throws ReadMessageException {
		Object readObject = null;
		while(true){
			readObject = session.getAttribute("SocketResponse");
			if(readObject!=null) {
				if(readObject instanceof Exception){
					throw new ReadMessageException("Method synchronouRead error! ",(Exception) readObject);
				}
				session.removeAttribute("SocketResponse");
				break;
			}
		}
		return readObject;
	}

	/**
	 * 发送消息
	 * @param obj  要发送的对象
	 * @throws SendMessageException  消息发送异常
	 */
	public void synchronouSend(Object obj) throws SendMessageException{
		if (obj != null) {
			try {
				filterChain().rewind();
				while (filterChain().hasNext()) {
					IoFilter fitler = filterChain().next();
					obj = fitler.encode(session, obj);
				}
				EventProcess.sendMessage(session, obj);
			}catch (Exception e){
				throw new SendMessageException("Method synchronouSend error! ",e);
			}
		}
	}

	@Override
	public boolean close(){
		if(socketChannel!=null){
			try{
				socketChannel.close();
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
