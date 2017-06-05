package org.voovan.network.messagesplitter;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Http 消息分割类
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpMessageSplitter implements MessageSplitter {

	private static final String	BODY_TAG	= "\r\n\r\n";
	private int result = -1;

	private int contentLength = -1;
    boolean isChunked = false;


    @Override
	public int canSplite(IoSession session, ByteBuffer byteBuffer) {

		if(byteBuffer.limit()==0){
			return -1;
		}

        if( "WebSocket".equals(session.getAttribute("Type")) ){
            result = isWebSocketFrame(byteBuffer);
            result = result==0 ? -1 : result;
        }else{
            result = isHttpFrame(byteBuffer);
            result = result==0 ? -1 : 0;
        }

        return result;
	}

    private int isHttpFrame(ByteBuffer byteBuffer){
        int bodyTagIndex = 0;
        byte[] buffer = TByteBuffer.toArray(byteBuffer);
        StringBuilder stringBuilder = new StringBuilder();
        String httpHead = null;
        for(int x=0;x<buffer.length-3;x++){
            if(buffer[x] == '\r' && buffer[x+1] == '\n' && buffer[x+2] == '\r' && buffer[x+3] == '\n'){
                bodyTagIndex = x + 3;
                httpHead = stringBuilder.toString();
                break;
            }else{
                stringBuilder.append((char)buffer[x]);
            }
        }

        if(httpHead !=null && isHttpHead(httpHead)) {

            String[] contentLengthLines = TString.searchByRegex(httpHead, "Content-Length: \\d+");
            if (contentLengthLines.length > 1) {
                contentLength = Integer.parseInt(contentLengthLines[0].split(" ")[1].trim());
            }

            isChunked = httpHead.contains("chunked");
            return bodyTagIndex;

        }else{
            return 0;
        }
    }

    private boolean isHttpHead(String str){
        //判断是否是 HTTP 头
        int firstLineIndex = str.indexOf("\r\n");
        if(firstLineIndex != -1) {
            String firstLine = str.substring(0, firstLineIndex);
            if (TString.regexMatch(firstLine, "HTTP\\/\\d\\.\\d\\s\\d{3}\\s.*") <= 0) {
                if (TString.regexMatch(firstLine, "^[A-Z]*\\s.*\\sHTTP\\/\\d\\.\\d") <= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 判断缓冲区中的数据是否是一个 WebSocket 帧
     * @param buffer 缓冲区对象
     * @return WebSocket 帧报文长度,-1不是WebSocket 帧, 大于0 返回的 WebSocket 的长度
     */
    public static int isWebSocketFrame(ByteBuffer buffer) {
        // 接受数据的大小
        int maxpacketsize = buffer.remaining();
        // 期望数据包的实际大小
        int expectPackagesize = 2;
        if (maxpacketsize < expectPackagesize) {
            return -1;
        }
        byte finByte = buffer.get();
        boolean fin = finByte >> 8 != 0;
        byte rsv = (byte) ((finByte & ~(byte) 128) >> 4);
        if (rsv != 0) {
            return -1;
        }
        byte maskByte = buffer.get();
        boolean mask = (maskByte & -128) != 0;
        int payloadlength = (byte) (maskByte & ~(byte) 128);
        int optcode = (byte) (finByte & 15);

        if (!fin) {
            if (optcode == 9 || optcode == 10 || optcode == 8) {
                return -1;
            }
        }

        if (payloadlength >= 0 && payloadlength <= 125) {
        } else {
            if (optcode == 9 || optcode == 10 || optcode == 8) {
                return -1;
            }
            if (payloadlength == 126) {
                expectPackagesize += 2;
                byte[] sizebytes = new byte[3];
                sizebytes[1] = buffer.get();
                sizebytes[2] = buffer.get();
                payloadlength = new BigInteger(sizebytes).intValue();
            } else {
                expectPackagesize += 8;
                byte[] bytes = new byte[8];
                for (int i = 0; i < 8; i++) {
                    bytes[i] = buffer.get();
                }
                long length = new BigInteger(bytes).longValue();
                if (length <= Integer.MAX_VALUE) {
                    payloadlength = (int) length;
                }
            }
        }

        expectPackagesize += (mask ? 4 : 0);
        expectPackagesize += payloadlength;

        // 如果实际接受的数据小于数据包的大小则报错
        if (maxpacketsize < expectPackagesize) {
            buffer.position(0);
            return -1;
        } else {
            buffer.position(0);
            return expectPackagesize;
        }
    }

}
