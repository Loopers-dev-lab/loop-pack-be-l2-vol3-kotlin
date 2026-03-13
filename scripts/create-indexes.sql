-- =============================================================
-- 상품 조회 성능 개선 인덱스
-- =============================================================

-- brandId 필터 없는 경우 (3개)
CREATE INDEX idx_products_deleted_created ON products (deleted_at, created_at DESC);
CREATE INDEX idx_products_deleted_price ON products (deleted_at, price ASC);
CREATE INDEX idx_products_deleted_like_count ON products (deleted_at, like_count DESC);

-- brandId 필터 있는 경우 (3개)
CREATE INDEX idx_products_brand_deleted_created ON products (brand_id, deleted_at, created_at DESC);
CREATE INDEX idx_products_brand_deleted_price ON products (brand_id, deleted_at, price ASC);
CREATE INDEX idx_products_brand_deleted_like_count ON products (brand_id, deleted_at, like_count DESC);
