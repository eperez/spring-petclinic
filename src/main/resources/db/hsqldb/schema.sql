DROP TABLE issues IF EXISTS;


CREATE TABLE issues (
  id         INTEGER IDENTITY PRIMARY KEY,
  title VARCHAR(255),
  description  VARCHAR(4000)
);
