import org.test.SomeObject;

class FieldAccess8 {
    void method() {
        SomeObject so = new SomeObject();
        int[] a = new int[4];
        int b = a[so.number];
    }
}