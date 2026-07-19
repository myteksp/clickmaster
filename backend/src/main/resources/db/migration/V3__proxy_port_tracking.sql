CREATE TABLE IF NOT EXISTS proxy_ports (
    id BIGSERIAL PRIMARY KEY,
    port_id INTEGER NOT NULL,
    pool_key VARCHAR(255) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    server VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_proxy_ports_pool_key ON proxy_ports(pool_key);
CREATE INDEX IF NOT EXISTS idx_proxy_ports_port_id ON proxy_ports(port_id);
