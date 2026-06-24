-- Link trust mark subjects to their TM-specific registration (cascade-delete subjects when registration removed).
ALTER TABLE trustmark_subject
    ADD COLUMN registration_id uuid NULL,
    ADD CONSTRAINT fk_trustmarksubject_registration
        FOREIGN KEY (registration_id) REFERENCES registrations (registration_id)
        ON DELETE CASCADE;

-- Allow TM-specific child registrations to reference their parent IM registration
-- (cascade-delete children when parent is removed).
ALTER TABLE registrations
    ADD COLUMN parent_registration_id uuid NULL,
    ADD CONSTRAINT fk_registration_parent
        FOREIGN KEY (parent_registration_id) REFERENCES registrations (registration_id)
        ON DELETE CASCADE;
