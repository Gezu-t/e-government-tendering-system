-- Seed a demo tender with criteria and items for testing

INSERT INTO tenders (title, description, tenderee_id, type, status, submission_deadline,
                     allocation_strategy, min_winners, max_winners, cutoff_score, is_average_allocation,
                     created_at, updated_at)
VALUES
    ('Supply of Office Equipment for Government Offices',
     'Procurement of office furniture, computers, and supplies for 5 government offices in Addis Ababa. '
     'This tender is open to all pre-qualified vendors with experience in office equipment supply.',
     2, 'OPEN', DATE_ADD(NOW(), INTERVAL 30 DAY),
     'SINGLE', 1, 1, 6.00, false,
     NOW(), NOW()),

    ('IT Infrastructure Upgrade - Phase 1',
     'Design, supply, and installation of network infrastructure including servers, switches, '
     'and cabling for the Ministry of Finance data center.',
     2, 'SELECTIVE', DATE_ADD(NOW(), INTERVAL 45 DAY),
     'COOPERATIVE', 1, 3, 7.00, true,
     NOW(), NOW());

-- Criteria for tender 1
INSERT INTO tender_criteria (tender_id, name, description, type, weight, prefer_higher, active, created_at, updated_at)
VALUES
    (1, 'Unit Price', 'Price per unit of equipment', 'PRICE', 40.00, false, true, NOW(), NOW()),
    (1, 'Delivery Time', 'Delivery timeline in days', 'TIME', 20.00, false, true, NOW(), NOW()),
    (1, 'Quality Certification', 'ISO and quality certifications', 'QUALITY', 25.00, true, true, NOW(), NOW()),
    (1, 'Past Experience', 'Years of experience in similar supply', 'EXPERIENCE', 15.00, true, true, NOW(), NOW());

-- Criteria for tender 2
INSERT INTO tender_criteria (tender_id, name, description, type, weight, prefer_higher, active, created_at, updated_at)
VALUES
    (2, 'Technical Proposal', 'Quality of technical solution design', 'QUALITY', 35.00, true, true, NOW(), NOW()),
    (2, 'Implementation Cost', 'Total implementation cost', 'PRICE', 30.00, false, true, NOW(), NOW()),
    (2, 'Project Timeline', 'Proposed project completion time', 'TIME', 15.00, false, true, NOW(), NOW()),
    (2, 'Team Experience', 'Experience of proposed team members', 'EXPERIENCE', 20.00, true, true, NOW(), NOW());

-- Items for tender 1
INSERT INTO tender_items (tender_id, criteria_id, name, description, quantity, unit, estimated_price, created_at, updated_at)
VALUES
    (1, 1, 'Desktop Computers', 'Core i7, 16GB RAM, 512GB SSD', 100, 'units', 45000.00, NOW(), NOW()),
    (1, 1, 'Office Desks', 'Executive office desks with drawers', 100, 'units', 8000.00, NOW(), NOW()),
    (1, 1, 'Office Chairs', 'Ergonomic office chairs', 100, 'units', 5000.00, NOW(), NOW()),
    (1, 1, 'Printers', 'Network laser printers', 20, 'units', 25000.00, NOW(), NOW());

-- Items for tender 2
INSERT INTO tender_items (tender_id, criteria_id, name, description, quantity, unit, estimated_price, created_at, updated_at)
VALUES
    (2, 5, 'Server Rack', 'Standard 42U server rack', 4, 'units', 120000.00, NOW(), NOW()),
    (2, 5, 'Network Switches', '48-port managed switches', 10, 'units', 35000.00, NOW(), NOW()),
    (2, 5, 'Structured Cabling', 'Cat6 cabling per floor', 5, 'floors', 50000.00, NOW(), NOW());

-- Tender categories
INSERT INTO tender_category (category_name, type, category_description, active, tender_id)
VALUES
    ('Office Supplies', 'GOODS', 'Office furniture and equipment', true, 1),
    ('IT Infrastructure', 'WORKS', 'IT system design and installation', true, 2);
