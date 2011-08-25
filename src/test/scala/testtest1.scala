import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import scala.collection.mutable.Stack

class StackSpec extends FlatSpec with ShouldMatchers {

  val stack = new Stack[Int]

  "A Stack (when empty)" should "be empty" in {
    stack should be ('empty)
  }

  it should "complain when popped" in {
    evaluating { stack.pop() } should produce [NoSuchElementException]
  }
}

class ExampleSpec extends FlatSpec {

  "A Stack" should "pop values in last-in-first-out order" in {
    val stack = new Stack[Int]
    stack.push(1)
    stack.push(2)
    assert(stack.pop() === 2)
    assert(stack.pop() === 1)
  }

  it should "throw NoSuchElementException if an empty stack is popped" in {
    val emptyStack = new Stack[String]
    intercept[NoSuchElementException] {
      emptyStack.pop()
    }
  }
}

class MyTest1 extends FlatSpec {
  "1" should "equal 1" in {
    assert(1 === 1)
  }

}
