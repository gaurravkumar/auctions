package com.prototype.auctions.repository;

import com.prototype.auctions.entity.BidEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<BidEntity, Long> {
    Optional<List<BidEntity>> findByProductId(Long productId);
}
