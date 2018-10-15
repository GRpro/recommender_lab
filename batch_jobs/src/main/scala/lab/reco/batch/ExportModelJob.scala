package lab.reco.batch


object ExportModelJob {

  trait Foo {
    type Bar
    def bar: Bar
  }

  class A extends Foo {
    override type Bar = String
    override def bar: Bar = "a"
  }

  class B extends Foo {
    override type Bar = Int
    override def bar: Bar = 1
  }

  def foo(f: Foo): f.Bar = f.bar

  def main(args: Array[String]): Unit = {

    trait Functor[F[_]]
    type F1 = Functor[Option] // OK
    type F2 = Functor[List]   // OK
    type IntKeyMap[A] = Map[Int, A]

    type F3 = Functor[IntKeyMap] // OK
    type F4 = Functor[({ type T[A] = Map[Int, A] })#T]

    val a = new A
    val b = new B

    def foo1[A[_, _], B](functor: Functor[({ type C[K] = A[B, K] })#C]): Unit = ()

    println(foo(a))
    println(foo(b))
  }




}
