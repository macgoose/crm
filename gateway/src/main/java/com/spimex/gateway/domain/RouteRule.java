package com.spimex.gateway.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "gateway_route_rule")
public class RouteRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_key", nullable = false, unique = true, length = 100)
    private String routeKey;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "path_pattern", nullable = false, length = 500)
    private String pathPattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_mode", nullable = false, length = 20)
    private AccessMode accessMode;

    @Column(name = "permission_code", length = 100)
    private String permissionCode;

    @Column(name = "target_uri", nullable = false, length = 1000)
    private String targetUri;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "rule_order", nullable = false)
    private int ruleOrder;
}
