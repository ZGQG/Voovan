package org.voovan.tools.json;

import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.*;

/**
 * JSON字符串分析成 Map
 * 
 * @author helyho
 * 
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class JSONDecode {
	private static int E_OBJECT = 1;
	private static int E_ARRAY = -1;

	public static Object parse(String jsonStr) {
		return parse(new StringReader(jsonStr.trim()+"\0"));
	}

	/**
	 * 解析 JSON 字符串
	 * 		如果是{}包裹的对象解析成 HashMap,如果是[]包裹的对象解析成 ArrayList
	 * @param reader	待解析的 JSON 字符串
	 * @return 解析后的对象
	 */
	public static Object parse(StringReader reader) {
		try {

			if (reader == null) {
				return null;
			}

			int type = 0;
			Object jsonResult = null;
			boolean isFirstChar = true;

			//根据起始和结束符号,决定返回的对象类型
			if (type == 0) {
				char flag = (char) reader.read();

				if (flag == '{') {
					type = E_OBJECT;
				}

				if (flag == '[') {
					type = E_ARRAY;
				}
			}

			//对象类型构造返回的对象
			if (E_OBJECT == type) {
				jsonResult = (Map) new HashMap<String, Object>();
				isFirstChar = false;
			} else if (E_ARRAY == type) {
				jsonResult = (List) new ArrayList<Object>();
				isFirstChar = false;
			} else {
				reader.skip(-1);
			}

			String keyString = null;
			Object value = null;
			int stringWarpFlag = 0;
			int functionWarpFlag = 0;
			boolean isString = false;
			boolean isFunction = false;
			int isComment = 0;
			StringBuilder itemString = new StringBuilder();

			char currentChar = 0;
			char nextChar = 0;
			char prevChar = 0;

			while (true) {
				currentChar = (char) reader.read();

				nextChar = (char) reader.read();
				if (nextChar != 65535) {
					reader.skip(-1);
				}

				if (!isFirstChar) {
					reader.skip(-2);
					prevChar = (char) reader.read();
					reader.skip(1);
				}

				isFirstChar = false;

				//分析字符串,如果是字符串不作任何处理
				if (currentChar == '"') {
					//i小于1的不是转意字符,判断为字符串(因为转意字符要2个字节),大于2的要判断是否\\"的转义字符
					if (nextChar != 0 && prevChar != '\\') {
						stringWarpFlag++;
						//字符串起始的"
						if (stringWarpFlag == 1) {
							isString = true;
						}
						//字符串结束的"
						else if (stringWarpFlag == 2) {
							stringWarpFlag = 0;
							isString = false;
						}
					}
				}

				//处理注释
				if (!isString) {

					if (currentChar == '/' && (nextChar != 0 && nextChar == '/') && isComment == 0) {
						isComment = 1; //单行注释
					}

					if (isComment == 1 && currentChar == '\n' && isComment > 0) {
						isComment = 0; //单行注释
					}

					if (currentChar == '/' && (nextChar != 0 && nextChar == '*') && isComment == 0) {
						isComment = 2; //多行注释
						continue;
					}

					if (isComment == 2 && currentChar == '/' && (prevChar != 0 && prevChar == '*') && isComment > 0) {
						isComment = 0; //多行注释
						continue;
					}

					if (isComment != 0) {
						continue;
					}
				}

				//JSON数组字符串分组,以符号对称的方式取 []
				if (!isString && !isFunction && currentChar == '[') {
					reader.skip(-1);
					//递归解析处理,取 value 对象
					value = JSONDecode.parse(reader);
					continue;
				} else if (!isString && !isFunction && currentChar == ']') {
					//最后一个元素,追加一个,号来将其附加到结果集
					if (itemString.length() != 0 || value != null) {
						currentChar = ',';
						reader.skip(-1);
					} else {
						return jsonResult;
					}
				}

				//JSON对象字符串分组,以符号对称的方式取 {}
				else if (!isString && !isFunction && currentChar == '{') {
					reader.skip(-1);
					//递归解析处理,取 value 对象
					value = JSONDecode.parse(reader);
					continue;
				} else if (!isString && !isFunction && currentChar == '}') {
					//最后一个元素,追加一个,号来将其附加到结果集
					if (itemString.length() != 0 || value != null) {
						currentChar = ',';
						reader.skip(-1);
					} else {
						return jsonResult;
					}
				}

				//如果为字符串则无条件瓶装
				//如果不是字符串,则只拼装可见字符
				if (isString || (!isString && !Character.isWhitespace(currentChar))) {
					itemString.append(currentChar);
				}

				if (jsonResult == null) {
					jsonResult = value;
				}

				//如果是函数 function 起始
				if (!isString && itemString.toString().trim().startsWith("function")) {

					if (currentChar == '{') {
						functionWarpFlag++;
					} else if (currentChar == '}') {
						functionWarpFlag--;

						if (functionWarpFlag == 0) {
							isFunction = false;
							value = itemString.toString();
							itemString = new StringBuilder();
						}
					} else {
						isFunction = true;
					}
				}

				//JSON对象字符串分组,取 Key 对象,当前字符是:则取 Key
				if (!isString && !isFunction && currentChar == ':') {
					keyString = itemString.substring(0, itemString.length() - 1).trim();
					itemString = new StringBuilder();
				}

				//JSON对象字符串分组,取 value 对象,当前字符是,则取 value
				if (!isString && !isFunction && currentChar == ',') {
					if (value == null) {
						value = itemString.substring(0, itemString.length() - 1).trim();
					}
					itemString = new StringBuilder();
				}

				//返回值处理
				if (value != null && jsonResult != null) {
					//判断取值不是任何对象
					if (value instanceof String) {
						String stringValue = TObject.cast(value);

						//判断是字符串去掉头尾的冒号
						if (stringValue.startsWith("\"") && stringValue.endsWith("\"")) {
							value = stringValue.substring(1, stringValue.length() - 1);
							value = value.toString().replace("\\u000a", "\n").replace("\\u000d", "\r").replace("\\u0022", "\"");
						}
						//判断不包含.即为整形
						else if (TString.isInteger(stringValue)) {
							Long longValue = Long.parseLong((String) value);
							if (longValue <= 2147483647 && longValue >= -2147483647) {
								value = Integer.parseInt((String) value);
							} else {
								value = longValue;
							}
						}
						//判断有一个.即为浮点数,转换成 Float
						else if (TString.isFloat(stringValue)) {
							value = new Float((String) value);
						}
						//判断是否是 boolean 类型
						else if (TString.isBoolean(stringValue)) {
							value = Boolean.parseBoolean((String) value);
						} else if (value.equals("null")) {
							value = null;
						}
					}

					//这里 key 和 value 都准备完成了

					//判断返回对象的类型,填充返回对象
					if (jsonResult instanceof HashMap) {
						@SuppressWarnings("unchecked")
						HashMap<String, Object> result = (HashMap<String, Object>) jsonResult;
						if (keyString != null) {
							//容错,如果是双引号包裹的则去除首尾的双引号
							if (keyString.startsWith("\"") && keyString.endsWith("\"")) {
								keyString = keyString.substring(1, keyString.length() - 1);
							}
							result.put(keyString, value);
						}
					} else if (jsonResult instanceof ArrayList && value != null) {
						@SuppressWarnings("unchecked")
						ArrayList<Object> result = (ArrayList<Object>) jsonResult;
						result.add(value);
					} else {
						jsonResult = value;
					}
					//处理完侯将 value 放空
					keyString = null;
					value = null;
				}

				if (currentChar == 65535) {
					break;
				}
			}
			return jsonResult;
		}catch(Exception e){
			try {
				int position = ((int) TReflect.getFieldValue(reader,"next") -1);
				String jsonStr = (String) TReflect.getFieldValue(reader,"str");
				jsonStr = jsonStr.substring(0, position)+"^"+jsonStr.substring(position, position+10);
				Logger.error(jsonStr, e);
			} catch (ReflectiveOperationException ex) {
				Logger.error(ex);
			}

			return null;

		}
	}
	
	/**
	 * 解析 JSON 字符串成为参数指定的类
	 * @param <T> 		范型
	 * @param jsonStr	JSON字符串
	 * @param clazz		JSON 字符串将要转换的目标类
	 * @return					JSON 转换后的 Java 对象
	 * @throws ReflectiveOperationException  反射异常
	 * @throws ParseException 解析异常
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T>T fromJSON(String jsonStr,Class<T> clazz, boolean ignoreCase) throws ReflectiveOperationException, ParseException {
		if(jsonStr==null){
			return null;
		}

		Object parseObject = parse(jsonStr);
		//{}包裹的对象处理
		if(parseObject instanceof Map){
			Map<String,Object> mapJSON = (Map<String, Object>) parseObject;
			return (T) TReflect.getObjectFromMap(clazz, mapJSON,ignoreCase);
		}
		//[]包裹的对象处理
		else if(parseObject instanceof Collection){
			return (T) TReflect.getObjectFromMap(clazz, TObject.newMap("value",parseObject),false);
		}
		//其他类型处理
		else{
			return null;
		}
	}

	/**
	 * 解析 JSON 字符串成为参数指定的类,默认严格限制字段大小写
	 * @param <T> 		范型
	 * @param jsonStr	JSON字符串
	 * @param clazz		JSON 字符串将要转换的目标类
	 * @param clazz			转换的目标 java 类
	 * @return					JSON 转换后的 Java 对象
	 * @throws ReflectiveOperationException  反射异常
	 * @throws ParseException 解析异常
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T>T fromJSON(String jsonStr,Class<T> clazz) throws ParseException, ReflectiveOperationException, IOException {
		return fromJSON(jsonStr, clazz, false);
	}
}
