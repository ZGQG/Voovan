package org.voovan.tools.log;

import org.voovan.tools.TFile;
import org.voovan.tools.TProperties;

import java.io.File;

/**
 * 静态对象类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class StaticParam {
	private static long		startTimeMillis	= System.currentTimeMillis();
	private static File		configFile		= loadConfig();

	public final static String LOG_LEVEL = "ALL";
	public final static String LOG_FILE = null;
	public final static String LOG_TYPE = "STDOUT";
	public final static String LOG_TEMPLATE = "--------------------------------------------------------------------------------------------------------------------------------------------------" +
										"{{n}}[{{P}}] [{{D}}] [Thread:{{T}}] [Time:{{R}}] ({{F}}:{{L}}) {{n}}" +
										"--------------------------------------------------------------------------------------------------------------------------------------------------" +
										"{{n}}{{I}}{{n}}{{n}}";
	public final static String LOG_INFO_INDENT = "";

	/**
	 * 读取日志配置文件信息
	 * @return 日志配置文件对象
     */
	protected static File loadConfig(){
		File tmpFile = TFile.getResourceFile("logger.properties");
		if(tmpFile!=null && tmpFile.exists()){
			return tmpFile;
		}else{
			System.out.println("Log util Waring: Can't found log config file!");
			System.out.println("Log util Waring: System will be use default config: LogType just STDOUT!");
			return null;
		}
	}

	/**
	 * 获取启动时间信息
	 * @return 启动时间
     */
	 protected static long getStartTimeMillis() {
		return startTimeMillis;
	}

	/**
	 * 获取日志配置项信息
	 * @param property  日志配置项
	 * @param defalut   默认值
     * @return  日志配置信息
     */
	protected static String getLogConfig(String property,String defalut) {
		String value = null;
		if(configFile!=null){
			value = TProperties.getString(configFile, property);
		}
		return value==null?defalut:value;
	}
}
