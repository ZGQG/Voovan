package org.voovan.tools.json;

import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

import java.text.ParseException;

/**
 * JAVA 对象和 JSON 对象转换类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class JSON {
	
	/**
	 * 将 Java 对象 转换成 JSON字符串
	 * @param object   	待转换的对象
	 * @return			转换后的 JSON 字符串
	 */
	public static String toJSON(Object object){
		String jsonString = null;
		try {
			jsonString = JSONEncode.fromObject(object);
		} catch (ReflectiveOperationException e) {
			Logger.error("Reflective Operation failed.",e);
		}
		return jsonString;
	}
	
	/**
	 * 将 JSON字符串 转换成 Java 对象
	 * @param <T>			范型
	 * @param jsonStr		待转换的 JSON 字符串
	 * @param clazz			转换的目标 java 类
	 * @return				转换后的 Java 对象
	 */
	public static <T> T toObject(String jsonStr,Class<T> clazz){
		T valueObject = null;
		try {
			valueObject = JSONDecode.fromJSON(jsonStr, clazz);
		} catch (ReflectiveOperationException | ParseException e) {
			Logger.error("Reflective Operation failed.",e);
		}
		return valueObject;
	}
	
	
	/**
	 * 解析 JSON 字符串
	 * 		如果是{}包裹的字符串解析成 HashMap,如果是[]包裹的字符串解析成 ArrayList
	 * @param jsonStr	待解析的 JSON 字符串
	 * @return 接口后的对象
	 */
	public static Object parse(String jsonStr){
		Object parseObject = null;
		parseObject = JSONDecode.parse(jsonStr);
		return parseObject;
	}

	/**
	 * 格式化 JSON
	 * @param jsonStr JSON 字符串
	 * @return  格式化后的 JSON 字符串
	 */
	public static String formatJson(String jsonStr) {
		if (jsonStr == null || jsonStr.isEmpty()) return "";
		StringBuilder jsongStrBuild = new StringBuilder();
		char prevChar = '\0';
		char current = '\0';
		int indent = 0;
		boolean inStr = false;
		for (int i = 0; i < jsonStr.length(); i++) {
			prevChar = current;
			current = jsonStr.charAt(i);

			//判断是否在字符串中
			if(current == '\"' && prevChar!='\\'){
				inStr = !inStr;
			}

			if(inStr){
				jsongStrBuild.append(current);
				continue;
			}

			if(current=='[' || current=='{'){
				jsongStrBuild.append(current);
				jsongStrBuild.append('\n');
				indent++;
				addIndentByNum(jsongStrBuild, indent);
				continue;
			}

			if(current==']' || current=='}'){
				jsongStrBuild.append('\n');
				indent--;
				addIndentByNum(jsongStrBuild, indent);
				jsongStrBuild.append(current);
				continue;
			}

			if(current==','){
				jsongStrBuild.append(current);
				jsongStrBuild.append('\n');
				addIndentByNum(jsongStrBuild, indent);
				continue;
			}

			jsongStrBuild.append(current);

		}

		return jsongStrBuild.toString();
	}

	/**
	 * 添加缩进
	 * @param str     需要追加缩进的字符串
	 * @param indent  缩进后的字符串
	 */
	private static void addIndentByNum(StringBuilder  str, int indent) {
		for (int i = 0; i < indent; i++) {
			str.append('\t');
		}
	}

	/**
	 * 清理json字符串串null节点
	 * @param jsonStr json 字符串
	 * @return 清理null节点的结果
	 */
	public static String removeNullNode(String jsonStr){
		jsonStr	= jsonStr.replaceAll("\\\"\\w+?\\\":null","").replaceAll("null","");
		return fixJSON(jsonStr);
	}

	/**
	 * 修复 JSON 字符串中因清理节点导致的多个","的分割异常问题
	 * @param jsonStr json 字符串
	 * @return 清理后点的结果
	 */
	protected 	 static String fixJSON(String jsonStr){

		while(TString.searchByRegex(jsonStr,",[\\s\\r\\n]*,").length>0) {
			jsonStr = jsonStr.replaceAll(",[\\s\\r\\n]*,", ",");
		}

		jsonStr	= jsonStr.replaceAll("(?:[\\{])[\\s\\r\\n]*,","{");
		jsonStr	= jsonStr.replaceAll("(?:[\\[])[\\s\\r\\n]*,","[");
		jsonStr	= jsonStr.replaceAll(",[\\s\\r\\n]*(?:[\\}])","}");
		jsonStr	= jsonStr.replaceAll(",[\\s\\r\\n]*(?:[\\]])","]");

		return jsonStr;
	}
}
