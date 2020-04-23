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

public class BeanFactory {

    // 最终解析完XML后，所有对象的集合（包括依赖关系）
    Map<String, Object> map = new HashMap<String, Object>();

    /**
     * 提供构造方法，传入需要解析的xml名称
     *
     * @param xmlName
     */
    public BeanFactory(String xmlName) {
        parseXml(xmlName);
    }

    /**
     * 解析xml文件，实例化对象，并且完成依赖注入
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

            boolean autoInjectFlag = false;

            if (injectTypeAttribute != null) { // 配置了自动装配，标示位为true
                autoInjectFlag = true;
            }

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

                // 判断当前一级子节点下是否还有二级子节点，决定是否需要依赖注入
                List<Element> childElementSecond = childElementFirstLevel.elements();

                if (childElementSecond != null && childElementSecond.size() > 0) { // 还有二级子节点的情况下，进行依赖注入，手动装配

                    // 定义存放constructor-arg的list，之后统一处理，因为构造方法中可以注入多个对象
                    List<Element> constructElementList = new ArrayList<Element>();

                    // 定义存放property标签需要setter注入的对象以及其对应的属性的map，之后统一注入，因为可能会有多个setter注入
                    Map<Field, Object> setterObjectMap = new HashMap<Field, Object>();

                    for (int i = 0; i < childElementSecond.size(); i++) {
                        // 获取一级子节点----<property>或者<constructor-arg>
                        Element childElementSecondLevel = childElementSecond.get(i);

                        if (childElementSecondLevel.getName().equals("property")) { // property情况下，setter注入

                            // 获取name属性的值
                            String setterNameValue = childElementSecondLevel.attributeValue("name");

                            // 获取ref属性的值
                            String setterRefValue = childElementSecondLevel.attributeValue("ref");

                            // 从map中获取已实例化过的将要注入的对象
                            /**
                             * 这里存在一个问题，必须保证顺序，得先实例化好要注入的bean，然后才能实例化被注入的对象
                             */
                            Object setterInjectObject = map.get(setterRefValue);

                            // 获取被注入对象的属性
                            Field setterField = instanceClass.getDeclaredField(setterNameValue);

                            if (setterField != null) { // 被注入对象有该属性，放进map，之后统一处理

                                setterObjectMap.put(setterField, setterInjectObject);

                            } else { // 没有该属性，报错

                                throw new MyException(beanName + "-----无[" + setterNameValue + "]属性，请确认！");

                            }

                        } else if (childElementSecondLevel.getName().equals("constructor-arg")) { // constructor-arg情况下，构造方法注入

                            constructElementList.add(childElementSecondLevel);

                        } else { // 两种注入方式都不是，报错

                            throw new MyException(beanName + "-----无[" + childElementSecondLevel.getName() + "]此种注入方式！");

                        }
                    }

                    // 此处处理构造方法注入
                    if (constructElementList.size() > 0) { // 有构造方法注入，通过构造方法去实例化目标对象

                        // 定义构造方法注入的实例对象数组
                        List<Object> constructInjectObjectList = new ArrayList<Object>();

                        // 定义构造方法参数类型数组
                        List<Class> constructInjectObjectClassList = new ArrayList<Class>();

                        for (int i = 0; i < constructElementList.size(); i++) {

                            // 获取constructor-arg标签
                            Element constructElement = constructElementList.get(i);

                            // 获取constructor-arg中name的值
                            String constructNameValue = constructElement.attributeValue("name");

                            // 获取constructor-arg中ref的值
                            String constructRefValue = constructElement.attributeValue("ref");

                            // 注入的对象
                            Object constructInjectObject = map.get(constructRefValue);

                            // 注入对象实现的接口类型，之所以这么取，是因为属性的类型是接口类型的，例如UserDao.class
                            Class constructInjectClass = constructInjectObject.getClass().getInterfaces()[0];

                            // 获取name对应的属性
                            Field constructField = instanceClass.getDeclaredField(constructNameValue);

                            // 获取属性类型
                            Class constructFieldClass = constructField.getType();

                            // 判断属性类型与注入的对象类型是否相同
                            if (constructInjectClass.getName().equals(constructFieldClass.getName())) {

                                // 将符合条件的属性先记录起来
                                constructInjectObjectList.add(constructInjectObject);

                                constructInjectObjectClassList.add(constructInjectClass);

                            } else {
                                throw new MyException(beanName + "-----无名称为[" + constructNameValue + "]类型为[" + constructInjectClass + "]的属性");
                            }

                        }

                        // list转换为数组
                        Object[] injectObjects = constructInjectObjectList.toArray();

                        Class[] injectObjectClasses = new Class[constructInjectObjectClassList.size()];

                        constructInjectObjectClassList.toArray(injectObjectClasses);

                        // 获取构造方法
                        Constructor constructor = instanceClass.getConstructor(injectObjectClasses);

                        // 根据构造方法实例化对象
                        instanceObject = constructor.newInstance(injectObjects);

                    } else { // 无构造方法注入，直接实例化对象

                        instanceObject = instanceClass.newInstance();

                    }

                    // 处理setter注入的属性
                    for (Field setterField : setterObjectMap.keySet()) {

                        Object setterInjectObject = setterObjectMap.get(setterField);

                        setterField.setAccessible(true);
                        setterField.set(instanceObject, setterInjectObject);

                    }


                } else { // 没有二级子节点的情况下，判断是否自动装配

                    if (autoInjectFlag) { // 自动装配情况下，根据自动装配方式装配

                        if (injectTypeAttribute.getValue().equals("byType")) { // byType自动装配

                            // 判断是否有依赖需要注入
                            Field[] autoByTypeFields = instanceClass.getDeclaredFields();

                            // 定义存放自动装配需要setter注入的对象以及其对应的属性的map，之后统一注入，因为可能会有多个属性需要自动装配
                            Map<Field, Object> autoByTypeObjectMap = new HashMap<Field, Object>();

                            // 遍历属性
                            for (Field autoByTypeField : autoByTypeFields) {

                                // 得到属性类型
                                Class autoByTypeInjectClass = autoByTypeField.getType();

                                /**
                                 * 因为是byType，所以需要遍历map，取出其中所有的已实例化的对象，
                                 * 判断类型是否匹配
                                 */

                                // 记录map中符合条件的对象，因为可能会找到多个，多个的话需要报错
                                int count = 0;

                                Object autoByTypeInjectObject = null;

                                // 遍历map，寻找符合类型的对象
                                for (String key : map.keySet()) {

                                    Class autoByTypeTempObject = map.get(key).getClass().getInterfaces()[0];

                                    // 判断map中对象实现的接口类型和属性类型是否相同
                                    if (autoByTypeTempObject.getName().equals(autoByTypeInjectClass.getName())) {

                                        autoByTypeInjectObject = map.get(key);

                                        count++;

                                    }

                                }

                                if (count > 1) { // 找到大于一个情况下，报错

                                    throw new MyException(beanName + "-----需要一个[" + autoByTypeInjectClass + "]对象，找到多个！");

                                } else {

                                    autoByTypeObjectMap.put(autoByTypeField, autoByTypeInjectObject);

                                }

                            }

                            // 遍历完成后，进行自动装配
                            instanceObject = instanceClass.newInstance();

                            // 处理自动装配的属性
                            for (Field autoByTypeField : autoByTypeObjectMap.keySet()) {

                                Object autoByTypeInjectObject = autoByTypeObjectMap.get(autoByTypeField);

                                autoByTypeField.setAccessible(true);
                                autoByTypeField.set(instanceObject, autoByTypeInjectObject);

                            }


                        } else if (injectTypeAttribute.getValue().equals("byName")) { // byName自动装配

                            // 判断是否有依赖需要注入
                            Field[] autoByNameFields = instanceClass.getDeclaredFields();

                            // 定义存放自动装配需要setter注入的对象以及其对应的属性的map，之后统一注入，因为可能会有多个属性需要自动装配
                            Map<Field, Object> autoByNameObjectMap = new HashMap<Field, Object>();

                            // 遍历属性
                            for (Field autoByNameField : autoByNameFields) {

                                // 得到属性类型
                                Class autoByNameInjectClass = autoByNameField.getType();

                                // 根据属性名称得到被注入的对象
                                Object autoByNameObject = map.get(autoByNameField.getName());

                                // 容器中被注入对象实现的接口
                                Class autoByNameObjectClass = autoByNameObject.getClass().getInterfaces()[0];

                                // 判断容器中对象类型与属性类型是否相同
                                if (autoByNameInjectClass.getName().equals(autoByNameObjectClass.getName())) {

                                    autoByNameObjectMap.put(autoByNameField, autoByNameObject);

                                } else {

                                    throw new MyException(beanName + "-----容器中没有名称为[" + autoByNameField.getName() + "]类型为[" + autoByNameInjectClass + "]的对象！");

                                }

                            }

                            // 遍历完成后，进行自动装配
                            instanceObject = instanceClass.newInstance();

                            // 处理自动装配的属性
                            for (Field autoByNameField : autoByNameObjectMap.keySet()) {

                                Object autoByNameInjectObject = autoByNameObjectMap.get(autoByNameField);

                                autoByNameField.setAccessible(true);
                                autoByNameField.set(instanceObject, autoByNameInjectObject);

                            }


                        } else { // 没有配置自动注入方式，直接实例化对象

                            instanceObject = instanceClass.newInstance();

                        }

                    } else { // 没有二级子节点，也非自动装配，直接实例化对象

                        instanceObject = instanceClass.newInstance();

                    }

                }

                // 判断容器中是否已有该name的bean
                if (map.get(beanName) != null) {

                    throw new MyException(beanName + "-----在容器中已存在！");

                } else {

                    // 将实例化后的对象放入map
                    map.put(beanName, instanceObject);

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取实例对象
     *
     * @param beanName
     * @return
     */
    public Object getBean(String beanName) {

        return map.get(beanName);
    }
}
