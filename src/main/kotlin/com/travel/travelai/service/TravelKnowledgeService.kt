package com.travel.travelai.service

import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Service
class TravelKnowledgeService(val vectorStore: VectorStore) {
    init {
        vectorStore.add(addDocuments())
    }


    fun addDocuments():List<Document>{
        return listOf(Document("Lufthansa allows cancellation within 24 hours without charge"),
            Document("Emirates allows baggage up to 30kg for Economy."),
            Document(" British Airways refunds are processed within 7 days."))
    }

    fun similaritySearch(query:String):List<Document>{
         return vectorStore.similaritySearch(
            SearchRequest.builder().query(query).topK(3).build()
        )
    }
}