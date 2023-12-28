package com.prototype.auctions.dto;

public record BidInputDTO(Long productId, Float bidPrice, String owner) {

    public BidInputDTO withUpdatedOwner(String owner) {
        return new BidInputDTO(this.productId, this.bidPrice, owner);
    }
}
