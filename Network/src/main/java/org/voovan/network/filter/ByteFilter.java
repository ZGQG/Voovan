package org.voovan.network.filter;

import org.voovan.network.IoFilter;
import org.voovan.network.IoSession;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;

/**
 * Byte数据过滤器
 *      encode 传入为 byte[]
 *      decode 返回为 byte[]
 *      -128 + -128+4位为数据长度+ -128 +数据
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ByteFilter implements IoFilter {
	public static Class BYTE_ARRAY_CLASS = (new byte[0]).getClass();
	public static byte SPLITER = -128;
	public static int HEAD_LEGNTH = 7;

	@Override
	public Object encode(IoSession session, Object object) {
		if(object.getClass() == BYTE_ARRAY_CLASS){

			byte[] data = (byte[])object;
			ByteBuffer byteBuffer = ByteBuffer.allocate(HEAD_LEGNTH + data.length);
			byteBuffer.put(SPLITER);
			byteBuffer.put(SPLITER);
			byteBuffer.putInt(data.length);
			byteBuffer.put(SPLITER);
			byteBuffer.put(data);
			byteBuffer.flip();
			return byteBuffer;
		}
		return null;
	}

	@Override
	public Object decode(IoSession session,Object object) {
		if(object instanceof ByteBuffer){
			boolean success = false;

			ByteBuffer byteBuffer = (ByteBuffer) object;
			int originPosition = byteBuffer.position();
			try {
				if(byteBuffer.remaining() < HEAD_LEGNTH){
					Logger.error("ByteFilter decode error: Not enough data length, socket will be close");
					return null;
				}

				if (byteBuffer.get() == SPLITER && byteBuffer.get() == SPLITER) {
					int length = byteBuffer.getInt();

					if (byteBuffer.get() == SPLITER) {
						if (length > 0) {
							byte[] data = new byte[length];
							byteBuffer.get(data);
							success = true;
							return data;
						}
					} else {
						Logger.error("ByteFilter decode error: payloadLength end not exists, socket will be close");
						session.close();
					}
				} else {
					Logger.error("ByteFilter decode error: payloadLength head not exists, socket will be close");
					session.close();
				}
			} catch(Exception e){
				e.printStackTrace();
				session.close();
			} finally {
				if(!success){
					byteBuffer.position(originPosition);
				}
			}
		}
		return null;
	}
}
