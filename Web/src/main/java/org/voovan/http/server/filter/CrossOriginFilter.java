package org.voovan.http.server.filter;

import org.voovan.http.server.HttpFilter;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.context.HttpFilterConfig;
import org.voovan.tools.TString;

/**
 * 支持跨域的过滤方式(JSONP 或者 跨域请求的方式)
 * 过滤器参数
 *      functionParamName: jsonp 的方法名称
 *      allowOrigin: 跨域访问允许的域名
 *      allowMethods: 跨域访问允许的方法
 *
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class CrossOriginFilter implements HttpFilter{

    @Override
    public Object onRequest(HttpFilterConfig filterConfig, HttpRequest request, HttpResponse response, Object prevFilterResult) {
        return true;
    }

    @Override
    public Object onResponse(HttpFilterConfig filterConfig, HttpRequest request, HttpResponse response, Object prevFilterResult) {

        //跨域请求头配置
        if(filterConfig.getParameters().containsKey("allowOrigin")) {
            String allowOrigin = filterConfig.getParameter("allowOrigin").toString();

            if("*".equals(allowOrigin)){
                allowOrigin = request.header().get("Origin");
            }

            response.header().put("Access-Control-Allow-Origin", allowOrigin);

            response.header().put("Access-Control-Allow-Methods", (String) filterConfig.getParameter("allowMethods"));
            response.header().put("Access-Control-Allow-Headers", (String) filterConfig.getParameter("allowHeaders"));
            response.header().put("Access-Control-Allow-Credentials", "true");
        }

        //JSONP 形式的跨域配置
        if(filterConfig.getParameters().containsKey("functionParamName")) {
            String functionParamName = (String) filterConfig.getParameter("functionParamName");
            String functionName = request.getParameter(functionParamName);
            if (functionName != null) {
                String jsonpResponse = TString.assembly(functionName, "(", response.body().getBodyString(), ")");
                response.clear();
                response.body().write(jsonpResponse.getBytes());
            }
        }
        return true;
    }
}
