package demo.endpoints

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class EndpointsSpec extends AnyFlatSpec with Matchers {

  "if we add 1 to 3 we" must "get 4" in {
    AdditionOp.op(1, 3) mustBe 4
  }

}
