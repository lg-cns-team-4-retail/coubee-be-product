package com.coubee.coubeebeproduct.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@NoArgsConstructor
public class ProductViewRecord extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="product_id")
    private Product product;

    private Long userId;

    private Long unixTimestamp;
    private int count;

    public void incrementViewCount() {
        this.count++;
    }
    @Builder
    public ProductViewRecord(Product product, Long userId) {
        this.product = product;
        this.userId = userId;
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");
        ZonedDateTime koreaTime = ZonedDateTime.now(koreaZone);
        long eventTimestamp = koreaTime.toEpochSecond();

        System.out.println("Personalize용 Unix Timestamp: " + eventTimestamp);
        this.unixTimestamp = System.currentTimeMillis()/1000;
        this.count = 1;
    }
}

