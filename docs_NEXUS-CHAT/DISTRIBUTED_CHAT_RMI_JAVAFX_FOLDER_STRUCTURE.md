# DISTRIBUTED CHAT ROOM — JAVAFX DESKTOP STRUCTURE
## Tài liệu cấu trúc thư mục, UI/UX và tổ chức mã nguồn

**Đề tài:** Distributed Chat Room with Java RMI Replication  
**Desktop UI:** JavaFX + FXML + CSS  
**Build tool:** Maven  
**Database:** MySQL/MariaDB qua JDBC — mỗi server có CSDL riêng  
**Network:** Java RMI — hai server chạy trên hai máy vật lý; các desktop client chạy trên máy ảo  
**Mục tiêu tài liệu:** Định nghĩa cấu trúc project rõ ràng trước khi triển khai code, tách UI khỏi logic, hỗ trợ giao diện desktop đẹp, dễ bảo trì và dễ demo.

---

## 1. Quyết định kiến trúc

Project sử dụng **JavaFX + FXML + CSS** để xây dựng desktop client. FXML mô tả bố cục giao diện; CSS chịu trách nhiệm màu sắc, typography, khoảng cách, trạng thái hover/focus và responsive behavior trong phạm vi JavaFX; Controller xử lý tương tác UI; Service gọi Java RMI; Server xử lý nghiệp vụ và lưu dữ liệu qua JDBC.

### Phân chia thành phần

- **Desktop Client:** JavaFX/FXML/CSS, kết nối tới một server RMI được cấu hình.
- **Server Node A:** chạy trên máy vật lý A, cung cấp remote services, lưu vào Database A và replication sang Node B.
- **Server Node B:** chạy trên máy vật lý B, cung cấp remote services, lưu vào Database B và replication sang Node A.
- **Shared Contract:** DTO và remote interfaces được dùng chung giữa client và hai server.
- **Database:** hai CSDL độc lập, mỗi server sở hữu CSDL của node mình.

> JavaFX/FXML chỉ là tầng giao diện desktop. Nó không thay thế Java RMI, nghiệp vụ server hoặc cơ chế đồng bộ dữ liệu.

---

## 2. Cấu trúc thư mục tổng thể khuyến nghị

Nên dùng Maven multi-module để tách contract, server và desktop client. Cách này giúp tránh đóng gói nhầm mã UI vào server và giúp triển khai server trên hai máy vật lý dễ hơn.

```text
distributed-chat-room/
├── pom.xml                         # Parent Maven: phiên bản Java và dependency chung
├── README.md                       # Cách build/chạy toàn bộ hệ thống
├── .gitignore
├── docs/
│   ├── ARCHITECTURE.md             # Kiến trúc RMI + database replication
│   ├── NETWORK_SETUP.md            # IP, port, firewall, cấu hình VM
│   ├── DATABASE_DESIGN.md          # Schema và chiến lược đồng bộ
│   ├── UI_UX_GUIDELINES.md         # Design system JavaFX
│   ├── TEST_PLAN.md                # Kịch bản kiểm thử
│   └── screenshots/
│
├── chat-contract/                  # DTO và Remote Interfaces dùng chung
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/distributedchat/contract/
│       │   ├── model/
│       │   │   ├── ChatMessage.java
│       │   │   ├── UserSession.java
│       │   │   ├── NodeInfo.java
│       │   │   └── SyncStatus.java
│       │   ├── remote/
│       │   │   ├── ChatClientService.java
│       │   │   └── ServerSyncService.java
│       │   └── dto/
│       │       ├── MessageBatch.java
│       │       ├── LoginRequest.java
│       │       └── LoginResponse.java
│       └── test/java/
│
├── chat-server/                    # Chạy trên Host A và Host B
│   ├── pom.xml
│   ├── src/
│   │   ├── main/java/com/distributedchat/server/
│   │   │   ├── bootstrap/
│   │   │   │   ├── ChatServerApplication.java
│   │   │   │   ├── RmiRegistryManager.java
│   │   │   │   └── ShutdownManager.java
│   │   │   ├── config/
│   │   │   │   ├── ServerConfig.java
│   │   │   │   ├── DatabaseConfig.java
│   │   │   │   └── PeerConfig.java
│   │   │   ├── remote/
│   │   │   │   ├── ChatClientServiceImpl.java
│   │   │   │   └── ServerSyncServiceImpl.java
│   │   │   ├── service/
│   │   │   │   ├── ChatService.java
│   │   │   │   ├── ReplicationService.java
│   │   │   │   ├── HistorySyncService.java
│   │   │   │   └── PresenceService.java
│   │   │   ├── repository/
│   │   │   │   ├── MessageRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── persistence/
│   │   │   │   ├── DatabaseManager.java
│   │   │   │   └── JdbcMessageRepository.java
│   │   │   ├── store/
│   │   │   │   └── MessageDeduplicationStore.java
│   │   │   └── util/
│   │   │       ├── InputValidator.java
│   │   │       └── AppLogger.java
│   │   ├── main/resources/
│   │   │   ├── application.properties.example
│   │   │   └── db/
│   │   │       └── db-schema.sql
│   │   └── test/java/com/distributedchat/server/
│   │       ├── service/
│   │       └── repository/
│   └── target/                     # Maven sinh ra; không commit
│
├── chat-desktop/                   # JavaFX client desktop chạy trên VM
│   ├── pom.xml
│   ├── src/
│   │   ├── main/java/com/distributedchat/desktop/
│   │   │   ├── DesktopApplication.java
│   │   │   ├── config/
│   │   │   │   └── ClientConfig.java
│   │   │   ├── navigation/
│   │   │   │   ├── ViewRouter.java
│   │   │   │   └── ViewId.java
│   │   │   ├── controller/
│   │   │   │   ├── LoginController.java
│   │   │   │   ├── ChatController.java
│   │   │   │   ├── SettingsController.java
│   │   │   │   └── ConnectionStatusController.java
│   │   │   ├── service/
│   │   │   │   ├── RmiClient.java
│   │   │   │   ├── ChatClientFacade.java
│   │   │   │   ├── MessagePollingService.java
│   │   │   │   └── ConnectionMonitor.java
│   │   │   ├── model/
│   │   │   │   ├── ChatMessageViewModel.java
│   │   │   │   └── ConnectionViewModel.java
│   │   │   ├── component/
│   │   │   │   ├── MessageBubble.java
│   │   │   │   ├── UserAvatar.java
│   │   │   │   └── StatusBadge.java
│   │   │   └── util/
│   │   │       ├── FxmlLoader.java
│   │   │       └── FxThread.java
│   │   ├── main/resources/com/distributedchat/desktop/
│   │   │   ├── fxml/
│   │   │   │   ├── login-view.fxml
│   │   │   │   ├── chat-view.fxml
│   │   │   │   ├── settings-view.fxml
│   │   │   │   ├── components/
│   │   │   │   │   ├── message-bubble.fxml
│   │   │   │   │   ├── user-avatar.fxml
│   │   │   │   │   └── status-badge.fxml
│   │   │   │   └── dialogs/
│   │   │   │       ├── connection-dialog.fxml
│   │   │   │       └── about-dialog.fxml
│   │   │   ├── css/
│   │   │   │   ├── design-tokens.css
│   │   │   │   ├── base.css
│   │   │   │   ├── layout.css
│   │   │   │   ├── components.css
│   │   │   │   ├── chat.css
│   │   │   │   ├── login.css
│   │   │   │   └── themes/
│   │   │   │       ├── dark.css
│   │   │   │       └── light.css
│   │   │   ├── fonts/
│   │   │   ├── icons/
│   │   │   └── images/
│   │   └── test/java/com/distributedchat/desktop/
│   │
├── scripts/
│   ├── build-all.ps1
│   ├── run-node-a.ps1
│   ├── run-node-b.ps1
│   ├── run-desktop-client.ps1
│   └── package-desktop.ps1
│
└── deployment/
    ├── node-a/
    │   └── application.properties.example
    ├── node-b/
    │   └── application.properties.example
    └── client/
        └── client.properties.example
```

**Không commit** `target/`, file `.class`, thông tin mật khẩu thật hoặc file cấu hình production chứa credentials. Các file `*.properties.example` chỉ chứa giá trị mẫu; mỗi máy tạo file cấu hình riêng khi triển khai.

---

## 3. Trách nhiệm của từng module

### 3.1 `chat-contract`

Chứa những thứ hai phía phải hiểu giống nhau:
- `ChatMessage` và các DTO truyền qua RMI.
- `ChatClientService` cho client gọi.
- `ServerSyncService` cho server peer gọi.
- Các enum/trạng thái được truyền giữa các node.

Không đặt JavaFX, FXML, JDBC hay code giao diện trong module này.

### 3.2 `chat-server`

Chạy độc lập trên từng máy vật lý:
- Export RMI remote objects trên port được cấu hình.
- Xử lý tin nhắn và validate dữ liệu.
- Ghi CSDL cục bộ qua repository/JDBC.
- Replicate sang peer qua RMI.
- Nhận replication, chống trùng theo `messageId` và không gửi ngược.
- Đồng bộ bù khi peer trở lại.
- Ghi log thành công/thất bại chính xác.

Node A và Node B dùng cùng một JAR server nhưng khác cấu hình node ID, IP peer, database URL và credentials.

### 3.3 `chat-desktop`

Chạy trên máy ảo:
- Hiển thị login, chat room, trạng thái kết nối và node hiện tại.
- Gọi RMI thông qua `RmiClient`/`ChatClientFacade`.
- Polling chạy nền, tuyệt đối không chặn JavaFX Application Thread.
- Cập nhật giao diện bằng `Platform.runLater(...)` hoặc cơ chế JavaFX phù hợp.
- Không kết nối trực tiếp vào MySQL của server; client chỉ gọi API RMI.

---

## 4. Quy ước FXML, Controller và CSS

### FXML
- Chỉ mô tả cấu trúc giao diện và liên kết `fx:id`, `onAction`, controller.
- Không đặt logic nghiệp vụ hoặc code đồng bộ trong FXML.
- Tách view lớn thành component nhỏ khi có lợi, tránh chia quá vụn gây khó quản lý.

### Controller
- Chịu trách nhiệm xử lý sự kiện UI và điều phối service.
- Không viết SQL hoặc logic replication trong controller.
- Không thực hiện lời gọi RMI chặn trên JavaFX Application Thread.
- Dọn dẹp listener, polling task và executor khi view/app đóng.

### CSS
- Đặt màu, font, spacing, border radius, shadow giả lập, hover/focus/disabled state ở stylesheet.
- Dùng class CSS có ý nghĩa như `.app-shell`, `.sidebar`, `.message-bubble`, `.message-bubble--own`, `.connection-badge`, `.primary-button`.
- Hạn chế inline style trong controller; không gọi `setStyle(...)` rải rác.
- Dùng design token tập trung để đổi theme và chỉnh đồng bộ toàn app.
- JavaFX CSS không hỗ trợ toàn bộ tính năng của web CSS; tránh phụ thuộc các thuộc tính web-only như CSS Grid/Flexbox hay pseudo-element không được JavaFX hỗ trợ.

### Component tái sử dụng
Tách component khi nó có giao diện, trạng thái hoặc hành vi lặp lại, ví dụ:
- `MessageBubble`
- `UserAvatar`
- `StatusBadge`
- `ConnectionDialog`

Không tạo component riêng cho mọi `Label` hoặc `HBox`; mức độ trừu tượng phải giúp code dễ đọc hơn.

---

## 5. Định hướng UI/UX: đẹp, hiện đại và nhất quán

Mục tiêu là giao diện desktop trông như một sản phẩm thực tế, không giống form bài tập mặc định. Đẹp không chỉ là màu sắc; cần có hierarchy, spacing, trạng thái và tương tác rõ ràng.

### 5.1 Hướng thiết kế đề xuất

- **Phong cách:** Modern desktop messenger, tối giản, premium, rõ ràng.
- **Theme mặc định:** Dark theme; có thể hỗ trợ light theme nếu đủ thời gian.
- **Màu nền:** charcoal/graphite thay vì đen tuyệt đối ở mọi khu vực.
- **Accent:** violet hoặc electric blue, dùng có tiết chế cho hành động chính và trạng thái focus.
- **Typography:** font dễ đọc; dùng một font chính và một font phụ nếu thực sự cần.
- **Layout:** sidebar điều hướng/thông tin phòng, vùng hội thoại trung tâm, header trạng thái, composer cố định phía dưới.
- **Motion:** chuyển trạng thái nhẹ, ngắn; không thêm animation gây rối hoặc ảnh hưởng hiệu năng.

Đây là định hướng khởi đầu. Hãy chốt palette và typography trong `UI_UX_GUIDELINES.md` trước khi viết CSS hàng loạt.

### 5.2 Màn hình chính

**Login View**
- Tên/logo sản phẩm và mô tả ngắn.
- Username input, trường địa chỉ server hoặc lựa chọn Node A/Node B.
- Nút kết nối chính, loading state và lỗi rõ ràng.
- Không hardcode IP thật trong FXML; đọc từ cấu hình hoặc cho người dùng chọn server.

**Chat View**
- Header hiển thị tên phòng và trạng thái kết nối.
- Hiển thị rõ server hiện tại, ví dụ `CONNECTED · NODE A`.
- Danh sách tin nhắn có phân biệt tin của mình và tin của người khác.
- Tên người gửi, nội dung, thời gian hiển thị vừa đủ.
- Composer có input, nút gửi, trạng thái đang gửi và validation.
- Banner/toast thông báo mất kết nối hoặc đang đồng bộ lại.
- Có empty state khi chưa có tin nhắn và loading state khi đang tải lịch sử.

**Settings/Connection View**
- Địa chỉ host, port Registry, tên binding hoặc lựa chọn node theo thiết kế API.
- Trạng thái kết nối và nút reconnect.
- Không hiển thị password database cho client; client không được kết nối trực tiếp vào DB.

### 5.3 Quy tắc visual quality

- Dùng spacing scale nhất quán, ví dụ 4/8/12/16/24/32 px.
- Không dùng quá nhiều màu accent, gradient, glow hoặc shadow.
- Kiểm tra độ tương phản chữ và trạng thái disabled.
- Button cần có default, hover, pressed, focused và disabled state.
- Text dài phải wrap hợp lý; cửa sổ thay đổi kích thước không làm vỡ layout.
- Ưu tiên `BorderPane`, `VBox`, `HBox`, `GridPane`, `StackPane`, `ScrollPane`, `ListView` và các layout container JavaFX phù hợp.
- Dùng `SplitPane` khi người dùng cần thay đổi độ rộng panel.
- Không cố định toàn bộ giao diện bằng tọa độ tuyệt đối hoặc kích thước cứng.
- Thử ở độ phân giải phổ biến của VM và khi phóng to/thu nhỏ cửa sổ.

---

## 6. Quy ước đặt tên

| Thành phần | Quy ước | Ví dụ |
|---|---|---|
| Java class | `PascalCase` | `ChatController` |
| Method/field | `camelCase` | `sendMessage()` |
| Constant | `UPPER_SNAKE_CASE` | `DEFAULT_POLL_INTERVAL_MS` |
| FXML view | `kebab-case-view.fxml` | `chat-view.fxml` |
| Component FXML | `kebab-case.fxml` | `message-bubble.fxml` |
| CSS | `kebab-case.css` | `design-tokens.css` |
| CSS class | `kebab-case` | `.message-bubble` |
| Resource path | lowercase theo module | `/com/distributedchat/desktop/fxml/chat-view.fxml` |

---

## 7. Quy tắc kết nối RMI từ JavaFX

- Tất cả remote call phải chạy ngoài JavaFX Application Thread.
- Dùng `ExecutorService`/`ScheduledExecutorService` có quản lý cho network và polling.
- Khi có kết quả, cập nhật UI bằng `Platform.runLater(...)` hoặc JavaFX Task/Service.
- Có timeout/chiến lược retry hợp lý; tránh retry liên tục không giới hạn.
- Khi app đóng, dừng polling và shutdown executor.
- Hiển thị trạng thái `Connecting`, `Connected`, `Disconnected`, `Retrying` và thông báo lỗi dễ hiểu.
- Không tuyên bố gửi thành công nếu remote call thất bại.
- Client kết nối tới IP LAN của server, không dùng `127.0.0.1` cho kết nối liên máy.
- Không nhúng credentials của database vào desktop client.

---

## 8. Quy tắc CSDL và replication

Mỗi node có database riêng:
- Node A → Database A.
- Node B → Database B.

Luồng gửi:
1. Client gọi `postMessage()` qua RMI.
2. Server kiểm tra đầu vào, tạo UUID.
3. Server ghi vào database cục bộ.
4. Sau khi lưu thành công, server trả trạng thái phù hợp và lên lịch replicate sang peer.
5. Peer nhận tin, kiểm tra `message_id` duy nhất, insert nếu chưa tồn tại và phát cho client cục bộ.
6. Peer không replicate ngược bản tin đã nhận.

Cần thống nhất chính sách khi database ghi thành công nhưng replication thất bại. Với lab, có thể ghi log, retry có backoff và đồng bộ bù sau reconnect. Nếu cần bảo đảm không mất yêu cầu replication khi process crash, nên bổ sung transactional outbox; đây là phần nâng cao, không bắt buộc cho bản đầu tiên.

---

## 9. Maven module dependencies

Quan hệ module dự kiến:

```text
chat-contract
   ↑             ↑
   |             |
chat-server   chat-desktop
```

- `chat-server` phụ thuộc `chat-contract`.
- `chat-desktop` phụ thuộc `chat-contract`.
- `chat-contract` không phụ thuộc hai module còn lại.
- Parent `pom.xml` quản lý Java version và dependency versions.
- JavaFX dependencies/plugin chỉ đặt trong `chat-desktop`.
- JDBC driver chỉ cần ở `chat-server`.
- Không đưa JavaFX runtime vào server JAR nếu server không dùng UI.

Nên chọn một Java LTS phù hợp với môi trường máy lab (ví dụ Java 21) và dùng nhất quán trên các máy. Cấu hình Maven JavaFX plugin để chạy desktop bằng `mvn javafx:run`; cấu hình Maven Shade/assembly hoặc `jpackage` khi cần đóng gói phân phối. Với RMI, phải bảo đảm JAR client và server có chung contract classes tương thích.

---

## 10. Thứ tự triển khai giao diện và code

1. Tạo parent Maven và ba module.
2. Hoàn thiện `chat-contract` trước.
3. Xây dựng server console tối giản và kiểm thử RMI/replication/database.
4. Tạo JavaFX application và theme CSS cơ bản.
5. Thiết kế `login-view.fxml` và `LoginController`.
6. Thiết kế `chat-view.fxml`, `ChatController` và các component dùng chung.
7. Kết nối UI tới `ChatClientFacade` và kiểm tra network chạy nền.
8. Thêm polling, loading/error/empty states và reconnect.
9. Tinh chỉnh UI ở nhiều kích thước cửa sổ.
10. Đóng gói và chạy thử trên VM.
11. Triển khai server JAR lên hai máy vật lý và demo hai chiều.

Không nên làm UI hoàn chỉnh trước khi xác định API client/server và trạng thái lỗi; hãy tạo giao diện đẹp nhưng gắn với luồng nghiệp vụ thực tế, tránh chỉ làm mockup.

---

## 11. Tiêu chí nghiệm thu cấu trúc

- [ ] Có parent Maven và ba module `chat-contract`, `chat-server`, `chat-desktop`.
- [ ] JavaFX/FXML/CSS chỉ nằm trong `chat-desktop`.
- [ ] DTO và remote interfaces nằm trong `chat-contract`.
- [ ] Server dùng JDBC để ghi database cục bộ.
- [ ] Client chỉ gọi RMI, không truy cập database trực tiếp.
- [ ] Network/polling không chặn JavaFX Application Thread.
- [ ] FXML không chứa logic nghiệp vụ.
- [ ] CSS tập trung, có design tokens và trạng thái tương tác.
- [ ] Hai server dùng cùng server artifact nhưng cấu hình riêng.
- [ ] Chạy được theo mô hình chấm: hai server trên hai host vật lý, desktop client trên VM.
- [ ] Có log rõ ràng để chứng minh replication hai chiều.
- [ ] Có README hướng dẫn build, cấu hình và chạy.

---

## 12. Nguyên tắc cuối cùng

**Tách rõ ba lớp:**
1. **Presentation:** JavaFX + FXML + CSS.
2. **Communication/business:** Java RMI, service và replication.
3. **Persistence:** JDBC + MySQL/MariaDB.

Giữ đúng ranh giới này giúp giao diện đẹp mà không làm rối logic mạng, đồng thời giúp việc kiểm thử, sửa lỗi và trình bày kiến trúc trước giảng viên rõ ràng hơn.
