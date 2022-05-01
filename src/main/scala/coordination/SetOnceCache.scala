package coordination

import io.chrisdavenport.mapref.MapRef
import cats.effect._
import cats.implicits._

trait SetOnceCache[K, V] {
  def get(k: K): IO[V]
  def delete(k: K): IO[Unit]
}

object SetOnceCache {
  private sealed trait State[V]
  // If the deferred is already complete we just use the value
  private case class Done[V](value: IO[V]) extends State[V]
  // If you are waiting then the runner with the same unique identifier is responsible to completing the Deferred
  private case class Waiting[V](identifier: Unique.Token, getWait: Deferred[IO, Outcome[IO, Throwable, V]]) extends State[V]

  // When Running only one runner will ever see the Set operation while the remaining runners will be waiting as a Wait operation.
  // If the State is already Done then the operation is already Completed
  private sealed trait Operation[V]
  private case class Set[V](set: Deferred[IO, Outcome[IO, Throwable, V]]) extends Operation[V]
  private case class Wait[V](getWait: Deferred[IO, Outcome[IO, Throwable, V]]) extends Operation[V]
  private case class Completed[V](value: IO[V]) extends Operation[V]

  def make[K, V](f: K => IO[V]): IO[SetOnceCache[K, V]] =
    MapRef
      .ofScalaConcurrentTrieMap[IO, K, State[V]]
      .map { mapRef =>
        new SetOnceCache[K, V] {
          def get(key: K): IO[V] =
            (IO.unique,  Deferred[IO, Outcome[IO, Throwable, V]]).tupled.flatMap {
              case (unique, wait) =>
                // uncancelable means we don't need to worry about other fibers interrupting us in the middle of each of these actions
                IO.uncancelable { poll =>
                  // poll is a "cancelable" block, it re-enables cancelation
                  // val ioa: IO[A] = ???; IO.uncancelable(poll => poll(ioa)) => ioa
                  mapRef(key).modify[Operation[V]] {
                    case None                           => (Some(Waiting(unique, wait)), Set(wait))
                    case state @ Some(Waiting(_, wait)) => (state, Wait(wait))
                    case state @ Some(Done(value))      => (state, Completed(value))
                  }.flatMap {
                    case Set(_) => poll(f(key)).guaranteeCase {
                      case succeeded @ Outcome.Succeeded(fa) =>
                        mapRef(key).modify {
                          case Some(Waiting(identifier, wait)) =>
                            (Some(Done(fa)), if (identifier == unique) wait.complete(succeeded).void else IO.unit)
                          case state @ Some(Done(_)) => (state, IO.unit)  // technically unreachable but type checker :(
                          case None => (None, IO.unit) // technically unreachable but type checker :(
                        }
                      case other => // What if f(k) didn't succeed?
                        mapRef(key).modify {
                          case Some(Waiting(identifier, wait)) =>
                            (None, if (identifier == unique) wait.complete(other).void else IO.unit) // clears the cached value but returns a failure
                          case state @ Some(Done(_)) => (state, IO.unit)
                          case None => (None, IO.unit)
                        }
                    }
                    case Wait(wait) => // What if we are a thread that doesn't have to `Set` the value but is instead waiting on it?
                      poll {
                        wait.get.flatMap { // `.get` "soft" blocks on IO but not on an actual thread
                          case Outcome.Succeeded(fa) => fa
                          case Outcome.Errored(e) => IO.raiseError(e)
                          case Outcome.Canceled() => IO.raiseError(new Throwable("Someone cancelled the setter"))
                        }
                      }
                    case Completed(value) => poll(value)
                  }
                }
              }

          def delete(key: K): IO[Unit] = IO.uncancelable { _ =>
            mapRef(key).modify {
              case Some(Waiting(_, wait)) =>
                (None, wait.complete(Outcome.errored(new Throwable(s"Value evicted while waiting on $key"))).void)
              case Some(Done(_))          => (None, IO.unit)
              case None                   => (None, IO.unit)
            }.flatten
          }
        }
      }
}
