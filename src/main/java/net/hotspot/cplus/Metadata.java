package net.hotspot.cplus;

public class Metadata extends MetaspaceObj {
    public Metadata(long address) {
        super(address);
    }

    public Metadata(Class<?> clazz) {
        super(clazz);
    }

    public boolean isMetadata() {
        return true;
    }

    public boolean isKlass() {
        return false;
    }

    public boolean isMethod() {
        return false;
    }

    public boolean isMethodData() {
        return false;
    }

    public boolean isConstantPool() {
        return false;
    }

    public boolean isMethodCounters() {
        return false;
    }

    public int size() {
        throw new UnsupportedOperationException();
    }

    public int type() {
        throw new UnsupportedOperationException();
    }

    public String internalName() {
        throw new UnsupportedOperationException();
    }

    public void metaspacePointersDo(Object iter) {
    }

    public void print() {
    }

    public void printValue() {
    }

    public static void printValueOnMaybeNull(Object st, Metadata m) {
        if (m == null) {
            // st.print("NULL");
        } else {
            m.printValueOn(st);
        }
    }

    public void printOn(Object st) {
    }

    public void printValueOn(Object st) {
        throw new UnsupportedOperationException();
    }

    public String printValueString() {
        return "";
    }

    public boolean onStack() {
        return false;
    }

    public void setOnStack(boolean value) {
    }

    public static void markOnStack(Metadata m) {
        m.setOnStack(true);
    }
}
