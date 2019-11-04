class FinalDemo {
    private int i;
    private final int j;
    private static FinalDemo obj;

    public FinalDemo() {
        i = 1;
        j = 2;
    }

    public static void writer() {
        obj = new FinalDemo(); // 即可能构造函数执行结束，但是还是没有对这个i进行赋值，此时obj读取到的i是一个0（初始值）
    }

    public static void reader() {
        FinalDemo demo = obj;  // 这个可能会发生空指针异常
        int i = demo.i;
        /*
        i的值可能为初始值0，也有可能被重排序到FinalDemo demo = obj;指令之前，从而造成空指针异常。
        Exception in thread "ReadThread" java.lang.NullPointerException
	    at FinalDemo.reader(FinalDemoTest.java:17)  // 即说明int i = demo.i;指令被重排序到Final demo = obj;指令之前。
	    at TargetReader.run(FinalDemoTest.java:32)
	    at java.lang.Thread.run(Thread.java:748)
        */
        int j = demo.j;
    }
}

class TargetWriter implements Runnable{
    @Override
    public void run(){
        FinalDemo.writer();
    }
}

class TargetReader implements Runnable{
    @Override
    public void run(){
        FinalDemo.reader();
    }
}

class FinalDemoTest{
    public static void main(String[] args) throws InterruptedException {
        /*
        如果先执行读的线程，那么可能会出现NullPointException。因为初始化 finalDemo为null
         */
        new Thread(new TargetWriter(), "WriteThread").start();
        Thread.sleep(5);
        new Thread(new TargetReader(), "ReadThread").start();
    }
}