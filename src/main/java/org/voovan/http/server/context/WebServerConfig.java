package org.voovan.http.server.context;

import org.voovan.tools.Chain;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

/**
 * WebServer 配置类
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebServerConfig {
    private String host             = "0.0.0.0";
    private int port                = 28080;
    private int timeout             = 30;
    private String contextPath      = "WEBAPP";
    private boolean MatchRouteIgnoreCase = false;
    private String characterSet     = "UTF-8";
    private String sessionContainer = "java.util.concurrent.ConcurrentHashMap";
    private int sessionTimeout      = 30;
    private int keepAliveTimeout    = 60;
    private boolean accessLog       = true;
    private boolean gzip            = true;
    private HttpsConfig https;
    private String indexFiles = "index.htm,index.html,default.htm,default.htm";

    private Chain<HttpFilterConfig> filterConfigs = new Chain<HttpFilterConfig>();
    private List<HttpRouterConfig> routerConfigs = new Vector<HttpRouterConfig>();
    private List<HttpModuleConfig> moduleConfigs = new Vector<HttpModuleConfig>();

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public boolean isMatchRouteIgnoreCase() {
        return MatchRouteIgnoreCase;
    }

    public void setMatchRouteIgnoreCase(boolean matchRouteIgnoreCase) {
        MatchRouteIgnoreCase = matchRouteIgnoreCase;
    }

    public void setCharacterSet(String characterSet) {
        this.characterSet = characterSet;
    }

    public void setSessionContainer(String sessionContainer) {
        this.sessionContainer = sessionContainer;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public void setKeepAliveTimeout(int keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getCharacterSet() {
        return characterSet;
    }

    public String getSessionContainer() {
        return sessionContainer;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public int getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public boolean isGzip() {
        return gzip;
    }

    public void setGzip(boolean gzip) {
        this.gzip = gzip;
    }

    public boolean isAccessLog() {
        return accessLog;
    }

    public void setAccessLog(boolean accessLog) {
        this.accessLog = accessLog;
    }

    public HttpsConfig getHttps() {
        return https;
    }

    public boolean isHttps(){
        return https!=null?true:false;
    }

    public void setHttps(HttpsConfig https) {
        this.https = https;
    }

    public String[] getIndexFiles() {
        return indexFiles.split(",");
    }

    public void setIndexFiles(String indexFiles) {
        this.indexFiles = indexFiles;
    }

    public Chain<HttpFilterConfig> getFilterConfigs() {
        return filterConfigs;
    }

    public List<HttpRouterConfig> getRouterConfigs() {
        return routerConfigs;
    }

    public List<HttpModuleConfig> getModuleonfigs() {
        return moduleConfigs;
    }
    /**
     * 使用列表初始话过滤器链
     *
     * @param filterInfoList 过滤器信息列表
     */
    public void addFilterByList(List<Map<String, Object>> filterInfoList) {
        for (Map<String, Object> filterConfigMap : filterInfoList) {
            HttpFilterConfig httpFilterConfig = new HttpFilterConfig(filterConfigMap);
            filterConfigs.addLast(httpFilterConfig);
            Logger.simple("Load HttpFilter ["+httpFilterConfig.getName()+
                    "] by ["+ httpFilterConfig.getClassName()+"]");
            filterConfigs.rewind();
        }
    }

    /**
     * 使用列表初始话路由处理器
     *
     * @param routerInfoList  路由处理器信息列表
     */
    public void addRouterByList(List<Map<String, Object>>  routerInfoList) {
        for (Map<String, Object> routerInfoMap : routerInfoList) {
            HttpRouterConfig httpRouterConfig = new HttpRouterConfig(routerInfoMap);
            routerConfigs.add(httpRouterConfig);
            Logger.simple("Load HttpRouter ["+httpRouterConfig.getName()+"] by Method ["+httpRouterConfig.getMethod()+
                    "] on route ["+httpRouterConfig.getRoute()+"] by ["+ httpRouterConfig.getClassName()+"]");
        }
    }


    /**
     * 使用列表初始话路由处理器
     *
     * @param moduleInfoList  路由处理器信息列表
     */
    public void addModuleByList(List<Map<String, Object>>  moduleInfoList) {
        for (Map<String, Object> moduleInfoMap : moduleInfoList) {
            HttpModuleConfig httpModuleConfig = new HttpModuleConfig(moduleInfoMap);
            moduleConfigs.add(httpModuleConfig);
            Logger.simple("Load HttpModule ["+httpModuleConfig.getName()+"] on [" + httpModuleConfig.getPath() +
                    "] by ["+ httpModuleConfig.getClassName()+"]");
        }
    }

    @Override
    public String toString(){
        try {
            Map<Field, Object> fieldValues = TReflect.getFieldValues(this);
            StringBuilder str = new StringBuilder();
            for(Entry<Field,Object> entry : fieldValues.entrySet()){
                str.append(entry.getKey().getName());
                str.append(":\t\t");
                str.append(entry.getValue());
                str.append("\r\n");
            }
            return str.toString();
        } catch (ReflectiveOperationException e) {
            Logger.error(e);
        }
        return "ReflectiveOperationException error by TReflect.getFieldValues Method. ";
    }
}


