package uk.gov.hmcts.reform.bulkscan.orchestrator.controllers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.microsoft.azure.servicebus.IMessageReceiver
import com.microsoft.azure.servicebus.Message
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.util.SocketUtils
import uk.gov.hmcts.reform.bulkscan.orchestrator.services.ccd.CaseRetriever
import uk.gov.hmcts.reform.bulkscan.orchestrator.services.ccd.CcdAuthenticatorFactory
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi
import java.io.File
import java.util.concurrent.TimeUnit

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [IntegrationTestConfig::class],
    webEnvironment = RANDOM_PORT
)
@TestPropertySource(properties = [
    "core_case_data.api.url=${TestUrls.wiremockUrls.CORE_CASE_DATA_URL}",
    "idam.s2s-auth.url=${TestUrls.wiremockUrls.IDAM_S2S_URL}",
    "idam.api.url=${TestUrls.wiremockUrls.IDAM_API_URL}",
    "idam.users.bulkscan.username=bulkscan+ccd@gmail.com",
    "idam.users.bulkscan.password=Password12",
    "queue.read-interval=100"
])
@AutoConfigureWireMock
class CaseRetrievalTest {
    companion object {
        init {
            //This needs to be done since AutoConfigureWireMock seems to have a bug where its using a random port.
            System.setProperty("wiremock.port", SocketUtils.findAvailableTcpPort().toString())
        }

        val USER_ID = "640"
        val JURIDICTION = "BULKSCAN"
        val CASE_TYPE = CaseRetriever.CASE_TYPE_ID
        val CASE_REF = "1537879748168579"

        private fun retrieveCase() =
            "/caseworkers/${USER_ID}/jurisdictions/${JURIDICTION}/case-types/${CASE_TYPE}/cases/${CASE_REF}"
    }

    @Autowired
    private lateinit var server: WireMockServer

    @Autowired
    private lateinit var factory: CcdAuthenticatorFactory

    @Autowired
    private lateinit var coreCaseDataApi: CoreCaseDataApi

    private lateinit var caseRetriever: CaseRetriever

    @BeforeEach
    fun before() {
        caseRetriever = CaseRetriever(factory, coreCaseDataApi)
    }

    @Test
    fun `Should call to retrieve the case from ccd`() {
        caseRetriever.retrieve(JURIDICTION, CASE_REF)

        server.verify(getRequestedFor(urlEqualTo(retrieveCase())))
    }
}