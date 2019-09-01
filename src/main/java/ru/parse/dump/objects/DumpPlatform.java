package ru.parse.dump.objects;

/**
 *
 * The platform type generated the dump file.
 *
 */
public enum DumpPlatform {
    _32_BIT(4, "x86 (32-bit)"), _64_BIT(8, "64-bit");

    int bytes;
    String name;

    DumpPlatform(int bytes, String name) {
        this.bytes = bytes;
        this.name = name;
    }

    public int getBytes() {
        return bytes;
    }

    @Override
    public String toString() {
        return name;
    }
}
