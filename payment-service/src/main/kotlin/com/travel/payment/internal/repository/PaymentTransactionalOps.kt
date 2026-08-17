package com.travel.payment.internal.repository

 import com.travel.payment.internal.entity.Payment
 import com.travel.payment.internal.entity.PaymentStatus
 import org.springframework.dao.DataIntegrityViolationException
 import org.springframework.stereotype.Component
 import org.springframework.transaction.annotation.Transactional
 import java.util.UUID
 import kotlin.jvm.optionals.getOrNull
 import kotlin.uuid.Uuid

@Component
class PaymentTransactionalOps(private val paymentRepository: PaymentRepository) {

    @Transactional
    fun findOrInsertPending(bookingId: Uuid,idempotencyKey:String, amountInPaise:Long, currency: String): Pair<Payment, Boolean> {
        paymentRepository.findByIdempotencyKey(idempotencyKey)?.let { return it to true }
        return try{
            val saved= paymentRepository.save(
                Payment(bookingId=bookingId, idempotencyKey = idempotencyKey, amountPaise = amountInPaise,currency = currency)
            )
            saved to false
        }catch (ex: DataIntegrityViolationException){
            val entity = paymentRepository .findByIdempotencyKey(idempotencyKey) ?: throw ex
            entity to true
        }
    }

    @Transactional
    fun recordRazorPayOrder(paymentId:Uuid,razorPayOrderId: String): Payment {
        val payment = paymentRepository.findById(paymentId.toString())
        payment?.razorpayOrderId = razorPayOrderId
        payment?.status = PaymentStatus.ATTEMPTED
        return payment as Payment

    }


}