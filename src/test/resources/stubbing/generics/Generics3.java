import org.example.SomeObject;
import org.example.SomeOtherObject;

class Generics3 {
    void method() {
        SomeObject<SomeOtherObject<String>> so = new SomeObject<>();
    }
}
