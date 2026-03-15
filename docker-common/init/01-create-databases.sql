-- Create all service databases
CREATE DATABASE IF NOT EXISTS user_service;
CREATE DATABASE IF NOT EXISTS tender_service;
CREATE DATABASE IF NOT EXISTS bidding_service;
CREATE DATABASE IF NOT EXISTS contract_service;
CREATE DATABASE IF NOT EXISTS document_service;
CREATE DATABASE IF NOT EXISTS notification_service;
CREATE DATABASE IF NOT EXISTS audit_service;
CREATE DATABASE IF NOT EXISTS evaluation_service;

-- Grant permissions
GRANT ALL PRIVILEGES ON user_service.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON tender_service.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON bidding_service.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON contract_service.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON document_service.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON notification_service.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON audit_service.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON evaluation_service.* TO 'root'@'%';
FLUSH PRIVILEGES;
