package com.example.bittorrentclient.controller;

import com.example.bittorrentclient.model.Torrent;
import com.example.bittorrentclient.service.TorrentFileParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
public class TorrentController {

    private final TorrentFileParser torrentFileParser;

    @Autowired
    public TorrentController(TorrentFileParser torrentFileParser) {
        this.torrentFileParser = torrentFileParser;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadTorrent(@RequestParam("file") MultipartFile file) {
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
}