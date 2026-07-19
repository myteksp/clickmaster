package com.clicker.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "proxy_ports")
public class ProxyPort {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "port_id", nullable = false)
    private Integer portId;

    @Column(name = "pool_key", nullable = false)
    private String poolKey;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Column(name = "server", nullable = false)
    private String server;

    @Column(name = "port", nullable = false)
    private Integer port;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ProxyPort() {}

    public ProxyPort(Integer portId, String poolKey, String countryCode,
                      String server, Integer port, Instant createdAt) {
        this.portId = portId;
        this.poolKey = poolKey;
        this.countryCode = countryCode;
        this.server = server;
        this.port = port;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getPortId() { return portId; }
    public void setPortId(Integer portId) { this.portId = portId; }

    public String getPoolKey() { return poolKey; }
    public void setPoolKey(String poolKey) { this.poolKey = poolKey; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
