package coordination

import cats.effect.{IO, IOApp, Ref}
import cats.syntax.parallel._

import scala.concurrent.duration._

object SetOnceCacheExample extends IOApp.Simple {

  override def run: IO[Unit] = {

    val actionFunctionRecorder = Ref[IO].of(List.empty[Int])
    val result = actionFunctionRecorder.flatMap { recorder =>
      val someExpensiveCall: Int => IO[String] = i =>
        for {
          _ <- IO.sleep(100.milliseconds)
          _ <- recorder.update(xs => i +: xs)
          _ <- IO.println(s"*******RECORDED: $i")
        } yield s"id: $i"

      val keys = Range(1, 25).inclusive.toList
      val processes = Range(1, 25).inclusive.toList
      // We run 25 different ids with 25 concurrent gets to each key at the same time at the same time
      val ids = for {
        k <- keys
        p <- processes
      } yield (k, p)

      val cache = SetOnceCache.make(someExpensiveCall)

      for {
        cache <- cache
        _ <- ids.parTraverse {
          case (key, process) => IO.println(s"key $key -> process $process") >> cache.get(key)
        }
        calledIds <- recorder.get
        _ <- IO.println("finished\n\n\n------------\n")
      } yield calledIds.sorted
    }
    result.flatMap(xs => IO.println(xs))
  }
}
