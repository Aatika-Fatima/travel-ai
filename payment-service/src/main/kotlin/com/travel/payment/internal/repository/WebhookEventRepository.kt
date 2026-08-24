package com.travel.payment.internal.repository

import com.travel.payment.internal.entity.WebhookEvent
import org.springframework.data.jpa.repository.JpaRepository

interface WebhookEventRepository: JpaRepository<WebhookEvent, Long>