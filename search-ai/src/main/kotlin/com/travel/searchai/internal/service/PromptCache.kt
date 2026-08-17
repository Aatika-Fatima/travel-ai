package com.travel.searchai.internal.service

interface PromptCache {

    fun get(key:String):String?
    fun put(key:String, value:String)
}