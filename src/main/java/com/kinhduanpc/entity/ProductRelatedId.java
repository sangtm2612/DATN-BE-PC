package com.kinhduanpc.entity;

import java.io.Serializable;
import java.util.Objects;

public class ProductRelatedId implements Serializable {
    private Long product;
    private Long relatedProduct;

    public ProductRelatedId() {}
    public ProductRelatedId(Long product, Long relatedProduct) {
        this.product = product;
        this.relatedProduct = relatedProduct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductRelatedId)) return false;
        ProductRelatedId that = (ProductRelatedId) o;
        return Objects.equals(product, that.product) && Objects.equals(relatedProduct, that.relatedProduct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, relatedProduct);
    }
}
