import org.test.SomeObject;

class FieldAccess3 {
    void method() {
        SomeObject so = new SomeObject();
        // this is not even compilable
        so.size;
    }
}