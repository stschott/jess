import org.example.SomeObject;

class MethodCall16 {
    void method() {

        if (true) {
            SomeObject so = new SomeObject();
            Object c = so.visit();
        }
    }
}
