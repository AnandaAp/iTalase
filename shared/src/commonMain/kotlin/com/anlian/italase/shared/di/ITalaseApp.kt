package com.anlian.italase.shared.di

import org.koin.core.annotation.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.plugin.module.dsl.startKoin

@KoinApplication(modules = [SharedModule::class, PlatformModule::class])
object ITalaseApp

private var koinStarted: Boolean = false

fun initKoin(additional: KoinAppDeclaration = {}) {
    if (koinStarted) return
    startKoin<ITalaseApp>(additional)
    koinStarted = true
}
