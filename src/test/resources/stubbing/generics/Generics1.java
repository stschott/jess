import org.example.SomeObject;
import org.example.SomeOtherObject;

class Generics1 {
    void method() {
        SomeObject<String, Integer, SomeOtherObject> so = new SomeObject<>();
    }
}