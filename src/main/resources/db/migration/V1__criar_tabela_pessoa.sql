CREATE TABLE pessoa (
    documento  VARCHAR(11)  PRIMARY KEY CHECK (documento ~ '^[0-9]{11}$'),
    nome       VARCHAR(100) NOT NULL,
    sobrenome  VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL
);
