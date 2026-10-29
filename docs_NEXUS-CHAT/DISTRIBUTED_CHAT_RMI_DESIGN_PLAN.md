# DISTRIBUTED CHAT ROOM — DESIGN & IMPLEMENTATION PLAN

**Project:** Distributed Chat Room using Java RMI  
**UI Technology:** JavaFX, FXML, CSS  
**Architecture:** Distributed Client–Server with Bidirectional Message Replication  
**Database:** MySQL / MariaDB + JDBC  
**Build Tool:** Maven  
**Language:** Java  
**Project Status:** Implementation Specification

---

## 1. Project Overview

### 1.1. Objective

Build a distributed chat system in which multiple clients can communicate in shared chat rooms even when they connect to different physical servers.

The system must demonstrate:

- Java RMI remote communication.
- Multiple servers running on separate physical computers.
- Clients running on VirtualBox or VMware virtual machines.
- Bidirectional message replication between servers.
- Persistent message storage using MySQL/MariaDB.
- JavaFX desktop client with a professional user interface.
- Network connection monitoring, error handling, and recovery.

### 1.2. Required Demonstration Environment

| Component | Deployment |
|---|---|
| Server A | Physical Computer A |
| Server B | Physical Computer B |
| Client 1 | Virtual Machine 1 |
| Client 2 | Virtual Machine 2 |
| Client 3 | Virtual Machine 3 (optional) |
| Database A | MySQL/MariaDB on Computer A |
| Database B | MySQL/MariaDB on Computer B |

The virtual machines and physical computers must be able to communicate over the same reachable network.

### 1.3. Core Demonstration Scenario

1. Client 1 connects to Server A.
2. Client 2 connects to Server B.
3. Client 1 sends a message.
4. Server A stores the message in Database A.
5. Server A replicates the message to Server B through Java RMI.
6. Server B stores the message in Database B.
7. Client 2 retrieves and displays the message.
8. Client 2 replies through Server B.
9. Server B replicates the reply to Server A.
10. Client 1 receives the reply.

**Expected result:** Clients connected to different servers can communicate in the same logical chat room.

---

## 2. Architecture

### 2.1. High-Level Architecture

```text
+----------------------+               +----------------------+
| VM Client 1          |               | VM Client 2          |
| JavaFX Desktop App   |               | JavaFX Desktop App   |
+----------+-----------+               +-----------+----------+
           |                                           |
           | Java RMI                                  | Java RMI
           v                                           v
+----------------------+   Java RMI   +----------------------+
| Physical Computer A   |<------------>| Physical Computer B   |
| Chat Server A        |              | Chat Server B        |
| RMI Registry         |              | RMI Registry         |
| Chat Service         |              | Chat Service         |
| Replication Service  |              | Replication Service  |
+----------+-----------+              +-----------+----------+
           |                                           |
           v                                           v
+----------------------+              +----------------------+
| Database A           |              | Database B           |
| MySQL / MariaDB      |              | MySQL / MariaDB      |
+----------------------+              +----------------------+
```

### 2.2. Architectural Modules

**Module 1 — `chat-contract`**

Contains shared DTOs, enums, and RMI interfaces.

**Module 2 — `chat-server`**

Contains the RMI server, business logic, message persistence, replication, client session tracking, and monitoring.

**Module 3 — `chat-desktop`**

Contains the JavaFX desktop application, FXML layouts, CSS, controllers, and client-side RMI services.

### 2.3. Important Design Principle

The JavaFX client must not connect directly to MySQL/MariaDB.

All client operations go through the RMI service. The server validates requests, applies business rules, persists messages, and communicates with its peer server.

---

## 3. Technology Stack

| Technology | Purpose |
|---|---|
| Java 17 or another consistently configured supported JDK | Application runtime |
| Maven | Dependency management and build |
| Java RMI | Remote communication |
| JavaFX | Desktop user interface |
| FXML | UI layout definition |
| CSS | UI styling |
| MySQL or MariaDB | Persistent storage |
| JDBC | Database access |
| SLF4J + Logback | Application logging |
| JUnit 5 | Unit testing |

Use one JDK version consistently across all computers and virtual machines.

Use a JavaFX version compatible with the selected JDK.

---

## 4. Maven Project Structure

```text
distributed-chat-rmi/
├── pom.xml
├── README.md
├── .gitignore
├── docs/
│   ├── architecture.md
│   ├── network-setup.md
│   └── test-plan.md
│
├── chat-contract/
│   ├── pom.xml
│   └── src/main/java/com/nexus/chat/contract/
│       ├── dto/
│       │   ├── ChatMessageDTO.java
│       │   ├── ChatRoomDTO.java
│       │   ├── UserDTO.java
│       │   ├── ClientSessionDTO.java
│       │   ├── ServerStatusDTO.java
│       │   └── OperationResult.java
│       ├── enums/
│       │   ├── MessageType.java
│       │   ├── SessionStatus.java
│       │   └── ServerStatus.java
│       └── remote/
│           ├── ChatRemote.java
│           ├── ReplicationRemote.java
│           └── MonitoringRemote.java
│
├── chat-server/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/nexus/chat/server/
│       │   ├── ServerApplication.java
│       │   ├── config/
│       │   │   ├── ServerConfig.java
│       │   │   └── DatabaseConfig.java
│       │   ├── rmi/
│       │   │   ├── RmiRegistryManager.java
│       │   │   └── RmiServiceExporter.java
│       │   ├── service/
│       │   │   ├── ChatServiceImpl.java
│       │   │   ├── ReplicationServiceImpl.java
│       │   │   ├── MonitoringServiceImpl.java
│       │   │   ├── ClientSessionService.java
│       │   │   └── PeerSyncService.java
│       │   ├── repository/
│       │   │   ├── MessageRepository.java
│       │   │   ├── JdbcMessageRepository.java
│       │   │   ├── ClientSessionRepository.java
│       │   │   └── ChatRoomRepository.java
│       │   ├── network/
│       │   │   ├── PeerConnectionManager.java
│       │   │   └── NetworkDiagnostics.java
│       │   └── monitoring/
│       │       ├── ServerMetrics.java
│       │       └── SystemLogService.java
│       └── resources/
│           ├── application.properties.example
│           └── db/
│               └── db-schema.sql
│
└── chat-desktop/
    ├── pom.xml
    └── src/main/
        ├── java/com/nexus/chat/desktop/
        │   ├── DesktopApplication.java
        │   ├── config/
        │   │   └── ClientConfig.java
        │   ├── rmi/
        │   │   └── RmiClientManager.java
        │   ├── service/
        │   │   ├── ChatClientService.java
        │   │   ├── MessagePollingService.java
        │   │   └── ConnectionMonitorService.java
        │   ├── controller/
        │   │   ├── LoginController.java
        │   │   ├── ChatController.java
        │   │   ├── ServerConnectionController.java
        │   │   └── SettingsController.java
        │   └── model/
        │       ├── ChatMessageModel.java
        │       └── ConnectionStateModel.java
        └── resources/com/nexus/chat/desktop/
            ├── fxml/
            │   ├── login-view.fxml
            │   ├── chat-view.fxml
            │   ├── server-connection-view.fxml
            │   └── settings-view.fxml
            ├── css/
            │   ├── design-system.css
            │   ├── login.css
            │   ├── chat.css
            │   └── components.css
            └── images/
```

This is the target structure. Create files progressively rather than creating every empty file at the beginning.

---

## 5. Java RMI Contract

### 5.1. `ChatRemote`

Responsibilities:

- Register or establish a client session.
- Join a chat room.
- Send a message.
- Retrieve messages after a given cursor.
- Retrieve room information.
- Leave a room or disconnect a session.
- Retrieve the current server identity.

Suggested API:

```java
public interface ChatRemote extends java.rmi.Remote {
    OperationResult registerClient(
        String username,
        String clientInstanceId
    ) throws java.rmi.RemoteException;

    OperationResult joinRoom(
        String sessionId,
        String roomId
    ) throws java.rmi.RemoteException;

    ChatMessageDTO sendMessage(
        String sessionId,
        String roomId,
        String content
    ) throws java.rmi.RemoteException;

    java.util.List<ChatMessageDTO> getMessages(
        String sessionId,
        String roomId,
        long afterSequence
    ) throws java.rmi.RemoteException;

    void disconnectClient(
        String sessionId
    ) throws java.rmi.RemoteException;
}
```

Adapt the API when the DTO and authentication design is finalized.

### 5.2. `ReplicationRemote`

Responsibilities:

- Receive messages replicated from another server.
- Receive synchronization batches.
- Return synchronization status.
- Avoid sending received messages back to the source as new replications.

Suggested API:

```java
public interface ReplicationRemote
        extends java.rmi.Remote {

    OperationResult replicateMessage(
        ChatMessageDTO message,
        String sourceServerId
    ) throws java.rmi.RemoteException;

    java.util.List<ChatMessageDTO> getMessagesAfter(
        long afterSequence,
        int limit
    ) throws java.rmi.RemoteException;
}
```

The implementation must validate the source server and use idempotent persistence.

### 5.3. `MonitoringRemote`

Responsibilities:

- Return server health.
- Return replication status.
- Return active client sessions.
- Return database connectivity status.
- Return recent operational metrics.

Do not expose credentials, passwords, database connection strings containing secrets, or unrestricted administrative operations.

---

## 6. Message Model

### 6.1. `ChatMessageDTO`

Recommended fields:

| Field | Purpose |
|---|---|
| `messageId` | Globally unique UUID |
| `roomId` | Logical chat room |
| `senderId` | User identifier |
| `senderName` | Display name |
| `content` | Message body |
| `createdAt` | Original creation time |
| `originServerId` | Server that first accepted the message |
| `originSequence` | Sequence allocated by the origin server |
| `messageType` | CHAT, SYSTEM, or another supported type |

Use `UUID` for globally unique message identity.

Use UTC timestamps internally and convert to local time for display.

### 6.2. Message Ordering

Two independent servers can accept messages simultaneously. Therefore, local database auto-increment IDs alone do not establish a global chronological order.

For the initial project:

- Use `messageId` as the identity.
- Use `createdAt` for display ordering.
- Use `originServerId` and `originSequence` for origin-specific synchronization.
- Define a deterministic tie-breaker when timestamps match.

Do not claim strict global total ordering unless an ordering protocol is implemented.

---

## 7. Database Design

### 7.1. Deployment Model

Each physical server runs its own database:

- Server A writes to Database A.
- Server B writes to Database B.
- Replicated messages are stored locally on both servers.

The initial implementation uses asynchronous replication through Java RMI. It does not require a shared database.

### 7.2. `chat_messages`

Recommended columns:

```sql
CREATE TABLE chat_messages (
    message_id CHAR(36) PRIMARY KEY,
    room_id VARCHAR(100) NOT NULL,
    sender_id VARCHAR(100) NOT NULL,
    sender_name VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    origin_server_id VARCHAR(100) NOT NULL,
    origin_sequence BIGINT NOT NULL,
    message_type VARCHAR(30) NOT NULL,
    received_at TIMESTAMP(3) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uq_origin_sequence (
        origin_server_id,
        origin_sequence
    ),
    INDEX idx_room_created (
        room_id,
        created_at
    )
);
```

Verify timestamp behavior and SQL compatibility with the selected MySQL/MariaDB version.

### 7.3. Additional Tables

Create these when the corresponding functionality is implemented:

**`chat_rooms`**
- `room_id`
- `room_name`
- `created_at`

**`client_sessions`**
- `session_id`
- `username`
- `client_instance_id`
- `remote_ip`
- `remote_port`
- `connected_at`
- `last_seen_at`
- `status`
- `connected_server_id`

**`replication_state`**
- `peer_server_id`
- `last_success_at`
- `last_sync_status`
- `last_error`

**`server_events`**
- `event_id`
- `event_type`
- `severity`
- `event_message`
- `created_at`

Do not store plaintext passwords or sensitive credentials in logs.

---

## 8. Bidirectional Replication

### 8.1. Normal Send Flow

When a client sends a message to Server A:

1. Validate the client session and room.
2. Validate message content and size.
3. Generate a UUID for the message.
4. Allocate an origin sequence.
5. Persist the message in Database A.
6. Make the message available to local clients.
7. Submit the message to Server B through `ReplicationRemote`.
8. Server B validates the request.
9. Server B inserts the message using an idempotent operation.
10. Server B makes the message available to its local clients.

The local message should not be lost merely because the peer server is unavailable.

### 8.2. Prevent Infinite Replication Loops

The rules are mandatory:

- A locally created message is replicated to the peer.
- A message received through `replicateMessage` is stored locally.
- A received replicated message must not be replicated back as a new local message.
- Duplicate `messageId` values must not create duplicate rows.
- Retries must preserve the original message ID.

Use database uniqueness constraints in addition to application-level duplicate checks.

### 8.3. Peer Failure

If Server B is unreachable:

- Server A continues accepting valid local messages.
- Record the replication failure.
- Mark the peer as disconnected or degraded.
- Retry using bounded exponential backoff.
- Retain unsynchronized messages or derive them from an origin-sequence cursor.
- Synchronize missing messages after connectivity returns.

For reliability, implement a persistent outbox or an equivalent recoverable synchronization mechanism. Do not rely exclusively on an in-memory queue.

### 8.4. Synchronization Recovery

A practical first version can use per-origin sequence cursors.

Each server tracks the last successfully synchronized sequence for each origin server. When reconnecting, it requests messages after the last acknowledged sequence and processes them in batches.

The receiver must tolerate duplicate batches safely.

Define acknowledgments only after the receiver has durably stored the batch. Handle partial batch failure without silently skipping messages.

### 8.5. Replication Status

Expose monitoring information such as:

- Peer server ID.
- Peer endpoint.
- Connection status.
- Last successful synchronization time.
- Pending message count, if measured.
- Last replication error.
- Last synchronization duration.

Do not display a pending count as authoritative unless it is calculated from real synchronization state.

---

## 9. Client Sessions and IP Monitoring

### 9.1. Tracking Requirements

The server should track:

- Session ID.
- Username.
- Client instance ID.
- Remote IP address.
- Remote port, when available.
- Connected server.
- Connection time.
- Last successful heartbeat or request.
- Current session status.

### 9.2. Obtaining Client IP

A Java RMI server can inspect the remote endpoint for an incoming RMI call using `RemoteServer.getClientHost()` from the server-side call context.

Important limitations:

- The address may be the VM's NAT address, a proxy address, or another network endpoint.
- Remote port information is not provided by `getClientHost()` itself.
- The server cannot infer every active client from the RMI Registry alone.
- A client session record must be maintained by the application.

Record the address obtained from the server-side call context when registering or handling a client request. Handle `ServerNotActiveException` and any context where the address is unavailable.

Do not trust a client-supplied IP address as authoritative.

### 9.3. Session Liveness

Use periodic heartbeat or successful client activity to update `last_seen_at`.

If a client abruptly loses power or network access, the server may not receive a disconnect call. Mark the session stale after a configured timeout.

A timeout is an application-defined estimate of liveness, not proof that the client machine is powered off.

---

## 10. JavaFX Client Design

### 10.1. UI Design Direction

Use a professional dark interface suitable for a technical networking project.

Suggested brand direction:

- Product name: NEXUS CHAT.
- Visual style: modern, minimal, technical.
- Background: dark charcoal.
- Panels: layered dark surfaces.
- Accent: restrained violet or cyan.
- Typography: readable sans-serif.
- Status colors: green for healthy, amber for degraded, red for disconnected.

Use consistent spacing, typography, borders, icons, focus states, and error messages.

Avoid excessive glow, neon effects, animations, or unnecessary decorative charts.

### 10.2. Login / Connection Screen

Required elements:

- Product logo and application name.
- Username field.
- Server address field.
- RMI service port or service profile.
- Connect button.
- Connection progress indicator.
- Validation and error messages.
- Remember non-sensitive connection preferences.

Never save credentials in plaintext.

### 10.3. Main Chat Screen

Required layout:

**Sidebar**
- Application logo.
- Connection status.
- Chat room list.
- Current user.
- Settings and disconnect actions.

**Main chat area**
- Room name and description.
- Message history.
- Sender and timestamp.
- Message input.
- Send button.
- Empty, loading, and error states.

**Optional information panel**
- Current server.
- Peer replication status.
- Connection details.

Network diagnostics should remain secondary to the main messaging experience.

### 10.4. Connection Status

Use meaningful states:

- Connecting.
- Connected.
- Reconnecting.
- Degraded.
- Disconnected.
- Authentication or authorization failure.

Display actual server results rather than changing the UI optimistically to a successful state.

### 10.5. JavaFX Thread Safety

Never perform blocking RMI calls or JDBC calls on the JavaFX Application Thread.

Use Java concurrency utilities, JavaFX background tasks, or a managed executor.

Use `Platform.runLater()` or JavaFX observable state to update UI controls from background work.

Ensure executors and scheduled tasks are shut down during application exit.

---

## 11. Admin Dashboard Scope

An administrative monitoring interface is useful for demonstrating the distributed architecture. It should be implemented after the core chat and replication flow works.

### 11.1. Overview

Display actual metrics:

- Current server identity.
- Server uptime.
- Database connectivity.
- Peer connectivity.
- Number of tracked active sessions.
- Replication health.
- Recent errors.

### 11.2. Network / Server View

Display:

- Server A and Server B.
- Configured endpoints.
- RMI service status.
- Peer connection status.
- Last successful synchronization.
- Replication failures.

Only show a connection as healthy when recent checks support that state.

### 11.3. Client Sessions

Display:

- Username.
- Remote IP.
- Remote port if collected reliably.
- Connected server.
- Connection time.
- Last seen.
- Session status.

Apply authorization before exposing this information. Treat IP addresses as operational data and avoid unnecessary retention.

### 11.4. Logs

Display:

- Timestamp.
- Severity.
- Component.
- Event description.
- Server identity.

Never log passwords, database credentials, session secrets, or full sensitive message contents.

### 11.5. Administration Security

Do not expose unrestricted monitoring or administrative methods to ordinary chat clients.

Use a dedicated administrator role and server-side authorization. A hidden button or hidden screen is not access control.

For a classroom demonstration, bind the admin interface to an appropriate trusted interface and document the security limitations.

---

## 12. Network Deployment

### 12.1. Required Configuration

Each server needs:

- A stable LAN address or a reliably resolvable hostname.
- A fixed RMI Registry port.
- A fixed exported RMI service port.
- A database endpoint.
- A unique server ID.
- The peer server endpoint.

Use configuration files or environment variables instead of hardcoding machine-specific IP addresses.

Example configuration for Server A:

```properties
server.id=server-a
server.rmi.registry.port=1099
server.rmi.service.port=1100
server.peer.host=192.168.1.102
server.peer.registry.port=1099
server.peer.service.port=1100

database.host=127.0.0.1
database.port=3306
database.name=nexus_chat
database.username=${DB_USERNAME}
database.password=${DB_PASSWORD}
```

The exact configuration loader must support the chosen environment-variable substitution strategy. Java `Properties` does not automatically expand `${...}`.

Use a separate configuration for Server B.

### 12.2. RMI Hostname

Set the advertised RMI hostname to the server's reachable LAN address before exporting remote objects.

For example:

```bash
java -Djava.rmi.server.hostname=192.168.1.101 ...
```

The IP is only an example. Replace it with the actual address of the host running the server.

If RMI exports objects on dynamic ports, configure fixed export ports or explicitly account for the ports required by the implementation and firewall.

### 12.3. VirtualBox / VMware

For a straightforward LAN demonstration, configure the VM network adapter as **Bridged Adapter** when the network permits it.

Verify that:

1. The VM has a LAN address.
2. The VM can reach Server A.
3. The VM can reach Server B.
4. Required RMI ports are reachable.
5. The servers can reach each other.
6. Host firewalls allow the required inbound connections.

Some Wi-Fi networks, university networks, and managed access points block bridged clients or peer-to-peer communication. If bridged mode is unavailable, select a documented alternative network topology and configure routing appropriately.

### 12.4. Network Testing

Test basic reachability before starting the application.

Windows:

```powershell
ipconfig
ping 192.168.1.101
Test-NetConnection 192.168.1.101 -Port 1099
Test-NetConnection 192.168.1.101 -Port 1100
```

Linux:

```bash
ip addr
ping 192.168.1.101
nc -vz 192.168.1.101 1099
nc -vz 192.168.1.101 1100
```

Replace the sample address with the real target address.

A successful ping alone does not prove RMI is configured correctly. Test the registry, exported service, and advertised RMI hostname.

---

## 13. Error Handling

The application must handle:

- Server unavailable.
- Registry unavailable.
- RMI timeout or remote exception.
- Database unavailable.
- Invalid message content.
- Session expiration.
- Duplicate message.
- Peer replication failure.
- Client disconnection.
- Partial synchronization failure.

Show clear, actionable messages to users. Keep stack traces and detailed diagnostics in server logs, not in ordinary UI notifications.

Do not report a replicated message as synchronized until the receiving server acknowledges durable persistence.

---

## 14. Security Requirements

Minimum safeguards:

- Validate usernames, room IDs, and message content.
- Limit message length.
- Restrict database permissions.
- Keep secrets outside source control.
- Restrict monitoring methods to authorized administrators.
- Validate replication callers.
- Use network firewall rules.
- Avoid logging credentials and secrets.

Java RMI is not automatically secure simply because it is remote method invocation. Production deployments require an appropriate authentication, authorization, transport-security, and deserialization strategy.

For the lab, document any simplifications and limit the service to the trusted demonstration network.

---

## 15. Implementation Plan

### Phase 1 — Environment and Build

- [ ] Create the root Maven project.
- [ ] Create `chat-contract`.
- [ ] Create `chat-server`.
- [ ] Create `chat-desktop`.
- [ ] Verify dependency resolution.
- [ ] Verify the complete project builds.

**Exit criterion:** `mvn clean package` succeeds.

### Phase 2 — Shared Contracts

- [ ] Implement DTOs.
- [ ] Implement enums.
- [ ] Define `ChatRemote`.
- [ ] Define `ReplicationRemote`.
- [ ] Define `MonitoringRemote`.
- [ ] Verify DTO compatibility across modules.

**Exit criterion:** All modules compile against the shared contracts.

### Phase 3 — Single-Server RMI

- [ ] Start the RMI Registry.
- [ ] Export and bind the chat service.
- [ ] Connect a small test client.
- [ ] Register a session.
- [ ] Send and retrieve a message.
- [ ] Test remote exceptions.

**Exit criterion:** A client can communicate with one server through RMI.

### Phase 4 — Database Persistence

- [ ] Create the database.
- [ ] Run `db-schema.sql`.
- [ ] Configure JDBC.
- [ ] Implement repositories.
- [ ] Persist messages.
- [ ] Retrieve messages from the database.
- [ ] Test duplicate insertion.

**Exit criterion:** Messages survive a server restart.

### Phase 5 — Bidirectional Replication

- [ ] Assign unique server IDs.
- [ ] Configure peer endpoints.
- [ ] Implement replication.
- [ ] Prevent replication loops.
- [ ] Enforce message idempotency.
- [ ] Track synchronization progress.
- [ ] Implement retry and recovery.
- [ ] Test both message directions.

**Exit criterion:** Messages created on either server become available on both servers without duplicate records.

### Phase 6 — JavaFX Client

- [ ] Implement connection screen.
- [ ] Implement the chat layout.
- [ ] Implement message history.
- [ ] Implement message composition.
- [ ] Implement connection state.
- [ ] Move network work off the UI thread.
- [ ] Add loading and error states.

**Exit criterion:** A JavaFX client can chat through RMI without freezing the interface.

### Phase 7 — Monitoring and Admin

- [ ] Track client sessions.
- [ ] Record remote IP addresses.
- [ ] Implement monitoring remote methods.
- [ ] Display server and peer health.
- [ ] Display replication status.
- [ ] Display recent operational events.
- [ ] Protect administrative access.

**Exit criterion:** Admin information comes from actual backend measurements.

### Phase 8 — Multi-Machine Demonstration

- [ ] Configure Server A.
- [ ] Configure Server B.
- [ ] Configure separate databases.
- [ ] Verify host-to-host connectivity.
- [ ] Verify VM-to-host connectivity.
- [ ] Run Client 1 against Server A.
- [ ] Run Client 2 against Server B.
- [ ] Verify cross-server messaging.
- [ ] Stop a peer temporarily and test recovery.
- [ ] Capture screenshots and logs for the report.

**Exit criterion:** The complete demonstration works across the intended physical machines and virtual machines.

---

## 16. Testing Plan

| Test | Expected result |
|---|---|
| Client connects to Server A | Session is registered |
| Client connects to Server B | Session is registered |
| Local message sent through A | Message appears for A's clients |
| Message from A reaches B | Message is persisted and visible on B |
| Message sent through B | Message is replicated to A |
| Duplicate replication request | No duplicate message record |
| Peer becomes unavailable | Local messages remain durable |
| Peer reconnects | Missing messages are synchronized |
| Database unavailable | Clear error; no false success |
| Client disconnects unexpectedly | Session becomes stale after timeout |
| Admin views session list | Actual tracked sessions and IPs are shown |
| JavaFX network request is slow | UI remains responsive |

---

## 17. Acceptance Criteria

The project is considered ready for demonstration when:

1. All Maven modules build successfully.
2. Two physical servers run independently.
3. Each server has its own database.
4. JavaFX clients run on virtual machines.
5. Clients connect to different servers.
6. Messages replicate in both directions.
7. Duplicate replication does not create duplicate rows.
8. Peer failures do not silently discard already persisted messages.
9. Missing messages can be synchronized after reconnection.
10. The Admin view displays real server, replication, and client-session data.
11. Client IP addresses are collected on the server side where available.
12. The UI remains responsive during network operations.
13. The deployment steps and limitations are documented.

---

## 18. Recommended Coding Order

**Do not start by building the entire UI.**

Follow this order:

1. Maven project structure.
2. Shared DTOs and remote interfaces.
3. One-server RMI proof of concept.
4. Database persistence.
5. Bidirectional replication.
6. Multi-machine network deployment.
7. JavaFX chat interface.
8. Client-session and IP tracking.
9. Admin dashboard.
10. Integration testing and documentation.

This order reduces the risk of creating an attractive interface that cannot communicate with the backend.

---

## 19. Final Architecture Principle

The project must demonstrate a real distributed system, not merely two server windows running on one computer.

The core proof is:

**Client A → Server A → Database A → RMI Replication → Server B → Database B → Client B**

The reverse direction must work as well.

The user interface, monitoring dashboard, and status indicators must reflect actual backend behavior. Any simulated metrics must be explicitly labeled as simulated and must not be presented as live system data.
