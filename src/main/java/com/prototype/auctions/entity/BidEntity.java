package com.prototype.auctions.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "BIDS", indexes = @Index(columnList = "productId"))
public class BidEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bidId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Float bidPrice;

    @Column(nullable = false)
    private String owner;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedDate;

    @PrePersist
    public void prePersist() {
        if (updatedDate == null) {
            updatedDate = LocalDateTime.now();
        }
    }

}
