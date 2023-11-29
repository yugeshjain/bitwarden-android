package com.x8bit.bitwarden.data.auth.repository

import com.bitwarden.core.Kdf
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson.CaptchaRequired
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson.Success
import com.x8bit.bitwarden.data.auth.datasource.network.model.RefreshTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.HaveIBeenPwnedService
import com.x8bit.bitwarden.data.auth.datasource.network.service.IdentityService
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toKdfTypeJson
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.auth.repository.util.toUserState
import com.x8bit.bitwarden.data.auth.repository.util.toUserStateJson
import com.x8bit.bitwarden.data.auth.util.KdfParamsConstants.DEFAULT_PBKDF2_ITERATIONS
import com.x8bit.bitwarden.data.auth.util.toSdkParams
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.platform.util.flatMap
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Singleton

/**
 * Default implementation of [AuthRepository].
 */
@Suppress("LongParameterList")
@Singleton
class AuthRepositoryImpl constructor(
    private val accountsService: AccountsService,
    private val haveIBeenPwnedService: HaveIBeenPwnedService,
    private val identityService: IdentityService,
    private val authSdkSource: AuthSdkSource,
    private val authDiskSource: AuthDiskSource,
    private val environmentRepository: EnvironmentRepository,
    private val vaultRepository: VaultRepository,
    dispatcherManager: DispatcherManager,
) : AuthRepository {
    private val scope = CoroutineScope(dispatcherManager.io)

    override val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    override val authStateFlow: StateFlow<AuthState> = authDiskSource
        .userStateFlow
        .map { userState ->
            userState
                ?.let {
                    AuthState.Authenticated(
                        userState
                            .activeAccount
                            .tokens
                            .accessToken,
                    )
                }
                ?: AuthState.Unauthenticated
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AuthState.Uninitialized,
        )

    override val userStateFlow: StateFlow<UserState?> = combine(
        authDiskSource.userStateFlow,
        vaultRepository.vaultStateFlow,
    ) { userStateJson, vaultState ->
        userStateJson?.toUserState(vaultState = vaultState)
    }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = authDiskSource
                .userState
                ?.toUserState(
                    vaultState = vaultRepository.vaultStateFlow.value,
                ),
        )

    private val mutableCaptchaTokenFlow =
        MutableSharedFlow<CaptchaCallbackTokenResult>(extraBufferCapacity = Int.MAX_VALUE)
    override val captchaTokenResultFlow: Flow<CaptchaCallbackTokenResult> =
        mutableCaptchaTokenFlow.asSharedFlow()

    override var rememberedEmailAddress: String?
        get() = authDiskSource.rememberedEmailAddress
        set(value) {
            authDiskSource.rememberedEmailAddress = value
        }

    override suspend fun deleteAccount(password: String): Result<Unit> {
        val profile = authDiskSource.userState?.activeAccount?.profile
            ?: return IllegalStateException("Not logged in.").asFailure()
        return authSdkSource
            .hashPassword(
                email = profile.email,
                password = password,
                kdf = profile.toSdkParams(),
            )
            .flatMap { hashedPassword -> accountsService.deleteAccount(hashedPassword) }
            .onSuccess { logout() }
    }

    override suspend fun login(
        email: String,
        password: String,
        captchaToken: String?,
    ): LoginResult = accountsService
        .preLogin(email = email)
        .flatMap {
            authSdkSource.hashPassword(
                email = email,
                password = password,
                kdf = it.kdfParams.toSdkParams(),
            )
        }
        .flatMap { passwordHash ->
            identityService.getToken(
                uniqueAppId = authDiskSource.uniqueAppId,
                email = email,
                passwordHash = passwordHash,
                captchaToken = captchaToken,
            )
        }
        .fold(
            onFailure = { LoginResult.Error(errorMessage = null) },
            onSuccess = { loginResponse ->
                when (loginResponse) {
                    is CaptchaRequired -> LoginResult.CaptchaRequired(loginResponse.captchaKey)
                    is Success -> {
                        val userStateJson = loginResponse.toUserState(
                            previousUserState = authDiskSource.userState,
                            environmentUrlData = environmentRepository
                                .environment
                                .environmentUrlData,
                        )
                        vaultRepository.unlockVault(
                            userId = userStateJson.activeUserId,
                            email = userStateJson.activeAccount.profile.email,
                            kdf = userStateJson.activeAccount.profile.toSdkParams(),
                            userKey = loginResponse.key,
                            privateKey = loginResponse.privateKey,
                            // TODO use actual organization keys BIT-1091
                            organizationalKeys = emptyMap(),
                            masterPassword = password,
                        )
                        authDiskSource.userState = userStateJson
                        authDiskSource.storeUserKey(
                            userId = userStateJson.activeUserId,
                            userKey = loginResponse.key,
                        )
                        authDiskSource.storePrivateKey(
                            userId = userStateJson.activeUserId,
                            privateKey = loginResponse.privateKey,
                        )
                        vaultRepository.sync()
                        LoginResult.Success
                    }

                    is GetTokenResponseJson.Invalid -> {
                        LoginResult.Error(errorMessage = loginResponse.errorModel.errorMessage)
                    }
                }
            },
        )

    override fun refreshAccessTokenSynchronously(userId: String): Result<RefreshTokenResponseJson> {
        val refreshAccount = authDiskSource.userState?.accounts?.get(userId)
            ?: return IllegalStateException("Must be logged in.").asFailure()
        return identityService
            .refreshTokenSynchronously(refreshAccount.tokens.refreshToken)
            .onSuccess {
                // Update the existing UserState with updated token information
                authDiskSource.userState = it.toUserStateJson(
                    userId = userId,
                    previousUserState = requireNotNull(authDiskSource.userState),
                )
            }
    }

    override fun logout() {
        activeUserId?.let { userId -> logout(userId) }
    }

    override fun logout(userId: String) {
        val currentUserState = authDiskSource.userState ?: return
        val wasActiveUser = userId == activeUserId

        // Remove the active user from the accounts map
        val updatedAccounts = currentUserState
            .accounts
            .filterKeys { it != userId }
        authDiskSource.storeUserKey(userId = userId, userKey = null)
        authDiskSource.storePrivateKey(userId = userId, privateKey = null)
        // Check if there is a new active user
        if (updatedAccounts.isNotEmpty()) {
            // If we logged out a non-active user, we want to leave the active user unchanged.
            // If we logged out the active user, we want to set the active user to the first one
            // in the list.
            val updatedActiveUserId = currentUserState
                .activeUserId
                .takeUnless { it == userId }
                ?: updatedAccounts.entries.first().key

            // Update the user information and emit an updated token
            authDiskSource.userState = currentUserState.copy(
                activeUserId = updatedActiveUserId,
                accounts = updatedAccounts,
            )
        } else {
            // Update the user information and log out
            authDiskSource.userState = null
        }

        // Lock the vault for the logged out user
        vaultRepository.lockVaultIfNecessary(userId)

        // Clear the current vault data if the logged out user was the active one.
        if (wasActiveUser) vaultRepository.clearUnlockedData()
    }

    @Suppress("ReturnCount", "LongMethod")
    override suspend fun register(
        email: String,
        masterPassword: String,
        masterPasswordHint: String?,
        captchaToken: String?,
        shouldCheckDataBreaches: Boolean,
    ): RegisterResult {
        if (shouldCheckDataBreaches) {
            haveIBeenPwnedService
                .hasPasswordBeenBreached(password = masterPassword)
                .onSuccess { foundDataBreaches ->
                    if (foundDataBreaches) {
                        return RegisterResult.DataBreachFound
                    }
                }
        }
        val kdf = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt())
        return authSdkSource
            .makeRegisterKeys(
                email = email,
                password = masterPassword,
                kdf = kdf,
            )
            .flatMap { registerKeyResponse ->
                accountsService.register(
                    body = RegisterRequestJson(
                        email = email,
                        masterPasswordHash = registerKeyResponse.masterPasswordHash,
                        masterPasswordHint = masterPasswordHint,
                        captchaResponse = captchaToken,
                        key = registerKeyResponse.encryptedUserKey,
                        keys = RegisterRequestJson.Keys(
                            publicKey = registerKeyResponse.keys.public,
                            encryptedPrivateKey = registerKeyResponse.keys.private,
                        ),
                        kdfType = kdf.toKdfTypeJson(),
                        kdfIterations = kdf.iterations,
                    ),
                )
            }
            .fold(
                onSuccess = {
                    when (it) {
                        is RegisterResponseJson.CaptchaRequired -> {
                            it.validationErrors.captchaKeys.firstOrNull()?.let { key ->
                                RegisterResult.CaptchaRequired(captchaId = key)
                            } ?: RegisterResult.Error(errorMessage = null)
                        }

                        is RegisterResponseJson.Success -> {
                            RegisterResult.Success(captchaToken = it.captchaBypassToken)
                        }

                        is RegisterResponseJson.Invalid -> {
                            RegisterResult.Error(
                                errorMessage = it
                                    .validationErrors
                                    ?.values
                                    ?.firstOrNull()
                                    ?.firstOrNull()
                                    ?: it.message,
                            )
                        }

                        is RegisterResponseJson.Error -> {
                            RegisterResult.Error(it.message)
                        }
                    }
                },
                onFailure = {
                    RegisterResult.Error(errorMessage = null)
                },
            )
    }

    override fun setCaptchaCallbackTokenResult(tokenResult: CaptchaCallbackTokenResult) {
        mutableCaptchaTokenFlow.tryEmit(tokenResult)
    }

    @Suppress("MagicNumber")
    override suspend fun getPasswordStrength(
        email: String,
        password: String,
    ): Result<PasswordStrength> {
        // TODO: Replace with SDK call (BIT-964)
        // Ex: return authSdkSource.passwordStrength(email, password)
        val length = password.length
        return when {
            length <= 3 -> PasswordStrength.LEVEL_0
            length <= 6 -> PasswordStrength.LEVEL_1
            length <= 9 -> PasswordStrength.LEVEL_2
            length <= 11 -> PasswordStrength.LEVEL_3
            else -> PasswordStrength.LEVEL_4
        }
            .asSuccess()
    }
}
