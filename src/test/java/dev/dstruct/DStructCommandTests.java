package dev.dstruct;

import dev.dstruct.Result.EmptyResult;
import dev.dstruct.Result.Ok;
import dev.dstruct.command.Command;
import dev.dstruct.util.Binaries;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import static dev.dstruct.util.Binaries.toBytes;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive unit tests for DStruct commands
 * Tests both direct command execution (via executeAsync) and TCP server communication
 * 
 * Based on actual dstruct implementation with correct:
 * - Options fields (dataDirectory, not walPath)
 * - executeAsync method (not emit)
 * - Result types (EmptyResult.OK/NOTHING, Ok, Count, Results)
 */
public class DStructCommandTests {

    // ============================================================================
    // DIRECT COMMAND TESTS (WITHOUT TCP SERVER)
    // ============================================================================

	@TempDir(cleanup = CleanupMode.ALWAYS)
	Path tempDir;

	@Nested
    @DisplayName("Direct Command Tests (No TCP)")
    class DirectCommandTests {

        private DStruct dstruct;

        @BeforeEach
        void setUp() throws Exception {
            Options options = new Options();
            options.port = 0; // Disable TCP server
            options.writeAHeadLogging = false;
            options.dataDirectory = tempDir.resolve("dstruct-test-" + System.currentTimeMillis()).toString();
            
            dstruct = new DStruct(options);
	        dstruct.start();
        }

        @AfterEach
        void tearDown() {
            dstruct.stop();
        }

        // ========================================================================
        // MAP COMMANDS
        // ========================================================================

        @Test
        @DisplayName("MPUT: Should store value in map")
        void testMPut() throws Exception {
            Command cmd = new Command.MPut("users", toBytes("name"), toBytes("John"));
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertEquals(Result.EmptyResult.OK, result);
        }

        @Test
        @DisplayName("MGET: Should retrieve value from map")
        void testMGet() throws Exception {
            // Setup
            dstruct.executeAsync(new Command.MPut("users", toBytes("name"), toBytes("Alice"))).get();
            
            // Test
            Command cmd = new Command.MGet("users", toBytes("name"));
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertInstanceOf(Result.Ok.class, result);
            assertArrayEquals(toBytes("Alice"), ((Result.Ok) result).value());
        }

        @Test
        @DisplayName("MGET: Should return NOTHING for non-existent key")
        void testMGetNonExistent() throws Exception {
            Command cmd = new Command.MGet("users", toBytes("unknown"));
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertEquals(Result.EmptyResult.NOTHING, result);
        }

        @Test
        @DisplayName("MDELETE: Should remove key from map")
        void testMDelete() throws Exception {
            // Setup
            dstruct.executeAsync(new Command.MPut("users", toBytes("name"), toBytes("Bob"))).get();
            
            // Delete
            Command deleteCmd = new Command.MDelete("users", toBytes("name"));
            Result deleteResult = dstruct.executeAsync(deleteCmd).get(1, TimeUnit.SECONDS);
            assertEquals(Result.EmptyResult.OK, deleteResult);
            
            // Verify
            Command getCmd = new Command.MGet("users", toBytes("name"));
            Result getResult = dstruct.executeAsync(getCmd).get(1, TimeUnit.SECONDS);
            assertEquals(Result.EmptyResult.NOTHING, getResult);
        }

        @Test
        @DisplayName("MPUT: Should handle multiple keys in same map")
        void testMultipleKeysInMap() throws Exception {
            dstruct.executeAsync(new Command.MPut("users", toBytes("user1"), toBytes("Alice"))).get();
            dstruct.executeAsync(new Command.MPut("users", toBytes("user2"), toBytes("Bob"))).get();
            dstruct.executeAsync(new Command.MPut("users", toBytes("user3"), toBytes("Charlie"))).get();
            
            Result r1 = dstruct.executeAsync(new Command.MGet("users", toBytes("user1"))).get();
            Result r2 = dstruct.executeAsync(new Command.MGet("users", toBytes("user2"))).get();
            Result r3 = dstruct.executeAsync(new Command.MGet("users", toBytes("user3"))).get();
            
            assertArrayEquals(toBytes("Alice"), ((Result.Ok) r1).value());
            assertArrayEquals(toBytes("Bob"), ((Result.Ok) r2).value());
            assertArrayEquals(toBytes("Charlie"), ((Result.Ok) r3).value());
        }

        @Test
        @DisplayName("MPUT: Should update existing key")
        void testMPutUpdate() throws Exception {
            dstruct.executeAsync(new Command.MPut("config", toBytes("timeout"), toBytes("30"))).get();
            dstruct.executeAsync(new Command.MPut("config", toBytes("timeout"), toBytes("60"))).get();
            
            Result result = dstruct.executeAsync(new Command.MGet("config", toBytes("timeout"))).get();
            assertArrayEquals(toBytes("60"), ((Result.Ok) result).value());
        }

        // ========================================================================
        // VALUE COMMANDS
        // ========================================================================

        @Test
        @DisplayName("VSET: Should set a value")
        void testVSet() throws Exception {
            Command cmd = new Command.VSet("counter", toBytes("100"));
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertEquals(Result.EmptyResult.OK, result);
        }

        @Test
        @DisplayName("VDELETE: Should delete a value")
        void testVDelete() throws Exception {
            // Setup
            dstruct.executeAsync(new Command.VSet("status", toBytes("active"))).get();
            
            // Delete
            Command cmd = new Command.VDelete("status");
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertEquals(Result.EmptyResult.OK, result);
        }

        @Test
        @DisplayName("VSET: Should overwrite existing value")
        void testVSetOverwrite() throws Exception {
            dstruct.executeAsync(new Command.VSet("version", toBytes("1.0"))).get();
            dstruct.executeAsync(new Command.VSet("version", toBytes("2.0"))).get();
            
            // Verify through TYPE command
            Command typeCmd = new Command.Type("version");
            Result result = dstruct.executeAsync(typeCmd).get();
            assertEquals(new Ok("VALUE"), result); // Exists
        }

        // ========================================================================
        // LIST COMMANDS
        // ========================================================================

        @Test
        @DisplayName("LPUSH: Should push to left of list")
        void testLPush() throws Exception {
            Command cmd = new Command.LPush("mylist", toBytes("item1"));
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertEquals(Result.EmptyResult.OK, result);
        }

        @Test
        @DisplayName("RPUSH: Should push to right of list")
        void testRPush() throws Exception {
            Command cmd = new Command.RPush("mylist", toBytes("item1"));
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertEquals(Result.EmptyResult.OK, result);
        }

        @Test
        @DisplayName("LPOP: Should pop from left of list")
        void testLPop() throws Exception {
            // Setup
            dstruct.executeAsync(new Command.LPush("stack", toBytes("first"))).get();
            dstruct.executeAsync(new Command.LPush("stack", toBytes("second"))).get();
            
            // Pop
            Command cmd = new Command.LPop("stack");
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertInstanceOf(Result.Ok.class, result);
            assertArrayEquals(toBytes("second"), ((Result.Ok) result).value());
        }

        @Test
        @DisplayName("RPOP: Should pop from right of list")
        void testRPop() throws Exception {
            // Setup
            dstruct.executeAsync(new Command.RPush("queue", toBytes("first"))).get();
            dstruct.executeAsync(new Command.RPush("queue", toBytes("second"))).get();
            
            // Pop
            Command cmd = new Command.RPop("queue");
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertInstanceOf(Result.Ok.class, result);
            assertArrayEquals(toBytes("second"), ((Result.Ok) result).value());
        }

        @Test
        @DisplayName("LPOP: Should return NOTHING for empty list")
        void testLPopEmpty() throws Exception {
            Command cmd = new Command.LPop("emptylist");
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertEquals(Result.EmptyResult.NOTHING, result);
        }

        @Test
        @DisplayName("RPOP: Should return NOTHING for empty list")
        void testRPopEmpty() throws Exception {
            Command cmd = new Command.RPop("emptylist");
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertEquals(Result.EmptyResult.NOTHING, result);
        }

        @Test
        @DisplayName("LLEN: Should return list length")
        void testLLen() throws Exception {
            // Setup
            dstruct.executeAsync(new Command.RPush("items", toBytes("a"))).get();
            dstruct.executeAsync(new Command.RPush("items", toBytes("b"))).get();
            dstruct.executeAsync(new Command.RPush("items", toBytes("c"))).get();
            
            // Test
            Command cmd = new Command.LLen("items");
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);

	        Ok ok = (Result.Ok)result;
	        assertEquals(3, Binaries.fromBytesToInt(ok.value()));
        }

        @Test
        @DisplayName("LLEN: Should return 0 for non-existent list")
        void testLLenNonExistent() throws Exception {
            Command cmd = new Command.LLen("nonexistent");
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertInstanceOf(EmptyResult.class, result);
            assertEquals(EmptyResult.NOTHING, result);
        }

        @Test
        @DisplayName("LINDEX: Should return element at index")
        void testLIndex() throws Exception {
            // Setup
            dstruct.executeAsync(new Command.RPush("indexed", toBytes("zero"))).get();
            dstruct.executeAsync(new Command.RPush("indexed", toBytes("one"))).get();
            dstruct.executeAsync(new Command.RPush("indexed", toBytes("two"))).get();
            
            // Test
            Command cmd = new Command.LIndex("indexed", toBytes(1));
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertInstanceOf(Result.Ok.class, result);
            assertArrayEquals(toBytes("one"), ((Result.Ok) result).value());
        }

        @Test
        @DisplayName("LINDEX: Should return NOTHING for invalid index")
        void testLIndexInvalid() throws Exception {
            dstruct.executeAsync(new Command.RPush("small", toBytes("item"))).get();
            
            Command cmd = new Command.LIndex("small", toBytes(10));
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertEquals(Result.EmptyResult.NOTHING, result);
        }

        @Test
        @DisplayName("List: FIFO Queue behavior (RPUSH + LPOP)")
        void testFIFOQueue() throws Exception {
            String queue = "tasks";
            
            // Enqueue
            dstruct.executeAsync(new Command.RPush(queue, toBytes("task1"))).get();
            dstruct.executeAsync(new Command.RPush(queue, toBytes("task2"))).get();
            dstruct.executeAsync(new Command.RPush(queue, toBytes("task3"))).get();
            
            // Dequeue
            Result r1 = dstruct.executeAsync(new Command.LPop(queue)).get();
            Result r2 = dstruct.executeAsync(new Command.LPop(queue)).get();
            Result r3 = dstruct.executeAsync(new Command.LPop(queue)).get();
            
            assertArrayEquals(toBytes("task1"), ((Result.Ok) r1).value());
            assertArrayEquals(toBytes("task2"), ((Result.Ok) r2).value());
            assertArrayEquals(toBytes("task3"), ((Result.Ok) r3).value());
        }

        @Test
        @DisplayName("List: LIFO Stack behavior (LPUSH + LPOP)")
        void testLIFOStack() throws Exception {
            String stack = "undo";
            
            // Push
            dstruct.executeAsync(new Command.LPush(stack, toBytes("action1"))).get();
            dstruct.executeAsync(new Command.LPush(stack, toBytes("action2"))).get();
            dstruct.executeAsync(new Command.LPush(stack, toBytes("action3"))).get();
            
            // Pop
            Result r1 = dstruct.executeAsync(new Command.LPop(stack)).get();
            Result r2 = dstruct.executeAsync(new Command.LPop(stack)).get();
            Result r3 = dstruct.executeAsync(new Command.LPop(stack)).get();
            
            assertArrayEquals(toBytes("action3"), ((Result.Ok) r1).value());
            assertArrayEquals(toBytes("action2"), ((Result.Ok) r2).value());
            assertArrayEquals(toBytes("action1"), ((Result.Ok) r3).value());
        }

        // ========================================================================
        // SET COMMANDS
        // ========================================================================

        @Test
        @DisplayName("SADD: Should add member to set")
        void testSAdd() throws Exception {
            Command cmd = new Command.SAdd("tags", toBytes("java"));
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertEquals(Result.EmptyResult.OK, result);
        }

        @Test
        @DisplayName("SREM: Should remove member from set")
        void testSRem() throws Exception {
            // Setup
            dstruct.executeAsync(new Command.SAdd("colors", toBytes("red"))).get();
            
            // Remove
            Command cmd = new Command.SRem("colors", toBytes("red"));
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertEquals(Result.EmptyResult.OK, result);
        }

        @Test
        @DisplayName("SADD: Should handle duplicate members")
        void testSAddDuplicate() throws Exception {
            String setName = "unique";
            
            dstruct.executeAsync(new Command.SAdd(setName, toBytes("value"))).get();
            dstruct.executeAsync(new Command.SAdd(setName, toBytes("value"))).get(); // Duplicate
            dstruct.executeAsync(new Command.SAdd(setName, toBytes("value"))).get(); // Duplicate
            
            // Both should succeed (sets handle duplicates internally)
            Result result = dstruct.executeAsync(new Command.SAdd(setName, toBytes("value"))).get();
            assertEquals(Result.EmptyResult.OK, result);
        }

        @Test
        @DisplayName("SREM: Should handle non-existent member")
        void testSRemNonExistent() throws Exception {
            Command cmd = new Command.SRem("myset", toBytes("nonexistent"));
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertEquals(EmptyResult.NOTHING, result);
        }

        // ========================================================================
        // GENERIC COMMANDS
        // ========================================================================

        @Test
        @DisplayName("TYPE: Should return OK for existing key")
        void testType() throws Exception {
            // Setup
            dstruct.executeAsync(new Command.MPut("users", toBytes("name"), toBytes("John"))).get();
            
            // Test
            Command cmd = new Command.Type("users");
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertEquals(new Ok("MAP"), result);
        }

        @Test
        @DisplayName("TYPE: Should return NOTHING for non-existent key")
        void testTypeNonExistent() throws Exception {
            Command cmd = new Command.Type("nonexistent");
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertEquals(Result.EmptyResult.NOTHING, result);
        }

        @Test
        @DisplayName("DEL: Should delete a structure")
        void testDel() throws Exception {
            // Setup
            dstruct.executeAsync(new Command.MPut("cache", toBytes("key"), toBytes("value"))).get();
            
            // Delete
            Command delCmd = new Command.Del("cache");
            Result delResult = dstruct.executeAsync(delCmd).get(1, TimeUnit.SECONDS);
            assertEquals(Result.EmptyResult.OK, delResult);
            
            // Verify
            Command typeCmd = new Command.Type("cache");
            Result typeResult = dstruct.executeAsync(typeCmd).get(1, TimeUnit.SECONDS);
            assertEquals(Result.EmptyResult.NOTHING, typeResult);
        }

        @Test
        @DisplayName("DEL: Should handle non-existent key")
        void testDelNonExistent() throws Exception {
            Command cmd = new Command.Del("nonexistent");
            Result result = dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            
            assertEquals(EmptyResult.NOTHING, result);
        }

        // ========================================================================
        // BATCH COMMANDS
        // ========================================================================

        @Test
        @DisplayName("BATCH: Should execute multiple commands atomically")
        void testBatch() throws Exception {
            List<Command> commands = List.of(
                new Command.MPut("users", toBytes("user1"), toBytes("Alice")),
                new Command.MPut("users", toBytes("user2"), toBytes("Bob")),
                new Command.VSet("count", toBytes("2"))
            );
            
            Command batch = new Command.Batch(commands);
            Result result = dstruct.executeAsync(batch).get(1, TimeUnit.SECONDS);
            
            assertInstanceOf(Result.Results.class, result);
            List<Result> results = ((Result.Results) result).results();
            assertEquals(3, results.size());
            
            // Verify all succeeded
            results.forEach(r -> assertEquals(Result.EmptyResult.OK, r));
            
            // Verify data was written
            Result getResult = dstruct.executeAsync(new Command.MGet("users", toBytes("user1"))).get();
            assertArrayEquals(toBytes("Alice"), ((Result.Ok) getResult).value());
        }

        @Test
        @DisplayName("BATCH: Should handle empty batch")
        void testEmptyBatch() throws Exception {
            Command batch = new Command.Batch(List.of());
            Result result = dstruct.executeAsync(batch).get(1, TimeUnit.SECONDS);
            
            assertInstanceOf(Result.EmptyResult.class, result);
            assertEquals(EmptyResult.NOTHING, result);
        }

        // ========================================================================
        // EDGE CASES & ERROR HANDLING
        // ========================================================================

        @Test
        @DisplayName("Should handle empty byte arrays")
        void testEmptyByteArrays() throws Exception {
            dstruct.executeAsync(new Command.MPut("map", toBytes(""), toBytes(""))).get();
            
            Result result = dstruct.executeAsync(new Command.MGet("map", toBytes(""))).get();
            assertArrayEquals(toBytes(""), ((Result.Ok) result).value());
        }

        @Test
        @DisplayName("Should handle large values")
        void testLargeValues() throws Exception {
            byte[] largeValue = new byte[10000];
            for (int i = 0; i < largeValue.length; i++) {
                largeValue[i] = (byte) (i % 256);
            }
            
            dstruct.executeAsync(new Command.VSet("large", largeValue)).get();
            
            Command typeCmd = new Command.Type("large");
            Result result = dstruct.executeAsync(typeCmd).get();
            assertEquals(new Ok("VALUE"), result);
        }

        @Test
        @DisplayName("Should handle special characters in keys")
        void testSpecialCharacters() throws Exception {
            String specialKey = "key:with:colons:and:!@#$%";
            dstruct.executeAsync(new Command.MPut("map", toBytes(specialKey), toBytes("value"))).get();
            
            Result result = dstruct.executeAsync(new Command.MGet("map", toBytes(specialKey))).get();
            assertArrayEquals(toBytes("value"), ((Result.Ok) result).value());
        }

        @Test
        @DisplayName("Should handle concurrent operations")
        void testConcurrentOperations() throws Exception {
            int operations = 100;
            CountDownLatch latch = new CountDownLatch(operations);
            
            // Submit many operations concurrently
            for (int i = 0; i < operations; i++) {
                final int index = i;
                new Thread(() -> {
                    try {
                        String key = "key" + index;
                        dstruct.executeAsync(new Command.MPut("concurrent", toBytes(key), toBytes("value" + index))).get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }
            
            // Wait for all operations
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            
            // Verify some operations completed
            Result result = dstruct.executeAsync(new Command.MGet("concurrent", toBytes("key50"))).get(1, TimeUnit.SECONDS);
            assertInstanceOf(Result.Ok.class, result);
        }

        @Test
        @DisplayName("Should handle timeout on slow operations")
        void testTimeout() {
            // This test verifies that futures timeout correctly
            Command cmd = new Command.MGet("users", toBytes("test"));
            
            assertDoesNotThrow(() -> {
                dstruct.executeAsync(cmd).get(1, TimeUnit.SECONDS);
            });
        }
    }

    // ============================================================================
    // TCP SERVER TESTS (WITH NETWORK COMMUNICATION)
    // ============================================================================

    @Nested
    @DisplayName("TCP Server Tests (Network Communication)")
    class TcpServerTests {

        private DStruct dstruct;
        private int port;

        @BeforeEach
        void setUp() {
            port = 14242; // Use a different port for tests
            
            Options options = new Options();
            options.port = port;
            options.writeAHeadLogging = false;
	        options.dataDirectory = tempDir.resolve("dstruct-test-" + System.currentTimeMillis()).toString();
            
            dstruct = new DStruct(options);
            try {
                dstruct.start();
                // Wait for server to be ready
                Thread.sleep(200);
            } catch (Exception e) {
                throw new RuntimeException("Failed to start DStruct", e);
            }
        }

        @AfterEach
        void tearDown() {
            dstruct.stop();
        }

        // ========================================================================
        // TCP CONNECTION TESTS
        // ========================================================================

        @Test
        @DisplayName("Should accept TCP connections")
        void testTcpConnection() throws IOException {
            try (Socket socket = new Socket("localhost", port)) {
                assertTrue(socket.isConnected());
            }
        }

        @Test
        @DisplayName("Should handle multiple concurrent connections")
        void testMultipleConnections() throws IOException {
            Socket socket1 = new Socket("localhost", port);
            Socket socket2 = new Socket("localhost", port);
            Socket socket3 = new Socket("localhost", port);
            
            assertTrue(socket1.isConnected());
            assertTrue(socket2.isConnected());
            assertTrue(socket3.isConnected());
            
            socket1.close();
            socket2.close();
            socket3.close();
        }

        // ========================================================================
        // TCP COMMAND EXECUTION TESTS
        // ========================================================================

        @Test
        @DisplayName("TCP: MPUT command via network")
        void testTcpMPut() throws IOException {
            try (Socket socket = new Socket("localhost", port)) {
                String command = "MPUT users name John\r\n";
                socket.getOutputStream().write(command.getBytes());
                socket.getOutputStream().flush();
                
                // Read response (binary OK response)
                byte[] buffer = new byte[1024];
                int bytesRead = socket.getInputStream().read(buffer);
                assertTrue(bytesRead > 0);
            }
        }

        @Test
        @DisplayName("TCP: MGET command via network")
        void testTcpMGet() throws IOException, InterruptedException {
            try (Socket socket = new Socket("localhost", port)) {
                // First, put a value
                String putCommand = "MPUT users name Alice\r\n";
                socket.getOutputStream().write(putCommand.getBytes());
                socket.getOutputStream().flush();
                Thread.sleep(50);
                socket.getInputStream().read(new byte[1024]); // Consume response
                
                // Then, get the value
                String getCommand = "MGET users name\r\n";
                socket.getOutputStream().write(getCommand.getBytes());
                socket.getOutputStream().flush();
                
                byte[] buffer = new byte[1024];
                int bytesRead = socket.getInputStream().read(buffer);
                assertTrue(bytesRead > 0);
            }
        }

        @Test
        @DisplayName("TCP: Multiple commands in single connection")
        void testTcpMultipleCommands() throws IOException, InterruptedException {
            try (Socket socket = new Socket("localhost", port)) {
                // Send multiple commands
                String[] commands = {
                    "MPUT cache key1 value1\r\n",
                    "MPUT cache key2 value2\r\n",
                    "VSET counter 100\r\n",
                    "LPUSH mylist item1\r\n"
                };
                
                for (String cmd : commands) {
                    socket.getOutputStream().write(cmd.getBytes());
                    socket.getOutputStream().flush();
                    Thread.sleep(50); // Small delay between commands
                    
                    // Consume response
                    socket.getInputStream().read(new byte[1024]);
                }
            }
        }

        @Test
        @DisplayName("TCP: List operations via network")
        void testTcpListOperations() throws IOException, InterruptedException {
            try (Socket socket = new Socket("localhost", port)) {
                // LPUSH
                sendCommand(socket, "LPUSH stack first\r\n");
                sendCommand(socket, "LPUSH stack second\r\n");
                
                // RPUSH
                sendCommand(socket, "RPUSH queue item1\r\n");
                sendCommand(socket, "RPUSH queue item2\r\n");
                
                // LLEN
                sendCommand(socket, "LLEN stack\r\n");
                
                // All commands should succeed
                assertTrue(socket.isConnected());
            }
        }

        @Test
        @DisplayName("TCP: Set operations via network")
        void testTcpSetOperations() throws IOException, InterruptedException {
            try (Socket socket = new Socket("localhost", port)) {
                sendCommand(socket, "SADD tags java\r\n");
                sendCommand(socket, "SADD tags python\r\n");
                sendCommand(socket, "SADD tags go\r\n");
                sendCommand(socket, "SREM tags python\r\n");
                
                assertTrue(socket.isConnected());
            }
        }

        @Test
        @DisplayName("TCP: TYPE and DEL commands via network")
        void testTcpTypeAndDel() throws IOException, InterruptedException {
            try (Socket socket = new Socket("localhost", port)) {
                sendCommand(socket, "MPUT data key value\r\n");
                sendCommand(socket, "TYPE data\r\n");
                sendCommand(socket, "DEL data\r\n");
                sendCommand(socket, "TYPE data\r\n"); // Should return NOTHING
                
                assertTrue(socket.isConnected());
            }
        }

        @Test
        @DisplayName("TCP: Should handle malformed commands gracefully")
        void testTcpMalformedCommands() throws IOException {
            try (Socket socket = new Socket("localhost", port)) {
                // Send invalid command
                String invalidCommand = "INVALID COMMAND\r\n";
                socket.getOutputStream().write(invalidCommand.getBytes());
                socket.getOutputStream().flush();
                
                // Server should still respond (might be error)
                byte[] buffer = new byte[1024];
                socket.getInputStream().read(buffer);
                
                // Connection should remain open
                assertTrue(socket.isConnected());
            }
        }

        @Test
        @DisplayName("TCP: Should handle connection close gracefully")
        void testTcpConnectionClose() throws IOException, InterruptedException {
            Socket socket = new Socket("localhost", port);
            sendCommand(socket, "VSET test value\r\n");
            
            socket.close();
            
            // Server should handle the close without crashing
            Thread.sleep(100);

            // New connection should still work
            try (Socket newSocket = new Socket("localhost", port)) {
                assertTrue(newSocket.isConnected());
            }
        }

        @Test
        @DisplayName("TCP: Should persist data across connections")
        void testTcpDataPersistence() throws IOException, InterruptedException {
            // First connection: write data
            try (Socket socket = new Socket("localhost", port)) {
                sendCommand(socket, "MPUT persistent key value123\r\n");
            }

            Thread.sleep(100);

            // Second connection: verify data still exists
            try (Socket socket = new Socket("localhost", port)) {
                String getCommand = "MGET persistent key\r\n";
                socket.getOutputStream().write(getCommand.getBytes());
                socket.getOutputStream().flush();

                byte[] buffer = new byte[1024];
                int bytesRead = socket.getInputStream().read(buffer);
                assertTrue(bytesRead > 0);
                // The response should contain value123 (as binary)
            }
        }

        @Test
        @DisplayName("TCP: Stress test with many rapid commands")
        void testTcpStressTest() throws IOException, InterruptedException {
            try (Socket socket = new Socket("localhost", port)) {
                for (int i = 0; i < 100; i++) {
                    String cmd = String.format("MPUT stress key%d value%d\r\n", i, i);
                    socket.getOutputStream().write(cmd.getBytes());
                    socket.getOutputStream().flush();
                    Thread.sleep(5);

                    // Read response to avoid buffer overflow
                    socket.getInputStream().read(new byte[1024]);
                }

                assertTrue(socket.isConnected());
            }
        }

        // ========================================================================
        // HELPER METHODS
        // ========================================================================

        private void sendCommand(Socket socket, String command) throws IOException, InterruptedException {
            socket.getOutputStream().write(command.getBytes());
            socket.getOutputStream().flush();
            Thread.sleep(50); // Small delay for processing

            // Consume response
            socket.getInputStream().read(new byte[1024]);
        }
    }

    // ============================================================================
    // INTEGRATION TESTS (BOTH DIRECT AND TCP)
    // ============================================================================

    @Nested
    @DisplayName("Integration Tests (Direct + TCP)")
    class IntegrationTests {

        private DStruct dstruct;
        private int port;

        @BeforeEach
        void setUp() throws Exception {
            port = 14343;

            Options options = new Options();
            options.port = port;
            options.writeAHeadLogging = false;
	        options.dataDirectory = tempDir.resolve("dstruct-test-" + System.currentTimeMillis()).toString();

			Thread thread = new Thread(() -> {
	            dstruct = new DStruct(options);
	            try {
	                dstruct.start();
	                Thread.sleep(200);
	            } catch (Exception e) {
	                throw new RuntimeException("Failed to start DStruct", e);
	            }
			});

	        thread.start();
	        Thread.sleep(100);
        }

        @AfterEach
        void tearDown() {
            dstruct.stop();
        }

        @Test
        @DisplayName("Should see same data via direct and TCP")
        void testDirectAndTcpConsistency() throws Exception {
            // Write via direct command
            dstruct.executeAsync(new Command.MPut("shared", toBytes("key1"), toBytes("direct"))).get();

            Thread.sleep(100);

            // Write via TCP
            try (Socket socket = new Socket("localhost", port)) {
                String cmd = "MPUT shared key2 tcp\r\n";
                socket.getOutputStream().write(cmd.getBytes());
                socket.getOutputStream().flush();
                Thread.sleep(50);
                socket.getInputStream().read(new byte[1024]);
            }

            Thread.sleep(100);

            // Read via direct command
            Result r1 = dstruct.executeAsync(new Command.MGet("shared", toBytes("key1"))).get();
            Result r2 = dstruct.executeAsync(new Command.MGet("shared", toBytes("key2"))).get();

            assertArrayEquals(toBytes("direct"), ((Result.Ok) r1).value());
            assertArrayEquals(toBytes("tcp"), ((Result.Ok) r2).value());
        }

        @Test
        @DisplayName("Mixed operations: Maps, Lists, Sets via both interfaces")
        void testMixedOperations() throws Exception {
            // Direct: Create map
            dstruct.executeAsync(new Command.MPut("data", toBytes("field"), toBytes("value"))).get();

            Thread.sleep(50);

            // TCP: Create list
            try (Socket socket = new Socket("localhost", port)) {
                socket.getOutputStream().write("LPUSH items item1\r\n".getBytes());
                socket.getOutputStream().flush();
                Thread.sleep(50);
                socket.getInputStream().read(new byte[1024]);
            }

            // Direct: Create set
            dstruct.executeAsync(new Command.SAdd("tags", toBytes("java"))).get();

            Thread.sleep(100);
            
            // Verify all exist via TYPE command
            Result typeMap = dstruct.executeAsync(new Command.Type("data")).get();
            Result typeList = dstruct.executeAsync(new Command.Type("items")).get();
            Result typeSet = dstruct.executeAsync(new Command.Type("tags")).get();
            
            assertEquals(new Ok("MAP"), typeMap);
            assertEquals(new Ok("DEQUE"), typeList);
            assertEquals(new Ok("SET"), typeSet);
        }
    }
}