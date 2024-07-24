import org.example.SomeAnnotation;
import org.example.SomeAnnotation2;

@SomeAnnotation(25)
public class Annotation2 {
    @SomeAnnotation2("abc")
    public int field = 5;

    @SomeAnnotation()
    public void visit() {

    }
}