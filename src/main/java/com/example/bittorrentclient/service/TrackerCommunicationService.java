package com.example.bittorrentclient.service;

import com.example.bittorrentclient.model.Torrent;
import com.example.bittorrentclient.model.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TrackerCommunicationService {

    private static final Logger logger = LoggerFactory.getLogger(TrackerCommunicationService.class);
    private final RestTemplate restTemplate;
    private final UdpTrackerClient udpTrackerClient;

    @Autowired
    public TrackerCommunicationService(RestTemplate restTemplate, UdpTrackerClient udpTrackerClient) {
        this.restTemplate = restTemplate;
        this.udpTrackerClient = udpTrackerClient;
    }

    public List<Peer> requestPeers(Torrent torrent, String clientPeerId, int clientPort) {
        List<Peer> peers = new ArrayList<>();

        logger.info("Requesting peers for torrent: {}", torrent.getInfoHash());
        for (String trackerUrl : torrent.getTrackerUrls()) {
            logger.debug("Attempting to communicate with tracker: {}", trackerUrl);
            try {
                List<Peer> trackerPeers = requestPeersFromTracker(trackerUrl, torrent, clientPeerId, clientPort);
                logger.info("Retrieved {} peers from tracker: {}", trackerPeers.size(), trackerUrl);
                peers.addAll(trackerPeers);
            } catch (Exception e) {
                logger.error("Error communicating with tracker: {} - {}", trackerUrl, e.getMessage(), e);
            }
        }

        logger.info("Total peers found: {}", peers.size());
        return peers;
    }

    private List<Peer> requestPeersFromTracker(String trackerUrl, Torrent torrent, String clientPeerId, int clientPort) {
        URI uri = URI.create(trackerUrl);
        String scheme = uri.getScheme();

        if ("udp".equalsIgnoreCase(scheme)) {
            return requestPeersFromUdpTracker(trackerUrl, torrent, clientPeerId, clientPort);
        } else if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            return requestPeersFromHttpTracker(trackerUrl, torrent, clientPeerId, clientPort);
        } else {
            logger.warn("Unsupported tracker protocol: {}", scheme);
            return new ArrayList<>();
        }
    }

    private List<Peer> requestPeersFromUdpTracker(String trackerUrl, Torrent torrent, String clientPeerId, int clientPort) {
        try {
            return udpTrackerClient.getPeersFromUdpTracker(trackerUrl, torrent, clientPeerId, clientPort);
        } catch (Exception e) {
            logger.error("Error communicating with UDP tracker: {} - {}", trackerUrl, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<Peer> requestPeersFromHttpTracker(String trackerUrl, Torrent torrent, String clientPeerId, int clientPort) {
        String url = buildTrackerUrl(trackerUrl, torrent, clientPeerId, clientPort);
        logger.debug("Sending HTTP request to tracker: {}", url);
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        List<Peer> peers = new ArrayList<>();
        if (response != null && response.containsKey("peers")) {
            Object peersData = response.get("peers");
            if (peersData instanceof String) {
                // Binary model
                byte[] peersBytes = ((String) peersData).getBytes(StandardCharsets.ISO_8859_1);
                for (int i = 0; i < peersBytes.length; i += 6) {
                    String ip = String.format("%d.%d.%d.%d",
                            peersBytes[i] & 0xFF, peersBytes[i+1] & 0xFF,
                            peersBytes[i+2] & 0xFF, peersBytes[i+3] & 0xFF);
                    int port = ((peersBytes[i+4] & 0xFF) << 8) | (peersBytes[i+5] & 0xFF);
                    Peer peer = new Peer();
                    peer.setIp(ip);
                    peer.setPort(port);
                    peer.setTorrent(torrent);
                    peers.add(peer);
                }
            } else if (peersData instanceof List) {
                // Dictionary model
                List<Map<String, Object>> peerList = (List<Map<String, Object>>) peersData;
                for (Map<String, Object> peerInfo : peerList) {
                    String ip = (String) peerInfo.get("ip");
                    int port = (int) peerInfo.get("port");
                    Peer peer = new Peer();
                    peer.setIp(ip);
                    peer.setPort(port);
                    peer.setTorrent(torrent);
                    peers.add(peer);
                }
            }
        }

        logger.info("Retrieved {} peers from HTTP tracker", peers.size());
        return peers;
    }

    private String buildTrackerUrl(String trackerUrl, Torrent torrent, String peerId, int port) {
        try {
            String encodedInfoHash = URLEncoder.encode(torrent.getInfoHash(), StandardCharsets.ISO_8859_1.toString());
            String encodedPeerId = URLEncoder.encode(peerId, StandardCharsets.ISO_8859_1.toString());

            return String.format("%s?info_hash=%s&peer_id=%s&port=%d&uploaded=0&downloaded=0&left=%d&compact=1&event=started",
                    trackerUrl, encodedInfoHash, encodedPeerId, port, torrent.getTotalSize());
        } catch (UnsupportedEncodingException e) {
            logger.error("Error encoding tracker URL parameters", e);
            return trackerUrl;
        }
    }
}