# ``Java``万物皆对象

## 四种类

1. ``Class``，``Constructor``，``Field``，``Method``

    + ``Class``类
        1. 构造器函数函数为私有的，即``Class<?>``类实例由``JVM``创建，且每一个类都对应唯一的``JVM``实例

            ```java

            ```

        2. ``newInstance()``

            通过``Class``的``newInstance()``方法来创建对应类的实例，本质上是通过构造器对象，然后调用构造器对象的``newInstance()``方法来创建对象。即前提是必须要有一个空参的构造函数，如果没有空参的构造函数，那么无法使用``class.newInstance()``方法。所以在这样的情况下，必须要获取其他的有参的构造器对象然后调用这个构造器对象的``newInstance()``方法来创建对象。

        3. 获取某个类的``Class``对象的3种方式

            + ``getClass()``方法

                ``getClass()``方法继承自``Object``，且是一个本地方法。

                ```java
                public final native Class<?> getClass();
                ```

                ```java
                Student stu = new Student();
                //通过实例对象调用getClass()方法来创建一个Class类实例
                Class<Student> stuclass = stu.getClass();
                ```

            + ``.class``属性

                ```java

                //可以看成任何数据类型（包括基本数据类型）都有一个静态的class属性
                //Student类型的class属性返回一个 Class<Student> 类型的stuclass
                //int是基本数据类型，也有一个class属性 即
                //Class<Integer> intT = int.class;
                Class<Student> stuclass = Student.class;
                ```

            + ``Class``类的``forName(name)``静态方法，根据``name``来确定要创建的类的``Class<?>``对象

                ```java
                //传入全限定名，即包名加上类名，没有包名则直接类名
                Class<Student> stuClass = (Class<Student>) Class.forName("Student");
                ```

            + **在运行期间，一个类，只有一个``Class``对象产生**。

    + ``Constructor``类

        + 得到构造器对象

            注意：子类不会继承父类的构造函数，子类调用父类构造函数super.constructor()**不代表创建了一个父类的对象**。抽象类不能创建对象，但是继承该抽象类的子类确实能够调用该父类的构造方法。

            ```java
            Constructor getConstructor(Class[] params)  // 获得使用特定的参数类型的public构造函数

            Constructor[] getConstructors()  // 获得类的所有public构造函数

            Constructor getDeclaredConstructor(Class[] params)  // 获得使用特定参数类型的构造函数(与访问权限无关)

            Constructor[] getDeclaredConstructors()  // 获得类的所有构造函数(与访问级别无关)

            ```

        + 构造器对象相关方法

            ```java
            newInstance(Object.. initargs);  // 创建一个对象实例，注意需要跟得到构造器对象的参数类型一样
            //否则会抛出异常
            ```

    + ``Filed``类

    + ``Method``类
        1. 方法获取

            ```java
            Method getMethod(String name, Class[] params)  // 获取特定类型参数的public方法

            Method[] getMethods()  // 获取所有的public修饰的方法，包括从父类继承下来的方法

            Method[] getDeclaredMethods() // 获取所有的方法，但仅仅是该类声明的方法，
            //与访问权限修饰符无关
            Method getDeclaredMethod(String name, Class[] params) // 返回特定类型参数的方法

            ```

        2. 方法调用

            ```java

            ```
