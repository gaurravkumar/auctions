package com.prototype.auctions.dto;

public record BidOutputDTO(Long productId, Float bidPrice, String owner, String error) {
}
