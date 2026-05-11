CREATE TABLE tags (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE user_tags (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tag_id  UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, tag_id)
);

CREATE INDEX idx_user_tags_user ON user_tags(user_id);
CREATE INDEX idx_user_tags_tag  ON user_tags(tag_id);

CREATE TABLE campaign_tags (
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    tag_id      UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (campaign_id, tag_id)
);

CREATE INDEX idx_campaign_tags_campaign ON campaign_tags(campaign_id);
CREATE INDEX idx_campaign_tags_tag      ON campaign_tags(tag_id);

CREATE TABLE invite_tags (
    invite_id UUID NOT NULL REFERENCES invites(id) ON DELETE CASCADE,
    tag_id    UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (invite_id, tag_id)
);

CREATE INDEX idx_invite_tags_invite ON invite_tags(invite_id);
CREATE INDEX idx_invite_tags_tag    ON invite_tags(tag_id);
