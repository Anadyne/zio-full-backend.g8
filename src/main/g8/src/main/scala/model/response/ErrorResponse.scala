package $package$.model.response

sealed trait ErrorResponse extends Product with Serializable

final case class InternalServerErrorResponse(message: String, exceptionMessage: String, exception: String)
    extends ErrorResponse

final case class NotFoundResponse(message: String)   extends ErrorResponse
final case class BadRequestResponse(message: String) extends ErrorResponse
