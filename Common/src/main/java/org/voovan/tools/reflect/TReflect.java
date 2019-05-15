package org.voovan.tools.reflect;


import org.voovan.Global;
import org.voovan.tools.*;
import org.voovan.tools.json.JSON;
import org.voovan.tools.json.annotation.NotJSON;
import org.voovan.tools.reflect.annotation.NotSerialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 反射工具类
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TReflect {

    //这里不用 ConcurrentHashMap 的原因是 key 和 value 都不能是 null
    private static Map<String, Field> FIELDS = new ConcurrentHashMap<String ,Field>();
    private static Map<String, Method> METHODS = new ConcurrentHashMap<String ,Method>();
    private static Map<String, Constructor> CONSTRUCTORS = new ConcurrentHashMap<String ,Constructor>();
    private static Map<String, Field[]> FIELD_ARRAYS = new ConcurrentHashMap<String ,Field[]>();
    private static Map<String, Method[]> METHOD_ARRAYS = new ConcurrentHashMap<String ,Method[]>();
    private static Map<String, Constructor[]> CONSTRUCTOR_ARRAYS = new ConcurrentHashMap<String ,Constructor[]>();
    private static Map<Class, String> CLASS_NAME = new ConcurrentHashMap<Class ,String>();
    private static Map<Class, Boolean> CLASS_BASIC_TYPE = new ConcurrentHashMap<Class ,Boolean>();


    public static String getCanonicalName(Class clazz){
        String canonicalName = CLASS_NAME.get(clazz);
        if(canonicalName == null){
            canonicalName = clazz.getCanonicalName();
            CLASS_NAME.put(clazz, canonicalName);
        }

        return canonicalName;
    }

    /**
     * 获得类所有的Field
     *
     * @param clazz 类对象
     * @return Field数组
     */
    public static Field[] getFields(Class<?> clazz) {
        String marker = getCanonicalName(clazz);
        Field[] fields = FIELD_ARRAYS.get(marker);

        if(fields==null){
            LinkedHashSet<Field> fieldArray = new LinkedHashSet<Field>();
            for (; clazz!=null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                Field[] tmpFields = clazz.getDeclaredFields();
                for (Field field : tmpFields){
                    field.setAccessible(true);
                }
                fieldArray.addAll(Arrays.asList(tmpFields));
            }

            fields = fieldArray.toArray(new Field[]{});

            if(marker!=null && fields!=null) {
                FIELD_ARRAYS.put(marker, fields);
                fieldArray.clear();
            }
        }

        return fields;
    }

    /**
     * 查找类特定的Field
     *
     * @param clazz   类对象
     * @param fieldName field 名称
     * @return field 对象
     */
    public static Field findField(Class<?> clazz, String fieldName) {

        String mark = new StringBuilder(getCanonicalName(clazz)).append(Global.CHAR_SHAPE).append(fieldName).toString();

        Field field = FIELDS.get(mark);

        if(field==null){

            for (; clazz!=null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                try {
                    field = clazz.getDeclaredField(fieldName);
                    break;
                }catch(ReflectiveOperationException e){
                    field = null;
                }
            }

            if(mark!=null && field!=null) {
                field.setAccessible(true);
                FIELDS.put(mark, field);
            }

        }

        return field;
    }

    /**
     * 查找类特定的Field
     * 			不区分大小写,并且替换掉特殊字符
     * @param clazz   类对象
     * @param fieldName Field 名称
     * @return Field 对象
     * @throws ReflectiveOperationException 反射异常
     */
    public static Field findFieldIgnoreCase(Class<?> clazz, String fieldName)
            throws ReflectiveOperationException{

        String marker = new StringBuilder(getCanonicalName(clazz)).append(Global.CHAR_SHAPE).append(fieldName).toString();

        Field field = FIELDS.get(marker);
        if (field==null){
            for (Field fieldItem : getFields(clazz)) {
                if (fieldItem.getName().equalsIgnoreCase(fieldName) || fieldItem.getName().equalsIgnoreCase(TString.underlineToCamel(fieldName))) {
                    if(marker!=null && fieldItem!=null) {
                        FIELDS.put(marker, fieldItem);
                        field = fieldItem;
                        break;
                    }

                }
            }
        }
        return field;
    }

    /**
     * 获取类型的范型类型
     * @param type 类型对象
     * @return Class[] 对象
     */
    public static Class[] getGenericClass(Type type) {
        ParameterizedType parameterizedType = null;
        if(type instanceof ParameterizedType) {
            parameterizedType = (ParameterizedType) type;
        }

        if(parameterizedType==null){
            return null;
        }

        Class[] result = null;
        Type[] actualType = parameterizedType.getActualTypeArguments();
        result = new Class[actualType.length];

        for(int i=0;i<actualType.length;i++){
            if(actualType[i] instanceof Class){
                result[i] = (Class)actualType[i];
            } else if(actualType[i] instanceof Type){
                String classStr = actualType[i].toString();
                classStr = TString.fastReplaceAll(classStr, "<.*>", "");
                try {
                    result[i] = Class.forName(classStr);
                } catch(Exception e){
                    result[i] = Object.class;
                }
            } else{
                result[i] = Object.class;
            }
        }
        return result;
    }
    /**
     * 获取对象的范型类型
     * @param object 对象
     * @return Class[] 对象
     */
    public static Class[] getGenericClass(Object object) {
        Class[] genericClazzs = TReflect.getGenericClass(object.getClass());

        if (genericClazzs == null) {
            if (object instanceof Map) {
                if (((Map) object).size() > 0) {
                    Map.Entry entry = (Map.Entry) ((Map) object).entrySet().iterator().next();
                    genericClazzs = new Class[]{entry.getKey().getClass(), entry.getValue().getClass()};
                }
            } else if (object instanceof Collection) {
                if (((Collection) object).size() > 0) {
                    Object obj = ((Collection) object).iterator().next();
                    genericClazzs = new Class[]{obj.getClass()};
                }
            }
        }

        return genericClazzs;
    }


    /**
     * 获取 Field 的范型类型
     * @param field  field 对象
     * @return 返回范型类型数组
     */
    public static Class[] getFieldGenericType(Field field) {
        Type fieldType = field.getGenericType();
        return getGenericClass((ParameterizedType)fieldType);
    }

    /**
     * 获取类中指定Field的值
     * @param <T> 范型
     * @param obj  对象
     * @param fieldName Field 名称
     * @return Field 的值
     * @throws ReflectiveOperationException 反射异常
     */
    @SuppressWarnings("unchecked")
    static public <T> T getFieldValue(Object obj, String fieldName)
            throws ReflectiveOperationException {
        Field field = findField(obj.getClass(), fieldName);
        return (T) field.get(obj);
    }


    /**
     * 更新对象中指定的Field的值
     * 		注意:对 private 等字段有效
     *
     * @param obj  对象
     * @param field field 对象
     * @param fieldValue field 值
     * @throws ReflectiveOperationException 反射异常
     */
    public static void setFieldValue(Object obj, Field field,
                                     Object fieldValue) throws ReflectiveOperationException {
        field.set(obj, fieldValue);
    }

    /**
     * 更新对象中指定的Field的值
     * 		注意:对 private 等字段有效
     *
     * @param obj  对象
     * @param fieldName field 名称
     * @param fieldValue field 值
     * @throws ReflectiveOperationException 反射异常
     */
    public static void setFieldValue(Object obj, String fieldName,
                                     Object fieldValue) throws ReflectiveOperationException {
        Field field = findField(obj.getClass(), fieldName);
        setFieldValue(obj, field, fieldValue);
    }

    /**
     * 将对象中的field和其值组装成Map 静态字段(static修饰的)不包括
     *
     * @param obj 对象
     * @return 所有 field 名称-值拼装的 Map
     * @throws ReflectiveOperationException 反射异常
     */
    public static Map<Field, Object> getFieldValues(Object obj)
            throws ReflectiveOperationException {
        HashMap<Field, Object> result = new HashMap<Field, Object>();
        Field[] fields = getFields(obj.getClass());
        for (Field field : fields) {

            //静态属性不序列化
            if((field.getModifiers() & 0x00000008) != 0){
                continue;
            }

            Object value = field.get(obj);
            result.put(field, value);
        }
        return result;
    }

    /**
     * 查找类中的方法
     * @param clazz        类对象
     * @param name		   方法名
     * @param paramTypes   参数类型
     * @return			   方法对象
     */
    public static Method findMethod(Class<?> clazz, String name,
                                    Class<?>... paramTypes) {
        StringBuilder markBuilder = new StringBuilder(getCanonicalName(clazz)).append(Global.CHAR_SHAPE).append(name);
        for(Class<?> paramType : paramTypes){
            markBuilder.append("$").append(getCanonicalName(paramType));
        }

        String marker = markBuilder.toString();

        Method method = METHODS.get(marker);
        if (method==null){

            for (; clazz!=null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                try {
                    method = clazz.getDeclaredMethod(name, paramTypes);
                    break;
                }catch(ReflectiveOperationException e){
                    method = null;
                }
            }

            if(marker!=null && method!=null) {
                METHODS.put(marker, method);
            }
        }

        return method;
    }

    /**
     * 查找类中的方法(使用参数数量)
     * @param clazz        类对象
     * @param name		   方法名
     * @param paramCount   参数数量
     * @return			   方法对象
     */
    public static Method[] findMethod(Class<?> clazz, String name,
                                      int paramCount) {
        String marker = new StringBuilder(getCanonicalName(clazz)).append(Global.CHAR_SHAPE).append(name).append(Global.CHAR_AT).append(paramCount).toString();

        Method[] methods = METHOD_ARRAYS.get(marker);

        if (methods==null){
            LinkedHashSet<Method> methodList = new LinkedHashSet<Method>();
            Method[] allMethods = getMethods(clazz, name);
            for (Method method : allMethods) {
                if (method.getParameterTypes().length == paramCount) {
                    methodList.add(method);
                }
            }

            methods = methodList.toArray(new Method[]{});

            if(marker!=null && methods!=null) {
                METHOD_ARRAYS.put(marker, methods);
                methodList.clear();
            }
        }

        return methods;
    }

    /**
     * 获取类的方法集合
     * @param clazz		类对象
     * @return Method 对象数组
     */
    public static Method[] getMethods(Class<?> clazz) {

        Method[] methods = null;

        String marker = getCanonicalName(clazz);

        methods = METHOD_ARRAYS.get(marker);

        if(methods==null){
            LinkedHashSet<Method> methodList = new LinkedHashSet<Method>();
            for (; clazz!=null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                Method[] tmpMethods = clazz.getDeclaredMethods();
                methodList.addAll(Arrays.asList(tmpMethods));
            }

            methods = methodList.toArray(new Method[]{});

            if(marker!=null && methods!=null) {
                METHOD_ARRAYS.put(marker, methods);
                methodList.clear();
            }

        }

        return methods;
    }

    /**
     * 获取类的特定方法的集合
     * 		类中可能存在同名方法
     * @param clazz		类对象
     * @param name		方法名
     * @return Method 对象数组
     */
    public static Method[] getMethods(Class<?> clazz,String name) {

        Method[] methods = null;

        String marker = new StringBuilder(getCanonicalName(clazz)).append(Global.CHAR_SHAPE).append(name).toString();
        methods = METHOD_ARRAYS.get(marker);
        if(methods==null){

            LinkedHashSet<Method> methodList = new LinkedHashSet<Method>();
            Method[] allMethods = getMethods(clazz);
            for (Method method : allMethods) {
                if (method.getName().equals(name))
                    methodList.add(method);
            }

            methods = methodList.toArray(new Method[0]);

            if(marker!=null && methods!=null) {
                METHOD_ARRAYS.put(marker, methods);
                methodList.clear();
            }
        }

        return methods;
    }

    /**
     * 获取方法的参数返回值的范型类型
     * @param method  method 对象
     * @param parameterIndex 参数索引(大于0)参数索引位置[第一个参数为0,以此类推], (-1) 返回值
     * @return 返回范型类型数组
     */
    public static Class[] getMethodParameterGenericType(Method method,int parameterIndex) {
        Class[] result = null;
        Type parameterType;

        if(parameterIndex == -1){
            parameterType = method.getGenericReturnType();
        }else{
            parameterType = method.getGenericParameterTypes()[parameterIndex];
        }

        return getGenericClass(parameterType);
    }

    /**
     * 使用对象执行它的一个方法
     * 		对对象执行一个指定Method对象的方法
     * @param obj				执行方法的对象
     * @param method			方法对象
     * @param parameters        多个参数
     * @param <T> 范型
     * @return					方法返回结果
     * @throws ReflectiveOperationException 反射异常
     */
    public static <T> T invokeMethod(Object obj, Method method, Object... parameters)
            throws ReflectiveOperationException {
        if(!method.isAccessible()) {
            method.setAccessible(true);
        }
        return (T)method.invoke(obj, parameters);
    }

    /**
     * 使用对象执行方法
     * 推荐使用的方法,这个方法会自动寻找参数匹配度最好的方法并执行
     * 对对象执行一个通过 方法名和参数列表选择的方法
     * @param obj				执行方法的对象,如果调用静态方法直接传 Class 类型的对象
     * @param name				执行方法名
     * @param args		方法参数
     * @param <T> 范型
     * @return					方法返回结果
     * @throws ReflectiveOperationException		反射异常
     */
    public static <T> T invokeMethod(Object obj, String name, Object... args)
            throws ReflectiveOperationException {
        if(args==null){
            args = new Object[0];
        }
        Class<?>[] parameterTypes = getArrayClasses(args);
        Method method = null;
        Class objClass = (obj instanceof Class) ? (Class)obj : obj.getClass();
        try {
            method = findMethod(objClass, name, parameterTypes);
            if(!method.isAccessible()) {
                method.setAccessible(true);
            }
            return (T)method.invoke(obj, args);
        }catch(Exception e){
            Exception lastExecption = e;

            if(e instanceof NoSuchMethodException || method == null) {
                //找到这个名称的所有方法
                Method[] methods = findMethod(objClass, name, parameterTypes.length);
                for (Method similarMethod : methods) {
                    Type[] methodParamTypes = similarMethod.getGenericParameterTypes();
                    //匹配参数数量相等的方法
                    if (methodParamTypes.length == args.length) {
                        try{
                            return (T)similarMethod.invoke(obj, args);
                        } catch (Exception ex){
                            //不处理
                        }

                        try {
                            Object[] convertedParams = new Object[args.length];
                            for (int i = 0; i < methodParamTypes.length; i++) {
                                Type parameterType = methodParamTypes[i];

                                //如果范型类型没有指定则使用 Object 作为默认类型
                                if(parameterType instanceof TypeVariable){
                                    parameterType = Object.class;
                                }

                                //参数类型转换
                                String value = "";

                                //这里对参数类型是 Object或者是范型 的提供支持
                                if (parameterType != Object.class && args[i] != null) {
                                    Class argClass = args[i].getClass();

                                    //复杂的对象通过 JSON转换成字符串,再转换成特定类型的对象
                                    if (args[i] instanceof Collection ||
                                            args[i] instanceof Map ||
                                            argClass.isArray() ||
                                            !TReflect.isBasicType(argClass)) {
                                        //增加对于基本类型 Array 的支持
                                        if(argClass.isArray() && TReflect.isBasicType(argClass.getComponentType())) {
                                            convertedParams[i]  = args[i];
                                            continue;
                                        } else {
                                            value = JSON.toJSON(args[i]);
                                        }
                                    } else {
                                        value = args[i].toString();
                                    }
                                    convertedParams[i] = TString.toObject(value, parameterType);
                                } else {
                                    convertedParams[i] = args[i];
                                }
                            }
                            method = similarMethod;
                            if(!method.isAccessible()) {
                                method.setAccessible(true);
                            }
                            return (T)method.invoke(obj, convertedParams);
                        } catch (Exception ex) {
                            lastExecption = ex;
                            continue;
                        }
                    }
                }
            }

            if ( !(lastExecption instanceof ReflectiveOperationException) ) {
                lastExecption = new ReflectiveOperationException(lastExecption.getMessage(), lastExecption);
            }

            throw (ReflectiveOperationException)lastExecption;
        }
    }

    /**
     * 构造新的对象
     * 	通过参数中的构造参数对象parameters,选择特定的构造方法构造
     * @param <T>           范型
     * @param clazz			类对象
     * @param args	构造方法参数
     * @return 新的对象
     * @throws ReflectiveOperationException 反射异常
     */
    public static <T> T newInstance(Class<T> clazz, Object ...args)
            throws ReflectiveOperationException {

        if(args==null){
            args = new Object[0];
        }

        Class targetClazz = clazz;

        //不可构造的类型使用最常用的类型
        if(isImpByInterface(clazz, List.class) && (Modifier.isAbstract(clazz.getModifiers()) || Modifier.isInterface(clazz.getModifiers()))){
            targetClazz = ArrayList.class;
        }

        //不可构造的类型使用最常用的类型
        if(isImpByInterface(clazz, Set.class) && (Modifier.isAbstract(clazz.getModifiers()) || Modifier.isInterface(clazz.getModifiers()))){
            targetClazz = LinkedHashSet.class;
        }

        //不可构造的类型使用最常用的类型
        if(isImpByInterface(clazz, Map.class) && (Modifier.isAbstract(clazz.getModifiers()) && Modifier.isInterface(clazz.getModifiers()))){
            targetClazz = LinkedHashMap.class;
        }


        Class<?>[] parameterTypes = getArrayClasses(args);

        StringBuilder markBuilder = new StringBuilder(getCanonicalName(targetClazz));
        for(Class<?> paramType : parameterTypes){
            markBuilder.append("$").append(getCanonicalName(paramType));
        }

        String mark = markBuilder.toString();

        Constructor<T> constructor = CONSTRUCTORS.get(mark);

        try {

            if (constructor==null){
                if (args.length == 0) {
                    try {
                        constructor = targetClazz.getConstructor();
                    }catch (Exception e) {
                        return (T) TUnsafe.getUnsafe().allocateInstance(targetClazz);
                    }
                } else {
                    constructor = targetClazz.getConstructor(parameterTypes);
                }

                if(mark!=null && constructor!=null) {
                    CONSTRUCTORS.put(mark, constructor);
                }
            }

            return constructor.newInstance(args);

        }catch(Exception e){
            Exception lastExecption = e;
            if(constructor==null) {

                //缓存构造函数
                mark = getCanonicalName(targetClazz);
                Constructor[] constructors =  CONSTRUCTOR_ARRAYS.get(mark);
                if(constructors==null){

                    constructors = targetClazz.getConstructors();

                    if(mark!=null && constructor!=null) {
                        CONSTRUCTOR_ARRAYS.put(mark, constructors);
                    }
                }

                for (Constructor similarConstructor : constructors) {
                    Class[] methodParamTypes = similarConstructor.getParameterTypes();
                    //匹配参数数量相等的方法
                    if (methodParamTypes.length == args.length) {

                        try{
                            return (T) similarConstructor.newInstance(args);
                        } catch (Exception ex){
                            //不处理
                        }

                        try {
                            Object[] convertedParams = new Object[args.length];
                            for (int i = 0; i < methodParamTypes.length; i++) {
                                Class parameterType = methodParamTypes[i];
                                //参数类型转换
                                String value = "";

                                Class parameterClass = args[i].getClass();

                                //复杂的对象通过 JSON转换成字符串,再转换成特定类型的对象
                                if (args[i] instanceof Collection ||
                                        args[i] instanceof Map ||
                                        parameterClass.isArray() ||
                                        !TReflect.isBasicType(parameterClass)) {
                                    value = JSON.toJSON(args[i]);
                                } else {
                                    value = args[i].toString();
                                }

                                convertedParams[i] = TString.toObject(value, parameterType);
                            }
                            constructor = similarConstructor;

                            return constructor.newInstance(convertedParams);
                        } catch (Exception ex) {
                            continue;
                        }
                    }
                }
            }

            if ( !(lastExecption instanceof ReflectiveOperationException) ) {
                lastExecption = new ReflectiveOperationException(lastExecption.getMessage(), lastExecption);
            }

            //尝试使用 Unsafe 分配
            try{
                return (T)allocateInstance(targetClazz);
            }catch(Exception ex) {
                throw e;
            }
        }

    }

    /**
     * 构造新的对象
     * @param <T> 范型
     * @param className		类名称
     * @param parameters	构造方法参数
     * @return 新的对象
     * @throws ReflectiveOperationException 反射异常
     */
    public static <T> T newInstance(String className, Object ...parameters) throws ReflectiveOperationException {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) Class.forName(className);
        return newInstance(clazz,parameters);
    }

    /**
     * 采用 Unsafe 构造一个对象,无须参数
     * @param clazz 对象类型
     * @param <T> 范型
     * @return 新的对象
     * @throws InstantiationException 实例化异常
     */
    public static <T> T allocateInstance(Class<T> clazz) throws InstantiationException {
        return (T) TUnsafe.getUnsafe().allocateInstance(clazz);
    }

    /**
     * 将对象数组转换成,对象类型的数组
     * @param objs	对象类型数组
     * @return 类数组
     */
    public static Class<?>[] getArrayClasses(Object[] objs){
        if(objs == null){
            return new Class<?>[0];
        }

        Class<?>[] parameterTypes= new Class<?>[objs.length];
        for(int i=0;i<objs.length;i++){
            if(objs[i]==null){
                parameterTypes[i] = Object.class;
            }else {
                parameterTypes[i] = objs[i].getClass();
            }
        }
        return parameterTypes;
    }

    /**
     * 将Map转换成指定的对象
     *
     * @param type			类对象
     * @param mapArg		Map 对象
     * @param ignoreCase    匹配属性名是否不区分大小写
     * @return 转换后的对象
     * @param <T> 范型
     * @throws ReflectiveOperationException 反射异常
     * @throws ParseException 解析异常
     */
    public static <T>T getObjectFromMap(Type type, Map<String, ?> mapArg,  boolean ignoreCase) throws ParseException, ReflectiveOperationException {
        Class[] genericType = null;

        if(type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            genericType = getGenericClass(parameterizedType);
        }

        return getObjectFromMap(type, mapArg, genericType, ignoreCase);
    }

    //只有值的 Map 的键数据
    public final static Object SINGLE_VALUE_KEY = new Object();

    /**
     * 将Map转换成指定的对象
     *
     * @param type			类对象
     * @param mapArg		Map 对象
     * @param genericType   范型类型描述
     * @param ignoreCase    匹配属性名是否不区分大小写
     * @return 转换后的对象
     * @param <T> 范型
     * @throws ReflectiveOperationException 反射异常
     * @throws ParseException 解析异常
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T>T getObjectFromMap(Type type, Map<String, ?> mapArg, Class[] genericType,  boolean ignoreCase)
            throws ReflectiveOperationException, ParseException {
        T obj = null;
        Class<?> clazz = null;
        if(type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            clazz = (Class)parameterizedType.getRawType();
        }else if(type instanceof Class){
            clazz = (Class)type;
        }

        if(mapArg==null){
            return null;
        }

        Object singleValue = mapArg;

        if(mapArg.containsKey(SINGLE_VALUE_KEY)){
            singleValue = mapArg.get(SINGLE_VALUE_KEY);
        } else if(mapArg.size() == 1) {
            singleValue = mapArg.values().iterator().next();
        }

        //对象类型
        if(clazz == Object.class){
            if(mapArg.containsKey(SINGLE_VALUE_KEY)) {
                obj = (T) singleValue;
            } else {
                obj = (T) mapArg;
            }
        }

        //java标准对象
        else if (clazz.isPrimitive()){
            if(singleValue!=null && singleValue.getClass() !=  clazz) {
                obj = TString.toObject(singleValue.toString(), clazz);
            } else {
                obj = (T)singleValue;
            }
        }
        //java基本对象
        else if (TReflect.isBasicType(clazz)) {
            //取 Map.Values 里的递第一个值
            obj = (T)(singleValue==null ? null : newInstance(clazz,  singleValue.toString()));
        }
        //java BigDecimal对象
        else if (clazz == BigDecimal.class) {
            //取 Map.Values 里的递第一个值
            String value = singleValue==null ? null:singleValue.toString();
            obj = (T)(singleValue==null ? null : new BigDecimal(value));
        }
        //对 Atom 类型的处理
        else if (clazz == AtomicLong.class || clazz == AtomicInteger.class || clazz == AtomicBoolean.class) {
            if(singleValue==null){
                obj = null;
            } else {
                obj = (T) TReflect.newInstance(clazz, singleValue);
            }
        }
        //java 日期对象
        else if(isExtendsByClass(clazz, Date.class)){
            //取 Map.Values 里的递第一个值
            String value = singleValue == null ? null : singleValue.toString();
            SimpleDateFormat dateFormat = new SimpleDateFormat(TDateTime.STANDER_DATETIME_TEMPLATE);
            Date dateObj = singleValue != null ? dateFormat.parse(value.toString()) : null;
            obj = (T)TReflect.newInstance(clazz,dateObj.getTime());
        }
        //Map 类型
        else if(isImpByInterface(clazz, Map.class)){
            Map mapObject = (Map)newInstance(clazz);

            if(genericType!=null) {
                Iterator iterator = mapArg.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry entry = (Entry) iterator.next();
                    Map keyOfMap = null;
                    Map valueOfMap = null;

                    if (entry.getKey() instanceof Map) {
                        keyOfMap = (Map) entry.getKey();
                    } else {
                        keyOfMap = TObject.asMap(SINGLE_VALUE_KEY, entry.getKey());
                    }

                    if (entry.getValue() instanceof Map) {
                        valueOfMap = (Map) entry.getValue();
                    } else {
                        valueOfMap = TObject.asMap(SINGLE_VALUE_KEY, entry.getValue());
                    }

                    Object keyObj = getObjectFromMap(genericType[0], keyOfMap, ignoreCase);
                    Object valueObj = getObjectFromMap(genericType[1], valueOfMap, ignoreCase);
                    mapObject.put(keyObj, valueObj);
                }
            }else{
                mapObject.putAll(mapArg);
            }
            obj = (T)mapObject;
        }
        //Collection 类型
        else if(isImpByInterface(clazz, Collection.class)){
            Collection collectionObject = (Collection)newInstance(clazz);

            if(singleValue!=null){
                if(genericType!=null){
                    for (Object listItem : (Collection)singleValue) {
                        Map valueOfMap = null;
                        if (listItem instanceof Map) {
                            valueOfMap = (Map) listItem;
                        } else {
                            valueOfMap = TObject.asMap(SINGLE_VALUE_KEY, listItem);
                        }

                        Object item = getObjectFromMap(genericType[0], valueOfMap, ignoreCase);
                        collectionObject.add(item);
                    }
                }else{
                    collectionObject.addAll((Collection)singleValue);
                }
            }
            obj = (T)collectionObject;
        }
        //Array 类型
        else if(clazz.isArray()){
            Class arrayClass = clazz.getComponentType();
            Object tempArrayObj = Array.newInstance(arrayClass, 0);
            return (T)((Collection)singleValue).toArray((Object[])tempArrayObj);
        }
        // 复杂对象
        else {
            try {
                obj = (T) newInstance(clazz);
            } catch (InstantiationException e){
                return null;
            }
            for(Entry<String,?> argEntry : mapArg.entrySet()){
                String key = argEntry.getKey();
                Object value = argEntry.getValue();

                Field field = null;
                if(ignoreCase) {
                    //忽略大小写匹配
                    field = findFieldIgnoreCase(clazz, key);
                }else{
                    //精确匹配属性
                    field = findField(clazz, key);
                }

                if(field!=null && !Modifier.isFinal(field.getModifiers())) {
                    String fieldName = field.getName();
                    Class fieldType = field.getType();
                    Type fieldGenericType = field.getGenericType();
                    try {

                        //value 和 fieldType class类型不同时，且　value 不为空时处理
                        if(value != null && fieldType != value.getClass()) {
                            //通过 JSON 将,String类型的 value转换,将 String 转换成 Collection, Map 或者 复杂类型 对象作为参数
                            if( value instanceof String &&
                                    (
                                            isImpByInterface(fieldType, Map.class) ||
                                                    isImpByInterface(fieldType, Collection.class) ||
                                                    !TReflect.isBasicType(fieldType)
                                    )
                            ){
                                value = TString.toObject(value.toString(), fieldType);
                            }

                            //对于 目标对象类型为 Map 的属性进行处理,查找范型,并转换为范型定义的类型
                            else if (isImpByInterface(fieldType, Map.class) && value instanceof Map) {
                                value = getObjectFromMap(fieldGenericType, (Map<String,?>)value, ignoreCase);
                            }
                            //对于 目标对象类型为 Collection 的属性进行处理,查找范型,并转换为范型定义的类型
                            else if (isImpByInterface(fieldType, Collection.class) && value instanceof Collection) {
                                value = getObjectFromMap(fieldGenericType, TObject.asMap(SINGLE_VALUE_KEY, value), ignoreCase);
                            }
                            //对于 目标对象类型不是 Map,则认定为复杂类型
                            else if (!isImpByInterface(fieldType, Map.class)) {
                                if (value instanceof Map) {
                                    value = getObjectFromMap(fieldType, (Map<String, ?>) value, ignoreCase);
                                } else {
                                    value = getObjectFromMap(fieldType, TObject.asMap(SINGLE_VALUE_KEY, value), ignoreCase);
                                }
                            }else{
                                throw new ReflectiveOperationException("Conver field object error! Exception type: " +
                                        fieldType.getName() +
                                        ", Object type: "+
                                        value.getClass().getName());
                            }
                        }
                        setFieldValue(obj, fieldName, value);
                    }catch(Exception e){
                        throw new ReflectiveOperationException("Fill object " + getCanonicalName(obj.getClass()) +
                                Global.CHAR_SHAPE+fieldName+" failed", e);
                    }
                }
            }
        }
        return obj;
    }

    /**
     * 将对象转换成 Map
     * 			key 对象属性名称
     * 			value 对象属性值
     * @param obj      待转换的对象
     * @return 转后的 Map
     * @throws ReflectiveOperationException 反射异常
     */
    public static Map<String, Object> getMapfromObject(Object obj) throws ReflectiveOperationException{
        return getMapfromObject(obj, false);
    }

    /**
     * 将对象转换成 Map
     * 			key 对象属性名称
     * 			value 对象属性值
     * @param obj      待转换的对象
     * @param allField 是否序列化所有属性
     * @return 转后的 Map
     * @throws ReflectiveOperationException 反射异常
     */
    public static Map<String, Object> getMapfromObject(Object obj, boolean allField) throws ReflectiveOperationException{

        LinkedHashMap<String, Object> mapResult = new LinkedHashMap<String, Object>();

        //如果是 java 标准类型
        if(obj==null || TReflect.isBasicType(obj.getClass())){
            mapResult.put(null, obj);
        }
        //java 日期对象
        else if(isExtendsByClass(obj.getClass(),Date.class)){
            mapResult.put(null,TDateTime.format((Date) obj, TDateTime.STANDER_DATETIME_TEMPLATE));
        }
        //对 Collection 类型的处理
        else if(obj instanceof Collection){
            Collection collection = new ArrayList();
            synchronized (obj) {
                Object[] objectArray = ((Collection) obj).toArray(new Object[0]);
                for (Object collectionItem : objectArray) {
                    Map<String, Object> item = getMapfromObject(collectionItem, allField);
                    collection.add((item.size() == 1 && item.containsKey(null)) ? item.get(null) : item);
                }
            }
            mapResult.put(null, collection);
        }
        //对 Array 类型的处理
        else if(obj.getClass().isArray()){
            Class arrayClass = obj.getClass().getComponentType();
            Object targetArray = Array.newInstance(arrayClass, Array.getLength(obj));

            for(int i=0;i<Array.getLength(obj);i++) {
                Object arrayItem = Array.get(obj, i);
                Map<String, Object> item = getMapfromObject(arrayItem, allField);
                Array.set(targetArray, i, (item.size()==1 && item.containsKey(null)) ? item.get(null) : item);
            }
            mapResult.put(null, targetArray);
        }
        //对 Atom 类型的处理
        else if (obj instanceof AtomicLong || obj instanceof AtomicInteger || obj instanceof AtomicBoolean) {
            mapResult.put(null, TReflect.invokeMethod(obj, "get"));
        }
        //对 BigDecimal 类型的处理
        else if (obj instanceof BigDecimal) {
            if(BigDecimal.ZERO.compareTo((BigDecimal)obj)==0){
                obj = BigDecimal.ZERO;
            }
            mapResult.put(null, ((BigDecimal) obj).toPlainString());
        }
        //对 Map 类型的处理
        else if(obj instanceof Map){
            Map mapObject = (Map)obj;

            Map map = new LinkedHashMap();
            synchronized (obj) {
                Iterator iterator = mapObject.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry<?, ?> entry = (Entry<?, ?>) iterator.next();
                    Map<String, Object> keyItem = getMapfromObject(entry.getKey(), allField);
                    Map<String, Object> valueItem = getMapfromObject(entry.getValue(), allField);
                    Object key = (keyItem.size() == 1 && keyItem.containsKey(null)) ? keyItem.get(null) : keyItem;
                    Object value = (valueItem.size() == 1 && valueItem.containsKey(null)) ? valueItem.get(null) : valueItem;
                    map.put(key, value);
                }
            }
            mapResult.put(null, map);
        }
        //复杂对象类型
        else{
            Map<Field, Object> fieldValues =  TReflect.getFieldValues(obj);
            for(Entry<Field,Object> entry : fieldValues.entrySet()){
                Field field = entry.getKey();

                //过滤不可序列化的字段
                if (!allField) {
                    if(	field.getAnnotation(NotSerialization.class)!=null ||
                            (field.getAnnotation(NotJSON.class)!=null && TEnv.classInCurrentStack(".tools.json.", null))) {
                        continue;
                    }
                }

                String key = entry.getKey().getName();
                Object value = entry.getValue();

                if(value == null){
                    //由于属性是按子类->父类顺序处理的, 所以如果子类和父类有重复属性, 则只在子类为空时用父类的属性覆盖
                    if(mapResult.get(key) == null) {
                        mapResult.put(key, value);
                    }
                }else if(!key.contains("$")){
                    Class valueClass = entry.getValue().getClass();
                    if(TReflect.isBasicType(valueClass)){
                        //由于属性是按子类->父类顺序处理的, 所以如果子类和父类有重复属性, 则只在子类为空时用父类的属性覆盖
                        if(mapResult.get(key) == null) {
                            mapResult.put(key, value);
                        }
                    }else {
                        //如果是复杂类型则递归调用
                        Map resultMap = getMapfromObject(value, allField);
                        if(resultMap.size()==1 && resultMap.containsKey(null)){
                            mapResult.put(key, resultMap.get(null));
                        }else{
                            mapResult.put(key, resultMap);
                        }
                    }
                }
            }
        }
        return mapResult;
    }

    /**
     * 判断某个类型是否实现了某个接口
     * 		包括判断其父接口
     * @param type               被判断的类型
     * @param interfaceClass     检查是否实现了次类的接口
     * @return 是否实现某个接口
     */
    public static boolean isImpByInterface(Class<?> type,Class<?> interfaceClass){
        if(type==interfaceClass && interfaceClass.isInterface()){
            return true;
        }

        return interfaceClass.isAssignableFrom(type);
    }


    /**
     * 判断某个类型是否继承于某个类
     * 		包括判断其父类
     * @param type			判断的类型
     * @param extendsClass	用于判断的父类类型
     * @return 是否继承于某个类
     */
    public static boolean isExtendsByClass(Class<?> type,Class<?> extendsClass){
        if(type==extendsClass && !extendsClass.isInterface()){
            return true;
        }

        return extendsClass.isAssignableFrom(type);
    }

    /**
     * 类检查器
     * 		是否符合 filters 中的约束条件, 注解/类/接口等
     * @param clazz    Class 对象
     * @param filters  过滤器
     * @return true: 符合约束, false: 不符合约束
     */
    public static boolean classChecker(Class clazz, Class[] filters){
        int matchCount = 0;
        List<Annotation> annotations = TObject.asList(clazz.getAnnotations());

        if(clazz.isAnonymousClass()) {
            return false;
        }

        for(Class filterClazz : filters){
            if(clazz == filterClazz){
                break;
            }

            if(filterClazz.isAnnotation() && clazz.isAnnotationPresent(filterClazz)){
                matchCount++;
            }else if(filterClazz.isInterface() && TReflect.isImpByInterface(clazz, filterClazz)){
                matchCount++;
            }else if(TReflect.isExtendsByClass(clazz, filterClazz)){
                matchCount++;
            }
        }

        if(matchCount < filters.length){
            return false;
        }else{
            return true;
        }
    }

    /**
     * 获取类的继承树上的所有父类
     * @param type 类型 Class
     * @return 所有父类
     */
    public static Class[] getAllExtendAndInterfaceClass(Class<?> type){
        if(type == null){
            return null;
        }

        LinkedHashSet<Class> classes = new LinkedHashSet<Class>();

        Class<?> superClass = type;
        do{
            superClass = superClass.getSuperclass();
            classes.addAll(Arrays.asList(superClass.getInterfaces()));
            classes.add(superClass);
        }while(superClass!=null && Object.class != superClass);
        return classes.toArray(new Class[]{});
    }

    /**
     * 获取类的 json 形式的描述
     * @param clazz  Class 类型对象
     * @return 类的 json 形式的描述
     */
    public static String getClazzJSONModel(Class clazz){
        StringBuilder jsonStrBuilder = new StringBuilder();
        if(TReflect.isBasicType(clazz)){
            jsonStrBuilder.append(clazz.getName());
        } else if(clazz.isArray()){
            String clazzName = getCanonicalName(clazz);
            clazzName = clazzName.substring(clazzName.lastIndexOf(Global.STR_POINT)+1,clazzName.length()-2)+"[]";
            jsonStrBuilder.append(clazzName);
        } else {
            jsonStrBuilder.append(Global.STR_LC_BRACES);
            for (Field field : TReflect.getFields(clazz)) {
                jsonStrBuilder.append(Global.STR_QUOTE);
                jsonStrBuilder.append(field.getName());
                jsonStrBuilder.append(Global.STR_QUOTE).append(Global.STR_COLON);
                String filedValueModel = getClazzJSONModel(field.getType());
                if(filedValueModel.startsWith(Global.STR_LC_BRACES) && filedValueModel.endsWith(Global.STR_RC_BRACES)) {
                    jsonStrBuilder.append(filedValueModel);
                    jsonStrBuilder.append(Global.STR_COMMA);
                } else if(filedValueModel.startsWith(Global.STR_LS_BRACES) && filedValueModel.endsWith(Global.STR_RS_BRACES)) {
                    jsonStrBuilder.append(filedValueModel);
                    jsonStrBuilder.append(Global.STR_COMMA);
                } else {
                    jsonStrBuilder.append(Global.STR_QUOTE);
                    jsonStrBuilder.append(filedValueModel);
                    jsonStrBuilder.append(Global.STR_QUOTE).append(Global.STR_COMMA);
                }
            }
            jsonStrBuilder.deleteCharAt(jsonStrBuilder.length()-1);
            jsonStrBuilder.append("}");
        }
        return jsonStrBuilder.toString();
    }

    /**
     * 过滤对象的属性, 产生一个 Map
     *      未包含的属性的值将会以 null 返回
     * @param obj 对象
     * @param fields 保留的属性
     * @return 最后产生的 Map
     */
    public static Map<String, Object> fieldFilter(Object obj, String ... fields) {
        Map<String, Object> resultMap = new LinkedHashMap<String, Object>();
        for(String fieldFilter : fields){
            int firstIndex = fieldFilter.indexOf(Global.STR_LS_BRACES);
            String field = firstIndex == -1? fieldFilter : fieldFilter.substring(0, firstIndex);
            ;
            Object value = null;

            //Map
            if(obj instanceof Map){
                Map paramMap = (Map)obj;
                value = paramMap.get(field);
            }
            //List/Array
            else if(obj.getClass().isArray() || obj instanceof List) {
                if(obj.getClass().isArray()){
                    obj = TObject.asList((Object[])obj);
                }
                for(Object subObj : (List)obj){
                    fieldFilter(subObj, fields);
                }
            }
            //complex object
            else {
                try {
                    value = TReflect.getFieldValue(obj, field);
                } catch (ReflectiveOperationException e) {
                    value = null;
                }
            }

            if(firstIndex>1) {
                Map<String, Object> subResultMap = new LinkedHashMap<String, Object>();
                String subFieldStr= fieldFilter.substring(firstIndex);
                subFieldStr = TString.removeSuffix(subFieldStr);
                subFieldStr = TString.removePrefix(subFieldStr);
                String[] subFieldArray = subFieldStr.split(",");
                for(String subField : subFieldArray) {
                    Map<String, Object> data = fieldFilter(value, subField);
                    subResultMap.putAll(data);
                }
                value = subResultMap;
            }

            resultMap.put(field, value);
        }
        return resultMap;
    }

    /**
     * 判读是否是基本类型(null, boolean, byte, char, double, float, int, long, short, string)
     * @param clazz Class 对象
     * @return true: 是基本类型, false:非基本类型
     */
    public static boolean isBasicType(Class clazz) {
        Boolean isBasicType = CLASS_BASIC_TYPE.get(clazz);
        if(isBasicType==null) {
            if (clazz == null ||
                    clazz.isPrimitive() ||
                    clazz.getName().startsWith("java.lang")
            ) {
                isBasicType = true;
            } else {
                isBasicType =  false;
            }
        }

        return isBasicType;
    }

    private static List<String> systemPackages = TObject.asList("java.","jdk.","sun.","javax.","com.sun","com.oracle","javassist");

    /**
     * 判读是否是 JDK 中定义的类(java包下的所有类)
     * @param clazz Class 对象
     * @return true: 是JDK 中定义的类, false:非JDK 中定义的类
     */
    public static boolean isSystemType(Class clazz){

        if( clazz.isPrimitive()){
            return true;
        }

        //排除的包中的 class 不加载
        for(String systemPackage : systemPackages){
            if(getCanonicalName(clazz).startsWith(systemPackage)){
                return true;
            }
        }

        return false;
    }

    /**
     * 判读是否是 JDK 中定义的类(java包下的所有类)
     * @param className Class 对象完全限定名
     * @return true: 是JDK 中定义的类, false:非JDK 中定义的类
     */
    public static boolean isSystemType(String className) {
        if(className.indexOf(Global.STR_POINT)==-1){
            return true;
        }

        //排除的包中的 class 不加载
        for(String systemPackage : systemPackages){
            if(className.startsWith(systemPackage)){
                return true;
            }
        }

        return false;
    }

    /**
     * 获得装箱类型
     * @param primitiveType 原始类型
     * @return 装箱类型
     */
    public static String getPackageType(String primitiveType){
        switch (primitiveType){
            case "int": return "java.lang.Integer";
            case "byte": return "java.lang.Byte";
            case "short": return "java.lang.Short";
            case "long": return "java.lang.Long";
            case "float": return "java.lang.Float";
            case "double": return "java.lang.Double";
            case "char": return "java.lang.Character";
            case "boolean": return "java.lang.Boolean";
            default : return null;
        }
    }
}

