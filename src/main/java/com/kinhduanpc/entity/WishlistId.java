package com.kinhduanpc.entity;

import java.io.Serializable;
import java.util.Objects;

public class WishlistId implements Serializable {
    private Long user;
    private Long product;

    public WishlistId() {}
    public WishlistId(Long user, Long product) {
        this.user = user;
        this.product = product;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WishlistId)) return false;
        WishlistId that = (WishlistId) o;
        return Objects.equals(user, that.user) && Objects.equals(product, that.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, product);
    }
}
