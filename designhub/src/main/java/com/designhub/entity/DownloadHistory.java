package com.designhub.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "download_histories",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_download_user_design",
                    columnNames = {"user_id", "design_id"}
            )
        }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"user", "design"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DownloadHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull(message = "User không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Design không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private Design design;

    @Column(nullable = false)
    private LocalDateTime downloadedAt;

    @PrePersist
    public void prePersist() {
        if (downloadedAt == null) {
            downloadedAt = LocalDateTime.now();
        }
    }
}
