
# NEXUS Distributed Chat — Kế hoạch kiểm thử và nghiệm thu

## 1. Mục đích

Tài liệu này xác định cách kiểm tra NEXUS Distributed Chat từ unit test đến kiểm thử hai server trên hai máy vật lý.

Mục tiêu là chứng minh hệ thống hoạt động thật, không chỉ chứng minh rằng project build thành công.

## 2. Quy tắc ghi nhận kết quả

Mỗi test phải có một trong các trạng thái:

- `PASS`: đã chạy và đạt yêu cầu.
- `FAIL`: đã chạy nhưng không đạt.
- `NOT RUN`: chưa chạy.
- `BLOCKED`: không thể chạy do thiếu điều kiện.
- `CODE REVIEW ONLY`: mới kiểm tra source, chưa thực thi.

Không ghi `PASS` nếu không có bằng chứng. Không đánh đồng unit test với integration test. Không tuyên bố đã kiểm thử hai host nếu chỉ chạy localhost.

## 3. Build baseline

Tại thư mục gốc:

```powershell
java -version
mvn -version
mvn clean package
```

Ghi lại:
- Java version.
- Maven version.
- Tổng số test.
- Passed, failed, errors và skipped.
- Lỗi build nếu có.

Kết quả build cũ hoặc số test trong báo cáo cũ không thay thế cho lần chạy mới.

## 4. Database và persistence

### DB-01 — Lưu message

1. Khởi động server với database hoạt động.
2. Gửi một message hợp lệ.
3. Kiểm tra message trong `chat_messages`.
4. Xác minh message ID, room ID, origin server ID và sequence.
5. Restart server.
6. Đọc lại lịch sử.

**Kỳ vọng:** message được lưu đúng và vẫn tồn tại sau restart.

### DB-02 — Atomic message + outbox

1. Gửi message từ Node A.
2. Kiểm tra message và outbox item tương ứng.
3. Tạo tình huống insert outbox thất bại bằng test phù hợp.
4. Xác minh transaction rollback.

**Kỳ vọng:** không có trường hợp message đã commit nhưng outbox bị thiếu do lỗi ở cùng transaction.

### DB-03 — Idempotency

1. Gửi cùng một message ID tới replication endpoint nhiều lần.
2. Kiểm tra database sau mỗi lần gọi.

**Kỳ vọng:** chỉ có một bản ghi logic cho message ID đó.

### DB-04 — Persistence mode

1. Chạy với DB khỏe mạnh.
2. Gửi message và kiểm tra kết quả.
3. Mô phỏng lỗi ghi DB.
4. Kiểm tra kết quả API và UI.

**Kỳ vọng:** hệ thống không báo `MYSQL_DURABLE` cho một thao tác chưa được lưu bền vững. Nếu dùng fallback RAM, trạng thái phải nói rõ điều đó.

### DB-05 — Outbox sau restart

1. Để outbox có message pending.
2. Dừng server.
3. Khởi động lại.
4. Kiểm tra outbox và retry worker.

**Kỳ vọng:** outbox durable vẫn còn và có thể được xử lý tiếp.

## 5. Replication

### REP-01 — A → B

1. Khởi động A và B với database riêng.
2. Kết nối Alice tới A.
3. Gửi message.
4. Kết nối Bob tới B.
5. Kiểm tra message trên Bob và DB B.

**Kỳ vọng:** message đến B đúng một lần.

### REP-02 — B → A

Lặp lại theo chiều ngược lại.

**Kỳ vọng:** B có thể gửi message được A nhận, không phụ thuộc chiều khởi tạo ban đầu.

### REP-03 — Chống vòng lặp

1. Gửi message từ A.
2. Theo dõi log và outbox ở A/B.
3. Kiểm tra message được nhận tại B.

**Kỳ vọng:** B lưu message nhận từ A nhưng không tạo một vòng replication vô hạn gửi ngược message đó về A.

### REP-04 — Catch-up trên 100 message

1. Làm B offline.
2. Tạo ít nhất 250 message trên A.
3. Khởi động lại B.
4. Đợi catch-up hoàn thành.
5. So sánh message ID ở hai DB.

**Kỳ vọng:** tất cả message cần đồng bộ đều có ở B, không thiếu và không duplicate.

### REP-05 — Lỗi giữa các batch

1. Chuẩn bị nhiều batch cần catch-up.
2. Mô phỏng lỗi lưu DB ở một batch giữa.
3. Khôi phục server/database.
4. Tiếp tục catch-up.

**Kỳ vọng:** hệ thống tiếp tục từ checkpoint đã commit, không bỏ sót dữ liệu.

### REP-06 — Restart trong catch-up

Restart receiver hoặc origin khi catch-up chưa hoàn tất.

**Kỳ vọng:** checkpoint và dữ liệu được xử lý nhất quán sau restart.

### REP-07 — Outbox retry

1. Tắt B.
2. Gửi nhiều message từ A.
3. Xác minh outbox có pending/failed item.
4. Bật lại B.
5. Xác minh outbox được xử lý và message đến B.

**Kỳ vọng:** lỗi peer tạm thời không khiến durable outbox bị mất.

## 6. Session và room

Kiểm tra các trường hợp:

- Session hợp lệ.
- Session null.
- Session không tồn tại.
- Session expired.
- Session đã disconnect.
- Room hợp lệ.
- Room không tồn tại.
- User không có quyền truy cập room, nếu có membership control.
- Nội dung rỗng.
- Nội dung vượt giới hạn.
- Request đồng thời với thao tác disconnect.

**Kỳ vọng:** server kiểm tra quyền ở phía server; UI không phải lớp bảo vệ duy nhất. API đọc lịch sử không được trả dữ liệu cho session không hợp lệ.

## 7. Client reconnect

### CL-01 — Server khởi động lại

1. Kết nối client với A.
2. Gửi và nhận message.
3. Dừng A.
4. Kiểm tra trạng thái UI.
5. Khởi động lại A.
6. Chờ client retry.
7. Kiểm tra session, room, lịch sử và polling.

**Kỳ vọng:** client thực sự lookup lại RMI service và kết nối lại hoặc thông báo lỗi rõ ràng.

### CL-02 — Server không khả dụng

1. Cấu hình host/port sai hoặc giữ server offline.
2. Quan sát retry.

**Kỳ vọng:** UI không treo; retry có backoff; người dùng có thể hủy retry; không tạo nhiều thread không kiểm soát.

### CL-03 — Deduplicate UI

Đưa cùng message ID qua nhiều response hoặc polling result.

**Kỳ vọng:** UI chỉ hiển thị một message.

### CL-04 — JavaFX responsiveness

Mô phỏng RMI response chậm.

**Kỳ vọng:** network call không chặn JavaFX Application Thread; giao diện vẫn phản hồi và thể hiện loading/error state.

## 8. Admin và monitoring

- Token đúng được chấp nhận.
- Token sai hoặc thiếu bị từ chối ở endpoint cần bảo vệ.
- DB offline được hiển thị là offline/degraded/fallback.
- Peer offline được hiển thị là unreachable.
- Pending outbox phản ánh dữ liệu thực.
- Lỗi lấy monitoring không bị trình bày thành trạng thái healthy.
- Dữ liệu stale có timestamp hoặc cảnh báo.
- Không ghi token/password vào log.

IP lấy từ RMI có thể là địa chỉ host quan sát được qua kết nối; không nên khẳng định đó luôn là IP vật lý cuối cùng khi có NAT hoặc proxy.

## 9. Nghiệm thu hai máy vật lý

### Mô hình

- Host A: Node A + Database A.
- Host B: Node B + Database B.
- Client A: VM/máy khác, kết nối A.
- Client B: VM/máy khác, kết nối B.

### Trình tự

1. Ghi IP LAN thật của A và B.
2. Ghi phiên bản Java, Maven và database.
3. Apply schema trên cả hai database.
4. Cấu hình advertised host, peer host và RMI ports.
5. Khởi động A và B.
6. Xác minh DB health và peer health.
7. Alice gửi message qua A.
8. Bob nhận message qua B.
9. Bob trả lời qua B.
10. Alice nhận reply qua A.
11. Gửi nhiều message và kiểm tra duplicate.
12. Tắt B.
13. Gửi ít nhất 5 message từ A.
14. Kiểm tra outbox của A.
15. Khởi động lại B.
16. Xác minh catch-up và outbox retry.
17. Restart một server khi còn outbox pending.
18. Xác minh message/outbox còn trong DB sau restart.
19. Lưu log, query kết quả và ảnh chụp làm bằng chứng.

### Tiêu chí đạt

- Replication A → B và B → A hoạt động.
- Hai node dùng database riêng.
- Message ID không bị duplicate.
- Outbox durable tồn tại sau restart.
- Catch-up lấy hết dữ liệu trong tình huống đã kiểm thử.
- Client reconnect hoặc hiển thị lỗi chính xác.
- Dashboard không báo trạng thái healthy giả.
- Có bằng chứng kiểm thử thực tế.

## 10. Bảng theo dõi nghiệm thu

| ID | Hạng mục | Trạng thái ban đầu | Bằng chứng cần có |
|---|---|---|---|
| BUILD-01 | `mvn clean package` | NOT RUN | Output build mới |
| DB-01 | Message persistence | NOT RUN | Test/log/query DB |
| DB-02 | Atomic message + outbox | NOT RUN | Test rollback |
| DB-05 | Outbox survives restart | NOT RUN | Query DB trước/sau restart |
| REP-01 | A → B | NOT RUN | Log + DB query |
| REP-02 | B → A | NOT RUN | Log + DB query |
| REP-04 | Catch-up >100 messages | NOT RUN | Count + message IDs |
| CL-01 | Client reconnect | NOT RUN | Log + UI |
| ADM-01 | Admin token validation | NOT RUN | Test output |
| NET-01 | Two physical hosts | NOT RUN | IP/port/log/screenshots |

Hãy cập nhật trạng thái sau khi chạy test. Không tự động chuyển `NOT RUN` thành `PASS`.
