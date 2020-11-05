package cardgame.endpoints.codecs

import org.scalatest.flatspec.AnyFlatSpec

class Base64StringToRSAPublicKeySpec extends AnyFlatSpec {

  val value = "DU1JR2ZNQTBHQ1NxR1NJYjNEUUVCQVFVQUE0R05BRENCaVFLQmdRQ082ajVJSmdCQnVxTHhmaTA5Yy8rWWJGZHENRHcza0NOK0FmaWVjZVptVjNSRDFEQ3AzdlpFU0lwN3p5RFNPSHRMRktRQ0FENzRvUWhMNTlUUWlqWFFXYkNucA14N251azRYNjFYYWV3bnROZlFKdHFYN1lIalNQMFV5YlJqMXVXckw1bEdpdXNqeTdlTWQxaGZic2tzbmJudG9iDW15K0RkYkxrWjZIZkVjY21PUUlEQVFBQg0N"

  "rsakey" must "be created from base54" in {
    rsa.fromString(value)
  }

}
