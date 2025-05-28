// LotTrackingRepository.java
package com.selbuy.repository;

import com.selbuy.model.LotTracking;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LotTrackingRepository extends JpaRepository<LotTracking, Long> {
    Optional<LotTracking> findByUserIdAndLotId(Long userId, Long lotId);
    List<LotTracking> findByUserId(Long userId);
    List<LotTracking> findByUserIdAndLikedTrue(Long userId);
    boolean existsByUserIdAndLotId(Long userId, Long lotId);
    @Modifying
    @Transactional
    @Query("DELETE FROM LotTracking lt WHERE lt.lot.id = :lotId")
    void deleteByLotId(Long lotId);


}