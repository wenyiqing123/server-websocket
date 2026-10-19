package cn.wyq.serverwebsocket.test;

public class test {
    public static void main(String[] args) {
        int a = 1, b = 1;
        Integer i1=1;
        Integer i2=1;
        System.out.println(a==b);
        System.out.println(i1==i2);
        System.out.println(i1.equals(i2));
        Stu stu1 = new Stu(1);
        Stu stu2 = new Stu(1);

        System.out.println(stu1.equals(stu2));
    }

}
class Stu{
    private int age;

    public Stu(int age) {
        this.age = age;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

//    @Override
//    public boolean equals(Object o) {
//        if (o == null || getClass() != o.getClass()) return false;
//
//        Stu stu = (Stu) o;
//        return age == stu.age;
//    }
//
//    @Override
//    public int hashCode() {
//        return age;
//    }
}
