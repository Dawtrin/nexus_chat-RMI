
# NEXUS Distributed Chat — Lộ trình triển khai và sửa lỗi

## 1. Mục tiêu

Hoàn thiện source NEXUS Distributed Chat hiện tại theo ba tài liệu thiết kế gốc, không tạo lại project từ đầu.

Công nghệ và kiến trúc mục tiêu:

- Java 21.
- Maven multi-module.
- Java RMI cho client-server và server-server.
- JavaFX/FXML/CSS cho desktop UI.
- JDBC với MySQL/MariaDB.
- Hai server chạy trên hai máy vật lý khác nhau.
- Mỗi server có database riêng.
- Replication hai chiều, idempotency, retry và catch-up.
- Admin Dashboard hiển thị trạng thái thực tế.

## 2. Quy tắc bắt buộc khi sửa code

1. Đọc toàn bộ source trước khi chỉnh sửa.
2. Đọc ba tài liệu gốc và các tài liệu bổ sung.
3. Nếu tài liệu không khớp với source, ghi lại khác biệt trước khi thay đổi.
4. Không viết lại toàn bộ project nếu có thể sửa có mục tiêu.
5. Không tự ý xóa chức năng hoặc phá RMI contracts mà không có lý do kỹ thuật.
6. Không hardcode IP LAN, database password hoặc token triển khai.
7. Không sử dụng `127.0.0.1` làm peer address khi triển khai trên hai máy vật lý.
8. Mỗi phase phải có build/test và báo cáo kết quả.
9. Không báo test pass nếu chưa chạy test.
10. Không báo đã kiểm thử đa máy nếu chỉ chạy localhost.

## 3. Phase 0 — Xác lập baseline

### Công việc

- Kiểm tra `java -version`.
- Kiểm tra `mvn -version`.
- Chạy `mvn clean package`.
- Ghi lại số test chạy, passed, failed, errors và skipped.
- Kiểm tra cấu trúc module và các script chạy.
- Kiểm tra config mẫu, secrets và `.gitignore`.
- Xác định chính xác lệnh khởi chạy server/client từ source hiện tại.

### Điều kiện hoàn thành

- Có kết quả build hiện tại.
- Các lỗi có sẵn trước khi sửa được ghi lại.
- Không dùng thư mục `target/` cũ làm bằng chứng cho build mới.

## 4. Phase 1 — Atomic persistence và transactional outbox

### Vấn đề

Lần rà soát trước ghi nhận thao tác lưu message và enqueue outbox được gọi riêng. Cần xác minh lại trong source hiện tại trước khi sửa.

### Mục tiêu

Khi sử dụng MySQL/MariaDB:

1. Validate session, room và nội dung.
2. Sinh message ID duy nhất và origin sequence hợp lệ.
3. Mở transaction database.
4. Lưu message cục bộ.
5. Ghi outbox item trong cùng transaction.
6. Commit transaction.
7. Sau commit mới đánh thức worker replication và trả kết quả phù hợp.

Nếu lưu message hoặc outbox thất bại, rollback toàn bộ transaction.

Không gọi remote peer bên trong transaction DB.

### Yêu cầu triển khai

- Có ranh giới transaction rõ ràng.
- Hai thao tác dùng cùng transaction/connection khi cần atomicity.
- Unique constraint bảo vệ message ID.
- Sequence không bị trùng do nhiều thread hoặc restart.
- In-memory fallback phải được mô tả là không durable.
- Lỗi ghi DB không được báo thành công durable.
- Không đánh dấu outbox đã giao trước khi peer xác nhận.

### Test bắt buộc

- Message và outbox commit thành công.
- Lỗi insert message.
- Lỗi insert outbox.
- Rollback khi một bước thất bại.
- Duplicate message ID.
- Restart khi outbox còn pending.
- Persistence mode phản ánh kết quả ghi thực tế.

## 5. Phase 2 — Durable checkpoint và catch-up phân trang

### Mục tiêu

Catch-up phải xử lý hết các message chưa đồng bộ, không phụ thuộc vào một lần truy vấn giới hạn 100 bản ghi.

### Yêu cầu triển khai

- Dùng sequence có ý nghĩa rõ ràng theo từng origin.
- Phân biệt `local_sequence` và `origin_sequence`.
- Lưu checkpoint bền vững theo origin/peer.
- Truy vấn batch theo thứ tự ổn định.
- Lặp đến khi không còn dữ liệu.
- Chỉ commit checkpoint sau khi batch được lưu thành công.
- Khi lỗi giữa batch, tiếp tục từ checkpoint đã commit.
- Khi nhận message từ peer, không enqueue message đó để gửi ngược.
- Bảo vệ duplicate ở repository/database, không chỉ bằng kiểm tra Java trước khi insert.

### Test bắt buộc

- 0 message.
- 1 message.
- 100 message.
- 250 message trở lên.
- Duplicate trong batch.
- Database lỗi giữa batch.
- Restart giữa catch-up.
- Peer restart nhiều lần.
- Replication hai chiều.
- Không có vòng lặp replication.

## 6. Phase 3 — Client reconnect thực sự

### State machine đề xuất

`DISCONNECTED → CONNECTING → CONNECTED → RECONNECTING → CONNECTED`

Nếu không thể kết nối sau chính sách retry, trạng thái phải thể hiện lỗi thực tế, không giả vờ đang kết nối.

### Yêu cầu triển khai

- Lưu cấu hình server đang kết nối.
- Có một retry coordinator duy nhất.
- Sử dụng exponential backoff hoặc chiến lược retry có giới hạn.
- Lookup lại RMI service khi kết nối bị mất.
- Đăng ký session mới nếu session cũ không còn hợp lệ.
- Tải lại rooms/history cần thiết.
- Khởi động một polling worker sau reconnect.
- Hủy retry khi người dùng disconnect hoặc đóng app.
- Chống tạo nhiều thread hoặc polling task.
- Deduplicate tin nhắn ở UI bằng message ID.
- Không block JavaFX Application Thread.

### Test bắt buộc

- Server dừng trong khi client đang polling.
- Server phục hồi trước lần retry đầu.
- Server phục hồi sau nhiều lần retry.
- Host/port sai.
- Session hết hạn.
- Người dùng disconnect trong lúc retry.
- Reconnect nhiều lần nhưng không sinh polling worker trùng.

## 7. Phase 4 — Session và room authorization

### Yêu cầu triển khai

- Tất cả API yêu cầu session đều validate session.
- `getMessages()` không trả dữ liệu khi session invalid.
- `getRooms()` xác thực session.
- `heartbeat()` không làm mới session không hợp lệ.
- `sendMessage()` kiểm tra room tồn tại.
- Kiểm tra quyền truy cập room nếu ứng dụng có chính sách membership.
- Chuẩn hóa lỗi để client xử lý thống nhất.
- Đảm bảo hành vi nhất quán giữa JDBC và in-memory repository.

### Test bắt buộc

- Session null.
- Session không tồn tại.
- Session expired.
- Session disconnected.
- Room không tồn tại.
- User không có quyền truy cập room.
- Nội dung rỗng hoặc vượt giới hạn.
- Request đồng thời trong lúc disconnect.

## 8. Phase 5 — Monitoring và Admin Dashboard

Dashboard phải lấy dữ liệu thật từ monitoring service.

### Thông tin cần hiển thị

- Server ID và trạng thái.
- Uptime nếu backend cung cấp.
- Database health.
- Persistence mode.
- Peer reachability.
- Replication state.
- Pending outbox.
- Thời điểm replication thành công gần nhất nếu backend hỗ trợ.
- Active sessions.
- IP/host mà server quan sát được.
- Event logs và lỗi gần nhất.

### Yêu cầu

- Không hardcode trạng thái `ONLINE`, `SYNCED` hoặc `HEALTHY`.
- Khi không lấy được dữ liệu, hiển thị `UNKNOWN`, `DEGRADED` hoặc trạng thái tương đương.
- Hiển thị thời điểm cập nhật để nhận biết dữ liệu stale.
- Kiểm tra token trên mọi monitoring endpoint cần bảo vệ.
- Không log token/password.
- Không dùng token mặc định không an toàn khi triển khai.

### Test bắt buộc

- Token đúng.
- Token sai.
- Token bị thiếu.
- Peer offline.
- Database offline.
- Monitoring remote lỗi.
- Outbox pending.
- Dữ liệu monitoring stale.

## 9. Phase 6 — Deployment đa máy

### Yêu cầu

- Cấu hình riêng cho Node A và Node B.
- Mỗi node có ID, advertised host, registry port, service port và peer configuration rõ ràng.
- Mỗi node sử dụng database riêng.
- Server advertise địa chỉ có thể truy cập từ máy khác.
- Không sử dụng localhost làm địa chỉ peer liên máy.
- Script nhận cấu hình qua file/environment/arguments thay vì hardcode IP.
- Kiểm tra các RMI ports thực tế trước khi mở firewall.
- Tài liệu hóa DB setup, schema, port và lệnh chạy.

### Điều kiện hoàn thành

- Chạy được A và B trên hai host khác nhau.
- Client A kết nối A.
- Client B kết nối B.
- Tin nhắn đồng bộ hai chiều.
- Peer offline rồi phục hồi không làm mất durable outbox.
- Catch-up không bỏ sót message.

## 10. Phase 7 — Hardening và hoàn thiện

- Thay token mặc định.
- Không commit credentials thật.
- Kiểm tra log không lộ dữ liệu bí mật.
- Có graceful shutdown cho executor, connection pool và RMI objects.
- Có timeout cho network calls.
- Có giới hạn nội dung đầu vào phù hợp.
- Kiểm tra lỗi serialization và remote exception.
- Ghi rõ giới hạn bảo mật nếu chưa triển khai TLS hoặc cơ chế xác thực mạnh hơn.

Không tuyên bố ứng dụng đã sẵn sàng production nếu TLS, xác thực, phân quyền và xử lý failure chưa được triển khai và kiểm chứng.

## 11. Báo cáo bắt buộc sau mỗi phase

Claude phải báo:

1. Những gì đã sửa.
2. Danh sách file đã thay đổi.
3. Lý do kỹ thuật.
4. Test đã chạy và kết quả thực tế.
5. Test chưa chạy và lý do.
6. Hướng dẫn chạy thủ công.
7. Rủi ro còn lại.

Không dừng lại ở việc đưa ra kế hoạch; cần chỉnh sửa source, chạy test và cập nhật tài liệu.
