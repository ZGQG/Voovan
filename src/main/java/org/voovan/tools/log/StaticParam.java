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

	public static File loadConfig(){
		File tmpFile = TFile.getResourceFile("logger.properties");
		if(tmpFile!=null){
			return tmpFile;
		}else{
			System.out.println("Log util Waring: Can't found log config file!");
			System.out.println("Log util Waring: System will be use default config: LogType just STDOUT!");
			return null;
		}
	}
	
	public static long getStartTimeMillis() {
		return startTimeMillis;
	}
	
	public static String getLogConfig(String property,String defalut) {
		String value = null;
		if(configFile!=null){
			value = TProperties.getString(configFile, property);
		}
		return value==null?defalut:value;
	}
}
