package com.kdt.reservation.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "feature")
@NoArgsConstructor
@Getter
@Setter
public class Feature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true,length = 30,nullable = false)
    private String code;
    @Column(length = 50,nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "category",nullable = false)
    private FeatureCategory featureCategory;
    @Enumerated(EnumType.STRING)
    @Column(name = "target",nullable = false)
    private FeatureTarget featureTarget;
}
