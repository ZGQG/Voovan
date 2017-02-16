package org.voovan.network;

import org.voovan.tools.Chain;

import java.io.IOException;

/**
 * socket 上下文
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class SocketContext {
	protected String host;
	protected int port;
	protected int readTimeout;
	
	protected IoHandler handler;
	protected Chain<IoFilter> filterChain;
	protected MessageSplitter messageSplitter;
	protected SSLManager sslManager;
	protected ConnectModel connectModel;
	protected int bufferSize = 10240;


	/**
	 * 构造函数
	 * @param host    主机地址
	 * @param port    主机端口
	 * @param readTimeout 超时时间
     */
	public SocketContext(String host,int port,int readTimeout) {
		this.host = host;
		this.port = port;
		this.readTimeout = readTimeout;
		connectModel = null;
		filterChain = new Chain<IoFilter>();
	}
	
	/**
	 * 克隆对象
	 * @param parentSocketContext 父 socket 对象
	 */
	protected void copyFrom(SocketContext parentSocketContext){
		this.readTimeout = parentSocketContext.readTimeout;
		this.handler = parentSocketContext.handler;
		this.filterChain = parentSocketContext.filterChain;
		this.messageSplitter = parentSocketContext.messageSplitter;
		this.sslManager = parentSocketContext.sslManager;
		this.bufferSize = parentSocketContext.bufferSize;
	}

	/**
	 * 获取缓冲区大小
	 * @return 缓冲区大小 (default:1024)
	 */
	public int getBufferSize() {
		return bufferSize;
	}

	/**
	 * 设置缓冲区大小
	 * @param bufferSize 缓冲区大小 (default:1024)
	 */
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	/**
	 * 无参数构造函数
	 */
	protected SocketContext() {
		filterChain = new Chain<IoFilter>();
	}

	/**
	 * 获取 SSL 管理器
	 * @return SSL 管理器
     */
	public SSLManager getSSLManager() {
		return sslManager;
	}

	/**
	 * 设置 SSL 管理器
	 * @param sslManager SSL 管理器
     */
	public void setSSLManager(SSLManager sslManager) {
		if(this.sslManager==null){
			this.sslManager = sslManager;
		}
	}

	/**
	 * 获取主机地址
	 * @return 主机地址
     */
	public String getHost() {
		return host;
	}

	/**
	 * 获取主机端口
	 * @return 主机端口
     */
	public int getPort() {
		return port;
	}

	/**
	 * 获取超时时间
	 * @return 超时时间
	 */
	public int getReadTimeout() {
		return readTimeout;
	}
	
	/**
	 * 获取连接模式
	 * @return 连接模式
	 */
	public ConnectModel getConnectModel() {
		return connectModel;
	}

	/**
	 * 获取业务处理句柄
	 * @return 业务处理句柄
	 */
	public IoHandler handler(){
		return this.handler;
	} 
	
	/**
	 * 设置业务处理句柄
	 * @param handler 业务处理句柄
	 */
	public void handler(IoHandler handler){
		this.handler = handler;
	} 
	
	/**
	 * 获取过滤器链
	 * @return 过滤器链
	 */
	public Chain<IoFilter> filterChain(){
		return this.filterChain;
	}
	
	/**
	 * 获取消息粘包分割器
	 * @return 消息粘包分割器
	 */
	public MessageSplitter messageSplitter() {
		return this.messageSplitter;
	}
	
	/**
	 * 设置消息粘包分割器
	 * @param  messageSplitter 消息分割器
	 */
	public void messageSplitter(MessageSplitter messageSplitter) {
		this.messageSplitter = messageSplitter;
	}
	
	/**
	 * 启动上下文连接
	 * @throws IOException IO 异常
	 */
	public abstract void start() throws IOException;

	/**
	 * 上下文连接是否打开
	 * @return true:连接打开,false:连接关闭
	 */
	public abstract boolean isOpen();


	/**
	 * 上下文连接是否连接
	 * @return true:连接,false:断开
	 */
	public abstract boolean isConnected();
	
	/**
	 * 关闭连接
	 * @return 是否关闭
	 */
	public abstract boolean close();
}
