package com.example.bittorrentclient.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "torrents")
public class Torrent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String infoHash;

    @Column(nullable = false)
    private long totalSize;

    @ElementCollection
    private Set<String> trackerUrls = new HashSet<>();

    @OneToMany(mappedBy = "torrent", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TorrentFile> files = new HashSet<>();

    @OneToMany(mappedBy = "torrent", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Piece> pieces = new HashSet<>();

}
