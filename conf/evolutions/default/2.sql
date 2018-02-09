# Mail Meta Schema

# --- !Ups

ALTER TABLE mails ADD meta_sending_address VARCHAR(255);
ALTER TABLE mails ADD meta_created BIGINT(20) NOT NULL;
ALTER TABLE mails ADD meta_sent BIGINT(20);
CREATE FULLTEXT INDEX meta_sender_index ON mails(meta_sending_address);

# --- !Downs

ALTER TABLE mails DROP INDEX meta_sender_index;
ALTER TABLE mails DROP COLUMN meta_sending_address;
ALTER TABLE mails DROP COLUMN meta_created;
ALTER TABLE mails DROP COLUMN meta_sent;