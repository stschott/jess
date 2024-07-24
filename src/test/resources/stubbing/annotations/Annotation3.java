import org.example.SomeAnnotation;

@SomeAnnotation(value1 = 5, value2 = "abc")
public class Annotation3 {
    public int field = 5;

    @SomeAnnotation(value3 = 'a')
    public void visit() {

    }
}