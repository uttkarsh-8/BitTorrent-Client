package com.example.bittorrentclient.service;

import com.example.bittorrentclient.model.Torrent;
import com.example.bittorrentclient.model.Piece;
import com.example.bittorrentclient.model.TorrentFile;
import com.example.bittorrentclient.repository.TorrentRepository;
import org.libtorrent4j.TorrentInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Service
public class TorrentFileParser {

    private final TorrentRepository torrentRepository;

    @Autowired
    public TorrentFileParser(TorrentRepository torrentRepository) {
        this.torrentRepository = torrentRepository;
    }

    @Transactional
    public Torrent parseTorrentFile(File torrentFile) throws IOException {
        byte[] torrentBytes = Files.readAllBytes(torrentFile.toPath());
        TorrentInfo torrentInfo = TorrentInfo.bdecode(torrentBytes);

        Torrent torrent = new Torrent();
        torrent.setName(torrentInfo.name());
        torrent.setInfoHash(torrentInfo.infoHash().toHex());
        torrent.setTotalSize(torrentInfo.totalSize());

        // Set tracker URLs
        Set<String> trackers = extractTrackersFromMagnet(torrentInfo.makeMagnetUri());
        torrent.setTrackerUrls(trackers);

        // Set pieces
        for (int i = 0; i < torrentInfo.numPieces(); i++) {
            Piece piece = new Piece();
            piece.setIndex(i);
            piece.setHash(torrentInfo.hashForPiece(i).toHex());
            piece.setDownloaded(false);
            piece.setTorrent(torrent);
            torrent.getPieces().add(piece);
        }

        // Set files
        for (int i = 0; i < torrentInfo.numFiles(); i++) {
            TorrentFile file = new TorrentFile();
            file.setPath(torrentInfo.files().filePath(i));
            file.setSize(torrentInfo.files().fileSize(i));
            file.setTorrent(torrent);
            torrent.getFiles().add(file);
        }

        return torrentRepository.save(torrent);
    }

    private Set<String> extractTrackersFromMagnet(String magnetUri) {
        Set<String> trackers = new HashSet<>();
        String[] parts = magnetUri.split("&");
        for (String part : parts) {
            if (part.startsWith("tr=")) {
                String tracker = URLDecoder.decode(part.substring(3), StandardCharsets.UTF_8);
                trackers.add(tracker);
            }
        }
        return trackers;
    }
}