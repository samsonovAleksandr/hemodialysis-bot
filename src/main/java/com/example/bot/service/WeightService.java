package com.example.bot.service;

import com.example.bot.entity.WeightRecord;
import com.example.bot.repository.WeightRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WeightService {
    @Autowired
    private WeightRecordRepository repository;

    public WeightRecord save(WeightRecord record) {
        record.setCreatedAt(LocalDateTime.now());
        return repository.save(record);
    }

    public WeightRecord getLastRecord() {
        return repository.findTopByOrderByCreatedAtDesc();
    }

    public List<WeightRecord> getLastMonthRecords() {
        LocalDateTime since = LocalDateTime.now().minusMonths(1);
        return repository.findLastMonthRecords(since);
    }
}