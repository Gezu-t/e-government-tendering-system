-- Add optimistic locking version column to bids table
-- Prevents race conditions on concurrent bid status updates
ALTER TABLE bids ADD COLUMN version BIGINT DEFAULT 0;
