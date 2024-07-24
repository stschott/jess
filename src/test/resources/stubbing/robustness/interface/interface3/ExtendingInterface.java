package interface3;

import org.example.SomeObject2;

interface ExtendingInterface extends SomeObject2, AutoCloseable {
    void method2(String a);
}