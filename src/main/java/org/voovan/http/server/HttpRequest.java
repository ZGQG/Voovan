package org.voovan.http.server;

import org.voovan.http.message.Request;
import org.voovan.http.message.packet.Cookie;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebServer 请求对象
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpRequest extends Request {

	private HttpSession session;
	private String remoteAddres;
	private int remotePort;
	private String characterSet;
	private Map<String, String> parameters;

	private Map<String, Object> attributes;
	
	protected HttpRequest(Request request,String characterSet){
		super(request);
		this.characterSet=characterSet;
		parameters = new HashMap<String, String>();
		attributes = new HashMap<String, Object>();
		parseQueryString();
	}

	/**
	 * 根据 Cookie 名称取 Cookie
	 *
	 * @param name  Cookie 名称
	 * @return Cookie
	 */
	public Cookie getCookie(String name){
		for(Cookie cookie : this.cookies()){
			if(cookie.getName().equals(name)){
				return cookie;
			}
		}
		return null;
	}

	/**
	 * 获取 Session
	 *
	 * @return HTTP-Session 对象
	 */
	public HttpSession getSession() {
		return session;
	}

	/**
	 * 设置一个 Session
	 *
	 * @param session  HTTP-Session 对象
	 */
	protected void setSession(HttpSession session) {
		this.session = session;
	}



	/**
	 * 获取对端连接的 IP
	 *
	 * @return 对端连接的 IP
	 */
	public String getRemoteAddres() {
		String xForwardedFor = header().get("X-Forwarded-For");
		String xRealIP = header().get("X-Real-IP");
		if (xRealIP != null) {
			return xRealIP;
		} else if (xForwardedFor != null) {
			return xForwardedFor.split(",")[0].trim();
		}else{
			return remoteAddres;
		}
	}

	/**
	 * 设置对端连接的 IP
	 *
	 * @param remoteAddres 对端连接的 IP
	 */
	protected void setRemoteAddres(String remoteAddres) {
		this.remoteAddres = remoteAddres;
	}

	/**
	 * 获取对端连接的端口
	 *
	 * @return 对端连接的端口
	 */
	public int getRemotePort() {
		return remotePort;
	}

	/**
	 * 设置对端连接的端口
	 *
	 * @param port 对端连接的端口
	 */
	protected void setRemotePort(int port) {
		this.remotePort = port;
	}

	/**
	 * 获取当前默认字符集
	 *
	 * @return 字符集
	 */
	public String getCharacterSet() {
		return characterSet;
	}

	/**
	 * 设置当前默认字符集
	 *
	 * @param charset 字符集
	 */
	protected void setCharacterSet(String charset) {
		this.characterSet = charset;
	}
	
	/**
	 * 获取请求字符串
	 *
	 * @return 请求字符串
	 */
	protected String getQueryString(){
		return getQueryString(characterSet);
	}
	
	/**
	 * 获取请求参数集合
	 *
	 * @return 请求参数集合
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}
	
	/**
	 * 获取请求参数
	 *
	 * @param paramName 请求参数名称
	 * @return 请求参数值
	 */
	public String getParameter(String paramName){
		return parameters.get(paramName);
	}

	/**
	 * 获取 int 类型的数据
	 * @param paramName 请求参数名称
	 * @return int 类型的数据
     */
	public int getParameterAsInt(String paramName){
		try {
			return (int) TString.toObject(parameters.get(paramName), int.class);
		}catch(Exception e){
			throw new RuntimeException("Get parameter ["+paramName+"] as int error.",e);
		}
	}

	/**
	 * 获取 float 类型的数据
	 * @param paramName 请求参数名称
	 * @return float 类型的数据
	 */
	public float getParameterAsFloat(String paramName){
		try {
			return (float) TString.toObject(parameters.get(paramName), float.class);
		}catch(Exception e){
			throw new RuntimeException("Get parameter ["+paramName+"] as float error.",e);
		}
	}

	/**
	 * 获取 long 类型的数据
	 * @param paramName 请求参数名称
	 * @return long 类型的数据
	 */
	public long getParameterAsLong(String paramName){
		try {
			return (long) TString.toObject(parameters.get(paramName), long.class);
		}catch(Exception e){
			throw new RuntimeException("Get parameter ["+paramName+"] as long error.",e);
		}
	}

	/**
	 * 获取 short 类型的数据
	 * @param paramName 请求参数名称
	 * @return short 类型的数据
	 */
	public short getParameterAsShort(String paramName){
		try {
			return (short) TString.toObject(parameters.get(paramName), short.class);
		}catch(Exception e){
			throw new RuntimeException("Get parameter ["+paramName+"] as short error.",e);
		}
	}

	/**
	 * 获取 double 类型的数据
	 * @param paramName 请求参数名称
	 * @return double 类型的数据
	 */
	public double getParameterAsDouble(String paramName){
		try {
			return (double) TString.toObject(parameters.get(paramName), double.class);
		}catch(Exception e){
			throw new RuntimeException("Get parameter ["+paramName+"] as double error.",e);
		}
	}

	/**
	 * 获取 boolean 类型的数据
	 * @param paramName 请求参数名称
	 * @return boolean 类型的数据
	 */
	public boolean getParameterAsBoolean(String paramName){
		try {
			return (boolean) TString.toObject(parameters.get(paramName), boolean.class);
		}catch(Exception e){
			throw new RuntimeException("Get parameter ["+paramName+"] as boolean error.",e);
		}
	}

	/**
	 * 获取 byte 类型的数据
	 * @param paramName 请求参数名称
	 * @return byte 类型的数据
	 */
	public byte getParameterAsByte(String paramName){
		try {
			return (byte) TString.toObject(parameters.get(paramName), byte.class);
		}catch(Exception e){
			throw new RuntimeException("Get parameter ["+paramName+"] as byte error.",e);
		}
	}

	/**
	 * 获取 char 类型的数据
	 * @param paramName 请求参数名称
	 * @return char 类型的数据
	 */
	public char getParameterAsChar(String paramName){
		try {
			return (char) TString.toObject(parameters.get(paramName), char.class);
		}catch(Exception e){
			throw new RuntimeException("Get parameter ["+paramName+"] as char error.",e);
		}
	}

	/**
	 * 获取 自定义 类型的数据
	 * @param clazz  自定义数据类型
	 * @return char 自定义数据类型的对象,转换时字段忽略大小写
	 */
	public char getParameterAsObject(Class<?> clazz){
		try {
			return (char) TReflect.getObjectFromMap(clazz, TObject.cast(getParameters()), true);
		} catch (ReflectiveOperationException | ParseException e) {
			throw new RuntimeException("Conver parameters to "+clazz.getCanonicalName()+" error.",e);
		}
	}

	/**
	 * 获取请求参数名称集合
	 *
	 * @return 请求参数集合
	 */
	public List<String> getParameterNames(){
		return Arrays.asList(parameters.keySet().toArray(new String[]{}));
	}

	/**
	 * 获取请求属性.此属性是会话级的
	 * @return 返回请求属性
     */
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	/**
	 * 获取请求属性值
	 * @param attrName 请求属性名称
	 * @return 请求属性值
     */
	public Object getAttributes(String attrName){
		return attributes.get(attrName);
	}

	/**
	 * 设置请求属性
	 * @param attrName 请求属性名称
	 * @param attrValue 请求属性值
     */
	public void setAttributes(String attrName,Object attrValue){
		attributes.put(attrName,attrValue);
	}


	/**
	 * 解析请求参数
	 */
	private void  parseQueryString() {
		if(getQueryString()!=null){
			String[] parameterEquals = getQueryString().split("&");
			for(String parameterEqual :parameterEquals){
				int equalFlagPos = parameterEqual.indexOf("=");
				if(equalFlagPos>0){
					String name = parameterEqual.substring(0, equalFlagPos);
					String value = parameterEqual.substring(equalFlagPos+1, parameterEqual.length());
					try {
						parameters.put(name, URLDecoder.decode(value,characterSet));
					} catch (UnsupportedEncodingException e) {
						Logger.error("QueryString URLDecoder.decode failed by charset:"+characterSet,e);
					}
				}else{
					parameters.put(parameterEqual, null);
				}
			}
		}
	}



	/**
	 * 重置请求
	 * 		用于在 HttpFilter 中重新定向,其他地方无用
	 * @param url 请求地址,"/"起始,可以包含"?"参数引导及参数.
	 */
	public void redirect(String url){
		String[] parsedURL = url.split("\\?");

		this.protocol().clear();
		this.body().clear();
		this.parts().clear();

		if(parsedURL.length>0) {
			this.protocol().setPath(parsedURL[0]);
		}

		if(parsedURL.length > 1) {
			this.protocol().setQueryString(parsedURL[1]);
		}
	}
}
