package com.nexus.chat.contract.remote;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.dto.OperationResult;
import com.nexus.chat.contract.dto.ServerStatusDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Server-to-server RMI interface for bidirectional message replication and peer health checking.
 */
public interface ReplicationRemote extends Remote {

    /**
     * Receives and idempotently stores a single message replicated from a peer server node.
     *
     * @param message Replicated chat message
     * @param sourceServerId Identifier of the source server sending the replication
     * @return OperationResult indicating whether the message was persisted or duplicate ignored
     * @throws RemoteException on network or RMI failure
     */
    OperationResult<Void> replicateMessage(ChatMessageDTO message, String sourceServerId) throws RemoteException;

    /**
     * Receives and idempotently stores a batch of messages replicated from a peer server node.
     *
     * @param messages List of replicated chat messages
     * @param sourceServerId Identifier of the source server sending the replication
     * @return OperationResult indicating batch delivery status
     * @throws RemoteException on network or RMI failure
     */
    OperationResult<Void> replicateBatch(List<ChatMessageDTO> messages, String sourceServerId) throws RemoteException;

    /**
     * Retrieves messages originating from a specific server after an acknowledged sequence cursor.
     * Used for reconciliation and backfilling after network partition recovery.
     *
     * @param originServerId Origin server whose sequence numbers are being queried
     * @param afterSequence Last known sequence number acknowledged by the peer
     * @param limit Maximum number of records to return
     * @return List of messages
     * @throws RemoteException on network or RMI failure
     */
    List<ChatMessageDTO> getMessagesAfter(String originServerId, long afterSequence, int limit) throws RemoteException;

    /**
     * Responds to peer health ping and returns the peer's operational status.
     *
     * @param queryingServerId Identifier of the server making the ping query
     * @return ServerStatusDTO of the queried peer
     * @throws RemoteException on network or RMI failure
     */
    ServerStatusDTO getPeerStatus(String queryingServerId) throws RemoteException;
}
