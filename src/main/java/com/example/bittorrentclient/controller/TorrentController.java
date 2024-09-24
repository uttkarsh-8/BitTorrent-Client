package com.example.bittorrentclient.controller;

import com.example.bittorrentclient.model.Torrent;
import com.example.bittorrentclient.model.Peer;
import com.example.bittorrentclient.service.TorrentFileParser;
import com.example.bittorrentclient.service.TrackerCommunicationService;
import com.example.bittorrentclient.repository.TorrentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/torrent")
public class TorrentController {

    private final TorrentFileParser torrentFileParser;
    private final TrackerCommunicationService trackerCommunicationService;
    private final TorrentRepository torrentRepository;

    @Autowired
    public TorrentController(TorrentFileParser torrentFileParser,
                             TrackerCommunicationService trackerCommunicationService,
                             TorrentRepository torrentRepository) {
        this.torrentFileParser = torrentFileParser;
        this.trackerCommunicationService = trackerCommunicationService;
        this.torrentRepository = torrentRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadTorrent(@RequestParam("file") MultipartFile file) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("temp", ".torrent");
            file.transferTo(tempFile);
            Torrent torrent = torrentFileParser.parseTorrentFile(tempFile);
            return ResponseEntity.ok("Torrent uploaded successfully. ID: " + torrent.getId());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error parsing torrent file: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @GetMapping("/{id}/peers")
    public ResponseEntity<?> getPeers(@PathVariable Long id) {
        try {
            Torrent torrent = torrentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Torrent not found"));

            String clientPeerId = UUID.randomUUID().toString().substring(0, 20);
            int clientPort = 6881; // Example port, you might want to make this configurable

            List<Peer> peers = trackerCommunicationService.requestPeers(torrent, clientPeerId, clientPort);
            return ResponseEntity.ok(peers);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error retrieving peers: " + e.getMessage());
        }
    }

    @PostMapping("/test-tracker")
    public ResponseEntity<?> testTrackerCommunication(@RequestParam String trackerUrl,
                                                      @RequestParam String infoHash,
                                                      @RequestParam long totalSize) {
        try {
            Torrent testTorrent = new Torrent();
            testTorrent.setInfoHash(infoHash);
            testTorrent.setTotalSize(totalSize);
            testTorrent.getTrackerUrls().add(trackerUrl);

            String clientPeerId = UUID.randomUUID().toString().substring(0, 20);
            int clientPort = 6881;

            List<Peer> peers = trackerCommunicationService.requestPeers(testTorrent, clientPeerId, clientPort);
            return ResponseEntity.ok("Successfully communicated with tracker. Found " + peers.size() + " peers.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error communicating with tracker: " + e.getMessage());
        }
    }
}