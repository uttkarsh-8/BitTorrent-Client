package com.example.bittorrentclient.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pieces")
public class Piece {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int index;

    @Column(nullable = false)
    private String hash;

    @Column(nullable = false)
    private boolean isDownloaded;

    @ManyToOne
    @JoinColumn(name = "torrent_id", nullable = false)
    private Torrent torrent;

}