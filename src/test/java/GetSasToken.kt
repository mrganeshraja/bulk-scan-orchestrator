import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object GetSasToken {

    @JvmStatic
    fun main(args: Array<String>) {

        val sas = getSASToken("amqp://localhost/",
            "send",
            "jKt86pkQS+ikUPJ9kRRKsdiQ1mnzK3mTfFN6+8+R8YY=")
        //Endpoint=sb://bulk-scan-servicebus-kevin.servicebus.windows.net/;SharedAccessKeyName=send;SharedAccessKey=jKt86pkQS+ikUPJ9kRRKsdiQ1mnzK3mTfFN6+8+R8YY=;EntityPath=envelopes
        println(sas)
    }

    private fun getSASToken(resourceUri: String, keyName: String, key: String): String {
        val epoch = System.currentTimeMillis() / 1000L
        val week = 60 * 60 * 24 * 7
        val expiry = java.lang.Long.toString(epoch + week)

        val stringToSign = URLEncoder.encode(resourceUri, "UTF-8") + "\n" + expiry
        val signature = getHMAC256(key, stringToSign)
        return "SharedAccessSignature sr=" + URLEncoder.encode(resourceUri, "UTF-8") + "&sig=" +
            URLEncoder.encode(signature, "UTF-8") + "&se=" + expiry + "&skn=" + keyName
    }


    private fun getHMAC256(key: String, input: String): String {
        val sha256HMAC = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(), "HmacSHA256")
        sha256HMAC.init(secretKey)
        val encoder = Base64.getEncoder()

        return String(encoder.encode(sha256HMAC.doFinal(input.toByteArray(UTF_8))))
    }
}
