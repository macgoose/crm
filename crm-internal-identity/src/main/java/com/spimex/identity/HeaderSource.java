package com.spimex.identity;

@FunctionalInterface
public interface HeaderSource {

    String firstValue(String name);
}
