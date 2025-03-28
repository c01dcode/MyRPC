package cn.edu.ustc.protocol;



import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;


public class WorkSpace {
    static Semaphore mod1  = new Semaphore(1);
    static Semaphore mod2 = new Semaphore(0);
    static Semaphore mod0 = new Semaphore(0);
    static int cnt = 1;
    static int max = 99;
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        new Thread(new Task(mod1, mod2), "Thread-1").start();
        new Thread(new Task(mod2, mod0), "Thread-2").start();
        new Thread(new Task(mod0, mod1), "Thread-3").start();
    }
    static class Task implements Runnable {
        Semaphore consume;
        Semaphore produce;

        public Task(Semaphore consume, Semaphore produce) {
            this.consume = consume;
            this.produce = produce;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    consume.acquire();
                    if (cnt > max)
                        break;
                    System.out.println(Thread.currentThread().getName() + " " + cnt++);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    produce.release();
                }
            }
        }
    }
}


