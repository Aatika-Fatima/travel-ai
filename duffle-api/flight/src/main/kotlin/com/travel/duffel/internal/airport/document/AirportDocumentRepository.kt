package com.travel.duffel.internal.airport.document

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository
interface AirportDocumentRepository : ElasticsearchRepository<AirportDocument, String>
