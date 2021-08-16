# If an error occurs in your `IO[A]` do you want to...

**perform an effect?**

###### Use onError(pf: PartialFunction[Throwable, IO[Unit]]): IO[A]

**transform any error into another error?***

###### Use adaptError(pf: PartialFunction[Throwable, Throwable]): IO[A]

**transform any error into a successful value?**

###### Use handleError(f: Throwable ⇒ A): IO[A]

**transform some kinds of errors into a successful value?**

###### Use recover(pf: PartialFunction[Throwable, A]): IO[A]

**transform some kinds of errors into another effect?**

###### Use recoverWith(pf: PartialFunction[Throwable, IO[A]]): IO[A]

**make errors visible but delay error-handling?**

###### Use attempt: IO[Either[Throwable, A]]

**otherwise, use**

###### handleErrorWith(f: Throwable ⇒ IO[A]): IO[A].