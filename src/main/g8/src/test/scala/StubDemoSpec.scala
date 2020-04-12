package $package$

import $package$.model.database.User
import $package$.model.response.ErrorResponse
import $package$.route.Endpoints._
import sttp.client.Identity
import sttp.client._
import sttp.client.monad.MonadError
import sttp.client.monad._
import sttp.client.testing.SttpBackendStub
import sttp.tapir.client.sttp._
import sttp.tapir.server.stub._

import zio.test.Assertion._
import zio.test._

object StubDemoSpec extends DefaultRunnableSpec {
  def spec = suite("Endpoints Spec")(
    test("Validate getUserEndpoint") {

      implicit val backend = SttpBackendStub
      // .withFallback(???)
        .apply(idMonad)
        .whenRequestMatches(_.uri.path.startsWith(List("a", "b")))
        .thenRespond("Hello there!")
        .whenRequestMatches(getUserEndpoint)
        .thenSuccess(user)

      val resp  = getUserEndpoint.toSttpRequestUnsafe(uri"http://test.com").apply(11).send()
      val resp1 = basicRequest.get(uri"http://example.org/a/b/c").send()
      println(resp1)

      val exp: Response[Either[ErrorResponse, User]] = Response.ok(Right(user))

      assert(resp)(equalTo(exp))

    }
  )

  implicit val idMonad: MonadError[Identity] = IdMonad

  val user = User(0, "Boris", 10)

}
