package net.tuis.primutils;

@SuppressWarnings("javadoc")
public class PushIntIntMap {

    public static void main(String[] args) {
        int last = 0;
        try {
            IntIntMap iim = new IntIntMap(-1, 1 << 24);
            for (int i = 0; i <= 500_000_000; i++) {
                iim.put(i,i);
                last = i;
                if (last % 10000000 == 0) {
                    System.out.println(last + " -> " + iim.toString());
                }
            }
        } catch (Throwable t) {
            System.out.println(last);
            t.printStackTrace();
        }
    }

}
