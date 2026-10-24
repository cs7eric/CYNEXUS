\set ON_ERROR_STOP on

-- Workout v1 persistence for the mobile Workout UI.  The client currently
-- records one daily session and dated body measurements; exercise/set detail
-- can be added later without changing these history contracts.
CREATE SCHEMA IF NOT EXISTS cynexus_appl_life;

CREATE TABLE IF NOT EXISTS cynexus_appl_life.workout_plan (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    name text NOT NULL DEFAULT 'My workout' CHECK (btrim(name) <> ''),
    cycle_start_date date NOT NULL,
    timezone text NOT NULL CHECK (btrim(timezone) <> ''),
    is_active boolean NOT NULL DEFAULT true,
    idempotency_key text NOT NULL CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 128),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(metadata) = 'object'),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_workout_plan_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_workout_plan_request UNIQUE (owner_key, idempotency_key)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_workout_plan_active_owner
    ON cynexus_appl_life.workout_plan (owner_key)
    WHERE is_active AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_workout_plan_owner
    ON cynexus_appl_life.workout_plan (owner_key, cycle_start_date DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_life.workout_plan_day (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    plan_id bigint NOT NULL,
    day_index integer NOT NULL CHECK (day_index >= 0),
    day_type text NOT NULL,
    split_code text NOT NULL,
    display_name text NOT NULL CHECK (btrim(display_name) <> ''),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(metadata) = 'object'),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_workout_plan_day_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_workout_plan_day_order UNIQUE (owner_key, plan_id, day_index),
    CONSTRAINT fk_workout_plan_day_plan FOREIGN KEY (owner_key, plan_id)
        REFERENCES cynexus_appl_life.workout_plan (owner_key, id),
    CONSTRAINT ck_workout_plan_day_type CHECK (
        (day_type = 'strength' AND split_code IN ('chest_triceps', 'back_biceps', 'shoulders'))
        OR (day_type = 'rest' AND split_code = 'rest')
    )
);

CREATE INDEX IF NOT EXISTS idx_workout_plan_day_plan
    ON cynexus_appl_life.workout_plan_day (owner_key, plan_id, day_index)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_life.workout_session (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    plan_id bigint,
    workout_date date NOT NULL,
    category text NOT NULL,
    duration_minutes integer NOT NULL,
    split_code text,
    note text,
    idempotency_key text NOT NULL CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 128),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(metadata) = 'object'),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_workout_session_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_workout_session_request UNIQUE (owner_key, idempotency_key),
    CONSTRAINT fk_workout_session_plan FOREIGN KEY (owner_key, plan_id)
        REFERENCES cynexus_appl_life.workout_plan (owner_key, id),
    CONSTRAINT ck_workout_session_category CHECK (category IN ('strength', 'cardio')),
    CONSTRAINT ck_workout_session_duration CHECK (duration_minutes BETWEEN 1 AND 1440),
    CONSTRAINT ck_workout_session_split CHECK (
        (category = 'cardio' AND split_code IS NULL)
        OR (category = 'strength' AND (split_code IS NULL OR split_code IN ('chest_triceps', 'back_biceps', 'shoulders')))
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_workout_session_owner_date
    ON cynexus_appl_life.workout_session (owner_key, workout_date)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_workout_session_owner_history
    ON cynexus_appl_life.workout_session (owner_key, workout_date DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_workout_session_owner_category
    ON cynexus_appl_life.workout_session (owner_key, category, workout_date DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_life.workout_body_measurement (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    metric_code text NOT NULL,
    side text,
    value numeric(10, 2) NOT NULL,
    unit text NOT NULL,
    measured_at date NOT NULL,
    note text,
    idempotency_key text NOT NULL CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 128),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(metadata) = 'object'),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_workout_body_measurement_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_workout_body_measurement_request UNIQUE (owner_key, idempotency_key),
    CONSTRAINT ck_workout_body_measurement_metric CHECK (metric_code IN (
        'weight', 'height', 'head', 'neck', 'shoulders', 'shoulder_width',
        'chest', 'under_chest', 'waist', 'abdomen', 'hips', 'upper_arm',
        'forearm', 'wrist', 'thigh', 'calf', 'ankle'
    )),
    CONSTRAINT ck_workout_body_measurement_side CHECK (
        (metric_code IN ('upper_arm', 'forearm', 'wrist', 'thigh', 'calf', 'ankle')
            AND side IN ('left', 'right'))
        OR (metric_code NOT IN ('upper_arm', 'forearm', 'wrist', 'thigh', 'calf', 'ankle')
            AND side IS NULL)
    ),
    CONSTRAINT ck_workout_body_measurement_unit CHECK (
        (metric_code = 'weight' AND unit = 'kg')
        OR (metric_code <> 'weight' AND unit = 'cm')
    ),
    CONSTRAINT ck_workout_body_measurement_value CHECK (
        value > 0 AND value < 'Infinity'::numeric
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_workout_body_measurement_identity
    ON cynexus_appl_life.workout_body_measurement (
        owner_key, metric_code, COALESCE(side, '_none'), measured_at
    )
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_workout_body_measurement_metric_history
    ON cynexus_appl_life.workout_body_measurement (owner_key, metric_code, side, measured_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_workout_body_measurement_owner_history
    ON cynexus_appl_life.workout_body_measurement (owner_key, measured_at DESC, id DESC)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE cynexus_appl_life.workout_plan IS
    'Owner-scoped recurring training cycle configuration used by the mobile Workout overview.';
COMMENT ON TABLE cynexus_appl_life.workout_plan_day IS
    'Ordered day definitions for a workout cycle. split_code is a stable API value; display_name is presentation text.';
COMMENT ON TABLE cynexus_appl_life.workout_session IS
    'Owner-scoped daily workout log. The current UI permits at most one active record per date.';
COMMENT ON TABLE cynexus_appl_life.workout_body_measurement IS
    'Owner-scoped dated body measurements used by the body summary, diagram, comparison and trend views.';
COMMENT ON COLUMN cynexus_appl_life.workout_session.split_code IS
    'Historical split snapshot; it remains stable when a plan is edited or replaced.';
COMMENT ON COLUMN cynexus_appl_life.workout_body_measurement.value IS
    'Positive measurement in unit; weight is kilograms and all other current metrics are centimeters.';

DO $comments$
DECLARE table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'workout_plan', 'workout_plan_day', 'workout_session', 'workout_body_measurement'
    ] LOOP
        EXECUTE format('COMMENT ON COLUMN cynexus_appl_life.%I.owner_key IS %L', table_name,
            'Authenticated stable owner identity, assigned server-side. Every query must enforce owner scope.');
        EXECUTE format('COMMENT ON COLUMN cynexus_appl_life.%I.updated_at IS %L', table_name,
            'Application-maintained update timestamp; inserts default to now().');
        EXECUTE format('COMMENT ON COLUMN cynexus_appl_life.%I.deleted_at IS %L', table_name,
            'Soft deletion marker. Historical rows remain addressable for audit but are excluded from active indexes.');
    END LOOP;
END
$comments$;
