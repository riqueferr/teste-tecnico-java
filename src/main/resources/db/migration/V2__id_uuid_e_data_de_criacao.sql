ALTER TABLE pessoa
    ADD COLUMN id        UUID,
    ADD COLUMN criado_em TIMESTAMPTZ;

-- linhas anteriores não guardam a data real: recebem a da migração
UPDATE pessoa SET id = gen_random_uuid(), criado_em = now();

ALTER TABLE pessoa
    ALTER COLUMN id SET NOT NULL,
    ALTER COLUMN criado_em SET NOT NULL,
    DROP CONSTRAINT pessoa_pkey,
    ADD PRIMARY KEY (id),
    ADD CONSTRAINT pessoa_documento_key UNIQUE (documento);
