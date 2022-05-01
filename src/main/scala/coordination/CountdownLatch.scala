/*package coordination

import cats.effect._
import cats.effect.concurrent._
import cats.implicits._

trait CountdownLatch {
  def await: IO[Unit]
  def decrement: IO[Unit]
}

object CountdownLatch {

  def apply(n: Long)(implicit cs: ContextShift[IO]): IO[CountdownLatch] =
    for {
      whenDone <- Deferred[IO, Unit]
      state <- Ref[IO].of[State](Outstanding(n, whenDone))
    } yield new CountdownLatch {
      def await: IO[Unit] =
        state.get.flatMap {
          case Outstanding(_, whenDone) => whenDone.get
          case Done() => IO.unit
        }
      def decrement: IO[Unit] = state.modify {
        case Outstanding(1, whenDone) => Done() -> whenDone.complete(())
        case Outstanding(n, whenDone) => Outstanding(n - 1, whenDone) -> IO.unit
        case Done() => Done() -> IO.unit }.flatten.uncancelable
    }

  sealed trait State
  case class Outstanding(n: Long, whenDone: Deferred[IO, Unit]) extends State
  case class Done() extends State
}*/
