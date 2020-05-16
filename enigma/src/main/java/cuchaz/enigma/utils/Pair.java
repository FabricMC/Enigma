package cuchaz.enigma.utils;

import java.util.Objects;

public class Pair<A, B> {
    public final A a;
    public final B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(a) * 31 +
               Objects.hashCode(b);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Pair &&
               Objects.equals(a, ((Pair<?, ?>) o).a) &&
               Objects.equals(b, ((Pair<?, ?>) o).b);
    }
}
