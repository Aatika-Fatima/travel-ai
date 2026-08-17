package com.travel.duffel.internal.airport.document

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.annotations.GeoPointField
import org.springframework.data.elasticsearch.annotations.InnerField
import org.springframework.data.elasticsearch.annotations.MultiField
import org.springframework.data.elasticsearch.annotations.Setting
import org.springframework.data.elasticsearch.core.geo.GeoPoint
import java.time.Instant

@Document(indexName = "airports")
@Setting(settingPath = "elasticsearch/airport-settings.json")
data class AirportDocument(
    @Id
    @Field(type = FieldType.Keyword, normalizer = "lowercase")
    val iataCode: String,

    @Field(type = FieldType.Keyword, normalizer = "lowercase")
    val icaoCode: String?,

    @MultiField(
        mainField = Field(type = FieldType.Text),
        otherFields = [
            InnerField(suffix = "autocomplete", type = FieldType.Text, analyzer = "autocomplete", searchAnalyzer = "autocomplete_search"),
            InnerField(suffix = "keyword", type = FieldType.Keyword),
        ],
    )
    val name: String,

    @MultiField(
        mainField = Field(type = FieldType.Text),
        otherFields = [
            InnerField(suffix = "autocomplete", type = FieldType.Text, analyzer = "autocomplete", searchAnalyzer = "autocomplete_search"),
        ],
    )
    val cityName: String?,

    @Field(type = FieldType.Keyword)
    val iataCountryCode: String?,

    @GeoPointField
    val location: GeoPoint?,

    @Field(type = FieldType.Date)
    val lastSyncedAt: Instant,
)