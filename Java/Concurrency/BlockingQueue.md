# 阻塞队列源码分析

阻塞队列与普通队列的异同点在于：
1.当执行出队操作时，如果队列为空，那么会一直等待队列直到队列非空。当执行入队操作时，如果队列已满，会一直等待队列直到队列有空余的空间。即阻塞入队和阻塞出对。
2.二者均遵循``FIFO``，即``first-in-first-out``。
本文主要分析两个常见的阻塞队列，``ArrayBlockingQueue``和``LinkedBlockingQueue``

## ArrayBlockingQueue源码分析

**综述**：``ArrayBlockingQueue``是一个有界阻塞队列，即构造函数初始化时**必须**要传入``capacity``。该阻塞队列的底层由数组实现，数组的大小在整个过程中是固定的。

1. 构造函数

    ```java
        public ArrayBlockingQueue(int capacity, boolean fair) {
            if (capacity <= 0)
                throw new IllegalArgumentException();
            this.items = new Object[capacity];  // items即内部数组
            lock = new ReentrantLock(fair);  // 全局唯一一把锁对象，根据传入的fair参数来指定是公平锁还是非公平锁，默认为非公平锁
            notEmpty = lock.newCondition();  // 出队线程的监视器
            notFull =  lock.newCondition();  // 入队线程的监视器
        }

        public ArrayBlockingQueue(int capacity) {
            this(capacity, false);  // 默认为非公平锁
    }
    ```

2. 重要属性

    ```java
    final Object[] items;  // 内部数组，用来存储入队的元素。
    int count;  // 此时数组中元素的个数，即已经入对的元素的个数
    final ReentrantLock lock;  // 唯一锁对象
    private final Condition notEmpty;  // 入队线程监视器
    private final Condition notFull;  // 出队线程监视器
    int putIndex;  // 入队线程放置元素的索引
    int takeIndex;  // 出队线程放置元素的索引
    ```

3. 重要方法

    + 入队操作

        1. ``offer()``当入队成功，返回true。否则返回false，或者可能抛出``InterruptedException``异常（根据传入的参数来决定）。

            ```java
                public boolean offer(E e) {
                    checkNotNull(e);  // 根据API文档说明：入队元素不能为null，否则抛出NullPointerException
                    final ReentrantLock lock = this.lock;  
                    lock.lock();  // 获取锁，如果此时这把锁被占用，那么该线程处于阻塞状态。注意：可能被入队线程占用也有可能被出队线程占用
                    //当成功获取到了锁，那么此时其他的所有的线程均会被阻塞。不论该线程是出队线程还是入队线程。
                    try {
                        if (count == items.length)  // 如果队列已满，那么插入失败，直接返回false
                            return false;
                        else {
                            enqueue(e);  // 将元素入队。即插入到数组中。同时唤醒一个在notEmpty监视器上等待的出队线程
                            return true;
                        }
                    } finally {
                        lock.unlock();  // 保证释放锁
                    }
                }
                public boolean offer(E e, long timeout, TimeUnit unit)
                    throws InterruptedException {
                    checkNotNull(e);  // 不允许插入为空
                    long nanos = unit.toNanos(timeout);
                    final ReentrantLock lock = this.lock;
                    lock.lockInterruptibly();  // 获得可打断的锁，即如果获取不到锁，那么会一直阻塞，但是若别的线程将其interrupt标志为设置为true，那么会抛出InterruptedException
                    try {
                        while (count == items.length) {
                            if (nanos <= 0)
                                return false;
                            nanos = notFull.awaitNanos(nanos);  
                            /*
                            1.获取到了锁，但是若此时队列已满，那么等待nanos（注意：等待是在获取锁的前提下等待nanos，而不是等待获取锁的时间为nanos）。并且会将获取到的锁释放。那么其他的出队或者入队线程都可能会争抢到这把锁。
                            2.可能出现的情况是等待的过程中被别的线程给唤醒，并且重新阻塞直到获取到锁成功，若此时队列还是满的，并且此时等待时间nanos不为0，那么仍需要等待。一直到队列满，然后判断等待时间到了，那么直接返回false。
                            */
                        }
                        enqueue(e);  // 队列没有满，那么直接入队元素。注意在enqueue()函数中，会唤醒在notEmpty()上等待的线程。即出队线程。
                        return true;
                    } finally {
                        lock.unlock();  // 将锁释放。注意：全局仅仅有一把锁。不管是入队线程获得还是出队线程获得，其他的入队或者出队线程均不会获得。
                    }
                }

                private static void checkNotNull(Object v) {
                    if (v == null)
                        throw new NullPointerException();
                }

                private void enqueue(E x) {
                    // assert lock.getHoldCount() == 1;
                    // assert items[putIndex] == null;
                    final Object[] items = this.items;
                    items[putIndex] = x;
                    if (++putIndex == items.length)
                        putIndex = 0;
                    count++;
                    notEmpty.signal();  // 将出队线程唤醒
                }

            ```

            + ``public boolean offer(E e)``与``public boolean offer(E e, long timeout, TimeUnit unit)``的区别：
                1. 前者获取到的锁是``lock()``，即如果获取不到会一直阻塞。即使中断标志位被其他线程置为``true``

                2. 后者获取到的锁是``lockInterruptibly()``，即如果获取不到锁，也会一直阻塞，但是在阻塞的过程中，如果有其他的线程将其中断标志位设置为``true``，那么会立马抛出异常。即我们如果处理这个异常。然后代码还是会继续执行。但是千万注意此时是没有获取到锁得。所以也不存在释放锁的过程。

                3. 这二者均是插入失败返回``false``。但是后者会比前者可能多抛出一个异常。但是注意这个异常是在等待获取锁的时候被打断抛出的。

                4. 前者获取到了锁，判断队列是否已满，没有满直接插入成功返回``true``，满了失败返回``false``。后者获取到了锁，判断队列是否满，满了会等待传入的时间，如果等待时间到了，队列还是满的，那么直接返回``false``。否则直接插入。

        2. ``put()``一直等待直到获取到锁，但是由于获取的是``lockInterruptibly``，所以在等待获取的过程中或被打断，直接抛出``InterruptedException``。等到获取锁成功，会一直等待，直到插入成功。注意与``offer(E e, long timeout, TimeUnit unit)``的区别

            ```java
                public void put(E e) throws InterruptedException {
                    checkNotNull(e);
                    final ReentrantLock lock = this.lock;
                    lock.lockInterruptibly();
                    try {
                        while (count == items.length)
                            notFull.await();
                        enqueue(e);
                    } finally {
                        lock.unlock();
                    }
                }
            ```

        3. ``add()``本质上是调用了``offer(e)``函数，而我们直到，如果``offer(e)``入队成功，那么直接返回``true``。否则返回``false``。而在``add()``函数中，在``offer(e)``函数返回``false``的情况下，会直接抛异常。

            ```java
                public boolean add(E e) {
                        if (offer(e))
                            return true;
                        else
                            throw new IllegalStateException("Queue full");
                }
            ```

    + 出队操作

        1. ``poll()``

            ```java
                public E poll() {
                    final ReentrantLock lock = this.lock;
                    lock.lock();  // 获取锁
                    try {
                        //如果此时队列为空，则返回null，否则直接出队元素，并且返回出队的元素。
                        return (count == 0) ? null : dequeue();
                    } finally {
                        lock.unlock();
                    }
                }
                public E poll(long timeout, TimeUnit unit) throws InterruptedException {
                    long nanos = unit.toNanos(timeout);
                    final ReentrantLock lock = this.lock;
                    lock.lockInterruptibly();  // 获取到可中断的锁，即在等待获取锁的过程中，若中断标志为为true，那么直接抛出InterruptedException异常。后续的代码均不会执行。
                    try {
                        while (count == 0) {
                            if (nanos <= 0)
                                return null;  // 返回null只有在队列为空的条件下会发生。因为入队是不允许null的。
                            nanos = notEmpty.awaitNanos(nanos);
                        }
                        /*
                        1.与入队操作相似，当获取到了锁，那么先判断此时队列中元素是否为空，如果为空，那么等待，并释放锁
                        2.等待时可能被入队线程所唤醒，并且此时又争抢到了锁，那么会继续判断是否为空，如果为空，且等待剩余时间不为0，那么继续等待剩余的时间并释放锁。
                        3.如果最终等待时间到了，且此时队列仍为空，那么返回null。
                        */
                        //  队列不为空，那么直接出队操作
                        return dequeue();  // 注意：该函数会将入队线程唤醒
                    } finally {
                        lock.unlock();
                    }
                }

                private E dequeue() {
                    // assert lock.getHoldCount() == 1;
                    // assert items[takeIndex] != null;
                    final Object[] items = this.items;
                    @SuppressWarnings("unchecked")
                    E x = (E) items[takeIndex];
                    items[takeIndex] = null;
                    if (++takeIndex == items.length)
                        takeIndex = 0;
                    count--;
                    if (itrs != null)
                        itrs.elementDequeued();
                    notFull.signal();  // 唤醒一个入队线程
                    return x;
                }

            ```

        2. ``take()``类似于入栈的``put()``函数。

            ```java
                public E take() throws InterruptedException {
                    final ReentrantLock lock = this.lock;
                    lock.lockInterruptibly();  // 获取可中断的锁
                    try {
                        while (count == 0)
                            notEmpty.await();  // 一直等待直到队列中有元素
                        return dequeue();
                    } finally {
                        lock.unlock();
                    }
                }

            ```

        3. ``peek()``

            ```java
                public E peek() {
                    final ReentrantLock lock = this.lock;
                    lock.lock();
                    try {
                        return itemAt(takeIndex); // null when queue is empty
                    } finally {
                        lock.unlock();
                    }
                }

            ```

        4. ``remove()``

            ```java
                public boolean remove(Object o) {
                    if (o == null) return false;
                    final Object[] items = this.items;
                    final ReentrantLock lock = this.lock;
                    lock.lock();  // 获取锁，当获取到了锁，那么此时别的线程是肯定不会执行。因为仅仅只有一个锁对象
                    try {
                        if (count > 0) {
                            final int putIndex = this.putIndex;
                            int i = takeIndex;
                            do {  // 在队列中，通过while循环来找到与传入的e相等的元素
                                if (o.equals(items[i])) {
                                    removeAt(i);  // 找到那么直接移除，返回true。
                                    return true;
                                }
                                if (++i == items.length)
                                    i = 0;
                            } while (i != putIndex);
                        }
                        return false;  // 找不到，最终返回false。
                    } finally {
                        lock.unlock();
                    }
                }
            ```
