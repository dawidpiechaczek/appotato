package com.firetms.appotato

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform