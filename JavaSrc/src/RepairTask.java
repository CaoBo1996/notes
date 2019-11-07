public class RepairTask implements Runnable{
    private Washroom washroom;

    public RepairTask(Washroom washroom){
        this.washroom = washroom;
    }

    @Override
    public void run() {

        synchronized (washroom.getLock()) {
            System.out.println("维修工 获取了厕所的锁");
            System.out.println("厕所维修中，维修厕所是一件辛苦活，需要很长时间。。。");

            try {
                Thread.sleep(5000L);    //用线程sleep表示维修的过程
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            washroom.setAvailable(true);    //维修结束把厕所置为可用状态

            washroom.getLock().notifyAll(); //通知所有在与锁对象关联的等待队列里的线程，它们可以继续执行了
            System.out.println("维修工把厕所修好了，准备释放锁了");
        }
    }
}