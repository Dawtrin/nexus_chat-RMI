
# NEXUS Distributed Chat — Tài liệu kiến trúc hệ thống

## 1. Mục tiêu kiến trúc

NEXUS Distributed Chat là ứng dụng chat desktop phân tán, dùng Java 21, Maven multi-module, Java RMI, JavaFX/FXML/CSS và MySQL/MariaDB.

Mục tiêu triển khai:

- Hai server chạy trên hai máy vật lý khác nhau.
- Mỗi server sử dụng database riêng.
- Client có thể kết nối tới Node A hoặc Node B.
- Message gửi qua A có thể được nhận bởi client đang kết nối B, và ngược lại.
- Peer offline tạm thời không được làm mất durable outbox đã commit.
- Sau khi peer trở lại, hệ thống retry/catch-up để đồng bộ dữ liệu còn thiếu.
- Admin Dashboard hiển thị trạng thái thực tế của server, DB, peer và replication.

Hệ thống dùng replication bất đồng bộ. Không tuyên bố strong consistency hoặc distributed transaction giữa hai database.

## 2. Các module

### `chat-contract`

Chứa hợp đồng giao tiếp dùng chung:

- DTO.
- Enum.
- Exception.
- Remote interfaces.
- Các kiểu kết quả operation.

Module này không nên phụ thuộc vào `chat-server` hoặc `chat-desktop`.

### `chat-server`

Chứa:

- RMI bootstrap và remote services.
- Business services.
- Session management.
- JDBC repositories.
- Database configuration.
- Replication/outbox/catch-up.
- Monitoring service.
- Logging và lifecycle management.

Server chỉ ghi vào database cục bộ của nó. Dữ liệu giữa hai server được chuyển qua RMI.

### `chat-desktop`

Chứa:

- JavaFX application.
- FXML views.
- CSS.
- Controllers.
- RMI client manager.
- Chat client service.
- Polling và reconnect.
- Admin monitoring UI/service theo cách tổ chức hiện có.

Remote calls không được chặn JavaFX Application Thread. Kết quả cần được chuyển về JavaFX thread trước khi cập nhật UI.

## 3. Module dependency

```text
                  chat-contract
                   ^         ^
                   |         |
             chat-server  chat-desktop
```

- `chat-server` phụ thuộc `chat-contract`.
- `chat-desktop` phụ thuộc `chat-contract`.
- `chat-contract` không phụ thuộc hai module ứng dụng.
- JDBC driver và cấu hình database thuộc server.
- JavaFX/FXML/CSS thuộc desktop module.

## 4. Runtime topology

```text
       Client A / VM                         Client B / VM
             |                                     |
             | Java RMI                            | Java RMI
             v                                     v
  +-------------------------+          +-------------------------+
  | Physical Host A         |          | Physical Host B         |
  | Node A                  |<-------->| Node B                  |
  | ChatRemote              |   RMI    | ChatRemote              |
  | ReplicationRemote       |          | ReplicationRemote       |
  | MonitoringRemote        |          | MonitoringRemote        |
  | Local JDBC repository   |          | Local JDBC repository   |
  +------------+------------+          +------------+------------+
               |                                    |
               v                                    v
        +--------------+                     +--------------+
        | Database A   |                     | Database B   |
        +--------------+                     +--------------+
```

Client không truy cập trực tiếp database. Node A không ghi trực tiếp vào Database B. Node A gửi message qua replication API, sau đó Node B lưu bản sao vào database cục bộ.

## 5. RMI contracts

Các interface được ghi nhận trong source:

- `ChatRemote`: giao tiếp client-server.
- `ReplicationRemote`: giao tiếp server-server.
- `MonitoringRemote`: cung cấp dữ liệu monitoring cho admin.

Các DTO cần được kiểm tra trong source hiện tại, bao gồm những kiểu dùng cho message, session, server status, database health, event và operation result.

Thay đổi remote interface/DTO phải cân nhắc:
- Serialization.
- Tính tương thích giữa các phiên bản client/server.
- Xử lý `RemoteException`.
- Thời gian chờ và lỗi kết nối.
- Tính tương thích của các node A/B.

## 6. Luồng gửi message mục tiêu

1. Client gọi `ChatRemote.sendMessage(...)`.
2. Server xác thực session.
3. Server kiểm tra room và nội dung.
4. Server tạo message ID duy nhất và origin sequence hợp lệ.
5. Server lưu message vào database cục bộ.
6. Server ghi outbox item trong cùng transaction khi dùng database durable.
7. Transaction commit.
8. Server trả kết quả phản ánh persistence mode thực tế.
9. Outbox worker gửi batch tới `ReplicationRemote` của peer.
10. Peer kiểm tra idempotency và lưu message vào database cục bộ.
11. Client trên peer nhận message thông qua cơ chế cập nhật/polling hiện có.
12. Message nhận từ peer không được enqueue để gửi ngược về origin.

**Điểm cần sửa theo lần rà soát trước:** thao tác lưu message và enqueue outbox đang được gọi riêng. Cần xác minh lại source hiện tại và đảm bảo atomic transaction trước khi tuyên bố transactional outbox đầy đủ.

## 7. Message ID và idempotency

Mỗi message cần có ID duy nhất toàn hệ thống. UUID là lựa chọn phù hợp cho định danh message nếu tương thích với DTO và schema hiện tại.

Receiver phải xử lý message gửi lại theo cách idempotent:

- Nếu message ID chưa tồn tại, lưu message.
- Nếu message ID đã tồn tại, không tạo thêm bản sao logic.
- Unique constraint ở database phải bảo vệ tính duy nhất khi có request đồng thời.
- Kết quả duplicate cần được xử lý như tình huống bình thường, không làm worker tạo vòng retry vô hạn.

Không chỉ dựa vào `if exists` trong Java rồi insert, vì hai request có thể chạy đồng thời.

## 8. Origin sequence và local sequence

Phải phân biệt hai khái niệm:

- `local_sequence`: thứ tự bản ghi trong database cục bộ.
- `origin_sequence`: thứ tự message theo server đã tạo message ban đầu.

Không sử dụng cursor của một miền sequence để truy vấn miền sequence khác.

Sequence dùng cho catch-up phải có ý nghĩa ổn định, không bị trùng hoặc reset ngoài ý muốn sau restart. Nếu một server cấp origin sequence bằng bộ đếm trong bộ nhớ, phải sửa cơ chế đó trước khi dựa vào sequence để đồng bộ bù bền vững.

## 9. Replication outbox

Outbox lưu các message cần gửi sang peer.

Vòng đời mục tiêu:

```text
Message accepted
       |
       v
Local message + outbox committed atomically
       |
       v
     PENDING
       |
       v
  Send to peer
    /      \
success    failure
  |           |
  v           v
DELIVERED  retryable failure
               |
               v
           retry later
```

Tên trạng thái chính xác phải khớp với enum/schema trong source hiện tại.

Outbox cần có khả năng:
- Tồn tại sau process restart khi dùng DB durable.
- Retry khi peer offline.
- Ghi số lần retry và lỗi gần nhất nếu schema hỗ trợ.
- Chỉ chuyển sang delivered khi peer đã xác nhận kết quả phù hợp.
- Không gây duplicate khi gửi lại.
- Không gửi ngược message nhận từ peer.
- Hiển thị pending/failure chính xác trên dashboard.

In-memory outbox không thể được coi là durable sau khi process dừng.

## 10. Catch-up và checkpoint

Catch-up giúp một node nhận các message bị bỏ lỡ khi peer offline.

Thiết kế mục tiêu:

1. Đọc checkpoint của origin.
2. Truy vấn batch tiếp theo theo sequence tăng dần.
3. Lưu message theo cơ chế idempotent.
4. Commit batch.
5. Cập nhật checkpoint sau khi batch đã được lưu thành công.
6. Tiếp tục tới khi không còn message.
7. Nếu có lỗi, tiếp tục từ checkpoint đã commit.

Lần rà soát trước ghi nhận phương thức catch-up truy vấn một batch giới hạn 100 và giữ cursor trong bộ nhớ. Cần xác minh lại source hiện tại và sửa nếu hành vi này vẫn tồn tại.

## 11. Client reconnect và polling

Client cần phân biệt các trạng thái thực:

- `DISCONNECTED`
- `CONNECTING`
- `CONNECTED`
- `RECONNECTING`
- `FAILED`, nếu được hỗ trợ

Khi mất kết nối:
- Không chỉ đổi nhãn UI.
- Thực hiện retry thật sự.
- Lookup lại remote service.
- Đăng ký session mới khi cần.
- Tải lại rooms/history.
- Khởi động đúng một polling worker.
- Dừng task khi đóng app.
- Deduplicate message theo ID.

Mọi network call có thể block phải chạy ngoài JavaFX Application Thread.

## 12. Session và room authorization

Server phải kiểm tra session trước khi thực hiện API cần session.

Các API đọc dữ liệu cũng phải được bảo vệ, không chỉ API gửi message. Room phải tồn tại và user phải có quyền truy cập theo chính sách ứng dụng.

UI có thể hỗ trợ trải nghiệm người dùng nhưng không thay thế validation ở server.

## 13. Monitoring

Dashboard nên hiển thị dữ liệu backend thực:

- Server ID/status.
- Uptime nếu có.
- Database health.
- Persistence mode.
- Peer reachability.
- Replication state.
- Pending outbox.
- Last successful replication nếu backend hỗ trợ.
- Active sessions.
- Observed remote host/IP.
- Operational logs.

Nếu không lấy được dữ liệu, hiển thị `UNKNOWN`, `DEGRADED` hoặc trạng thái lỗi phù hợp. Không hiển thị `ONLINE`/`SYNCED` mặc định để che lỗi.

Host được quan sát từ RMI không nhất thiết luôn là địa chỉ thiết bị vật lý cuối cùng trong topology có NAT hoặc proxy.

## 14. Giới hạn cần công khai

- Replication bất đồng bộ có thể trễ khi peer offline.
- Không tuyên bố strong consistency giữa hai DB.
- Không tuyên bố distributed transaction giữa hai server.
- Không tuyên bố không mất dữ liệu khi cả hai host/database cùng hỏng.
- TLS RMI và cơ chế xác thực mạnh hơn chỉ được coi là đã có khi source thực sự triển khai và test.
- Build thành công không chứng minh deployment đa máy thành công.

## 15. Điều kiện để coi kiến trúc đã được xác minh

- Atomic message + outbox transaction đã được test.
- Checkpoint bền vững và catch-up phân trang đã được test.
- Replication A → B và B → A đã được test.
- Client reconnect đã được test.
- Session/room authorization đã được test.
- Hai node dùng hai database riêng.
- RMI advertised host/port hoạt động qua LAN.
- Dashboard phản ánh trạng thái backend thật.
- Acceptance test hai máy vật lý đã chạy và có bằng chứng.
