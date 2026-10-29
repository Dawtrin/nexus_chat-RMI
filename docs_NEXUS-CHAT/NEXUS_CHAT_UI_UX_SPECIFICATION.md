# NEXUS CHAT — UI/UX DESIGN SPECIFICATION
## Distributed Chat Room sử dụng Java RMI, JavaFX, MySQL/MariaDB

> **Loại tài liệu:** Đặc tả thiết kế sản phẩm, UI/UX, design system, màn hình, trạng thái và yêu cầu tích hợp  
> **Phiên bản:** 1.0  
> **Trạng thái:** Design specification — dùng làm cơ sở thiết kế prototype và triển khai code  
> **Tên sản phẩm tạm thời:** NEXUS CHAT  
> **Nền tảng:** JavaFX Desktop Client + JavaFX Admin Dashboard  
> **Kiến trúc hệ thống:** Java RMI, nhiều server, replication hai chiều, JDBC, MySQL/MariaDB  
> **Mục tiêu:** Một ứng dụng chat desktop hoàn chỉnh và một giao diện quản trị/giám sát hệ thống phân tán có thể demo trên nhiều máy vật lý và máy ảo.

---

# MỤC LỤC

1. Tầm nhìn sản phẩm và nguyên tắc thiết kế
2. Phạm vi hệ thống và phân tách Client/Admin
3. Người dùng mục tiêu và các nhiệm vụ chính
4. Brand identity và design system
5. Quy tắc layout desktop và responsive
6. Client Desktop — đặc tả từng màn hình
7. Admin Dashboard — đặc tả từng màn hình
8. IP, thiết bị, client connection và server monitoring
9. Network Topology và Java RMI monitoring
10. Replication Monitor và tính nhất quán dữ liệu
11. Database Health và lưu trữ
12. System Logs, diagnostics và audit trail
13. Trạng thái UI và quy tắc hiển thị dữ liệu thật
14. Component library dùng chung
15. Quy tắc UX, accessibility và keyboard
16. Kiến trúc tích hợp JavaFX với backend
17. Cấu trúc module và tổ chức tài nguyên giao diện
18. Data model và API/RMI contract gợi ý cho monitoring
19. Quy tắc bảo mật và quyền riêng tư
20. Hiệu năng, độ tin cậy và xử lý lỗi
21. Chiến lược kiểm thử UI/UX
22. Tiêu chí nghiệm thu
23. Lộ trình triển khai
24. Prompt triển khai thiết kế và code
25. Checklist cuối cùng

---

# 1. TẦM NHÌN SẢN PHẨM VÀ NGUYÊN TẮC THIẾT KẾ

## 1.1. Tầm nhìn

NEXUS CHAT là ứng dụng nhắn tin desktop chạy trên JavaFX, được xây dựng trên kiến trúc Distributed Chat Room sử dụng Java RMI. Hệ thống gồm nhiều RMI server chạy trên các máy vật lý khác nhau; mỗi server có thể kết nối tới database riêng. Các server đồng bộ tin nhắn để client kết nối tới các server khác nhau vẫn có thể nhìn thấy cuộc trò chuyện chung.

Sản phẩm có hai ứng dụng giao diện riêng biệt:

1. **NEXUS CHAT Desktop Client:** phục vụ người dùng chat hằng ngày.
2. **NEXUS CONTROL Admin Dashboard:** phục vụ quản trị, theo dõi kết nối, giám sát server, replication, database và chẩn đoán sự cố.

Không được trộn các màn hình monitoring chuyên sâu vào luồng chat chính. Client cần đơn giản và tập trung vào giao tiếp; Admin cần giàu thông tin kỹ thuật, nhưng phải có phân cấp rõ ràng và không biến thành một bảng số liệu rối mắt.

## 1.2. Định hướng thiết kế

**Modern Enterprise + Network Intelligence + Premium Dark UI**

Các nguyên tắc:
- Product-first: thao tác chat rõ ràng, nhanh, dễ học.
- Network-aware: thể hiện được đặc điểm distributed system khi người dùng có nhu cầu xem.
- Engineering-grade: thông tin mạng và trạng thái đồng bộ phải dựa trên dữ liệu thật.
- Consistent: mọi màn hình dùng chung design tokens, typography, spacing, icon và quy tắc trạng thái.
- Honest UI: không giả lập số liệu, không báo “Connected” khi chưa xác minh, không báo “Replicated” khi peer chưa xác nhận.
- Desktop-first: tối ưu cho cửa sổ desktop và môi trường demo qua VirtualBox/VMware.
- Maintainable: component tái sử dụng, giao diện tách khỏi business logic.

## 1.3. Quy tắc quan trọng

UI không được tự suy luận rằng hệ thống khỏe chỉ vì database có dữ liệu. Cần phân biệt rõ:

- Client đang kết nối được với server.
- Server RMI đang hoạt động.
- Server peer có thể truy cập được.
- Database đang kết nối được.
- Tin nhắn đã được server tiếp nhận.
- Tin nhắn đã được lưu bền vững.
- Tin nhắn đã được peer nhận và lưu thành công.
- Client khác đã nhận được tin nhắn.

Đây là các trạng thái riêng biệt, có thể xảy ra ở các thời điểm khác nhau.

---

# 2. PHẠM VI HỆ THỐNG VÀ PHÂN TÁCH CLIENT/ADMIN

## 2.1. Ứng dụng 1 — NEXUS CHAT Desktop Client

**Đối tượng:** người dùng chat.

**Mục tiêu:** đăng nhập hoặc nhập tên hiển thị, chọn server, tham gia phòng chat, gửi/nhận tin nhắn, theo dõi trạng thái kết nối cơ bản.

Màn hình:
1. Welcome / Connect to Server
2. Main Chat Workspace
3. Rooms & Members
4. User Profile / Preferences
5. Connection Details (cơ bản)
6. About

Không đưa vào Client các trang chuyên sâu như replication queue, database health, raw RMI logs hay server management.

## 2.2. Ứng dụng 2 — NEXUS CONTROL Admin Dashboard

**Đối tượng:** quản trị viên hoặc người demo/kiểm thử hệ thống.

**Mục tiêu:** quan sát trạng thái toàn hệ thống, xác định client nào đang kết nối, client kết nối tới server nào, địa chỉ IP nào được server quan sát, trạng thái peer, replication, database và log.

Màn hình:
1. Admin Login / Admin Connection
2. Overview Dashboard
3. Network Topology
4. Server Management / Server Details
5. Connected Clients & Sessions
6. Replication Monitor
7. Database Health
8. System Logs & Diagnostics
9. Rooms & Users Management (nếu backend hỗ trợ)
10. Audit Log (nếu có thao tác quản trị)
11. Admin Settings / About

## 2.3. Ranh giới kiến trúc

- `chat-contract`: DTO, remote interfaces và model dùng chung.
- `chat-server`: nghiệp vụ chat, RMI, replication, JDBC, database và cung cấp dữ liệu monitoring.
- `chat-desktop`: ứng dụng Client JavaFX.
- `chat-admin`: ứng dụng Admin JavaFX riêng.

Admin là **ứng dụng quản trị**, không phải chính tiến trình server. Tắt Admin không được làm server ngừng phục vụ chat hoặc dừng replication.

Admin nên lấy thông tin qua các RMI method/API quản trị có xác thực và phân quyền, hoặc một monitoring service riêng. Không cho Admin truy cập trực tiếp database để suy đoán tình trạng kết nối RMI.

---

# 3. NGƯỜI DÙNG MỤC TIÊU VÀ NHIỆM VỤ

## 3.1. Người dùng Client

Cần:
- Kết nối tới server có sẵn hoặc nhập IP/port.
- Biết mình đang kết nối tới server nào.
- Xem phòng chat, tin nhắn và thành viên.
- Gửi/nhận tin nhắn và nhận phản hồi khi thất bại.
- Nhận biết tình trạng kết nối bị mất và có thể thử lại.
- Sử dụng bằng chuột và các phím tắt cơ bản.

## 3.2. Người dùng Admin

Cần:
- Nhìn nhanh trạng thái tổng thể của Server 1, Server 2 và database.
- Xem IP LAN/host của mỗi server.
- Xem IP của các client đã kết nối tới từng server.
- Phân biệt địa chỉ IP quan sát từ socket với IP được cấu hình hoặc IP suy ra từ hostname.
- Biết thời điểm client kết nối lần cuối, thời gian hoạt động của session và server đang phục vụ client.
- Kiểm tra trạng thái kết nối giữa các server.
- Theo dõi tiến trình replication và các tin nhắn chưa đồng bộ nếu hệ thống hỗ trợ queue.
- Tìm log theo thời gian, server, mức độ, loại sự kiện và message ID.
- Chẩn đoán lỗi kết nối hoặc lưu trữ mà không cần đọc log console thủ công.

## 3.3. Các kịch bản demo quan trọng

1. Client A kết nối Server 1; Client B kết nối Server 2.
2. Admin nhìn thấy cả hai server và trạng thái peer link.
3. Admin nhìn thấy mỗi client thuộc server nào và IP được server quan sát.
4. Client A gửi tin; Server 1 lưu và chuyển tin sang Server 2.
5. Client B nhận tin; Admin có thể tìm sự kiện tương ứng trong log.
6. Tắt Server 2 hoặc chặn kết nối peer: Admin phản ánh peer unavailable/unknown và không hiển thị replication thành công giả.
7. Khôi phục Server 2: nếu có retry/backfill thì Admin thể hiện quá trình đồng bộ bù.
8. Tắt Admin: Client và server tiếp tục hoạt động bình thường.

---

# 4. BRAND IDENTITY VÀ DESIGN SYSTEM

## 4.1. Tên và nhận diện

Tên giao diện tạm thời: **NEXUS CHAT**  
Tên Admin: **NEXUS CONTROL**

Ý nghĩa: Nexus là điểm kết nối; Control là khu vực giám sát và quản trị. Đây là tên đề xuất, có thể đổi mà không ảnh hưởng kiến trúc.

Logo:
- Biểu tượng monogram hoặc các node liên kết tối giản.
- Hình khối rõ ở kích thước 24×24 px.
- Tránh logo quá phức tạp hoặc chi tiết nhỏ.
- Không phụ thuộc vào emoji làm logo chính.

Wordmark:
- Viết hoa: `NEXUS CHAT`, `NEXUS CONTROL`.
- Có thể dùng font đậm vừa, tracking nhẹ.
- Logo và wordmark phải dùng được ở sidebar, cửa sổ đăng nhập và About.

## 4.2. Bảng màu

Dark theme là theme mặc định.

| Token | Giá trị gợi ý | Công dụng |
|---|---|---|
| `color-bg` | `#111318` | nền cửa sổ chính |
| `color-surface` | `#191C24` | sidebar, panel, card |
| `color-surface-elevated` | `#252936` | menu, hover, card nổi |
| `color-border` | `#343946` | viền và divider |
| `color-text-primary` | `#F3F4F6` | nội dung chính |
| `color-text-secondary` | `#A0A5B4` | mô tả, metadata |
| `color-accent` | `#A78BFA` | nút chính, selection, điểm nhấn |
| `color-success` | `#36D399` | trạng thái thành công thực tế |
| `color-warning` | `#F5B84B` | trạng thái chờ, cảnh báo |
| `color-error` | `#F87171` | lỗi, mất kết nối |
| `color-info` | `#60A5FA` | thông tin trung tính |

Quy tắc:
- Không dùng màu accent cho tất cả thành phần.
- Không dùng xanh lá chỉ để trang trí.
- Không dựa vào màu duy nhất để truyền đạt trạng thái; luôn có text/icon.
- Giữ độ tương phản đủ cao cho chữ và icon.
- Không dùng hiệu ứng glow/neon tràn lan.
- Có thể bổ sung light theme ở giai đoạn sau, nhưng không nên làm giảm chất lượng dark theme để hỗ trợ quá nhiều theme sớm.

## 4.3. Typography

Font ưu tiên:
- UI: Inter.
- Fallback: Segoe UI, Arial, sans-serif.
- Dữ liệu kỹ thuật: JetBrains Mono hoặc Consolas.

Cỡ chữ gợi ý:
- Page title: 24–28 px.
- Section title: 16–20 px.
- Body: 13–15 px.
- Secondary/help text: 12–13 px.
- Table metadata/log: 12–13 px.
- KPI number: 24–32 px.
- Tránh dùng chữ dưới 11 px cho thông tin quan trọng.

Không dùng quá nhiều font hoặc font weight. Tối đa hai họ font trong một ứng dụng.

## 4.4. Spacing, shape và elevation

Dùng thang spacing 4 px:
- 4 px: khoảng cách rất nhỏ.
- 8 px: khoảng cách giữa icon và label.
- 12 px: khoảng cách trong component.
- 16 px: padding thông thường.
- 24 px: khoảng cách giữa section.
- 32 px: khoảng cách lớn giữa vùng nội dung.

Bo góc:
- Input/button: 6–8 px.
- Card/panel: 8–12 px.
- Dialog: 12–16 px.
- Tránh bo góc quá lớn làm giao diện giống mobile app.

Elevation:
- Ưu tiên khác biệt màu nền và border.
- Shadow chỉ dùng nhẹ ở popover/dialog.
- Không dùng nhiều lớp shadow chồng lên nhau.

## 4.5. Iconography

Dùng một bộ icon line nhất quán, ví dụ Lucide-style hoặc bộ icon tương thích JavaFX. Không trộn icon outline, filled, emoji và icon 3D một cách tùy tiện.

Các icon gợi ý:
- Chat: message-circle
- Rooms: hash
- Members: users
- Server: server
- Network: network
- Database: database
- Replication: arrows-left-right
- Logs: scroll-text
- Settings: settings
- Search: search
- Error: circle-alert
- Success: circle-check
- Retry: refresh-cw

Mỗi icon phải có tooltip hoặc accessible text khi ý nghĩa không rõ.

## 4.6. Component states

Mọi component tương tác phải có:
- Default
- Hover
- Focus
- Pressed
- Disabled
- Loading nếu áp dụng
- Error/invalid nếu áp dụng

Nút nguy hiểm (delete, disconnect, clear log) cần phân biệt rõ và có xác nhận nếu hành động khó hoàn tác.

---

# 5. LAYOUT DESKTOP VÀ RESPONSIVE

## 5.1. Kích thước mục tiêu

Tối ưu trước cho:
- 1366×768
- 1440×900
- 1920×1080

Ứng dụng phải hoạt động ở cửa sổ khoảng 1024×650 trở lên nếu có thể. Không để các nút quan trọng bị cắt ở độ phân giải thấp của máy ảo.

## 5.2. Main layout

Client:
- Navigation rail: khoảng 56–72 px.
- Conversation sidebar: khoảng 260–320 px.
- Chat area: vùng co giãn chính.
- Details panel: khoảng 260–300 px, có thể ẩn.

Admin:
- Sidebar: khoảng 220–260 px, có thể thu gọn.
- Top bar: khoảng 56–64 px.
- Content area: padding 20–24 px.
- KPI cards: grid tự co giãn.
- Tables: có scroll ngang khi cần, không bóp chữ đến mức khó đọc.
- Log detail: drawer hoặc panel phụ.

## 5.3. Quy tắc responsive desktop

- Dưới khoảng 1200 px: tự ẩn hoặc thu gọn panel phụ.
- Không xếp mọi card thành một cột nếu vẫn còn đủ không gian để hiển thị grid.
- Table ưu tiên giữ cột quan trọng; cột phụ có thể ẩn hoặc chuyển vào detail view.
- Sidebar có thể collapse nhưng vẫn giữ icon và tooltip.
- Dialog không vượt quá chiều cao màn hình; phần nội dung dài phải scroll.
- Không dùng kích thước cố định cứng cho mọi cửa sổ nếu có thể dùng layout co giãn.

---

# 6. CLIENT DESKTOP — ĐẶC TẢ MÀN HÌNH

## 6.1. Welcome / Connect to Server

### Mục tiêu
Cho phép người dùng chọn server, kiểm tra kết nối và vào ứng dụng nhanh chóng.

### Layout
- Logo và wordmark ở phần đầu.
- Connection panel.
- Username/authentication panel nếu có.
- Khu vực status và error message.
- Phiên bản ứng dụng ở footer.

### Thành phần
1. Server profile selector: Server 1, Server 2, Custom.
2. Host/IP input.
3. RMI Registry Port.
4. Service name nếu hệ thống có dùng.
5. Username/display name.
6. Password chỉ khi backend hỗ trợ authentication.
7. `Test Connection`.
8. `Connect & Continue`.
9. Status indicator.
10. Link/section xem lỗi chi tiết.

### UX
- Validate host và port trước khi kết nối.
- Khi đang kết nối, disable nút Connect để tránh request trùng.
- Timeout phải có thông báo dễ hiểu.
- Không hiện stack trace trong thông báo chính.
- Có thể cung cấp `Copy details` cho lỗi kỹ thuật.
- Không lưu password dạng plaintext.
- Nếu kết nối thành công, lưu server profile không chứa secret nếu người dùng đồng ý.

### Trạng thái
- Not configured
- Ready to connect
- Connecting
- Connected
- Timeout
- Server unreachable
- Invalid host/port
- Authentication failed (nếu có)

## 6.2. Main Chat Workspace

### Layout
1. Navigation rail bên trái.
2. Conversation/room sidebar.
3. Chat timeline chính.
4. Details panel tùy chọn.

### Navigation rail
- Messages
- Rooms / Members
- Connection details
- Settings
- About

Không đưa các công cụ admin vào navigation của user thông thường.

### Conversation sidebar
- Header `Messages`.
- Search.
- Room list.
- Room name.
- Last message preview.
- Last message time.
- Unread count nếu có.
- Pin/favorite nếu đã triển khai.
- Create/join room chỉ khi backend hỗ trợ.

### Chat header
- Room name.
- Room description nếu có.
- Member count nếu backend cung cấp.
- Menu hành động phù hợp.
- Nhãn server hiện tại hoặc connection status nhỏ, không chiếm trọng tâm.

### Message timeline
Mỗi message có thể bao gồm:
- Avatar hoặc chữ cái đại diện.
- Display name.
- Timestamp.
- Nội dung text.
- Trạng thái gửi của message do người dùng hiện tại gửi.
- Chi tiết kỹ thuật tùy chọn khi debug.

Quy tắc:
- Gom nhóm các tin nhắn liên tiếp cùng người gửi khi hợp lý.
- Không hiển thị timestamp lặp lại quá dày.
- Tin nhắn dài phải wrap đúng.
- Hỗ trợ scroll đến tin mới.
- Khi tải lịch sử cũ, không làm nhảy vị trí đọc hiện tại.
- Escape hoặc render nội dung an toàn; không coi nội dung người dùng là markup tùy ý.

### Message composer
- Text area nhiều dòng.
- Send button.
- Có thể dùng Enter để gửi và Shift+Enter xuống dòng; cần thể hiện rõ trong tooltip/help.
- Disable Send khi nội dung rỗng hoặc request đang ở trạng thái không cho phép.
- Giữ draft khi mất kết nối nếu có thể.
- Không tự gửi lặp tin nhắn sau lỗi nếu chưa có quy tắc idempotency.

### Message states
- Sending
- Accepted by server
- Persisted, nếu server có trả xác nhận lưu bền vững
- Replicated to peer, chỉ khi peer xác nhận
- Failed
- Retry available

Không gộp tất cả thành một dấu tick chung nếu backend không phân biệt được các trạng thái này.

### Empty/loading/error states
- Chưa chọn phòng: hướng dẫn chọn hoặc tham gia phòng.
- Phòng chưa có tin nhắn: gợi ý gửi tin đầu tiên.
- Đang tải: loading indicator nhẹ.
- Mất kết nối: banner nhỏ, không che toàn bộ lịch sử.
- Lỗi gửi: trạng thái gắn với đúng message.
- Không có lịch sử: thông báo phù hợp, không để vùng trắng khó hiểu.

## 6.3. Rooms & Members

Chỉ hiển thị chức năng backend đã hỗ trợ:
- Danh sách room.
- Tìm room.
- Room details.
- Thành viên room.
- Join/leave room.
- Tạo room nếu có.
- Online/offline nếu hệ thống có presence tracking thật.

Không hiển thị trạng thái online chỉ dựa vào việc có message gần đây.

## 6.4. User Profile / Preferences

- Display name.
- Avatar nếu có hỗ trợ.
- Server profile đang dùng.
- Notification preference nếu đã triển khai.
- Theme nếu có.
- Nút disconnect/logout.
- About/version.

Không xây dựng profile phức tạp nếu hệ thống không có tài khoản bền vững.

## 6.5. Connection Details (Client)

Màn hình hoặc popover nhỏ:
- Server name/ID.
- Host/IP và port client đang dùng.
- Thời điểm kết nối.
- Trạng thái hiện tại.
- Lần kiểm tra kết nối gần nhất.
- Thông tin lỗi gần nhất nếu có.
- Nút reconnect nếu backend/client hỗ trợ.

Không cần hiển thị raw replication logs cho user thông thường.

---

# 7. ADMIN DASHBOARD — ĐẶC TẢ MÀN HÌNH

## 7.1. Admin Login / Admin Connection

Mục tiêu:
- Xác thực người có quyền quản trị.
- Kết nối tới monitoring endpoint hoặc server quản trị.
- Không nhầm với username chat thông thường.

Thành phần:
- Logo NEXUS CONTROL.
- Admin username/password hoặc cơ chế xác thực được triển khai.
- Monitoring endpoint/host nếu cấu hình thủ công.
- `Connect`.
- Status, lỗi và version.

Quy tắc bảo mật:
- Không hardcode mật khẩu admin trong source.
- Không để credential mặc định không đổi trong môi trường demo.
- Không lưu password plaintext.
- Nếu không có authentication thực sự, phải gọi rõ là “Admin access for demo” và không giả vờ đã có bảo mật đầy đủ.

## 7.2. Overview Dashboard

### Mục tiêu
Trong 5–10 giây, admin hiểu hệ thống có đang hoạt động hay không.

### KPI cards gợi ý
- Servers reachable / total configured.
- Active client sessions.
- Peer link status.
- Replication backlog/pending count nếu có queue thực.
- Database connection health.
- Recent error count trong một khoảng thời gian xác định.
- Last successful replication timestamp.

Mỗi KPI cần:
- Tên rõ.
- Giá trị.
- Trạng thái hoặc xu hướng nếu có dữ liệu lịch sử.
- Timestamp `Last updated`.
- Tooltip giải thích ý nghĩa và nguồn dữ liệu.

Không hiển thị số liệu mẫu trong môi trường thật. Khi chưa lấy được dữ liệu, hiển thị `—`, `Unknown` hoặc `Not monitored`.

### Khu vực chính
1. Server overview cards.
2. Network topology preview.
3. Active clients summary.
4. Replication summary.
5. Recent critical events.
6. Database health summary.

### Filters
- All servers / Server 1 / Server 2.
- Time range cho logs/events.
- Auto-refresh on/off nếu có.
- Refresh now.

### Refresh behavior
- Có thể polling định kỳ, ví dụ 3–5 giây cho trạng thái nhẹ và 5–15 giây cho bảng log; giá trị cuối cùng phải phù hợp backend.
- Hiển thị thời gian cập nhật gần nhất.
- Không khởi tạo request mới nếu request trước chưa hoàn tất.
- Có thể tạm dừng auto-refresh khi người dùng đang xem chi tiết log hoặc chọn text.

## 7.3. Network Topology

### Mục tiêu
Trực quan hóa các server, client và link giữa chúng.

### Nodes
- Server 1.
- Server 2.
- Client nodes hoặc client groups.
- Database node của từng server nếu có database riêng.
- Monitoring/Admin node nếu hữu ích, nhưng phải phân biệt nó với node phục vụ chat.

### Mỗi server node hiển thị
- Server name/ID.
- Hostname.
- LAN IP được cấu hình hoặc được xác minh.
- RMI registry port/service port.
- Status: Online, Unreachable, Degraded, Unknown.
- Uptime nếu có.
- Database status nếu đã kiểm tra.
- Peer connection status.

### Links
- Client → Server: đường chỉ kết nối client đang dùng.
- Server 1 ↔ Server 2: peer/RMI link.
- Server → Database: đường logical thể hiện cấu hình DB.
- Link status: Connected, Failed, Unknown, Not monitored.
- Last successful check/replication nếu có.
- Hướng mũi tên phải đúng với flow dữ liệu thực tế; link hai chiều không có nghĩa mọi message được gửi vô hạn hai chiều.

### Interactions
- Click node mở details panel.
- Click link mở trạng thái kết nối/replication.
- Tooltip hiển thị địa chỉ, trạng thái và last update.
- Filter server/client/database.
- Có legend cố định giải thích màu và loại đường.
- Nếu chưa có telemetry, thể hiện “Configured topology” hoặc “Status unknown”, không giả lập animation hoạt động.

### Tránh
- Hiệu ứng đường truyền sáng chạy liên tục nếu không gắn với sự kiện thật.
- Đồ họa 3D không cần thiết.
- Đặt IP và port quá nhỏ.
- Dùng màu duy nhất mà không có nhãn trạng thái.

## 7.4. Server Management / Server Details

### Server list table
Các cột đề xuất:
- Status.
- Server ID/name.
- Hostname.
- Configured LAN IP.
- Observed/local bind address nếu backend có cung cấp.
- RMI Registry Port.
- Service name.
- Peer status.
- Database status.
- Uptime.
- Last heartbeat/check.
- Actions.

### Server detail page/drawer
Tabs:
1. Overview
2. Network
3. Replication
4. Database
5. Recent Logs
6. Configuration (read-only mặc định)

Thông tin:
- Server ID.
- Hostname.
- IP address và nguồn của IP.
- RMI port và service name.
- Runtime version nếu backend cung cấp.
- Started at / uptime.
- Last health check.
- Peer server.
- DB connectivity.
- Error summary.
- Last successful replication.

### Actions
Chỉ hiển thị thao tác được backend hỗ trợ và có quyền:
- Refresh status.
- View logs.
- Test peer connection nếu có method thật.
- Graceful shutdown chỉ khi yêu cầu bài tập và backend hỗ trợ rõ ràng.

Không cung cấp nút “Restart server” nếu Admin không có cơ chế an toàn để thực hiện.

## 7.5. Connected Clients & Sessions — MÀN HÌNH BẮT BUỘC

Đây là màn hình quan trọng để demo hệ thống nhiều máy. Admin cần nhìn thấy client nào kết nối, kết nối tới server nào, địa chỉ IP quan sát được và thời điểm hoạt động.

### Bảng client sessions

Cột khuyến nghị:

| Cột | Ý nghĩa |
|---|---|
| Status | Active, Idle, Disconnected hoặc Unknown theo session model |
| Client ID | ID phiên hoặc client duy nhất; không dùng IP làm ID |
| Display name | Tên hiển thị của người dùng nếu có |
| Remote IP | IP nguồn mà server quan sát từ kết nối thực |
| Remote port | Source port nếu có thể lấy từ socket |
| Hostname | Chỉ khi được resolve; có thể trống/Unknown |
| Connected server | Server 1 hoặc Server 2 |
| Connected at | Thời điểm session bắt đầu |
| Last activity | Hoạt động gần nhất được server ghi nhận |
| Session duration | Thời gian phiên hoạt động |
| Protocol/channel | RMI hoặc channel/session type nếu phù hợp |
| Client version | Nếu client gửi thông tin version |
| Actions | View details / logs / disconnect nếu có hỗ trợ |

### IP address — yêu cầu chính xác

Admin cần hiển thị:
- **Remote IP:** địa chỉ nguồn của kết nối mà server quan sát được.
- **Configured server IP:** địa chỉ LAN được cấu hình cho server.
- **Observed server address:** địa chỉ local/bind mà server nhìn thấy, nếu backend có thể cung cấp.
- **Remote port:** source port nếu được thu thập.
- **Hostname:** chỉ là thông tin bổ sung, không thay thế IP.
- **IP family:** IPv4/IPv6 nếu cần.

Không gộp các trường trên thành một trường “IP” mơ hồ.

### Lưu ý kỹ thuật rất quan trọng với Java RMI

Java RMI không tự cung cấp một API chuẩn ở mức business để liệt kê đầy đủ tất cả client đang kết nối và IP của chúng. Để Admin hiển thị client session chính xác, phải thiết kế cơ chế tracking rõ ràng, ví dụ:

1. Client gọi `registerSession` hoặc `connectClient` qua remote interface.
2. Server ghi nhận client ID, username/display name, thời điểm kết nối và server ID.
3. Server lấy địa chỉ remote từ socket/session context đáng tin cậy hoặc dùng một cơ chế kết nối/heartbeat chuyên biệt để quan sát IP.
4. Client gửi heartbeat định kỳ; server cập nhật `lastSeen`.
5. Khi client disconnect bình thường, client gọi `unregisterSession`.
6. Nếu client crash/mất mạng, session được đánh dấu stale/disconnected sau timeout.
7. Admin đọc session registry thông qua monitoring/admin remote interface có quyền.

**Không nên tin IP do client tự gửi lên như một giá trị xác thực.** Client có thể gửi hostname/IP khai báo sai. Nếu lấy IP từ socket của một lời gọi RMI cụ thể, cần đảm bảo cách triển khai thực sự truy cập được remote address ở đúng execution context; không được giả định rằng một remote method thông thường luôn có sẵn IP caller một cách portable.

Với RMI thuần, nếu yêu cầu hiển thị IP chính xác là quan trọng, hãy cân nhắc:
- Một connection/session gateway chuyên biệt ghi nhận socket remote address; hoặc
- Một kênh heartbeat/registration riêng có cơ chế lấy peer address đáng tin cậy; hoặc
- Một lớp transport/monitoring được thiết kế và kiểm thử rõ ràng.

Không chỉ dùng `InetAddress.getLocalHost()` để tìm IP của client: lệnh đó thường trả về địa chỉ local của chính máy đang chạy code, không phải remote client.

### Session detail drawer
Khi click một dòng:
- Client ID.
- Display name.
- Remote IP/port.
- Connected server.
- Connected at.
- Last heartbeat.
- Session duration.
- Last known activity.
- Trạng thái session và lý do stale nếu có.
- Các log liên quan.
- Message/request ID gần nhất nếu có.
- Nút copy thông tin kỹ thuật.
- Nút disconnect chỉ khi server có remote method thực sự để thu hồi session hoặc ngắt kết nối logic.

### Filters
- Server.
- Active/stale/disconnected.
- IP search.
- Username/display name.
- Connected time.
- Client version.
- Sort by last activity/session duration.

### Summary
- Active sessions.
- Sessions by server.
- Stale sessions.
- Distinct remote IP count nếu phù hợp.
- Last updated.

Cần phân biệt “số client” với “số session”; một client có thể reconnect và tạo session mới.

## 7.6. Replication Monitor

### Mục tiêu
Cho biết dữ liệu chat có được đồng bộ giữa các server hay không.

### KPI
- Peer link status.
- Last successful replication.
- Messages replicated trong khoảng thời gian.
- Pending/retry count nếu backend có queue.
- Duplicate messages ignored.
- Replication errors.
- Last reconciliation/backfill nếu đã triển khai.

### Replication events table
- Timestamp.
- Source server.
- Destination server.
- Message ID.
- Event type.
- Status.
- Duration/latency nếu đo được.
- Error summary.
- Retry count nếu có.

### Trạng thái đề xuất
- `Delivered / Persisted`: chỉ khi destination đã xác nhận tương ứng.
- `Pending`: đang chờ.
- `Retrying`: đang thử lại.
- `Failed`: thao tác thất bại theo kết quả thực tế.
- `Duplicate ignored`: message ID đã tồn tại và được xử lý idempotently.
- `Unknown`: chưa có dữ liệu đủ để kết luận.

### Luồng cần thể hiện
1. Server nguồn nhận message.
2. Server nguồn lưu message.
3. Server nguồn yêu cầu peer replicate.
4. Server đích xác thực/kiểm tra message ID.
5. Server đích lưu hoặc bỏ qua duplicate.
6. Server đích xác nhận kết quả.
7. Server nguồn cập nhật trạng thái replication.

Nếu backend hiện tại chỉ fire-and-forget và không có acknowledgement, không được tuyên bố chắc chắn rằng destination đã persist. Hãy sửa contract/backend hoặc hiển thị trạng thái trung thực.

### Actions
- Refresh.
- Retry selected failed event nếu backend hỗ trợ.
- View related logs.
- Copy message ID.
- Filter by server/time/status.

Không cho phép admin sửa/xóa message replication event trực tiếp trong DB qua UI.

## 7.7. Database Health

### Mục tiêu
Hiển thị tình trạng database của từng server, không nhầm lẫn với peer link.

Thông tin:
- Database alias/name; không hiển thị password.
- Server owner (Server 1/2).
- Connection status.
- Last successful health check.
- Last query/persistence error.
- Number of persisted messages nếu query được.
- Connection pool state nếu ứng dụng có pool.
- Schema version nếu có migration/versioning.
- Last successful write timestamp nếu backend ghi nhận.
- Latency nếu thực sự đo được.

Không hiển thị credential, JDBC URL có password, hoặc dữ liệu nhạy cảm.

### Database states
- Connected
- Degraded
- Unreachable
- Unknown
- Not configured

### Actions
- Check connection nếu backend có health-check.
- Refresh.
- View DB-related logs.
- Không cung cấp “repair database” hay destructive actions nếu chưa có cơ chế an toàn.

## 7.8. System Logs & Diagnostics

### Bảng log
- Timestamp có timezone rõ hoặc định dạng nhất quán.
- Level: DEBUG, INFO, WARN, ERROR.
- Source: CLIENT, SERVER-1, SERVER-2, RMI, REPLICATION, DATABASE, ADMIN.
- Event/category.
- Message.
- Correlation ID / request ID.
- Message ID nếu liên quan chat.
- Server ID.
- Remote IP nếu liên quan session và được phép ghi nhận.

### Filters
- Time range.
- Severity.
- Source server.
- Category.
- Keyword search.
- Message ID.
- Client ID/session ID.
- IP address.
- Correlation ID.

### Detail panel
- Full message.
- Stack trace nếu cần.
- Related events.
- Copy.
- Export.
- Open related client/server/session detail.

### UX
- Log mới xuất hiện ở đầu hoặc cuối phải nhất quán; nên mặc định newest-first trong table.
- Khi auto-refresh, không làm mất lựa chọn hoặc vị trí người dùng.
- Có thể pause auto-refresh.
- Không tự clear log server chỉ vì người dùng bấm clear view.
- Phân biệt `Clear view` (xóa hiển thị cục bộ) với `Clear persisted logs` (hành động quản trị nguy hiểm, không nên có trong scope mặc định).
- Redact password, token, secret và thông tin nhạy cảm.
- Có giới hạn số bản ghi tải mỗi lần và pagination/virtualization nếu log nhiều.

### Log minh họa

```text
10:24:03.121 INFO  SERVER-1     RMI service started
10:24:06.432 INFO  CLIENT       Client session registered
10:24:12.018 INFO  REPLICATION  Replication request submitted
10:24:12.044 INFO  SERVER-2     Message persisted successfully
10:25:01.500 WARN  SERVER-1     Peer health check timed out
```

Các dòng trên chỉ là ví dụ thiết kế. Chỉ ghi log tương ứng khi sự kiện thực sự xảy ra.

## 7.9. Rooms & Users Management

Chỉ triển khai khi backend có các nghiệp vụ tương ứng.

Room table:
- Room ID.
- Name.
- Member count.
- Created at.
- Status.
- Last activity.

User table:
- User ID.
- Display name.
- Account status nếu có.
- Last seen nếu có presence tracking.
- Session count.
- Actions theo quyền.

Không tạo giả tính năng ban/kick/role management nếu server chưa có model và remote methods tương ứng.

## 7.10. Audit Log

Nếu Admin có thể thay đổi cấu hình, disconnect session hoặc thao tác quản trị, nên ghi:
- Actor/admin ID.
- Action.
- Target.
- Timestamp.
- Result.
- Correlation ID.
- Reason nếu cần.

Audit log khác với system log: audit log tập trung vào hành động quản trị và trách nhiệm giải trình.

## 7.11. Admin Settings / About

- Auto-refresh intervals.
- Timezone/display format.
- Default server filter.
- Log retention/display limits.
- UI theme nếu hỗ trợ.
- App version.
- Java/JavaFX version nếu lấy được.
- Build identifier.
- Monitoring endpoint profile.
- About project/credits.

Không để Admin settings thay đổi cấu hình server trực tiếp nếu chưa có API và xác nhận an toàn.

---

# 8. IP, THIẾT BỊ, CLIENT CONNECTION VÀ SERVER MONITORING

## 8.1. Quy tắc hiển thị IP

Tách riêng tối thiểu các loại địa chỉ:

1. **Client Remote IP:** địa chỉ nguồn server quan sát từ kết nối của client.
2. **Client Remote Port:** source port nếu có.
3. **Server Configured LAN IP:** IP LAN được cấu hình/quảng bá của server.
4. **Server Bind Address:** địa chỉ interface server bind, nếu có.
5. **RMI Advertised Hostname/IP:** giá trị `java.rmi.server.hostname` hoặc giá trị cấu hình tương đương.
6. **Peer Server Address:** địa chỉ peer server dùng để kết nối.
7. **Hostname:** thông tin phụ, có thể không resolve được.

Không hiển thị một địa chỉ cấu hình như thể đó chắc chắn là địa chỉ remote quan sát được.

## 8.2. IP không phải định danh client

Không dùng IP làm primary key:
- Nhiều client có thể cùng IP do NAT hoặc cùng một máy.
- DHCP có thể làm IP thay đổi.
- Một client có thể reconnect.
- IPv4/IPv6 có thể cùng xuất hiện.
- Proxy/NAT có thể làm server chỉ nhìn thấy địa chỉ trung gian.

Dùng `clientId`/`sessionId` riêng; IP là metadata.

## 8.3. Hiển thị IP trong UI

- Dùng font monospace.
- Cho phép copy bằng nút hoặc context menu.
- Hỗ trợ tìm kiếm theo IP.
- IPv6 dài cần wrap hoặc tooltip.
- Nếu chưa xác định: `Unknown`; không điền IP giả.
- Nếu là loopback: ghi rõ `127.0.0.1 (loopback)` hoặc `::1`, không nhầm với IP LAN.
- Nếu địa chỉ lấy từ cấu hình: thêm label `Configured`.
- Nếu lấy từ remote socket: label `Observed remote address`.
- Thể hiện timestamp lần quan sát cuối.

## 8.4. Client session lifecycle

Backend nên định nghĩa lifecycle rõ:
- Registered/Connected
- Active
- Idle (nếu có quy tắc)
- Stale (quá thời gian heartbeat)
- Disconnected
- Unknown

Đừng coi client offline ngay lập tức chỉ vì một heartbeat bị trễ. Dùng timeout có cấu hình và thể hiện `Last seen`.

## 8.5. Heartbeat

Nếu triển khai heartbeat:
- Client gửi heartbeat theo chu kỳ phù hợp.
- Server cập nhật `lastSeen`.
- Admin lấy session registry.
- Nếu quá timeout, session đánh dấu stale.
- Khi client reconnect, dùng session mới hoặc refresh session theo quy tắc rõ.
- Không gửi heartbeat quá dày gây tải không cần thiết.
- Không để heartbeat chạy mãi sau khi app đóng.

---

# 9. NETWORK TOPOLOGY VÀ JAVA RMI MONITORING

## 9.1. Topology phải phản ánh kiến trúc thực

Ví dụ logic:
- Client A → Server 1.
- Client B → Server 2.
- Server 1 ↔ Server 2.
- Server 1 → Database A.
- Server 2 → Database B.
- Admin Dashboard → Monitoring interface.

Mũi tên thể hiện quan hệ/kênh thực tế, không phải mọi loại request đều dùng chung một đường vật lý.

## 9.2. Server health

Cần phân biệt:
- Process running (nếu có cơ chế xác minh).
- RMI registry reachable.
- Remote service reachable.
- Peer replication reachable.
- Database connected.
- Last successful application-level health check.

Một server có thể đang chạy process nhưng RMI service không truy cập được; database có thể hoạt động trong khi peer replication lỗi.

## 9.3. Heartbeat / health check

- Health check có timeout.
- Không chặn UI thread.
- Ghi thời điểm kiểm tra.
- Nếu check thất bại, giữ last-known state và chuyển sang trạng thái phù hợp.
- Có `Unknown` nếu chưa kiểm tra được.
- Tránh đổi Online/Offline liên tục do một lần timeout; có thể dùng threshold hoặc hysteresis.

## 9.4. RMI-specific notes

- Dùng fixed ports nếu cần cấu hình firewall rõ ràng.
- `java.rmi.server.hostname` phải là địa chỉ client/peer thực sự truy cập được.
- Không dùng `localhost` làm endpoint cho kết nối từ máy khác.
- Admin hiển thị RMI registry port và service endpoint.
- Log lỗi RMI theo loại: timeout, connection refused, marshal/unmarshal, remote exception, name lookup failure.
- Không hiển thị stack trace cho mọi người dùng; chỉ trong diagnostics.
- RMI remote call có thể bị block/lâu; dùng timeout và executor phù hợp.

---

# 10. REPLICATION MONITOR VÀ TÍNH NHẤT QUÁN DỮ LIỆU

## 10.1. Nguyên tắc replication

- Message ID duy nhất (UUID hoặc ID ổn định).
- Replication idempotent.
- `postMessage` và `replicateMessage` có trách nhiệm khác nhau.
- Method nhận replication không được gửi ngược lại vô hạn.
- Có kết quả acknowledgement nếu UI muốn khẳng định peer đã lưu.
- Retry/backoff nếu cần.
- Đồng bộ bù/backfill nếu yêu cầu khôi phục sau outage.
- Log có correlation ID/message ID để trace end-to-end.

## 10.2. Phân biệt trạng thái message

Client UI và Admin phải dùng cùng định nghĩa:
- `SUBMITTED`: request đã được gửi.
- `ACCEPTED`: server nhận xử lý.
- `PERSISTED_LOCAL`: server nguồn đã lưu.
- `REPLICATION_PENDING`: chờ peer.
- `REPLICATED`: peer xác nhận lưu hoặc đã xử lý thành công theo contract.
- `FAILED`: thất bại theo kết quả được xác định.
- `DUPLICATE_IGNORED`: message ID đã tồn tại, duplicate được bỏ qua đúng cách.

Tên trạng thái thực tế có thể khác nhưng semantics phải được ghi rõ trong contract.

## 10.3. Không overclaim

Nếu server nguồn không nhận được acknowledgement từ peer:
- Không đánh dấu replicated.
- Có thể hiển thị `Pending` hoặc `Unknown`.
- Ghi lỗi và thời gian lần thử cuối.
- Retry nếu backend hỗ trợ.

Nếu backend chưa có queue:
- Không hiển thị pending count như số đo chính xác.
- Có thể chỉ hiển thị peer health và recent replication events đã được log.

## 10.4. Idempotency và duplicate

Admin có thể hiển thị số duplicate ignored nếu backend đếm chính xác. Duplicate không nhất thiết là lỗi; trong hệ thống retry, duplicate có thể là tình huống được xử lý an toàn.

---

# 11. DATABASE HEALTH VÀ LƯU TRỮ

## 11.1. Database cards

Mỗi server có thể có database riêng:
- Server 1 → DB A.
- Server 2 → DB B.

Mỗi card:
- Server owner.
- DB alias.
- Connection state.
- Last health check.
- Last successful write.
- Persisted message count nếu query được.
- Recent error.
- Query latency nếu đo thực tế.

## 11.2. Quy tắc an toàn

- Không hiển thị password hoặc secret.
- Không cho Admin nhập SQL tùy ý trong scope mặc định.
- Không cung cấp nút xóa database.
- Health check chỉ dùng query an toàn, nhẹ.
- Tách DB connectivity khỏi replication status.
- DB Connected không đồng nghĩa hai database đã đồng bộ.

---

# 12. SYSTEM LOGS, DIAGNOSTICS VÀ AUDIT TRAIL

## 12.1. Structured logging

Nên có các trường:
- timestamp
- level
- source
- serverId
- clientId/sessionId nếu có
- remoteIp nếu cần và được phép
- eventType
- correlationId
- messageId
- message
- exception type/stack trace khi cần

## 12.2. Correlation

Dùng `correlationId` cho một request/flow xuyên qua nhiều thành phần nếu có thể. Với tin nhắn, `messageId` giúp tìm được:
- Client submission.
- Server nhận.
- Local persistence.
- Replication request.
- Peer persistence/ack.
- Client nhận message.

## 12.3. Redaction

Không ghi:
- Password.
- Authentication token.
- Secret.
- JDBC password.
- Nội dung nhạy cảm không cần thiết.
- Thông tin riêng tư vượt ngoài nhu cầu debug.

IP address có thể là dữ liệu nhạy cảm theo môi trường triển khai; chỉ hiển thị cho người được phân quyền và chỉ giữ log theo retention phù hợp.

---

# 13. TRẠNG THÁI UI VÀ QUY TẮC HIỂN THỊ DỮ LIỆU THẬT

## 13.1. Màu và nhãn

| State | Màu | Nhãn gợi ý |
|---|---|---|
| Healthy / Connected | Success | Connected / Healthy |
| Connecting / Pending | Warning hoặc accent | Connecting / Pending |
| Degraded | Warning | Degraded |
| Failed / Unreachable | Error | Failed / Unreachable |
| Unknown | Neutral | Unknown |
| Not monitored | Neutral | Not monitored |
| Disconnected | Neutral hoặc Error tùy ngữ cảnh | Disconnected |

## 13.2. Timestamp

- Dùng định dạng nhất quán, ví dụ `2026-10-29 10:24:03`.
- Xác định timezone; hiển thị rõ khi có thể gây nhầm lẫn.
- Relative time (“2 minutes ago”) chỉ là phụ trợ.
- Dùng cùng một clock policy cho log và event.
- Tránh so sánh timestamp của nhiều máy nếu chưa xử lý sai lệch đồng hồ.

## 13.3. Loading / Empty / Error

Mọi màn hình có truy vấn dữ liệu phải có:
- Loading state.
- Empty state.
- Error state.
- Stale data state nếu giữ lại dữ liệu cũ trong khi refresh thất bại.
- Last updated timestamp.

Nếu refresh thất bại, không xóa toàn bộ số liệu cũ rồi thay bằng 0. Hiển thị dữ liệu lần cuối kèm cảnh báo `Refresh failed — showing last known data`.

## 13.4. Không dùng dữ liệu giả

- Mock data chỉ dùng trong prototype/demo mode được đánh dấu rõ.
- Không dùng số liệu mẫu trong production mode.
- Nếu server không cung cấp metric, UI phải hiển thị `Not available`.
- Không hiển thị latency, uptime, queue length nếu không đo/ghi nhận.
- Không tự động chuyển trạng thái thành success chỉ vì request không ném exception nếu response không xác nhận thao tác.

---

# 14. COMPONENT LIBRARY DÙNG CHUNG

Các component nên tái sử dụng giữa Client và Admin khi phù hợp:

## 14.1. Shared components
- `AppLogo`
- `AppHeader`
- `SidebarNavigation`
- `PageHeader`
- `StatusBadge`
- `ConnectionStatusIndicator`
- `ServerStatusCard`
- `MetricCard`
- `SearchField`
- `FilterBar`
- `DataTable`
- `PaginationControls`
- `TimestampLabel`
- `CopyableValue`
- `EmptyState`
- `LoadingState`
- `ErrorBanner`
- `ConfirmDialog`
- `ToastNotification`
- `DetailsDrawer`
- `LogLevelBadge`

## 14.2. Client-specific
- `ConversationList`
- `RoomListItem`
- `MessageBubble`
- `MessageTimeline`
- `MessageComposer`
- `MessageDeliveryStatus`
- `MemberList`
- `ReconnectBanner`

## 14.3. Admin-specific
- `TopologyNode`
- `TopologyLink`
- `ReplicationEventRow`
- `ClientSessionTable`
- `ClientSessionDetails`
- `ServerDetailsPanel`
- `DatabaseHealthCard`
- `LogViewer`
- `EventTimeline`
- `HealthSummary`

## 14.4. Quy tắc component

- Component nhận data/state qua property hoặc view model, không tự gọi RMI trực tiếp.
- Component không chứa business logic.
- StatusBadge dùng một enum/mapper chung.
- Các nút phải có action thật; không để button inert.
- Loading state tránh layout shift quá lớn.
- Table hỗ trợ empty/loading/error.
- Tooltip chỉ bổ sung, không thay thế label quan trọng.
- Dialog phải có focus behavior hợp lý và xử lý Escape nếu phù hợp.

---

# 15. UX, ACCESSIBILITY VÀ KEYBOARD

## 15.1. Keyboard

Client:
- `Enter`: gửi tin nếu đã quy định.
- `Shift+Enter`: xuống dòng.
- `Ctrl/Cmd+F`: focus search nếu phù hợp.
- `Esc`: đóng popup/dialog hoặc bỏ focus tùy ngữ cảnh.

Admin:
- `Ctrl/Cmd+F`: focus search/filter khi hợp lý.
- `Esc`: đóng drawer/dialog.
- Tab/Shift+Tab: điều hướng các control.
- Enter/Space: kích hoạt button/selection theo chuẩn.

Không gán shortcut gây xung đột với thao tác nhập tin nhắn.

## 15.2. Accessibility

- Không dùng màu là cách duy nhất để truyền tải trạng thái.
- Button có accessible text.
- Icon-only button có tooltip và accessible label.
- Focus indicator dễ thấy.
- Text tương phản tốt.
- Không dùng animation nhấp nháy.
- Hỗ trợ scaling chữ ở mức hợp lý.
- Bảng phải có header rõ ràng.

## 15.3. Notifications

- Không spam toast cho từng heartbeat.
- Dùng toast cho hành động người dùng vừa thực hiện.
- Dùng persistent banner cho lỗi kết nối nghiêm trọng.
- Dùng badge/counter cho sự kiện mới.
- Không thông báo cùng một lỗi liên tục; có deduplication/throttling.

---

# 16. KIẾN TRÚC TÍCH HỢP JAVAFX VỚI BACKEND

## 16.1. Layering

```text
JavaFX Views (FXML + CSS)
        |
Controllers / ViewModels
        |
Application Services
        |
RMI Client / Admin Remote Client
        |
Remote Interfaces in chat-contract
        |
chat-server
        |
Replication + JDBC + Database
```

## 16.2. Threading

- Không gọi remote method chậm trên JavaFX Application Thread.
- Dùng `ExecutorService`, `Task` hoặc `Service` phù hợp.
- Cập nhật UI qua `Platform.runLater` hoặc cơ chế JavaFX tương ứng.
- Tránh tạo thread mới cho mỗi heartbeat.
- Đóng executor, timer và listeners khi ứng dụng thoát.
- Có timeout và xử lý cancellation.
- Tránh để request cũ ghi đè dữ liệu mới sau khi người dùng đổi filter.

## 16.3. Refresh strategy

- Không polling quá dày.
- Không gửi request trùng nếu request trước đang chạy.
- Hiển thị last-updated.
- Có thể dùng incremental fetch cho logs/events.
- Dùng pagination/virtualization cho bảng lớn.
- Nếu backend không hỗ trợ push event, polling phải được ghi đúng là polling, không quảng bá thành realtime push.

## 16.4. Error mapping

Chuyển exception kỹ thuật thành thông báo dễ hiểu:
- `ConnectException` → server không truy cập được.
- `SocketTimeoutException`/timeout tương đương → quá thời gian chờ.
- `NotBoundException` → không tìm thấy RMI service name.
- `RemoteException` → lỗi giao tiếp remote; xem diagnostics.
- Database exception → lỗi lưu trữ; không được tuyên bố message đã persist.

Giữ nguyên chi tiết exception trong log bảo vệ; UI chính hiển thị message thân thiện và mã/correlation ID nếu có.

---

# 17. CẤU TRÚC MODULE VÀ TỔ CHỨC TÀI NGUYÊN

Cấu trúc đề xuất:

```text
distributed-chat/
├── pom.xml
├── chat-contract/
│   ├── pom.xml
│   └── src/main/java/
│       └── .../
│           ├── dto/
│           ├── model/
│           └── remote/
├── chat-server/
│   ├── pom.xml
│   └── src/main/java/
│       └── .../
│           ├── bootstrap/
│           ├── rmi/
│           ├── service/
│           ├── replication/
│           ├── monitoring/
│           ├── session/
│           ├── repository/
│           └── database/
├── chat-desktop/
│   ├── pom.xml
│   └── src/main/java/
│       └── .../
│           ├── app/
│           ├── controller/
│           ├── service/
│           ├── viewmodel/
│           ├── navigation/
│           └── util/
│   └── src/main/resources/
│       ├── fxml/
│       ├── css/
│       ├── images/
│       ├── icons/
│       └── i18n/
└── chat-admin/
    ├── pom.xml
    └── src/main/java/
        └── .../
            ├── app/
            ├── controller/
            ├── service/
            ├── viewmodel/
            ├── navigation/
            ├── monitoring/
            └── util/
    └── src/main/resources/
        ├── fxml/
        ├── css/
        ├── images/
        ├── icons/
        └── i18n/
```

Tên package thực tế cần tuân theo package base của project.

Quy tắc:
- FXML chỉ mô tả view.
- CSS chứa design tokens và styling.
- Controller xử lý event UI, gọi service.
- Application service điều phối nghiệp vụ phía client.
- RMI client wrapper tập trung lookup/call/timeout/error mapping.
- ViewModel giữ trạng thái trình bày nếu cần.
- Không đặt JDBC logic trong Client/Admin.
- Không để Admin truy cập DB trực tiếp để đọc replication state.
- Server cung cấp monitoring data qua remote interface có xác thực.

---

# 18. DATA MODEL VÀ RMI CONTRACT GỢI Ý CHO MONITORING

Phần này là yêu cầu kiến trúc gợi ý, cần điều chỉnh theo code hiện tại trước khi triển khai.

## 18.1. DTO gợi ý

### `ServerStatusDto`
- `serverId`
- `displayName`
- `hostname`
- `configuredAddress`
- `bindAddress` (nếu xác định được)
- `advertisedRmiHost`
- `registryPort`
- `serviceName`
- `serverState`
- `startedAt`
- `uptimeSeconds`
- `lastHealthCheckAt`
- `peerServerId`
- `peerState`
- `databaseState`
- `lastErrorSummary`

### `ClientSessionDto`
- `sessionId`
- `clientId`
- `displayName`
- `remoteAddress`
- `remotePort`
- `hostname` (nullable)
- `connectedServerId`
- `connectedAt`
- `lastSeenAt`
- `lastActivityAt`
- `sessionState`
- `clientVersion`
- `lastCorrelationId`

`remoteAddress` chỉ được điền là observed remote address nếu backend có cơ chế thu thập đáng tin cậy. Nếu chỉ là client-provided field, phải đặt tên khác, ví dụ `reportedAddress`, và không coi là xác thực.

### `ReplicationStatusDto`
- `sourceServerId`
- `destinationServerId`
- `peerState`
- `lastAttemptAt`
- `lastSuccessAt`
- `pendingCount` (nullable nếu không có queue)
- `retryCount` hoặc metrics tương đương
- `lastError`
- `statusUpdatedAt`

### `ReplicationEventDto`
- `eventId`
- `timestamp`
- `sourceServerId`
- `destinationServerId`
- `messageId`
- `eventType`
- `status`
- `durationMillis` (nullable nếu không đo)
- `retryCount`
- `errorSummary`
- `correlationId`

### `DatabaseHealthDto`
- `serverId`
- `databaseAlias`
- `connectionState`
- `lastCheckAt`
- `lastSuccessfulWriteAt`
- `persistedMessageCount` (nullable nếu không lấy)
- `latencyMillis` (nullable nếu không đo)
- `lastErrorSummary`

### `SystemLogDto`
- `timestamp`
- `level`
- `source`
- `serverId`
- `sessionId`
- `remoteAddress`
- `eventType`
- `correlationId`
- `messageId`
- `message`
- `exceptionSummary`

Không nhất thiết phải có tất cả DTO ngay ở phiên bản đầu. Bắt đầu với các trường backend thực sự có thể cung cấp.

## 18.2. Remote interface gợi ý

Ví dụ về nhóm method, không phải code bắt buộc:

```java
public interface AdminMonitoringRemote extends Remote {
    List<ServerStatusDto> getServerStatuses() throws RemoteException;
    List<ClientSessionDto> getClientSessions() throws RemoteException;
    ReplicationStatusDto getReplicationStatus(String serverId)
        throws RemoteException;
    List<ReplicationEventDto> getReplicationEvents(LogFilterDto filter)
        throws RemoteException;
    List<SystemLogDto> getSystemLogs(LogFilterDto filter)
        throws RemoteException;
}
```

Các method thực tế cần:
- `throws RemoteException`.
- DTO phải serializable nếu truyền qua RMI.
- Có pagination/limit cho logs/events.
- Có authentication/authorization phù hợp.
- Không trả toàn bộ log vô hạn trong một lần gọi.
- Không trả secrets.
- Xác định rõ trường nào nullable và ý nghĩa từng state.

## 18.3. Session registration

Nếu cần theo dõi client:
- Định nghĩa register/unregister/heartbeat trong contract.
- Server tạo hoặc xác thực session ID.
- Có timeout cho stale session.
- Client không thể tự khai báo trạng thái session là “verified”.
- IP remote cần lấy bằng cơ chế đã thiết kế, không lấy từ `InetAddress.getLocalHost()` trên client/server để thay thế.
- Cần test nhiều client cùng IP và reconnect.

## 18.4. Authorization

- Tách quyền user chat và admin.
- Admin methods không được mở công khai không kiểm soát.
- Chỉ admin có quyền xem IP/session/log.
- Không để client thường gọi method quản trị.
- Có thể dùng admin credential/role phù hợp scope đồ án.
- Không coi việc ẩn nút trên UI là biện pháp bảo mật; server phải kiểm tra quyền.

---

# 19. BẢO MẬT VÀ QUYỀN RIÊNG TƯ

- Không hiển thị password, secret hoặc JDBC credential.
- Không tin IP do client tự khai báo.
- Validate dữ liệu nhận từ remote calls.
- Giới hạn số lượng kết quả logs/session.
- Có timeout cho remote calls.
- Không cho phép log injection; escape line breaks/controls phù hợp.
- Không để raw stack trace hiện cho người dùng thông thường.
- Giới hạn quyền xem IP, session và log.
- Ghi audit cho hành động quản trị quan trọng.
- Không lưu password plaintext.
- Nếu triển khai trong mạng demo không an toàn, nêu rõ giới hạn bảo mật của RMI và cân nhắc firewall/network isolation.
- Không coi Java RMI trên LAN là tự động an toàn chỉ vì nằm trong mạng nội bộ.

---

# 20. HIỆU NĂNG, ĐỘ TIN CẬY VÀ XỬ LÝ LỖI

## 20.1. Client
- Không block UI thread.
- Giữ tin nhắn đã tải khi mất kết nối.
- Có retry hợp lý.
- Tránh duplicate send.
- Giữ draft nếu có thể.
- Tránh tạo thread/timer không được giải phóng.

## 20.2. Admin
- Tách refresh của KPI, sessions và logs theo tần suất phù hợp.
- Pagination cho bảng lớn.
- Không tải tất cả log vào memory.
- Debounce search/filter.
- Giữ filter/sort khi refresh.
- Hiển thị last-updated và stale data.
- Không gọi health check quá dày.

## 20.3. Network failure
Khi peer mất kết nối:
- Server không được báo replication thành công khi chưa có xác nhận.
- Admin thể hiện peer state và thời điểm check cuối.
- Client vẫn có thể chat với server hiện tại nếu server cho phép, nhưng cần thông báo về giới hạn đồng bộ.
- Khi reconnect, xử lý retry/backfill nếu đã triển khai.
- Tránh gửi vòng lặp replication giữa hai server.

---

# 21. CHIẾN LƯỢC KIỂM THỬ UI/UX

## 21.1. Client tests
- Kết nối Server 1.
- Kết nối Server 2.
- Username rỗng/invalid.
- IP/port sai.
- Server offline.
- Timeout.
- Gửi tin nhắn rỗng.
- Gửi tin nhắn dài.
- Gửi liên tục.
- Mất mạng khi gửi.
- Nhận tin từ peer server.
- Reconnect.
- Đóng ứng dụng và giải phóng thread.

## 21.2. Admin tests
- Không kết nối được monitoring endpoint.
- Server 1 online, Server 2 offline.
- Peer link lỗi nhưng database vẫn online.
- Database offline nhưng RMI vẫn online.
- Không có client session.
- Nhiều client từ cùng IP.
- Client đổi IP/reconnect.
- Stale heartbeat.
- Không lấy được remote IP.
- Log lớn và filter nhiều lần.
- Auto-refresh khi đang mở detail drawer.
- Admin bị mất kết nối trong khi dashboard đang mở.
- Admin không có quyền xem chi tiết.
- Dữ liệu cũ khi refresh thất bại.

## 21.3. Multi-machine demo
- Server 1 trên máy vật lý A.
- Server 2 trên máy vật lý B.
- Client A/B trên VM hoặc các VM khác nhau.
- Kiểm tra IP LAN, RMI advertised host và firewall.
- Kiểm tra client session hiển thị đúng server.
- Kiểm tra IP quan sát được, không nhầm với IP VM/host khác.
- Gửi tin hai chiều và trace theo message ID.
- Ngắt peer link, quan sát UI.
- Khôi phục link, kiểm tra retry/backfill nếu có.

---

# 22. TIÊU CHÍ NGHIỆM THU

## 22.1. Design
- [ ] Có design tokens dùng chung.
- [ ] Màu, font, spacing, icon nhất quán.
- [ ] Không có JavaFX default look chưa được styling.
- [ ] Có hover/focus/disabled/loading states.
- [ ] Layout không bị vỡ ở 1366×768 và 1920×1080.
- [ ] Có empty/loading/error states.
- [ ] Bảng/log dễ đọc.

## 22.2. Client
- [ ] Có Connect screen.
- [ ] Có Main Chat Workspace.
- [ ] Có room/member screen phù hợp backend.
- [ ] Có trạng thái sending/sent/failed đúng semantics.
- [ ] Có connection status cơ bản.
- [ ] Không block UI thread.
- [ ] Không báo gửi thành công giả.

## 22.3. Admin
- [ ] Có Overview.
- [ ] Có Network Topology.
- [ ] Có Server Details.
- [ ] Có Connected Clients & Sessions.
- [ ] Có cột Remote IP/Remote Port và Connected Server.
- [ ] Có Connected at/Last seen/Session duration.
- [ ] Có Replication Monitor.
- [ ] Có Database Health.
- [ ] Có System Logs & Diagnostics.
- [ ] Có last updated và Unknown/Not monitored state.
- [ ] Không hiển thị dữ liệu giả trong chế độ thật.
- [ ] Không hiển thị secret.
- [ ] Có authorization phía server cho admin operations.

## 22.4. Kiến trúc
- [ ] `chat-contract` chứa DTO/remote interfaces dùng chung.
- [ ] `chat-server` chứa business logic, RMI, replication, JDBC.
- [ ] `chat-desktop` không truy cập DB trực tiếp.
- [ ] `chat-admin` dùng monitoring/admin API/RMI, không đọc DB trực tiếp.
- [ ] Admin không phải dependency bắt buộc để chat hoạt động.
- [ ] Có cơ chế theo dõi session/IP được thiết kế và kiểm thử.
- [ ] Replication có idempotency và trạng thái minh bạch.

---

# 23. LỘ TRÌNH TRIỂN KHAI

## Phase 1 — Audit code và chốt design
1. Kiểm tra project hiện tại.
2. Xác định chức năng đã có và chức năng còn thiếu.
3. Chốt brand, palette, typography, layout.
4. Tạo shared CSS tokens và components.
5. Không xóa chức năng đang hoạt động nếu chưa hiểu dependency.

## Phase 2 — Client MVP
1. Welcome/Connect.
2. Main Chat Workspace.
3. Message states.
4. Room/member UI theo backend.
5. Connection status.
6. Error handling và keyboard behavior.

## Phase 3 — Admin foundation
1. Admin app bootstrap.
2. Admin authentication/authorization phù hợp.
3. Sidebar/top bar.
4. Overview với dữ liệu thật.
5. Server list/detail.
6. Loading/error/unknown states.

## Phase 4 — Monitoring
1. Client session tracking.
2. Remote IP collection theo cơ chế đã kiểm chứng.
3. Connected Clients table.
4. Network topology.
5. Replication Monitor.
6. Database Health.
7. Logs/diagnostics.

## Phase 5 — Hardening
1. Test multi-machine.
2. Test server/peer/database outage.
3. Kiểm tra race condition trong UI refresh.
4. Kiểm tra memory/thread cleanup.
5. Test độ phân giải và keyboard.
6. Viết hướng dẫn chạy demo.

---

# 24. PROMPT TRIỂN KHAI THIẾT KẾ VÀ CODE

Có thể sử dụng phần dưới đây làm prompt cho AI coding agent sau khi cung cấp repository/project zip.

---

## PROMPT

Bạn là Senior Java Engineer, JavaFX UI/UX Engineer, Distributed Systems Engineer và Software Architect.

Hãy đọc toàn bộ repository trước khi sửa code. Dự án là Distributed Chat Room dùng Java RMI, JavaFX, Maven và MySQL/MariaDB. Hệ thống có hai ứng dụng UI riêng:
1. `chat-desktop`: Client chat cho người dùng.
2. `chat-admin`: Admin Dashboard để giám sát hệ thống.
Backend nằm trong `chat-server`; DTO/remote interfaces nằm trong `chat-contract`.

### Mục tiêu

Triển khai giao diện desktop chất lượng cao theo tài liệu `NEXUS_CHAT_UI_UX_SPECIFICATION.md`. Phong cách Modern Enterprise + Network Intelligence + Premium Dark UI. Ưu tiên thiết kế nhất quán, sạch, dễ đọc, hoạt động thực tế và có thể demo trên nhiều máy vật lý/VM.

### Yêu cầu bắt buộc

1. Audit repository và báo cáo ngắn cấu trúc, công nghệ, tính năng hiện có, rủi ro trước khi chỉnh sửa.
2. Không tự ý viết lại toàn bộ backend hoặc phá vỡ các RMI contract đang hoạt động.
3. Không xóa code/chức năng cũ nếu chưa xác định được tác động.
4. Tách Client và Admin thành các ứng dụng/module rõ ràng.
5. Dùng FXML cho view, CSS cho design tokens/style, controller/service/view model theo kiến trúc dự án.
6. Triển khai Client screens: Welcome/Connect, Main Chat Workspace, Rooms/Members nếu backend hỗ trợ, Connection Details, Settings/About.
7. Triển khai Admin screens: Overview, Network Topology, Server Details, Connected Clients & Sessions, Replication Monitor, Database Health, System Logs & Diagnostics.
8. Admin phải hiển thị client sessions với các trường phù hợp: Client ID, display name, Remote IP, Remote port nếu có, Connected server, Connected at, Last seen/last activity, session state và client version nếu có.
9. Phân biệt `Remote IP`, `Configured server IP`, `Bind address`, `Advertised RMI host` và `Hostname`; không gộp thành một IP không rõ nguồn.
10. Không giả định Java RMI tự cung cấp danh sách mọi client và IP. Audit cách transport/session đang được triển khai. Nếu chưa có tracking, thiết kế register/unregister/heartbeat/session registry và một phương án thu thập remote address đáng tin cậy. Không dùng `InetAddress.getLocalHost()` để lấy IP client. Không coi IP client tự khai báo là xác thực.
11. Nếu cần thay đổi `chat-contract`, bổ sung DTO/remote methods một cách tương thích, ghi rõ tác động tới server/client và cập nhật tất cả module liên quan.
12. Admin lấy dữ liệu qua monitoring/admin remote API có phân quyền; không kết nối database trực tiếp để suy luận trạng thái RMI/replication.
13. Không hiển thị số liệu giả trong chế độ thật. Khi thiếu dữ liệu dùng `Unknown`, `Not monitored`, `—` hoặc trạng thái tương đương.
14. Không báo `Replicated` nếu peer chưa xác nhận theo contract.
15. Không block JavaFX Application Thread bằng RMI, JDBC hoặc polling.
16. Dùng executor/task/service phù hợp; dọn thread/timer khi app đóng.
17. Có loading, empty, error, stale-data và success states.
18. Không hiển thị password, token, JDBC credential hoặc secret trong UI/log.
19. Có filter/search/pagination phù hợp cho session/log tables.
20. Tất cả button phải thực hiện hành động thật hoặc được thể hiện rõ là disabled/not implemented; không để button giả.
21. Giữ UI hoạt động ở 1366×768, 1440×900 và 1920×1080.
22. Cập nhật README/run instructions và cấu hình multi-machine cần thiết.
23. Chạy build/test hiện có; sửa compile errors do thay đổi của bạn.
24. Không tuyên bố hoàn tất tính năng nếu chưa triển khai và kiểm chứng.

### Design direction

- Background `#111318`
- Surface `#191C24`
- Elevated surface `#252936`
- Border `#343946`
- Primary text `#F3F4F6`
- Secondary text `#A0A5B4`
- Accent `#A78BFA`
- Success `#36D399`
- Warning `#F5B84B`
- Error `#F87171`
- Font UI Inter/Segoe UI fallback
- Font kỹ thuật JetBrains Mono/Consolas
- Spacing theo bội số 4 px
- Icon line-style thống nhất
- Hạn chế neon/glow/gradient; ưu tiên hierarchy, spacing và typography.

### Quy trình làm việc

Phase A: Audit repository, xác định chức năng và contract hiện có.  
Phase B: Đề xuất file sẽ sửa/thêm và kế hoạch tương thích.  
Phase C: Triển khai shared design system.  
Phase D: Triển khai Client UI.  
Phase E: Triển khai Admin UI.  
Phase F: Bổ sung monitoring/session/IP collection cần thiết ở backend nếu chưa có.  
Phase G: Test build, test UI states, test multi-machine và cập nhật README.

Không dừng ở mockup hoặc screenshot. Hãy triển khai code thực sự trong repository. Nếu một metric hoặc hành động chưa có backend hỗ trợ, hãy đánh dấu chưa hỗ trợ và đề xuất contract/implementation cần bổ sung thay vì tạo dữ liệu giả.

Khi kết thúc, cung cấp:
- Tóm tắt kiến trúc UI mới.
- Danh sách file tạo/sửa.
- Những chức năng đã triển khai thật.
- Những chức năng còn thiếu hoặc phụ thuộc backend.
- Kết quả build/test.
- Hướng dẫn chạy Client và Admin.
- Hướng dẫn demo Server 1, Server 2, client trên VM.
- Những giới hạn còn lại về IP monitoring, RMI, replication và bảo mật.

---

# 25. CHECKLIST CUỐI CÙNG

## Product
- [ ] Client tập trung vào chat.
- [ ] Admin tập trung vào monitoring và quản trị.
- [ ] Hai ứng dụng có chung design language nhưng navigation riêng.
- [ ] Tắt Admin không ảnh hưởng hoạt động của server/client.

## IP & sessions
- [ ] Có danh sách client sessions.
- [ ] Hiển thị remote IP theo nguồn đáng tin cậy.
- [ ] Có remote port nếu thu thập được.
- [ ] Hiển thị server đang phục vụ session.
- [ ] Có connected-at, last-seen và session state.
- [ ] Phân biệt observed IP với configured IP.
- [ ] Xử lý stale session khi client crash.
- [ ] Không dùng IP làm client identity.

## Distributed systems
- [ ] Có topology phản ánh server/client/DB thực tế.
- [ ] Có peer health.
- [ ] Có replication status/events theo backend.
- [ ] Có database health riêng.
- [ ] Có logs và correlation/message ID.
- [ ] Không báo replication success giả.
- [ ] Có retry/backfill nếu phạm vi yêu cầu.
- [ ] Có authorization cho admin monitoring.

## UI/UX
- [ ] Shared design tokens.
- [ ] Consistent typography/icons/spacing.
- [ ] Có loading/empty/error/stale states.
- [ ] Có keyboard/focus states.
- [ ] Table/log dễ đọc và lọc.
- [ ] Không block UI thread.
- [ ] Không có button giả.
- [ ] Test ở độ phân giải desktop/VM.

---

# KẾT LUẬN

NEXUS CHAT nên được phát triển như một sản phẩm gồm **hai ứng dụng bổ trợ nhau**:

- **Client Desktop:** chat rõ ràng, gọn gàng, dễ sử dụng.
- **NEXUS CONTROL Admin Dashboard:** quan sát hệ thống phân tán, IP client, server, kết nối RMI, replication, database và diagnostics.

Điểm nhấn quan trọng nhất của Admin không chỉ là dashboard đẹp, mà là **có thể trả lời chính xác: client nào đang kết nối, từ địa chỉ nào, vào server nào, server peer có hoạt động không và tin nhắn đã được đồng bộ đến đâu**.

Để đạt điều đó, UI/UX và backend monitoring phải được thiết kế cùng nhau. Các thông tin IP, session, heartbeat, replication acknowledgement và database health cần có nguồn dữ liệu rõ ràng; không thể giải quyết chỉ bằng CSS hoặc FXML.

Tài liệu này là đặc tả thiết kế nền tảng. Trước khi coding, cần audit repository hiện tại để map từng yêu cầu vào các chức năng/backend đã có, tránh tạo UI đẹp nhưng không phản ánh đúng hệ thống.
