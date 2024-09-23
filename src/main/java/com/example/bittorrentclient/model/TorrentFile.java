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
@Table(name = "torrent_files")
public class TorrentFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private long size;

    @ManyToOne
    @JoinColumn(name = "torrent_id", nullable = false)
    private Torrent torrent;
}