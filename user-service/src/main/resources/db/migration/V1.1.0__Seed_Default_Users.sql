-- Seed default users for all roles
-- Passwords are BCrypt-encoded. Change these in production!
--
-- | Role       | Username   | Password      |
-- |------------|------------|---------------|
-- | ADMIN      | admin      | admin123      |
-- | TENDEREE   | tenderee   | tenderee123   |
-- | TENDERER   | tenderer   | tenderer123   |
-- | EVALUATOR  | evaluator  | evaluator123  |
-- | COMMITTEE  | committee  | committee123  |

INSERT INTO users (username, email, password_hash, role, status, created_at, updated_at)
VALUES
    ('admin', 'admin@egov.gov',
     '$2b$10$aQPDP94qMpxqJz7Y.x9TluNVDZ8kQJQ7Mcv.yvm4w53b8o8mVi5Dm',
     'ADMIN', 'ACTIVE', NOW(), NOW()),

    ('tenderee', 'tenderee@egov.gov',
     '$2b$10$Lt8AmcuthvNKphtj4ZZA5eqW/TrlHqDtTA202IdbAumB7HTPykxNG',
     'TENDEREE', 'ACTIVE', NOW(), NOW()),

    ('tenderer', 'tenderer@egov.gov',
     '$2b$10$krypeXH1aPwQeRAROI9IeeajDtQEwh9vNBshAPu3TlNy3cSQJVxdK',
     'TENDERER', 'ACTIVE', NOW(), NOW()),

    ('evaluator', 'evaluator@egov.gov',
     '$2b$10$FBAwCe4qX6jFbmbt3M3EuuKhKj1e9HU2PvpcibHWGN3WoEveFSB/K',
     'EVALUATOR', 'ACTIVE', NOW(), NOW()),

    ('committee', 'committee@egov.gov',
     '$2b$10$6raTGVp3zsvZQ5o.pYCGwe2dH.XkcLS6dtnJ7hZ5J0osp7y0AqQda',
     'COMMITTEE', 'ACTIVE', NOW(), NOW());

-- Create a demo organization for the tenderer
INSERT INTO organizations (name, registration_number, address, contact_person, phone, email, organization_type, status, created_at, updated_at)
VALUES
    ('Demo Vendor Ltd', 'BRN-2024-001', '123 Business Ave, Addis Ababa', 'Demo Contact',
     '+251911000001', 'info@demovendor.com', 'PRIVATE', 'ACTIVE', NOW(), NOW());

-- Link tenderer to the demo organization
INSERT INTO user_organizations (user_id, organization_id, role, created_at, updated_at)
SELECT u.id, o.id, 'OWNER', NOW(), NOW()
FROM users u, organizations o
WHERE u.username = 'tenderer' AND o.registration_number = 'BRN-2024-001';
