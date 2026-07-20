CREATE TABLE tasks (
    id          UUID PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    description TEXT,
    status      VARCHAR(20)   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);

INSERT INTO tasks (id, title, description, status, created_at, updated_at) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Set up Keycloak realm', 'Import the showcase realm and create the user/admin roles', 'DONE', now(), now()),
    ('22222222-2222-2222-2222-222222222222', 'Wire up Angular OIDC login', 'Integrate keycloak-angular against the Keycloak realm', 'IN_PROGRESS', now(), now()),
    ('33333333-3333-3333-3333-333333333333', 'Add task filtering by status', 'Allow the task list to be filtered by TODO/IN_PROGRESS/DONE', 'TODO', now(), now());
