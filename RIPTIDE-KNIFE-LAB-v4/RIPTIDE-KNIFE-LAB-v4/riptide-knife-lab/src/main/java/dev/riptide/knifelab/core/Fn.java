package dev.riptide.knifelab.core;

/** Function of normalized blade position t in [0,1]. */
@FunctionalInterface
public interface Fn {
    float f(float t);
}
