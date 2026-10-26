-- V3: Enforce dependency ownership at the database level.

-- Fail safely if any dependency has no owner.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM api_dependencies
        WHERE owner_id IS NULL
    ) THEN
        RAISE EXCEPTION
            'Cannot enforce dependency ownership: one or more dependencies have NULL owner_id. Assign each dependency to a real user before migrating.';
    END IF;

    -- Fail safely if an owner does not exist in app_users.
    IF EXISTS (
        SELECT 1
        FROM api_dependencies d
        LEFT JOIN app_users u ON u.id = d.owner_id
        WHERE u.id IS NULL
    ) THEN
        RAISE EXCEPTION
            'Cannot enforce dependency ownership: one or more owner_id values do not reference an existing app_users row.';
    END IF;
END
$$;

-- Ensure every dependency references a real application user.
ALTER TABLE api_dependencies
    ADD CONSTRAINT fk_api_dependencies_owner
    FOREIGN KEY (owner_id)
    REFERENCES app_users (id);

-- Every dependency must have an owner.
ALTER TABLE api_dependencies
    ALTER COLUMN owner_id SET NOT NULL;