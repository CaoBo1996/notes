public class ShitTask implements Runnable {

    private Washroom washroom;

    private String name;

    public ShitTask(Washroom washroom, String name) {
        this.washroom = washroom;
        this.name = name;
    }

    @Override
    public void run() {
        synchronized (washroom.getLock()) {
            System.out.println(name + " 获取了厕所的锁");
            while (!washroom.isAvailable()) {
                try {
                    washroom.getLock().wait();  //调用锁对象的wait()方法，让出锁，并把当前线程放到与锁关联的等待队列
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println(name + " 上完了厕所");
        }
    }
}