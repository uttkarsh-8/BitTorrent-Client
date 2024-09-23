package com.example.bittorrentclient.repository;

import com.example.bittorrentclient.model.Torrent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TorrentRepository extends JpaRepository<Torrent, Long> {
    Optional<Torrent> findByInfoHash(String infoHash);
}