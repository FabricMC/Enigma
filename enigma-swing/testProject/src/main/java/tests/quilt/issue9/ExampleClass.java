/*
 * Class to reproduce issue #9
 * https://github.com/QuiltMC/enigma/issues/9
 */

package tests.quilt.issue9;

public class ExampleClass extends SuperClass {
    public ExampleClass(int x, int y, int z) {
        super(x, y, z);
    }

    public ExampleClass foo() {
        System.out.println("bar");
        return this;
    }

    public ExampleClass foo(int x) {
        return null;
    }

    public ExampleClass foo(int x, int y) {
        return baz(y);
    }

    public ExampleClass bar() {
        return new ExampleClass(getX(), -1, 0);
    }

    public ExampleClass bar(int x) {
        return baz(-1, x);
    }

    public ExampleClass baz(int xz) {
        return new ExampleClass(xz, getY(), getZ() + xz);
    }

    public ExampleClass baz(int xz, int y) {
        if (y == 0) {
            return this;
        }
        return new ExampleClass(getX() - xz, getY() * y, getZ() + xz);
    }

    public static class InnerSubClass extends ExampleClass {
        public InnerSubClass(int x, int y, int z) {
            super(x, y, z);
        }
    }
}
