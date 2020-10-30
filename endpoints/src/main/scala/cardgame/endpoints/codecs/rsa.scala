package cardgame.endpoints.codecs

import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object rsa {
  def fromString(base64: String): RSAPublicKey = {
    import java.security.KeyFactory

    import org.bouncycastle.util.io.pem.PemReader
    val content = new String(Base64.getDecoder.decode(base64), StandardCharsets.UTF_8)
    val pemString =
      s"""
        |-----BEGIN PUBLIC KEY-----
        |${content}
        |-----END PUBLIC KEY-----
        |""".stripMargin
    val factory = KeyFactory.getInstance("RSA")
    val pemReader = new PemReader(new StringReader(pemString))
    val pemObject = pemReader.readPemObject()
    val keyBytes = pemObject.getContent
    val pubKeySpec = new X509EncodedKeySpec(keyBytes)
    factory.generatePublic(pubKeySpec).asInstanceOf[RSAPublicKey]
  }
}
