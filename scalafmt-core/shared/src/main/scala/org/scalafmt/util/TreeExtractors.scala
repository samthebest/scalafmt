package org.scalafmt.util

import scala.meta._

case class InfixApp(lhs: Tree, op: Name, rhs: Seq[Tree], all: Tree) {

  @inline
  def isAssignment: Boolean = InfixApp.isAssignment(op.value)

  @inline
  lazy val precedence: Int = InfixApp.getPrecedence(op.value)

}

object InfixApp {

  def apply(t: Type.ApplyInfix): InfixApp = InfixApp(t.lhs, t.op, Seq(t.rhs), t)
  def apply(t: Term.ApplyInfix): InfixApp = InfixApp(t.lhs, t.op, t.args, t)
  def apply(t: Pat.ExtractInfix): InfixApp = InfixApp(t.lhs, t.op, t.rhs, t)

  def unapply(tree: Tree): Option[InfixApp] =
    tree match {
      case t: Type.ApplyInfix => Some(apply(t))
      case t: Term.ApplyInfix => Some(apply(t))
      case t: Pat.ExtractInfix => Some(apply(t))
      case _ => None
    }

  // https://scala-lang.org/files/archive/spec/2.11/06-expressions.html#infix-operations
  private val infixOpPrecedence: Map[Char, Int] = {
    val builder = Map.newBuilder[Char, Int]
    def add(precedence: Int, ops: Char*): Unit = addSeq(precedence, ops)
    def addSeq(precedence: Int, ops: Iterable[Char]): Unit =
      ops.foreach(builder += _ -> precedence)
    // start with 1; all other special characters will get 0 by omission
    add(1, '*', '/', '%')
    add(2, '+', '-')
    add(3, ':')
    add(4, '<', '>')
    add(5, '=', '!')
    add(6, '&')
    add(7, '^')
    add(8, '|')
    addSeq(9, 'a' to 'z')
    addSeq(9, 'A' to 'Z')
    builder.result()
  }

  // https://scala-lang.org/files/archive/spec/2.11/06-expressions.html#assignment-operators
  private val nonAssignmentOperators = Set("<=", ">=", "!=")

  // https://scala-lang.org/files/archive/spec/2.11/06-expressions.html#infix-operations
  // The precedence of an assignment ... is lower than the precedence of any other ...
  def isAssignment(op: String): Boolean =
    op.lastOption.contains('=') && (op.length == 1 ||
      op.head != '=' && !nonAssignmentOperators.contains(op))

  // https://scala-lang.org/files/archive/spec/2.11/06-expressions.html#infix-operations
  // Operators ending in a colon `:' are right-associative. All other operators are left-associative.
  @inline def isLeftAssoc(op: String): Boolean = op.last != ':'

  @inline def getPrecedence(op: String): Int =
    infixOpPrecedence.getOrElse(op.head, 0)

}

object WithChain {
  def unapply(t: Type.With): Option[Type.With] = {
    // self types, params, val/def/var/type definitions or declarations
    val top = TreeOps.topTypeWith(t)
    top.parent.collect {
      case _: Defn | _: Decl | _: Term.Param | _: Self | _: Type.Apply =>
        top
    }
  }
}
