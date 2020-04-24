package com.lwy.factory;

import com.lwy.exception.MyException;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * XML配置依赖注入完整版
 */
public class XmlBeanFactory {

    String xmlName;

    // 最终解析完XML后，所有对象的集合（包括依赖关系）
    Map<String, Object> instancedMap = new HashMap<String, Object>();

    /**
     * 提供构造方法，传入需要解析的xml名称
     *
     * @param xmlName
     */
    public XmlBeanFactory(String xmlName) {
        this.xmlName = xmlName;
        parseXml(xmlName);
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
     * 解析xml文件，实例化对象，并且完成依赖注入
     * 1、得到根节点<beans>下面所有的<bean>
     * 2、循环<bean>，如果<bean>下面还有子标签，则先存起来，之后再进行处理，
     * 因为可能这个bean所需要引入的依赖在xml配置中在他位置后面
     * 3、<bean>下面没有子标签的情况下，判断是否自动装配
     * 4、如果是自动装配，且配置了自动装配的方式且有属性需要装配，那么也存起来，之后再进行处理，原因同2，
     * 没有属性需要自动装配，直接实例化
     * 没有配置自动装配方式，直接实例化
     * 5、如果不是自动装配，直接实例化
     * 6、循环完<bean>之后，写一个死循环，里面去循环等待实例化的<bean>
     * 之所以写死循环，是因为等待实例化的bean中，他所依赖的bean可能也在等待实例化，同时顺序在他后面
     *
     * @param xmlName
     */
    public void parseXml(String xmlName) {

        // 获取根目录下的xml文件
        File xmlFile = new File(this.getClass().getResource("/").getPath() + "//" + xmlName);

        // 使用dom4j解析xml
        SAXReader saxReader = new SAXReader();

        try {

            // 开始解析xml
            Document document = saxReader.read(xmlFile);

            // 获取根节点----<beans>
            Element rootElement = document.getRootElement();

            // 获取是否配置了自动装配
            Attribute injectTypeAttribute = rootElement.attribute("injectType");

            // 是否自动装配
            boolean autoInjectFlag = false;

            // 自动装配方式
            String autoInjectType = "";

            if (injectTypeAttribute != null) { // 配置了自动装配，标示位为true
                autoInjectFlag = true;
                autoInjectType = injectTypeAttribute.getValue();
            }

            // 定义等待实例化的对象map，等不需要依赖注入的对象实例化完成以后，再进行实例化
            Map<String, Element> waitForInstanceMap = new HashMap<String, Element>();

            // 获取根节点下的一级子节点，进行循环，完成对象实例化以及依赖注入
            for (Iterator<Element> childElementFirst = rootElement.elementIterator(); childElementFirst.hasNext(); ) {

                // 获取一级子节点----<bean>
                Element childElementFirstLevel = childElementFirst.next();

                // 获取id属性，将id值赋给beanName
                String beanName = childElementFirstLevel.attributeValue("id");

                // 获取class属性，得到class的全类名
                String className = childElementFirstLevel.attributeValue("class");

                // 获取类的类对象
                Class instanceClass = Class.forName(className);

                // 定义即将实例化的对象
                Object instanceObject = null;

                // 判断当前一级子节点下是否还有二级子节点<property>或者<constructor-arg>，决定是否需要手动依赖注入
                List<Element> childElementSecondList = childElementFirstLevel.elements();

                if (childElementSecondList != null && childElementSecondList.size() > 0) { // 还有二级子节点的情况下，先存起来，之后处理

                    /**
                     * 先存起来的意义在于解决xml中配置顺序问题，解决A中需要依赖B，那么B必须先实例化的问题
                     */
                    waitForInstanceMap.put(beanName, childElementFirstLevel);

                } else { // 没有二级子节点的情况下，判断是否自动装配

                    if (autoInjectFlag) { // 自动装配情况下，判断是否有属性需要自动装配

                        if (autoInjectType.equals("byType") || autoInjectType.equals("byName")) { // 配置了自动装配方式

                            // 判断是否有依赖需要注入
                            Field[] autoByTypeFields = instanceClass.getDeclaredFields();

                            if (autoByTypeFields != null && autoByTypeFields.length > 0) { // 有属性需要自动装配，之后处理

                                waitForInstanceMap.put(beanName, childElementFirstLevel);

                            } else { // 无属性需要自动装配，直接实例化对象

                                instanceObject = instanceClass.newInstance();

                                // 实例化好的bean放入容器放入
                                putInstancedObjectIntoContainer(beanName, instanceObject);

                            }

                        } else { // 没有配置自动装配方式，直接实例化对象

                            instanceObject = instanceClass.newInstance();

                            // 实例化好的bean放入容器放入
                            putInstancedObjectIntoContainer(beanName, instanceObject);

                        }

                    } else { // 没有二级子节点，也非自动装配，直接实例化对象

                        instanceObject = instanceClass.newInstance();

                        // 实例化好的bean放入容器放入
                        putInstancedObjectIntoContainer(beanName, instanceObject);

                    }

                }

            }

            boolean flag = true;

            // 存放key值，遍历完一次后进行移除
            List<String> beanNameList = new ArrayList<String>();
            /**
             * 写死循环，为了让waitForInstanceMap中对象全部实例化成功
             * 因为waitForInstanceMap中对象也可能存在A依赖B，但B的顺序在A后面
             */
            while (flag) {
                // 处理等待实例化的一级子标签
                for (String beanName : waitForInstanceMap.keySet()) {

                    // 获取一级子节点bean
                    Element childElementFirstLevel = waitForInstanceMap.get(beanName);

                    // 获取class属性，得到class的全类名
                    String className = childElementFirstLevel.attributeValue("class");

                    // 获取类的类对象
                    Class instanceClass = Class.forName(className);

                    // 定义即将实例化的对象
                    Object instanceObject = null;

                    // 判断当前一级子节点下是否还有二级子节点<property>或者<constructor-arg>，决定是否需要手动依赖注入
                    List<Element> childElementSecond = childElementFirstLevel.elements();

                    if (childElementSecond != null && childElementSecond.size() > 0) { // 有二级子节点，手动装配

                        instanceObject = instanceObjectByXmlElement(childElementSecond, instanceObject,
                                instanceClass, beanName, waitForInstanceMap);

                    } else { // 没有二级子节点，自动装配

                        if (autoInjectType.equals("byType")) { // 根据类型自动装配

                            instanceObject = autoInstanceObjectByType(instanceObject,
                                    instanceClass, beanName, waitForInstanceMap);

                        } else if (autoInjectType.equals("byName")) { // 根据名称自动装配

                            instanceObject = autoInstanceObjectByName(instanceObject,
                                    instanceClass, beanName, waitForInstanceMap);

                        } else { // 这里应该不会出现这种情况，除非代码有问题

                            throw new MyException("-----程序内部错误！");

                        }

                    }

                    if (instanceObject == null) { // 对象未被实例化，跳出本次循环进行下一次

                        continue;

                    } else {

                        // 实例化好的bean放入容器放入
                        putInstancedObjectIntoContainer(beanName, instanceObject);

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
     * 将实例化好的bean放到容器中
     *
     * @param beanName       实例名称
     * @param instanceObject 实例对象
     * @throws MyException
     */
    public void putInstancedObjectIntoContainer(String beanName, Object instanceObject) throws MyException {

        // 判断容器中是否已有该name的bean
        if (instancedMap.get(beanName) != null) {

            throw new MyException(beanName + "-----在容器中已存在！");

        } else {

            // 将实例化后的对象放入map
            instancedMap.put(beanName, instanceObject);

        }

    }

    /**
     * 根据标签，手动装配
     * 1、循环二级子标签，如果标签引用的对象不在已经实例化好的bean中，也不在等待实例化的bean中，那么报错。
     * 如果在未实例化的bean中，则终止二级子标签循环，返回空，回到上层循环中，进行continue
     * 如果在已实例化好的bean中，进行以下操作：
     * （1）prop标签情况下，从容器中取出所有prop标签引用的对象，放入一个map中，最后处理
     * 因为可能有多个prop标签，而且cons标签与prop标签可能同时存在，得先处理cons标签，然后通过构造方法得到对象，才能再进行setter
     * （2）cons标签情况下，从容器中取出所有cons标签引用的对象，放入list中，同时取出他们所对应的类型，
     * 也放入一个list中
     * 2、判断是否需要通过有参的构造方法去实例化对象，之后进行实例化
     * 3、将需要set的属性set进去
     * 4、返回实例化对象
     *
     * @param childElementSecond 二级子标签集合
     * @param instanceObject     即将实例化的对象
     * @param instanceClass      即将实例化对象的类
     * @param beanName           实例名称
     * @param waitForInstanceMap 等待实例化的对象集合
     * @return
     * @throws Exception
     */
    public Object instanceObjectByXmlElement(List<Element> childElementSecond,
                                             Object instanceObject,
                                             Class instanceClass,
                                             String beanName,
                                             Map<String, Element> waitForInstanceMap) throws Exception {

        // 定义构造方法注入的实例对象数组
        List<Object> constructInjectObjectList = new ArrayList<Object>();

        // 定义构造方法参数类型数组
        List<Class> constructInjectObjectClassList = new ArrayList<Class>();

        // 定义存放property标签需要setter注入的对象以及其对应的属性的map，之后统一注入，因为可能会有多个setter注入
        Map<Field, Object> setterObjectMap = new HashMap<Field, Object>();

        // 判断要注入的对象是否在等待实例化的waitForInstanceMap中
        boolean injectObjectInWaitMapFlag = false;

        for (int i = 0; i < childElementSecond.size(); i++) {

            // 获取一级子节点----<property>或者<constructor-arg>
            Element childElementSecondLevel = childElementSecond.get(i);

            // 获取name属性的值
            String nameValue = childElementSecondLevel.attributeValue("name");

            // 获取ref属性的值
            String refValue = childElementSecondLevel.attributeValue("ref");

            // 从map中获取已实例化过的将要注入的对象
            Object injectObject = instancedMap.get(refValue);

            if (injectObject == null) { // 需要注入的属性不在已经实例化的对象中

                if (waitForInstanceMap.get(refValue) == null) { // 也不在等待实例化的对象中

                    throw new Exception(beanName + "-----容器中无[" + refValue + "]对象！");

                } else { // 在等待实例化的对象中，终止本次子标签循环，在外层死循环中进行下一个循环

                    injectObjectInWaitMapFlag = true;

                    break;

                }

            }

            // 获取被注入对象的属性
            Field field = instanceClass.getDeclaredField(nameValue);

            if (field != null) { // 被注入对象有该属性，判断该属性类型与注入对象类型是否相同

                // 注入对象实现的接口类型，之所以这么取，是因为属性的类型是接口类型的，例如UserDao.class
                Class injectClass = injectObject.getClass().getInterfaces()[0];

                // 获取属性类型
                Class fieldClass = field.getType();

                // 判断属性类型与注入的对象类型是否相同
                if (injectClass.getName().equals(fieldClass.getName())) {

                    if (childElementSecondLevel.getName().equals("property")) { // property情况下，setter注入

                        // 将符合条件的属性先记录起来
                        setterObjectMap.put(field, injectObject);

                    } else if (childElementSecondLevel.getName().equals("constructor-arg")) { // constructor-arg情况下，构造方法注入

                        // 将符合条件的属性先记录起来
                        constructInjectObjectList.add(injectObject);

                        constructInjectObjectClassList.add(injectClass);

                    } else { // 两种注入方式都不是，报错

                        throw new MyException(beanName + "-----无[" + childElementSecondLevel.getName() + "]此种注入方式！");

                    }

                } else {

                    throw new MyException(beanName + "-----无名称为[" + nameValue + "]类型为[" + injectClass + "]的属性");

                }

            } else { // 没有该属性，报错

                throw new MyException(beanName + "-----无名称为[" + nameValue + "]属性，请确认！");

            }

        }

        if (!injectObjectInWaitMapFlag) { // 不在等待初始化的waitForInstanceMap中，实例化对象

            // 此处处理构造方法注入
            if (constructInjectObjectList.size() > 0) { // 有构造方法注入，通过构造方法去实例化目标对象

                // list转换为数组
                Object[] constructInjectObjects = constructInjectObjectList.toArray();

                Class[] constructInjectObjectClasses = new Class[constructInjectObjectClassList.size()];

                constructInjectObjectClassList.toArray(constructInjectObjectClasses);

                // 获取构造方法
                Constructor constructor = instanceClass.getConstructor(constructInjectObjectClasses);

                // 根据构造方法实例化对象
                instanceObject = constructor.newInstance(constructInjectObjects);

            } else { // 无构造方法注入，直接实例化对象

                instanceObject = instanceClass.newInstance();

            }

            // 处理setter注入的属性
            for (Field setterField : setterObjectMap.keySet()) {

                Object setterInjectObject = setterObjectMap.get(setterField);

                setterField.setAccessible(true);
                setterField.set(instanceObject, setterInjectObject);

            }

        }

        return instanceObject;
    }

    /**
     * 根据类型，自动装配
     * 1、循环目标类的属性
     * 2、在已实例化的map中找符合属性类型的对象
     * （1）没有找到，去找未实例化的，未实例化的找到1个，则终止属性循环，返回空，回到上层循环中，进行continue；
     * 找到多个，报错；没有找到，报错。
     * （2）找到，判断找到的个数，大于1个报错。等于一个时，也需要去未实例化的里面去找，如果能找到，也需要报错
     * 3、正常情况下，只有已实例化里有一个符合类型的，然后实例化对象，返回
     *
     * @param instanceObject     即将实例化的对象
     * @param instanceClass      即将实例化对象的类
     * @param beanName           实例名称
     * @param waitForInstanceMap 等待实例化的对象集合
     * @return
     * @throws Exception
     */
    public Object autoInstanceObjectByType(Object instanceObject,
                                           Class instanceClass,
                                           String beanName,
                                           Map<String, Element> waitForInstanceMap) throws Exception {

        // 定义存放自动装配需要setter注入的对象以及其对应的属性的map，之后统一注入，因为可能会有多个属性需要自动装配
        Map<Field, Object> autoByTypeInjectObjectMap = new HashMap<Field, Object>();

        // 需要依赖注入的属性
        Field[] autoByTypeFields = instanceClass.getDeclaredFields();

        // 判断要注入的对象是否在等待实例化的waitForInstanceMap中
        boolean injectObjectInWaitMapFlag = false;

        // 遍历属性
        for (Field autoByTypeField : autoByTypeFields) {

            // 得到属性类型
            Class autoByTypeFieldClass = autoByTypeField.getType();

            /**
             * 因为是byType，所以需要遍历map，取出其中所有的已实例化的对象，
             * 判断类型是否匹配
             */

            // 记录map中符合条件的对象，因为可能会找到多个，多个的话需要报错
            int instancedCount = 0;

            Object autoByTypeInjectObject = null;

            // 遍历已实例化map，寻找符合类型的对象
            for (String key : instancedMap.keySet()) {

                Class autoByTypeTempObject = instancedMap.get(key).getClass().getInterfaces()[0];

                // 判断map中对象实现的接口类型和属性类型是否相同
                if (autoByTypeTempObject.getName().equals(autoByTypeFieldClass.getName())) {

                    autoByTypeInjectObject = instancedMap.get(key);

                    instancedCount++;

                }

            }

            if (instancedCount == 0) { // 已实例化的对象中没有该种类型的对象，找未实例化的对象中有没有

                // 记录waitForInstanceMap中符合条件的对象，因为可能会找到多个，多个的话需要报错
                int waitInstanceCount = 0;

                // 遍历未实例化waitForInstanceMap，寻找符合类型的对象
                for (String key : waitForInstanceMap.keySet()) {

                    // waitForInstanceMap中存的是一级子标签，所以获取到class对应的值就是全类名
                    String autoByTypeTempClassName = waitForInstanceMap.get(key).attributeValue("class");

                    // 判断waitForInstanceMap中一级子标签配置的类的类型和属性类型是否相同
                    if (autoByTypeTempClassName.equals(autoByTypeFieldClass.getName())) {

                        waitInstanceCount++;

                    }

                }

                if (waitInstanceCount == 0) {

                    throw new MyException(beanName + "-----容器中无[" + autoByTypeFieldClass + "]类型对象！");

                } else if (waitInstanceCount > 1) {

                    throw new MyException(beanName + "-----需要一个[" + autoByTypeFieldClass + "]类型对象，找到多个！");

                } else {

                    injectObjectInWaitMapFlag = true;

                    break;

                }

            } else if (instancedCount > 1) { // 找到大于一个情况下，报错

                throw new MyException(beanName + "-----需要一个[" + autoByTypeFieldClass + "]类型对象，找到多个！");

            } else { // 已实例化对象中有一个符合类型的对象，再判断未实例化中有没有，有的话同样报错，没有的话正常

                // 遍历未实例化waitForInstanceMap，寻找符合类型的对象
                for (String key : waitForInstanceMap.keySet()) {

                    // waitForInstanceMap中存的是一级子标签，所以获取到class对应的值就是全类名
                    String autoByTypeTempClassName = waitForInstanceMap.get(key).attributeValue("class");

                    // 判断waitForInstanceMap中一级子标签配置的类的类型和属性类型是否相同
                    if (autoByTypeTempClassName.equals(autoByTypeFieldClass.getName())) {

                        throw new MyException(beanName + "-----需要一个[" + autoByTypeFieldClass + "]类型对象，找到多个！");

                    }

                }

                autoByTypeInjectObjectMap.put(autoByTypeField, autoByTypeInjectObject);

            }

        }

        if (!injectObjectInWaitMapFlag) { // 不在等待初始化的waitForInstanceMap中，实例化对象

            // 遍历完成后，进行自动装配
            instanceObject = instanceClass.newInstance();

            // 处理自动装配的属性
            for (Field autoByTypeField : autoByTypeInjectObjectMap.keySet()) {

                Object autoByTypeInjectObject = autoByTypeInjectObjectMap.get(autoByTypeField);

                autoByTypeField.setAccessible(true);
                autoByTypeField.set(instanceObject, autoByTypeInjectObject);

            }

        }

        return instanceObject;

    }

    /**
     * 根据名称，自动装配
     * 1、循环目标类的属性
     * 2、在已实例化的对象中，根据属性名称去找对应的对象
     * （1）如果找到，匹配类型，相同的情况下，正常实例化返回；不同的情况下，报错
     * （2）未找到，去未实例化的对象中找，如果找到，匹配类型，相同情况下，则终止属性循环，返回空，回到上层循环中，进行continue；
     * 不相同情况下，报错
     * 3、正常情况下，只有已实例化里有一个符合类型的，然后实例化对象，返回
     * 4、注意：根据名称只可能找到一个，因为再往容器中放的时候（putInstancedObjectIntoContainer方法）判断过，
     * 相同名字的往里面放会报错
     *
     * @param instanceObject     即将实例化的对象
     * @param instanceClass      即将实例化对象的类
     * @param beanName           实例名称
     * @param waitForInstanceMap 等待实例化的对象集合
     * @return
     * @throws Exception
     */
    public Object autoInstanceObjectByName(Object instanceObject,
                                           Class instanceClass,
                                           String beanName,
                                           Map<String, Element> waitForInstanceMap) throws Exception {

        // 定义存放自动装配需要setter注入的对象以及其对应的属性的map，之后统一注入，因为可能会有多个属性需要自动装配
        Map<Field, Object> autoByNameInjectObjectMap = new HashMap<Field, Object>();

        // 需要依赖注入的属性
        Field[] autoByNameFields = instanceClass.getDeclaredFields();

        // 判断要注入的对象是否在等待实例化的waitForInstanceMap中
        boolean injectObjectInWaitMapFlag = false;

        // 遍历属性
        for (Field autoByNameField : autoByNameFields) {

            // 得到属性类型
            Class autoByNameFieldClass = autoByNameField.getType();

            // 根据属性名称得到被注入的对象
            Object autoByNameInjectObject = instancedMap.get(autoByNameField.getName());

            // 已实例化的对象中没有，去未实例化的对象中找
            if (autoByNameInjectObject == null) {

                // 获取到未实例化的一级标签
                Element childElementFirstLevel = waitForInstanceMap.get(autoByNameField.getName());

                if (childElementFirstLevel == null) { // 未实例化的对象中也没有

                    throw new MyException(beanName + "-----容器中没有名称为[" + autoByNameField.getName() + "]类型为[" + autoByNameFieldClass + "]的对象！");

                } else { // 未实例化的对象中有

                    // waitForInstanceMap中存的是一级子标签，所以获取到class对应的值就是全类名
                    String autoByNameTempClassName = childElementFirstLevel.attributeValue("class");

                    // 判断类型是否相同，相同的话，终止循环
                    if (autoByNameTempClassName.equals(autoByNameFieldClass.getName())) {

                        injectObjectInWaitMapFlag = true;

                        break;

                    } else {

                        throw new MyException(beanName + "-----容器中没有名称为[" + autoByNameField.getName() + "]类型为[" + autoByNameFieldClass + "]的对象！");

                    }

                }

            } else { // 已实例化对象中有

                // 容器中被注入对象实现的接口
                Class autoByNameInjectObjectClass = autoByNameInjectObject.getClass().getInterfaces()[0];

                // 判断容器中对象类型与属性类型是否相同
                if (autoByNameFieldClass.getName().equals(autoByNameInjectObjectClass.getName())) {

                    autoByNameInjectObjectMap.put(autoByNameField, autoByNameInjectObject);

                } else {

                    throw new MyException(beanName + "-----容器中没有名称为[" + autoByNameField.getName() + "]类型为[" + autoByNameFieldClass + "]的对象！");

                }

            }


        }

        if (!injectObjectInWaitMapFlag) {

            // 遍历完成后，进行自动装配
            instanceObject = instanceClass.newInstance();

            // 处理自动装配的属性
            for (Field autoByNameField : autoByNameInjectObjectMap.keySet()) {

                Object autoByNameInjectObject = autoByNameInjectObjectMap.get(autoByNameField);

                autoByNameField.setAccessible(true);
                autoByNameField.set(instanceObject, autoByNameInjectObject);

            }

        }

        return instanceObject;

    }
}


