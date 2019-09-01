package ru.parse.dump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.parse.dump.objects.*;
import ru.parse.dump.vind.VirtualIndexSystem;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * A stream parsing the PHD file.
 */
public class DumpParserStream implements Closeable {
    private static final int BUFFER_SIZE = 64 * 1024 * 1024;
    private static final int BYTES_CACHE_SIZE = 1024;
    private static final Logger LOGGER = LoggerFactory.getLogger(DumpParserStream.class);

    private InputStream stream;
    private byte[] bytesCache = new byte[BYTES_CACHE_SIZE];

    private long bytesReadTotal = 0;

    private static final long READ_TOTAL_LOG_INTERVAL = 100 * 1024 * 1024;
    private long logReadTotalAfter = READ_TOTAL_LOG_INTERVAL;

    /**
     * Create a stream from common {@link InputStream}.
     *
     * @param stream
     * @return
     */
    public static DumpParserStream create(InputStream stream) {
        return new DumpParserStream(new BufferedInputStream(stream, BUFFER_SIZE));
    }

    /**
     * Create a stream from the bytes. Used mainly for the test purposes.
     *
     * @param bytes
     * @return
     */
    public static DumpParserStream create(byte[] bytes) {
        return new DumpParserStream(new ByteArrayInputStream(bytes));
    }

    /**
     *
     * Parse the PHD stream.
     * Because usually PHD files are too large to contain in memory, parsed objects are contained in the index system.
     *
     * @param virtualIndexSystem
     * @return
     * @throws IOException
     */
    public DumpParsingStatistics readObjects(VirtualIndexSystem virtualIndexSystem) throws IOException {
        DumpHeader header = readHeader();
        return readBody(header, virtualIndexSystem);
    }

    /**
     * Read the dump header.
     *
     * @return
     * @throws IOException
     */
    DumpHeader readHeader() throws IOException {
        long bytesReadBefore = bytesReadTotal;

        String title = readString();
        if (!"portable heap dump".equals(title)) {
            throw new IllegalStateException("Header has an invalid format");
        }

        long version = readUnsignedInt();
        long flags = readUnsignedInt();
        DumpPlatform platform = ((flags & 1) != 0 ? DumpPlatform._64_BIT : DumpPlatform._32_BIT);
        boolean hashed = ((flags & 2) != 0);
        boolean j9VM = ((flags & 4) != 0);

        int tag = readUnsignedByte();
        if (tag != 1) {
            throw new IllegalStateException("Header has an invalid format");
        }

        String jvmVersion = null;
        boolean endOfHeader = false;
        while (!endOfHeader) {
            int headerRecordTag = readUnsignedByte();
            switch (headerRecordTag) {
                // J9 VM tags
                case 1:
                case 3:
                    break;
                case 2:
                    endOfHeader = true;
                    break;
                case 4:
                    jvmVersion = readString();
                    break;
                default:
                    throw new IllegalStateException("Header has an invalid format");
            }
        }

        LOGGER.info("Total bytes read in header {}", bytesReadTotal - bytesReadBefore);

        return new DumpHeader(version, platform, hashed, j9VM, jvmVersion);
    }

    /**
     * Read body. Because usually PHD files are too large to contain in memory, parsed objects are contained in the index system.
     *
     * @param header
     * @param virtualIndexSystem
     * @throws IOException
     */
    DumpParsingStatistics readBody(DumpHeader header, VirtualIndexSystem virtualIndexSystem) throws IOException {
        int startTag = readUnsignedByte();
        if (startTag != 2) {
            throw new IllegalStateException("Body has an invalid format");
        }

        long totalClassesParsed = 0;
        long totalObjectsParsed = 0;
        long totalObjectArraysParsed = 0;
        long totalPrimitiveArraysParsed = 0;

        ParsingContext context = new ParsingContext();

        while (true) {
            int recordTag = readUnsignedByte();
            if ((recordTag & 0x80) != 0) {
                DumpObject object = readShortObject(header, recordTag, context);
                virtualIndexSystem.save(object.getAddress(), object);
                ++totalObjectsParsed;
            } else if ((recordTag & 0x40) != 0) {
                DumpObject object = readMediumObject(header, recordTag, context);
                virtualIndexSystem.save(object.getAddress(), object);
                ++totalObjectsParsed;
            } else if ((recordTag & 0x20) != 0) {
                DumpPrimitiveArray primitiveArray = readPrimitiveArray(header, recordTag, context);
                virtualIndexSystem.save(primitiveArray.getAddress(), primitiveArray);
                ++totalPrimitiveArraysParsed;
            } else if (recordTag == 4) {
                DumpObject object = readLongObject(header, context);
                virtualIndexSystem.save(object.getAddress(), object);
                ++totalObjectsParsed;
            } else if (recordTag == 5) {
                DumpObjectArray objectArray = readObjectArray(header, context);
                virtualIndexSystem.save(objectArray.getAddress(), objectArray);
                ++totalObjectArraysParsed;
            } else if (recordTag == 6) {
                DumpClass aClass = readClass(header, context);
                virtualIndexSystem.save(aClass);
                ++totalClassesParsed;
            } else if (recordTag == 7) {
                DumpPrimitiveArray primitiveArray = readLongPrimitiveArray(header, context);
                virtualIndexSystem.save(primitiveArray.getAddress(), primitiveArray);
                ++totalPrimitiveArraysParsed;
            } else if (recordTag == 8) {
                DumpObjectArray objectArray = readObjectArrayV5(header, context);
                virtualIndexSystem.save(objectArray.getAddress(), objectArray);
                ++totalObjectArraysParsed;
            } else if (recordTag == 3) {
                LOGGER.info("Exited the body. {} bytes read", bytesReadTotal);
                break;
            } else {
                throw new IllegalStateException(String.format("Body has an invalid format. %d bytes read", bytesReadTotal));
            }
        }

        return new DumpParsingStatistics(header, totalClassesParsed, totalObjectsParsed, totalObjectArraysParsed, totalPrimitiveArraysParsed);
    }

    @Override
    public void close() throws IOException {
        if (stream != null) {
            stream.close();
        }
    }

    int readUnsignedByte() throws IOException {
        readBytes(1);
        return 0xff & bytesCache[0];
    }

    int readSignedByte() throws IOException {
        return (byte) readUnsignedByte();
    }

    int readUnsignedShort() throws IOException {
        readBytes(2);
        return (0xff & bytesCache[0]) << 8 |
                (0xff & bytesCache[1]);
    }

    int readSignedShort() throws IOException {
        return (short) readUnsignedShort();
    }

    long readUnsignedInt() throws IOException {
        readBytes(4);
        return (0xff & bytesCache[0]) << 24 |
                (0xff & bytesCache[1]) << 16 |
                (0xff & bytesCache[2]) << 8 |
                (0xff & bytesCache[3]);
    }

    int readSignedInt() throws IOException {
        return (int) readUnsignedInt();
    }

    long readSignedLong() throws IOException {
        readBytes(8);
        return (0xffL & bytesCache[0]) << 56 |
                (0xffL & bytesCache[1]) << 48 |
                (0xffL & bytesCache[2]) << 40 |
                (0xffL & bytesCache[3]) << 32 |
                (0xffL & bytesCache[4]) << 24 |
                (0xffL & bytesCache[5]) << 16 |
                (0xffL & bytesCache[6]) << 8 |
                (0xffL & bytesCache[7]);
    }

    long readUnsignedWord(DumpPlatform platform) throws IOException {
        if (platform == null) {
            throw new NullPointerException("Platform is null");
        }

        switch (platform) {
            case _32_BIT:
                return readUnsignedInt();
            case _64_BIT:
                return readSignedLong();
            default:
                throw new IllegalArgumentException();
        }
    }

    long readSignedWord(DumpPlatform platform) throws IOException {
        if (platform == null) {
            throw new NullPointerException("Platform is null");
        }

        switch (platform) {
            case _32_BIT:
                return readSignedInt();
            case _64_BIT:
                return readSignedLong();
            default:
                throw new IllegalArgumentException();
        }
    }

    String readString() throws IOException {
        int length = readUnsignedShort();
        if (length == 0) {
            return "";
        }

        ensureBytesCacheSize(length * 4);
        readBytes(length);

        int needToReadBytesAdditionally = 0;
        int symbolIndex = 0;
        int bytesReadAlready = length;
        int octetPartLeft = 0;
        int byteIndex = 0;
        while (symbolIndex < length) {
            int currentByte = bytesCache[byteIndex] & 0xff;
            if (octetPartLeft == 0) {
                int octetsCount;
                if (currentByte <= 0x7f) {
                    octetsCount = 1;
                } else if (currentByte >= 0xc0 && currentByte <= 0xdf) {
                    octetsCount = 2;
                } else if (currentByte >= 0xe0 && currentByte <= 0xef) {
                    octetsCount = 3;
                } else if (currentByte >= 0xf0 && currentByte <= 0xf7) {
                    octetsCount = 4;
                } else {
                    throw new IOException(String.format("Current byte %02x. %d bytes read", currentByte, bytesReadTotal));
                }
                octetPartLeft = octetsCount;
                needToReadBytesAdditionally += (octetsCount - 1);
            } else {
                if (currentByte < 0x80 || currentByte > 0xbf) {
                    throw new IOException(String.format("Current byte %02x. %d bytes read", currentByte, bytesReadTotal));
                }
            }
            --octetPartLeft;
            if (octetPartLeft == 0) {
                ++symbolIndex;
            }

            ++byteIndex;
            if (byteIndex == bytesReadAlready) {
                if (needToReadBytesAdditionally > 0) {
                    readBytes(bytesReadAlready, needToReadBytesAdditionally);
                    bytesReadAlready += needToReadBytesAdditionally;
                    needToReadBytesAdditionally = 0;
                } else {
                    break;
                }
            }
        }
        return new String(bytesCache, 0, bytesReadAlready, StandardCharsets.UTF_8);
    }



    private void readBytes(int bytesCount) throws IOException {
        readBytes(0, bytesCount);
    }

    private void readBytes(int cacheOffset, int bytesCount) throws IOException {
        ensureBytesCacheSize(bytesCount);
        int read = stream.read(bytesCache, cacheOffset, bytesCount);
        if (read < bytesCount) {
            throw new IOException(String.format("Cannot read %d bytes. Read only %d", bytesCount, read));
        }

        updateReadBytesCount(read);
    }

    private void updateReadBytesCount(int read) {
        bytesReadTotal += read;

        if (bytesReadTotal >= logReadTotalAfter) {
            LOGGER.info("Bytes read {}", bytesReadTotal);
            logReadTotalAfter += READ_TOTAL_LOG_INTERVAL;
        }
    }

    private void ensureBytesCacheSize(int length) {
        if (bytesCache.length < length) {
            bytesCache = new byte[length];
        }
    }

    private DumpParserStream(InputStream stream) {
        this.stream = stream;
    }

    DumpPrimitiveArray readPrimitiveArray(DumpHeader header, int firstByte, ParsingContext context) throws IOException {
        int flag = firstByte & 0x1f;
        DumpPrimitiveType arrayType = detectType((flag >> 2) & 0x7);
        Measurement measurement = measurement(flag & 0x3);

        long addressGap = 0;
        long length = 0;
        switch (Objects.requireNonNull(measurement)) {
            case BYTE:
                addressGap = readSignedByte();
                length = readUnsignedByte();
                break;
            case SHORT:
                addressGap = readSignedShort();
                length = readUnsignedShort();
                break;
            case INT:
                addressGap = readSignedInt();
                length = readUnsignedInt();
                break;
            case LONG:
                addressGap = readSignedLong();
                length = readSignedLong();
                break;
        }

        int hash = (header.isHashed() ? readSignedShort() : 0);

        final int wordSize = 4;
        long sizeInWords = (readUnsignedInt() & 0xffffffffL);
        long sizeInBytes = sizeInWords * wordSize;

        long address = context.lastAddress + addressGap * wordSize;
        if (address < 0) {
            throw new IllegalStateException(String.format("Got address %d. Base address %d, address gap %d. Bytes read %d", address, context.lastAddress, addressGap, bytesReadTotal));
        }
        context.lastAddress = address;

        return new DumpPrimitiveArray(address, arrayType, length, hash, sizeInBytes);
    }

    DumpPrimitiveArray readLongPrimitiveArray(DumpHeader header, ParsingContext context) throws IOException {
        int flag = readUnsignedByte();
        DumpPrimitiveType arrayType = detectType((flag >> 5) & 0x7);
        int measurement = (flag >> 4) & 0x1;
        boolean hashedAndMoved = ((flag >> 1) & 0x1) == 1;

        long addressGap = 0;
        long length = 0;
        switch (measurement) {
            case 0:
                addressGap = readSignedByte();
                length = readUnsignedByte();
                break;
            case 1:
                addressGap = readSignedWord(header.getPlatform());
                length = readUnsignedWord(header.getPlatform());
                break;
        }

        int hash = 0;
        if (header.isHashed()) {
            hash = readSignedShort();
        } else if (hashedAndMoved) {
            hash = readSignedInt();
        }

        final int wordSize = 4;
        long sizeInWords = (readUnsignedInt() & 0xffffffffL);
        long sizeInBytes = sizeInWords * wordSize;

        long address = context.lastAddress + addressGap * wordSize;
        if (address < 0) {
            throw new IllegalStateException(String.format("Got address %d. Base address %d, address gap %d. Bytes read %d", address, context.lastAddress, addressGap, bytesReadTotal));
        }
        context.lastAddress = address;

        return new DumpPrimitiveArray(address, arrayType, length, hash, sizeInBytes);
    }

    DumpObjectArray readObjectArray(DumpHeader header, ParsingContext context) throws IOException {
        int flag = readUnsignedByte();
        Measurement gapMeasurement = measurement((flag >> 6) & 0x3);
        Measurement refSizeMeasurement = measurement((flag >> 4) & 0x3);
        boolean hashedAndMoved = ((flag >> 1) & 0x1) == 1;

        long addressGap = 0;
        switch (Objects.requireNonNull(gapMeasurement)) {
            case BYTE:
                addressGap = readSignedByte();
                break;
            case SHORT:
                addressGap = readSignedShort();
                break;
            case INT:
                addressGap = readSignedInt();
                break;
            case LONG:
                addressGap = readSignedLong();
                break;
        }

        long classAddress = readUnsignedWord(header.getPlatform());

        int hash = 0;
        if (header.isHashed()) {
            hash = readSignedShort();
        } else if (hashedAndMoved) {
            hash = readSignedInt();
        }

        final int wordSize = 4;

        long address = context.lastAddress + addressGap * wordSize;
        if (address < 0) {
            throw new IllegalStateException(String.format("Got address %d. Base address %d, address gap %d. Bytes read %d", address, context.lastAddress, addressGap, bytesReadTotal));
        }
        context.lastAddress = address;

        long numberOfReferences = readUnsignedInt();
        long[] references = new long[(int) numberOfReferences];
        for (int i = 0; i < numberOfReferences; ++i) {
            long refAddressGap = 0;
            switch (Objects.requireNonNull(refSizeMeasurement)) {
                case BYTE:
                    refAddressGap = readSignedByte();
                    break;
                case SHORT:
                    refAddressGap = readSignedShort();
                    break;
                case INT:
                    refAddressGap = readSignedInt();
                    break;
                case LONG:
                    refAddressGap = readSignedLong();
                    break;
            }
            references[i] = address + refAddressGap * wordSize;
        }

        long sizeInWords = (readUnsignedInt() & 0xffffffffL);
        long sizeInBytes = sizeInWords * wordSize;

        return new DumpObjectArray(address, classAddress, hash, sizeInBytes, references);
    }

    DumpObjectArray readObjectArrayV5(DumpHeader header, ParsingContext context) throws IOException {
        int flag = readUnsignedByte();
        Measurement gapMeasurement = measurement((flag >> 6) & 0x3);
        Measurement refSizeMeasurement = measurement((flag >> 4) & 0x3);
        boolean hashedAndMoved = ((flag >> 1) & 0x1) == 1;

        long addressGap = 0;
        switch (Objects.requireNonNull(gapMeasurement)) {
            case BYTE:
                addressGap = readSignedByte();
                break;
            case SHORT:
                addressGap = readSignedShort();
                break;
            case INT:
                addressGap = readSignedInt();
                break;
            case LONG:
                addressGap = readSignedLong();
                break;
        }

        long classAddress = readUnsignedWord(header.getPlatform());

        int hash = 0;
        if (header.isHashed()) {
            hash = readSignedShort();
        } else if (hashedAndMoved) {
            hash = readSignedInt();
        }

        final int wordSize = 4;

        long address = context.lastAddress + addressGap * wordSize;
        if (address < 0) {
            throw new IllegalStateException(String.format("Got address %d. Base address %d, address gap %d. Bytes read %d", address, context.lastAddress, addressGap, bytesReadTotal));
        }
        context.lastAddress = address;

        long numberOfReferences = readUnsignedInt();
        long[] references = new long[(int) numberOfReferences];
        for (int i = 0; i < numberOfReferences; ++i) {
            long refAddressGap = 0;
            switch (Objects.requireNonNull(refSizeMeasurement)) {
                case BYTE:
                    refAddressGap = readSignedByte();
                    break;
                case SHORT:
                    refAddressGap = readSignedShort();
                    break;
                case INT:
                    refAddressGap = readSignedInt();
                    break;
                case LONG:
                    refAddressGap = readSignedLong();
                    break;
            }
            references[i] = address + refAddressGap * wordSize;
        }

        long sizeInWords = (readUnsignedInt() & 0xffffffffL);
        long sizeInBytes = sizeInWords * wordSize;

        long readSize = readUnsignedInt();

        return new DumpObjectArray(address, classAddress, hash, sizeInBytes, readSize, references);
    }

    DumpObject readShortObject(DumpHeader header, int firstByte, ParsingContext context) throws IOException {
        int flag = firstByte & 0x7f;
        int classCacheIndex = (flag >> 5) & 0x3;
        int numberOfReferences = (flag >> 3) & 0x3;
        ObjectGapSize gapSize = gapSize((flag >> 2) & 0x1);
        Measurement referenceSize = measurement(flag & 0x3);

        int addressGap = 0;
        switch (gapSize) {
            case BYTE:
                addressGap = readSignedByte();
                break;
            case SHORT:
                addressGap = readSignedShort();
                break;
        }

        long classAddress = context.getFromCache(classCacheIndex);
        int hash = (header.isHashed() ? readSignedShort() : 0);

        final int wordSize = 4;
        long address = context.lastAddress + addressGap * wordSize;
        if (address < 0) {
            throw new IllegalStateException(String.format("Got address %d. Base address %d, address gap %d. Bytes read %d", address, context.lastAddress, addressGap, bytesReadTotal));
        }
        context.lastAddress = address;

        long[] references = new long[numberOfReferences];
        for (int i = 0; i < numberOfReferences; ++i) {
            long refAddressGap = 0;
            switch (Objects.requireNonNull(referenceSize)) {
                case BYTE:
                    refAddressGap = readSignedByte();
                    break;
                case SHORT:
                    refAddressGap = readSignedShort();
                    break;
                case INT:
                    refAddressGap = readSignedInt();
                    break;
                case LONG:
                    refAddressGap = readSignedLong();
                    break;
            }
            references[i] = address + refAddressGap * wordSize;
        }

        return new DumpObject(address, classAddress, hash, references);
    }

    DumpObject readMediumObject(DumpHeader header, int firstByte, ParsingContext context) throws IOException {
        int flag = firstByte & 0x3f;
        int numberOfReferences = (flag >> 3) & 0x7;
        ObjectGapSize gapSize = gapSize((flag >> 2) & 0x1);
        Measurement referenceSize = measurement(flag & 0x3);

        int addressGap = 0;
        switch (gapSize) {
            case BYTE:
                addressGap = readSignedByte();
                break;
            case SHORT:
                addressGap = readSignedShort();
                break;
        }

        long classAddress = readUnsignedWord(header.getPlatform());
        context.putToCache(classAddress);

        int hash = (header.isHashed() ? readSignedShort() : 0);

        final int wordSize = 4;
        long address = context.lastAddress + addressGap * wordSize;
        if (address < 0) {
            throw new IllegalStateException(String.format("Got address %d. Base address %d, address gap %d. Bytes read %d", address, context.lastAddress, addressGap, bytesReadTotal));
        }
        context.lastAddress = address;

        long[] references = new long[numberOfReferences];
        for (int i = 0; i < numberOfReferences; ++i) {
            long refAddressGap = 0;
            switch (Objects.requireNonNull(referenceSize)) {
                case BYTE:
                    refAddressGap = readSignedByte();
                    break;
                case SHORT:
                    refAddressGap = readSignedShort();
                    break;
                case INT:
                    refAddressGap = readSignedInt();
                    break;
                case LONG:
                    refAddressGap = readSignedLong();
                    break;
            }
            references[i] = address + refAddressGap * wordSize;
        }

        return new DumpObject(address, classAddress, hash, references);
    }

    DumpObject readLongObject(DumpHeader header, ParsingContext context) throws IOException {
        int flag = readUnsignedByte();
        Measurement gapMeasurement = measurement((flag >> 6) & 0x3);
        Measurement refSizeMeasurement = measurement((flag >> 4) & 0x3);
        boolean hashedAndMoved = ((flag >> 1) & 0x1) == 1;

        long addressGap = 0;
        switch (Objects.requireNonNull(gapMeasurement)) {
            case BYTE:
                addressGap = readSignedByte();
                break;
            case SHORT:
                addressGap = readSignedShort();
                break;
            case INT:
                addressGap = readSignedInt();
                break;
            case LONG:
                addressGap = readSignedLong();
                break;
        }

        long classAddress = readUnsignedWord(header.getPlatform());
        context.putToCache(classAddress);

        int hash = 0;
        if (header.isHashed()) {
            hash = readSignedShort();
        } else if (hashedAndMoved) {
            hash = readSignedInt();
        }

        final int wordSize = 4;

        long address = context.lastAddress + addressGap * wordSize;
        if (address < 0) {
            throw new IllegalStateException(String.format("Got address %d. Base address %d, address gap %d. Bytes read %d", address, context.lastAddress, addressGap, bytesReadTotal));
        }
        context.lastAddress = address;

        long numberOfReferences = readUnsignedInt();
        long[] references = new long[(int) numberOfReferences];
        for (int i = 0; i < numberOfReferences; ++i) {
            long refAddressGap = 0;
            switch (Objects.requireNonNull(refSizeMeasurement)) {
                case BYTE:
                    refAddressGap = readSignedByte();
                    break;
                case SHORT:
                    refAddressGap = readSignedShort();
                    break;
                case INT:
                    refAddressGap = readSignedInt();
                    break;
                case LONG:
                    refAddressGap = readSignedLong();
                    break;
            }
            references[i] = address + refAddressGap * wordSize;
        }

        return new DumpObject(address, classAddress, hash, references);
    }

    DumpClass readClass(DumpHeader header, ParsingContext context) throws IOException {
        int flag = readUnsignedByte();
        Measurement gapMeasurement = measurement((flag >> 6) & 0x3);
        Measurement refSizeMeasurement = measurement((flag >> 4) & 0x3);
        boolean hashedAndMoved = ((flag >> 3) & 0x1) == 1;

        long addressGap = 0;
        switch (Objects.requireNonNull(gapMeasurement)) {
            case BYTE:
                addressGap = readSignedByte();
                break;
            case SHORT:
                addressGap = readSignedShort();
                break;
            case INT:
                addressGap = readSignedInt();
                break;
            case LONG:
                addressGap = readSignedLong();
                break;
        }

        long instanceSize = readUnsignedInt();

        int hash = 0;
        if (header.isHashed()) {
            hash = readSignedShort();
        } else if (hashedAndMoved) {
            hash = readSignedInt();
        }

        long superClassAddress = readUnsignedWord(header.getPlatform());
        String className = readString();

        final int wordSize = 4;

        long address = context.lastAddress + addressGap * wordSize;
        if (address < 0) {
            throw new IllegalStateException(String.format("Got address %d. Base address %d, address gap %d. Bytes read %d", address, context.lastAddress, addressGap, bytesReadTotal));
        }
        context.lastAddress = address;

        long numberOfReferences = readUnsignedInt();
        long[] references = new long[(int) numberOfReferences];
        for (int i = 0; i < numberOfReferences; ++i) {
            long refAddressGap = 0;
            switch (Objects.requireNonNull(refSizeMeasurement)) {
                case BYTE:
                    refAddressGap = readSignedByte();
                    break;
                case SHORT:
                    refAddressGap = readSignedShort();
                    break;
                case INT:
                    refAddressGap = readSignedInt();
                    break;
                case LONG:
                    refAddressGap = readSignedLong();
                    break;
            }
            references[i] = address + refAddressGap * wordSize;
        }

        return new DumpClass(address, superClassAddress, instanceSize, className, hash, references);
    }

    private ObjectGapSize gapSize(int value) {
        return (value == 0 ? ObjectGapSize.BYTE : ObjectGapSize.SHORT);
    }

    private Measurement measurement(int value) {
        switch (value) {
            case 0:
                return Measurement.BYTE;
            case 1:
                return Measurement.SHORT;
            case 2:
                return Measurement.INT;
            case 3:
                return Measurement.LONG;
            default:
                return null;
        }
    }

    private DumpPrimitiveType detectType(int type) {
        switch (type) {
            case 0:
                return DumpPrimitiveType.BOOLEAN;
            case 1:
                return DumpPrimitiveType.CHAR;
            case 2:
                return DumpPrimitiveType.FLOAT;
            case 3:
                return DumpPrimitiveType.DOUBLE;
            case 4:
                return DumpPrimitiveType.BYTE;
            case 5:
                return DumpPrimitiveType.SHORT;
            case 6:
                return DumpPrimitiveType.INT;
            case 7:
                return DumpPrimitiveType.LONG;
            default:
                return null; // Should never happen, because should be calculated using X & 7
        }
    }

    static class ParsingContext {
        static final int MAX_CLASSES_CACHE_SIZE = 4;
        long lastAddress = 0;
        long[] cachedClassAddresses = new long[MAX_CLASSES_CACHE_SIZE];
        int cacheFilled = 0;

        void putToCache(long address) {
            long[] temp = new long[MAX_CLASSES_CACHE_SIZE];
            System.arraycopy(cachedClassAddresses, 0, temp, 0, cacheFilled);

            int copiedAddresses = 0;

            cachedClassAddresses[copiedAddresses++] = address;
            for (int i = 0; i < cacheFilled && copiedAddresses < MAX_CLASSES_CACHE_SIZE; ++i) {
                if (temp[i] != address) {
                    cachedClassAddresses[copiedAddresses++] = temp[i];
                }
            }

            cacheFilled = copiedAddresses;
        }

        long getFromCache(int position) {
            if (position >= cacheFilled) {
                throw new IndexOutOfBoundsException(String.format("Cache size is %d, but requested %d", cacheFilled, position));
            }
            return cachedClassAddresses[position];
        }
    }

    enum ObjectGapSize {
        BYTE, SHORT
    }

    enum Measurement {
        BYTE, SHORT, INT, LONG
    }
}
