# GC

## 垃圾收集器

### 新生代垃圾收集器

**1、Copy（别名：Serial、DefNew）**

使用一条垃圾收集线程来进行垃圾回收。并且在进行垃圾回收的时候，会暂停其他所有的用户正常工作的线程。直到它收集完成。即会进行 Stop The World。采用复制算法来进行垃圾回收，即 从Eden区和From Survivor区拷贝存活对象到To Survivor区域 。