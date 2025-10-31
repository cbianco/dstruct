package dev.dstruct.wal;

import dev.dstruct.util.Binaries;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * A buffered wrapper around {@link ReadableByteChannel} that provides
 * {@link ByteBuffer}-like interface with automatic refilling from the underlying channel.
 * 
 * <p>This class transparently manages the internal buffer, reading from the channel
 * only when necessary. It provides methods similar to ByteBuffer for reading data
 * without exposing the complexity of buffer management.
 * 
 * <p>All I/O exceptions are wrapped in {@link UncheckedIOException}.
 * 
 * <p><b>Example usage:</b>
 * <pre>
 * try (ReadableByteChannel channel = ...) {
 *     BufferedChannel buffered = new BufferedChannel(channel, 8192);
 *     
 *     while (buffered.hasRemaining()) {
 *         byte b = buffered.get();
 *         // process byte
 *     }
 * }
 * </pre>
 */
public class BufferedChannel {

    private final ReadableByteChannel channel;
    private final ByteBuffer buffer;
    private boolean endOfStream = false;

    /**
     * Creates a buffered channel with default buffer size (8KB).
     * 
     * @param channel the underlying channel to read from
     */
    public BufferedChannel(ReadableByteChannel channel) {
        this(channel, 8192);
    }

    /**
     * Creates a buffered channel with specified buffer size.
     * 
     * @param channel the underlying channel to read from
     * @param bufferSize the internal buffer size in bytes
     */
    public BufferedChannel(ReadableByteChannel channel, int bufferSize) {
        this.channel = channel;
        this.buffer = ByteBuffer.allocate(bufferSize);
        this.buffer.flip(); // Start in "empty" state
    }

    /**
     * Checks if there is data available to read.
     * Automatically refills the buffer from the channel if needed.
     * 
     * @return true if data is available, false if end of stream reached
     * @throws UncheckedIOException if an I/O error occurs
     */
    public boolean hasRemaining() {
        if (buffer.hasRemaining()) {
            return true;
        }
        
        if (endOfStream) {
            return false;
        }
        
        return fillBuffer();
    }

    /**
     * Reads a single byte from the buffer.
     * Automatically refills if buffer is empty.
     * 
     * @return the next byte
     * @throws UncheckedIOException if an I/O error occurs
     * @throws java.nio.BufferUnderflowException if no data available
     */
    public byte get() {
        ensureRemaining();
        return buffer.get();
    }

    /**
     * Reads bytes into the destination array.
     * 
     * @param dst the destination array
     * @return the number of bytes read
     * @throws UncheckedIOException if an I/O error occurs
     */
    public int get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    /**
     * Reads bytes into the destination array.
     * 
     * @param dst the destination array
     * @param offset the offset in the array
     * @param length the maximum number of bytes to read
     * @return the number of bytes actually read
     * @throws UncheckedIOException if an I/O error occurs
     */
    public int get(byte[] dst, int offset, int length) {
        int totalRead = 0;
        
        while (length > 0 && hasRemaining()) {
            int toRead = Math.min(length, buffer.remaining());
            buffer.get(dst, offset, toRead);
            
            offset += toRead;
            length -= toRead;
            totalRead += toRead;
        }
        
        return totalRead;
    }

    /**
     * Reads a short value (2 bytes) in big-endian order.
     * Works even if buffer capacity is less than 2 bytes.
     * 
     * @return the short value
     * @throws UncheckedIOException if an I/O error occurs
     */
    public short getShort() {
        if (Short.BYTES <= buffer.capacity()) {
            ensureBytes(Short.BYTES);
            return buffer.getShort();
        } else {
            byte[] bytes = new byte[Short.BYTES];
            readExact(bytes);
            return Binaries.fromBytesToShort(bytes);
        }
    }

    /**
     * Reads an int value (4 bytes) in big-endian order.
     * Works even if buffer capacity is less than 4 bytes.
     * 
     * @return the int value
     * @throws UncheckedIOException if an I/O error occurs
     */
    public int getInt() {
        if (Integer.BYTES <= buffer.capacity()) {
            ensureBytes(Integer.BYTES);
            return buffer.getInt();
        } else {
            byte[] bytes = new byte[Integer.BYTES];
            readExact(bytes);
            return Binaries.fromBytesToInt(bytes);
        }
    }

    /**
     * Reads a long value (8 bytes) in big-endian order.
     * Works even if buffer capacity is less than 8 bytes.
     * 
     * @return the long value
     * @throws UncheckedIOException if an I/O error occurs
     */
    public long getLong() {
        if (Long.BYTES <= buffer.capacity()) {
            ensureBytes(Long.BYTES);
            return buffer.getLong();
        } else {
            byte[] bytes = new byte[Long.BYTES];
            readExact(bytes);
            return Binaries.fromBytesToLong(bytes);
        }
    }

    /**
     * Reads a double value (8 bytes) in big-endian order.
     * Works even if buffer capacity is less than 8 bytes.
     * 
     * @return the double value
     * @throws UncheckedIOException if an I/O error occurs
     */
    public double getDouble() {
        if (Double.BYTES <= buffer.capacity()) {
            ensureBytes(Double.BYTES);
            return buffer.getDouble();
        } else {
            byte[] bytes = new byte[Double.BYTES];
            readExact(bytes);
            return Binaries.fromBytesToDouble(bytes);
        }
    }

    /**
     * Reads a float value (4 bytes) in big-endian order.
     * Works even if buffer capacity is less than 4 bytes.
     * 
     * @return the float value
     * @throws UncheckedIOException if an I/O error occurs
     */
    public float getFloat() {
        if (Float.BYTES <= buffer.capacity()) {
            ensureBytes(Float.BYTES);
            return buffer.getFloat();
        } else {
            byte[] bytes = new byte[Float.BYTES];
            readExact(bytes);
            return Binaries.fromBytesToFloat(bytes);
        }
    }

    /**
     * Returns the number of bytes remaining in the internal buffer.
     * Note: This does NOT include bytes still in the channel.
     * 
     * @return the number of bytes in the buffer
     */
    public int remaining() {
        return buffer.remaining();
    }

    /**
     * Returns whether the end of stream has been reached.
     * 
     * @return true if end of stream, false otherwise
     */
    public boolean isEndOfStream() {
        return endOfStream && !buffer.hasRemaining();
    }

    /**
     * Reads exactly n bytes into the destination array.
     * Used when reading primitives larger than buffer capacity.
     * 
     * @param dst the destination array
     * @throws UncheckedIOException if an I/O error occurs or not enough data available
     */
    private void readExact(byte[] dst) {
        int offset = 0;
        int remaining = dst.length;
        
        while (remaining > 0) {
            if (!hasRemaining()) {
                throw new UncheckedIOException(
                    new IOException("End of stream: needed " + dst.length + 
                        " bytes but only " + offset + " available")
                );
            }
            
            int toRead = Math.min(remaining, buffer.remaining());
            buffer.get(dst, offset, toRead);
            
            offset += toRead;
            remaining -= toRead;
        }
    }

    /**
     * Fills the internal buffer from the channel.
     * 
     * @return true if data was read, false if end of stream
     * @throws UncheckedIOException if an I/O error occurs
     */
    private boolean fillBuffer() {
        if (endOfStream) {
            return false;
        }

        try {
            buffer.clear();
            int bytesRead = channel.read(buffer);
            buffer.flip();

            if (bytesRead == -1) {
                endOfStream = true;
                return false;
            }

            return bytesRead > 0;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Ensures at least one byte is available in the buffer.
     * 
     * @throws UncheckedIOException if an I/O error occurs or end of stream reached
     */
    private void ensureRemaining() {
        if (!hasRemaining()) {
            throw new UncheckedIOException(new IOException("End of stream reached"));
        }
    }

    /**
     * Ensures at least n bytes are available in the buffer.
     * Only used when n <= buffer capacity.
     * 
     * @param n the number of bytes required (must be <= buffer capacity)
     * @throws UncheckedIOException if an I/O error occurs or not enough data available
     */
    private void ensureBytes(int n) {
        // If we have enough in buffer, we're done
        if (buffer.remaining() >= n) {
            return;
        }

        try {
            // Compact or clear buffer
            if (buffer.hasRemaining()) {
                buffer.compact();
            } else {
                buffer.clear();
            }

            // Read until we have enough bytes or reach end of stream
            while (buffer.position() < n) {
                int bytesRead = channel.read(buffer);
                
                if (bytesRead == -1) {
                    endOfStream = true;
                    buffer.flip();
                    throw new IOException("End of stream: needed " + n + " bytes but only " + 
                        buffer.remaining() + " available");
                }
                
                if (bytesRead == 0) {
                    buffer.flip();
                    throw new IOException("Channel returned 0 bytes, cannot fulfill request");
                }
            }

            buffer.flip();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the underlying buffer for advanced operations.
     * Use with caution as direct manipulation may break internal state.
     * 
     * @return the internal ByteBuffer
     */
    public ByteBuffer buffer() {
        return buffer;
    }
}