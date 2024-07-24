import org.example.SomeObject;

import java.io.IOException;

class MethodCall17 extends SomeObject{

    protected int method() throws IOException {
        int c = super.method();
        return c;
    }
}
