/*
 * Class to reproduce issue #10
 * https://github.com/QuiltMC/enigma/issues/10
 */

package tests.quilt.issue10;

import java.util.function.Function;

public class TestClass {
    public static void foo() {
        bar(TestClass::new);
    }

    public static void bar(Function<Integer, TestClass> f) {
        TestClass tc = f.apply(1);
        System.out.println(tc);
        System.out.println(tc.increment());
    }

    private int i;

    public TestClass(int i) {
        this.i = i;
    }

    public TestClass increment() {
        this.i++;
        return this;
    }

    @Override
    public String toString() {
        return "TestClass{" + i + "}";
    }
}
