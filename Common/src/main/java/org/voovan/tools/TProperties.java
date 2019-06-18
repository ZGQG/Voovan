package org.voovan.tools;

import org.voovan.Global;
import org.voovan.tools.hashwheeltimer.HashWheelTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * properties文件操作类
 * 		当properties 文件变更后自动移除缓存内的数据, 下次访问时会重新读取文件内容
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TProperties {

	private static ConcurrentHashMap<File, Properties> propertiesCache = new ConcurrentHashMap<File, Properties>();
	private static ConcurrentHashMap<String, File> propertiesFile = new ConcurrentHashMap<String, File>();
	private static String TIME_STAMP_NAME = "$$LMT";

	static {
		Global.getHashWheelTimer().addTask(new HashWheelTask() {
			@Override
			public void run() {
				Iterator<Map.Entry<File, Properties>> iterator = propertiesCache.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<File, Properties> entry = iterator.next();
					if(entry.getKey().exists() && entry.getValue().containsKey(TIME_STAMP_NAME)) {
						String lastTimeStamp = String.valueOf(entry.getKey().lastModified());
						String cachedTimeStamp = entry.getValue().getProperty(TIME_STAMP_NAME);
						if (!lastTimeStamp.equals(cachedTimeStamp)) {
							iterator.remove();
						}
					}
				}
			}

		}, 5, true);
	}


	/**
	 * 解析 Properties 文件
	 *
	 * @param file 文件对象
	 * @return Properties 对象
	 */
	public static Properties getProperties(File file) {
		try {
			if (!propertiesCache.containsKey(file)) {
				Properties properites = new Properties();
				String content = null;
				if(!file.getPath().contains("!"+File.separator)) {
					content = new String(TFile.loadFile(file));
				}else{
					String filePath = file.getPath();
					String resourcePath = filePath.substring(filePath.indexOf("!"+File.separator)+2, filePath.length());
					content = new String(TFile.loadResource(resourcePath));
				}
				properites.load(new StringReader(content));
				properites.setProperty(TIME_STAMP_NAME, String.valueOf(file.lastModified()));
				propertiesCache.put(file, properites);
				System.out.println("[PROPERTIES] Load Properties file: " + file.getPath());
			}

			return propertiesCache.get(file);

		} catch (IOException e) {
			System.out.println("Get properites file failed. File:" + file.getAbsolutePath() + "-->" + e.getMessage());
			return null;
		}
	}

	/**
	 * 解析 Properties 文件
	 *
	 * @param fileName 文件名, 不包含扩展名, 或自动瓶装环境参数和扩展名
	 *                 传入 database 参数会拼装出 database-环境名.properties 作为文件名
	 *                 并且在 classes 或者 target/classes 目录下寻找指定文件.
	 *                 如果没有指定环境名的配置文件则使用默认的配置文件
	 * @return Properties 对象
	 */
	public static Properties getProperties(String fileName) {
		File file = null;

		if(!propertiesFile.containsKey(fileName)) {

			String configFileNameWithEnv = null;
			String configFileName = "";
			if (!fileName.contains(".properties")) {

				String envName = TEnv.getEnvName();
				envName = envName == null ? "" : "-" + envName;
				configFileNameWithEnv = fileName + envName + ".properties";
				configFileName = fileName + ".properties";
			}

			File configFile = TFile.getResourceFile(configFileName);
			File configFileWithEnv = TFile.getResourceFile(configFileNameWithEnv);

			if (configFileWithEnv != null) {
				file = configFileWithEnv;
			} else if (configFile != null) {
				file = configFile;
			}

			propertiesFile.put(fileName, file);

		} else {
			file = propertiesFile.get(fileName);
		}

		if(file!=null) {
			return getProperties(file);
		} else {
			System.out.println("Get properites file failed. File:" + file.getName());
			return null;
		}
	}

	/**
	 * 从Properties文件读取字符串
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static String getString(File file, String name, String defaultValue) {
		Properties properites = getProperties(file);
		String value = properites.getProperty(name);
		return TString.isNullOrEmpty(value) ? defaultValue: value;
	}

	/**
	 * 从Properties文件读取字符串
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static String getString(File file, String name) {
		return getString(file, name, null);
	}

	/**
	 * 从Properties文件读取整形
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static int getInt(File file, String name, Integer defaultValue) {
		defaultValue = defaultValue == null ? 0 : defaultValue;
		String value = getString(file, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Integer.valueOf(value.trim());
	}

	/**
	 * 从Properties文件读取整形
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static int getInt(File file, String name) {
		return getInt(file, name, null);
	}

	/**
	 * 从Properties文件读取浮点数
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static float getFloat(File file, String name, Float defaultValue) {
		defaultValue = defaultValue == null ? 0f : defaultValue;
		String value = getString(file, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Float.valueOf(value.trim());
	}

	/**
	 * 从Properties文件读取浮点数
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static float getFloat(File file, String name) {
		return getFloat(file, name, null);
	}

	/**
	 * 从Properties读取双精度浮点数
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static double getDouble(File file, String name, Double defaultValue) {
		defaultValue = defaultValue == null ? 0d : defaultValue;
		String value = getString(file, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Double.valueOf(value.trim());
	}

	/**
	 * 从Properties读取双精度浮点数
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static double getDouble(File file, String name) {
	    return getDouble(file, name, null);
	}

	/**
	 * 从Properties文件读取 Boolean
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static boolean getBoolean(File file, String name, Boolean defaultValue) {
		defaultValue = defaultValue == null ? false : defaultValue;
		String value = getString(file, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Boolean.valueOf(value.trim());
	}

	/**
	 * 从Properties读取双精度浮点数
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static boolean defaultValue(File file, String name) {
		return getBoolean(file, name, null);
	}

	/**
	 * 保存信息到 Properties文件
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @param value 属性值
	 * @throws IOException IO异常
	 */
	public static void setString(File file, String name, String value) throws IOException {
		Properties properites = getProperties(file);
		properites.setProperty(name, value);
		properites.store(new FileOutputStream(file), null);
	}

	//-----------------------------------------------------------------------------


	/**
	 * 从Properties文件读取字符串
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static String getString(String fileName, String name, String defaultValue) {
		Properties properites = getProperties(fileName);
		String value = properites.getProperty(name);
		return TString.isNullOrEmpty(value) ? defaultValue : value;
	}

	/**
	 * 从Properties文件读取字符串
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static String getString(String fileName, String name) {
		return getString(fileName, name, null);
	}

	/**
	 * 从Properties文件读取整形
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static int getInt(String fileName, String name, Integer defaultValue) {
		defaultValue = defaultValue == null ? 0 : defaultValue;
		String value = getString(fileName, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Integer.valueOf(value.trim());
	}

	/**
	 * 从Properties文件读取整形
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static int getInt(String fileName, String name) {
	    return getInt(fileName, name, null);
	}

	/**
	 * 从Properties文件读取浮点数
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static float getFloat(String fileName, String name, Float defaultValue) {
		defaultValue = defaultValue == null ? 0f : defaultValue;
		String value = getString(fileName, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Float.valueOf(value.trim());
	}

	/**
	 * 从Properties文件读取浮点数
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static float getFloat(String fileName, String name) {
		return getFloat(fileName, name, null);
	}

	/**
	 * 从Properties读取双精度浮点数
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static double getDouble(String fileName, String name, Double defaultValue) {
		defaultValue = defaultValue == null ? 0d : defaultValue;
		String value = getString(fileName, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Double.valueOf(value.trim());
	}

	/**
	 * 从Properties读取双精度浮点数
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static double getDouble(String fileName, String name) {
	    return getDouble(fileName, name, null);
	}


	/**
	 * 从Properties文件读取 Boolean
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static boolean getBoolean(String fileName, String name, Boolean defaultValue) {
		defaultValue = defaultValue == null ? false : defaultValue;
		String value = getString(fileName, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Boolean.valueOf(value.trim());
	}

	/**
	 * 从Properties文件读取 Boolean
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static boolean getBoolean(String fileName, String name) {
		return getBoolean(fileName, name, null);
	}

	/**
	 * 清空 指定文件的 Properites 缓存
	 * @param fileName 文件名, 可以是完整文件名,也可以是不带扩展名的文件名
	 */
	public static void clear(String fileName){
		Iterator<File> iterator = propertiesCache.keySet().iterator();
		while(iterator.hasNext()){
			File file = iterator.next();
			if (file.getName().startsWith(fileName)){
				iterator.remove();
			}
		}
	}

	/**
	 * 清空 Properites 缓存
	 */
	public void clear(){
		propertiesCache.clear();
	}
}
