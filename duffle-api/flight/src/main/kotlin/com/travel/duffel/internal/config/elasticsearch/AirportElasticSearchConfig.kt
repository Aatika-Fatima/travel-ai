package com.travel.duffel.internal.config.elasticsearch

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

// Elasticsearch is not deployed in the AWS profile (see docs/amazon.html P1): the airport search
// path is 100% Postgres/pg_trgm there. Gating the repository scan on !aws keeps Spring from
// instantiating AirportDocumentRepository at startup when no cluster is reachable, without
// touching local dev, where this config is active and behaves exactly as before.
@Configuration
@Profile("!aws")
@EnableConfigurationProperties(AirportElasticSearchProperties::class)
@EnableElasticsearchRepositories(basePackages = ["com.travel.duffel.internal.airport.document"])
class AirportElasticSearchConfig
