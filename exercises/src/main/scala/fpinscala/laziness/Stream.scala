package fpinscala.laziness

import Stream._

import scala.annotation.tailrec

trait Stream[+A] {

  def foldRight[B](z: => B)(f: (A, => B) => B): B = // The arrow `=>` in front of the argument type `B` means that the function `f` takes its second argument by name and may choose not to evaluate it.
    this match {
      case Cons(h,t) => f(h(), t().foldRight(z)(f)) // If `f` doesn't evaluate its second argument, the recursion never occurs.
      case _ => z
    }

  def exists(p: A => Boolean): Boolean = 
    foldRight(false)((a, b) => p(a) || b) // Here `b` is the unevaluated recursive step that folds the tail of the stream. If `p(a)` returns `true`, `b` will never be evaluated and the computation terminates early.

  @annotation.tailrec
  final def find(f: A => Boolean): Option[A] = this match {
    case Empty => None
    case Cons(h, t) => if (f(h())) Some(h()) else t().find(f)
  }
  def take(n: Int): Stream[A] = {
    def go(n: Int, s: Stream[A], acc: Stream[A]): Stream[A] = {
      if (n == 0) acc
      else this match {
        case Cons(h, t) => go(n - 1, t(), cons(h(), acc))
        case _ => acc
      }
    }
    go(n, this, Stream[A]())

  }

  @tailrec
  final def drop(n: Int): Stream[A] = this match {
    case Cons(_, t) if n > 0 => t().drop(n - 1)
    case _ => this
  }

  def takeWhile(p: A => Boolean): Stream[A] = this match {
    case Cons(h, t) if p(h()) => cons(h(), t() takeWhile(p))
    case _ => empty
  }

  def takeWhile2(p: A => Boolean): Stream[A] =
    foldRight(Empty[A])((a,b) =>
      if(p(a)) cons(a,b)
      else b)

  def forAll(p: A => Boolean): Boolean =
    foldRight(true) ((a, b) => p(a) && b)

  def headOption: Option[A] = this match {
    case Cons(h, _) => Some(h())
    case _ => None
  }

  def headOption2: Option[A] =
    foldRight(None: Option[A])((h, _) => Some(h))

  def toListRecursive: List[A] = this match {
    case Cons(h,t) => h() :: t().toListRecursive
    case _ => List()
  }

  def toList: List[A] = {
    @annotation.tailrec
    def go(s: Stream[A], acc: List[A]): List[A] = s match {
      case Cons(h,t) => go(t(), h() :: acc)
      case _ => acc
    }
    go(this, List[A]())
  }


  // 5.7 map, filter, append, flatmap using foldRight. Part of the exercise is
  // writing your own function signatures.

  def map[B](f: A => B): Stream[B] =
    foldRight(Empty[B])((a, acc) =>
      cons(f(a), acc))

  def filter(p: A => Boolean): Stream[A] =
    foldRight(Empty[A])((a, acc) =>
      if (p(a)) cons(a, acc)
      else acc
  )

  def append[B >: A](s: Stream[B]): Stream[B] =
    foldRight(this)((a, acc) =>
      cons(a, acc)
    )


  def flatMap[B](f: A => Stream[B]): Stream[B] =
    foldRight(Empty[B])((a, acc) => f(a) append acc
  )


  def startsWith[B](s: Stream[B]): Boolean = sys.error("todo")
}
case object Empty extends Stream[Nothing]
case class Cons[+A](h: () => A, t: () => Stream[A]) extends Stream[A]

object Stream {
  def cons[A](hd: => A, tl: => Stream[A]): Stream[A] = {
    lazy val head = hd
    lazy val tail = tl
    Cons(() => head, () => tail)
  }

  def empty[A]: Stream[A] = Empty

  def apply[A](as: A*): Stream[A] =
    if (as.isEmpty) empty 
    else cons(as.head, apply(as.tail: _*))

  val ones: Stream[Int] = Stream.cons(1, ones)

  def constant[A](a: A): Stream[A] = {
    lazy val result: Stream[A] = Stream.cons(a, result)
    result
  }


  def from(n: Int): Stream[Int] = cons(n, from(n + 1))

  val fibs = {
    def go(n1: Int, n2: Int):Stream[Int] = cons(n1, go(n2, n1 + n2))
    go(0,1)
  }

  def unfold[A, S](z: S)(f: S => Option[(A, S)]): Stream[A] = sys.error("todo")
}