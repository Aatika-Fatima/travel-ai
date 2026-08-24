package com.travel.payment.internal.controller.dto

data class ConfirmRequest(val razorpayOrderId:String,val razorpayPaymentId:String,val razorpaySignature:String)
