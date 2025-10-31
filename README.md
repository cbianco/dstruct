# DStruct 

dstruct is my pet project that implements an in-memory database in Java. 
The goal is to replicate redis features.

Like Redis, the memory layout consists of data structures such as lists, hashes, sets, etc.

The communication with dstruct is only via commands, which are processed in a single-threaded event loop.
Commands are stored in memory and optionally to the disk in wal store (WAL write-ahead logging);

When wal is enabled the commands are saved in the order which they arrive.

## Features

- **In-memory data structures**: Support for values, lists, hashes, and sets
- **Single-threaded event loop**: Commands are processed sequentially for consistency
- **WAL (Write-Ahead Logging)**: Optional persistence to disk with command replay on restart
- **Command-based interface**: All operations are performed through commands

## Architecture

### Core Components

- **Event Loop**: Single-threaded command processor
- **Data Store**: In-memory storage engine with support for multiple data structures
- **WAL Manager**: Write-ahead logging system for durability
- **TCP Server**: NIO-based server using `Selector` and `ServerSocketChannel` for non-blocking I/O


## Supported Data Structures

### Value (String)
Simple key-value pairs for storing byte arrays.

**Commands:**
- `VSET <name> <value>` - Set a value
- `VGET <name>` - Get a value (read-only)
- `VDELETE <name>` - Delete a value

### Hash (Map)
Maps of field-value pairs, similar to Redis hashes.

**Commands:**
- `MPUT <name> <key> <value>` - Put a key-value pair in the hash
- `MGET <name> <key>` - Get a value by key from the hash (read-only)
- `MDELETE <name> <key>` - Delete a key from the hash

### List (Deque)
Ordered collections of elements with operations at both ends.

**Commands:**
- `LPUSH <name> <value>` - Push an element to the left (head) of the list
- `LPOP <name>` - Pop an element from the left (head) of the list
- `RPUSH <name> <value>` - Push an element to the right (tail) of the list
- `RPOP <name>` - Pop an element from the right (tail) of the list
- `LLEN <name>` - Get the length of the list (read-only)
- `LINDEX <name> <index>` - Get an element by index (supports negative indices) (read-only)

### Set
Unordered collections of unique elements.

**Commands:**
- `SADD <name> <value>` - Add an element to the set
- `SREM <name> <value>` - Remove an element from the set
- `SMEMBERS <name>` - Get all members of the set (read-only)

### Generic Commands
Commands that work across all data structures.

- `DEL <name>` - Delete a key and its associated data structure
- `TYPE <name>` - Get the type of a key (MAP, SET, DEQUE, VALUE, or NOTHING) (read-only)
- `BATCH [<command1>, <command2>, ...]` - Execute multiple commands atomically
- `CAST <type> <command>` - Cast the result of a command to a specific type (1=int, 2=boolean, 3=double) (read-only)
- `PING [<message>]` - Ping the server, returns "PONG" or the provided message (read-only)

### TCP Communication

DStruct uses a custom text-based protocol over TCP. The server:
- Uses Java NIO (`Selector`, `ServerSocketChannel`) for non-blocking operations
- Runs on a single-threaded event loop for connection management
- Supports asynchronous request/response handling

**Protocol format:**
- Commands are sent as text lines
- Type prefixes for values: `#` (boolean), `:` (integer), `,` (double), `+` (string) default is string
- Responses end with `\r\n`
- Cast operation with type prefix + '(' + command + ')' or `CAST : command`

**Example:**
```
VSET mykey :42
OK
VGET mykey
        <- 42 in bytes form
:(VGET mykey) <- cast operation ( sugar syntax )
42
CAST : VGET mykey <- cast operation ( explicit form ) 
42
```

## Quick start

```bash
docker build . -t dstruct
docker run -d -p 4242:4242 dstruct

telnet localhost 4242
Trying ::1...
telnet: connect to address ::1: Connection refused
Trying 127.0.0.1...
Connected to localhost.
Escape character is '^]'.
VSET marameo marameo
OK
VGET marameo
marameo
```

