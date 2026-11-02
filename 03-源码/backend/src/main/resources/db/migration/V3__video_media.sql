ALTER TABLE t_media
  ADD COLUMN media_type VARCHAR(8) NOT NULL DEFAULT 'IMAGE',
  ADD COLUMN duration_ms BIGINT NULL,
  ADD COLUMN poster_storage_key VARCHAR(180) NULL,
  ADD COLUMN poster_size_bytes BIGINT NULL,
  ADD CONSTRAINT ck_media_type CHECK (media_type IN ('IMAGE','VIDEO')),
  ADD CONSTRAINT ck_video_media CHECK (media_type <> 'VIDEO' OR
    (purpose='NOTE' AND mime='video/mp4' AND duration_ms>0 AND poster_storage_key IS NOT NULL AND poster_size_bytes>0));
