CREATE TABLE invites (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL,
    usages_limit INT NOT NULL,
    activations_count INT NOT NULL DEFAULT 0,
    valid_until TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    invite_id UUID REFERENCES invites(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE campaigns (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    mode VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    deadline TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE evaluation_criteria (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES campaigns(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    max_score INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE works (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES campaigns(id),
    student_id UUID NOT NULL REFERENCES users(id),
    content_text TEXT,
    file_path VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE work_assignments (
    id UUID PRIMARY KEY,
    work_id UUID NOT NULL REFERENCES works(id),
    reviewer_id UUID NOT NULL REFERENCES users(id),
    assigned_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL UNIQUE REFERENCES work_assignments(id),
    total_score INT,
    feedback TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE review_scores (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES reviews(id),
    criteria_id UUID NOT NULL REFERENCES evaluation_criteria(id),
    score INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Master invite for admin to bootstrap the system
INSERT INTO invites (id, code, role, usages_limit, activations_count, is_active, created_at)
VALUES ('00000000-0000-0000-0000-000000000000', 'MASTER-ADMIN-INVITE-777', 'ADMIN', 10, 0, TRUE, CURRENT_TIMESTAMP);
