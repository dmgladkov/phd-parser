package ru.parse.dump.objects;

/**
 *
 * Java array of primitives.
 *
 */
public class DumpPrimitiveArray {
    private final long address;
    private final DumpPrimitiveType type;
    private final long length;
    private final int hash;
    private final long size;

    public DumpPrimitiveArray(long address, DumpPrimitiveType type, long length, int hash, long size) {
        this.address = address;
        this.type = type;
        this.length = length;
        this.hash = hash;
        this.size = size;
    }

    public long getAddress() {
        return address;
    }

    public DumpPrimitiveType getType() {
        return type;
    }

    public long getLength() {
        return length;
    }

    public int getHash() {
        return hash;
    }

    public long getSize() {
        return size;
    }
}
