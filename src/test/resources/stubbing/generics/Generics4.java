import org.example.SomeObject;
import org.example.SomeObject2;
import org.example.SomeObject3;

import java.util.List;

import java.io.IOException;

class Generics4 {

    void method(SomeObject<? extends SomeObject2> arg) throws IOException {

    }

    void method2(List<SomeObject3> a) {

    }
}
