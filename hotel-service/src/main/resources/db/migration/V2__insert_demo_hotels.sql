INSERT INTO hotels (name, address) VALUES
    ('Aurora', 'Moscow, Tverskaya 10'),
    ('Neva', 'Saint Petersburg, Nevsky 20');

INSERT INTO rooms (hotel_id, number, available) VALUES
    (1, '101', TRUE),
    (1, '102', TRUE),
    (1, '103', TRUE),
    (1, '104-repair', FALSE),
    (2, '201', TRUE),
    (2, '202', TRUE);
