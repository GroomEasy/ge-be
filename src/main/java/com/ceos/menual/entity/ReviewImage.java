package com.ceos.menual.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(
        name = "review_image",
        indexes = {
                @Index(name = "idx_review_image_review_id", columnList = "review_id"),
                @Index(name = "idx_review_image_review_order", columnList = "review_id, display_order")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReviewImage extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;  // 이미지 표시 순서


    // 메서드
    public void setReview(Review review) {
        this.review = review;
        if (review != null && !review.getImages().contains(this)) {
            review.getImages().add(this);
        }
    }
}