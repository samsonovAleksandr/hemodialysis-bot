package com.example.bot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "weight_records")
public class WeightRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double preWeight;
    private Double postWeight;
    private Double dryWeight;
    private LocalDateTime createdAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getPreWeight() { return preWeight; }
    public void setPreWeight(Double preWeight) { this.preWeight = preWeight; }

    public Double getPostWeight() { return postWeight; }
    public void setPostWeight(Double postWeight) { this.postWeight = postWeight; }

    public Double getDryWeight() { return dryWeight; }
    public void setDryWeight(Double dryWeight) { this.dryWeight = dryWeight; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}