package ru.parse.dump.objects;

/**
 *
 * Java class.
 *
 */
public class DumpClass {
    private final long address;
    private final long superClassAddress;
    private final long instanceSize;
    private final String className;
    private final int hash;
    private final long[] references;

    public DumpClass(long address, long superClassAddress, long instanceSize, String className, int hash, long[] references) {
        this.address = address;
        this.superClassAddress = superClassAddress;
        this.instanceSize = instanceSize;
        this.className = className;
        this.hash = hash;
        this.references = references;
    }

    public long getAddress() {
        return address;
    }

    public long getSuperClassAddress() {
        return superClassAddress;
    }

    public long getInstanceSize() {
        return instanceSize;
    }

    public String getClassName() {
        return className;
    }

    public int getHash() {
        return hash;
    }

    public long[] getReferences() {
        return references;
    }
}
