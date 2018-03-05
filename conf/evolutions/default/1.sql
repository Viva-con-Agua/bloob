# Mail Schema

# --- !Ups
CREATE TABLE mails (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  author_email VARCHAR(255) NOT NULL,
  subject VARCHAR(512),
  body MEDIUMTEXT,
  CONSTRAINT pk PRIMARY KEY (id),
  FULLTEXT(author_email,subject,body)
) ENGINE=InnoDB CHARACTER SET=utf8;

CREATE TABLE receiver (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  user_email VARCHAR(255) NOT NULL,
  mail_id BIGINT(20) NOT NULL,
  CONSTRAINT pk PRIMARY KEY (id),
  CONSTRAINT fk_mails FOREIGN KEY (mail_id) REFERENCES mails(id),
  FULLTEXT(user_email)
) ENGINE=InnoDB CHARACTER SET=utf8;

# --- !Downs

DROP TABLE receiver;
DROP TABLE mails;