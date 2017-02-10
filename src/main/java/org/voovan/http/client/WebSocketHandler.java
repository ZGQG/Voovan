package org.voovan.http.client;

import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.http.websocket.WebSocketTools;
import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.tools.TObject;

import java.nio.ByteBuffer;

/**
 * 类文字命名
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class WebSocketHandler implements IoHandler{

    private WebSocketRouter webSocketRouter;
    private HttpClient httpClient;
    public WebSocketHandler(HttpClient httpClient, WebSocketRouter webSocketRouter){
        this.webSocketRouter = webSocketRouter;
        this.httpClient = httpClient;
    }

    @Override
    public Object onConnect(IoSession session) {
        //不会被触发
        return null;
    }

    @Override
    public void onDisconnect(IoSession session) {
        webSocketRouter.onClose();
    }

    @Override
    public Object onReceive(IoSession session, Object obj) {

        //分片 (1) fin=0 , opcode=1
        //分片 (2) fin=0 , opcode=0
        //分片 (3) fin=1 , opcode=0

        WebSocketFrame respWebSocketFrame = null;
        WebSocketFrame reqWebSocketFrame = null;
        if(obj instanceof WebSocketFrame) {
            reqWebSocketFrame = TObject.cast(obj);
        }else{
            return null;
        }
        if (reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.CLOSING) {
            return WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.CLOSING, false, reqWebSocketFrame.getFrameData());
        }
        // WS_PING 收到 ping 帧则返回 pong 帧
        else if (reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.PING) {
            return WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.PONG, false, null);
        }
        // WS_PING 收到 pong 帧则返回 ping 帧
        else if (reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.PONG) {
            return WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.PING, false, null);
        }
        // WS_RECIVE 文本和二进制消息出发 Recived 事件
        else if (reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.TEXT || reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.BINARY) {

            ByteBuffer respData = webSocketRouter.onRecived(reqWebSocketFrame.getFrameData());

            //判断解包是否有错
            if (reqWebSocketFrame.getErrorCode() == 0) {
                respWebSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.BINARY, true, respData);
            } else {
                //解析时出现异常,返回关闭消息
                respWebSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.CLOSING, false, ByteBuffer.wrap(WebSocketTools.intToByteArray(reqWebSocketFrame.getErrorCode(), 2)));
            }
        }

        return respWebSocketFrame;
    }

    @Override
    public void onSent(IoSession session, Object obj) {
        webSocketRouter.setSession(session);
        WebSocketFrame webSocketFrame = WebSocketFrame.parse(TObject.cast(obj));
        if(webSocketFrame.getOpcode() == WebSocketFrame.Opcode.CLOSING){
            session.close();
            return;
        }
        ByteBuffer data = webSocketFrame.getFrameData();
        webSocketRouter.onSent(data);
    }

    @Override
    public void onException(IoSession session, Exception e) {

    }
}
