Concurrency means that a single logical thread can have its tasks distributed across different threads.
Or, from the thread perspective, that a single thread can execute interleaved tasks coming from different logical threads.

In order to be able to do so, we have to use asynchronous operations.
Once the task has been initiated, it crosses the **asynchronous boundary**, and it resumes somewhere else.

###### Asynchronous process is a process that continues its execution in a different place or time than the one it started in.

`IO` can describe asynchronous process via the `IO.async`.
```scala
object IO {
  def async[A](k: (Either[Throwable, A] => Unit) => Unit): IO[A]
}
```

Method `async` provides us with a way to describe an asynchronous operation (that is, operation 
that happens on the other side of an asynchronous boundary) in our synchronous code.

We could create a type alias for part of the signature, it becomes a bit easier to understand:
```scala
type Callback[A] = Either[Throwable, A] => Unit

type AsyncProcess[A] = Callback[A] => Unit
```
`k` stands for continuation, `async` invokes it, providing a callback `cb`. A callback is a function 
that *receives* the result of a computation. In this case, the result is either an error (`Throwable`) 
or a successful value of type `A`. Then we will invoke callback when, and
only when, we have a result of the asynchronous computation.
```scala
trait API {
  def compute: Future[Int] = ???
}

val k: (Either[Throwable,Int] => Unit) => Unit = 
  cb => api.compute.onComplete {
    case Failure(t) => cb(Left(t))
    case Success(a) => cb(Right(a))
  }
```