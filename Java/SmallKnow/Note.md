# 效的知识点

1. 默认包下的类无法导入

```java
//写法成立
Class<?> studentClass = Class.forName("Student");
//写法不成立，因为此时编译期无法识别这个类名
Class<Student> studentClass = Class.forName("Student");

```

尽管默认包下的类是``default``修饰，但是反射仍然能够访问，**子类是不能继承父类的构造器方法的**。
