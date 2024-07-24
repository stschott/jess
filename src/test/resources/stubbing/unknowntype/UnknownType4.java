package org.example3;

import org.example.*;

class UnknownType4 {

    void method() {
        SomeObject so = new SomeObject();
        so.visit(so.name);
    }
}