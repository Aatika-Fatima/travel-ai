package com.travel.notification.internal.repository

import com.travel.notification.internal.entity.EmailDeliveryEntity
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface EmailDeliveryRepository : CrudRepository<EmailDeliveryEntity, Long> {
    fun existsByEmailId(emailId: UUID): Boolean
}