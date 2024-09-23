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
@Table(name = "peers")
public class Peer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ip;

    @Column(nullable = false)
    private int port;

    @Column(nullable = false)
    private String peerId;

    @ManyToOne
    @JoinColumn(name = "torrent_id", nullable = false)
    private Torrent torrent;

}
