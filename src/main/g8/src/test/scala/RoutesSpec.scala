package $package$.client

import io.circe.Json
import io.circe.generic.auto._
import io.circe.generic.auto._, io.circe.syntax._

import $package$.client.Client
import $package$.model.database.User
import $package$.model.database.User
import sttp.client._
import sttp.model.MediaType.ApplicationJson

import zio.test.Assertion._
import zio.test.TestAspect.ignore
import zio.test._
import zio.{ ZEnv }

object RoutesSpec extends DefaultRunnableSpec {
  def spec = suite("Routes Spec")(
    testM("Hello World Request") {

      val req: RequestT[sttp.client.Identity, String, Nothing] = basicRequest
        .get(uri"http://localhost:5566/hello?name=Boris")
        .response(asStringAlways)

      val exp: Response[Either[Nothing, String]] = Response.ok(Right(user.asJson.toString))

      assertM(run(req))(equalTo(exp))
    } @@ ignore,
    testM("Create User") {
      val req: RequestT[sttp.client.Identity, Either[String, String], Nothing] = basicRequest
        .post(uri"http://localhost:5566/user")
        .body(user.asJson)

      val exp: Response[Either[Nothing, Unit]] = Response.ok(Right(()))

      assertM(run(req))(equalTo(exp))
    } @@ ignore,
    testM("Get User") {
      val req: RequestT[sttp.client.Identity, String, Nothing] = basicRequest
        .get(uri"http://localhost:5566/user/11")
        .response(asStringAlways)

      val exp: Response[Json] = Response.ok(user.asJson)
      assertM(run(req))(equalTo(exp))
    },
    testM("Delete User") {
      val req: RequestT[sttp.client.Identity, String, Nothing] = basicRequest
        .delete(uri"http://localhost:5566/user/11")
        .response(asStringAlways)

      val exp: Response[Unit] = Response.ok(())
      assertM(run(req))(equalTo(exp))
    } @@ ignore
  )

  // Define a Serializer
  implicit val userSerializer: BodySerializer[Json] = { usr: Json =>
    StringBody(usr.toString, "UTF-8", Some(ApplicationJson))
  }

  val user = User(11, "Boris", 15)

  private val client = new Client()

  private def run[A](req: RequestT[sttp.client.Identity, A, Nothing]) = client.run[A](req).provideLayer(ZEnv.live)

}
