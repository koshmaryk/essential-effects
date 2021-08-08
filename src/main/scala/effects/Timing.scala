package effects

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

object Timing extends App {
  val clock: MyIO[Long] = MyIO(() => System.currentTimeMillis)

  def time[A](action: MyIO[A]): MyIO[(FiniteDuration, A)] =
    for {
      start <- clock
      a <- action
      end <- clock
    } yield (FiniteDuration(end - start, MILLISECONDS), a)

  val timedHello = Timing.time(MyIO.putStr("hello"))

  timedHello.unsafeRun() match {
    case (duration, _) => println(s"'hello' took $duration")
  }
}
