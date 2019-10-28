# AtomicInteger源码分析

对``AtomicInteger``深入了解之前，首先要明确悲观锁与乐观锁的概念。首先这两个概念是广义上的概念，并不是说真的有锁的名称是乐观锁或者悲观锁。乐观锁或者悲观锁体现了看待线程同步的不同态度。即乐观的态度和悲观的态度。
**总结如下：**对于一个共享变量而言，由于每一个线程均能访问该共享变量，所以可能会出现一些数据不同步的问题。

1. **悲观锁**认为：**自己在使用这个数据的时候，悲观的认为一定会有别的线程来修改数据，所以自己在获取这个数据之前，先判断这个数据有没有被别的线程加锁，如果没有加锁，那么自己会对这个数据进行加锁，从而确保自己在使用的过程中没有别的线程来修改这个数据。在``Java``中，``Synchronized``和``Lock``实现类均是悲观锁**。之所以称之为悲观锁，因为体现了一种悲观的态度，即认为自己在使用某个共享数据的同时肯定会有其他线程来修改这个数据。

2. **乐观锁**认为：**自己在使用这个数据的时候，乐观的认为不会有别的线程来修改这个变量，所以不会去添加锁，只是每次在更新数据的时候去判断之前有没有别的线程更新了这个数据，如果这个数据没有被更新，当前线程将自己修改的数据成功写入。如果数据已经被其他线程更新，则根据不同的实现方式执行不同的操作（例如报错或者自动重试）。乐观锁在``Java``中是通过使用无锁编程来实现，最常采用的是``CAS``算法，``Java``原子类中的递增操作就通过``CAS``自旋实现的**。之所以称之为乐观锁，因为其乐观的认为自己在操作数据的时候不会有别的线程来更新。

3. 源码分析

    1. 构造方法

        ```java
        public AtomicInteger(int initialValue) {
            value = initialValue; // 根据传入的initialValue，初始化value的值，而value即实际原子变量，类型为int。
        }

        public AtomicInteger() {
            //默认初始化value的值为0
        }
        ```

    2. 重要的属性

        ```java
        /*
        实际存放int类型数据的的变量，注意该变量运用了volatile关键字修饰。从而该变量是内存可见的。
        一个线程修改了这个数据，对其他线程都是立马可见的。即其他的线程获取这个变量的值，得到的都是最新的值。
        */
        private volatile int value;

        private static final long valueOffset; // value变量在对象所在的内存的地址偏移量

        static {
            try {
                //通过该方法获取到value在内存中的地址，那么后续可以直接通过这个地址对这个变量进行操作。
                valueOffset = unsafe.objectFieldOffset
                    (AtomicInteger.class.getDeclaredField("value"));
            } catch (Exception ex) { throw new Error(ex); }
        }
        ```

    3. 重要方法

        + ``get()``和``set()``方法

            ```java
            /*
            注意：
            1.如果value没有用volatile修饰的话，那么get()和set()方法可能会出现数据不同步的问题。因为一个线程调用set()方法将value变量更新为新的值。
            但是如果没有volatile修饰，那么修改后的值可能不会立即刷新到主内存中。这样其他线程通过get()方法读取这个变量值，仍然从自己的本地缓存中读取。
            从而导致数据不同步
            2. 若使用了volatile关键字修饰，那么该线程的修改会立即刷新到主内存，并且通知其他线程的本地缓存无效，从而其他线程通过get()方法读取这个变
            量时，会从主内存中读取。从而得到的是该变量最新的值。从而部分解决了保证了数据同步的问题。
            */
            public final int get() {
                return value;
            }

            public final void set(int newValue) {
                value = newValue;
            }
            ```

        + 原子性更新
            1. ``getAndSet()``

                ```java
                /*
                函数作用：原子性更新当前值。并返回旧值
                如何做到原子性更新操作：
                1.调用unsafe类的getAndSetInt()方法，该方法需要的参数是：
                1.1.哪个对象，即this，表明是当前对象
                1.2.相对于该对象所在的内存的偏移地址，即valueOffset
                1.3.以及要改变成的值，即新值。

                2.getAndSetInt()方法内部有一个do-while循环。该循环每次都会调用native方法getIntVolatile()来
                获取此时value的值。注意：由于value用了volatile修饰，那么每次获取的值都是最新的值。然后调
                用compareAndSwapInt()方法，来进行原子性的比较并更新。即CAS算法。注意“比较”与“更新”是一个原子
                性的操作。由硬件保证。

                3.CAS算法思路：
                1.在do-while循环中，每次都会获取此时value的最新值（因为用的是volatile修饰）。但是可能出
                    现的一个情况就是当获取到了最新值，但此时分配给该线程的时间片用完了。此时另外的线程得到执行，该新执行的
                    线程直接无阻碍的将该值给更新，即全程没有发生时间片用完的情况。然后该线程结束。
                2.那么对于之前一开始的线程而言，获取到的最新值便不是最新值。然后该线程继续执行，当执行
                    到compareAndSwapInt()时，由于预期的值，也就是var5与此时内存中存在的值不相等。所以compareAndSwap()
                    返回false.所以继续执行循环。获取当前值，并且再次执行compareAndSwap()操作。直到所期望的值与此时内
                    存中的当前值相同。说明此时没有其他线程来更新这个值，那么直接更新这个值。即实现了原子性的更新操作。

                4.CAS算法：
                4.1.CAS全称 Compare And Swap（比较与交换），是一种无锁算法。在不使用锁（没有线程被阻塞）的情
                    况下实现多线程之间的变量同步。比较与交换虽然看起来是两个操作，但是由硬件会保证肯定是原子性的。
                    comapreAndSwap()这个函数是一个native函数，一定要注意：比较和更新在一起是一个原子性的操作。
                4.2.CAS算法涉及到三个操作数：
                    4.2.1.需要读写的内存值 V，此处通过var1来指明哪个对象，var2来指明value在对象所在的内存的地址偏移，通过这两个参数
                            来得到V
                    4.2.2.进行比较的值 A,此处即var5。即我们期望的值。即如果我们调用getIntVolatile()方法得到的值。因为
                            如果该值没有被其他的线程所修改，那么此时内存中的值和该值相等。
                    4.2.3.要写入的新值 B。即我们准备要更新的值。
                4.3.当且仅当 V 的值等于 A 时，CAS通过原子方式用新值B来更新V的值（“比较+更新”整体是一个原子操
                    作），否则不会执行任何操作。一般情况下，“更新”是一个不断重试的操作。即通过do-while循环来不断尝试更新。

                */

                public final int getAndSet(int newValue) {
                    //getAndSetInt()方法传入当前对象引用，value在对象所在的内存的地址偏移，以及要更新的新值。
                    return unsafe.getAndSetInt(this, valueOffset newValue);

                public final int getAndSetInt(Object var1, long var2, int var4) {
                    int var5; // 存储每次通过getIntVolatile()获取到的value当前值，这个值是最新的值。因为用了volatile修饰
                    do {
                    var5 = this.getIntVolatile(var1, var2); // 根据this和valueOffste来获取当前value的值
                    /*
                    期望值是var5，要更新成的值是var4。注意上一句代码与该语句之间可能会出现线程CPU时间片用完的情况。所以可能此时
                    内存中的值与var5的值不一样。
                    */
                    } while(!this.compareAndSwapInt(var1, var2, var5, var4));  // 若期望值与当前值不相等，那么函数返回false。while条件成立，继续循环

                    return var5;  // 将当前的值返回。
                }

                //本地方法，通过对象引用，以及地址偏移量来获得某个属性的值。
                public native int getIntVolatile(Object var1, long var2);

                //比较并交换，是一个原子性的操作
                public final native boolean compareAndSwapInt(Object var1, long var2, int var4, int var5);

                }
                ```

                + **注意：**``volatile``修饰变量很重要。因为只有``volatile``修饰的变量，才能保证每次获取到的都是最新值。否则的话，后续所有的操作均没有什么意义。

                + ``CAS``算法所带来的问题

                    1. ``ABA``**问题**。我们知道，只有当前值与预期值相等的情况下才会更新为新值。但是当前值与预期值相等不意味着就没发生变化。比如，原来的值是3，然后线程``A``获取到该值为3，然后线程``A``时间片用完，此时线程``B``成功的将3改为4。线程``B``结束，然后线程``C``成功的将4改为3，线程``C``结束。对于线程``A``而言，此时的3已经不能与之前的3相同对待。虽然此时二者均是3。但是线程``A``仍然会进行更新操作，因为预期值与此时的当前值相等。典型的例子运用在栈当中。
                        + 解决思路：``ABA``问题的解决思路就是在变量前面添加版本号，每次变量更新的时候都把版本号加一，这样变化过程就从“``A－B－A``”变成了“``1A－2B－3A``”
                        + ``JDK``从1.5开始提供了``AtomicStampedReference``类来解决``ABA``问题，具体操作封装在``compareAndSet()``中。``compareAndSet()``首先检查当前引用和当前标志与预期引用和预期标志是否相等，如果都相等，则以原子方式将引用值和标志的值设置为给定的更新值

                    2. **循环时间长开销大**。``CAS``操作如果长时间不成功，会导致其一直自旋，不断地进行循环操作。给``CPU``带来非常大的开销。

                    3. **只能保证一个共享变量的原子操作**。对一个共享变量执行操作时，``CAS``能够保证原子操作，但是对多个共享变量操作时，``CAS``是无法保证操作的原子性的。
                        + ``Java``从1.5开始``JDK``提供了``AtomicReference``类来保证引用对象之间的原子性，可以把多个变量放在一个对象里
                        来进行``CAS``操作。

        + 原子性自增1或者增加特定的值。
            1. ``getAndAdd()``

                ```java

                /*
                类比于getAndSet()函数，不过该函数更能体现不加锁的情况下如何解决共享变量在并发情况下的数据安全问题。
                1.由于是在当前值的基础上进行增加delta的操作，所以，必须要获取当前值。而获取到当前值之后，可能当前线程的时间片用完
                另外的线程执行成功，并且修改了当前值。那么此时的当前值是无效的。所以compareAndSwap()函数返回为false。继续在循环中
                重新获取当前值，直到compareAndSwap()函数成功返回。

                2. 相比较与getAndSet()方法，该方法必须依赖最新的当前值。而getAndSet()方法要设置成的新值不依赖当前的值。
                */
                public final int getAndAdd(int delta) {
                    return unsafe.getAndAddInt(this, valueOffset, delta);
                }

                public final int getAndAddInt(Object var1, long var2, int var4) {
                    int var5;
                    do {
                        var5 = this.getIntVolatile(var1, var2);
                        //依赖于当前值，在当前值的基础上加上delta。作为更新后的值。
                    } while(!this.compareAndSwapInt(var1, var2, var5, var5 + var4));
                    return var5;
                }
                ```