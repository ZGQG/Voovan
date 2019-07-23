package org.voovan.tools.security;

import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * hash 算法类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class THash {

	/**
	 * BASE64解密
	 * 
	 * @param key 待解密字符串
	 * @return 解密后字符串
	 */
	public static String decryptBASE64(String key) {
		return  new String(TBase64.decode(key));
	}

	/**
	 * BASE64加密
	 *
	 * @param key 待加密字符串
	 * @return 加密后字符串
	 */
	public static String encryptBASE64(String key) {
		return TBase64.encode(key.getBytes());
	}

	/**
	 * MD5加密
	 * @param str 待加密字符串
	 * @return 加密结果
     */
	public static String encryptMD5(String str){
		return digest("MD5", str);
	}

	/**
	 * SHA 加密
	 * @param str 待加密字符串
	 * @return 加密结果
     */
	public static String encryptSHA(String str){
		return digest("SHA", str);
	}

	public static String digest(String code,String str) {

        try {
			//code 可选 hash 算法
            //MD2
            //MD5
            //SHA-1/SHA
            //SHA-256
            //SHA-384
            //SHA-512
			MessageDigest messageDigest = MessageDigest.getInstance(code);
  
            messageDigest.reset();  
  
            messageDigest.update(str.getBytes());

			byte[] byteArray = messageDigest.digest();

			StringBuffer md5StrBuff = new StringBuffer();

			for (int i = 0; i < byteArray.length; i++) {
				if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)
					md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));
				else
					md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
			}

			return md5StrBuff.toString();

		} catch (NoSuchAlgorithmException e) {
        	Logger.error("No such algorithm",e);
            return null;
        } 

    }

	/**
	 * Time31算法
	 * @param source  字节数据
	 * @param offset 字节数据偏移量
	 * @param length 长度* @param source 待加密字符串
	 * @return 加密结果
     */
	public static int hashTime31(byte[] source, int offset, int length) {
		int hash = 0;
		for (int i = offset; i < length; i++) {
			hash = ((hash << 5) - hash) + source[i];
		}
		return hash;
	}




	/**
	 * Time31算法
	 * @param byteBuffer  字节数据
	 * @param offset 字节数据偏移量
	 * @param length 长度
	 * @return 加密结果
	 */
	public static int hashTime31(ByteBuffer byteBuffer, int offset, int length) {
		int hash = 0;
		for (int i = offset; i < length; i++) {
			hash = ((hash << 5) - hash) + byteBuffer.get(i);
		}
		return hash;
	}



	/**
	 * Time31算法
	 * @param str 字符串
	 * @param offset 字节数据偏移量
	 * @param length 长度
	 * @param seed 上次 hash 的种子
	 * @return 加密结果
	 */
	public static int hashTime31(String str, int offset, int length, int seed) {
		int hash = seed;
		for (int i = offset; i < length; i++) {
			hash = ((hash << 5) - hash) + str.charAt(i);
		}
		return hash;
	}

	/**
	 * Time31算法
	 * @param str 字符串
	 * @param offset 字节数据偏移量
	 * @param length 长度* @param source 待加密字符串
	 * @return 加密结果
	 */
	public static int hashTime31(String str, int offset, int length) {

		return hashTime31(str, offset, length, 0);
	}


	/**
	 * Time31算法
	 * @param str 字符串
	 * @return 加密结果
	 */
	public static int hashTime31(String str) {

		return hashTime31(str, 0, str.length(), 0);
	}

    /**
     * Time31算法
     * @param strs 字符串数组
     * @return 加密结果
     */
	public static int hashTime31(String ... strs) {
		int hash = 0;
		for(int i=0;i<strs.length;i++){
			String val = strs[i];
			if(val !=null){
				hash = hash + hashTime31(val, 0, val.length(), hash);
			}
		}

		return hash;
	}

	/**
	 * 改进的32位FNV算法1
	 * @param data 数组
	 * @param offset 数据偏移量
	 * @param length 长度
	 * @return int值
	 */
	public static int HashFNV1(byte[] data, int offset, int length)
	{
		final int p = 16777619;
		int hash = (int)2166136261L;
		for (int i = offset; i < length; i++) {
			byte b = data[i];
			hash = (hash ^ b) * p;
		}

		hash += hash << 13;
		hash ^= hash >> 7;
		hash += hash << 3;
		hash ^= hash >> 17;
		hash += hash << 5;
		return hash;

	}

	/**
	 * 改进的32位FNV算法1
	 * @param byteBuffer  字节数据
	 * @param offset 字节数据偏移量
	 * @param length 长度
	 * @return int值
	 */
	public static int HashFNV1(ByteBuffer byteBuffer, int offset, int length)
	{
		final int p = 16777619;
		int hash = (int)2166136261L;
		for (int i = offset; i < length; i++) {
			byte b = byteBuffer.get(i);
			hash = (hash ^ b) * p;
		}

		hash += hash << 13;
		hash ^= hash >> 7;
		hash += hash << 3;
		hash ^= hash >> 17;
		hash += hash << 5;
		return hash;
	}

	/**
	 * 改进的32位FNV算法1
	 * @param str 字符串
	 * @param offset 字节数据偏移量
	 * @param length 长度
	 * @param seed 上次 hash 的种子
	 * @return int值
	 */
	public static int HashFNV1(String str, int offset, int length, int seed)
	{
		final int p = 16777619;
		int hash = seed;
		for (int i = offset; i < length; i++) {
			byte b = (byte)str.charAt(i);
			hash = (hash ^ b) * p;
		}

		hash += hash << 13;
		hash ^= hash >> 7;
		hash += hash << 3;
		hash ^= hash >> 17;
		hash += hash << 5;
		return hash;
	}

	/**
	 * 改进的32位FNV算法1
	 * @param str 字符串
	 * @param offset 字节数据偏移量
	 * @param length 长度
	 * @return int值
	 */
	public static int HashFNV1(String str, int offset, int length) {
		return HashFNV1(str, offset, length, (int)2166136261L);
	}


	/**
	 * 改进的32位FNV算法1
	 * @param str 字符串
	 * @return int值
	 */
	public static int HashFNV1(String str) {
		return HashFNV1(str, 0, str.length(), (int)2166136261L);
	}

	/**
	 * Time31算法
	 * @param strs 字符串数组
	 * @return 加密结果
	 */
	public static int HashFNV1(String ... strs) {
		int hash = (int)2166136261L;
		for(int i=0;i<strs.length;i++){
			String val = strs[i];
			if(val !=null){
				hash = hash + HashFNV1(val, 0, val.length(), hash);
			}
		}

		return hash;
	}
}