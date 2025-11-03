package com.availelabs.availe.di

import com.availelabs.availe.data.auth.IAuthService

interface IAppContainer {
    val authService: IAuthService
}