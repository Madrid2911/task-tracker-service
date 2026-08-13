-- User onboarding is out of scope for this service; seed a handful of users
-- so that task assignment can be exercised without a registration API.
INSERT INTO users (name, email) VALUES
    ('Alice Ivanova', 'alice@example.com'),
    ('Boris Petrov', 'boris@example.com'),
    ('Carla Sidorova', 'carla@example.com'),
    ('Dmitry Orlov', 'dmitry@example.com'),
    ('Elena Kuznetsova', 'elena@example.com');
