package ru.parse.dump.objects;

/**
 * Dump header.
 */
public class DumpHeader {
    private final long version;
    private final DumpPlatform platform;
    private final boolean hashed;
    private final boolean j9VM;
    private final String jvmVersion;

    public DumpHeader(long version, DumpPlatform platform, boolean hashed, boolean j9VM, String jvmVersion) {
        this.version = version;
        this.platform = platform;
        this.hashed = hashed;
        this.j9VM = j9VM;
        this.jvmVersion = jvmVersion;
    }

    public long getVersion() {
        return version;
    }

    public DumpPlatform getPlatform() {
        return platform;
    }

    public boolean isHashed() {
        return hashed;
    }

    public boolean isJ9VM() {
        return j9VM;
    }

    public String getJvmVersion() {
        return jvmVersion;
    }

    @Override
    public String toString() {
        return "DumpHeader{" +
                "version=" + version +
                ", platform=" + platform +
                ", hashed=" + hashed +
                ", j9VM=" + j9VM +
                ", jvmVersion='" + jvmVersion + '\'' +
                '}';
    }
}
