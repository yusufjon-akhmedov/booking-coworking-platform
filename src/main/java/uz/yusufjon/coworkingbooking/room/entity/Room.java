package uz.yusufjon.coworkingbooking.room.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.yusufjon.coworkingbooking.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rooms")
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(length = 255)
    private String location;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "hourly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyPrice;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Column(nullable = false)
    private Boolean active = true;
}