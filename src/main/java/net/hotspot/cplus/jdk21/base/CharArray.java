package net.hotspot.cplus.jdk21.base;

public final class CharArray extends CArray {

    public CharArray(long length) {
        super(length, 2);
    }

    public char get(long i) {
        return getChar(i);
    }

    public void set(long i, char v) {
        putChar(i, v);
    }

    public String toString(long index, int count) {
        checkAlive();
        if (count < 0) throw new IllegalArgumentException();
        if (index < 0 || index + count > length) throw new ArrayIndexOutOfBoundsException();
        char[] buf = new char[count];
        for (int k = 0; k < count; k++) buf[k] = getChar(index + k);
        return new String(buf);
    }

    public void set(long index, String value) {
        if (value == null) throw new NullPointerException();
        checkAlive();
        int n = value.length();
        if (index < 0 || index + n > length) throw new ArrayIndexOutOfBoundsException();
        for (int k = 0; k < n; k++) putChar(index + k, value.charAt(k));
    }
}
