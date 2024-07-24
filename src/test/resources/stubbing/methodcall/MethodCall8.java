import org.example.SomeObject;

class MethodCall8 {
    void method() {
        SomeObject so = new SomeObject();
        int a = so.visit(method2(method3()));
    }

    String method2(boolean a) {
        return "abc";
    }

    boolean method3() {
        return false;
    }
}
