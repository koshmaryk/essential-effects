# Resource
Resource handling refers to the concept of acquiring some resource 
(e.g. opening a file, connecting to the database etc.) and releasing it after usage.

```scala
def mkResource(s: String): Resource[IO, String] = {
  val acquire = IO(println(s"Acquiring $s")) *> IO.pure(s)
  def release(s: String) = IO(println(s"Releasing $s"))
  Resource.make(acquire)(release)
}

val r = for {
  outer <- mkResource("outer")
  inner <- mkResource("inner")
} yield (outer, inner)

override def run(args: List[String]): IO[ExitCode] =
  r.use { case (a, b) => IO(println(s"Using $a and $b")) }.map(_ => ExitCode.Success)
```
Resource takes care of the LIFO (Last-In-First-Out) order of acquiring / releasing. 
The output of the above program is:
```
Acquiring outer
Acquiring inner
Using outer and inner
Releasing inner
Releasing outer
```

Both `acquire` and `release` are non-interruptible and hence safe in the face of cancelation.
Outer resources will be released irrespective of failure in the lifecycle of an inner resource.