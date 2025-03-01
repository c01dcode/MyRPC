package cn.edu.ustc.protocol;


public class WorkSpace {
    public static void main(String[] args) {
        System.out.println("主函数");
        new WorkSpace();
        new WorkSpace();

    }
    public WorkSpace () {
        System.out.println("无参构造");
    }
    {
        System.out.println("代码块");
    }
    static {
        System.out.println("静态代码块");
    }
}
