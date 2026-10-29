# Launch NEXUS Server Node A
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Starting NEXUS Server Node A (Port 1099)" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

java -cp "chat-server/target/chat-server-1.0.0-SNAPSHOT.jar;chat-contract/target/chat-contract-1.0.0-SNAPSHOT.jar;$HOME/.m2/repository/org/slf4j/slf4j-api/2.0.12/slf4j-api-2.0.12.jar;$HOME/.m2/repository/ch/qos/logback/logback-classic/1.5.3/logback-classic-1.5.3.jar;$HOME/.m2/repository/ch/qos/logback/logback-core/1.5.3/logback-core-1.5.3.jar;$HOME/.m2/repository/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar;$HOME/.m2/repository/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar" `
    com.nexus.chat.server.ServerApplication `
    --server.id=server-a `
    --server.rmi.registry.port=1099 `
    --server.rmi.service.port=1100 `
    --server.peer.id=server-b `
    --server.peer.registry.port=2099 `
    --server.peer.service.port=2100
