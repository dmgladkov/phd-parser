package ru.parse.dump;

import ru.parse.dump.objects.DumpHeader;

public class DumpParsingStatistics {
    private final DumpHeader header;
    private final long totalClassesParsed;
    private final long totalObjectsParsed;
    private final long totalObjectArraysParsed;
    private final long totalPrimitiveArraysParsed;

    public DumpParsingStatistics(DumpHeader header, long totalClassesParsed, long totalObjectsParsed, long totalObjectArraysParsed, long totalPrimitiveArraysParsed) {
        this.header = header;
        this.totalClassesParsed = totalClassesParsed;
        this.totalObjectsParsed = totalObjectsParsed;
        this.totalObjectArraysParsed = totalObjectArraysParsed;
        this.totalPrimitiveArraysParsed = totalPrimitiveArraysParsed;
    }

    public DumpHeader getHeader() {
        return header;
    }

    public long getTotalClassesParsed() {
        return totalClassesParsed;
    }

    public long getTotalObjectsParsed() {
        return totalObjectsParsed;
    }

    public long getTotalObjectArraysParsed() {
        return totalObjectArraysParsed;
    }

    public long getTotalPrimitiveArraysParsed() {
        return totalPrimitiveArraysParsed;
    }

    @Override
    public String toString() {
        return "DumpParsingStatistics{" +
                "header=" + header +
                ", totalClassesParsed=" + totalClassesParsed +
                ", totalObjectsParsed=" + totalObjectsParsed +
                ", totalObjectArraysParsed=" + totalObjectArraysParsed +
                ", totalPrimitiveArraysParsed=" + totalPrimitiveArraysParsed +
                '}';
    }
}
