package org.voovan.tools.cache;

import org.voovan.Global;
import org.voovan.tools.TString;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.json.JSON;
import org.voovan.tools.json.annotation.NotJSON;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;

/**
 * 对象池
 *      支持超时清理,并且支持指定对象的借出和归还操作
 *      仅仅按照时间长短控制对象的存活周期
 *
 * @author helyho
 * <p>
 * Vestful Framework.
 * WebSite: https://github.com/helyho/Vestful
 * Licence: Apache v2 License
 */
public class ObjectPool {

    private volatile ConcurrentSkipListMap<Object, PooledObject> objects = new ConcurrentSkipListMap<Object, PooledObject>();
    private volatile ConcurrentLinkedDeque<Object> unborrowedObjectIdList  = new ConcurrentLinkedDeque<Object>();

    private long aliveTime = 0;
    private boolean autoRefreshOnGet = true;
    private Function destory;

    /**
     * 构造一个对象池
     * @param aliveTime 对象存活时间,小于等于0时为一直存活,单位:秒
     * @param autoRefreshOnGet 获取对象时刷新对象存活时间
     */
    public ObjectPool(long aliveTime, boolean autoRefreshOnGet){
        this.aliveTime = aliveTime;
        this.autoRefreshOnGet = autoRefreshOnGet;
        removeDeadObject();
    }

    /**
     * 构造一个对象池
     * @param aliveTime 对象存活时间,单位:秒
     */
    public ObjectPool(long aliveTime){
        this.aliveTime = aliveTime;
        removeDeadObject();
    }

    /**
     * 构造一个对象池
     */
    public ObjectPool(){
    }

    /**
     * 获取对象销毁函数
     *      在对象被销毁前工作
     * @return 对象销毁函数
     */
    public Function getDestory() {
        return destory;
    }

    /**
     * 设置对象销毁函数
     *      在对象被销毁前工作
     * @param destory 对象销毁函数, 如果返回 null 则 清理对象, 如果返回为非 null 则刷新对象
     */
    public void setDestory(Function destory) {
        this.destory = destory;
    }

    /**
     * 设置对象池的对象存活时间
     * @param aliveTime 对象存活时间,单位:秒
     */
    public void setAliveTime(long aliveTime) {
        this.aliveTime = aliveTime;
    }

    /**
     * 生成ObjectId
     * @return 生成的ObjectId
     */
    private String genObjectId(){
        return TString.generateShortUUID();
    }


    /**
     * 是否获取对象时刷新对象存活时间
     * @return 是否获取对象时刷新对象存活时间
     */
    public boolean isAutoRefreshOnGet(){
        return autoRefreshOnGet;
    }

    /**
     * 获取池中的对象
     * @param id 对象的 hash 值
     * @return 池中的对象
     */
    public Object get(Object id){
        PooledObject pooledObject = objects.get(id);
        if(pooledObject!=null) {
            return pooledObject.getObject();
        }else{
            return null;
        }
    }



    /**
     * 增加池中的对象
     * @param obj 增加到池中的对象
     * @return 对象的 id 值
     */
    public synchronized Object add(Object obj){
        if(obj == null){
            return null;
        }
        Object id = genObjectId();
        objects.put(id, new PooledObject(this, id, obj));
        unborrowedObjectIdList.offer(id);
        return id;
    }

    /**
     * 增加池中的对象
     * @param obj 增加到池中的对象ID
     * @param obj 增加到池中的对象
     * @return 对象的 id 值
     */
    public synchronized Object add(Object id, Object obj){
        if(obj == null){
            return null;
        }
        objects.put(id, new PooledObject(this, id, obj));
        unborrowedObjectIdList.offer(id);
        return id;
    }

    /**
     * 增加池中的对象
     * @param obj 增加到池中的对象ID
     * @param obj 增加到池中的对象
     * @return 对象的 id 值
     */
    public Object addAndBorrow(Object id, Object obj){
        if(obj == null){
            return null;
        }
        objects.put(id, new PooledObject(this, id, obj));
        return id;
    }

    /**
     * 判断池中是否存在对象
     * @param id 对象的 hash 值
     * @return true: 存在, false: 不存在
     */
    public boolean contains(Object id){
        return objects.containsKey(id);
    }

    /**
     * 移除池中的对象
     * @param id 对象的 hash 值
     */
    public synchronized void remove(Object id){
        objects.remove(id);
        unborrowedObjectIdList.remove(id);
    }

    /**
     * 获取当前对象池的大小
     * @return 对象池的大小
     */
    public int size(){
        return objects.size();
    }

    /**
     * 清理池中所有的对象
     */
    public synchronized void clear(){
        objects.clear();
        unborrowedObjectIdList.clear();
    }

    /**
     * 借出这个对象
     * @return 借出的对象的 ID
     */
    public Object borrow(){
        return unborrowedObjectIdList.poll();
    }

    /**
     * 归还借出的对象
     */
    public void restitution(Object objectId){
        unborrowedObjectIdList.addLast(objectId);
    }

    private void removeDeadObject(){
        Global.getHashWheelTimer().addTask(new HashWheelTask() {
            @Override
            public void run() {
                try {
                    Iterator<PooledObject> iterator = objects.values().iterator();
                    while (iterator.hasNext()) {
                        PooledObject pooledObject = iterator.next();

                        //被借出的对象不进行清理
                        if(unborrowedObjectIdList.contains(pooledObject.getId())){
                            continue;
                        }

                        if (!pooledObject.isAlive()) {
                            if(destory!=null){
                                //如果返回 null 则 清理对象, 如果返回为非 null 则刷新对象
                                if(destory.apply(pooledObject)==null){
                                    remove(pooledObject.getId());
                                } else {
                                    pooledObject.refresh();
                                }
                            } else {
                                remove(pooledObject.getId());
                            }
                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }, 5, true);
    }

    /**
     * 池中缓存的对象模型
     */
    public class PooledObject{
        private long lastVisiediTime;
        private Object id;
        @NotJSON
        private Object object;
        @NotJSON
        private ObjectPool objectCachedPool;

        public PooledObject(ObjectPool objectCachedPool, Object id, Object object) {
            this.objectCachedPool = objectCachedPool;
            this.lastVisiediTime = System.currentTimeMillis();
            this.id = id;
            this.object = object;
        }

        /**
         * 刷新对象
         */
        public void refresh() {
            lastVisiediTime = System.currentTimeMillis();
        }

        /**
         * 获取对象
         * @return
         */
        public Object getObject() {
            if(objectCachedPool.isAutoRefreshOnGet()) {
                refresh();
            }
            return object;
        }

        /**
         * 设置设置对象
         * @param object
         */
        public void setObject(Object object) {
            this.object = object;
        }

        public Object getId() {
            return id;
        }

        /**
         * 判断对象是否存活
         * @return
         */
        public boolean isAlive(){
            if(objectCachedPool.aliveTime<=0){
                return true;
            }

            long currentAliveTime = System.currentTimeMillis() - lastVisiediTime;
            if (objectCachedPool.aliveTime>0 && currentAliveTime >= objectCachedPool.aliveTime*1000){
                return false;
            }else{
                return true;
            }
        }

        public String toString(){
            return JSON.toJSON(this).replace("\"","");
        }
    }

    public String toString(){
        return "{Total:" + objects.size() + ", unborrow:" + unborrowedObjectIdList.size()+"}";
    }
}

