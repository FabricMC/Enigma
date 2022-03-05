/*
 * Test class to make sure nothing is broken.
 */

package tests.quilt.issue9;

import java.util.function.Function;
import java.util.function.Supplier;

public class TestClass implements Supplier<Integer>, Function<String, Integer> {
    @Override
    public Integer get() {
        return -1;
    }

    @Override
    public Integer apply(String s) {
        return s.hashCode();
    }
}
