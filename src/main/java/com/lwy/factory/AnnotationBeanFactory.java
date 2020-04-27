package com.lwy.factory;

import com.lwy.annotation.myAnnotation.MyAutowired;
import com.lwy.annotation.myAnnotation.MyController;
import com.lwy.annotation.myAnnotation.MyRepository;
import com.lwy.annotation.myAnnotation.MyService;
import com.lwy.exception.MyException;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 注解配置依赖注入完整版
 *
 * @author lwy
 */
public class AnnotationBeanFactory {

    String basePackage;

    // 最终扫描完包后，所有对象的集合（包括依赖关系）
    Map<String, Object> instancedMap = new HashMap<String, Object>();

    public AnnotationBeanFactory(String basePackage) {
        this.basePackage = basePackage;
        scanAndInstance(basePackage);

        System.out.println(instancedMap);
    }

    /**
     * 获取实例对象
     *
     * @param beanName
     * @return
     */
    public Object getBean(String beanName) {

        return instancedMap.get(beanName);
    }

    /**
     * 扫描包，然后实例化对象
     * 1、以递归的方式，将传入的包下面的所有.class文件对应的全类名（例如com.lwy.dao.UserDaoImpl）得到
     * 2、遍历上面得到的全类名，获取到Class对象
     * 3、判断Class对象有没有被注解标识
     * （1）被注解标识，看注解上有没有配置value值，配置了的话，以value值作为beanName；没有配置的话，以类名首字母小写作为beanName
     * （2）没有被注解标识，说明不需要实例化，跳过
     * 4、判断是否有属性需要依赖注入，有的话先存起来（原因同之前写xml配置的时候的顺序问题），之后处理，没有的话直接实例化，放到容器中
     * 5、将不需要有依赖注入的实例化完后，写一个死循环，里面去循环等待实例化的
     * 之所以写死循环，是因为等待实例化的bean中，他所依赖的bean可能也在等待实例化，同时顺序在他后面
     *
     * @param basePackage 被扫描的包名
     */
    public void scanAndInstance(String basePackage) {

        try {

            // 获取根目录
            String rootPath = this.getClass().getResource("/").getPath();

            // 将包名换成路径格式
            String bathPackagePath = basePackage.replaceAll("\\.", "\\\\");

            // 获取该包对应的文件夹
            File file = new File(rootPath + "//" + bathPackagePath);

            // 获取包文件夹下所有文件，可能有文件，也可能有文件夹
            File[] files = file.listFiles();

            // 定义存储全类名的list
            List<String> fileClassNameList = new ArrayList<String>();

            // 递归，获得包下所有文件的全类名
            fileClassNameList = getAllFileClassName(files, fileClassNameList, basePackage);

            // 定义等待实例化的对象map，等不需要依赖注入的对象实例化完成以后，再进行实例化
            Map<String, Class> waitForInstanceMap = new HashMap<String, Class>();

            // 遍历全类名list，开始实例化对象
            for (String instanceClassName : fileClassNameList) {

                // 获取要实例化的类的类对象
                Class instanceClass = Class.forName(instanceClassName);

                // 定义即将实例化的对象
                Object instanceObject = null;

                // 实例名称
                String beanName = "";

                if (instanceClass.isAnnotationPresent(MyRepository.class)
                        || instanceClass.isAnnotationPresent(MyService.class)
                        || instanceClass.isAnnotationPresent(MyController.class)){

                    // 根据所标注的注解，获取value值作为beanName
                    if (instanceClass.isAnnotationPresent(MyRepository.class)) {

                        MyRepository myRepository = (MyRepository) instanceClass.getAnnotation(MyRepository.class);
                        beanName = myRepository.value();

                    } else if (instanceClass.isAnnotationPresent(MyService.class)) {

                        MyService myService = (MyService) instanceClass.getAnnotation(MyService.class);
                        beanName = myService.value();

                    } else if (instanceClass.isAnnotationPresent(MyController.class)) {

                        MyController myController = (MyController) instanceClass.getAnnotation(MyController.class);
                        beanName = myController.value();

                    }

                    // 如果注解没有配置value值，那么取类名作为beanName，首字母小写
                    if (beanName.equals("")) {

                        // 获取类名
                        String tempName = instanceClass.getSimpleName();

                        beanName = tempName.substring(0, 1).toLowerCase()
                                + tempName.substring(1, tempName.length());

                    }

                    // 判断是否有依赖需要注入
                    Field[] autoInjectFields = instanceClass.getDeclaredFields();

                    // 被标识情况下，判断是否有属性需要被注入
                    if (autoInjectFields != null && autoInjectFields.length > 0) { // 有属性需要依赖注入

                        if (waitForInstanceMap.get(beanName) != null || instancedMap.get(beanName) != null) {

                            throw new MyException(beanName + "-----在容器中已存在！");

                        } else {

                            waitForInstanceMap.put(beanName, instanceClass);

                        }

                    } else { // 没有属性需要依赖注入，直接实例化，放到容器中

                        instanceObject = instanceClass.newInstance();

                        putInstancedObjectIntoContainer(beanName, instanceObject, waitForInstanceMap);

                    }

                }

            }

            // 死循环，遍历未实例化的map，进行实例化
            boolean flag = true;

            // 存放key值，遍历完一次后进行移除
            List<String> beanNameList = new ArrayList<String>();

            while (flag) {
                // 处理等待实例化的对象
                for (String beanName : waitForInstanceMap.keySet()) {

                    Object instanceObject = null;

                    Class instanceClass = waitForInstanceMap.get(beanName);

                    instanceObject = autoInstanceObject(instanceObject,
                            instanceClass, beanName, waitForInstanceMap);

                    if (instanceObject == null) { // 对象未被实例化，跳出本次循环进行下一次

                        continue;

                    } else {

                        // 实例化好的bean放入容器放入
                        instancedMap.put(beanName, instanceObject);

                        beanNameList.add(beanName);

                    }

                }

                for (String beanName : beanNameList) {

                    waitForInstanceMap.remove(beanName);

                }

                beanNameList.clear();

                // waitForInstanceMap中没有值了，证明没有对象需要去实例化了，结束死循环
                if (waitForInstanceMap.size() == 0) {

                    flag = false;

                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 使用递归方式，获取包下所有文件的全类名
     *
     * @param files             要扫描的包下所有的文件夹及文件
     * @param fileClassNameList 存储全类名的list
     * @param basePackage       要扫描的包
     * @return
     */
    public List<String> getAllFileClassName(File[] files, List<String> fileClassNameList, String basePackage) {


        // 使用递归的方式，获取所有的文件，记录文件全类名
        for (File targetFile : files) {

            if (targetFile.isFile()) { // 是文件的情况下，全类名=包名+文件名去掉.class

                String targetFileClassName = (basePackage + "." + targetFile.getName())
                        .replaceAll(".class", "");

                fileClassNameList.add(targetFileClassName);

            } else if (targetFile.isDirectory()) { //是文件夹的情况下，包名后面拼接一下，然后继续往下找

                String basePackageTemp = basePackage + "." + targetFile.getName();

                getAllFileClassName(targetFile.listFiles(), fileClassNameList, basePackageTemp);

            }

        }

        return fileClassNameList;

    }

    /**
     * 将实例化好的bean放到容器中
     *
     * @param beanName       实例名称
     * @param instanceObject 实例对象
     * @throws MyException
     */
    public void putInstancedObjectIntoContainer(String beanName, Object instanceObject, Map<String, Class> waitForInstanceMap) throws MyException {

        // 判断容器中是否已有该name的bean
        if (instancedMap.get(beanName) != null || waitForInstanceMap.get(beanName) != null) {

            throw new MyException(beanName + "-----在容器中已存在！");

        } else {

            // 将实例化后的对象放入map
            instancedMap.put(beanName, instanceObject);

        }

    }

    /**
     * 自动注入，默认byType，符合类型的有多个的话，byName
     * 1、循环要实例化的类的属性
     * 2、判断属性是否需要被注入，不需要注入的话进行下一个
     * 3、需要注入的情况下
     *  （1）首先去已实例化的bean中寻找类型符合的
     *      （1）已实例化的bean中没有符合类型的，再去未实例化的bean中寻找
     *          （1）未实例化的里面也没有符合类型的，报错
     *          （2）未实例化的里面有多个符合类型的，那么再根据属性名称去寻找，如果找到一个，那么先跳出属性循环，回到上层循环continue；没找到，报错
     *          （3）未实例化的里面有1个符合类型的，那么先跳出属性循环，回到上层循环continue
     *      （2）已实例化的bean中有多个符合类型的
     *          （1）先在已实例化的里面根据属性名寻找，如果能找到，那么就准备注入这个找到的
     *          （2）如果没找到，再在未实例化的里面找
     *              （1）未实例化的里面没有符合类型的，报错
     *              （2）未实例化的里面有多个符合类型的，那么再根据属性名称去寻找，如果找到一个，那么先跳出属性循环，回到上层循环continue；没找到，报错
     *              （3）未实例化的里面有1个符合类型的，那么判断名称是否符合，符合的话先跳出属性循环，回到上层循环continue；不符合，报错
     *      （3）已实例化的bean中有1个符合类型的
     *          （1）看未实例化的里面有没有符合类型的，如果没有，那么就准备注入这个
     *          （2）如果有，不管有几个，都判断是否符合属性名
     *              （1）如果已实例化的这个符合属性名，那么就准备注入这个
     *              （2）如果未实例化的符合属性名，那么先跳出属性循环，回到上层循环continue
     *              （3）都不符合，报错
     * 4、属性都能找到需要注入的对象，那么正常实例化
     *
     * @param instanceObject     即将实例化的对象
     * @param instanceClass      即将实例化对象的类
     * @param beanName           实例名称
     * @param waitForInstanceMap 等待实例化的对象集合
     * @return
     * @throws Exception
     */
    public Object autoInstanceObject(Object instanceObject,
                                     Class instanceClass,
                                     String beanName,
                                     Map<String, Class> waitForInstanceMap) throws Exception {

        // 定义存放自动装配需要setter注入的对象以及其对应的属性的map，之后统一注入，因为可能会有多个属性需要自动装配
        Map<Field, Object> autoInjectObjectMap = new HashMap<Field, Object>();

        // 获取需要注入的属性
        Field[] autoInjectFields = waitForInstanceMap.get(beanName).getDeclaredFields();

        // 判断要注入的对象是否在等待实例化的waitForInstanceMap中
        boolean injectObjectInWaitMapFlag = false;

        // 遍历属性
        for (Field autoInjectField : autoInjectFields) {

            // 属性被注解标识，证明需要注入
            if (autoInjectField.isAnnotationPresent(MyAutowired.class)) {

                // 得到属性类型
                Class autoInjectFieldClass = autoInjectField.getType();

                // 得到属性名称
                String autoInjectFieldName = autoInjectField.getName();

                Object autoInjectObject = null;

                /*
                 * 首先byType，byType找到多个的话，byName
                 */

                // 用于暂时存放instancedMap中符合类型的对象，因为可能存在符合类型条件的有多条，
                // 但是符合名称条件的不在这几条中，而是在其他不符合类型的实例中，所以需要把符合类型条件的先存起来，然后再从里面找符合名称条件的
                Map<String, Object> instancedTempMap = new HashMap<String, Object>();

                // 遍历已实例化map，寻找符合类型的对象
                for (String key : instancedMap.keySet()) {

                    Class autoInjectTempClass = instancedMap.get(key).getClass().getInterfaces()[0];

                    // 判断map中对象实现的接口类型和属性类型是否相同
                    if (autoInjectTempClass.getName().equals(autoInjectFieldClass.getName())) {

                        autoInjectObject = instancedMap.get(key);

                        instancedTempMap.put(key, autoInjectObject);

                    }

                }

                if (instancedTempMap == null || instancedTempMap.size() == 0) { // 已实例化的对象中没有该种类型的对象，找未实例化的对象中有没有

                    Map<String, Class> waitForInstanceTempMap = findInWaitForInstanceMap(waitForInstanceMap, autoInjectFieldClass);

                    if (waitForInstanceTempMap == null || waitForInstanceTempMap.size() == 0) { // 未实例化中也没有

                        throw new MyException(beanName + "-----容器中无[" + autoInjectFieldClass + "]类型对象！");

                    } else if (waitForInstanceTempMap.size() > 1) { // 有多个，再byName

                        if (waitForInstanceTempMap.get(autoInjectFieldName) != null) { // byName可以取到

                            injectObjectInWaitMapFlag = true;

                            break;


                        } else { // 取不到

                            throw new MyException(beanName + "-----容器中有多个类型为[" + autoInjectFieldClass + "]的对象，但无名称为[" + autoInjectFieldName + "]的！");

                        }

                    } else { // 只有一个，跳出属性循环

                        injectObjectInWaitMapFlag = true;

                        break;

                    }

                } else if (instancedTempMap.size() > 1) { // 已实例化对象中有多个符合类型的，先byName

                    if (instancedTempMap.get(autoInjectFieldName) != null) { // byName能找到，那就注入这个

                        autoInjectObjectMap.put(autoInjectField, instancedTempMap.get(autoInjectFieldName));

                    } else { // byName找不到，去未实例化的里面找

                        Map<String, Class> waitForInstanceTempMap = findInWaitForInstanceMap(waitForInstanceMap, autoInjectFieldClass);

                        if (waitForInstanceTempMap == null || waitForInstanceTempMap.size() == 0) { // 未实例化中没有

                            throw new MyException(beanName + "-----容器中有多个类型为[" + autoInjectFieldClass + "]的对象，但无名称为[" + autoInjectFieldName + "]的！");

                        } else if (waitForInstanceTempMap.size() > 1) { // 有多个，再byName

                            if (waitForInstanceTempMap.get(autoInjectFieldName) != null) { // byName可以取到

                                injectObjectInWaitMapFlag = true;

                                break;


                            } else { // 取不到

                                throw new MyException(beanName + "-----容器中有多个类型为[" + autoInjectFieldClass + "]的对象，但无名称为[" + autoInjectFieldName + "]的！");

                            }

                        } else { // 未实例化中有一个，判断名称是否符合

                            if (waitForInstanceTempMap.get(autoInjectFieldName) != null) {

                                injectObjectInWaitMapFlag = true;

                                break;

                            } else {

                                throw new MyException(beanName + "-----容器中有多个类型为[" + autoInjectFieldClass + "]的对象，但无名称为[" + autoInjectFieldName + "]的！");

                            }


                        }

                    }

                } else { // 已实例化对象中有一个符合类型的对象，判断未实例化里有没有，有的话byName，没有的话就是这个

                    Map<String, Class> waitForInstanceTempMap = findInWaitForInstanceMap(waitForInstanceMap, autoInjectFieldClass);

                    if (waitForInstanceTempMap == null || waitForInstanceTempMap.size() == 0) { // 未实例化中没有符合类型的，那么就注入这个

                        // 这里需要把这个map中唯一的值取出来，因为他的名称可能和属性名不一样，所以需要这样取
                        autoInjectObjectMap.put(autoInjectField, instancedTempMap.get(instancedTempMap.keySet().iterator().next()));

                    } else { // 未实例化的里面有，不管一个还是多个，都得分别byName判断

                        if (instancedTempMap.get(autoInjectFieldName) != null) { // 已实例化中的符合名称，那么就注入他

                            autoInjectObjectMap.put(autoInjectField, instancedTempMap.get(autoInjectFieldName));

                        } else if (waitForInstanceTempMap.get(autoInjectFieldName) != null) { // 未实例化的中符合名称，结束属性循环

                            injectObjectInWaitMapFlag = true;

                            break;

                        } else {

                            throw new MyException(beanName + "-----容器中有多个类型为[" + autoInjectFieldClass + "]的对象，但无名称为[" + autoInjectFieldName + "]的！");

                        }

                    }

                }

            }

        }

        if (!injectObjectInWaitMapFlag) { // 不在等待初始化的waitForInstanceMap中，实例化对象

            // 遍历完成后，进行自动装配
            instanceObject = instanceClass.newInstance();

            // 处理自动装配的属性
            for (Field autoInjectField : autoInjectObjectMap.keySet()) {

                Object autoInjectObject = autoInjectObjectMap.get(autoInjectField);

                autoInjectField.setAccessible(true);
                autoInjectField.set(instanceObject, autoInjectObject);

            }

        }

        return instanceObject;

    }

    /**
     * 看要注入的属性的对象是否在未实例化map中
     * 1、根据类型，在waitForInstanceMap里找符合属性类型的
     * 2、符合的存起来返回
     *
     * @param waitForInstanceMap   等待实例化的对象集合
     * @param autoInjectFieldClass 等待注入的属性的类型
     * @return
     * @throws Exception
     */
    public Map<String, Class> findInWaitForInstanceMap(Map<String, Class> waitForInstanceMap,
                                                       Class autoInjectFieldClass) throws Exception {

        // 用于暂时存放waitForInstanceMap中符合类型的对象，因为可能存在符合类型条件的有多条，
        // 但是符合名称条件的不在这几条中，所以需要把符合类型条件的先存起来，然后再从里面找符合名称条件的
        Map<String, Class> waitForInstanceTempMap = new HashMap<String, Class>();

        // 遍历未实例化waitForInstanceMap，寻找符合类型的对象
        for (String key : waitForInstanceMap.keySet()) {

            // waitForInstanceMap中存的是类对象，所以获取到实现的接口再getName对应的值就是全类名
            String autoInjectTempClassName = waitForInstanceMap.get(key).getInterfaces()[0].getName();

            // 判断waitForInstanceMap中类的类型和属性类型是否相同
            if (autoInjectTempClassName.equals(autoInjectFieldClass.getName())) {

                waitForInstanceTempMap.put(key, waitForInstanceMap.get(key));

            }

        }

        return waitForInstanceTempMap;
    }

}
