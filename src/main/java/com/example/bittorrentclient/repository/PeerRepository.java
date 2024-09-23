package com.example.bittorrentclient.repository;


import com.example.bittorrentclient.model.Peer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeerRepository extends JpaRepository<Peer, Long> {
    List<Peer> findByTorrentId(Long torrentId);
}