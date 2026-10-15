package dev.truedoctales.sample.quarkus;

import dev.truedoctales.sample.domain.HeroService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class SampleQuarkusConfiguration {

  @Produces
  @ApplicationScoped
  HeroService heroService() {
    return new HeroService();
  }
}
