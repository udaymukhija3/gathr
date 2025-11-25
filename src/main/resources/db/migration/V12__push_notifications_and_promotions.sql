-- Push Notifications & Promotions System
-- Enables monetization through partner venue promotions

-- =====================================================
-- DEVICE TOKEN MANAGEMENT
-- =====================================================

CREATE TABLE user_devices (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_token VARCHAR(512) NOT NULL,
    device_type VARCHAR(20) NOT NULL CHECK (device_type IN ('IOS', 'ANDROID', 'WEB')),
    device_name VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(device_token)
);

CREATE INDEX idx_user_devices_user_id ON user_devices(user_id);
CREATE INDEX idx_user_devices_token ON user_devices(device_token);
CREATE INDEX idx_user_devices_active ON user_devices(user_id, is_active) WHERE is_active = TRUE;

-- =====================================================
-- USER NOTIFICATION PREFERENCES
-- =====================================================

CREATE TABLE user_notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE UNIQUE,

    -- Notification type toggles
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    activity_reminders BOOLEAN NOT NULL DEFAULT TRUE,
    chat_messages BOOLEAN NOT NULL DEFAULT TRUE,
    promotional_offers BOOLEAN NOT NULL DEFAULT TRUE,

    -- Frequency controls
    max_promotions_per_day INT NOT NULL DEFAULT 3,

    -- Quiet hours (stored as minutes from midnight, e.g., 1380 = 23:00)
    quiet_hours_start INT, -- null means no quiet hours
    quiet_hours_end INT,

    -- Interest categories for targeted promotions
    interested_categories TEXT[], -- Array of activity categories: SPORTS, FOOD, ART, MUSIC

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_prefs_user ON user_notification_preferences(user_id);

-- =====================================================
-- PARTNER/VENUE ENHANCEMENTS
-- =====================================================

-- Add partner fields to hubs
ALTER TABLE hubs ADD COLUMN is_partner BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE hubs ADD COLUMN partner_tier VARCHAR(20) DEFAULT 'FREE' CHECK (partner_tier IN ('FREE', 'BASIC', 'PREMIUM'));
ALTER TABLE hubs ADD COLUMN contact_email VARCHAR(255);
ALTER TABLE hubs ADD COLUMN contact_phone VARCHAR(50);
ALTER TABLE hubs ADD COLUMN commission_rate DECIMAL(5,2) DEFAULT 0.00; -- percentage
ALTER TABLE hubs ADD COLUMN verified_at TIMESTAMP;

CREATE INDEX idx_hubs_partner ON hubs(is_partner) WHERE is_partner = TRUE;

-- =====================================================
-- PROMOTIONS/OFFERS SYSTEM
-- =====================================================

CREATE TABLE promotions (
    id BIGSERIAL PRIMARY KEY,
    hub_id BIGINT NOT NULL REFERENCES hubs(id) ON DELETE CASCADE,

    -- Promotion details
    title VARCHAR(255) NOT NULL,
    description TEXT,
    terms_conditions TEXT,

    -- Discount configuration
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'BOGO', 'FREE_ITEM')),
    discount_value DECIMAL(10,2), -- percentage or fixed amount
    min_spend DECIMAL(10,2), -- minimum spend to qualify

    -- Targeting
    target_categories TEXT[], -- Activity categories this promo applies to
    target_day_of_week INT[], -- 0=Sunday, 1=Monday, etc. Null = all days
    target_time_start INT, -- Minutes from midnight (e.g., 720 = 12:00)
    target_time_end INT,

    -- Limits
    max_redemptions INT, -- Total limit, null = unlimited
    max_redemptions_per_user INT DEFAULT 1,
    current_redemptions INT NOT NULL DEFAULT 0,

    -- Validity
    starts_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Tracking
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_promotions_hub ON promotions(hub_id);
CREATE INDEX idx_promotions_active ON promotions(is_active, starts_at, expires_at) WHERE is_active = TRUE;
CREATE INDEX idx_promotions_categories ON promotions USING GIN(target_categories);

-- Promo codes for specific promotions
CREATE TABLE promo_codes (
    id BIGSERIAL PRIMARY KEY,
    promotion_id BIGINT NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL UNIQUE,
    max_uses INT,
    current_uses INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_promo_codes_code ON promo_codes(code);
CREATE INDEX idx_promo_codes_promotion ON promo_codes(promotion_id);

-- User-Promotion interactions
CREATE TABLE user_promotions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    promotion_id BIGINT NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    promo_code_id BIGINT REFERENCES promo_codes(id),

    -- Interaction tracking
    viewed_at TIMESTAMP,
    clicked_at TIMESTAMP,
    saved_at TIMESTAMP,
    redeemed_at TIMESTAMP,

    -- Redemption details
    activity_id BIGINT REFERENCES activities(id),
    redemption_amount DECIMAL(10,2),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE(user_id, promotion_id) -- One interaction record per user per promotion
);

CREATE INDEX idx_user_promotions_user ON user_promotions(user_id);
CREATE INDEX idx_user_promotions_promotion ON user_promotions(promotion_id);
CREATE INDEX idx_user_promotions_redeemed ON user_promotions(redeemed_at) WHERE redeemed_at IS NOT NULL;

-- =====================================================
-- NOTIFICATION HISTORY
-- =====================================================

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Notification content
    type VARCHAR(50) NOT NULL, -- ACTIVITY_REMINDER, PROMOTIONAL, CHAT_MESSAGE, SYSTEM
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    data JSONB, -- Additional payload data

    -- Related entities
    activity_id BIGINT REFERENCES activities(id),
    promotion_id BIGINT REFERENCES promotions(id),

    -- Delivery tracking
    scheduled_for TIMESTAMP,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    clicked_at TIMESTAMP,

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'CANCELLED')),
    failure_reason TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_scheduled ON notifications(scheduled_for) WHERE status = 'PENDING';
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_status ON notifications(status);
