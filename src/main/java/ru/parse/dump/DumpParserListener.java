package ru.parse.dump;

import ru.parse.dump.objects.DumpHeader;

public interface DumpParserListener {
    void onHeaderReceive(long bytesRead, DumpHeader header);
    void onDataPortionReceive(long bytesRead, DumpParsingStatistics statistics);
}
