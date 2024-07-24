import org.test.SomeException;
import org.test.SomeOtherException1;
import org.test.SomeOtherException2;

class Exception2 {
    void method()  {
        try {

        } catch (SomeException e) {

        } catch (SomeOtherException1 | SomeOtherException2 e) {

        } finally {

        }
    }
}