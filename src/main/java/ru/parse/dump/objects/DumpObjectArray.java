package ru.parse.dump.objects;

/**
 *
 * Java array of objects.
 *
 */
public class DumpObjectArray {
    private final long address;
    private final long classAddress;
    private final int hash;
    private final long size;
    private final long actualLength;
    private final long[] references;

    public DumpObjectArray(long address, long classAddress, int hash, long size, long[] references) {
        this(address, classAddress, hash, size, references.length, references);
    }

    public DumpObjectArray(long address, long classAddress, int hash, long size, long actualLength, long[] references) {
        this.address = address;
        this.classAddress = classAddress;
        this.hash = hash;
        this.size = size;
        this.actualLength = actualLength;
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

    public long getSize() {
        return size;
    }

    public long getActualLength() {
        return actualLength;
    }

    public long[] getReferences() {
        return references;
    }
}
