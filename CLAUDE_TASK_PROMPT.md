
# PROMPT GIAO NHIỆM VỤ CHO CLAUDE — NEXUS DISTRIBUTED CHAT

## 1. Vai trò

Bạn là Senior Java Engineer, Distributed Systems Engineer, Java RMI Engineer, Database Engineer và QA Engineer.

Nhiệm vụ của bạn là audit, sửa lỗi và hoàn thiện **source code NEXUS Distributed Chat hiện tại**. Không tạo một project mới thay thế project cũ. Không chỉ viết báo cáo hoặc đề xuất kế hoạch; hãy trực tiếp chỉnh sửa source, chạy test và cập nhật tài liệu trong repository.

## 2. Bối cảnh dự án

Dự án dùng:

- Java 21.
- Maven multi-module.
- Java RMI.
- JavaFX/FXML/CSS.
- JDBC với MySQL/MariaDB.

Các module được ghi nhận:

- `chat-contract`
- `chat-server`
- `chat-desktop`

Mục tiêu cuối cùng:

1. Node A và Node B chạy trên hai máy vật lý khác nhau.
2. Mỗi node sử dụng database riêng.
3. Client có thể kết nối tới một trong hai node.
4. Message gửi qua A có thể đến client kết nối B và ngược lại.
5. Khi peer offline, durable outbox tiếp tục tồn tại nếu DB còn hoạt động.
6. Khi peer trở lại, hệ thống retry và catch-up các message bị bỏ lỡ.
7. Admin Dashboard hiển thị dữ liệu thật, không sử dụng số liệu giả để báo hệ thống healthy.

## 3. Tài liệu bắt buộc phải đọc

Đọc toàn bộ ba tài liệu gốc:

1. `docs_NEXUS-CHAT/DISTRIBUTED_CHAT_RMI_DESIGN_PLAN.md`
2. `docs_NEXUS-CHAT/DISTRIBUTED_CHAT_RMI_JAVAFX_FOLDER_STRUCTURE.md`
3. `docs_NEXUS-CHAT/NEXUS_CHAT_UI_UX_SPECIFICATION.md`

Đọc toàn bộ sáu tài liệu bổ sung:

4. `CODE_AUDIT_REPORT.md`
5. `IMPLEMENTATION_ROADMAP.md`
6. `TESTING_AND_ACCEPTANCE.md`
7. `MULTI_MACHINE_DEPLOYMENT.md`
8. `SYSTEM_ARCHITECTURE.md`

Sau đó đọc file prompt này.

Nếu tên hoặc vị trí tài liệu trong repository khác với danh sách, hãy tìm đúng file trong repository. Nếu thiếu tài liệu, ghi rõ file nào không tìm thấy. Không giả định nội dung của file chưa đọc.

## 4. Quy tắc bắt buộc

- Đọc source thực tế trước khi sửa.
- Tài liệu bổ sung là hướng dẫn audit, không thay thế việc kiểm tra code.
- Nếu source khác với audit report, ghi lại sai khác và xác minh bằng code/test.
- Không viết lại toàn bộ project nếu không cần thiết.
- Không phá RMI contracts hoặc schema hiện tại mà không có lý do kỹ thuật và phương án tương thích.
- Không xóa chức năng đang hoạt động mà chưa kiểm tra ảnh hưởng.
- Không hardcode IP LAN hoặc credentials thật.
- Không dùng `127.0.0.1` làm peer host trong deployment đa máy.
- Không dùng mock data để giả lập thành công của replication hoặc monitoring.
- Không tuyên bố test pass nếu chưa chạy.
- Không tuyên bố hai-host verified nếu chưa thực sự kiểm thử hai máy vật lý.
- Giữ JavaFX network operations ngoài JavaFX Application Thread.
- Giữ code dễ đọc, có error handling và lifecycle management phù hợp.

## 5. Bước 1 — Audit và baseline

Trước khi sửa:

1. In cây thư mục và xác định các module.
2. Kiểm tra Java/Maven version.
3. Đọc parent POM và module POM.
4. Đọc RMI interfaces, DTO, service implementations, repositories và schema.
5. Đọc scripts chạy Node A/B và client.
6. Đọc cấu hình database, RMI và monitoring.
7. Chạy:

   `mvn clean package`

8. Ghi lại kết quả thực tế:
    - Build success/failure.
    - Số test chạy.
    - Passed, failed, errors và skipped.
    - Lỗi cụ thể nếu có.

Không dựa vào thư mục `target/` cũ để kết luận build hiện tại thành công.

## 6. Bước 2 — Sửa các lỗi ưu tiên

### A. Transactional outbox

Kiểm tra luồng `sendMessage()`, message repository và outbox repository.

Nếu message và outbox đang được ghi riêng, hãy sửa để khi dùng MySQL/MariaDB:

- Message và outbox item được lưu trong cùng transaction.
- Nếu một bước thất bại, rollback cả hai.
- Chỉ đánh thức replication worker sau commit.
- Không gọi RMI peer bên trong transaction database.
- Duplicate message ID được xử lý idempotent.
- Outbox còn tồn tại sau restart.
- In-memory fallback không bị mô tả là durable.

Bổ sung test cho lỗi insert message, lỗi insert outbox, rollback, duplicate và restart.

### B. Catch-up và checkpoint

Kiểm tra `performCatchUpSync()`, `getMessagesAfter(...)`, sequence và replication state.

Nếu catch-up chỉ lấy một batch hoặc cursor chỉ nằm trong bộ nhớ, hãy sửa để:

- Phân trang đến hết dữ liệu.
- Checkpoint được lưu bền vững.
- Checkpoint chỉ được cập nhật sau khi batch lưu thành công.
- Sequence có ý nghĩa rõ ràng theo origin.
- `local_sequence` không bị nhầm với `origin_sequence`.
- Restart hoặc lỗi giữa batch không làm mất message.
- Message nhận từ peer không bị gửi ngược.
- Duplicate không tạo thêm bản ghi logic.

Bổ sung test với ít nhất 250 message, duplicate, lỗi giữa batch, restart và replication hai chiều.

### C. Client reconnect

Kiểm tra `ChatClientService`, `RmiClientManager` và polling lifecycle.

Nếu client chỉ thay đổi trạng thái UI khi lỗi, hãy triển khai reconnect thực sự:

- Retry coordinator duy nhất.
- Backoff hợp lý.
- Lookup lại remote service.
- Register session mới khi cần.
- Reload rooms/history.
- Khởi động đúng một polling worker.
- Hủy retry khi disconnect hoặc đóng ứng dụng.
- Không tạo session/thread trùng.
- Deduplicate UI message theo ID.
- Không block JavaFX Application Thread.

Bổ sung test mô phỏng server offline rồi online. Nếu chưa thể kiểm thử RMI thật trong môi trường hiện tại, ghi rõ limitation.

### D. Session và room authorization

Kiểm tra mọi method trong `ChatRemote`.

Đảm bảo:
- API cần session đều validate session.
- `getMessages()` không trả dữ liệu cho session invalid.
- `getRooms()` và `heartbeat()` xử lý session hợp lệ đúng quy tắc.
- `sendMessage()` kiểm tra room tồn tại.
- Kiểm tra membership/authorization nếu ứng dụng yêu cầu.
- Lỗi được trả về nhất quán.
- JDBC và in-memory repositories có hành vi phù hợp.

Bổ sung test cho session null, unknown, expired, disconnected và room không tồn tại.

### E. Persistence mode và monitoring

Kiểm tra `MYSQL_DURABLE`, `IN_MEMORY_FALLBACK`, database health và admin monitoring.

Đảm bảo:
- Kết quả ghi phản ánh persistence thực tế.
- Không báo durable nếu DB write thất bại.
- Dashboard không báo healthy khi DB hoặc peer offline.
- Dữ liệu chưa biết được hiển thị là unknown/degraded.
- Outbox count và replication status đến từ backend thực.
- Admin token được xác minh ở tất cả endpoint cần bảo vệ.
- Không log token hoặc password.
- Không sử dụng token mặc định không an toàn trong deployment.

### F. Multi-machine deployment

Kiểm tra `ServerConfig`, `RmiRegistryManager` và toàn bộ scripts.

Đảm bảo:
- Node A và B có ID khác nhau.
- Advertised host có thể truy cập từ máy khác.
- Peer host dùng IP/hostname thực tế của host đối diện.
- Registry/service ports có thể cấu hình.
- Nếu replication remote object dùng port riêng, phải tài liệu hóa và mở đúng port.
- Mỗi node dùng database riêng.
- Scripts không hardcode localhost cho peer.
- Cấu hình có thể cung cấp qua file/environment/arguments theo cơ chế source thực sự hỗ trợ.
- Hướng dẫn firewall khớp với port thực tế.

Không tự giả định port chỉ từ tài liệu. Kiểm tra cách RMI objects được export trong source.

## 7. Bước 3 — Test và xác minh

Sau khi sửa, chạy:

`mvn clean package`

Chạy các test liên quan tới:

- Persistence transaction.
- Outbox durability.
- Idempotency.
- Catch-up lớn hơn 100 message.
- Checkpoint sau restart.
- Session/room validation.
- Client reconnect.
- Monitoring authentication.
- RMI integration nếu môi trường cho phép.

Nếu không có database hoặc không thể mở port trong môi trường của bạn:

- Không giả vờ test đã chạy.
- Chạy các test khả thi.
- Ghi rõ test nào bị `BLOCKED` hoặc `NOT RUN`.
- Cung cấp các bước thủ công để người dùng chạy trên hai máy.

## 8. Bước 4 — Kiểm tra UI

Không redesign toàn bộ giao diện nếu không cần cho việc sửa lỗi logic.

Đảm bảo UI thể hiện trạng thái backend thật:

- Connected/disconnected/reconnecting.
- Sending/sent/failed.
- Database/persistence mode.
- Peer reachability.
- Replication/outbox state.
- Loading/error/unknown states.

Không dùng trạng thái mặc định để che lỗi remote call.

## 9. Bước 5 — Cập nhật tài liệu

Cập nhật README và tài liệu liên quan nếu implementation thay đổi.

Tài liệu phải mô tả chính xác:
- Cách build.
- Cách cấu hình Node A/B.
- Database setup.
- RMI ports và firewall.
- Cách chạy client.
- Cách kiểm tra replication.
- Các test đã chạy.
- Các test chưa chạy.
- Những giới hạn còn tồn tại.

Không đánh dấu checklist nghiệm thu là PASS nếu chưa có bằng chứng.

## 10. Báo cáo cuối cùng

Trả về báo cáo có các phần:

### A. Tóm tắt
Những vấn đề đã sửa và trạng thái hiện tại.

### B. Files changed
Liệt kê file đã tạo/sửa và lý do.

### C. Build/test
Ghi chính xác các lệnh đã chạy, số test và kết quả thực tế.

### D. Verified behavior
Nêu các hành vi đã được chứng minh bằng automated test.

### E. Manual verification required
Nêu các test người dùng phải chạy trên hai máy vật lý.

### F. Known limitations
Ghi những vấn đề chưa xử lý, chưa test hoặc bị chặn bởi môi trường.

### G. Run instructions
Cung cấp lệnh và cấu hình chạy phù hợp với source hiện tại.

Kết thúc bằng bảng:

| Hạng mục | Code implemented | Automated test | Two-host verified | Ghi chú |
|---|---|---|---|---|
| Transactional outbox | Chưa xác minh | Chưa xác minh | Chưa xác minh | |
| Catch-up/checkpoint | Chưa xác minh | Chưa xác minh | Chưa xác minh | |
| Client reconnect | Chưa xác minh | Chưa xác minh | Chưa xác minh | |
| Session validation | Chưa xác minh | Chưa xác minh | Chưa xác minh | |
| Monitoring/auth | Chưa xác minh | Chưa xác minh | Chưa xác minh | |
| Deployment A/B | Chưa xác minh | Chưa xác minh | Chưa xác minh | |

Cập nhật bảng theo kết quả thực tế. Không dùng một cột để suy ra cột khác.

## 11. Chỉ dẫn cuối cùng

Bắt đầu bằng việc đọc tài liệu và audit source, sau đó chạy baseline build. Tiếp tục sửa code theo thứ tự:

1. Transactional outbox.
2. Catch-up/checkpoint.
3. Client reconnect.
4. Session/room authorization.
5. Persistence mode/monitoring.
6. Multi-machine deployment.

Không dừng lại sau khi chỉ viết kế hoạch. Hãy triển khai thay đổi, chạy test, cập nhật tài liệu và báo cáo trung thực những gì đã được xác minh.


## BẮT BUỘC: SOURCE SAFETY, DATA INTEGRITY VÀ ACCEPTANCE GATE

### 1. Bảo toàn dự án hiện tại
- Đọc toàn bộ ba tài liệu yêu cầu gốc, các tài liệu bổ sung và source code trước khi sửa.
- Chạy baseline build/test và ghi lại lỗi có sẵn trước khi thay đổi.
- Không tự ý xóa chức năng, thay framework, thay kiến trúc tổng thể hoặc tạo lại project từ đầu nếu chưa chứng minh được sự cần thiết.
- Không thay thế implementation thật bằng mock, dữ liệu giả hoặc in-memory fallback để làm cho demo có vẻ hoạt động.
- Giữ nguyên các chức năng đang hoạt động và cập nhật tài liệu khi thiết kế thực tế thay đổi.

### 2. Tính toàn vẹn dữ liệu và replication
- Xác minh message persistence và replication outbox có tính nguyên tử trong phạm vi một database transaction.
- Xác minh retry có backoff, không mất message khi peer offline và không tạo duplicate khi retry hoặc catch-up.
- Phân biệt message ID, origin server ID, origin sequence và local sequence; không sử dụng lẫn các loại sequence.
- Catch-up phải phân trang hoặc lặp cho đến khi đồng bộ hết dữ liệu, không được mặc định chỉ lấy một batch rồi kết luận đã đồng bộ.
- Chỉ xác nhận message đã được replication khi có acknowledgement theo đúng hợp đồng giao tiếp.
- Xử lý an toàn trường hợp server restart, peer mất kết nối giữa batch, dữ liệu đến sai thứ tự và hai server gửi đồng thời.
- Không tuyên bố hệ thống có exactly-once delivery nếu chưa chứng minh được; mô tả chính xác cơ chế retry và idempotency đang triển khai.

### 3. Session, authentication và monitoring
- Kiểm tra quyền truy cập nhất quán ở tất cả remote methods, bao gồm lấy tin nhắn, danh sách phòng, heartbeat và gửi tin.
- Xác minh room tồn tại và người dùng có quyền gửi tin trước khi chấp nhận message.
- Dashboard phải phân biệt trạng thái kết nối peer, trạng thái database, persistence mode và số message đang chờ replication.
- Không hiển thị trạng thái ONLINE hoặc MYSQL_DURABLE nếu chỉ dựa trên cấu hình hay trạng thái kết nối cũ.
- Không ghi mật khẩu, token hoặc thông tin xác thực nhạy cảm vào log.

### 4. Acceptance gate
Không coi task là hoàn tất chỉ vì code biên dịch thành công.

Phải thực hiện:
1. Build và toàn bộ automated tests.
2. Kiểm thử database persistence, transactional outbox, idempotency và catch-up nhiều batch.
3. Kiểm thử reconnect, session validation và room authorization.
4. Kiểm thử tích hợp hai server và hai client.
5. Kiểm thử peer offline, gửi tin trong thời gian offline, khởi động lại peer và xác minh đồng bộ sau phục hồi.
6. Kiểm tra dashboard phản ánh đúng trạng thái thực tế.

Nếu không có quyền truy cập hai máy vật lý hoặc môi trường mạng phù hợp, ghi rõ bài kiểm thử nào chưa chạy. Không được tạo log, ảnh chụp hoặc kết quả giả để chứng minh đã nghiệm thu.

### 5. Báo cáo bàn giao
Báo cáo cuối phải có:
- Các file đã thay đổi và lý do.
- Lỗi đã xác minh, lỗi đã sửa và lỗi còn tồn tại.
- Lệnh build/test đã chạy cùng kết quả thực tế.
- Bằng chứng hoặc log cho từng acceptance test.
- Những test chưa chạy, nguyên nhân và cách chạy tiếp.
- Cấu hình cần thay đổi khi triển khai trên hai máy.
- Các giới hạn bảo mật và vận hành còn lại.

Chỉ đánh dấu DONE khi có bằng chứng phù hợp. Nếu chỉ kiểm tra source code, dùng CODE REVIEW ONLY; nếu chưa chạy, dùng NOT RUN. Ưu tiên tính đúng đắn và tính trung thực hơn việc tuyên bố đã hoàn thành toàn bộ.


## FINAL REQUIREMENT: STRICT DESIGN DOCUMENT COMPLIANCE

The three original documents are the authoritative product specification:

1. `DISTRIBUTED_CHAT_RMI_DESIGN_PLAN.md`
2. `DISTRIBUTED_CHAT_RMI_JAVAFX_FOLDER_STRUCTURE.md`
3. `NEXUS_CHAT_UI_UX_SPECIFICATION.md`

### A. Design-first implementation

Before modifying the project, read all three original documents completely. Do not rely solely on supplementary audit reports or previous implementation summaries.

Treat the original documents as the source of truth for product requirements, architecture, module structure, UI/UX, screens, workflows and functionality.

Do not silently omit, simplify, replace or reinterpret requirements. If a requirement conflicts with the current implementation, document the conflict and implement the original requirement unless a genuine technical limitation requires an explicitly documented alternative.

### B. Requirement traceability

Create `docs/REQUIREMENT_TRACEABILITY.md`.

For every requirement identified in the original documents, record:
- A unique requirement ID.
- The original document and relevant section.
- The expected behavior or design.
- The corresponding implementation file, class, view or component.
- The test or visual evidence used to verify it.
- Its status: IMPLEMENTED, PARTIAL, MISSING, BLOCKED or NOT VERIFIED.

Do not mark a requirement IMPLEMENTED based only on the existence of a class, file, button or placeholder.

### C. UI/UX fidelity

Use `NEXUS_CHAT_UI_UX_SPECIFICATION.md` as the visual and interaction reference.

Inspect the actual JavaFX screens, FXML layouts, CSS, navigation, components, spacing, typography, colors, states and user workflows against the specification.

Preserve the intended NEXUS CHAT client identity and NEXUS CONTROL administration identity. Do not substitute an unrelated template or redesign the product according to personal preference.

Implement all specified screens and interactions. Buttons and controls must perform their intended actions; do not leave decorative controls that appear functional but do nothing.

Run the application and inspect actual rendered screens whenever the environment permits. Record missing screens, visual mismatches, broken layouts and incomplete interactions. If screenshots or runtime access are unavailable, explicitly report the limitation.

### D. Architecture and folder-structure compliance

Compare the actual repository with the specified Maven multi-module structure and responsibilities of `chat-contract`, `chat-server` and `chat-desktop`.

Do not move, rename or merge modules without a documented technical justification. Ensure the JavaFX client, RMI contracts, server services, persistence, replication and monitoring follow the intended separation of responsibilities.

### E. Definition of done

The project is not complete merely because it compiles or passes unit tests.

Completion requires:
1. All original requirements mapped in `REQUIREMENT_TRACEABILITY.md`.
2. No unresolved MISSING requirements that are mandatory for the specified product.
3. All specified screens and workflows implemented and checked.
4. Automated tests passing, with integration tests reported separately.
5. Multi-server behavior tested in the available environment.
6. Any unverified visual, network or deployment behavior explicitly disclosed.

At the end, provide a requirement-by-requirement compliance report. Never claim exact design compliance without comparing the implementation against the original documents and recording the verification evidence.

## MANDATORY REQUIREMENT: TWO SEPARATE DESKTOP APPLICATIONS

The project MUST provide two distinct desktop applications with separate windows, independent entry points, and clearly separated responsibilities.

### 1. NEXUS CHAT — Client Application

- A standalone JavaFX desktop application for regular users.
- Has its own application entry point and main window.
- Implements the chat client UI/UX defined in `NEXUS_CHAT_UI_UX_SPECIFICATION.md`.
- Provides the user-facing features specified in the original design, including authentication, chat rooms, messaging, and relevant connection states.
- Connects to the appropriate chat server through the defined RMI contracts.
- Must not display the Admin Dashboard as part of its normal interface.
- Must not require the administrator application to be running for ordinary chat functionality.

### 2. NEXUS CONTROL — Admin Application

- A separate standalone JavaFX desktop application for administrators.
- Has its own application entry point and independent main window.
- Implements the administration and monitoring interface defined in the original design documents.
- Displays the specified server and peer status, database and persistence status, connected sessions and client IP addresses, replication status, pending outbox information, and relevant system logs, where supported by the design and implementation.
- Uses the appropriate monitoring interfaces and administrator authentication or authorization.
- Must not be embedded inside the NEXUS CHAT client window.
- Must not require a regular chat client window to be open for administration and monitoring.

### 3. Strict application separation

- Launching NEXUS CHAT must open the client window only.
- Launching NEXUS CONTROL must open the admin window only.
- Each application must have a distinct JavaFX entry point and a clearly documented launch command.
- Both applications may reuse `chat-contract` and other appropriate shared code, but must not be implemented as two tabs or two modes inside a single application window.
- Keep shared business logic and communication contracts in their appropriate modules; avoid duplicating server-side business logic in the desktop applications.
- Closing one desktop application must not automatically close the other application or shut down a remote server.
- The admin application must communicate with the server using the intended monitoring and administration interfaces, not by directly modifying database tables from the UI.
- If the current Maven structure requires changes to support two independently launchable applications, make the smallest justified change and document it.

### 4. Build and acceptance requirements

- Both applications must compile successfully.
- Both applications must launch independently using documented commands.
- Verify that their windows, titles, navigation and responsibilities are distinct.
- Verify that NEXUS CHAT can perform its intended user workflows without opening NEXUS CONTROL.
- Verify that NEXUS CONTROL can monitor the configured server independently of NEXUS CHAT.
- Update the Maven configuration, README, folder structure documentation, launch scripts and requirement traceability document accordingly.
- Do not report this requirement as complete based solely on separate FXML files. Demonstrate two independently launchable desktop applications and record the actual verification results.

### VISUAL REFERENCES AND DESIGN FIDELITY

Use the provided reference images as visual guidance for the intended layout, visual hierarchy, spacing, typography, color palette, components and interaction states.

The original design documents remain the authoritative functional and architectural specification. Do not blindly copy reference images or invent functionality that conflicts with the documents.

Implement two independently launchable desktop applications:
1. NEXUS CHAT — the user-facing chat application.
2. NEXUS CONTROL — the administrator-facing monitoring application.

Each application must have its own main window and entry point. Do not combine them into tabs or modes within one window.

Before implementation, identify which reference images correspond to which screens. After implementation, run both applications and compare the actual rendered UI against the references and original UI/UX specification. Report visual mismatches and incomplete interactions honestly.

Do not use placeholder buttons, mock server status or simulated replication as substitutes for required functional behavior. Clearly label any mock data used exclusively for development or testing.