
# NEXUS Distributed Chat — Báo cáo rà soát mã nguồn

**Phiên bản:** 1.0  
**Ngày:** 29-10-2026  
**Phạm vi:** Source code NEXUS Distributed Chat, ba tài liệu thiết kế gốc và báo cáo triển khai Phase 2 hiện có.

## 1. Mục đích

Tài liệu này tổng hợp các vấn đề kỹ thuật đã được ghi nhận trong quá trình rà soát source trước đó. Đây là đầu vào cho việc audit lại và sửa code, không phải chứng nhận rằng mọi lỗi đã được tái hiện bằng test.

Claude phải đọc lại source thực tế trước khi thay đổi. Nếu source khác với nội dung tài liệu này, hãy ghi rõ sai khác và sử dụng source hiện tại làm căn cứ để xác minh.

## 2. Quy ước trạng thái

- **[GHI NHẬN TỪ SOURCE]**: vấn đề đã được quan sát trong source ở lần rà soát trước.
- **[CẦN XÁC MINH]**: cần kiểm tra lại source hiện tại hoặc chạy test để kết luận.
- **[YÊU CẦU SỬA]**: thay đổi cần triển khai.
- **[CHƯA ĐƯỢC NGHIỆM THU]**: chưa có bằng chứng đủ để kết luận tính năng hoạt động trong môi trường mục tiêu.

Không suy luận rằng một tính năng hoạt động đầy đủ chỉ vì source có class, method hoặc database table mang tên tương ứng.

## 3. Kiến trúc tổng quan

Theo source và tài liệu đã cung cấp, dự án được tổ chức thành ba Maven module:

- `chat-contract`: DTO, enum, exception và các RMI interfaces dùng chung.
- `chat-server`: server bootstrap, service nghiệp vụ, JDBC repositories, session management, replication và monitoring.
- `chat-desktop`: JavaFX client/admin UI, controllers, RMI client và các service phục vụ giao diện.

Các RMI interface quan trọng gồm:

- `ChatRemote`: giao tiếp giữa client và server.
- `ReplicationRemote`: giao tiếp replication/catch-up giữa server.
- `MonitoringRemote`: cung cấp dữ liệu monitoring cho admin.

Mục tiêu triển khai cuối cùng không phải chỉ chạy trên localhost. Hệ thống cần chạy trên hai máy vật lý khác nhau, dùng hai database riêng và giao tiếp qua mạng LAN.

## 4. Các phát hiện cần ưu tiên

### AUD-01 — Lưu message và enqueue outbox chưa nằm trong cùng transaction

**Mức độ:** Critical đối với yêu cầu bảo đảm replication sau khi process gặp sự cố.

**[GHI NHẬN TỪ SOURCE]** Lần rà soát trước ghi nhận `ChatServiceImpl.sendMessage()` gọi lưu message qua `messageRepository.save(...)`, sau đó gọi `replicationManager.submitReplication(...)`. Việc enqueue được thực hiện riêng trong `PeerReplicationManager`.

**Vấn đề:** Nếu process dừng sau khi message đã được lưu nhưng trước khi outbox được ghi, message có thể tồn tại trong database cục bộ mà không có outbox để retry.

**YÊU CẦU SỬA:**

1. Đối với MySQL/MariaDB, lưu message và tạo outbox item trong cùng transaction JDBC.
2. Nếu một thao tác insert thất bại, rollback toàn bộ transaction.
3. Chỉ đánh thức worker gửi replication sau khi transaction commit.
4. Không thực hiện RMI call tới peer bên trong transaction ghi database.
5. Bổ sung test xác minh message và outbox được commit hoặc rollback cùng nhau.
6. Xác minh message và outbox còn tồn tại sau khi server khởi động lại.

Nếu repository hiện tại không hỗ trợ dùng chung connection/transaction, hãy thiết kế lại ranh giới transaction phù hợp thay vì mở hai transaction độc lập.

### AUD-02 — Catch-up có nguy cơ không lấy hết dữ liệu

**Mức độ:** High.

**[GHI NHẬN TỪ SOURCE]** Lần rà soát trước ghi nhận `PeerReplicationManager.performCatchUpSync()` dùng cursor `AtomicLong lastAcknowledgedPeerOriginSeq`, khởi tạo bằng `0`, và gọi `getMessagesAfter(..., 100)` một lần. Cursor này được lưu trong bộ nhớ.

**Vấn đề cần xác minh:**

- Một lần truy vấn tối đa 100 message có thể không đủ khi có hơn 100 message chưa đồng bộ.
- Cursor trong bộ nhớ không tự tồn tại sau restart.
- Nếu khởi động lại từ cursor 0, hệ thống có thể quét lại nhiều dữ liệu.
- Nếu cursor được cập nhật sai thời điểm, có nguy cơ bỏ qua dữ liệu.
- `origin_sequence` và `local_sequence` có thể thuộc hai miền sequence khác nhau.

**YÊU CẦU SỬA:**

1. Phân trang đến khi không còn dữ liệu cần đồng bộ.
2. Lưu checkpoint bền vững theo origin/peer trong database.
3. Chỉ cập nhật checkpoint sau khi batch tương ứng được lưu thành công.
4. Khi có lỗi giữa batch, lần chạy sau phải tiếp tục từ checkpoint đã commit.
5. Xác minh sequence được tạo ổn định, không bị trùng hoặc reset sai sau restart.
6. Bổ sung test với ít nhất 250 message, lỗi giữa batch và restart giữa catch-up.

### AUD-03 — Trạng thái RECONNECTING cần được kiểm chứng

**Mức độ:** High.

**[GHI NHẬN TỪ SOURCE]** Lần rà soát trước ghi nhận `ChatClientService` có nhánh xử lý lỗi polling đặt trạng thái `RECONNECTING` và hiển thị thông báo thử kết nối lại. Chưa thấy quy trình reconnect đầy đủ trong nhánh đó.

**Vấn đề:** Cập nhật nhãn giao diện sang `RECONNECTING` không tự động làm client kết nối lại.

**YÊU CẦU SỬA:**

- Tạo retry coordinator thực sự.
- Lưu cấu hình host, port và thông tin phiên đăng nhập cần thiết để kết nối lại.
- Lookup lại RMI remote object khi retry.
- Đăng ký session mới khi session cũ không còn hợp lệ.
- Tải lại danh sách room và lịch sử phù hợp.
- Khởi động đúng một polling worker sau khi reconnect thành công.
- Dùng backoff để tránh retry liên tục.
- Hủy retry khi người dùng chủ động disconnect hoặc đóng ứng dụng.
- Chống tạo nhiều executor, polling task hoặc session trùng.
- Thêm test cho trường hợp server tắt rồi bật lại.

### AUD-04 — Xác thực session cần nhất quán giữa các API

**Mức độ:** High.

**[GHI NHẬN TỪ SOURCE]** Lần rà soát trước ghi nhận `sendMessage()` và `joinRoom()` gọi `sessionManager.getValidSession(sessionId)`, trong khi `getMessages()`, `getRooms()` và `heartbeat()` có cách xử lý session khác.

**Vấn đề:** Các API đọc dữ liệu hoặc heartbeat có thể xử lý session không hợp lệ khác với các API gửi tin nhắn.

**YÊU CẦU SỬA:**

1. Xác thực session ở mọi API yêu cầu session.
2. Từ chối session null, không tồn tại, hết hạn hoặc đã disconnect.
3. Không trả lịch sử tin nhắn khi session không hợp lệ.
4. Không làm mới heartbeat cho session không hợp lệ.
5. Xác minh room tồn tại và quyền truy cập trước khi đọc/gửi message.
6. Chuẩn hóa kết quả lỗi để client xử lý nhất quán.
7. Bổ sung test cho các trạng thái session và room nói trên.

### AUD-05 — Persistence mode phải phản ánh kết quả ghi thực tế

**Mức độ:** High.

**[GHI NHẬN TỪ SOURCE]** Dự án có JDBC repositories và đường fallback in-memory. Source cũng có thông tin persistence mode, trong đó có các trạng thái như `MYSQL_DURABLE` và `IN_MEMORY_FALLBACK`.

**[CẦN XÁC MINH]** Cần kiểm tra trường hợp database pool vẫn báo connected nhưng một câu lệnh ghi cụ thể thất bại. Trạng thái kết nối tổng thể không đủ để chứng minh thao tác ghi hiện tại thành công.

**YÊU CẦU SỬA:**

- Kết quả mỗi thao tác ghi phải phản ánh nơi dữ liệu thực sự được lưu.
- Không báo `MYSQL_DURABLE` cho message nếu thao tác ghi database thất bại.
- Nếu fallback in-memory được cho phép, phải thông báo rõ rằng dữ liệu RAM có thể mất khi process dừng.
- Dashboard phải thể hiện trạng thái degraded/fallback một cách trung thực.
- Không dùng dữ liệu giả để che lỗi database hoặc replication.

### AUD-06 — Kiểm tra room trước khi lưu message

**Mức độ:** Medium/High.

**[GHI NHẬN TỪ SOURCE]** Lần rà soát trước ghi nhận `sendMessage()` kiểm tra session và nội dung nhưng chưa thấy kiểm tra room tồn tại trước khi lưu.

**Vấn đề:** Hành vi giữa JDBC và in-memory repository có thể khác nhau nếu room không tồn tại.

**YÊU CẦU SỬA:**

- Kiểm tra room ID.
- Kiểm tra room tồn tại.
- Kiểm tra membership/authorization nếu hệ thống yêu cầu.
- Trả lỗi rõ ràng nếu room không hợp lệ.
- Test hành vi giống nhau giữa các persistence mode.

## 5. Những phần đã có trong source nhưng chưa mặc nhiên được nghiệm thu

Lần rà soát trước ghi nhận source có các thành phần tương ứng với:

- RMI contracts và DTO dùng chung.
- Message ID, origin server ID và sequence.
- JDBC message repository.
- Replication outbox repository.
- Replication service có cơ chế kiểm tra duplicate.
- Client session repository/manager.
- Thu thập host quan sát được từ RMI.
- Tiện ích chạy tác vụ ngoài JavaFX Application Thread.
- Client polling và trạng thái kết nối.
- Admin monitoring service/controller.
- Script chạy Node A và Node B.

Các thành phần trên cần được xác minh lại trong source hiện tại và kiểm thử theo `TESTING_AND_ACCEPTANCE.md`.

## 6. Các mục chưa có bằng chứng nghiệm thu

Không được coi các mục sau là đã hoàn tất chỉ dựa trên báo cáo code:

1. Hai server chạy trên hai máy vật lý khác nhau.
2. Hai server sử dụng hai database độc lập.
3. Replication A → B và B → A hoạt động thực tế.
4. Outbox không mất dữ liệu sau process restart.
5. Catch-up lấy hết dữ liệu khi có hơn 100 message.
6. Client reconnect thực sự sau khi server được khởi động lại.
7. Session và room authorization hoạt động nhất quán.
8. Admin token bảo vệ tất cả monitoring endpoints.
9. Dashboard phản ánh chính xác DB health, peer health và outbox.
10. RMI hoạt động với IP LAN, firewall và port cấu hình thực tế.

## 7. Thứ tự ưu tiên đề xuất

1. AUD-01: atomic message + outbox transaction.
2. AUD-02: checkpoint bền vững và catch-up phân trang.
3. AUD-03: client reconnect thực sự.
4. AUD-04 và AUD-06: session/room validation.
5. AUD-05: persistence mode trung thực.
6. Monitoring/admin và deployment đa máy.
7. Hardening bảo mật, logging và graceful shutdown.

## 8. Điều kiện đóng audit

Một lỗi chỉ được đánh dấu đã sửa khi có:

- Thay đổi code cụ thể.
- Test phù hợp.
- Kết quả test thực tế.
- Ghi nhận các test chưa chạy.
- Hướng dẫn xác minh thủ công nếu cần môi trường bên ngoài.

Không được đánh dấu `Two-host verified` nếu chưa thực sự kiểm thử trên hai máy vật lý.
