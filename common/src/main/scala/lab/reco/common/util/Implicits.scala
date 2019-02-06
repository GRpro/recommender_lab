package lab.reco.common.util

import com.sksamuel.elastic4s.http.Response
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object Implicits {

  implicit class ElasticOperationsFutureOps[T](val future: Future[Response[T]]) extends AnyVal {

    def escalateElasticFailure(implicit executionContext: ExecutionContext): Future[Response[T]] =
      future.map { response =>
        if (response.isError) {
          throw new RuntimeException(response.error.toString)
        }
        response
      }

  }

  implicit class FutureOps[T](val future: Future[T]) extends AnyVal {

    def logFailure(logger: Logger, message: String)(implicit executionContext: ExecutionContext): Future[T] =
      future recover {
        case NonFatal(e) =>
          logger.warn(message, e)
          throw e
      }


    def logSuccess(logger: Logger, message: String)(implicit executionContext: ExecutionContext): Future[T] =
      future map { res =>
        logger.info(message)
        res
      }
  }

}
