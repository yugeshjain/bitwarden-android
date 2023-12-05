package com.x8bit.bitwarden.data.tools.generator.datasource.sdk

import com.bitwarden.core.PassphraseGeneratorRequest
import com.bitwarden.core.PasswordGeneratorRequest
import com.bitwarden.sdk.ClientGenerators
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GeneratorSdkSourceTest {
    private val clientGenerators = mockk<ClientGenerators>()
    private val generatorSdkSource: GeneratorSdkSource = GeneratorSdkSourceImpl(clientGenerators)

    @Test
    fun `generatePassword should call SDK and return a Result with the generated password`() =
        runBlocking {
            val request = PasswordGeneratorRequest(
                lowercase = true,
                uppercase = true,
                numbers = true,
                special = true,
                length = 12U,
                avoidAmbiguous = false,
                minLowercase = 1U,
                minUppercase = 1U,
                minNumber = 1U,
                minSpecial = 1U,
            )
            val expectedResult = "GeneratedPassword123!"

            coEvery {
                clientGenerators.password(request)
            } returns expectedResult

            val result = generatorSdkSource.generatePassword(request)

            assertEquals(Result.success(expectedResult), result)

            coVerify {
                clientGenerators.password(request)
            }
        }

    @Test
    fun `generatePassphrase should call SDK and return a Result with the generated passphrase`() =
        runBlocking {
            val request = PassphraseGeneratorRequest(
                numWords = 4U,
                wordSeparator = "-",
                capitalize = true,
                includeNumber = true,
            )
            val expectedResult = "Generated-Passphrase123"

            coEvery {
                clientGenerators.passphrase(request)
            } returns expectedResult

            val result = generatorSdkSource.generatePassphrase(request)

            assertEquals(Result.success(expectedResult), result)

            coVerify {
                clientGenerators.passphrase(request)
            }
        }
}
