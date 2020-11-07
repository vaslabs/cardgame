package cardgame.endpoints.codecs

import java.io.{StringReader, StringWriter}
import java.nio.charset.StandardCharsets
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

import org.bouncycastle.util.io.pem.{PemObject, PemWriter}


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


  def show(publicKey: RSAPublicKey): String = {

    val pubKeySpec = new X509EncodedKeySpec(publicKey.getEncoded)

    val pemObject = new PemObject("PUBLIC KEY", pubKeySpec.getEncoded)
    val stringWriter = new StringWriter()
    val pemWriter = new PemWriter(stringWriter)
    pemWriter.writeObject(pemObject)
    pemWriter.flush()
    val bareKey =  stringWriter.getBuffer.toString
      .replaceAll("-----BEGIN PUBLIC KEY-----", "")
      .replaceAll("-----END PUBLIC KEY-----", "")
      .replaceAll("\n", "")
    val content = {
      Base64.getEncoder.encodeToString(
        bareKey.getBytes(StandardCharsets.UTF_8)
      )
    }
    stringWriter.close()
    content

  }
}
