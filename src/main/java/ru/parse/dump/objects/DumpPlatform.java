package ru.parse.dump.objects;

/**
 *
 * The platform type generated the dump file.
 *
 */
public enum DumpPlatform {
    _32_BIT(4), _64_BIT(8);

    int bytes;

    DumpPlatform(int bytes) {
        this.bytes = bytes;
    }

    public int getBytes() {
        return bytes;
    }
}
