import org.example.SomeObject;

class MethodCall1 {
    void method() {
        SomeObject so = new SomeObject();
        // the method can either return void or drop the returned value, which will produce different bytecode
        so.visit();
    }
}
