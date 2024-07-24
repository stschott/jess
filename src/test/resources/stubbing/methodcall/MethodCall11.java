import org.example.SomeObject;

class MethodCall11 {
    void method() {
        int a = getObj().add();
    }

    private SomeObject getObj() {
        return new SomeObject();
    }
}
