
CREATE TABLE users (
                       id UUID PRIMARY KEY,

                       username VARCHAR(50) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,

                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL

);

CREATE TABLE breeds (
                        id UUID PRIMARY KEY,

                        name VARCHAR(100) NOT NULL UNIQUE,
                        description TEXT,

                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL
);

CREATE TABLE pets (
                      id UUID PRIMARY KEY,

                      owner_id UUID NOT NULL,
                      breed_id UUID NOT NULL,

                      name VARCHAR(100) NOT NULL,

                      sex VARCHAR(10) NOT NULL,

                      birth_date DATE NOT NULL,

                      neutered BOOLEAN NOT NULL DEFAULT FALSE,

                      microchip_number VARCHAR(50),

                      created_at TIMESTAMP NOT NULL,
                      updated_at TIMESTAMP NOT NULL,

                      CONSTRAINT fk_pet_owner
                          FOREIGN KEY (owner_id)
                              REFERENCES users(id),

                      CONSTRAINT fk_pet_breed
                          FOREIGN KEY (breed_id)
                              REFERENCES breeds(id)
);
