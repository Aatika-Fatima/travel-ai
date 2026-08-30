package com.travel.orderservice.internal.service

import com.travel.duffel.api.booking.DuffelBookingService
import com.travel.orderservice.api.OrderView
import com.travel.orderservice.api.SubmitOrderCommand
import com.travel.orderservice.api.toBookingRequest
import com.travel.orderservice.internal.outbox.OutboxWriter
import com.travel.orderservice.internal.persistence.OrderEntity
import com.travel.orderservice.internal.persistence.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull
import kotlin.uuid.Uuid

@Service
class OrderServiceImpl(
    private val orders: OrderRepository,
    private val gateway: DuffelBookingService,
    private val outbox: OutboxWriter,
    private val metrics: OrderMetrics,
    private val transitions: OrderTransitions,
) : OrderService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun submit(command: SubmitOrderCommand): OrderView {
        // Cheap check first -- handles UC2's common case (a client retrying
        // well after the first attempt already finished) with a plain read.
        orders.findByIdempotencyKey(command.idempotencyKey)?.let {
            metrics.duplicatePrevented("unique_constraint")
            return it.toView()
        }

        val order =
            try {
                orders.saveAndFlush(
                    OrderEntity(
                        id = command.orderId ?: Uuid.random(),
                        idempotencyKey = command.idempotencyKey,
                        offerId = command.offerId,
                    ),
                )
            } catch (ex: DataIntegrityViolationException) {
                // A truly concurrent request (UC2's double-click case) won
                // the insert in the gap between the read above and this one.
                // uq_orders_idempotency_key (P1) is what actually caught it
                // -- see §P2 for why that's sufficient on its own, with no
                // lock in front of it.
                metrics.duplicatePrevented("unique_constraint_race")
                return orders.findByIdempotencyKey(command.idempotencyKey)?.toView() ?: throw ex
            }
        outbox.append(order.id, OrderEvent.pending(order.id))
        log.info("Submitting order {} to Duffel (offer {}, key {})", order.id, order.offerId, order.idempotencyKey)

        return callDuffelAndAdvance(order, command)
    }

    private fun callDuffelAndAdvance(order: OrderEntity, command: SubmitOrderCommand): OrderView {
        val outcome = metrics.timeDuffelCall {
            runCatching {
                gateway.createBooking(command.toBookingRequest(), order.idempotencyKey, order.id.toString())
            }
        }
        val view = outcome.fold(
            onSuccess = { result ->
                // advance() only carries a status event, not Duffel's own
                // response data -- and it's shared with the webhook
                // controller and reconciliation sweep, neither of which has
                // a BookingResult to give it. order is still the same
                // managed entity within this transaction, so setting these
                // here is picked up by the same flush advance() triggers.
                order.duffelOrderId = result.orderId
                order.bookingReference = result.bookingReference
                log.info("Order {} confirmed by Duffel (order {}, ref {})", order.id, result.orderId, result.bookingReference)
                transitions.advance(order.id, DuffelOutcome.confirmed(result))
            },
            onFailure = { ex ->
                // Persisted on the same managed entity (same flush as
                // advance()); the customer only ever sees a generic
                // "airline could not confirm" message, so this row + this
                // log line are the only trail a failed booking leaves.
                order.failureReason = ex.message?.take(500)
                log.warn("Order {} rejected by Duffel (offer {}): {}", order.id, order.offerId, ex.message, ex)
                transitions.advance(order.id, DuffelOutcome.from(ex))
            },
        )
        // advance() has no notion of "who's asking" -- it's shared with the
        // webhook controller and the reconciliation sweep, neither of which
        // has a justCreated concept. Only submit()'s own caller, right here,
        // knows this order was freshly inserted in this same call.
        return view.copy(justCreated = true)
    }

    override fun findById(orderId: Uuid): OrderView? = orders.findById(orderId).getOrNull()?.toView()

    override fun findByIdempotencyKey(idempotencyKey: String): OrderView? =
        orders.findByIdempotencyKey(idempotencyKey)?.toView()
}
