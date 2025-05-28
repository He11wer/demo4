// AuctionTransactionRepository.java
package com.selbuy.repository;

import com.selbuy.model.AuctionLot;
import com.selbuy.model.AuctionTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionTransactionRepository extends JpaRepository<AuctionTransaction, Long> {

    List<AuctionTransaction> findBySellerId(Long id);
    List<AuctionTransaction> findByWinnerId(Long id);
}