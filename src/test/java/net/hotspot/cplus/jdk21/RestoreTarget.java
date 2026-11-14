package net.hotspot.cplus.jdk21;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class RestoreTarget {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Marker {
        int value();
    }

    @Marker(42)
    public static int compute(int x) {
        int a = x + 1;
        try {
            return a * 3;
        } catch (RuntimeException e) {
            return -1;
        }
    }
}
