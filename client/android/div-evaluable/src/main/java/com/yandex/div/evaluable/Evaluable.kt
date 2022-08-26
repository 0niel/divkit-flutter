package com.yandex.div.evaluable

import com.yandex.div.evaluable.internal.Parser
import com.yandex.div.evaluable.internal.Token
import com.yandex.div.evaluable.internal.Tokenizer

abstract class Evaluable(val rawExpr: String) {

    abstract val variables: List<String>

    @Throws(EvaluableException::class)
    internal abstract fun eval(evaluator: Evaluator): Any

    internal class Lazy(private val expr: String): Evaluable(expr) {
        private val tokens = Tokenizer.tokenize(expr)
        private lateinit var expression: Evaluable
        override val variables: List<String>
            get() =  if (this::expression.isInitialized) {
                expression.variables
            } else {
                tokens.filterIsInstance(Token.Operand.Variable::class.java).map { it.name }
            }
        override fun eval(evaluator: Evaluator): Any {
            if (!this::expression.isInitialized) {
                expression = Parser.parse(tokens, rawExpr)
            }
            return expression.eval(evaluator)
        }
        override fun toString(): String = expr
    }

    internal data class Unary(
        val token: Token.Operator,
        val expression: Evaluable,
        val rawExpression: String,
    ) : Evaluable(rawExpression) {
        override val variables: List<String> = expression.variables
        override fun eval(evaluator: Evaluator): Any = evaluator.evalUnary(this)
        override fun toString(): String = "$token$expression"
    }

    internal data class Binary(
        val token: Token.Operator.Binary,
        val left: Evaluable,
        val right: Evaluable,
        val rawExpression: String,
    ) : Evaluable(rawExpression) {
        override val variables: List<String> = left.variables + right.variables
        override fun eval(evaluator: Evaluator): Any = evaluator.evalBinary(this)
        override fun toString(): String = "($left $token $right)"
    }

    internal data class Ternary(
        val token: Token.Operator,
        val firstExpression: Evaluable,
        val secondExpression: Evaluable,
        val thirdExpression: Evaluable,
        val rawExpression: String,
    ) : Evaluable(rawExpression) {
        override val variables: List<String> = firstExpression.variables +
                secondExpression.variables + thirdExpression.variables
        override fun eval(evaluator: Evaluator): Any = evaluator.evalTernary(this)
        override fun toString(): String {
            val opIf = Token.Operator.TernaryIf
            val opElse = Token.Operator.TernaryElse
            return "($firstExpression $opIf $secondExpression $opElse $thirdExpression)"
        }
    }

    internal data class FunctionCall(
        val token: Token.Function,
        val arguments: List<Evaluable>,
        val rawExpression: String,
    ) : Evaluable(rawExpression) {
        override val variables: List<String> =
            arguments.map { it.variables }.reduceOrNull { acc, vars -> acc + vars } ?: emptyList()
        override fun eval(evaluator: Evaluator): Any = evaluator.evalFunctionCall(this)
        override fun toString(): String {
            val argsString = arguments.joinToString(separator = Token.Function.ArgumentDelimiter.toString())
            return "${token.name}($argsString)"
        }
    }

    internal data class StringTemplate(
        val arguments: List<Evaluable>,
        val rawExpression: String,
    ) : Evaluable(rawExpression) {
        override val variables: List<String> = arguments.map { it.variables }.reduce { acc, vars -> acc + vars }
        override fun eval(evaluator: Evaluator): Any = evaluator.evalStringTemplate(this)
        override fun toString(): String = arguments.joinToString(separator = "")
    }

    internal data class Variable(
        val token: Token.Operand.Variable,
        val rawExpression: String,
    ) : Evaluable(rawExpression) {
        override val variables: List<String> = listOf(token.name)
        override fun eval(evaluator: Evaluator): Any = evaluator.evalVariable(this)
        override fun toString(): String = token.name
    }

    internal data class Value(
        val token: Token.Operand.Literal,
        val rawExpression: String,
    ) : Evaluable(rawExpression) {
        override val variables: List<String> = emptyList()
        override fun eval(evaluator: Evaluator): Any = evaluator.evalValue(this)
        override fun toString(): String = when (token) {
            is Token.Operand.Literal.Str -> "'${token.value}'"
            is Token.Operand.Literal.Num -> token.value.toString()
            is Token.Operand.Literal.Bool -> token.value.toString()
        }
    }

    companion object {
        @JvmStatic
        fun prepare(expr: String) : Evaluable {
            return Parser.parse(Tokenizer.tokenize(expr), expr)
        }

        @JvmStatic
        fun lazy(expr: String) : Evaluable {
            return Lazy(expr)
        }
    }
}