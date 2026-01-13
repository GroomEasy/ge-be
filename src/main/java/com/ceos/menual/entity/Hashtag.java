package com.ceos.menual.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "hashtags")
public class Hashtag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String name;

    @Builder.Default
    private Integer usageCount = 0;

    public void incrementUsageCount() {
        if (this.usageCount == null) {
            this.usageCount = 0;
        }
        this.usageCount++;
    }

    public void decrementUsageCount() {
        if (this.usageCount == null || this.usageCount == 0) {
            return;
        }
        this.usageCount--;
    }
}