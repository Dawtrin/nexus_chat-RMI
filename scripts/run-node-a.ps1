# Launch NEXUS Server Node A
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Starting NEXUS Server Node A (Port 1099)" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

mvn -pl chat-server exec:java "-Dexec.mainClass=com.nexus.chat.server.ServerApplication" "-Dexec.args=--server.id=server-a --server.rmi.registry.port=1099 --server.rmi.service.port=1100 --server.peer.id=server-b --server.peer.registry.port=2099 --server.peer.service.port=2100"
