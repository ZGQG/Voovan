package org.voovan.http.server;

import org.voovan.http.server.context.HttpModuleConfig;
import org.voovan.http.server.context.HttpRouterConfig;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.network.SSLManager;
import org.voovan.network.aio.AioServerSocket;
import org.voovan.network.messagesplitter.HttpMessageSplitter;
import org.voovan.tools.TEnv;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

import java.io.IOException;

/**
 * WebServer 对象
 * 
 * @author helyho
 * 
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebServer {
	private AioServerSocket	aioServerSocket;
	private HttpDispatcher	httpDispatcher;
	private WebSocketDispatcher webSocketDispatcher;
	private SessionManager sessionManager;
	private WebServerConfig config;

	/**
	 * 构造函数
	 * 
	 * @param config  WEB 配对对象
	 * @throws IOException
	 *             异常
	 */
	public WebServer(WebServerConfig config) throws IOException {
		this.config = config;

		//[Socket] 准备 socket 监听
		aioServerSocket = new AioServerSocket(config.getHost(), config.getPort(), config.getTimeout()*1000);

		//[HTTP] 构造 SessionManage
		sessionManager = SessionManager.newInstance(config);

		//[HTTP]请求派发器创建
		this.httpDispatcher = new HttpDispatcher(config,sessionManager);

		this.webSocketDispatcher = new WebSocketDispatcher(config);

		//[Socket]确认是否启用 HTTPS 支持
		if(config.isHttps()) {
			SSLManager sslManager = new SSLManager("TLS", false);
			sslManager.loadCertificate(System.getProperty("user.dir") + config.getHttps().getCertificateFile(),
					config.getHttps().getCertificatePassword(), config.getHttps().getKeyPassword());
			aioServerSocket.setSSLManager(sslManager);
		}

		aioServerSocket.handler(new WebServerHandler(config, httpDispatcher,webSocketDispatcher));
		aioServerSocket.filterChain().add(new WebServerFilter());
		aioServerSocket.messageSplitter(new HttpMessageSplitter());
	}

	/**
	 * 将配置文件中的 Router 配置载入到 WebServer
     */
	private void  initConfigedRouter(){
		for(HttpRouterConfig httpRouterConfig : config.getRouterConfigs()){
			String method = httpRouterConfig.getMethod();
			String route = httpRouterConfig.getRoute();
			String className = httpRouterConfig.getClassName();
			otherMethod(method,route,httpRouterConfig.getHttpRouterInstance());
		}
	}

	/**
	 * 模块安装
     */
	public void initModule() {
		for (HttpModuleConfig httpModuleConfig : config.getModuleonfigs()) {
			HttpModule httpModule = httpModuleConfig.getHttpModuleInstance(this);
			if(httpModule!=null){
				httpModule.install();
			}
		}
	}


	/**
	 * 获取 Http 服务配置对象
	 * @return 返回 Http 服务配置对象
     */
	public WebServerConfig getWebServerConfig() {
		return config;
	}

	/**
	 * 以下是一些 HTTP 方法的成员函数
	 */

	/**
	 * GET 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
     * @return WebServer对象
     */
	public WebServer get(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("GET", routeRegexPath, router);
		return this;
	}

	/**
	 * POST 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
     */
	public WebServer post(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("POST", routeRegexPath, router);
		return this;
	}

	/**
	 * HEAD 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer head(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("HEAD", routeRegexPath, router);
		return this;
	}

	/**
	 * PUT 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer put(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("PUT", routeRegexPath, router);
		return this;
	}

	/**
	 * DELETE 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer delete(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("DELETE", routeRegexPath, router);
		return this;
	}

	/**
	 * TRACE 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer trace(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("TRACE", routeRegexPath, router);
		return this;
	}

	/**
	 * CONNECT 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer connect(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("CONNECT", routeRegexPath, router);
		return this;
	}

	/**
	 * OPTIONS 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer options(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("OPTIONS", routeRegexPath, router);
		return this;
	}

	/**
	 * 其他请求
	 * @param method 请求方法
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer otherMethod(String method, String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteMethod(method);
		httpDispatcher.addRouteHandler(method, routeRegexPath, router);
		return this;
	}

	/**
	 * WebSocket 服务
	 * @param routeRegexPath 匹配路径
	 * @param router WebSocket处理句柄
	 * @return WebServer对象
     */
	public WebServer socket(String routeRegexPath, WebSocketRouter router) {
		webSocketDispatcher.addRouteHandler(routeRegexPath, router);
		return this;
	}

	/**
	 * 构建新的 WebServer,从配置对象读取配置
	 * @param config  WebServer配置类
	 * @return WebServer 对象
	 */
	public static WebServer newInstance(WebServerConfig config) {

		try {
			if(config!=null) {
				return new WebServer(config);
			}else{
				Logger.error("Create WebServer failed: WebServerConfig object is null.");
			}
		} catch (IOException e) {
			Logger.error("Create WebServer failed.",e);
		}

		return null;
	}

	/**
	 * 构建新的 WebServer,指定服务端口
	 * @param port  HTTP 服务的端口号
	 * @return WebServer 对象
	 */
	public static WebServer newInstance(int port) {
		WebServerConfig config = WebContext.getWebServerConfig();
		config.setPort(port);
		return newInstance(config);
	}

	/**
	 * 构建新的 WebServer,从配置文件读取配置
	 *
	 * @return WebServer 对象
	 */
	public static WebServer newInstance() {
		return newInstance(WebContext.getWebServerConfig());
	}

	/**
	 * 读取Classes目录和lib目录中的class或者jar文件
	 */
	private static void loadContextBin(){
		try {
			TEnv.loadBinary(TEnv.getSystemPath("classes"));
			TEnv.loadJars(TEnv.getSystemPath("lib"));
		} catch (NoSuchMethodException | IOException | SecurityException e) {
			Logger.warn("Voovan WebServer Loader ./classes or ./lib error." ,e);
		}
	}

	/**
	 * 启动服务
	 *
	 * @return WebServer 对象
	 */
	public WebServer serve() {
		try {
			//输出欢迎信息
			WebContext.welcome(config);
			WebContext.initWebServerPlugin();

			loadContextBin();
			initConfigedRouter();
			initModule();
			Logger.simple("Process ID: "+ TEnv.getCurrentPID());
			Logger.simple("WebServer working on: http"+(config.isHttps()?"s":"")+"://"+config.getHost()+":"+config.getPort()+" ...");
			aioServerSocket.start();
		} catch (IOException e) {
			Logger.error("Start HTTP server error.",e);
		}
		return this;
	}

	/**
	 * 启动 WebServer 服务
	 * @param args 启动参数
	 */
	public static void main(String[] args) {
		 WebServerConfig config = null;
		if(args.length>0){
			for(int i=0;i<args.length;i++){
				//服务端口
				if(args[i].equals("-p")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.setPort(Integer.parseInt(args[i]));
				}

				//连接超时时间(s)
				if(args[i].equals("-t")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.setTimeout(Integer.parseInt(args[i]));
				}

				//上下文路径
				if(args[i].equals("-cp")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.setContextPath(args[i]);
				}

				//首页索引文件的名称,默认index.htm,index.html,default.htm,default.htm
				if(args[i].equals("-i")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.setIndexFiles(args[i]);
				}

				//匹配路由不区分大小写,默认是 false
				if(args[i].equals("-mri")){
					config = config==null?WebContext.getWebServerConfig():config;
					config.setMatchRouteIgnoreCase(true);
				}

				//默认字符集,默认 UTF-8
				if(args[i].equals("-c")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.setCharacterSet(args[i]);
				}

				//是否启用Gzip压缩,默认 true
				if(args[i].equals("--noGzip")){
					config = config==null?WebContext.getWebServerConfig():config;
					config.setGzip(false);
				}

				//是否记录access.log,默认 true
				if(args[i].equals("--noAccessLog")){
					config = config==null?WebContext.getWebServerConfig():config;
					config.setAccessLog(false);
				}

				//HTTPS 证书
				if(args[i].equals("--https.CertificateFile")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.getHttps().setCertificateFile(args[i]);
				}
				//证书密码
				if(args[i].equals("--https.CertificatePassword")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.getHttps().setCertificatePassword(args[i]);
				}
				//证书Key 密码
				if(args[i].equals("--https.KeyPassword")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.getHttps().setKeyPassword(args[i]);
				}

				//输出版本号
				if(args[i].equals("-v")){
					Logger.simple("Version:"+WebContext.getVERSION());
					return;
				}

				if(args[i].equals("--help") || args[i].equals("-h") || args[i].equals("-?")){
					Logger.simple("Usage: java -jar voovan-framework.jar [Options]");
					Logger.simple("");
					Logger.simple("Start voovan webserver");
					Logger.simple("");
					Logger.simple("Options:");
					Logger.simple(TString.rightPad("  -p ",35,' ')+"Webserver bind port number");
					Logger.simple(TString.rightPad("  -t ",35,' ')+"Socket timeout");
					Logger.simple(TString.rightPad("  -cp ",35,' ')+"Context path, contain webserver static file");
					Logger.simple(TString.rightPad("  -i ",35,' ')+"index file for client access to webserver");
					Logger.simple(TString.rightPad("  -mri ",35,' ')+"Match route ignore case");
					Logger.simple(TString.rightPad("  -c ",35,' ')+"set default charset");
					Logger.simple(TString.rightPad("  --noGzip ",35,' ')+"Do not use gzip for client");
					Logger.simple(TString.rightPad("  --noAccessLog ",35,' ')+"Do not write access log to access.log");
					Logger.simple(TString.rightPad("  --https.CertificateFile ",35,' ')+"Certificate file for https");
					Logger.simple(TString.rightPad("  --https.CertificatePassword ",35,' ')+"ertificate file for https");
					Logger.simple(TString.rightPad("  --https.KeyPassword ",35,' ')+"Certificate file for https");
					Logger.simple(TString.rightPad("  -h or --help ",35,' ')+"how to use this command");
					Logger.simple(TString.rightPad("  -v ",35,' ')+"Show the version information");
					Logger.simple("");

					Logger.simple("This WebServer based on VoovanFramework.");
					Logger.simple("WebSite: http://www.voovan.org");
					Logger.simple("Author: helyho");
					Logger.simple("E-mail: helyho@gmail.com");
					Logger.simple("");

					return;
				}
			}
		}
		config = config==null?WebContext.getWebServerConfig():config;

		WebServer webServer = WebServer.newInstance(config);

		webServer.serve();
	}
}
