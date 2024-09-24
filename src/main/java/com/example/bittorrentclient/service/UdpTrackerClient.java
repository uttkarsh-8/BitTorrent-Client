package com.example.bittorrentclient.service;

import com.example.bittorrentclient.model.Peer;
import com.example.bittorrentclient.model.Torrent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class UdpTrackerClient {

    private static final Logger logger = LoggerFactory.getLogger(UdpTrackerClient.class);
    private static final int CONNECT_ACTION = 0;
    private static final int ANNOUNCE_ACTION = 1;
    private static final long CONNECTION_ID = 0x41727101980L;
    private static final int TIMEOUT = 15000;

    public List<Peer> getPeersFromUdpTracker(String trackerUrl, Torrent torrent, String clientPeerId, int clientPort) throws IOException {
        logger.info("Attempting to get peers from UDP tracker: {}", trackerUrl);
        URI uri = URI.create(trackerUrl);
        InetAddress address = InetAddress.getByName(uri.getHost());
        int port = uri.getPort();

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);
            logger.debug("Created UDP socket with timeout: {} ms", TIMEOUT);

            long connectionId = sendConnectRequest(socket, address, port);
            logger.debug("Received connection ID: {}", connectionId);

            return sendAnnounceRequest(socket, address, port, connectionId, torrent, clientPeerId, clientPort);
        }
    }

    private long sendConnectRequest(DatagramSocket socket, InetAddress address, int port) throws IOException {
        logger.debug("Sending connect request to {}:{}", address.getHostAddress(), port);
        ByteBuffer connectRequest = ByteBuffer.allocate(16);
        connectRequest.putLong(CONNECTION_ID);
        connectRequest.putInt(CONNECT_ACTION);
        connectRequest.putInt(new Random().nextInt());

        DatagramPacket packet = new DatagramPacket(connectRequest.array(), connectRequest.capacity(), address, port);
        socket.send(packet);
        logger.debug("Connect request sent");

        byte[] receiveBuffer = new byte[16];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        socket.receive(receivePacket);
        logger.debug("Received connect response");

        ByteBuffer receiveData = ByteBuffer.wrap(receivePacket.getData());
        int action = receiveData.getInt();
        int transactionId = receiveData.getInt();
        if (action != 0 || transactionId != CONNECT_ACTION) {
            logger.error("Invalid connect response. Action: {}, Transaction ID: {}", action, transactionId);
            throw new IOException("Invalid connect response");
        }

        return receiveData.getLong();
    }

    private List<Peer> sendAnnounceRequest(DatagramSocket socket, InetAddress address, int port, long connectionId,
                                           Torrent torrent, String clientPeerId, int clientPort) throws IOException {
        logger.debug("Sending announce request to {}:{}", address.getHostAddress(), port);
        ByteBuffer announceRequest = ByteBuffer.allocate(98);
        announceRequest.putLong(connectionId);
        announceRequest.putInt(ANNOUNCE_ACTION);
        announceRequest.putInt(new Random().nextInt());
        System.arraycopy(hexStringToByteArray(torrent.getInfoHash()), 0, announceRequest.array(), 16, 20);
        System.arraycopy(clientPeerId.getBytes(), 0, announceRequest.array(), 36, 20);
        announceRequest.putLong(0); // downloaded
        announceRequest.putLong(torrent.getTotalSize()); // left
        announceRequest.putLong(0); // uploaded
        announceRequest.putInt(0); // event
        announceRequest.putInt(0); // IP address
        announceRequest.putInt(new Random().nextInt()); // key
        announceRequest.putInt(-1); // num_want
        announceRequest.putShort((short) clientPort);

        DatagramPacket packet = new DatagramPacket(announceRequest.array(), announceRequest.capacity(), address, port);
        socket.send(packet);
        logger.debug("Announce request sent");

        byte[] receiveBuffer = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        socket.receive(receivePacket);
        logger.debug("Received announce response");

        ByteBuffer receiveData = ByteBuffer.wrap(receivePacket.getData());
        int action = receiveData.getInt();
        int transactionId = receiveData.getInt();
        if (action != 1 || transactionId != ANNOUNCE_ACTION) {
            logger.error("Invalid announce response. Action: {}, Transaction ID: {}", action, transactionId);
            throw new IOException("Invalid announce response");
        }

        int interval = receiveData.getInt();
        int leechers = receiveData.getInt();
        int seeders = receiveData.getInt();
        logger.debug("Announce response details - Interval: {}, Leechers: {}, Seeders: {}", interval, leechers, seeders);

        List<Peer> peers = new ArrayList<>();
        while (receiveData.remaining() >= 6) {
            byte[] ipBytes = new byte[4];
            receiveData.get(ipBytes);
            String ip = InetAddress.getByAddress(ipBytes).getHostAddress();
            int peerPort = receiveData.getShort() & 0xffff;

            Peer peer = new Peer();
            peer.setIp(ip);
            peer.setPort(peerPort);
            peer.setPeerId(""); // UDP doesn't provide peer_id
            peer.setTorrent(torrent);
            peers.add(peer);
            logger.debug("Added peer: {}:{}", ip, peerPort);
        }

        return peers;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}