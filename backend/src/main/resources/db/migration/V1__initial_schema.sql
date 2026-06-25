CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE sites (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    base_url VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sites_user_id ON sites(user_id);

CREATE TYPE simulation_level AS ENUM ('HTTP_ONLY', 'BROWSER_NAVIGATION', 'FULL_BROWSER');
CREATE TYPE traffic_pattern AS ENUM ('CONSTANT', 'RAMP_UP', 'PULSE', 'REALISTIC_WAVE');
CREATE TYPE campaign_status AS ENUM ('DRAFT', 'READY', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED', 'CANCELLED');

CREATE TABLE campaigns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    status campaign_status NOT NULL DEFAULT 'DRAFT',

    simulation_level simulation_level NOT NULL DEFAULT 'HTTP_ONLY',

    traffic_pattern traffic_pattern NOT NULL DEFAULT 'CONSTANT',
    visits_per_hour INTEGER NOT NULL DEFAULT 100,
    duration_minutes INTEGER NOT NULL DEFAULT 60,
    schedule_cron VARCHAR(100),

    geo_distribution JSONB NOT NULL DEFAULT '[]',
    device_profile JSONB NOT NULL DEFAULT '[]',
    user_agent_config JSONB NOT NULL DEFAULT '{"rotation": "RANDOM"}',

    proxy_config JSONB NOT NULL DEFAULT '{"provider": "ASOCKS"}',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_run_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_campaigns_user_id ON campaigns(user_id);
CREATE INDEX idx_campaigns_site_id ON campaigns(site_id);
CREATE INDEX idx_campaigns_status ON campaigns(status);

CREATE TYPE run_status AS ENUM ('RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED');

CREATE TABLE campaign_runs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    status run_status NOT NULL DEFAULT 'RUNNING',
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMP WITH TIME ZONE,
    total_visits INTEGER NOT NULL DEFAULT 0,
    successful_visits INTEGER NOT NULL DEFAULT 0,
    failed_visits INTEGER NOT NULL DEFAULT 0,
    error_log JSONB,
    stats JSONB
);

CREATE INDEX idx_campaign_runs_campaign_id ON campaign_runs(campaign_id);

CREATE TABLE scenarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scenarios_user_id ON scenarios(user_id);

CREATE TYPE step_action AS ENUM ('LOAD', 'CLICK', 'SCROLL', 'WAIT', 'HOVER', 'TYPE', 'EXTRACT_TEXT', 'SCREENSHOT', 'CUSTOM_JS');

CREATE TABLE scenario_steps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scenario_id UUID NOT NULL REFERENCES scenarios(id) ON DELETE CASCADE,
    order_index INTEGER NOT NULL,
    action_type step_action NOT NULL,
    selector VARCHAR(500),
    value TEXT,
    delay_before_ms INTEGER NOT NULL DEFAULT 0,
    delay_after_ms INTEGER NOT NULL DEFAULT 0,
    probability DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    config JSONB
);

CREATE INDEX idx_scenario_steps_scenario_id ON scenario_steps(scenario_id);

CREATE TABLE campaign_scenarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    scenario_id UUID NOT NULL REFERENCES scenarios(id) ON DELETE CASCADE,
    entry_url VARCHAR(2048),
    weight INTEGER NOT NULL DEFAULT 100,
    UNIQUE (campaign_id, scenario_id)
);

CREATE TABLE visit_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    campaign_run_id UUID NOT NULL REFERENCES campaign_runs(id) ON DELETE CASCADE,
    proxy_address VARCHAR(255),
    country_code VARCHAR(2),
    path VARCHAR(2048),
    status_code INTEGER,
    response_time_ms INTEGER,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_visit_events_run_id ON visit_events(campaign_run_id);
