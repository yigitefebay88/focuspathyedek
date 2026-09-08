package com.focuspath.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
