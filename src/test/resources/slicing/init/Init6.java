class Init6 {
    static { int a = 3; }
    static { int a = 3; }
    { int a = 3; }
    void method() {

    }
    static class Inner {
        static { int a = 3; }
    }

    static class OtherInner {
        static  { int a = 3; }
        { int a = 3; }
    }
}