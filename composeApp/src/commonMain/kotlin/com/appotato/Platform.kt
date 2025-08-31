package com.appotato

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform