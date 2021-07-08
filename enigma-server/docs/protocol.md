# Enigma protocol
Enigma uses TCP sockets for communication. Data is sent in each direction as a continuous stream, with packets being
concatenated one after the other.

In this document, data will be represented in C-like pseudocode. The primitive data types will be the same as those
defined by Java's [DataOutputStream](https://docs.oracle.com/javase/7/docs/api/java/io/DataOutputStream.html), i.e. in
big-endian order for multi-byte integers (`short`, `int` and `long`). The one exception is for Strings, which do *not*
use the same modified UTF format as in `DataOutputStream`, I repeat, the normal `writeUTF` method in `DataOutputStream`
(and the corresponding method in `DataInputStream`) should *not* be used. Instead, there is a custom `utf` struct for
Strings, see below.

## Login protocol
```
Client     Server
|               |
|     Login     |
| >>>>>>>>>>>>> |
|               |
| SyncMappings  |
| <<<<<<<<<<<<< |
|               |
| ConfirmChange |
| >>>>>>>>>>>>> |
```
1. On connect, the client sends a login packet to the server. This allows the server to test the validity of the client,
   as well as allowing the client to declare metadata about itself, such as the username.
1. After validating the login packet, the server sends all its mappings to the client, and the client will apply them.
1. Upon receiving the mappings, the client sends a `ConfirmChange` packet with `sync_id` set to 0, to confirm that it
   has received the mappings and is in sync with the server. Once the server receives this packet, the client will be
   allowed to modify mappings.

The server will not accept any other packets from the client until this entire exchange has been completed. 

## Kicking clients
When the server kicks a client, it may optionally send a `Kick` packet immediately before closing the connection, which
contains the reason why the client was kicked (so the client can display it to the user). This is not required though -
the server may simply terminate the connection.

## Changing mappings
This section uses the example of renaming, but the same pattern applies to all mapping changes.
```
Client A   Server    Client B
|           |               |
| RenameC2S |               |
| >>>>>>>>> |               |
|           |               |
|           |   RenameS2C   |
|           | >>>>>>>>>>>>> |
|           |               |
|           | ConfirmChange |
|           | <<<<<<<<<<<<< |
```

1. Client A validates the name and updates the mapping client-side to give the impression there is no latency >:)
1. Client A sends a rename packet to the server, notifying it of the rename.
1. The server assesses the validity of the rename. If it is invalid for whatever reason (e.g. the mapping was locked or
   the name contains invalid characters), then the server sends an appropriate packet back to client A to revert the
   change, with `sync_id` set to 0. The server will ignore any `ConfirmChange` packets it receives in response to this.
1. If the rename was valid, the server will lock all clients except client A from being able to modify this mapping, and
   then send an appropriate packet to all clients except client A notifying them of this rename. The `sync_id` will be a
   unique non-zero value identifying this change.
1. Each client responds to this packet by updating their mappings locally to reflect this change, then sending a
   `ConfirmChange` packet with the same `sync_id` as the one in the packet they received, to confirm that they have
   received the change.
1. When the server receives the `ConfirmChange` packet, and another change to that mapping hasn't occurred since, the
   server will unlock that mapping for that client and allow them to make changes again.

## Packets
```c
struct Packet {
    unsigned short packet_id;
    data[]; // depends on packet_id
}
```
The IDs for client-to-server packets are as follows:
- 0: `Login`
- 1: `ConfirmChange`
- 6: `Message`
- 7: `EntryChange`

The IDs for server-to-client packets are as follows:
- 0: `Kick`
- 1: `SyncMappings`
- 6: `Message`
- 7: `UserList`
- 8: `EntryChange`

### The utf struct
```c
struct utf {
    unsigned short length;
    byte data[length];
}
```
- `length`: The number of bytes in the UTF-8 encoding of the string. Note, this may not be the same as the number of
            Unicode characters in the string.
- `data`: A standard UTF-8 encoded byte array representing the string. 

### The Entry struct
```c
enum EntryType {
    ENTRY_CLASS = 0, ENTRY_FIELD = 1, ENTRY_METHOD = 2, ENTRY_LOCAL_VAR = 3;
}
struct Entry {
    unsigned byte type;
    boolean has_parent;
    if<has_parent> {
        Entry parent;
    }
    utf name;
    boolean has_javadoc;
    if<has_javadoc> {
        utf javadoc;
    }
    if<type == ENTRY_FIELD || type == ENTRY_METHOD> {
        utf descriptor;
    }
    if<type == ENTRY_LOCAL_VAR> {
        unsigned short index;
        boolean parameter;
    }
}
```
- `type`: The type of entry this is. One of `ENTRY_CLASS`, `ENTRY_FIELD`, `ENTRY_METHOD` or `ENTRY_LOCAL_VAR`.
- `parent`: The parent entry. Only class entries may have no parent. fields, methods and inner classes must have their
            containing class as their parent. Local variables have a method as a parent.
- `name`: The class/field/method/variable name.
- `javadoc`: The javadoc of an entry, if present.
- `descriptor`: The field/method descriptor.
- `index`: The index of the local variable in the local variable table.
- `parameter`: Whether the local variable is a parameter.

### The Message struct
```c
enum MessageType {
    MESSAGE_CHAT = 0,
    MESSAGE_CONNECT = 1,
    MESSAGE_DISCONNECT = 2,
    MESSAGE_EDIT_DOCS = 3,
    MESSAGE_MARK_DEOBF = 4,
    MESSAGE_REMOVE_MAPPING = 5,
    MESSAGE_RENAME = 6
};
typedef unsigned byte message_type_t;

struct Message {
    message_type_t type;
    union { // Note that the size of this varies depending on type, it is not constant size
        struct {
            utf user;
            utf message;
        } chat;
        struct {
            utf user;
        } connect;
        struct {
            utf user;
        } disconnect;
        struct {
            utf user;
            Entry entry;
        } edit_docs;
        struct {
            utf user;
            Entry entry;
        } mark_deobf;
        struct {
            utf user;
            Entry entry;
        } remove_mapping;
        struct {
            utf user;
            Entry entry;
            utf new_name;
        } rename;
    } data;
};
```
- `type`: The type of message this is. One of `MESSAGE_CHAT`, `MESSAGE_CONNECT`, `MESSAGE_DISCONNECT`,
    `MESSAGE_EDIT_DOCS`, `MESSAGE_MARK_DEOBF`, `MESSAGE_REMOVE_MAPPING`, `MESSAGE_RENAME`.
- `chat`: Chat message. Use in case `type` is `MESSAGE_CHAT`
- `connect`: Sent when a user connects. Use in case `type` is `MESSAGE_CONNECT`
- `disconnect`: Sent when a user disconnects. Use in case `type` is `MESSAGE_DISCONNECT`
- `edit_docs`: Sent when a user edits the documentation of an entry. Use in case `type` is `MESSAGE_EDIT_DOCS`
- `mark_deobf`: Sent when a user marks an entry as deobfuscated. Use in case `type` is `MESSAGE_MARK_DEOBF`
- `remove_mapping`: Sent when a user removes a mapping. Use in case `type` is `MESSAGE_REMOVE_MAPPING`
- `rename`: Sent when a user renames an entry. Use in case `type` is `MESSAGE_RENAME`
- `user`: The user that performed the action.
- `message`: The message the user sent.
- `entry`: The entry that was modified.
- `new_name`: The new name for the entry.

### The entry_change struct
```c
typedef enum tristate_change {
    TRISTATE_CHANGE_UNCHANGED = 0,
    TRISTATE_CHANGE_RESET = 1,
    TRISTATE_CHANGE_SET = 2
} tristate_change_t;

typedef enum access_modifier {
    ACCESS_MODIFIER_UNCHANGED = 0,
    ACCESS_MODIFIER_PUBLIC = 1,
    ACCESS_MODIFIER_PROTECTED = 2,
    ACCESS_MODIFIER_PRIVATE = 3
} access_modifier_t;

// Contains 4 packed values:
// bitmask   type
// 00000011  tristate_change_t deobf_name_change;
// 00001100  tristate_change_t access_change;
// 00110000  tristate_change_t javadoc_change;
// 11000000  access_modifier_t access_modifiers;
typedef uint8_t entry_change_flags;

struct entry_change {
    Entry entry;
    entry_change_flags flags;
    if <deobf_name_change == TRISTATE_CHANGE_SET> {
        utf deobf_name;
    }
    if <javadoc_change == TRISTATE_CHANGE_SET> {
        utf javadoc;
    }
}
```
- `entry`: The entry this change gets applied to.
- `flags`: See definition of `entry_change_flags`.
- `deobf_name`: The new deobfuscated name, if deobf_name_change == TRISTATE_CHANGE_SET
- `javadoc`: The new javadoc, if javadoc_change == TRISTATE_CHANGE_SET
- `access_modifiers`: The new access modifier, if access_change == TRISTATE_CHANGE_SET (otherwise 0)

### Login (client-to-server)
```c
struct LoginC2SPacket {
    unsigned short protocol_version;
    byte checksum[20];
    unsigned byte password_length;
    char password[password_length];
    utf username;
}
```
- `protocol_version`: the version of the protocol. If the version does not match on the server, then the client will be
                      kicked immediately. Currently always equal to 0.
- `checksum`: the SHA-1 hash of the JAR file the client has open. If this does not match the SHA-1 hash of the JAR file
              the server has open, the client will be kicked.
- `password`: the password needed to log into the server. Note that each `char` is 2 bytes, as per the Java data type.
              If this password is incorrect, the client will be kicked.
- `username`: the username of the user logging in. If the username is not unique, the client will be kicked.

### ConfirmChange (client-to-server)
```c
struct ConfirmChangeC2SPacket {
    unsigned short sync_id;
}
```
- `sync_id`: the sync ID to confirm.

### Message (client-to-server)
```c
struct MessageC2SPacket {
    utf message;
}
```
- `message`: The text message the user sent.

### EntryChange (client-to-server)
```c
struct EntryChangeC2SPacket {
    entry_change change;
}
```
- `change`: The change to apply.

### Kick (server-to-client)
```c
struct KickS2CPacket {
    utf reason;
}
```
- `reason`: the reason for the kick, may or may not be a translation key for the client to display to the user.

### SyncMappings (server-to-client)
```c
struct SyncMappingsS2CPacket {
    int num_roots;
    MappingNode roots[num_roots];
}
struct MappingNode {
    NoParentEntry obf_entry;
    boolean is_named;
    utf name;
    utf javadoc;
    unsigned short children_count;
    MappingNode children[children_count];
}
typedef { Entry but without the has_parent or parent fields } NoParentEntry;
```
- `roots`: The root mapping nodes, containing all the entries without parents.
- `obf_entry`: The value of a node, containing the obfuscated name and descriptor of the entry.
- `name`: The deobfuscated name of the entry, if it exists, otherwise the empty string.
- `javadoc`: The documentation for the entry, if it exists, otherwise the empty string.
- `children`: The children of this node

### Message (server-to-client)
```c
struct MessageS2CPacket {
    Message message;
}
```

### UserList (server-to-client)
```c
struct UserListS2CPacket {
    unsigned short len;
    utf user[len];
}
```

### EntryChange (server-to-client)
```c
struct EntryChangeS2CPacket {
    uint16_t sync_id;
    entry_change change;
}
```
- `sync_id`: The sync ID of the change for locking purposes.
- `change`: The change to apply.