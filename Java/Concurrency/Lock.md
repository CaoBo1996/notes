# 显示锁

## Lock接口

1. ``Lock``接口的实现类就是所谓的显式锁，意思它的加锁和释放锁的操作都需要显式的调用方法来实现，而不像内置锁那样进入同步代码块就算是加锁，从同步代码块出来就算是释放锁。

    ```java

    Lock l = new ReentrantLock();
    l.lock();
    try {
        // ... 业务逻辑代码
    } finally {
        l.unlock();
    }

    ```

注意：如果一个线程被``wait()``,那么一定要``notify``或者``interrupt``，才会进入到获取锁的阻塞状态，注意：``wait``方法也会抛出``InterruptedException``异常。

1. ``lock``
获取锁失败，发生阻塞，此时当其他线程将中断标志设置为``true``，那么无影响，继续等待获取锁，获取到了锁，直接执行代码。但是如果执行的代码调用了``sleep``，``join``等可能发出``interruptedexception``异常的方法时，则立马发生异常。否则的话不会发生中断异常。

2. ``lockInterruptibly()``
在**等待获取锁的过程中**或者准备获取锁之前，如果中断标志位被设置为了``true``，那么立马抛出异常，但是我们可以接住这个异常，然后**继续运行没有获取到锁的逻辑代码**即可。注意：**此时并没有获取到锁，所以也不会存在锁得释放，否则会抛出监视器异常**

    ```java
    Lock lock = new ReentrantLock();
    try{
        lock.lockInterruptibly();
        try{
            // 获取到了锁
        }finally{
            lock.unlock();  // 因为获取到了锁，所以此处finally块中要将锁释放
        }
    }catch(InterruptedException e){
        // 获取锁一开始或者过程中被中断业务逻辑代码
        //注意此时并没有获取倒锁，所以也不需要在finally块中释放锁
    }finally{
        // 可以为空。但是一定要注意，不要进行释放锁的操作。因为并没有获取到锁
    }

    ```

3. ``trylock()``
努力的获取锁，如果获取成功，那么则返回``true``，获取失败，则返回``false``.尽管当被设置为公平锁，但是只要锁能用，那么它会最先获取到锁，尽管可能其他的线程排在它前面获取锁。
**获取锁失败策略**：
    + 我们可以在获取锁失败后立即重试，也可以在获取锁失败后隔1秒后重试，也可以随机休息一段时间后再重试，也可以设置重试次数，比如重试100次后便停止获取锁的操作，这样自定义的重试策略就极大的提升了我们编程的灵活性。

        ```java
        Lock lock = new ReentrantLock();
        Random random = new Random();

        while (true) {
            boolean result = lock.tryLock(); //尝试获取锁的操作
            if (result) {  // result返回true，表示获取锁成功
                try {
                    // ... 具体业务代码
                } finally {
                    lock.unlock();
                }
            }

            // 获取锁失败后随即休息一段时间后重试
            try {
                Thread.sleep(random.nextInt(1000)); //随机休眠1秒内的时间
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        ```

4. ``trylock(long time, TimeUnit unit)``
在指定的时间内获取锁，但是如果是公平锁，那么不会像无参重载函数那样，立马获取锁，要等待。如果在规定的时间没有获取到锁，那么返回false,如果在等待时间内被别的线程打断，那么会抛出``InterruptedException``异常。

    ```java
    Lock lock = new ReentrantLock();
    try{
        boolean result = look.tryLock(1000L, TimeUnit.MILLISECONDS);  // 设置获取锁等待时间
        if(result){
            try{
                // 获取到了锁业务逻辑处理代码
            }finally{
                lock.unlock();
            }
        }else{
            //指定的时间类没有获取到锁业务处理代码，可以重试，也可以有其他的处理逻辑
        }
    }catch(InterruptedException e){
        // 获取锁一开始被中断，或者等待过程中被中断业务逻辑处理代码
    }finally{
        // 不需要进行释放锁的操作
    }
    ```

5. 线程中断的本质
    1. 每一个线程都有一个中断标志位。来标记自己是否被中断。当线程未被中断，意味中断标志位为``false``。当线程被中断，意味着中断标志位为``true``。一个线程可以将另外一个线程中断标志位为``true``。

    2. 相关方法

        ```java
        void  interrupt();  // 线程实例方法。使某个线程中断
        boolean isInterrupted();  // 返回该线程的中断状态
        static boolean interrupted();  // 返回该线程的中断状态，如果当前中断状态为true，那么调用该方法会将该线程中断状态置为false
        ```

        注意：
        + ``Interrupting a thread that is not alive need not have any effect``。即线程只有``start()``之后，该方法才会起作用。否则不会起任何作用。

    3. 举例运用

        ```java
        public class InterrputedDemo {

            public static void main(String[] args) {
                Thread t1 = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        while (true) {
                            if (Thread.currentThread().isInterrupted()) {
                                System.out.println("有别的线程把线程t1的中断状态变为true，退出循环");
                                break;
                            }
                        }
                    }
                }, "t1");

                t1.start();
                System.out.println("线程t1是否处于中断状态：" + t1.isInterrupted());

                System.out.println("在main线程中给t1线程发送中断信号");
                t1.interrupt();  // 注意是线程t1调用自己的interript()实例方法将自己中断。只不过这个方法的调用是发生在main线程中

                System.out.println("现在线程t1是否处于中断状态：" + t1.isInterrupted());
            }
        }
        ```

    4. 总结
        1. 一个线程给另外一个线程发送中断信号（注意：是某个线程调用自己的``interrupt()``的方法将自己的中断标志位设置为``true``）只是一厢情愿的事，被中断的线程对于这个信号也置之不理。
        2. 如果此时线程已经被中断，即中断标志位为``true``（注意:**``interrupt()``方法只有在线程``start()``之后才会起作用**），那么此时如果准备调用``wait()``、``join()``、``sleep()``等会抛出``InterruptedException``异常的方法，那么会直接抛出异常。如果此时准备获取可中断的锁，比如``tryLock(long time, TimeUnit unit)``或者``lockInterruptibly()``，那么也会直接抛出``InterruptedException()``异常。
        3. 综上：一个线程在调用这些方法**之前**或者**阻塞过程中**都会**实时监测**自己的中断状态是否为``true``，如果为``true``，立即返回并且抛出一个``InterruptedException``的异常，而且还会**清除该线程的中断状态**，也就是把中断状态再次修改为``false``。同理，一个线程会在获取可中断锁**之前**或者**获取锁失败阻塞的过程中**也会实时监测自己的中断状态。若中断标志位为``true``，那么直接抛出异常。
        4. 一个线程被中断的本质就是该线程的中断标志位被设置成``true``

6. 锁的公平性：注意线程调度的顺序和获取锁的顺序的区别：
公平锁只能保证获取锁的顺序时公平的，即先请求获取锁的线程会先得到锁，当该线程把锁释放了，如果此时没有线程在请求获取锁，则那么如果该线程还是被调度成功，那么该线程还是会获取到锁，但是，如果此时有线程在获取锁，那么该线程肯定是晚于之后的线程获取锁的。**被阻塞的线程是不会占用``CPU``内存的.所以整个``for``循环不会无限制的循环**
