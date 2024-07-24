import org.apache.commons.io.LineIterator;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;

class Test {
    void method(File file) throws IOException {
        LineIterator it = FileUtils.lineIterator(file, "UTF-8");
        try {
            while (it.hasNext()) {
                String line = it.nextLine();
            }
        } finally {
            LineIterator.closeQuietly(it);
        }
    }
}