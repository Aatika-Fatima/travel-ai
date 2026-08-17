package com.travel.notification.internal.repository

import com.travel.notification.internal.entity.NotificationRequestEntity
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface NotificationRequestRepository : CrudRepository<NotificationRequestEntity, Long>