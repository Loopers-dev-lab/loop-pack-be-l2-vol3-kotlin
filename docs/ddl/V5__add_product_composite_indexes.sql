-- 브랜드 필터 + 좋아요 순 (핵심)
CREATE INDEX IF NOT EXISTS idx_product_brand_status_like
  ON product (brand_id, status, like_count DESC, id DESC);

-- 브랜드 필터 + 가격 순
CREATE INDEX IF NOT EXISTS idx_product_brand_status_price
  ON product (brand_id, status, price ASC, id DESC);

-- 기존 idx_product_brand_id는 위 복합 인덱스에 포함되므로 제거
DROP INDEX idx_product_brand_id ON product;
