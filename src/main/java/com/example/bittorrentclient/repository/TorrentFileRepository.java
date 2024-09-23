package com.example.bittorrentclient.repository;

import com.example.bittorrentclient.model.TorrentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TorrentFileRepository extends JpaRepository<TorrentFile, Long> {
    List<TorrentFile> findByTorrentId(Long torrentId);
}