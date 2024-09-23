package com.example.bittorrentclient.repository;

import com.example.bittorrentclient.model.Piece;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PieceRepository extends JpaRepository<Piece, Long> {
    List<Piece> findByTorrentId(Long torrentId);
    List<Piece> findByTorrentIdAndIsDownloaded(Long torrentId, boolean isDownloaded);
}