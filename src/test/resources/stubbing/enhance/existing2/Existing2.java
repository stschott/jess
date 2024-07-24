package existing2;

class Existing2 {

    void method() {
        SomeObject so = new SomeObject();
        so.visit1("abc");
        int a = so.visit1(123);
    }
}