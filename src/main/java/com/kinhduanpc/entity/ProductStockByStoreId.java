package com.kinhduanpc.entity;

import java.io.Serializable;
import java.util.Objects;

public class ProductStockByStoreId implements Serializable {
    private Long product;
    private Long store;

    public ProductStockByStoreId() {}
    public ProductStockByStoreId(Long product, Long store) {
        this.product = product;
        this.store = store;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductStockByStoreId)) return false;
        ProductStockByStoreId that = (ProductStockByStoreId) o;
        return Objects.equals(product, that.product) && Objects.equals(store, that.store);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, store);
    }
}
