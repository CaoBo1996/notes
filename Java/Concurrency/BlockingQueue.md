# 阻塞队列源码分析

**阻塞队列与普通队列的异同点在于**：

1. 当执行出队操作时，如果队列为空，那么出队线程试图从队列取出元素会被阻塞，直到队列非空。当执行入队操作时，如果队列已满，那么入队线程试图往队列中添加元素会被阻塞，直到队列非满。即阻塞入队和阻塞出对。

2. 二者均遵循``FIFO``，即``first-in-first-out``。

本文主要分析两个常见的阻塞队列，``ArrayBlockingQueue``和``LinkedBlockingQueue``

## ArrayBlockingQueue源码分析

**综述**：``ArrayBlockingQueue``是一个**有界阻塞队列**，即构造函数初始化时**必须**要传入``capacity``的初始值。该阻塞队列的底层由数组实现，数组的大小在整个过程中是固定的，并且在``ArrayBlockingQueue``初始化实例时**就会给数组赋予一段连续的内存（即 ``this.items = new Object[capacity];``）**。

1. 构造函数

    ```java
    /**
     1.注意ArrayBlockingQueue内部仅仅只有一个锁对象。但该锁对
       象对应着两个等待队列。即lock调用了两次newCondition()方法。
     2.由于只有一个锁对象，那么某个时刻，仅仅只有一个线程在执行
       入队或者出队操作。因为执行这些操作的前提都是先获取到锁。
     */
    public ArrayBlockingQueue(int capacity, boolean fair) {
        if (capacity <= 0)
            throw new IllegalArgumentException();
        // items即内部数组，为数组分配连续内存空间
        this.items = new Object[capacity];
        // 唯一锁对象，根据传入的fair参数来指定是公平锁还是非公平锁，默认为非公平锁
        lock = new ReentrantLock(fair);
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
    int count;  // 此时数组中元素的个数，即队列中此时元素的个数
    final ReentrantLock lock;  // 唯一锁对象
    // 出队线程监视器，notEmpty，说明队列没有空，那么出队线程可执行出队操作
    private final Condition notEmpty;
    // 入队线程监视器，notFull，说明队列没有满。那么入队线程可执行入队操作
    private final Condition notFull;
    int putIndex;  // 下一个入队线程放置元素到数组的位置索引
    int takeIndex;  // 下一个出队线程放置元素到数组的位置索引
    /**
     * 本质上数组是一个“循环数组”。即如果进行了一次添加元素后，
     * 下一次添加的位置超过了数组尾部，
     * 即进行一次入队操作之后会进行putIndex ++。如果此
     * 时putIndex加1之后等于element.length(注意数组的最后
     * 一位索引下标为length-1)，那么直接将putIndex == 0。
     * 同理takeIndex也是如此。进行出队，也会takeIndex ++。如
     * 果此时 takeIndex == length，那么直接将takeIndex == 0
     */
    ```

3. 重要方法

    + 入队操作

        1. ``offer()``当入队成功，返回true。否则返回false，或者可能抛出``InterruptedException``异常（根据传入的参数来决定）。

            ```java
            public boolean offer(E e) {
                checkNotNull(e);  // 根据API文档说明：入队元素不能为null，否则抛出NullPointerException
                final ReentrantLock lock = this.lock;
                /**
                    * 获取锁，如果此时这把锁被占用，那么该线程处于阻塞状态。
                    * 注意：可能被入队线程占用也有可能被出队线程占用
                    * 当成功获取到了锁，那么此时其他的所有的线程均会被阻塞。
                    * 不管该线程是出队线程还是入队线程。即同一时刻，
                    * 只有一个线程可以执行入队或者出队操作。
                    */
                lock.lock();
                //成功获取到了锁，那么会判断此时队列是否已满，如果是，直接返回直接返回false
                try {
                    if (count == items.length)
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
                /**
                    * 获得可打断的锁，即如果获取不到锁，那么会一直阻塞，
                    * 但是若此时别的线程将其interrupt标志为设置为true，
                    * 那么会抛出InterruptedException，并且方法体内没有
                    * 处理这个异常，该异常在方法签名中被抛出
                    */
                lock.lockInterruptibly();
                try {
                    while (count == items.length) {
                        if (nanos <= 0)
                            return false;
                        nanos = notFull.awaitNanos(nanos);  // 在队列满的情况下等待的时间
                        /**
                            1.获取到了锁，但是若此时队列已满，那么等待nanos（注意：等
                            待是在获取到了锁的前提下等待nanos，而不是等待获取锁的时
                            间为nanos）。并且会将获取到的锁释放。那么其他的出队或者
                            入队线程都可能会争抢到这把锁。
                            2.可能出现的情况是等待的过程中被别的线程给唤醒（即没有等待
                            完整的时间参数，提前被唤醒），并且获取锁成功，那么会继续判断此
                            时队列是否满，若此时队列还是满的，那么继续等待剩余的时间。一直
                            到重新获取到锁，并且判断此时队列已经满了，然后判断等待时间也到
                            了，那么直接返回false，说明插入失败
                            */
                    }
                    // 队列没有满，那么直接入队元素。注意在enqueue()函数中，
                    //会唤醒在notEmpty()上等待的线程，即出队线程。
                    enqueue(e);
                    return true;
                } finally {
                    // 将锁释放。注意：全局仅仅有一把锁。
                    // 不管是入队线程获得还是出队线程获得，其他的入队或者出队线程均不会获得。
                    lock.unlock();
                }
            }

            /**
             * 队列不允许插入null
             */
            private static void checkNotNull(Object v) {
                if (v == null)
                    throw new NullPointerException();
            }

            private void enqueue(E x) {
                // assert lock.getHoldCount() == 1;
                // assert items[putIndex] == null;
                final Object[] items = this.items;
                items[putIndex] = x; // 直接将元素赋值给items[putIndex]
                //putIndex进行自增操作，结果是下一个入队元素所在的数组位置索引
                if (++putIndex == items.length)
                    putIndex = 0;
                count++;  // 将此时队伍元素个数增1操作
                // 将出队线程唤醒，即出队线程负责唤醒入队线程。
                // 而入队线程负责唤醒出队线程。即均是负责唤醒对方线程
                notEmpty.signal();
            }

            ```

            + ``public boolean offer(E e)``与``public boolean offer(E e, long timeout, TimeUnit unit)``的区别：
                1. 前者获取的锁是``lock()``，即如果获取不到会一直阻塞。即使中断标志位被其他线程置为``true``。**注意是在获取锁的时候如果获取失败一直阻塞，在成功获取到锁之后，进行入队操作，尽管队列已经满了，也不会发生阻塞**。

                2. 后者获取到的锁是``lockInterruptibly()``，即如果获取不到锁，也会一直阻塞，但是在阻塞的过程中，如果有其他的线程将其中断标志位设置为``true``，那么会立马抛出异常。**该异常在方法内部没有进行捕捉处理，而是直接在方法签名中抛出**。当获取到了锁，如果队列已满，那么最久会阻塞等待方法形参传入的时间参数。如果等待时间到了，队列还是满状态，那么插入失败，那么直接返回``false``。

                3. 这二者均是插入失败返回``false``。但是后者会比前者可能多抛出一个异常。但是注意这个异常是在等待获取锁的时候被打断抛出的。

                4. 前者获取到了锁，判断队列是否已满，没有满直接插入成功返回``true``，满了直接失败返回``false``。后者获取到了锁，判断队列是否满，满了会等待传入的参数时间，进行阻塞操作（阻塞操作需要其它的线程唤醒，如果阻塞过程中提前被唤醒，那么会继续尝试获取锁，如果获取成功，那么会判断此时队列是否满，如果没有满，那么插入到队列中，如果队列已满，那么继续阻塞之前阻塞剩下的时间。**如果此时获取到了锁，并且队列已满，并且又判断此时阻塞时间到了**，那么直接返回``false``，说明插入失败）。

        2. ``put()``一直阻塞等待直到获取到锁，但是由于获取的是``lockInterruptibly``，所以在等待获取锁的过程中被打断，那么直接抛出``InterruptedException``。获取锁成功，如果队列已满，那么会一直阻塞等待，直到入队成功。注意与``offer(E e, long timeout, TimeUnit unit)``的区别

            ```java
            public void put(E e) throws InterruptedException {
                checkNotNull(e);
                final ReentrantLock lock = this.lock;
                lock.lockInterruptibly();  // 注意该InterruptedException异常在方法签名处被抛出
                try {
                    while (count == items.length)
                        notFull.await();  // 会一直等待，直到入队成功
                    enqueue(e);
                } finally {
                    lock.unlock();
                }
            }
            ```

            + ``put(E e)``与``offer(E e, long timeout, TimeUnit unit)``方法区别
                1. 首先二者在获取锁的时候，获取的都是``lockInterruptibly``，即说明获取锁失败阻塞时可以被中断，抛出``InterruptedException``。
                2. 当获取到了锁，二者都会先判断此时队列是否满，如果没有满，那么会直接插入要入队的元素``E e``
                3. 如果队列满了，那么前者会直接执行``await()``，在``notFull``监视器上阻塞等待。一直到重新被唤醒，再次成功获取锁，然后判断队列是否满，若满了，再继续等待，重复上面的步骤，即一直等待插入。除非被打断（``await()``阻塞过程是可以被打断的）。而后者仅仅会阻塞等待传入的参数时间。如果时间到了，获取到了锁，判断队列还是满的，那么直接返回``false``。
                4. 注意：**所有的入队在获取到锁之后，都会先判断此时队列是否已满。然后根据队列满了执行不同的处理策略**，如果队列没有满，那么直接插入队列中。

            + 注意：此时有两个可能的打断操作，一个是在获取``lockInterruptibly``锁失败阻塞过程中，可以被中断。第二个是在成功获取了锁，然后由于队列已满，调用``await()``方法发生阻塞，此时阻塞的过程也可以被中断,``await()``也会抛出``InterruptedException``异常。这两个阻塞过程抛出的异常都最终被入队方法锁抛出。``await()``方法抛出异常给``put()``或者其他方法，然后该方法再次将异常抛出给调用者。

        3. ``add()``本质上是调用了``offer(e)``函数，而我们直到，如果``offer(e)``入队成功，那么直接返回``true``。否则返回``false``。而在``add()``函数中，在``offer(e)``函数返回``false``的情况下，会执行``else``部分，从而抛出``IllegalStateException``异常。

            ```java
            public boolean add(E e) {
                        if (offer(e))
                        return true;
                        else
                        throw new IllegalStateException("Queue full");
            }
            ```

        4. **一般插入操作，用``put()``方法**。

    + 出队操作

        1. ``poll()``出队操作。如果队列为空，那么直接返回``null`，队列不为空，那么直接将队伍的队首元素出队。

            ```java
            public E poll() {
                final ReentrantLock lock = this.lock;
                lock.lock();  // 获取lock锁，获取失败一直阻塞
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
                // 获取到可中断的锁，获取锁失败阻塞的过程中，若中断标志为true，
                // 那么直接抛出InterruptedException异常，该异常在方法签名被抛出
                lock.lockInterruptibly();
                try {
                    while (count == 0) {
                        if (nanos <= 0)
                            return null;  // 返回null只有在队列为空的条件下会发生。因为入队是不允许null的。
                        nanos = notEmpty.awaitNanos(nanos);
                    }
                    /**
                     1.与入队操作相似，当获取到了锁，那么先判断此时队列是否为空，
                      如果为空，那么等待传入的时间参数，进行阻塞操作并释放锁
                     2.等待时可能被入队线程所唤醒，并且此时又争抢到了锁，
                      那么会继续判断队列是否为空，如果为空，且等待的剩余时间不为0，
                      那么继续等待剩余的时间并释放锁。
                     3.如果获取到了锁，且此时队列仍为空，并且等待时间完成，
                      那么返回null。
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
                // 将数组takeIndex位置元素保留
                // 然后将该位置清空
                items[takeIndex] = null;
                if (++takeIndex == items.length)
                takeIndex = 0;
                count--;

                //将迭代器的元素删除
                if (itrs != null)
                    itrs.elementDequeued();
                notFull.signal();  // 唤醒一个入队线程，即将对方唤醒
                    return x;  // 将takeIndex位置的元素返回
            }

            ```

        2. ``take()``类似于插入队列的``put()``函数。

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

        3. ``remove()``，移除特定的元素。如果队列中有与该元素``equals()``相等的元素，那么会移除该元素。并且返回``true``。否则，返回``false``

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

    + 总结出队与入队的方法如下

      |             | ``Throws exception`` | ``Special value`` |    ``Blocks``     |       ``Time out``       |
      | :---------: | :------------------: | :---------------: | :---------------: | :----------------------: |
      | ``Insert``  |      ``add(e)``      |   ``offer(e)``    |    ``put(e)``     | ``offer(e, time, unit)`` |
      | ``Remove``  |     ``remove()``     |    ``poll()``     |    ``take()``     |   ``poll(time, unit)``   |
      | ``Examine`` |    ``element()``     |    ``peek()``     | ``not applicble`` |    ``not applicble``     |

        **说明**：

        1. ``Throws exception``
            + ``add(e)``：尝试插入元素到阻塞队列，成功返回``true``，如果队列此时已满，那么直接抛出``IllegalStateException``异常，说明队伍已满，不会阻塞当前线程
            + ``remove()``：尝试插入元素到阻塞队列，成功，返回``true``。如果队列此时已满，返回``false``。不会抛出异常，也不会阻塞当前线程。

        2. ``Special value``
            + ``offer(e)``获取的是``lock``锁。尝试插入队列，如果插入成功，返回``true``。如果队列已满，返回``false``，不会阻塞当前线程。
            + ``poll()``获取的是``lock``锁。尝试将队首元素出队。如果出队成功，返回``true``。如果队列为空，那么返回``null``，不会阻塞当前线程。
        3. ``Blocks``
            + ``put(e)``获取的是``lockInterruptibly``锁。尝试添加元素到队列中，函数返回值是``void``。如果队列已满，会一直阻塞，直至插入成功，但阻塞的过程中可以被打断。
            + ``take()``获取的是``lockInterruptibly``锁。尝试将队列队首元素出队。如果出队成功，那么将出队的元素返回。如果队列为空，会一直阻塞，直至出队成功。阻塞的过程可以被中断。
        4. ``Time out``
            + ``offer(e, time, unit)``获取的是``lockInterruptibly``锁。尝试将元素插入到阻塞队列中，如果插入成功，返回``true``。队伍已满，入队失败，阻塞当前线程。如果在传入的时间参数里仍然插入失败，那么返回``false``
            + ``poll(e, time, unit)``获取的是``lockInterruptibly``锁。尝试将队首元素出队。如果出队成功，返回出队元素。队伍为空，出队失败，阻塞当前线程。如果在传入的时间参数里仍然出队失败，那么返回``null``

4. 生产者与消费者模式

    ```java
    class Producer implements Runnable {
        private final BlockingQueue queue;
        Producer(BlockingQueue q) {
            queue = q;
        }
        public void run() {
            try {
                while (true) {
                    queue.put(produce());  // 注意：一般使用put()方法将元素插入到队列中
                }
            } catch (InterruptedException ex) {
                // 异常处理操作
            }
        }
        Object produce() {
            // ... 生产操作
        }
    }

    class Consumer implements Runnable {
        private final BlockingQueue queue;
        Consumer(BlockingQueue q) {
            queue = q;
        }
        public void run() {
            try {
                while (true) {
                    consume(queue.take());   // 一般使用take()方法将队首元素出队
                }
            } catch (InterruptedException ex) {
                // 异常处理操作
            }
        }
        void consume(Object x) {
            // ... 生产操作
        }
    }

    class Setup {
        public static void main(String[] args) {
            BlockingQueue q = new ArrayBlockingQueue();
            Producer p = new Producer(q);
            Consumer c1 = new Consumer(q);
            Consumer c2 = new Consumer(q);
            new Thread(p).start();
            new Thread(c1).start();
            new Thread(c2).start();
        }
    }
    ```

## ``LinkedBlockingQueue``

底层是链表实现。出队线程**只能操作``head``指针**。入队线程**只能操作``last``指针**。并且``LinkedBlockingQueue``初始化时，构造函数会创建一个头节点，该头节点不存放任何数据，并且初始化时，``head``和``last``均指向这个不保存数据的头节点。综合以上两点，从而可以保证入队和出队操作可以同时进行，而不会造成什么数据的不安全性。并且内部记录此时队列元素个数的``count``是一个``AtomicInteger``类，所以保证``count``在入队和出队的并发访问中是可以保证数据正确的。

```java

/**
 * 构造函数会先创建一个不存放任何数据的头节点
 */

public LinkedBlockingQueue(int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException();
    this.capacity = capacity;
    //创建一个不存放数据的头节点，并且last和head初始化均指向这个头节点
    last = head = new Node<E>(null);
}

public LinkedBlockingQueue() {
    this(Integer.MAX_VALUE);
}

/**
 * 存放数据的节点类
 */
static class Node<E> {
    E item;  // 具体的数据

    /**
     * One of:
     * - the real successor Node
     * - this Node, meaning the successor is head.next
     * - null, meaning there is no successor (this is the last node)
     */
    Node<E> next;  //指向队列下一个元素的引用

    Node(E x) {
        item = x;
    }
}
```

```java
/**
 * 在入队和出队操作中，都是先获取count的值，然后再
 * 对count进行加1（入队）或者减1（c出队）操作。
 */

public void put(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();
    // Note: convention in all put/take/etc is to preset local var
    // holding count negative to indicate failure unless set.
    int c = -1;
    Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;

    //注意：count是AtomicInteger类型的。从而保证了count的内存可见性
    //count表示的是当前队伍中元素的个数，是LinkedBlockingQueue的一个成员变量
    final AtomicInteger count = this.count;

    putLock.lockInterruptibly();
    try {
        while (count.get() == capacity) {
            notFull.await();
        }
        enqueue(node);  //将节点入队
        /**
         * 注意：
         *   1.是先将节点入队之后才进行原子的加1操作。如果整个入队操作
         *     没有完成，那么消费者在已经将所有的元素出队，也不能把这个元素
         *     出队，避免造成数据混乱错误的现象
         *   2.先是get然后再进行原子性的减1操作
         */

        c = count.getAndIncrement();
        if (c + 1 < capacity)
            //说明入队一个元素之后还没到队列的容量（可能此时出队线程也进行了操作
            //此时c与c = count.ggetAndIncrement()得到的c不相同。但是没关系，如果
            //此时出队线程进行了操作，那么c肯定是比原子性得到的c要小）
            notFull.signal();  // 唤醒同类的生产者线程。
    } finally {
        putLock.unlock();
    }
    /**
     1.如果c == 0，而c是在入队操作之前获取的（c=getAndIncrement），
       所以此时c == 0，说明在入队操作之前队列已经为空，那么可能出现
       的一个情况就是所有的出队线程均处在await()状态。注意是可能出现这种情况的。
       因为出队线程和入队线程在上述的代码中只唤醒自己同类的线程。而之前的生产者消费者
       是唤醒对方线程。所以，如果没有下面的if，可能所有的出队线程此时已经阻塞。没有其他的线程
       将其唤醒。所以下面的语句必须要有。
     */
    if (c == 0)
        signalNotEmpty();  // 唤醒一个出队线程。
}

```

说明：``LinkedBlockingQueue``是一个单向队列，队列的头节点不存储元素。示意图如下：
![``LinkedBlockingQueue``示意图](../Image/LinkedBlockingQueue.png)

```java

/**
 * 处理入队和出队的操作
 */
private void enqueue(Node<E> node) {
    // assert putLock.isHeldByCurrentThread();
    // assert last.next == null;
    last = last.next = node;

    /**
     * 上面代码可以拆解成下面几步：
     * node = new Node(E);
     * last.next = node;
     * last = last.next;
     */
}

/**
 * 出队逻辑：
 * 将当前头节点的下一个节点作为新的头节点，并在上述操作之前将该节点的元素返回
 * 作为出队的元素
 */
private E dequeue() {
    // assert takeLock.isHeldByCurrentThread();
    // assert head.item == null;
    Node<E> h = head;
    Node<E> first = h.next;
    h.next = h; // help GC
    head = first;
    E x = first.item;
    first.item = null;
    return x;
}
```

## ``ArrayBlockingQueue``和``LinkedBlockingQueue``比较

1. **队列大小不同**
    + ``ArrayBlockingQueue``是**有界**的，即在创建``ArrayBlockingQueue``实例时，必须在构造函数中指定``capacity``。而``LinkedBlockingQueue``可以是无界的。即在创建``LinkedBlockingQueue``时，可以指定容量大小，也可以不指定容量大小（**即此时创建的是无界队列**）。如果不指定的话，那么``capacity``的默认为``Integer.MAX_VALUE``。

    + 如果``LinkedBlockingQueue``在初始化实例时没有指定容量大小，那么容量为``Integer.MAX_VALUE``。这样的话，如果入队线程添加元素的速度大于出队线程出队元素的速度，那么由于没有队列的容量限制，所以不会发生阻塞，一直入队，造成队列元素个数越来越多，从而可能造成内存溢出，发生``OOM``。

2. **存储队列元素的容器不同**

    + ``ArrayBlockingQueue``采用的是数组作为数据存储容器。且数组的容量大小在创建``ArrayBlockingQueue``实例时即必须要指定，并且之后不能够更改。即创建``ArrayBlockingQueue``时，就要给**数组赋予一大片连续的内存空间**。

    + ``LinkedBlockingQueue``是采用链表作为存储对象的数组。链表的每一个节点是该类的一个内部类``Node``。每次入队线程添加一个新的元素到队列中去，都会新创建一个``Node``节点来存储这个元素。所以，``LinkedBlockingQueue``不会像``ArrayBlockingQueue``一样，初始化就需要申请占用一大片连续的内存，初始化仅仅会创建一个**不存储数据**的头节点。

3. **对``GC``可能造成影响**

    + 由于``ArrayBlockingQueue``采用的数组来存储元素，所以在插入和删除数据的时候，不会产生或者销毁额外的对象。而``LinkedBlockingQueue``由于每次入队一个元素都会额外的生成一个``Node``对象实例来存储这个元素，在出队时，将头节点``Node``对象给销毁，进行``GC``操作。所以，在长时间内需要高效并发的处理大批量数据的时候，对``GC``可能造成影响。

4. **内部实现同步的锁不一样**

    + ``ArrayBlockingQueue``内部全局只有一个锁对象。出队线程和入队线程都必须获得这个锁对象才能进行出队和入队的操作。也就是说，同一时刻，只能有一个线程获取到锁。可能是出队线程，也可能是入队线程。

        ```java
        /**
         * Main lock guarding all access
         */
        final ReentrantLock lock;

        /**
         * Condition for waiting takes
         */
        private final Condition notEmpty;

        /**
         * Condition for waiting puts
         */
        private final Condition notFull;
        ```

    + ``LinkedBlockingQueue``内部定义了两个锁对象。所有的入队线程获取的是同一个锁对象，所有的出队线程获取的是同一个锁对象。出队线程获取锁与入队线程获取锁相互不干扰。也就是说，出队线程和入队线程可以同时进行。由于代表此时队列中元素个数的成员变量``count``是``AtomicInteger``类型的，从而保证了数据的一致性。

        ```java
        /** Lock held by take, poll, etc */
        private final ReentrantLock takeLock = new ReentrantLock();

        /** Wait queue for waiting takes */
        private final Condition notEmpty = takeLock.newCondition();

        /** Lock held by put, offer, etc */
        private final ReentrantLock putLock = new ReentrantLock();

        /** Wait queue for waiting puts */
        private final Condition notFull = putLock.newCondition();
        ```

    + 由于``LinkedBlockingQueue``实现了锁分离，即入队线程采用的是``putLock``，出队线程采用的是``takeLock``，这样能大大的提高队列的**吞吐量**，也就是意味着在高并发的情况下生产者和消费者可以同时并行的操作队列中的数据，以此来提高整个队列的并发性能。

        ```java
        //TODO 啥叫吞吐量
        ```

    + ``LinkedBlockingQueue``记录当前队列元素个数的``count``是``AtomicInteger``类型的，而``ArrayBlockingQueue``记录队列元素个数的``count``是普通``int``类型的。

        ```java
        //LinkedBlockingQueue
         private final AtomicInteger count = new AtomicInteger();
        //ArrayBlockingQueue
        int count;
        ```
