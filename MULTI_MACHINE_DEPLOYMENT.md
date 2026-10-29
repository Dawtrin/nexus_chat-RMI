
# NEXUS Distributed Chat — Hướng dẫn triển khai hai máy vật lý

## 1. Mục tiêu triển khai

Mô hình mục tiêu:

- Host A chạy Node A và Database A.
- Host B chạy Node B và Database B.
- Client chạy trên VM hoặc máy khác.
- Client kết nối server qua Java RMI.
- Hai server đồng bộ tin nhắn qua RMI.
- Mỗi server ghi dữ liệu vào database cục bộ của chính nó.

Không để client kết nối trực tiếp tới database. Không để Node A ghi trực tiếp vào Database B; Node A phải gửi dữ liệu qua replication API của Node B.

## 2. Điều kiện tiên quyết

Trên cả hai host, kiểm tra:

```powershell
java -version
mvn -version
ipconfig
```

Cần chuẩn bị:

- JDK 21.
- Maven theo cấu hình dự án.
- MySQL hoặc MariaDB tương thích với schema.
- IP LAN thực tế của mỗi host.
- Quyền mở TCP ports cần thiết.
- Source code và dependencies đã build thành công.

Các địa chỉ IP trong tài liệu chỉ là ví dụ. Không sử dụng địa chỉ ví dụ thay cho IP thực tế của bạn.

## 3. Build

Tại thư mục gốc repository:

```powershell
mvn clean package
```

Chỉ tiếp tục nếu build thành công. Lưu output để đối chiếu khi chạy server.

Trước khi chạy script trên một máy mới, kiểm tra script có yêu cầu JAR trong Maven local repository hay không. Nếu có, máy đó cần build/download dependencies trước hoặc cần gói triển khai phù hợp.

## 4. Cấu hình RMI

RMI cần registry để lookup remote object và cần địa chỉ/port mà client hoặc peer có thể truy cập.

Lần rà soát source trước ghi nhận `ServerConfig` có các thuộc tính tương ứng với:

- `server.id`
- `server.advertised.host`
- `server.rmi.registry.port`
- `server.rmi.service.port`
- `server.peer.id`
- `server.peer.host`
- `server.peer.registry.port`
- `server.peer.service.port`

**Claude phải kiểm tra lại tên property và cách đọc config trong source hiện tại trước khi triển khai.** Không tự ý thêm tên cấu hình mới nếu ứng dụng không đọc chúng.

### Cấu hình minh họa

Thay `IP_LAN_HOST_A` và `IP_LAN_HOST_B` bằng IP thật.

| Thuộc tính | Host A | Host B |
|---|---|---|
| `server.id` | `server-a` | `server-b` |
| `server.advertised.host` | `IP_LAN_HOST_A` | `IP_LAN_HOST_B` |
| `server.rmi.registry.port` | `1099` | `2099` |
| `server.rmi.service.port` | `1100` | `2100` |
| `server.peer.id` | `server-b` | `server-a` |
| `server.peer.host` | `IP_LAN_HOST_B` | `IP_LAN_HOST_A` |
| `server.peer.registry.port` | `2099` | `1099` |
| `server.peer.service.port` | `2100` | `1100` |

Các port trên phù hợp với cấu hình mặc định của script đã được ghi nhận trong lần rà soát trước. Chúng **không phải cam kết rằng source hiện tại chắc chắn chỉ dùng các port này**. Phải kiểm tra `RmiRegistryManager`, remote export và scripts trước khi mở firewall.

Nếu replication remote object dùng một port riêng, phải bổ sung port đó vào bảng cấu hình và firewall. Không suy ra rằng chat service và replication service luôn dùng chung port.

### Lưu ý về RMI advertised host

RMI có thể trả về stub chứa hostname/port được advertise bởi server. Vì vậy, chỉ cấu hình được registry host là chưa đủ nếu remote object advertise địa chỉ không thể truy cập.

- Host A phải advertise địa chỉ mà Host B và client có thể truy cập.
- Host B phải advertise địa chỉ mà Host A và client có thể truy cập.
- Không dùng `127.0.0.1` làm advertised host trong deployment đa máy.
- Không chỉ kiểm tra kết nối registry; phải thử remote method call thực tế.

## 5. Database riêng cho từng node

Trên Host A:
1. Khởi động MySQL/MariaDB.
2. Tạo database riêng.
3. Tạo user DB với quyền tối thiểu cần thiết.
4. Chạy schema của dự án.
5. Cấu hình Node A dùng database này.

Trên Host B, thực hiện tương tự nhưng cấu hình Node B dùng Database B.

File schema được ghi nhận trong source ở:

`chat-server/src/main/resources/db/db-schema.sql`

Hãy kiểm tra file thực tế trước khi chạy, đặc biệt nếu database đã có dữ liệu.

Ví dụ tạo database:

```sql
CREATE DATABASE nexus_chat
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Tên database có thể giống nhau trên hai host vì đó là hai database instance riêng. Không cấu hình cả hai node cùng ghi vào một database từ xa nếu mục tiêu là kiểm thử replication giữa hai DB riêng biệt.

Không commit password thật vào repository. Dùng environment variables hoặc file cấu hình cục bộ không được đưa lên Git.

## 6. Firewall và kiểm tra TCP

Mở các TCP ports mà cấu hình RMI thực tế yêu cầu. Giới hạn nguồn truy cập trong phạm vi subnet demo nếu có thể.

Các script được ghi nhận trước đó dùng các port mặc định sau:

- Node A registry: `1099`.
- Node A service: `1100`.
- Node B registry: `2099`.
- Node B service: `2100`.

Xác minh lại source trước khi sử dụng danh sách này. Nếu có port riêng cho replication remote object, phải mở port đó nữa.

Từ Host B, kiểm tra các port của A:

```powershell
Test-NetConnection IP_LAN_HOST_A -Port 1099
Test-NetConnection IP_LAN_HOST_A -Port 1100
```

Từ Host A, kiểm tra các port của B:

```powershell
Test-NetConnection IP_LAN_HOST_B -Port 2099
Test-NetConnection IP_LAN_HOST_B -Port 2100
```

Thay địa chỉ ví dụ bằng IP thật. Kết quả TCP thành công chỉ chứng minh có thể kết nối tới port đó; nó không chứng minh remote interface hoặc replication hoạt động đúng.

Không mở MySQL/MariaDB ra LAN nếu server chỉ cần truy cập database cục bộ.

## 7. Scripts và cách khởi động

Source có các script chạy Node A và Node B được ghi nhận trong lần rà soát trước, gồm:

- `scripts/run-node-a.ps1`
- `scripts/run-node-b.ps1`

Trước khi chạy, kiểm tra nội dung script và xác nhận:
- Classpath đúng.
- Module/JAR đã build.
- Server ID và port đúng.
- Peer host là IP của host đối diện.
- Advertised host đúng.
- Database config đúng.
- Không có giá trị localhost được sử dụng làm peer trong deployment đa máy.

Nếu script chưa nhận IP/peer host từ tham số hoặc file cấu hình, hãy sửa script để hỗ trợ cấu hình theo môi trường thay vì hardcode IP vào code Java.

Trình tự khởi động:

1. Khởi động Database A và Database B.
2. Kiểm tra schema trên hai DB.
3. Cấu hình riêng cho A và B.
4. Khởi động Node A.
5. Khởi động Node B.
6. Kiểm tra log và peer health.
7. Khởi động client kết nối A.
8. Khởi động client kết nối B.

## 8. Kiểm thử sau triển khai

### Test 1 — Chat hai chiều

1. Alice kết nối A.
2. Alice gửi message.
3. Bob kết nối B.
4. Bob xác nhận nhận được message.
5. Bob trả lời.
6. Alice xác nhận nhận được reply.

### Test 2 — Peer offline

1. Dừng Node B.
2. Gửi nhiều message từ Alice qua A.
3. Xác nhận message được lưu trên DB A.
4. Xác nhận outbox có message chưa giao nếu database/outbox đang hoạt động.
5. Khởi động lại B.
6. Xác minh message được đồng bộ và không duplicate.

### Test 3 — Restart khi còn outbox

1. Tạo tình huống outbox còn pending.
2. Restart server.
3. Kiểm tra outbox vẫn tồn tại.
4. Xác minh worker tiếp tục xử lý sau khi peer reachable.

Không kết luận durable outbox hoạt động chỉ vì dashboard hiển thị số pending. Hãy đối chiếu database và log.

## 9. Checklist trước demo

- [ ] Có IP LAN thật của Host A và Host B.
- [ ] Hai server chạy trên hai máy vật lý khác nhau.
- [ ] Node A và Node B dùng database riêng.
- [ ] Schema đã được apply trên cả hai database.
- [ ] Advertised host đúng.
- [ ] Peer host trỏ tới IP host đối diện.
- [ ] Không dùng `127.0.0.1` làm địa chỉ peer liên máy.
- [ ] Firewall mở đúng các RMI ports.
- [ ] Client A kết nối A.
- [ ] Client B kết nối B.
- [ ] Replication A → B hoạt động.
- [ ] Replication B → A hoạt động.
- [ ] Kiểm tra duplicate message.
- [ ] Tắt/bật peer và kiểm tra retry/catch-up.
- [ ] Restart khi outbox còn pending.
- [ ] Có log và ảnh chụp/query DB làm bằng chứng.

## 10. Xử lý sự cố

| Triệu chứng | Kiểm tra |
|---|---|
| Không lookup được registry | IP, registry port, firewall, registry có chạy không |
| Lookup được nhưng remote call lỗi | Advertised host, exported port, firewall, remote object |
| Peer không reachable | Peer config, binding name, registry/service ports |
| Database fallback | JDBC URL, DB service, credentials, driver, schema |
| Outbox không giảm | Peer health, lỗi remote call, retry worker, outbox state |
| Message chỉ xuất hiện trên một DB | Transaction, outbox, peer config, catch-up/checkpoint |
| Message duplicate | Message ID, unique constraint, idempotent insert, UI dedup |
| Client reconnect mãi | Retry coordinator, RMI lookup, session registration, log |
| Dashboard báo healthy sai | Monitoring API, lỗi fetch, cached/stale state |

Kết quả cuối cùng phải được xác minh bằng test trên hai máy vật lý, không chỉ bằng việc server khởi động thành công.
