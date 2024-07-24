import org.test.SomeObject;

class FieldAccess7 {
    void method() {
        SomeObject so = new SomeObject();
        int[] a = new int[4];
        a[3] = so.number;
    }
}