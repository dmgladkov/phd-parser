package ru.parse.dump.objects;

/**
 *
 * Java object instance.
 *
 */
public class DumpObject {
    private final long address;
    private final long classAddress;
    private final int hash;
    private final long[] references;

    public DumpObject(long address, long classAddress, int hash, long[] references) {
        this.address = address;
        this.classAddress = classAddress;
        this.hash = hash;
        this.references = references;
    }

    public long getAddress() {
        return address;
    }

    public long getClassAddress() {
        return classAddress;
    }

    public int getHash() {
        return hash;
    }

    public long[] getReferences() {
        return references;
    }
}
