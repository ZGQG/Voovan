package org.voovan.network.messagesplitter;

import org.voovan.Global;
import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.tools.TByteBuffer;

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
    private static final String HTTP_PROTCOL = "HTTP";
    private int result = -1;

//    private int contentLength = -1;
//    boolean isChunked = false;


    @Override
    public int canSplite(IoSession session, ByteBuffer byteBuffer) {
        //返回 -1: 报文未完整接受, -2: 报文异常, 管理连接

        if(byteBuffer.limit()==0){
            return -1;
        }

        if( "WebSocket".equals(session.getAttribute(0x1111)) ){
            result = isWebSocketFrame(byteBuffer);
        }else{
            result = isHttpFrame(byteBuffer);

            if(result>0){
                result = 0;
            } else if(result == -1){
                return result;
            } else if(result == -2){
//                采用异步的方式, 防止导致死锁
                Global.getThreadPool().execute(new Thread("CHECK_HTTP_HEAD_FAILED") {
                    @Override
                    public void run() {
                        session.close();
                    }
                });
            }
        }

        return result;
    }

    private int isHttpFrame(ByteBuffer byteBuffer){
        int bodyTagIndex = -1;
        int protocolLineIndex = -1;
        StringBuilder stringBuilder = new StringBuilder();
        String protocolLine = null;

        bodyTagIndex = TByteBuffer.revIndexOf(byteBuffer, "\r\n\r\n".getBytes());
        protocolLineIndex = TByteBuffer.indexOf(byteBuffer, "\r\n".getBytes());

        byte[] protocolLineBytes = new byte[protocolLineIndex-4];
        byteBuffer.get(protocolLineBytes);
        byteBuffer.position(0);

        protocolLine = new String(protocolLineBytes);

        if(bodyTagIndex > 0 && protocolLine !=null && isHttpHead(protocolLine)) {
            if(bodyTagIndex > 0) {
                return bodyTagIndex;
            } else {
                return -1;
            }

        }else{
            return -2;
        }
    }

    private boolean isHttpHead(String str){
        //判断是否是 HTTP 头
        if (str.startsWith(HTTP_PROTCOL) || str.endsWith(HTTP_PROTCOL)) {
            return true;
        }
        return false;
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
            return -2;
        }
        byte finByte = buffer.get();
        boolean fin = finByte >> 8 != 0;
        byte rsv = (byte) ((finByte & ~(byte) 128) >> 4);
        if (rsv != 0) {
            return -2;
        }
        byte maskByte = buffer.get();
        boolean mask = (maskByte & -128) != 0;
        int payloadlength = (byte) (maskByte & ~(byte) 128);
        int optcode = (byte) (finByte & 15);

        if(optcode < 0 && optcode > 10){
            return -2;
        }

        if (!fin) {
            if (optcode == 9 || optcode == 10 || optcode == 8) {
                return -2;
            }
        }

        if (payloadlength >= 0 && payloadlength <= 125) {
        } else {
            if (optcode == 9 || optcode == 10 || optcode == 8) {
                return -2;
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
            return -2;
        } else {
            buffer.position(0);
            return expectPackagesize;
        }
    }
}
