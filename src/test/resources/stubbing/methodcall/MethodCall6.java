import org.example.SomeObject;

class MethodCall6 {
    long y = 5000L;

    void method(char x) {
        SomeObject so = new SomeObject();
        boolean b = false;
        String c = "abc";
        String a;
        a = so.visit(b, c, x, y);
    }
}