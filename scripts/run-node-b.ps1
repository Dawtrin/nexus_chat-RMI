# Launch NEXUS Server Node B
Write-Host "=========================================" -ForegroundColor Magenta
Write-Host " Starting NEXUS Server Node B (Port 2099)" -ForegroundColor Magenta
Write-Host "=========================================" -ForegroundColor Magenta

mvn -pl chat-server exec:java "-Dexec.mainClass=com.nexus.chat.server.ServerApplication" "-Dexec.args=--server.id=server-b --server.rmi.registry.port=2099 --server.rmi.service.port=2100 --server.peer.id=server-a --server.peer.registry.port=1099 --server.peer.service.port=1100"
