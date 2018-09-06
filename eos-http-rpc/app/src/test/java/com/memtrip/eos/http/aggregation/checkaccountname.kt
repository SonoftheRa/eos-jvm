package com.memtrip.eos.http.aggregation

import com.memtrip.eos.core.crypto.EosPrivateKey
import com.memtrip.eos.http.aggregation.accountname.CheckAccountNameExists
import com.memtrip.eos.http.aggregation.createaccount.CreateAccountAggregate
import com.memtrip.eos.http.rpc.Api
import com.memtrip.eos.http.rpc.Config
import com.memtrip.eos.http.rpc.generateUniqueAccountName
import com.memtrip.eos.http.rpc.toFutureDate
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.Assert
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.concurrent.TimeUnit

@RunWith(JUnitPlatform::class)
class CheckAccountNameTest : Spek({

    given("an Api") {

        val okHttpClient by memoized {
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
        }

        val chainApi by memoized { Api(Config.CHAIN_API_BASE_URL, okHttpClient).chain }

        on("v1/chain/get_currency_balance -> Account exists") {

            val privateKey = EosPrivateKey("5KQwrPbwdL6PhXujxW37FSSQZ1JiwsST4cqQzDeyXtP79zkvFD3")

            val firstAccountName = generateUniqueAccountName()
            CreateAccountAggregate(chainApi).createAccount(
                CreateAccountAggregate.Args(
                    firstAccountName,
                    CreateAccountAggregate.Args.Quantity(
                        "100.0000 SYS",
                        "100.0000 SYS",
                        "100.0000 SYS"),
                    privateKey.publicKey,
                    privateKey.publicKey,
                    "eosio",
                    privateKey,
                    Calendar.getInstance().toFutureDate()
                )
            ).blockingGet()

            val accountExists = CheckAccountNameExists(chainApi)
                .checkAccountNameExists(firstAccountName)
                .blockingGet()

            it("should return true") {
                Assert.assertTrue(accountExists)
            }
        }

        on("v1/chain/get_currency_balance -> Account does not exists") {

            val accountExists = CheckAccountNameExists(chainApi)
                .checkAccountNameExists("123456789012")
                .blockingGet()

            it("should return false") {
                Assert.assertFalse(accountExists)
            }
        }
    }
})