# ``wait/notify``等待唤醒机制

线程状态：

1. 首先要明确，``Java``中线程状态分为6种

    |状态|说明|
    |:-:|:-:|
    |``NEW``|``Thread state for a thread which has not yet started``，即新创建了一个线程对象，但是还有没有调用``start()``方法|
    |``RUNNABLE``|``Java``将线程的就绪（``ready``）和正在运行（``running``）两种状态笼统的称之为``RUNNABLE``。某个线程对创建后，其他线程（比如``main``线程）调用了该线程对象的``start()``，那么此时处于``ready``状态，在此状态下，线程等待被线程调度系统选中，从而获取``CPU``的执行权。``ready``状态的线程在获得``CPU``的时间片后变成正在运行的状态``running``|
    |``BLOCKED``|表示线程获取锁失败，发生阻塞|
    |``WAITING``|等待其他线程的唤醒|
    |``TIMED_WAITING``|等到指定时间后自动返回|
    |``TERMINATED``|表示线程执行完毕|

    **说明**

    ```java
    public enum State {
        /**
        * Thread state for a thread which has not yet started.
        */
        NEW,  // 所有的线程对象都必须是Thread类或者其子类的实例，当new一个线程实例出来，那么此线程就处于NEW状态。

        /**
        * Thread state for a runnable thread.  A thread in the runnable
        * state is executing in the Java virtual machine but it may
        * be waiting for other resources from the operating system
        * such as processor.
        */

        /**
        * 1.当线程对象调用自己的实例方法start()，那么此时线程就处于RUNNABLE状态，即线程在可运行线程池中。而RUNNABLE状态又可以细分为就绪状态和运行状态
        * 2.处于就绪状态的线程，说明有资格运行，但是要等待调度，等待的是系统资源，CPU时间片等。但是必须要等到线程调度程序选中该线程，该线程才能执行，从而进入到运行状态。
        * 3.当线程获取同步锁失败，线程进入到阻塞状态
        * 4.在wait状态的线程被唤醒后，如果线程获得了执行权，说明线程进入到RUNNABLE状态，此时如果正在执行的线程时间片用完了，但还没获取到锁，而该线程获取到了时间片，那么会进入到获取锁失败的阻塞状态
        */
        RUNNABLE,

        /**
        * Thread state for a thread blocked waiting for a monitor lock.
        * A thread in the blocked state is waiting for a monitor lock
        * to enter a synchronized block/method or
        * reenter a synchronized block/method after calling
        * {@link Object#wait() Object.wait}.
        */
        BLOCKED,

        /**
        * Thread state for a waiting thread.
        * A thread is in the waiting state due to calling one of the
        * following methods:
        * <ul>
        *   <li>{@link Object#wait() Object.wait} with no timeout</li>
        *   <li>{@link #join() Thread.join} with no timeout</li>
        *   <li>{@link LockSupport#park() LockSupport.park}</li>
        * </ul>
        *
        * <p>A thread in the waiting state is waiting for another thread to
        * perform a particular action.
        *
        * For example, a thread that has called <tt>Object.wait()</tt>
        * on an object is waiting for another thread to call
        * <tt>Object.notify()</tt> or <tt>Object.notifyAll()</tt> on
        * that object. A thread that has called <tt>Thread.join()</tt>
        * is waiting for a specified thread to terminate.
        */
        WAITING,

        /**
        * Thread state for a waiting thread with a specified waiting time.
        * A thread is in the timed waiting state due to calling one of
        * the following methods with a specified positive waiting time:
        * <ul>
        *   <li>{@link #sleep Thread.sleep}</li>
        *   <li>{@link Object#wait(long) Object.wait} with timeout</li>
        *   <li>{@link #join(long) Thread.join} with timeout</li>
        *   <li>{@link LockSupport#parkNanos LockSupport.parkNanos}</li>
        *   <li>{@link LockSupport#parkUntil LockSupport.parkUntil}</li>
        * </ul>
        */
        TIMED_WAITING,

        /**
        * Thread state for a terminated thread.
        * The thread has completed execution.
        */
        TERMINATED;
    }
    ```

    每一个锁对象都对应着一个等待队列。也就是说如果一个线程在获取到锁之后发现某个条件不满足，就主动让出锁然后把这个线程放到与它获取到的锁对应的那个等待等待队列里，另一个线程在完成对应条件后通知它获取的锁对应的等待队列。这个过程意味着**锁和等待队列建立了一对一的关联**。
    **让出锁并且把线程放到与锁相关联的等待队列中**
    **完成了任务释放锁时通知等待在与这个锁相关联的等待队列里的线程可以再次尝试获取锁**

    ```java
    /*
    注意：wait()方法会释放锁，而sleep()方法不会释放锁。但二者在线程中断标志位true的情况下
    均会抛出InterruptedException异常。
    */
    public final void wait() throws InterruptedException
    public final void wait(long timeout) throws InterruptedException
    public final void wait(long timeout, int nanos) throws InterruptedException
    public final void notify();
    public final void notifyAll();
    ```

    |方法|说明|
    |:-:|:-:|
    |``wait()``|在线程获取到锁之后，调用**锁对象的本方法**，线程释放锁并且把该线程放置到与该锁对象关联的等待队列中|
    ``wait(long timeout)``|与``wait()``方法相似，只不过等待指定的毫秒数，如果超时，那么会自动把该线程从等待队列中移除，从而可以重新获取锁|
    |``wait(long timeout, int nanos)``|与上面的方法相同，只不过时间粒度更小，指定毫秒数加上纳秒数|
    |``notify()``|通知一个在与该锁对象关联的等待队列里的线程，使它从``waiit()``方法中返回，重新具有获取锁的资格|
    |``notifyAll()``|通知所有的线程，使所有的线程具有获取锁的机会|

    **说明**：

    1. **必须在同步代码块中调用``wait()``、``notify()``、或者``notifyAll()``方法**
    因为``wait()``方法是运行在等待线程里的，作用让某个线程释放锁，并且将该线程加入到与该锁对应的等待队列中。``notify()``或者``notifyAll()``是运行在通知线程中的。

    2. 补充
        1. ``notify()``方法只会将等待队列中的一个线程移出，而``notifyAll()``方法会将等待队列中的所有线程移出。

        2. 在调用完锁对象的``notify``或者``notifyAll``方法后，等待线程并不会立即从``wait()``方法返回，需要调用``notify()``或者``notifyAll()``的线程释放锁之后，等待线程才从``wait()``返回继续执行。
        也就是说如果通知线程在调用完锁对象的notify或者notifyAll方法后还有需要执行的代码，就像这样：

            ```java
            synchronized (对象) {
                完成条件
                对象.notifyAll();
            ... 通知后的处理逻辑
            }
            ```

    3. 生产者消费者模式

        生产者消费者具体的来说，就是在一个系统中，存在着生产者和消费者这两种角色，他们通过**缓冲区**进行通信，生产者生产消费者需要的数据（生产数据），消费者把数据做成产品（消费数据）。

        1. 生产者把数据生产到缓冲区，消费者从缓冲区中取数据进行消费

        2. 如果缓冲区已经满了，那么生产者线程``wait()``，放弃锁。如果缓冲区为空，那么消费者线程``wait()``，放弃锁

        3. 当生产者/消费者向缓冲区放入/取出一个产品之后，会唤醒其它所有的等待线程，包括生产者线程和消费者线程，之后放弃锁，结束自己的生产/消费操作。**此时自己仍然可以获取到锁**

        4. **同一时刻只能有一个生产者或者一个消费者操作缓冲区**，**禁止多个生产者*或者*多个消费者同时操作缓冲区，禁止一个生产者*和*一个消费者同时操作缓冲区**。即操作缓冲区的线程无论何时，仅仅只有一个，可能为生产者线程，也可能为消费者线程。

        5. ``wait()/notifyAll()``实现生产者消费者模式

            1. **为什么采用``notifyAll``，而不采用``notify``**，可能的一种情况如下：
                1. 首先所有的生产消费线程在主线程中``start()``之后，准备执行生产消费任务，所有的线程处于就绪的状态
                2. 假设生产者1首先拿到了锁，那么生产完产品，释放锁，下次又立马争抢到了锁，发现缓冲区满，所以释放锁，唤醒一个线程（此时所有的线程处于就绪状态，所以不会唤醒任何一个线程）后``wait``。
                3. 此时生产者2获取到了锁，缓冲区满，那么释放锁，``wait``
                4. 消费者1拿到锁，进行消费，并唤醒了生产者1，释放锁，下次又立马拿到了锁，此时释放锁``wait``
                5. 消费者2拿到锁，进行消费，由于缓冲区为空，那么释放锁，``wait``
                6. 注意此时生产者2``wait``，俩个消费者也``wait``。缓冲区为空
                7. 生产者1生产完产品，**但是唤醒了生产者2**，释放锁，立马又得到锁，此时``wait``
                8. 生产者2发现缓冲区满，那么释放锁，``wait``
                9. 从而所有的生产消费线程均``wait``。发生了**假死**的状态。

                即当**生产者生产了产品，那么此时应该通知消费者来消费这个线程，但是如果采用notify，那么可能唤醒的不是消费者，仍然是生产者，如上7所示**。总结上述死锁状态出现的可能情况（针对生产者线程而言）：**此时消费者全部``wait``**，而生产者除了一个生产者之外，其他也全部``wait``，缓冲区可能为空，也可能，不为空。
                    1. 如果为空，那么该生产者生产了产品，释放锁，但是此时唤醒了``另外一个生产者``。然后该生产者**再次立马**获取到锁，发现缓冲区已满，那么直接``wait``。此时另外一个生产者获取到锁，发现缓冲区满，直接``wait``，从而所有的生产消费线程全部``wait``，发生死锁
                    2. 如果此时缓冲区已经满了，那么直接``wait``。从而此时所有的线程均``wait``，发生死锁
                但是采用``notifyAll``也有不好的地方。我们的目的是当生产者生产了产品，我们希望**此时能够唤醒**消费者消费产品，而不是唤醒其他的生产者。而``notifyAll``会唤醒所有的线程，而唤醒线程是需要额外的开销的。
                但是如果此时仅仅只有一个生产者一个消费者的情况，那么可以直接使用``notify``，从而避免资源的消耗

            2. **为什么使用while而不使用if**

                1. 生产者与消费者的数量可能是不确定的。可能一个生产者多个消费者，也可能多个生产者一个消费者，也有可能一个生产者一个消费者，也有可能多个生产者，多个消费者。

                    ```java
                    /*
                    资源类。向外提供了操作资源的remove()和add()方法。这两个方法一定是要在资源类中定义。防止耦合
                    */

                    /*
                    注意：num和size的线程可见性问题，由于是在同步代码块中，所以任何线程的修改对其他线程都是可见的。

                    注意：下面的get()和set()方法都必须要加synchronized关键字。仅仅对set()方法进行同步是没有用的。调用get()的线程仍然会看到失效值。所以get()方法也必须要加synchronized关键字。
                    public class SynchronizedInteger{
                        private int value;
                        public synchronized int get(){
                            return value;
                        }
                        public synchronized void set(int value){
                            this.value = value;
                        }
                    }
                    */

                    /*
                    TODO lock锁也是内存可见的吗？是的。
                    */
                    public class Resource {
                        private int num;  // 缓冲区当前容量
                        private int size = 10;  // 缓冲区容量大小

                        public synchronized void remove() {
                            if (num > 0) {
                                num--;
                                System.out.println("消费者：" + Thread.currentThread().getName() + "消耗一件资源，" + "当前缓冲区还剩 "
                                        + num + " 个资源");
                                notifyAll();
                            } else {
                                try {
                                    System.out.println("消费者：" + Thread.currentThread().getName() + "线程进入等待状态," + "当前缓冲区还剩 "
                                            + num + " 个资源");
                                    wait();  // 直接等待，不需要做任何事
                                } catch (InterruptedException e) {
                                    System.out.println("消费者：" + Thread.currentThread().getName() + "等待过程被中断，抛出中断异常");
                                }
                            }

                        }

                        public synchronized void removeVer1() {
                            while (num == 0) {  // 注意：一定时要放在while循环中
                                try {
                                    System.out.println("消费者：" + Thread.currentThread().getName() + "线程进入等待状态," + "当前缓冲区还剩 "
                                            + num + " 个资源");  // 当执行该打印语句的时候，肯定时获取到了锁，那么此时num的值就是当前缓冲区的大小
                                    wait();
                                } catch (InterruptedException e) {
                                    System.out.println("消费者：" + Thread.currentThread().getName() + "等待过程被中断，抛出中断异常");
                                }
                            }
                            num--;
                            System.out.println("消费者：" + Thread.currentThread().getName() + "消耗一件资源，" + "当前缓冲区还剩 "
                                    + num + " 个资源");
                            notifyAll();  // 为了防止唤醒同一类的线程，所以要
                        }

                        public synchronized void addVer1(){
                            while(num == size){
                                try{
                                    System.out.println("生产者：" + Thread.currentThread().getName() + "线程进入等待状态" + "当前缓冲区还剩 "
                                            + num + " 个资源");
                                    wait();
                                }catch (InterruptedException e){
                                    System.out.println("生产者：" + Thread.currentThread().getName() + "等待过程被中断，抛出中断异常");
                                }
                            }
                            num++;
                            System.out.println("生产者：" + Thread.currentThread().getName() + "生产一件资源，当前缓冲区还有 "
                                    + num + " 个资源");
                            notifyAll();
                        }

                        /**
                        * TODO 注意另外一种处理逻辑：
                        * public synchronized void remove(){
                        * while(num == 0){
                        * try {
                        * System.out.println("消费者：" + Thread.currentThread().getName() + "线程进入等待状态," + "当前缓冲区还剩 "
                        * + num + " 个资源");
                        * wait();
                        * } catch (InterruptedException e) {
                        * System.out.println("消费者：" + Thread.currentThread().getName() + "等待过程被中断，抛出中断异常");
                        * }
                        * }
                        * num--;
                        * System.out.println("消费者：" + Thread.currentThread().getName() + "消耗一件资源，" + "当前缓冲区还剩 "
                        * + num + " 个资源");
                        * notifyAll();
                        * }
                        */

                        public synchronized void add() {
                            if (num < size) {
                                num++;
                                System.out.println("生产者：" + Thread.currentThread().getName() + "生产一件资源，当前缓冲区还有 "
                                        + num + " 个资源");
                                notifyAll();
                            } else {
                                try {
                                    System.out.println("生产者：" + Thread.currentThread().getName() + "线程进入等待状态" + "当前缓冲区还剩 "
                                            + num + " 个资源");
                                    wait();
                                } catch (InterruptedException e) {
                                    System.out.println("生产者：" + Thread.currentThread().getName() + "等待过程被中断，抛出中断异常");
                                }
                            }
                        }
                    }

                    //最好不要继承Thread，否则无法继承其它的类，造成耦合。
                    //直接实现Runnable接口，从而该类也可以进行继承其他的类
                    public class Consumer implements Runnable {
                        private Resource resource;
                        private ResourceCondition resourceCondition;

                        //在构造器中将该任务要操作的资源类的实例传入
                        public Consumer(Resource resource) {
                            this.resource = resource;
                        }

                        public Consumer(ResourceCondition resourceCondition) {
                            this.resourceCondition = resourceCondition;
                        }

                        @Override
                        public void run() {
                            while (true) {
                                try {
                                    Thread.sleep(1000);  // 睡眠1秒，防止该该线程多次获取到锁
                                } catch (InterruptedException e) {
                                    System.out.println(Thread.currentThread().getName() + "：消费线程被打断sleep");
                                    return;
                                }
                                resource.removeVer1();
                            }
                        }
                    }


                    public class Producer implements Runnable{
                        private Resource resource;
                        private ResourceCondition resourceCondition;

                        public Producer(Resource resource) {
                            this.resource = resource;
                        }

                        public Producer(ResourceCondition resourceCondition) {
                            this.resourceCondition = resourceCondition;
                        }

                        @Override
                        public void run() {
                            while (true) {  // 线程会一直执行
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    System.out.println(Thread.currentThread().getName() + "：生产线程被打断sleep");
                                    return;  // 当线程被打断，那么直接返回，run()方法结束，整个线程执行结束
                                }
                                resource.addVer1();
                            }
                        }
                    }

                    ```

        6. ``await()/singalAll()``实现生产者消费者模式
            上面实现中，``notifyAll()``会唤醒所有的线程，包括生产者线程和消费者线程。但是我们本质上只要把不是同类的线程唤醒即可。即生产者只能唤醒消费者线程，而消费者只能唤醒生产者线程。这样就不需要将所有的线程唤醒，从而避免了资源的消耗。

            ```java
            import java.util.concurrent.locks.Condition;
            import java.util.concurrent.locks.Lock;

            public class ResourceCondition {
                private int num;
                private int size = 10;
                private Lock lock;
                private Condition producerCondition;
                private Condition consumerCondition;

                public ResourceCondition(Lock lock, Condition producerCondition, Condition consumerCondition) {
                    this.lock = lock;
                    this.producerCondition = producerCondition;
                    this.consumerCondition = consumerCondition;
                }

                public void remove() {
                    lock.lock();
                    try {
                        if (num > 0) {
                            num--;
                            System.out.println("消费者：" + Thread.currentThread().getName() + "消耗一件资源，" + "当前缓冲区还剩 "
                                    + num + " 个资源");
                            producerCondition.signal();  // TODO 注意此处用signal(),还是signalAll(),感觉用前者比较好，因为如果将所有的生产线程唤醒，最终还是只有一个生产线程能生产
                        } else {
                            try {
                                System.out.println("消费者：" + Thread.currentThread().getName() + "线程进入等待状态," + "当前缓冲区还剩 "
                                        + num + " 个资源");
                                consumerCondition.await();
                            } catch (InterruptedException e) {
                                System.out.println("消费者：" + Thread.currentThread().getName() + "等待过程被中断，抛出中断异常");
                            }
                        }
                    } finally {
                        lock.unlock();
                    }

                }

                public void add() {
                    lock.lock();
                    try {
                        if (num < size) {
                            num++;
                            System.out.println("生产者：" + Thread.currentThread().getName() + "生产一件资源，当前缓冲区还有 "
                                    + num + " 个资源");
                            consumerCondition.signal();
                        } else {
                            try {
                                System.out.println("生产者：" + Thread.currentThread().getName() + "线程进入等待状态" + "当前缓冲区还剩 "
                                        + num + " 个资源");
                                producerCondition.await();
                            } catch (InterruptedException e) {
                                System.out.println("生产者：" + Thread.currentThread().getName() + "等待过程被中断，抛出中断异常");
                            }
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            }

            ```

        7. 阻塞队列实现

            ```java
            //TODO
            ```

        8. 假死现象的产生

            当唤醒其他线程时，如果采用的是``notify()``，那么可能会出现假死的现象，即此时所有的线程（包括生产者线程和消费者线程）都处于``wait``状态。造成的原因是因为``notify``唤醒的是同类（对于仅仅一个消费者和一个生产者而言使用**notify**是不会出问题的）。对于不是这种情况，解决的办法如下：
            + 如果用``synchronized``来进行同步，那么用``notify()``来唤醒所有的线程（包括生产者和消费者）。如果是用显示锁``ReentrantLock()``来进行同步，且仅仅定义一个``Condition``的情况下，那么使用``signalAll()``来唤醒所有的线程。

            + 如果用``ReentrantLock()``来进行同步，并且定义了两个``Condition``的情况下，对于生产者有``producerCondition``，对于消费者有``consumerCondition``。那么唤醒的时候只需要调用相应的``Condition``的``signal()``方法即可。即不需要调用``signalAll()``方法。不需要唤醒所有的生产者线程或者是唤醒所有的消费者线程。只需要唤醒某一个生产者线程或者某一个消费者线程。

        9. 用``if``还是``while``

            注意生产和消费有两种处理逻辑，另外一种处理逻辑如下：

            ```java
            public synchronized void remove(){
                /*
                首先先如果不能消费，那么先wait。当被唤醒的时候，在此判断能不能消费，如果不能，那么继续等着。知道能消费。即先等，然后消费
                而之前的写法是如果能消费，那么消费，不能就直接wait()。整个函数结束
                */
                while(num == 0){
                    try{
                        System.out.println("消费者：" + Thread.currentThread().getName() + "线程进入等待状态," + "当前缓冲区还剩 "
                            + num + " 个资源");
                        wait();
                    }catch(InterruptedException e){
                        System.out.println("消费者：" + Thread.currentThread().getName() + "等待过程被中断，抛出中断异常");
                    }
                }
                num--;
                System.out.println("消费者：" + Thread.currentThread().getName() + "消耗一件资源，" + "当前缓冲区还剩 "
                        + num + " 个资源");
                notifyAll();
            }

            ```

            在上面的处理逻辑中，必须保证``wait()``方法的调用在``while``循环中。其实如果只有一个生产者线程并且只有一个消费者线程的话，那么其实可以用``if``代替``while``。因为唯一的生产者线程只能被唯一的消费者线程唤醒。唯一的消费者线程只能被唯一的生产者线程唤醒。因此，当生产者线程或者消费者线程被唤醒时，条件总是成立。但是在多个生产者线程和多个消费者线程的情况下，一个消费者（以消费者线程说明，生产者同理）线程被唤醒，并获取到了锁，并不能保证该消费者线程**此时便能够消费**。因为唤醒并争取到了锁，在这一段时间内，可能有其他的线程将生产者生产的产品全部消费。
