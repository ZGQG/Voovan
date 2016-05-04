package org.voovan.db;

import org.voovan.tools.TSQL;
import org.voovan.tools.log.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 结果集和数据库连接封装
 *
 * @author helyho
 *
 * Java Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ResultInfo {
    private ResultSet resultSet;
    private boolean isTrancation;

    public ResultInfo(ResultSet resultSet,boolean isTrancation) {
        this.resultSet = resultSet;
        this.isTrancation = isTrancation;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getObjectList(Class<T> t) {
        try{
            return (List<T>) TSQL.getAllRowWithObjectList(t, this.resultSet);
        }catch(SQLException | ReflectiveOperationException | ParseException e){
            Logger.error("JdbcOperate.getObjectList error",e);
        }finally{
            // 非事物模式执行
            if (!isTrancation) {
                JdbcOperate.closeConnection(resultSet);
            }else{
                JdbcOperate.closeResult(resultSet);
            }
        }
        return new ArrayList<T>();

    }

    public List<Map<String, Object>> getMapList() {
        try{
            return TSQL.getAllRowWithMapList(this.resultSet);
        }catch(SQLException | ReflectiveOperationException e){
            Logger.error("JdbcOperate.getMapList error",e);
        }finally{
            // 非事物模式执行
            if (!isTrancation) {
                JdbcOperate.closeConnection(resultSet);
            }else{
                JdbcOperate.closeResult(resultSet);
            }
        }
        return new ArrayList<Map<String, Object>>();
    }

    @SuppressWarnings("unchecked")
    public <T> Object getObject(Class<T> t){
        try{
            if(resultSet.next()){
                return (T) TSQL.getOneRowWithObject(t, this.resultSet);
            }else{
                return null;
            }
        }catch(SQLException | ReflectiveOperationException | ParseException e){
            Logger.error("JdbcOperate.getObject error: "+e.getMessage(),e);
        }finally{
            // 非事物模式执行
            if (!isTrancation) {
                JdbcOperate.closeConnection(resultSet);
            }else{
                JdbcOperate.closeResult(resultSet);
            }
        }
        return null;
    }

    public Map<String, Object> getMap(){
        try{
            if(resultSet.next()){
                return TSQL.getOneRowWithMap(this.resultSet);
            }else{
                return null;
            }
        }catch(SQLException | ReflectiveOperationException e){
            Logger.error("JdbcOperate.getMap error",e);
        }finally{
            // 非事物模式执行
            if (!isTrancation) {
                JdbcOperate.closeConnection(resultSet);
            }else{
                JdbcOperate.closeResult(resultSet);
            }
        }
        return null;
    }
}
