package ru.parse.dump;

import org.junit.Test;
import ru.parse.dump.objects.DumpClass;
import ru.parse.dump.objects.DumpHeader;
import ru.parse.dump.objects.DumpPlatform;
import ru.parse.dump.vind.StandardVirtualIndexSystem;
import ru.parse.dump.vind.VirtualIndexSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static org.junit.Assert.*;

public class DumpParserStreamTest {
    @Test
    public void testReadByte() throws IOException {
        DumpParserStream stream = DumpParserStream.create(
                new byte[]{0x56});
        assertEquals(0x56, stream.readUnsignedByte());
    }

    @Test
    public void testReadString() throws IOException {
        DumpParserStream stream = DumpParserStream.create(
                new byte[]{0x00, 0x12, 0x70, 0x6F, 0x72, 0x74, 0x61, 0x62, 0x6C, 0x65, 0x20, 0x68, 0x65, 0x61, 0x70, 0x20, 0x64, 0x75, 0x6D, 0x70});
        assertEquals("portable heap dump", stream.readString());
    }

    @Test
    public void testReadString1() throws IOException {
        DumpParserStream stream = DumpParserStream.create(new byte[]{0x00, 0x3A, 0x73, 0x75, 0x6E, 0x2F, 0x72, 0x65, 0x66, 0x6C, 0x65,
                0x63, 0x74, 0x2F, 0x47, 0x65, 0x6E, 0x65, 0x72, 0x61, 0x74, 0x65, 0x64, 0x53, 0x65, 0x72, 0x69, 0x61, 0x6C, 0x69, 0x7A,
                0x61, 0x74, 0x69, 0x6F, 0x6E, 0x43, 0x6F, 0x6E, 0x73, 0x74, 0x72, 0x75, 0x63, 0x74, 0x6F, 0x72, 0x41, 0x63, 0x63, 0x65,
                0x73, 0x73, 0x6F, 0x72, 0x39, 0x39, 0x30, 0x36, 0x36, 0x00, 0x00, 0x00, 0x07, (byte) 0xFF});
        assertEquals("sun/reflect/GeneratedSerializationConstructorAccessor99066", stream.readString());
    }

    @Test
    public void testReadHeader() throws IOException {
        DumpParserStream stream = DumpParserStream.create(new byte[]{0x00, 0x12, 0x70, 0x6F, 0x72, 0x74, 0x61, 0x62, 0x6C,
                0x65, 0x20, 0x68, 0x65, 0x61, 0x70, 0x20, 0x64, 0x75, 0x6D, 0x70, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00,
                0x05, 0x01, 0x04, 0x00, 0x45, 0x4A, 0x52, 0x45, 0x20, 0x31, 0x2E, 0x38, 0x2E, 0x30, 0x20, 0x4C, 0x69, 0x6E,
                0x75, 0x78, 0x20, 0x61, 0x6D, 0x64, 0x36, 0x34, 0x2D, 0x36, 0x34, 0x20, 0x62, 0x75, 0x69, 0x6C, 0x64, 0x20,
                0x20, 0x28, 0x70, 0x78, 0x61, 0x36, 0x34, 0x38, 0x30, 0x73, 0x72, 0x34, 0x66, 0x70, 0x35, 0x2D, 0x32, 0x30,
                0x31, 0x37, 0x30, 0x34, 0x32, 0x31, 0x5F, 0x30, 0x31, 0x28, 0x53, 0x52, 0x34, 0x20, 0x46, 0x50, 0x35, 0x29,
                0x20, 0x29, 0x02});
        DumpHeader header = stream.readHeader();
        assertEquals(6, header.getVersion());
        assertEquals(DumpPlatform._64_BIT, header.getPlatform());
        assertFalse(header.isHashed());
        assertTrue(header.isJ9VM());
        assertEquals("JRE 1.8.0 Linux amd64-64 build  (pxa6480sr4fp5-20170421_01(SR4 FP5) )", header.getJvmVersion());
    }

    @Test
    public void testReadBody() throws IOException {
        VirtualIndexSystem indexSystem = new VirtualIndexSystem() {
            @Override
            public <T> T find(long address, Class<T> objectType) {
                return null;
            }

            @Override
            public void save(long address, Object object) {

            }

            @Override
            public void save(DumpClass aClass) {

            }
        };

        DumpParserStream stream = DumpParserStream.create(new byte[]{0x02, 0x27, 0x00, 0x00, 0x00, 0x00, (byte) 0x88, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0F, 0x00, 0x00, 0x00, 0x0A, /**/ 0x24, 0x0A, 0x12, 0x00, 0x00,
                0x00, 0x0C, /**/ 0x24, 0x0C, 0x20, 0x00, 0x00, 0x00, 0x12, /**/ 0x4A, 0x12, 0x00, 0x00, 0x00, 0x06, 0x5B, 0x39, (byte) 0xCD,
                (byte) 0xE0, 0x00, 0x03, 0x2C, 0x3C, /**/ 0x24, 0x06, 0x15, 0x00, 0x00, 0x00, 0x0E, /**/ (byte) 0x8A, 0x0E, 0x00, 0x03, 0x2C, 0x3A, /**/
                0x24, 0x06, 0x07, 0x00, 0x00, 0x00, 0x06, /**/ 0x24, 0x06, 0x20, 0x00, 0x00, 0x00, 0x12, /**/ 0x24, 0x12, 0x09, 0x00,
                0x00, 0x00, 0x08, /**/ 0x24, 0x08, 0x20, 0x00, 0x00, 0x00, 0x12, /**/ 0x24, 0x12, 0x20, 0x00, 0x00, 0x00, 0x12, /**/ 0x40,
                0x12, 0x00, 0x00, 0x00, 0x06, 0x5B, 0x3D, 0x02, 0x30, /**/ (byte) 0xA0, 0x06, /**/ (byte) 0x8A, 0x06, 0x00, 0x03, 0x2B, (byte) 0xF6, /**/ (byte) 0x8A,
                0x06, 0x00, 0x03, 0x2B, (byte) 0xFA, /**/ (byte) 0xA0, 0x06, /**/ (byte) 0x8A, 0x06, 0x00, 0x03, 0x2B, (byte) 0xFA, /**/ 0x4A, 0x06, 0x00, 0x00, 0x00,
                0x06, 0x5F, 0x62, 0x73, (byte) 0xE0, 0x00, 0x03, 0x2C, 0x06, /**/ (byte) 0x8A, 0x04, 0x00, 0x03, 0x2C, 0x08, /**/ (byte) 0xCA, 0x06, 0x00,
                0x03, 0x2C, 0x10, /**/ (byte) 0x8A, 0x04, 0x00, 0x03, 0x2C, 0x12, /**/ (byte) 0x8A, 0x06, 0x00, 0x03, 0x2C, 0x12, /**/ (byte) 0xA0, 0x06, /**/ 0x40,
                0x06, 0x00, 0x00, 0x00, 0x06, 0x49, (byte) 0xCF, 0x30, (byte) 0xF8, /**/ (byte) 0x8A, 0x04, 0x00, 0x03, 0x2C, 0x14, /**/ 0x03});
        DumpHeader header = new DumpHeader(6, DumpPlatform._64_BIT, false, true, "JRE 1.8.0 Linux amd64-64 build  (pxa6480sr4fp5-20170421_01(SR4 FP5) )");
        stream.readBody(header, indexSystem);
    }

    @Test
    public void testReadClass() throws IOException {
        DumpParserStream stream = DumpParserStream.create(new byte[]{(byte) 0xA8, (byte) 0xFD, (byte) 0xC0, (byte) 0xF7, 0x46, 0x00, 0x00, 0x00, 0x08,
                (byte) 0xEE, 0x22, 0x5E, 0x62, 0x00, 0x00, 0x00, 0x06, 0x53, (byte) 0xBB, (byte) 0xC2, 0x48, 0x00, 0x3B, 0x73, 0x75, 0x6E, 0x2F, 0x72,
                0x65, 0x66, 0x6C, 0x65, 0x63, 0x74, 0x2F, 0x47, 0x65, 0x6E, 0x65, 0x72, 0x61, 0x74, 0x65, 0x64, 0x53, 0x65, 0x72,
                0x69, 0x61, 0x6C, 0x69, 0x7A, 0x61, 0x74, 0x69, 0x6F, 0x6E, 0x43, 0x6F, 0x6E, 0x73, 0x74, 0x72, 0x75, 0x63, 0x74,
                0x6F, 0x72, 0x41, 0x63, 0x63, 0x65, 0x73, 0x73, 0x6F, 0x72, 0x31, 0x30, 0x30, 0x31, 0x38, 0x32, 0x00, 0x00, 0x00,
                0x07, (byte) 0xFF, (byte) 0xFF, (byte) 0xFB, (byte) 0xC4, (byte) 0xFF, (byte) 0xFF, (byte) 0xFB, (byte) 0xC4, (byte) 0x9A, (byte) 0xED, (byte) 0xF9, (byte) 0x88, (byte) 0x9D, (byte) 0xB9, 0x1A, 0x6A, (byte) 0x9A, (byte) 0xEE,
                0x18, 0x48, (byte) 0x9C, (byte) 0xCD, 0x7C, (byte) 0x9E, (byte) 0x9A, (byte) 0xED, (byte) 0xF9, (byte) 0x88, 0x06, 0x68, (byte) 0x9E, (byte) 0xEA, 0x00, 0x00, 0x00, 0x08, (byte) 0xB4,
                0x10, (byte) 0xC8, 0x72, 0x00, 0x00, 0x00, 0x06, 0x53, (byte) 0xBB, (byte) 0xC2, 0x48});
        DumpHeader header = new DumpHeader(6, DumpPlatform._64_BIT, false, true, "JRE 1.8.0 Linux amd64-64 build  (pxa6480sr4fp5-20170421_01(SR4 FP5) )");
        DumpParserStream.ParsingContext context = new DumpParserStream.ParsingContext();
        DumpClass aClass = stream.readClass(header, context);
    }

    @Test
    public void parseFile() throws IOException {
        StandardVirtualIndexSystem indexSystem = new StandardVirtualIndexSystem();

        try (DumpParserStream stream = DumpParserStream.create(
                Files.newInputStream(Paths.get("D:", "Downloads", "heapdump20190827", "heapdump20190827.phd"), StandardOpenOption.READ))) {
            DumpParsingStatistics stat = stream.readObjects(indexSystem);
            System.out.println(stat);
        }
    }
}