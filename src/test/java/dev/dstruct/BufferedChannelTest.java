package dev.dstruct;

import dev.dstruct.wal.BufferedChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.*;

class BufferedChannelTest {

    @TempDir(cleanup = CleanupMode.ALWAYS)
    Path tempDir;

    @Test
    void testReadSingleBytes() throws IOException {
        // Arrange
        Path file = createTestFile("Hello");
        
        try (ReadableByteChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            BufferedChannel buffered = new BufferedChannel(channel, 2); // Small buffer
            
            // Act & Assert
            assertTrue(buffered.hasRemaining());
            assertEquals('H', buffered.get());
            assertEquals('e', buffered.get());
            assertEquals('l', buffered.get());
            assertEquals('l', buffered.get());
            assertEquals('o', buffered.get());
            assertFalse(buffered.hasRemaining());
        }
    }

    @Test
    void testReadByteArray() throws IOException {
        // Arrange
        Path file = createTestFile("Hello World!");
        
        try (ReadableByteChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            BufferedChannel buffered = new BufferedChannel(channel, 4);
            
            // Act
            byte[] data = new byte[5];
            int read = buffered.get(data);
            
            // Assert
            assertEquals(5, read);
            assertEquals("Hello", new String(data, StandardCharsets.UTF_8));
            
            // Read rest
            byte[] rest = new byte[100];
            int readRest = buffered.get(rest);
            assertEquals(7, readRest);
            assertEquals(" World!", new String(rest, 0, readRest, StandardCharsets.UTF_8));
        }
    }

    @Test
    void testReadPrimitiveTypes() throws IOException {
        // Arrange
        Path file = tempDir.resolve("primitives.bin");
        
        // Write test data
        try (FileChannel channel = FileChannel.open(file, 
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            ByteBuffer writeBuffer = ByteBuffer.allocate(100);
            writeBuffer.put((byte) 42);
            writeBuffer.putShort((short) 1234);
            writeBuffer.putInt(567890);
            writeBuffer.putLong(9876543210L);
            writeBuffer.putFloat(3.14f);
            writeBuffer.putDouble(2.71828);
            writeBuffer.flip();
            channel.write(writeBuffer);
        }
        
        // Read test data
        try (ReadableByteChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            BufferedChannel buffered = new BufferedChannel(channel, 8);
            
            // Assert
            assertEquals(42, buffered.get());
            assertEquals(1234, buffered.getShort());
            assertEquals(567890, buffered.getInt());
            assertEquals(9876543210L, buffered.getLong());
            assertEquals(3.14f, buffered.getFloat(), 0.001);
            assertEquals(2.71828, buffered.getDouble(), 0.00001);
            assertFalse(buffered.hasRemaining());
        }
    }

    @Test
    void testBufferRefillAcrossReads() throws IOException {
        // Arrange: Create file larger than buffer
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("X");
        }
        Path file = createTestFile(sb.toString());
        
        try (ReadableByteChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            BufferedChannel buffered = new BufferedChannel(channel, 10); // Small buffer
            
            // Act & Assert: Read all bytes
            int count = 0;
            while (buffered.hasRemaining()) {
                assertEquals('X', buffered.get());
                count++;
            }
            
            assertEquals(1000, count);
            assertTrue(buffered.isEndOfStream());
        }
    }

    @Test
    void testReadIntAcrossBufferBoundary() throws IOException {
        // Arrange: Create file where int spans buffer boundary
        Path file = tempDir.resolve("boundary.bin");
        
        try (FileChannel channel = FileChannel.open(file, 
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            ByteBuffer writeBuffer = ByteBuffer.allocate(100);
            writeBuffer.put((byte) 1);
            writeBuffer.put((byte) 2);
            writeBuffer.put((byte) 3);
            writeBuffer.putInt(0x12345678); // This will span buffer boundary
            writeBuffer.flip();
            channel.write(writeBuffer);
        }
        
        try (ReadableByteChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            BufferedChannel buffered = new BufferedChannel(channel, 4); // Buffer exactly 4 bytes
            
            // Act & Assert
            assertEquals(1, buffered.get());
            assertEquals(2, buffered.get());
            assertEquals(3, buffered.get());
            assertEquals(0x12345678, buffered.getInt()); // Should handle boundary correctly
        }
    }

    @Test
    void testEndOfStreamException() throws IOException {
        // Arrange
        Path file = createTestFile("Hi");
        
        try (ReadableByteChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            BufferedChannel buffered = new BufferedChannel(channel, 8);
            
            // Act
            buffered.get();
            buffered.get();
            
            // Assert
            assertThrows(UncheckedIOException.class, buffered::get);
        }
    }

    @Test
    void testReadMoreBytesThanAvailable() throws IOException {
        // Arrange
        Path file = createTestFile("ABC");
        
        try (ReadableByteChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            BufferedChannel buffered = new BufferedChannel(channel, 8);
            
            // Act
            byte[] data = new byte[10];
            int read = buffered.get(data);
            
            // Assert
            assertEquals(3, read);
            assertEquals("ABC", new String(data, 0, read, StandardCharsets.UTF_8));
        }
    }

    @Test
    void testEmptyFile() throws IOException {
        // Arrange
        Path file = createTestFile("");
        
        try (ReadableByteChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            BufferedChannel buffered = new BufferedChannel(channel);
            
            // Assert
            assertFalse(buffered.hasRemaining());
            assertTrue(buffered.isEndOfStream());
            assertThrows(UncheckedIOException.class, buffered::get);
        }
    }

    @Test
    void testReadLargerThanBufferCapacity() throws IOException {
        // Arrange
        Path file = tempDir.resolve("large.bin");
        
        try (FileChannel channel = FileChannel.open(file, 
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            ByteBuffer writeBuffer = ByteBuffer.allocate(100);
            writeBuffer.putLong(0x123456789ABCDEF0L);
            writeBuffer.flip();
            channel.write(writeBuffer);
        }
        
        try (ReadableByteChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            BufferedChannel buffered = new BufferedChannel(channel, 4); // Buffer smaller than long
            
            // Act & Assert: Should compact and read multiple times
            assertEquals(0x123456789ABCDEF0L, buffered.getLong());
        }
    }

    @Test
    void testRequestMoreBytesThanBufferCapacity() throws IOException {
        // Arrange
        Path file = createTestFile("test");
        
        try (ReadableByteChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            BufferedChannel buffered = new BufferedChannel(channel, 4);
            
            // Act & Assert: Requesting 8 bytes with 4-byte buffer should throw
            assertThrows(UncheckedIOException.class, buffered::getLong);
        }
    }

    @Test
    void testRemainingOnlyShowsBufferContent() throws IOException {
        // Arrange
        Path file = createTestFile("Hello World");
        
        try (ReadableByteChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            BufferedChannel buffered = new BufferedChannel(channel, 5);
            
            // Act & Assert: remaining() only shows buffer, not total file
            assertTrue(buffered.hasRemaining());
            assertTrue(buffered.remaining() <= 5); // Should be <= buffer size
            
            // After reading some bytes
            buffered.get();
            buffered.get();
            int remaining = buffered.remaining();
            assertTrue(remaining < 5);
        }
    }

    @Test
    void testRealWorldProtocolParsing() throws IOException {
        // Arrange: Simulate a binary protocol
        // Format: [command:byte][length:int][data:bytes]
        Path file = tempDir.resolve("protocol.bin");
        
        String message = "Hello, DStruct!";
        try (FileChannel channel = FileChannel.open(file, 
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            ByteBuffer writeBuffer = ByteBuffer.allocate(100);
            writeBuffer.put((byte) 0x01); // Command
            writeBuffer.putInt(message.length()); // Length
            writeBuffer.put(message.getBytes(StandardCharsets.UTF_8)); // Data
            writeBuffer.flip();
            channel.write(writeBuffer);
        }
        
        // Act: Parse protocol
        try (ReadableByteChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            BufferedChannel buffered = new BufferedChannel(channel, 8);
            
            byte command = buffered.get();
            int length = buffered.getInt();
            byte[] data = new byte[length];
            buffered.get(data);
            
            // Assert
            assertEquals(0x01, command);
            assertEquals(message.length(), length);
            assertEquals(message, new String(data, StandardCharsets.UTF_8));
        }
    }

    // Helper method
    private Path createTestFile(String content) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}