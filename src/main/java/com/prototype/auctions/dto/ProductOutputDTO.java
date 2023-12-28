package com.prototype.auctions.dto;

public record ProductOutputDTO(Long productId, String name, Float minimumPrice, boolean inAuction, String owner, String error) {
    public ProductOutputDTO withUpdatedInAuctionStatus(boolean inAuctionStatus) {
        return new ProductOutputDTO(this.productId,this.name,this.minimumPrice, inAuctionStatus, this.owner, this.error);
    }
}
