CREATE TABLE IF NOT EXISTS assistant_instruction_config (
    id TINYINT NOT NULL PRIMARY KEY,
    draft JSON NOT NULL,
    published JSON,
    previous JSON,
    revision BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
