# 线程池


进程与线程

在``web``开发的过程中，服务器需要接受并处理请求，会为每一个请求都分配一个线程来进行处理。如果每次都**新创建**一个线程来处理这个请求，实现起来很简单，但是存在一个问题：
如果并发请求的数量非常多，但是每次线程处理这个请求的时间都非常短。这样就会频繁的创建和销毁线程，从而大大降低系统效率。可能出现服务器在为每个请求**创建新线程和销毁线程所花的时间和消耗的系统资源**要比**处理实际的用户请求所花费的时间和资源更多**
合理的利用线程池能带来很多的好处：
第一：**降低资源消耗**。通过重复利用已创建的线程降低线程创建和销毁造成的资源消耗。
第二：**提高响应速度**。当任务到达时，任务可以不需要的等到线程创建就能立即执行。
第三：**提高线程的可管理性**。线程是稀缺资源，如果无限制的创建，不仅会消耗系统资源，还会降低系统的稳定性，使用线程池可以进行统一的分配，调优和监控。

线程与进程的区别：


## ``ThreadPoolExecutor``

1. 构造函数

    ```java
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler)
    ```

    + 说明：
        ``corePoolSize``：核心线程数量。在新建一个``ThreadPoolExecutor``对象后，**默认**的情况下，线程池中并没有任何线程。而是等待有任务到来时，才会创建新的线程去执行任务。除非调用``pre``

2. 工作流程






