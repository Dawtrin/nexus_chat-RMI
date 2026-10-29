package com.nexus.chat.contract.remote;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.dto.ChatRoomDTO;
import com.nexus.chat.contract.dto.ClientSessionDTO;
import com.nexus.chat.contract.dto.OperationResult;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Client-facing RMI interface for chat communication, room management, and sessions.
 */
public interface ChatRemote extends Remote {

    /**
     * Registers a new client session with the server.
     * The server observes the client's network address during the RMI call.
     *
     * @param username Client display name
     * @param clientInstanceId Unique device or instance ID
     * @return OperationResult containing the registered session details
     * @throws RemoteException on network or RMI failure
     */
    OperationResult<ClientSessionDTO> registerClient(String username, String clientInstanceId) throws RemoteException;

    /**
     * Joins the client session to a chat room.
     *
     * @param sessionId Valid active session ID
     * @param roomId Target room ID
     * @return OperationResult indicating success or failure
     * @throws RemoteException on network or RMI failure
     */
    OperationResult<Void> joinRoom(String sessionId, String roomId) throws RemoteException;

    /**
     * Sends a chat message to a room.
     *
     * @param sessionId Active session ID
     * @param roomId Target room ID
     * @param content Message text body
     * @return OperationResult containing the persisted ChatMessageDTO
     * @throws RemoteException on network or RMI failure
     */
    OperationResult<ChatMessageDTO> sendMessage(String sessionId, String roomId, String content) throws RemoteException;

    /**
     * Retrieves messages for a room created after a specific origin sequence number.
     *
     * @param sessionId Active session ID
     * @param roomId Target room ID
     * @param afterSequence Sequence number cutoff (0 for all recent)
     * @return List of messages
     * @throws RemoteException on network or RMI failure
     */
    List<ChatMessageDTO> getMessages(String sessionId, String roomId, long afterSequence) throws RemoteException;

    /**
     * Retrieves all available chat rooms on the server.
     *
     * @param sessionId Active session ID
     * @return List of available rooms
     * @throws RemoteException on network or RMI failure
     */
    List<ChatRoomDTO> getRooms(String sessionId) throws RemoteException;

    /**
     * Sends a periodic heartbeat to maintain session liveness.
     *
     * @param sessionId Active session ID
     * @throws RemoteException on network or RMI failure
     */
    void heartbeat(String sessionId) throws RemoteException;

    /**
     * Explicitly unregisters and disconnects a client session.
     *
     * @param sessionId Active session ID
     * @throws RemoteException on network or RMI failure
     */
    void disconnectClient(String sessionId) throws RemoteException;

    /**
     * Retrieves the identifier of the connected server node.
     *
     * @return Server identifier (e.g. "server-a", "server-b")
     * @throws RemoteException on network or RMI failure
     */
    String getServerId() throws RemoteException;
}
