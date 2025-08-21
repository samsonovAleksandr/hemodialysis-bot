package com.example.bot.repository;

import com.example.bot.entity.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {
    @Query("SELECT w FROM WeightRecord w WHERE w.createdAt > :since ORDER BY w.createdAt ASC")
    List<WeightRecord> findLastMonthRecords(LocalDateTime since);

    WeightRecord findTopByOrderByCreatedAtDesc();
}