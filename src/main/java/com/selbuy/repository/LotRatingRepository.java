// LotRatingRepository.java
package com.selbuy.repository;

import com.selbuy.model.LotRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LotRatingRepository extends JpaRepository<LotRating, Long> {
    List<LotRating> findByLotId(Long lotId);
    Optional<LotRating> findByLotIdAndUserId(Long lotId, Long userId);
    void deleteByLotId(Long lotId);
}