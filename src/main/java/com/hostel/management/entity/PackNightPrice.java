package com.hostel.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "pack_night_prices",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pack_id", "nights", "room_type"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "pack")           // ✅ évite StackOverflow
@EqualsAndHashCode(exclude = "pack")  // ✅ évite StackOverflow
public class PackNightPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pack_id", nullable = false)
    private Pack pack;

    @Column(nullable = false)
    private int nights; // 3 à 10

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    private Room.RoomType roomType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal promoPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal regularPrice;
}