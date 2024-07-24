import org.example.SomeObject;

class MethodCall4 {
    void method() {
        SomeObject so = new SomeObject();
        int a = so.visit(5, true, 3.5, "abc", 'z');
    }
}
