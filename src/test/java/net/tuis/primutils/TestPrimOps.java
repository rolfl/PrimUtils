package net.tuis.primutils;

import static net.tuis.primutils.PrimOps.*;

import java.util.stream.Stream;

@SuppressWarnings("javadoc")
public class TestPrimOps {
    
    public static void main(String[] args) {
        final int size = 10000;
        String[][] data = buildValueMatrix(String.class, getRowsFor(size));
        for (int i = 0; i < size; i++) {
            data = ensure(String.class, data, i);
            setValue(data, i, String.format("%d", i));
        }
        long filledcount = Stream.of(data).filter(r -> r != null).mapToLong(row -> Stream.of(row).filter(v -> v != null).count()).sum();
        System.out.printf("There are %d populated values in data[][]\n", filledcount);
        for (int i = 100; i < 110; i++) {
            System.out.printf("Value at %d is %s\n", i, getValue(data, i));
        }
        // add another 10,000;
        for (int i = 0; i < size; i++) {
            data = ensure(String.class, data, i + size);
            setValue(data, i + size, String.format("%d", i));
        }
        filledcount = Stream.of(data).filter(r -> r != null).mapToLong(row -> Stream.of(row).filter(v -> v != null).count()).sum();
        System.out.printf("There are %d populated values in data[][]\n", filledcount);
        for (int i = 19995; i < 20005; i++) {
            System.out.printf("Value at %d is %s\n", i, getValue(data, i));
        }
    }

}
