/*
 * Class to reproduce issue #9
 * https://github.com/QuiltMC/enigma/issues/9
 */

package tests.quilt.issue9;

public class SuperClass {
    private int x;
    private int y;
    private int z;

    public SuperClass(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public SuperClass foo() {
        System.out.println("foo");
        return this;
    }

    public SuperClass foo(int x) {
        return new SuperClass(x, 0, 0);
    }

    public SuperClass foo(int x, int y) {
        return bar(1);
    }

    public SuperClass bar() {
        return new SuperClass(0, 0, 0);
    }

    public SuperClass bar(int x) {
        return baz(1, x);
    }

    public SuperClass baz(int xz) {
        return new SuperClass(xz, 0, xz);
    }

    public SuperClass baz(int xz, int y) {
        if (y == 0) {
            return this;
        }
        return new SuperClass(getX(), y, xz);
    }
}
