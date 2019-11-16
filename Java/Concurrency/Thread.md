# 线程

## 创建线程的3种方式

``Java``中任务被抽象成了一个``Runnable``

1. 继承``Thread``类

    ```java
    public class Thread implements Runnable {

        private Runnable target;


        public Thread(Runnable target) {
            //构造方法中传入任务对象给成员变量target
            init(null, target, "Thread-" + nextThreadNum(), 0);
        }

        public Thread(Runnable target, String name) {
            //传入任务和线程名字
            init(null, target, name, 0);
        }

        public Thread() {
            //什么都不传入，此时target初始化为null
            init(null, null, "Thread-" + nextThreadNum(), 0);
        }

        @Override
        public void run() {

            //override Runnable接口中的run()方法。该run()方法执行target
            //而terget即传入的任务对象
            if (target != null) {
                target.run();  // 如果target为空，即创建该线程时没有传入任务
            }
        }

        // ... 省略其他方法和字段
    }
    ```

    可见，``Thread``类实现了``Runnable``接口。``Thread``类就是一个线程类，``Java``中所有的线程实例都是``Thread``类或者其子类的实例。``Thread``实例化时将传入的任务对象赋值给成员变量``target``。然后``run()``方法中执行的就是这个任务。

2. 使用``Callable``和``Future``创建线程

    1. ``Callable``接口源码如下

        ```java
        public interface Callable<V> {
            /**
             * Computes a result, or throws an exception if unable to do so.
             * @return computed result
             * @throws Exception if unable to compute a result
             *
             * call()方法与run()方法类似，即线程执行体。比run()方法更强大，主要在于
             * 有返回值。并且可以声明抛出异常
             */



            V call() throws Exception;
        }
        ```

    2. ``Future``

        是一个接口，提供了一些对任务进行处理的相应操作。
        1. 源码如下（``FutureTask``中的实现）

            ```java
            public boolean isCancelled() {

                //cancell()方法返回true，那么则返回true。
                return state >= CANCELLED;
            }

            public boolean isDone() {
                return state != NEW;
            }
            ```

        2.

    3. ``FutureTask``

        包装``Callable``实例。因为``Callable``不是``Runnable``的子接口，所以不能直接作为``Thread``的``target``，所以要对``Callable``对象进行包装。使之能作为target被``Thread``执行。由于``FutureTask``实现了``Runnable``接口，所以可以作为``target``被``Thread``执行。
        ![FutureTask](../Image/FutureTask.png)
        根据``FutureTask``的``run()``函数源码可知，``FutureTask``只能被执行一次。``FutureTask``作为一个任务类（因为是``Runnable``接口的实现类），创建该类实例即代表着创建了一个任务实例。任务也定义了几个状态，从而用来支撑``cancell()``方法来取消任务。

        ```java
        /**
         * The run state of this task, initially NEW.  The run state
         * transitions to a terminal state only in methods set,
         * setException, and cancel.  During completion, state may take on
         * transient values of COMPLETING (while outcome is being set) or
         * INTERRUPTING (only while interrupting the runner to satisfy a
         * cancel(true)). Transitions from these intermediate to final
         * states use cheaper ordered/lazy writes because values are unique
         * and cannot be further modified.
         * <p>
         * Possible state transitions:
         * NEW -> COMPLETING -> NORMAL
         * NEW -> COMPLETING -> EXCEPTIONAL
         * NEW -> CANCELLED
         * NEW -> INTERRUPTING -> INTERRUPTED
         * 线程的状态只能被set(),setException(),cancel()方法所改变。
         */
        private volatile int state;
        private static final int NEW = 0;
        private static final int COMPLETING = 1;
        private static final int NORMAL = 2;
        private static final int EXCEPTIONAL = 3;
        private static final int CANCELLED = 4;
        private static final int INTERRUPTING = 5;
        private static final int INTERRUPTED = 6;
        private volatile Thread runner;  // 执行这个FutureTask任务的线程对象
        ```

        1. 源码

            ```java

            /**
             * Thread执行的还是run()方法。只不过FutureTask的run()将Callable对象的call()方法进行了封装
             * 即本质上是在FutureTask的run()方法里调用了Callable对象的call()方法
             * FutureTask在创建实例时，会将传入的Callable对象实例赋值给内部成员变量callable
             */
            public void run() {
                //FutureTask是只能只能一次的任务，当第一次执行完之后，那么任务的状态就不能再次回到NEW状态。
                if (state != NEW ||
                    !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                                null, Thread.currentThread()))

                    //注意：同一时间只能有一个线程执行run方法成功 ，因为在执行run()之前，首先要
                    //判断此时有没有线程在执行，即如果没有线程即null，那么CAS执行成功。
                    return;
                try {
                    Callable<V> c = callable;
                    if (c != null && state == NEW) {  // 即如果这个if条件判断成立，那么call()方法可能会执行
                    //可能会抛出出现中断异常
                        V result;
                        boolean ran;
                        try {

                            //注意：在执行set和setException()方法之前，线程的状态一直为NEW
                            result = c.call(); //执行callable的call()方法。而callable即是传入的参数
                            ran = true;
                        } catch (Throwable ex) {
                            result = null;
                            ran = false;
                            setException(ex);  // 调用call()方法发生异常，将任务状态从NEW->COMPLETING->EXCEPTIONAL
                        }
                        if (ran)
                            set(result);  // 将状态从NEW->COMPLETING->NORMAL.即任务正常执行完状态变化
                    }
                } finally {
                    // runner must be non-null until state is settled to
                    // prevent concurrent calls to run()
                    runner = null;
                    // state must be re-read after nulling runner to prevent
                    // leaked interrupts
                    int s = state;
                    if (s >= INTERRUPTING)
                        handlePossibleCancellationInterrupt(s);
                }
            }

            public boolean cancel(boolean mayInterruptIfRunning) {
                /*
                如果线程不是NEW状态，说明任务已经完成，已经被取消，或者被中断。
                那么方法直接返回false。如果是NEW状态，并且后续
                也能原子性将状态从NEW->INTERRUPTING或者CANCELLED，那么执行接下来的
                语句部分，最终返回true

                1. 如果mayInterruptIfRuning为true，那么状态NEW->INTERRUPTING->INTERRUPTED
                2. 如果mayInterruptIfRuning为false，那么状态NEW->CANCELLED

                不论是1还是2，最终都会返回true。
                */
                if (!(state == NEW &&
                //也就是说只要Task还没有执行，那么只要执行cancell，那么状态肯定会从NEW转变成其他状态
                        UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
                                mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
                    return false;
                try {    // in case call to interrupt throws exception
                    if (mayInterruptIfRunning) {
                        try {
                            Thread t = runner;
                            if (t != null)  // 此处可能为null，因为可能无线程执行这个任务
                                t.interrupt();  // 中断正在执行这个任务的线程
                        } finally { // final state，将任务的状态设置为INTERRUPTED.
                            UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
                        }
                    }
                } finally {
                    finishCompletion();  // 不会进行线程状态的设置
                }
                return true;
            }

            /**
             * cancell()和run()方法交叉执行分析：
             * 1.因为在run()方法中，本质上执行的是callable实例对象的call()方法
             * 并且在未执行set()方法或者setException()方法之前，任务的状态仍然是NEW
             *
             * 2. 分情况讨论：
             *
             * 2.1.如果Task已经被线程执行，即调用call()方法（正在调用或者调用完，状态还没改变），
             *     然后执行cancell()方法
             *
             *     1.调用cancell(false)成功即cancell()方法if条件判断不成立：
             *       如果此时callable.call()还没有返回，或者已经返回了，但是状态还没有发生改变
             *       即还没有调用set()或者setException()（发生异常的情况下会调用这个方法，并且
             *       不会再调用set()方法），那么callable()不会对任务的执行（即执行call()方法）有
             *       什么影响。仅仅会改变任务状态（即任务的最终状态是cancell）。
             *
             *     2.调用cancell(true)成功即也是if条件判断不成立:
             *       此时set()和setException()会更新状态失败。cancell()方法会将线程的中断标志位
             *       设置为true。如果call()没有返回，那么可能会对任务有影响。因为
             *       任务可能会相应中断。如果call()已经返回，那么仅仅会改变任务的状态（任务的最终状态为Inerrupted）
             *
             *2.2.先调用cancell()方法成功再执行Task
             *
             *     1.因为call()不论传入的参数是true还是false。cancell()方法的成功调用都会改变此时任务的状态。
             *        所以肯定Task不会执行，因为Task执行的前提任务状态必须是NEW。
             *
             *2.3.cancell()成功调用会返回true。失败会返回false。如果cancell()方法返回true，那么
             *    isCancelled()方法返回true。但cancell()方法返回true。不代表任务call()方法就没有被执行。
             */

            ```

        2. 构造方法

            ```java

            /**
             * 直接传入一个Callable对象，将实例赋值给内部成员变量callable
             */
            public FutureTask(Callable<V> callable) {
                if (callable == null)
                    throw new NullPointerException();
                this.callable = callable;
                this.state = NEW;  // 创建一个FutureTask实例，会将State状态设置为NEW.
            }


            /**
             * 传入Runnable对象实例和V类型的结果，如果成功执行完成，返回result
             */
            public FutureTask(Runnable runnable, V result) {
                this.callable = Executors.callable(runnable, result);
                this.state = NEW;
            }

            public static <T> Callable<T> callable(Runnable task, T result) {
                if (task == null)
                    throw new NullPointerException();
                return new RunnableAdapter<T>(task, result);
            }

            /**
             * 注意：RunnableAdapter类实现了Callable类
             */
            static final class RunnableAdapter<T> implements Callable<T> {
                final Runnable task;
                final T result;
                RunnableAdapter(Runnable task, T result) {
                    this.task = task;
                    this.result = result;
                }

                //call()方法本质上是调用Runnable的run()方法
                public T call() {
                    task.run();
                    return result;  // run()方法成功完成，那么会将result返回
                }
            }

            ```

        3. 适配器模式



