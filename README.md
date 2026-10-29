# NEXUS DISTRIBUTED CHAT — SYSTEM ARCHITECTURE & FOUNDATION

> **Distributed Desktop Chat System using Java RMI, JavaFX, and MySQL/MariaDB with Bidirectional Replication**  
> **Coursework Project Baseline — Foundation Release 1.0.0-SNAPSHOT**

---

## 1. Executive Summary

**NEXUS Distributed Chat** is an enterprise-grade distributed network messaging system built from scratch in Java 21 LTS. It allows desktop clients across distinct physical computers and virtual machines to participate in shared chat rooms through **Java Remote Method Invocation (RMI)** and **bidirectional server-to-server replication**.

The project features:
1. **NEXUS CHAT Desktop Client:** A modern JavaFX messenger with dark enterprise styling, asynchronous non-blocking RMI dispatch, dynamic channel switching, and real-time message timelines.
2. **NEXUS CONTROL Admin Dashboard:** A dedicated cluster telemetry and monitoring panel displaying verified server health, peer replication links, database connectivity, observed client socket IP addresses, and operational diagnostic logs.
3. **Resilient Dual-Mode Backend:** Seamless execution with or without an active MySQL/MariaDB instance (graceful in-memory fallback ensures full demonstrability without mandatory database installation).

---

## 2. Target Demonstration Topology

```
+------------------------------------+          +------------------------------------+
|       VIRTUAL MACHINE 1            |          |       VIRTUAL MACHINE 2            |
|       NEXUS JavaFX Client 1        |          |       NEXUS JavaFX Client 2        |
+-----------------+------------------+          +-----------------+------------------+
                  |                                               |
                  | Java RMI                                      | Java RMI
                  v                                               v
+-----------------+------------------+          +-----------------+------------------+
|       PHYSICAL MACHINE A           |          |       PHYSICAL MACHINE B           |
|       Chat Server Node A           |          |       Chat Server Node B           |
|       RMI Registry: 1099           | Java RMI |       RMI Registry: 1099           |
|       Exported Service: 1100       |<-------->|       Exported Service: 1100       |
|       Replication Remote           |          |       Replication Remote           |
+-----------------+------------------+          +-----------------+------------------+
                  |                                               |
                  | JDBC (HikariCP)                               | JDBC (HikariCP)
                  v                                               v
+-----------------+------------------+          +-----------------+------------------+
|       DATABASE A                   |          |       DATABASE B                   |
|       MySQL / MariaDB on Host A    |          |       MySQL / MariaDB on Host B    |
+------------------------------------+          +------------------------------------+
```

- **Physical Computer A:** Runs Chat Server Node A and Database A.
- **Physical Computer B:** Runs Chat Server Node B and Database B.
- **VM 1:** Connected via RMI to Server Node A.
- **VM 2:** Connected via RMI to Server Node B.
- **Replication Link:** Bidirectional RMI link between Server A and Server B guarantees that messages sent by VM 1 are stored locally, replicated to Server B, stored in Database B, and delivered to VM 2 (and vice versa).

---

## 3. Multi-Module Project Structure

```
nexus_chat-RMI/
├── pom.xml                                   # Root Maven reactor project
├── .gitignore                                # Clean repository rules (no secrets/binaries)
├── README.md                                 # Complete system documentation
│
├── chat-contract/                            # Shared Contract Module (API & DTOs)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexus/chat/contract/
│       │   ├── dto/                          # Serializable DTOs crossing RMI
│       │   │   ├── ChatMessageDTO.java       # Immutable message payload (UUID, origin sequence)
│       │   │   ├── ChatRoomDTO.java          # Room metadata
│       │   │   ├── UserDTO.java              # User identity
│       │   │   ├── ClientSessionDTO.java     # Tracked session with observed IP
│       │   │   ├── ServerStatusDTO.java      # Server, peer, DB, and uptime status
│       │   │   ├── DatabaseHealthDTO.java    # Connection pool metrics without secrets
│       │   │   ├── ServerEventDTO.java       # Operational diagnostic log records
│       │   │   └── OperationResult.java      # Generic status response wrapper
│       │   ├── enums/                        # System enums
│       │   │   ├── MessageType.java          # CHAT, SYSTEM, JOIN, LEAVE
│       │   │   ├── SessionStatus.java        # ACTIVE, IDLE, STALE, DISCONNECTED
│       │   │   ├── ServerStatus.java         # ONLINE, DEGRADED, UNREACHABLE, UNKNOWN
│       │   │   ├── PeerSyncStatus.java       # SYNCED, IN_PROGRESS, FAILED, UNKNOWN
│       │   │   └── EventSeverity.java        # INFO, WARN, ERROR
│       │   └── remote/                       # Remote RMI Interfaces
│       │       ├── ChatRemote.java           # Client-to-server operations
│       │       ├── ReplicationRemote.java    # Server-to-server replication & recovery
│       │       └── MonitoringRemote.java     # Protected administrative monitoring
│       └── test/java/.../DtoSerializationTest.java
│
├── chat-server/                              # Server-Side Application Module
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/nexus/chat/server/
│       │   │   ├── ServerApplication.java    # Server bootstrap & graceful shutdown hooks
│       │   │   ├── config/                   # Config loaders (files, env, command-line)
│       │   │   │   ├── ServerConfig.java
│       │   │   │   └── DatabaseConfig.java
│       │   │   ├── database/                 # HikariCP connection pool & health checks
│       │   │   │   └── DatabaseManager.java
│       │   │   ├── repository/               # JDBC repositories & resilient in-memory fallbacks
│       │   │   │   ├── MessageRepository.java
│       │   │   │   ├── JdbcMessageRepository.java
│       │   │   │   ├── ChatRoomRepository.java
│       │   │   │   ├── JdbcChatRoomRepository.java
│       │   │   │   ├── ClientSessionRepository.java
│       │   │   │   └── InMemoryClientSessionRepository.java
│       │   │   ├── rmi/                      # RMI registry manager & client IP inspection
│       │   │   │   ├── RmiRegistryManager.java
│       │   │   │   └── RemoteClientContext.java  # RemoteServer.getClientHost() extraction
│       │   │   ├── service/                  # Core distributed services
│       │   │   │   ├── ChatServiceImpl.java      # Implements ChatRemote
│       │   │   │   ├── ReplicationServiceImpl.java# Implements ReplicationRemote
│       │   │   │   ├── MonitoringServiceImpl.java # Implements MonitoringRemote
│       │   │   │   ├── ClientSessionManager.java  # Liveness & reaper background thread
│       │   │   │   └── PeerReplicationManager.java# Asynchronous loop-free peer replication
│       │   │   └── monitoring/               # Metrics and operational diagnostic buffer
│       │   │       ├── ServerMetrics.java
│       │   │       └── SystemLogService.java
│       │   └── resources/
│       │       ├── application.properties.example # Safe template (no committed secrets)
│       │       ├── logback.xml               # Structured logging configuration
│       │       └── db/
│       │           └── db-schema.sql         # Idempotent MySQL/MariaDB DDL script
│       └── test/java/...                     # Unit tests (ServerConfigTest, ChatServiceTest)
│
├── chat-desktop/                             # JavaFX Modern Desktop Client
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/nexus/chat/desktop/
│       │   │   ├── DesktopApplication.java   # JavaFX Application lifecycle
│       │   │   ├── Launcher.java             # Standalone JavaFX launcher wrapper
│       │   │   ├── config/ClientConfig.java  # Host, port, and profile defaults
│       │   │   ├── rmi/RmiClientManager.java # Thread-safe RMI client lookups
│       │   │   ├── service/                  # Non-blocking client services
│       │   │   │   ├── ChatClientService.java    # Message polling & session management
│       │   │   │   └── AdminMonitoringService.java # Telemetry fetching for admin views
│       │   │   ├── navigation/               # Clean scene & controller routing
│       │   │   │   ├── ViewRouter.java
│       │   │   │   └── ViewId.java
│       │   │   ├── model/                    # Presentation models
│       │   │   │   ├── ChatMessageModel.java
│       │   │   │   ├── ChatRoomModel.java
│       │   │   │   ├── ConnectionState.java
│       │   │   │   ├── SessionViewModel.java
│       │   │   │   └── EventViewModel.java
│       │   │   ├── controller/               # JavaFX view controllers
│       │   │   │   ├── LoginController.java
│       │   │   │   ├── ChatController.java
│       │   │   │   ├── AdminController.java
│       │   │   │   └── SettingsController.java
│       │   │   └── util/FxAsync.java         # Worker thread pool with Platform.runLater
│       │   └── resources/com/nexus/chat/desktop/
│       │       ├── fxml/                     # Structured UI layouts
│       │       │   ├── login-view.fxml       # Server selection & profile login
│       │       │   ├── chat-view.fxml        # Timeline, channels, composer
│       │       │   ├── admin-view.fxml       # KPI cards, sessions table, logs table
│       │       │   └── settings-view.fxml    # Connection & architecture info
│       │       ├── css/                      # CSS design tokens & styling
│       │       │   ├── design-system.css     # Dark enterprise theme tokens
│       │       │   ├── login.css
│       │       │   ├── chat.css
│       │       │   └── admin.css
│       │       └── logback.xml
│       └── test/java/.../PresentationModelTest.java
│
├── deployment/                               # Production & demo deployment profiles
│   ├── node-a/application.properties.example
│   ├── node-b/application.properties.example
│   └── client/client.properties.example
│
└── scripts/                                  # PowerShell execution helpers
    ├── run-node-a.ps1                        # Run Server A (port 1099)
    ├── run-node-b.ps1                        # Run Server B (port 2099)
    └── run-client.ps1                        # Run JavaFX Client via Maven
```

---

## 4. Technical Baseline & Requirements

| Component | Verified Version | Purpose |
|---|---|---|
| **JDK** | Java 21 LTS (`21.0.4+7`) | Core runtime baseline |
| **Build Tool** | Apache Maven 3.9+ | Dependency management & multi-module reactor |
| **GUI Framework** | OpenJFX 21.0.2 | Desktop client user interface |
| **Networking** | Java RMI (JRMP) | Client-to-server and peer-to-peer communication |
| **Database** | MySQL 8.0+ / MariaDB 10.5+ | Relational message and session persistence |
| **Connection Pool** | HikariCP 5.1.0 | High-performance JDBC connection pooling |
| **Logging** | SLF4J 2.0.12 + Logback 1.5.3 | Structured operational logging |
| **Testing** | JUnit Jupiter 5.10.2 + AssertJ 3.25.3 | Automated unit and integration testing |

---

## 5. Build Verification

Build all three modules, run unit tests, and assemble artifacts with:

```bash
mvn clean package
```

To install artifacts into your local Maven cache (`~/.m2/repository`):

```bash
mvn install -DskipTests
```

---

## 6. How to Run the System

### 6.1. Running on a Single Machine (Development & Testing)

You can run both server nodes and two clients on the same machine by assigning distinct ports:

#### Step 1: Start Server Node A (Port 1099)
```powershell
./scripts/run-node-a.ps1
```
*Server A binds to RMI Registry 1099 and exported service port 1100, targeting peer Server B at 2099.*

#### Step 2: Start Server Node B (Port 2099)
Open a second terminal:
```powershell
./scripts/run-node-b.ps1
```
*Server B binds to RMI Registry 2099 and exported service port 2100, targeting peer Server A at 1099.*

#### Step 3: Launch JavaFX Client 1
Open a third terminal:
```powershell
./scripts/run-client.ps1
```
- On the Login screen, choose **"Server A (Local Node A: 1099)"**.
- Enter display name `Alice` and click **"Connect & Enter Chat"**.

#### Step 4: Launch JavaFX Client 2
Open a fourth terminal:
```powershell
mvn -pl chat-desktop javafx:run
```
- On the Login screen, choose **"Server B (Local Node B: 2099)"**.
- Enter display name `Bob` and click **"Connect & Enter Chat"**.

#### Result:
Messages typed by `Alice` on Server A replicate to Server B and instantly appear in `Bob`'s timeline. Replies from `Bob` on Server B replicate back to Server A and appear in `Alice`'s timeline.

---

### 6.2. Running on Two Physical Computers & Virtual Machines (Demonstration Topology)

When running across separate machines on a local network (LAN):

1. **Configure Advertised Host on Server A (Computer A, e.g. 192.168.1.101):**
   ```bash
   java -Djava.rmi.server.hostname=192.168.1.101 -jar chat-server-1.0.0-SNAPSHOT.jar \
       --server.id=server-a \
       --server.advertised.host=192.168.1.101 \
       --server.rmi.registry.port=1099 \
       --server.rmi.service.port=1100 \
       --server.peer.id=server-b \
       --server.peer.host=192.168.1.102 \
       --server.peer.registry.port=1099
   ```

2. **Configure Advertised Host on Server B (Computer B, e.g. 192.168.1.102):**
   ```bash
   java -Djava.rmi.server.hostname=192.168.1.102 -jar chat-server-1.0.0-SNAPSHOT.jar \
       --server.id=server-b \
       --server.advertised.host=192.168.1.102 \
       --server.rmi.registry.port=1099 \
       --server.rmi.service.port=1100 \
       --server.peer.id=server-a \
       --server.peer.host=192.168.1.101 \
       --server.peer.registry.port=1099
   ```

3. **VirtualBox / VMware Configuration:**
   - Configure VM network adapters as **Bridged Adapter** so each VM receives an IP address on the same subnet.
   - On VM 1, start the desktop client and enter Server A's LAN IP (`192.168.1.101:1099`).
   - On VM 2, start the desktop client and enter Server B's LAN IP (`192.168.1.102:1099`).

4. **Firewall Verification:**
   Ensure inbound TCP ports `1099` (Registry) and `1100` (Service) are allowed in Windows Defender Firewall or `iptables`.

---

## 7. Database Setup & Resilient Fallback

### 7.1. Database Schema Execution
The DDL script is located at:
`chat-server/src/main/resources/db/db-schema.sql`

To initialize MySQL / MariaDB on Computer A or B:
```bash
mysql -u root -p < chat-server/src/main/resources/db/db-schema.sql
```

### 7.2. Resilient In-Memory Fallback
- If MySQL is offline or `database.enabled=false`, the server **does not crash**.
- It logs a clear notification (`Database persistence is disabled / offline. Running in resilient in-memory mode.`) and marks database status as `IN-MEMORY FALLBACK`.
- Repositories automatically buffer messages and sessions in memory, allowing students and instructors to verify RMI replication and client UI even on workstations where MySQL is not installed.

---

## 8. Network & Distributed System Implementation Details

### 8.1. Loop-Free Replication Rules
1. When a client sends a message to Server A, Server A tags it with `originServerId = "server-a"` and assigns an atomic `originSequence`.
2. Server A persists the message locally and dispatches it asynchronously to Server B via `ReplicationRemote.replicateMessage(...)`.
3. When Server B receives the message, it validates that `messageId` does not already exist (idempotency check) and saves it locally.
4. **Loop Protection Guarantee:** Server B **never** re-replicates foreign messages (`!message.getOriginServerId().equals(this.serverId)`). This strictly prevents ping-pong replication loops.

### 8.2. Authentic Server-Side Client IP Observation
In Java RMI, client-reported IP addresses cannot be trusted. In NEXUS Chat:
- The server calls `RemoteServer.getClientHost()` within the incoming RMI invocation thread (`RemoteClientContext`).
- The actual socket IP address is captured at connection registration time.
- If called outside RMI dispatch (e.g. in local unit tests), it safely returns `"127.0.0.1"` rather than failing.

### 8.3. Honest UI / UX Guarantee
In accordance with system specifications:
- No simulated data, fake IP addresses, or fabricated metrics are presented.
- When the client or admin dashboard is disconnected, it displays clear `DISCONNECTED`, `NOT CONNECTED`, or `UNKNOWN` badges.
- Data tables display clear empty placeholders until live telemetry is retrieved from an authorized server endpoint.

---

## 9. Handoff Plan for Subsequent Phases

| Phase | Objective | Scope & Next Steps |
|---|---|---|
| **Phase 1** | Build & Core Architecture Foundation | **COMPLETED:** Multi-module Maven setup, shared DTOs & remote contracts, server bootstrap, database schema & HikariCP pool, in-memory resilient fallback, JavaFX client with dark theme tokens, FXML layouts, RMI service exporter, loop prevention, and unit tests. |
| **Phase 2** | Durable Replication & Recovery Queue | Add transactional outbox table (`replication_outbox`) and exponential backoff retry worker for when peer servers temporarily disconnect. |
| **Phase 3** | Authentication & TLS (Optional) | Extend username login with salted Argon2/PBKDF2 password hashing and secure RMI custom socket factories (SSL/TLS). |
| **Phase 4** | Advanced Admin Controls | Add remote session termination (`revokeSession`), message search by UUID, and historical replication latency histograms. |
