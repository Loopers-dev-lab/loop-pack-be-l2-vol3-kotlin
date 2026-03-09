-- Brand 필터링 용
CREATE INDEX idx_brand_like ON products(brand_id, like_count DESC);
CREATE INDEX idx_brand_created ON products(brand_id, created_at DESC);
CREATE INDEX idx_brand_price ON products(brand_id, price);

-- Status 필터링 용
CREATE INDEX idx_status_like ON products(status, like_count DESC);
CREATE INDEX idx_status_created ON products(status, created_at DESC);
CREATE INDEX idx_status_price ON products(status, price);
