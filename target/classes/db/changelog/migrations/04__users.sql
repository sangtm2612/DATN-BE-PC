--liquibase formatted sql

--changeset kinhduanpc:04__users runOnChange:false splitStatements:true endDelimiter:;
--comment Create users and authentication tables

-- ============================================================
-- PHẦN 4: NGƯỜI DÙNG & XÁC THỰC (FR-01, FR-02, FR-03)
-- ============================================================

CREATE TABLE users (
                       id               SERIAL PRIMARY KEY,
                       role             user_role    NOT NULL DEFAULT 'customer',
                       status           user_status  NOT NULL DEFAULT 'active',
                       email            VARCHAR(200) UNIQUE,                    -- NULL nếu đăng ký qua OAuth
                       phone            VARCHAR(20)  UNIQUE,
                       password_hash    VARCHAR(255),                           -- NULL nếu chỉ dùng OAuth
                       full_name        VARCHAR(150),
                       avatar_url       VARCHAR(500),
                       date_of_birth    DATE,
                       gender           VARCHAR(10),                            -- 'male','female','other'
                       email_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
                       phone_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    -- FR-02: Bảo vệ brute-force
                       login_attempts   SMALLINT     NOT NULL DEFAULT 0,        -- Reset về 0 khi đăng nhập thành công
                       locked_until     TIMESTAMP,                              -- NULL = không bị khóa
    -- FR-17: Hóa đơn doanh nghiệp
                       tax_code         VARCHAR(20),
                       company_name     VARCHAR(200),
                       last_login_at    TIMESTAMP,
                       created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
                       updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
                       CONSTRAINT chk_users_credential CHECK (
                           email IS NOT NULL OR phone IS NOT NULL
                           )
);
CREATE INDEX idx_users_email  ON users(email);
CREATE INDEX idx_users_phone  ON users(phone);
CREATE INDEX idx_users_role   ON users(role);
CREATE INDEX idx_users_status ON users(status);

-- FR-02: OAuth login (Google, Facebook)
CREATE TABLE user_oauth (
                            id          SERIAL PRIMARY KEY,
                            user_id     INT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            provider    VARCHAR(30)  NOT NULL,   -- 'google', 'facebook'
                            oauth_id    VARCHAR(200) NOT NULL,   -- ID từ provider
                            email       VARCHAR(200),
                            created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
                            UNIQUE (provider, oauth_id)
);
CREATE INDEX idx_user_oauth_user ON user_oauth(user_id);

-- FR-01/FR-03: Token xác thực (OTP email verify, password reset, refresh JWT)
CREATE TABLE user_tokens (
                             id          SERIAL PRIMARY KEY,
                             user_id     INT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             token       VARCHAR(255) NOT NULL UNIQUE,
                             token_type  VARCHAR(30)  NOT NULL,   -- 'email_verify','phone_otp','password_reset','refresh'
                             expires_at  TIMESTAMP    NOT NULL,
                             used_at     TIMESTAMP,
                             ip_address  VARCHAR(45),             -- Ghi nhận IP tạo token (bảo mật)
                             created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_user_tokens_token  ON user_tokens(token);
CREATE INDEX idx_user_tokens_user   ON user_tokens(user_id);
CREATE INDEX idx_user_tokens_expiry ON user_tokens(expires_at);

-- FR-03b: Địa chỉ giao hàng (tối đa 5 địa chỉ/tài khoản)
CREATE TABLE user_addresses (
                                id             SERIAL PRIMARY KEY,
                                user_id        INT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                full_name      VARCHAR(150) NOT NULL,
                                phone          VARCHAR(20)  NOT NULL,
                                province       VARCHAR(100) NOT NULL,
                                district       VARCHAR(100) NOT NULL,
                                ward           VARCHAR(100) NOT NULL,
                                address_detail VARCHAR(300) NOT NULL,
                                is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
                                created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
                                updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_user_addresses_user ON user_addresses(user_id);
