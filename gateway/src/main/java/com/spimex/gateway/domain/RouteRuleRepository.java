package com.spimex.gateway.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRuleRepository extends JpaRepository<RouteRule, Long> {

    List<RouteRule> findAllByEnabledTrueOrderByRuleOrderAsc();
}
