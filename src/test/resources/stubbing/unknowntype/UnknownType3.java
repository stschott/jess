package org.example3;

import org.example.*;

class UnknownType3 {

    void method() {
        SomeObject so = new SomeObject();
        so.visit(so.visit2());
    }
}