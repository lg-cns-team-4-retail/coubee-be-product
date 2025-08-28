package com.coubee.coubeebeproduct.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "item_recommend", schema = "coubee_product")
@NoArgsConstructor
@Getter
public class ItemRecommend {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recommend_items", nullable = false, columnDefinition = "TEXT")
    private String recommendItems;
}