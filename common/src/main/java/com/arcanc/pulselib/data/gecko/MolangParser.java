/**
 * @author ArcAnc
 * Created at: 30.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data.gecko;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Parses and evaluates Molang expressions against frame queries, persistent instance variables and
 * deterministic random state.
 */
public final class MolangParser
{
	private static final int MAX_LOOP_ITERATIONS = 1_024;

	/**
	 * Creates a molang parser.
	 */
	private MolangParser()
	{
	}

	/**
	 * Parses source text into a reusable Molang expression.
	 *
	 * @param source the Molang source text
	 * @return the reusable parsed expression
	 */
	public static Expression parse(String source)
	{
		if (source == null || source.isBlank())
			throw new IllegalArgumentException("Molang expression must not be blank");
		return new Parser(source).parse();
	}

	/**
	 * Parses and evaluates Molang source text using the supplied context.
	 *
	 * @param source the Molang source text
	 * @param context the Molang context containing current frame queries and persistent instance values
	 * @return the expression's numeric value
	 */
	public static float evaluate(String source, Context context)
	{
		return parse(source).evaluate(context);
	}

	/**
	 * Parsed expression that can be evaluated repeatedly with different Molang contexts.
	 */
	@FunctionalInterface
	public interface Expression
	{
		/**
		 * Evaluates this parsed expression against the supplied context values.
		 *
		 * @param context the Molang context containing current frame queries and persistent instance values
		 * @return the expression's numeric value
		 */
		float evaluate(Context context);

		/**
		 * Returns the context-dependency classification of this expression.
		 *
		 * @return the context-dependency classification of this expression
		 */
		default PExpressionDependency dependency()
		{
			return PExpressionDependency.INSTANCE;
		}

		/**
		 * Evaluates this parsed expression against the supplied context values.
		 *
		 * @param data the optional expression context used to sample source-format animation values
		 * @return the expression's numeric value
		 */
		default float evaluate(Object data)
		{
			if (data instanceof Context context)
				return evaluate(context);
			if (data instanceof Map<?, ?> values)
				return evaluate(Context.from(values));
			return evaluate(new Context());
		}
	}

	/**
	 * Mutable Molang namespaces, frame queries, component values and persistent instance state.
	 */
	public static final class Context
	{
		private final Map<String, Float> queryValues = new HashMap<>();
		private final Map<String, Float> variables = new HashMap<>();
		private final Map<String, Float> contextValues = new HashMap<>();
		private @Nullable QueryResolver queryResolver;
		private float thisValue;
		private float thisX;
		private float thisY;
		private float thisZ;
		private final RandomSource random = RandomSource.create();

		/**
		 * Creates a Molang context from supported namespaced map values.
		 *
		 * @param values the values
		 * @return the populated Molang context
		 */
		public static Context from(Map<?, ?> values)
		{
			Context context = new Context();
			for (Map.Entry<?, ?> entry : values.entrySet())
				if (entry.getKey() != null && entry.getValue() instanceof Number number)
					context.value(String.valueOf(entry.getKey()), number.floatValue());
			return context;
		}

		/**
		 * Stores a directly addressed Molang value in this context.
		 *
		 * @param name the namespaced Molang value name
		 * @param value the numeric value stored in the selected Molang namespace
		 * @return this instance
		 */
		public Context value(String name, float value)
		{
			String normalized = normalizeName(name);
			if (normalized.startsWith("query."))
				this.queryValues.put(normalized.substring("query.".length()), value);
			else if (normalized.startsWith("variable."))
				this.variables.put(normalized.substring("variable.".length()), value);
			else if (normalized.startsWith("context."))
				this.contextValues.put(normalized.substring("context.".length()), value);
			else
				this.queryValues.put(normalized, value);
			return this;
		}

		/**
		 * Stores a query-namespace value for the current frame.
		 *
		 * @param name the namespaced Molang value name
		 * @param value the numeric value stored in the selected Molang namespace
		 * @return this instance
		 */
		public Context query(String name, float value)
		{
			this.queryValues.put(stripNamespace(normalizeName(name), "query"), value);
			return this;
		}

		/**
		 * Stores a persistent instance variable in this context.
		 *
		 * @param name the namespaced Molang value name
		 * @param value the numeric value stored in the selected Molang namespace
		 * @return this instance
		 */
		public Context variable(String name, float value)
		{
			this.variables.put(stripNamespace(normalizeName(name), "variable"), value);
			return this;
		}

		/**
		 * Stores a context-namespace value for this instance.
		 *
		 * @param name the namespaced Molang value name
		 * @param value the numeric value stored in the selected Molang namespace
		 * @return this instance
		 */
		public Context context(String name, float value)
		{
			this.contextValues.put(stripNamespace(normalizeName(name), "context"), value);
			return this;
		}

		/**
		 * Sets the scalar Molang {@code this} value for the next evaluation.
		 *
		 * @param value the numeric value stored in the selected Molang namespace
		 * @return this instance
		 */
		public Context thisValue(float value)
		{
			this.thisValue = value;
			return this;
		}

		/**
		 * Stores accumulated vector components used by component-wise Molang {@code this} evaluation.
		 *
		 * @param x the x component or coordinate
		 * @param y the y component or coordinate
		 * @param z the z component or coordinate
		 * @return this instance
		 */
		public Context thisValues(float x, float y, float z)
		{
			this.thisX = x;
			this.thisY = y;
			this.thisZ = z;
			return this;
		}

		/**
		 * Selects the accumulated vector component used as Molang {@code this} .
		 *
		 * @param component the zero-based vector component selected for Molang this-value evaluation
		 * @return the selected accumulated component used as the scalar this-value
		 */
		public float thisComponent(int component)
		{
			return switch (component)
			{
				case 0 -> this.thisX;
				case 1 -> this.thisY;
				case 2 -> this.thisZ;
				default -> throw new IndexOutOfBoundsException("Molang vector component: " + component);
			};
		}

		/**
		 * Sets the callback used to resolve dynamic game queries.
		 *
		 * @param resolver the resolver
		 * @return this instance
		 */
		public Context queryResolver(QueryResolver resolver)
		{
			this.queryResolver = resolver;
			return this;
		}

		/**
		 * Initializes deterministic random state for this instance context.
		 *
		 * @param seed the deterministic random seed
		 * @return this instance
		 */
		public Context randomSeed(long seed)
		{
			this.random.setSeed(seed);
			return this;
		}

		/**
		 * Refreshes frame and query inputs while retaining controller-owned persistent state.
		 *
		 * @param source the context supplying current frame values
		 * @return this instance
		 */
		public Context copyFrameValuesFrom(Context source)
		{
			this.queryValues.clear();
			this.queryValues.putAll(source.queryValues);
			this.contextValues.clear();
			this.contextValues.putAll(source.contextValues);
			this.queryResolver = source.queryResolver;
			this.thisValue = source.thisValue;
			this.thisX = source.thisX;
			this.thisY = source.thisY;
			this.thisZ = source.thisZ;
			return this;
		}

		/**
		 * Resolves a namespaced Molang value or dynamic query in the current evaluation.
		 *
		 * @param name the namespaced Molang value name
		 * @param arguments the evaluated query arguments
		 * @param evaluation the current Molang evaluation state and control-flow environment
		 * @return the resolved namespaced Molang value or dynamic query in the current evaluation
		 */
		private Value resolve(String name, List<Value> arguments, Evaluation evaluation)
		{
			name = normalizeName(name);
			if (name.equals("this"))
				return Value.of(this.thisValue);
			if (name.equals("true"))
				return Value.of(1f);
			if (name.equals("false"))
				return Value.of(0f);
			if (name.equals("math.pi"))
				return Value.of((float) Math.PI);

			if (name.startsWith("math."))
				return math(name.substring("math.".length()), arguments,
						this :: random);
			if (name.startsWith("temp."))
				return Value.ofNullable(evaluation.temporary.get(name.substring("temp.".length())));
			if (name.startsWith("variable."))
				return Value.ofNullable(this.variables.get(name.substring("variable.".length())));
			if (name.startsWith("context."))
				return Value.ofNullable(this.contextValues.get(name.substring("context.".length())));
			if (name.startsWith("query."))
			{
				String query = name.substring("query.".length());
				if (this.queryResolver != null)
				{
					Float result = this.queryResolver.resolve(query, arguments.stream().map(Value :: number).toList(), this);
					if (result != null)
						return Value.of(result);
				}
				return Value.ofNullable(this.queryValues.get(query));
			}
			return Value.UNDEFINED;
		}

		/**
		 * Samples the context-owned random source within the requested bounds.
		 *
		 * @param roll the deterministic sample index for the random operation
		 * @param low the lower random sampling bound
		 * @param high the upper random sampling bound
		 * @param integer whether sampling is restricted to integer values
		 * @return the sampled context-owned random source within the requested bounds
		 */
		private float random(int roll, float low, float high, boolean integer)
		{
			float normalized = this.random.nextFloat();
			float from = Math.min(low, high);
			float to = Math.max(low, high);
			if (integer)
			{
				int minimum = (int) from;
				int maximum = (int) to;
				return minimum == maximum ? minimum : minimum + (int) (normalized * (maximum - minimum + 1L));
			}
			return from + normalized * (to - from);
		}

		/**
		 * Writes an evaluated value into the selected persistent or temporary namespace.
		 *
		 * @param name the namespaced Molang value name
		 * @param value the numeric value stored in the selected Molang namespace
		 * @param evaluation the current Molang evaluation state and control-flow environment
		 * @return the value assigned to the selected namespace
		 */
		private Value assign(String name, Value value, Evaluation evaluation)
		{
			name = normalizeName(name);
			float number = value.number();
			if (name.startsWith("temp."))
				evaluation.temporary.put(name.substring("temp.".length()), number);
			else if (name.startsWith("variable."))
				this.variables.put(name.substring("variable.".length()), number);
			else
				throw new IllegalArgumentException("Only temp.* and variable.* values are writable: " + name);
			return Value.of(number);
		}
	}

	/**
	 * Resolves dynamic game queries that are not supplied as direct context values.
	 */
	@FunctionalInterface
	public interface QueryResolver
	{
		/**
		 * Resolves a dynamic query, returning {@code null} when the query is not handled.
		 *
		 * @param name the namespaced Molang value name
		 * @param arguments the evaluated query arguments
		 * @param context the Molang context containing current frame queries and persistent instance values
		 * @return the query value, or {@code null} if the query is not handled
		 */
		Float resolve(String name, List<Float> arguments, Context context);
	}

	/**
	 * Per-evaluation namespaces, temporary variables and control-flow state used by Molang syntax nodes.
	 */
	private static final class Evaluation
	{
		private final Context context;
		private final Map<String, Float> temporary = new HashMap<>();
		private Flow flow = Flow.NONE;
		private Value result = Value.of(0f);

		/**
		 * Creates temporary evaluation state around the supplied persistent Molang context.
		 *
		 * @param context the Molang context containing current frame queries and persistent instance values
		 */
		private Evaluation(Context context)
		{
			this.context = context;
		}
	}

	/**
	 * Control-flow signal carried between evaluated Molang syntax nodes.
	 */
	private enum Flow
	{
		/**
		 * Continues evaluation normally.
		 */
		NONE,
		/**
		 * Returns a numeric result from the program.
		 */
		RETURN,
		/**
		 * Exits the current loop.
		 */
		BREAK,
		/**
		 * Starts the next iteration of the current loop.
		 */
		CONTINUE
	}

	/**
	 * Numeric or missing Molang value paired with its control-flow signal.
	 *
	 * @param value the value written by this operation; may be {@code null}
	 */
	private record Value(@Nullable Float value)
	{
		private static final Value UNDEFINED = new Value(null);

		/**
		 * Creates a numeric Molang value with normal control flow.
		 *
		 * @param value the value written by this operation
		 * @return the created numeric Molang value with normal control flow
		 */
		private static Value of(float value)
		{
			return new Value(value);
		}

		/**
		 * Converts an optional query value into a numeric or missing Molang value.
		 *
		 * @param value the value written by this operation; may be {@code null}
		 * @return a numeric value or the missing-value representation
		 */
		private static Value ofNullable(@Nullable Float value)
		{
			return value == null ? UNDEFINED : of(value);
		}

		/**
		 * Returns the numeric representation of this evaluated value.
		 *
		 * @return the numeric representation of this evaluated value
		 */
		private float number()
		{
			return this.value == null ? 0f : this.value;
		}

		/**
		 * Returns whether this value satisfies Molang truth-value rules.
		 *
		 * @return whether this value satisfies Molang truth-value rules
		 */
		private boolean truthy()
		{
			return number() != 0f;
		}
	}

	/**
	 * Executable node in the parsed Molang syntax tree.
	 */
	@FunctionalInterface
	private interface Node
	{
		/**
		 * Evaluates this syntax node and returns its numeric result and control-flow state.
		 *
		 * @param evaluation the current Molang evaluation state and control-flow environment
		 * @return the numeric or missing value and its control-flow signal
		 */
		Value evaluate(Evaluation evaluation);
	}

	/**
	 * Parsed Molang statement sequence with its conservative context dependency.
	 *
	 * @param statements the statements
	 * @param dependency the dependency
	 * @param constantValue the constant value; may be {@code null}
	 */
	private record Program(List<Node> statements, PExpressionDependency dependency, @Nullable Float constantValue) implements Expression
	{
		/**
		 * {@inheritDoc}
		 */
		@Override
		public float evaluate(@Nullable Context context)
		{
			if (this.constantValue != null)
				return this.constantValue;
			Evaluation evaluation = new Evaluation(context == null ? new Context() : context);
			Value value = Value.of(0f);
			for (Node statement : this.statements)
			{
				value = statement.evaluate(evaluation);
				if (evaluation.flow == Flow.RETURN)
					return evaluation.result.number();
				if (evaluation.flow != Flow.NONE)
					throw new IllegalArgumentException("Molang " + evaluation.flow.name().toLowerCase(Locale.ROOT) + " used outside a loop");
			}
			return value.number();
		}
	}

	/**
	 * Molang syntax node containing a constant numeric value.
	 *
	 * @param value the value written by this operation
	 */
	private record Literal(float value) implements Node
	{
		/**
		 * {@inheritDoc}
		 */
		@Override public Value evaluate(Evaluation evaluation) { return Value.of(this.value); }
	}

	/**
	 * Molang syntax node that reads a namespaced variable or query.
	 *
	 * @param name the namespaced Molang value name
	 */
	private record Variable(String name) implements Node
	{
		/**
		 * {@inheritDoc}
		 */
		@Override public Value evaluate(Evaluation evaluation) { return evaluation.context.resolve(this.name, List.of(), evaluation); }
	}

	/**
	 * Molang syntax node that invokes a math function or dynamic query.
	 *
	 * @param name the namespaced Molang value name
	 * @param arguments the evaluated query arguments
	 */
	private record Call(String name, List<Node> arguments) implements Node
	{
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Value evaluate(Evaluation evaluation)
		{
			List<Value> values = new ArrayList<>(this.arguments.size());
			for (Node argument : this.arguments)
			{
				values.add(argument.evaluate(evaluation));
				if (evaluation.flow != Flow.NONE)
					return Value.of(0f);
			}
			return evaluation.context.resolve(this.name, values, evaluation);
		}
	}

	/**
	 * Molang syntax node applying a unary numeric or logical operator.
	 *
	 * @param operator the operator
	 * @param value the value written by this operation
	 */
	private record Unary(String operator, Node value) implements Node
	{
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Value evaluate(Evaluation evaluation)
		{
			Value result = this.value.evaluate(evaluation);
			return switch (this.operator)
			{
				case "-" -> Value.of(-result.number());
				case "!" -> Value.of(result.truthy() ? 0f : 1f);
				default -> throw new IllegalStateException("Unknown unary operator " + this.operator);
			};
		}
	}

	/**
	 * Molang syntax node applying a binary operator with short-circuit semantics where required.
	 *
	 * @param operator the operator
	 * @param left the left
	 * @param right the local right basis direction
	 */
	private record Binary(String operator, Node left, Node right) implements Node
	{
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Value evaluate(Evaluation evaluation)
		{
			Value a = this.left.evaluate(evaluation);
			if (this.operator.equals("&&") && !a.truthy())
				return Value.of(0f);
			if (this.operator.equals("||") && a.truthy())
				return Value.of(1f);
			if (this.operator.equals("??") && a.value != null)
				return a;
			Value b = this.right.evaluate(evaluation);
			return switch (this.operator)
			{
				case "+" -> Value.of(a.number() + b.number());
				case "-" -> Value.of(a.number() - b.number());
				case "*" -> Value.of(a.number() * b.number());
				case "/" -> Value.of(b.number() == 0f ? 0f : a.number() / b.number());
				case "<" -> bool(a.number() < b.number());
				case "<=" -> bool(a.number() <= b.number());
				case ">" -> bool(a.number() > b.number());
				case ">=" -> bool(a.number() >= b.number());
				case "==" -> bool(a.number() == b.number());
				case "!=" -> bool(a.number() != b.number());
				case "&&" -> bool(b.truthy());
				case "||" -> bool(b.truthy());
				case "??" -> b;
				default -> throw new IllegalStateException("Unknown binary operator " + this.operator);
			};
		}
	}

	/**
	 * Molang conditional expression with true and false branches.
	 *
	 * @param condition the graph transition predicate
	 * @param yes the yes
	 * @param no the no
	 */
	private record Conditional(Node condition, Node yes, Node no) implements Node
	{
		/**
		 * {@inheritDoc}
		 */
		@Override public Value evaluate(Evaluation evaluation) { return this.condition.evaluate(evaluation).truthy() ? this.yes.evaluate(evaluation) : this.no.evaluate(evaluation); }
	}

	/**
	 * Molang syntax node that updates a persistent or temporary variable.
	 *
	 * @param operator the operator
	 * @param target the target
	 * @param value the value written by this operation
	 */
	private record Assignment(String operator, Variable target, Node value) implements Node
	{
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Value evaluate(Evaluation evaluation)
		{
			Value next = this.value.evaluate(evaluation);
			if (!this.operator.equals("="))
			{
				float previous = this.target.evaluate(evaluation).number();
				next = switch (this.operator)
				{
					case "+=" -> Value.of(previous + next.number());
					case "-=" -> Value.of(previous - next.number());
					case "*=" -> Value.of(previous * next.number());
					case "/=" -> Value.of(next.number() == 0f ? 0f : previous / next.number());
					default -> throw new IllegalStateException("Unknown assignment operator " + this.operator);
				};
			}
			return evaluation.context.assign(this.target.name, next, evaluation);
		}
	}

	/**
	 * Molang statement block evaluated in source order.
	 *
	 * @param statements the statements
	 */
	private record Block(List<Node> statements) implements Node
	{
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Value evaluate(Evaluation evaluation)
		{
			Value value = Value.of(0f);
			for (Node statement : this.statements)
			{
				value = statement.evaluate(evaluation);
				if (evaluation.flow != Flow.NONE)
					break;
			}
			return value;
		}
	}

	/**
	 * Molang return statement that carries a value out of the current program.
	 *
	 * @param value the value written by this operation
	 */
	private record Return(Node value) implements Node
	{
		/**
		 * {@inheritDoc}
		 */
		@Override public Value evaluate(Evaluation evaluation) { evaluation.result = this.value.evaluate(evaluation); evaluation.flow = Flow.RETURN; return evaluation.result; }
	}

	/**
	 * Molang break or continue statement for loop control.
	 *
	 * @param flow the flow
	 */
	private record FlowNode(Flow flow) implements Node
	{
		/**
		 * {@inheritDoc}
		 */
		@Override public Value evaluate(Evaluation evaluation) { evaluation.flow = this.flow; return Value.of(0f); }
	}

	/**
	 * Molang loop statement with its count expression and repeated body.
	 *
	 * @param count the count
	 * @param body the body
	 */
	private record Loop(Node count, Node body) implements Node
	{
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Value evaluate(Evaluation evaluation)
		{
			int iterations = Math.clamp((int) this.count.evaluate(evaluation).number(), 0, MAX_LOOP_ITERATIONS);
			Value value = Value.of(0f);
			for (int index = 0; index < iterations; index++)
			{
				value = this.body.evaluate(evaluation);
				if (evaluation.flow == Flow.RETURN)
					break;
				if (evaluation.flow == Flow.BREAK)
				{
					evaluation.flow = Flow.NONE;
					break;
				}
				if (evaluation.flow == Flow.CONTINUE)
					evaluation.flow = Flow.NONE;
			}
			return value;
		}
	}

	/**
	 * Converts a boolean into the Molang numeric representation.
	 *
	 * @param value the value written by this operation
	 * @return a Molang value containing one for true or zero for false
	 */
	private static Value bool(boolean value)
	{
		return Value.of(value ? 1f : 0f);
	}

	/**
	 * Evaluates a supported Molang math function against its evaluated arguments.
	 *
	 * @param name the namespaced Molang value name
	 * @param arguments the evaluated query arguments
	 * @param randomProvider the random provider
	 * @return the evaluated function value
	 */
	private static Value math(String name, List<Value> arguments, RandomProvider randomProvider)
	{
		name = name.toLowerCase(Locale.ROOT);
		float a = argument(arguments, 0);
		float b = argument(arguments, 1);
		float c = argument(arguments, 2);
		if (name.startsWith("ease_"))
			return Value.of(ease(name, a, b, c));
		return switch (name)
		{
			case "abs" -> Value.of(Math.abs(a));
			case "acos" -> Value.of((float) Math.toDegrees(Math.acos(a)));
			case "asin" -> Value.of((float) Math.toDegrees(Math.asin(a)));
			case "atan" -> Value.of((float) Math.toDegrees(Math.atan(a)));
			case "atan2" -> Value.of((float) Math.toDegrees(Math.atan2(a, b)));
			case "ceil" -> Value.of((float) Math.ceil(a));
			case "clamp" -> Value.of(Math.clamp(a, b, c));
			case "copy_sign", "copysign" -> Value.of(Math.copySign(a, b));
			case "cos" -> Value.of((float) Math.cos(Math.toRadians(a)));
			case "die_roll" -> Value.of(dieRoll((int) a, b, c, false, randomProvider));
			case "die_roll_integer" -> Value.of(dieRoll((int) a, b, c, true, randomProvider));
			case "exp" -> Value.of((float) Math.exp(a));
			case "floor" -> Value.of((float) Math.floor(a));
			case "hermite_blend" -> Value.of(a * a * (3f - 2f * a));
			case "lerp" -> Value.of(a + (b - a) * c);
			case "lerprotate", "lerp_rotate" -> Value.of(a + minAngle(b - a) * c);
			case "ln" -> Value.of((float) Math.log(a));
			case "max" -> Value.of(max(arguments));
			case "min" -> Value.of(min(arguments));
			case "min_angle" -> Value.of(minAngle(a));
			case "mod" -> Value.of(b == 0f ? 0f : a % b);
			case "pow" -> Value.of((float) Math.pow(a, b));
			case "random" -> Value.of(randomProvider.next(0, a, b, false));
			case "random_integer" -> Value.of(randomProvider.next(0, a, b, true));
			case "round" -> Value.of(Math.round(a));
			case "sign" -> Value.of(Math.signum(a));
			case "sin" -> Value.of((float) Math.sin(Math.toRadians(a)));
			case "sqrt" -> Value.of((float) Math.sqrt(a));
			case "trunc" -> Value.of((float) (a < 0f ? Math.ceil(a) : Math.floor(a)));
			case "fract" -> Value.of(a - (float) Math.floor(a));
			default -> throw new IllegalArgumentException("Unknown Molang math function: math." + name);
		};
	}

	/**
	 * Reads a numeric function argument using the evaluator's missing-argument fallback.
	 *
	 * @param arguments the evaluated query arguments
	 * @param index the index
	 * @return the decoded numeric function argument using the evaluator's missing-argument fallback
	 */
	private static float argument(List<Value> arguments, int index)
	{
		return index < arguments.size() ? arguments.get(index).number() : 0f;
	}

	/**
	 * Returns the minimum numeric function argument.
	 *
	 * @param values the values
	 * @return the minimum numeric function argument
	 */
	private static float min(List<Value> values)
	{
		float result = Float.POSITIVE_INFINITY;
		for (Value value : values) result = Math.min(result, value.number());
		return result == Float.POSITIVE_INFINITY ? 0f : result;
	}

	/**
	 * Returns the maximum numeric function argument.
	 *
	 * @param values the values
	 * @return the maximum numeric function argument
	 */
	private static float max(List<Value> values)
	{
		float result = Float.NEGATIVE_INFINITY;
		for (Value value : values) result = Math.max(result, value.number());
		return result == Float.NEGATIVE_INFINITY ? 0f : result;
	}

	/**
	 * Sums the requested number of random samples within the supplied bounds.
	 *
	 * @param count the count
	 * @param low the lower random sampling bound
	 * @param high the upper random sampling bound
	 * @param integer whether sampling is restricted to integer values
	 * @param randomProvider the random provider
	 * @return the sum of the sampled rolls
	 */
	private static float dieRoll(int count, float low, float high, boolean integer, RandomProvider randomProvider)
	{
		float result = 0f;
		for (int index = 0; index < Math.clamp(count, 0, MAX_LOOP_ITERATIONS); index++)
			result += randomProvider.next(index, low, high, integer);
		return result;
	}

	/**
	 * Mixes seed bits for deterministic random-number generation.
	 *
	 * @param value the value written by this operation
	 * @return the mixed deterministic seed bits
	 */
	private static long mix(long value)
	{
		value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
		value = (value ^ value >>> 27) * 0x94D049BB133111EBL;
		return value ^ value >>> 31;
	}

	/**
	 * Supplies deterministic or context-owned random values to Molang math functions.
	 */
	@FunctionalInterface
	private interface RandomProvider
	{
		/**
		 * Samples a random value within the requested bounds, optionally restricted to integers.
		 *
		 * @param roll the deterministic sample index for the random operation
		 * @param low the lower random sampling bound
		 * @param high the upper random sampling bound
		 * @param integer whether sampling is restricted to integer values
		 * @return the sampled numeric value
		 */
		float next(int roll, float low, float high, boolean integer);
	}

	/**
	 * Wraps an angle to the minimal signed degree representation.
	 *
	 * @param angle the angle in degrees
	 * @return the wrapped signed angle in degrees
	 */
	private static float minAngle(float angle)
	{
		float result = angle % 360f;
		if (result >= 180f) result -= 360f;
		if (result < -180f) result += 360f;
		return result;
	}

	/**
	 * Interpolates the supplied endpoints using a named Molang easing function.
	 *
	 * @param name the namespaced Molang value name
	 * @param start the start
	 * @param end the end
	 * @param time the normalized easing progress
	 * @return the interpolated value selected by the easing function
	 */
	private static float ease(String name, float start, float end, float time)
	{
		String[] parts = name.split("_");
		if (parts.length != 3)
			throw new IllegalArgumentException("Unknown Molang math function: math." + name);
		float t = Math.clamp(time, 0f, 1f);
		boolean out = parts[1].equals("out");
		boolean inOut = parts[1].equals("inout");
		float eased = switch (parts[2])
		{
			case "quad" -> powerEase(t, 2, out, inOut);
			case "cubic" -> powerEase(t, 3, out, inOut);
			case "quart" -> powerEase(t, 4, out, inOut);
			case "quint" -> powerEase(t, 5, out, inOut);
			case "sine" -> sineEase(t, out, inOut);
			case "expo" -> expoEase(t, out, inOut);
			case "circ" -> circEase(t, out, inOut);
			case "back" -> backEase(t, out, inOut);
			case "bounce" -> bounceEase(t, out, inOut);
			case "elastic" -> elasticEase(t, out, inOut);
			default -> throw new IllegalArgumentException("Unknown Molang math easing: math." + name);
		};
		return start + (end - start) * eased;
	}

	/**
	 * Evaluates polynomial easing with the requested ease-in, ease-out or combined direction.
	 *
	 * @param t the t
	 * @param power the power
	 * @param out the out
	 * @param inOut the in out
	 * @return the eased interpolation weight
	 */
	private static float powerEase(float t, int power, boolean out, boolean inOut)
	{
		if (inOut) return t < .5f ? (float) Math.pow(2f * t, power) / 2f : 1f - (float) Math.pow(2f - 2f * t, power) / 2f;
		return out ? 1f - (float) Math.pow(1f - t, power) : (float) Math.pow(t, power);
	}

	/**
	 * Evaluates sinusoidal easing with the requested ease-in, ease-out or combined direction.
	 *
	 * @param t the t
	 * @param out the out
	 * @param inOut the in out
	 * @return the eased interpolation weight
	 */
	private static float sineEase(float t, boolean out, boolean inOut)
	{
		if (inOut) return (float) (-(Math.cos(Math.PI * t) - 1d) / 2d);
		return out ? (float) Math.sin(Math.PI * t / 2d) : (float) (1d - Math.cos(Math.PI * t / 2d));
	}

	/**
	 * Evaluates exponential easing with the requested ease-in, ease-out or combined direction.
	 *
	 * @param t the t
	 * @param out the out
	 * @param inOut the in out
	 * @return the eased interpolation weight
	 */
	private static float expoEase(float t, boolean out, boolean inOut)
	{
		if (inOut) return t == 0f ? 0f : t == 1f ? 1f : t < .5f ? (float) Math.pow(2d, 20d * t - 10d) / 2f : (float) (2d - Math.pow(2d, -20d * t + 10d)) / 2f;
		if (out) return t == 1f ? 1f : (float) (1d - Math.pow(2d, -10d * t));
		return t == 0f ? 0f : (float) Math.pow(2d, 10d * t - 10d);
	}

	/**
	 * Evaluates circular easing with the requested ease-in, ease-out or combined direction.
	 *
	 * @param t the t
	 * @param out the out
	 * @param inOut the in out
	 * @return the eased interpolation weight
	 */
	private static float circEase(float t, boolean out, boolean inOut)
	{
		if (inOut) return t < .5f ? (float) ((1d - Math.sqrt(1d - Math.pow(2d * t, 2d))) / 2d) : (float) ((Math.sqrt(1d - Math.pow(-2d * t + 2d, 2d)) + 1d) / 2d);
		return out ? (float) Math.sqrt(1d - Math.pow(t - 1d, 2d)) : (float) (1d - Math.sqrt(1d - t * t));
	}

	/**
	 * Evaluates overshooting back easing with the requested ease-in, ease-out or combined direction.
	 *
	 * @param t the t
	 * @param out the out
	 * @param inOut the in out
	 * @return the eased interpolation weight
	 */
	private static float backEase(float t, boolean out, boolean inOut)
	{
		float c1 = 1.70158f;
		if (inOut) { float c2 = c1 * 1.525f; return t < .5f ? (float) (Math.pow(2f * t, 2) * ((c2 + 1f) * 2f * t - c2)) / 2f : (float) (Math.pow(2f * t - 2f, 2) * ((c2 + 1f) * (t * 2f - 2f) + c2) + 2f) / 2f; }
		return out ? (float) (1f + (c1 + 1f) * Math.pow(t - 1f, 3) + c1 * Math.pow(t - 1f, 2)) : (c1 + 1f) * t * t * t - c1 * t * t;
	}

	/**
	 * Evaluates bounce easing with the requested ease-in, ease-out or combined direction.
	 *
	 * @param t the t
	 * @param out the out
	 * @param inOut the in out
	 * @return the eased interpolation weight
	 */
	private static float bounceEase(float t, boolean out, boolean inOut)
	{
		if (inOut) return t < .5f ? (1f - bounceEase(1f - 2f * t, true, false)) / 2f : (1f + bounceEase(2f * t - 1f, true, false)) / 2f;
		if (!out) return 1f - bounceEase(1f - t, true, false);
		float n = 7.5625f, d = 2.75f;
		if (t < 1f / d) return n * t * t;
		if (t < 2f / d) { t -= 1.5f / d; return n * t * t + .75f; }
		if (t < 2.5f / d) { t -= 2.25f / d; return n * t * t + .9375f; }
		t -= 2.625f / d; return n * t * t + .984375f;
	}

	/**
	 * Evaluates elastic easing with the requested ease-in, ease-out or combined direction.
	 *
	 * @param t the t
	 * @param out the out
	 * @param inOut the in out
	 * @return the eased interpolation weight
	 */
	private static float elasticEase(float t, boolean out, boolean inOut)
	{
		if (inOut) return t == 0f || t == 1f ? t : t < .5f ? (float) (-Math.pow(2d, 20d * t - 10d) * Math.sin((20d * t - 11.125d) * (2d * Math.PI / 4.5d))) / 2f : (float) (Math.pow(2d, -20d * t + 10d) * Math.sin((20d * t - 11.125d) * (2d * Math.PI / 4.5d))) / 2f + 1f;
		if (t == 0f || t == 1f) return t;
		return out ? (float) (Math.pow(2d, -10d * t) * Math.sin((10d * t - .75d) * (2d * Math.PI / 3d)) + 1d) : (float) (-Math.pow(2d, 10d * t - 10d) * Math.sin((10d * t - 10.75d) * (2d * Math.PI / 3d)));
	}

	/**
	 * Normalizes case and expands supported shorthand Molang namespaces.
	 *
	 * @param name the namespaced Molang value name
	 * @return the lowercase name with shorthand namespaces expanded
	 */
	private static String normalizeName(String name)
	{
		name = name.toLowerCase(Locale.ROOT);
		if (name.equals("q") ||
				name.startsWith("q."))
			return "query" + name.substring(1);
		if (name.equals("v") ||
				name.startsWith("v."))
			return "variable" + name.substring(1);
		if (name.equals("t") ||
				name.startsWith("t."))
			return "temp" + name.substring(1);
		if (name.equals("c") ||
				name.startsWith("c."))
			return "context" + name.substring(1);
		return name;
	}

	/**
	 * Removes the requested namespace prefix from a normalized Molang name.
	 *
	 * @param name the namespaced Molang value name
	 * @param namespace the namespace
	 * @return the name without the matching namespace prefix
	 */
	private static String stripNamespace(String name, String namespace)
	{
		return name.startsWith(namespace + ".") ?
				name.substring(namespace.length() + 1) :
				name;
	}

	/**
	 * Recursive-descent Molang parser with precedence handling and dependency classification.
	 */
	private static final class Parser
	{
		private final Lexer lexer;
		private Token current;

		/**
		 * Creates recursive-descent parsing state for the supplied Molang source.
		 *
		 * @param source the Molang source text
		 */
		private Parser(String source)
		{
			this.lexer = new Lexer(source);
			this.current = this.lexer.next();
		}

		/**
		 * Parses the complete source token stream into an executable Molang program.
		 *
		 * @return the parsed complete source token stream into an executable Molang program
		 */
		private Expression parse()
		{
			List<Node> statements = statements(TokenType.END);
			expect(TokenType.END, "end of expression");
			PExpressionDependency dependency = dependency(statements);
			Program program = new Program(statements, dependency, null);
			return dependency == PExpressionDependency.CONSTANT ?
					new Program(statements, dependency, program.evaluate(new Context())) :
					program;
		}

		/**
		 * Combines the dependencies required by the supplied syntax node or node sequence.
		 *
		 * @param nodes the nodes
		 * @return the combined context-dependency classification
		 */
		private static PExpressionDependency dependency(List<Node> nodes)
		{
			PExpressionDependency result = PExpressionDependency.CONSTANT;
			for (Node node : nodes)
				result = PExpressionDependency.combine(result, dependency(node));
			return result;
		}

		/**
		 * Combines the dependencies required by the supplied syntax node or node sequence.
		 *
		 * @param node the node
		 * @return the combined context-dependency classification
		 */
		private static PExpressionDependency dependency(Node node)
		{
			if (node instanceof Literal)
				return PExpressionDependency.CONSTANT;
			if (node instanceof Variable variable)
				return variableDependency(variable.name());
			if (node instanceof Call call)
			{
				PExpressionDependency result = dependency(call.arguments());
				String name = normalizeName(call.name());
				if (name.equals("math.random") || name.equals("math.random_integer") ||
						name.equals("math.die_roll") || name.equals("math.die_roll_integer"))
					return PExpressionDependency.STATEFUL;
				return PExpressionDependency.combine(result, variableDependency(name));
			}
			if (node instanceof Unary unary)
				return dependency(unary.value());
			if (node instanceof Binary binary)
				return PExpressionDependency.combine(dependency(binary.left()), dependency(binary.right()));
			if (node instanceof Conditional conditional)
				return PExpressionDependency.combine(dependency(conditional.condition()),
						PExpressionDependency.combine(dependency(conditional.yes()), dependency(conditional.no())));
			if (node instanceof Assignment)
				return PExpressionDependency.STATEFUL;
			if (node instanceof Block block)
				return dependency(block.statements());
			if (node instanceof Return returned)
				return dependency(returned.value());
			if (node instanceof FlowNode)
				return PExpressionDependency.STATEFUL;
			if (node instanceof Loop loop)
				return PExpressionDependency.combine(dependency(loop.count()), dependency(loop.body()));
			return PExpressionDependency.STATEFUL;
		}

		/**
		 * Classifies the frame or instance state required to read the named value.
		 *
		 * @param name the namespaced Molang value name
		 * @return the context state required by the named variable
		 */
		private static PExpressionDependency variableDependency(String name)
		{
			name = normalizeName(name);
			if (name.equals("math.random") || name.equals("math.random_integer") ||
					name.equals("math.die_roll") || name.equals("math.die_roll_integer"))
			{
				return PExpressionDependency.STATEFUL;
			}
			if (name.equals("true") || name.equals("false") || name.equals("math.pi") || name.startsWith("math."))
				return PExpressionDependency.CONSTANT;
			if (name.equals("query.anim_time"))
				return PExpressionDependency.TIME_ONLY;
			if (name.startsWith("variable.") || name.startsWith("temp."))
				return PExpressionDependency.STATEFUL;
			return PExpressionDependency.INSTANCE;
		}

		/**
		 * Parses statements until the requested closing token or end marker.
		 *
		 * @param terminator the terminator
		 * @return the parsed statements until the requested closing token or end marker
		 */
		private List<Node> statements(TokenType terminator)
		{
			List<Node> nodes = new ArrayList<>();
			while (this.current.type != terminator && this.current.type != TokenType.END)
			{
				nodes.add(statement());
				if (match(";")) continue;
				if (this.current.type != terminator && this.current.type != TokenType.END)
					error("Expected ';'");
			}
			return nodes;
		}

		/**
		 * Parses one Molang statement or flow-control construct.
		 *
		 * @return the parsed one Molang statement or flow-control construct
		 */
		private Node statement()
		{
			if (matchWord("return"))
				return new Return(expression());
			if (matchWord("break"))
				return new FlowNode(Flow.BREAK);
			if (matchWord("continue"))
				return new FlowNode(Flow.CONTINUE);
			return expression();
		}

		/**
		 * Parses a complete Molang expression using assignment precedence.
		 *
		 * @return the parsed complete Molang expression using assignment precedence
		 */
		private Node expression()
		{
			return assignment();
		}

		/**
		 * Returns the assignment.
		 *
		 * @return the assignment
		 */
		private Node assignment()
		{
			Node left = conditional();
			if (this.current.type == TokenType.OPERATOR &&
					List.of("=", "+=", "-=", "*=", "/=").contains(this.current.text))
			{
				String operator = this.current.text;
				next();
				if (!(left instanceof Variable))
					error("Assignment target must be a variable");
				return new Assignment(operator, (Variable) left, assignment());
			}
			return left;
		}

		/**
		 * Returns the graph transition predicate.
		 *
		 * @return the graph transition predicate
		 */
		private Node conditional()
		{
			Node condition = coalesce();
			if (!match("?"))
				return condition;
			Node yes = expression();
			Node no = match(":") ? conditional() : new Literal(0f);
			return new Conditional(condition, yes, no);
		}

		/**
		 * Parses null-coalescing expressions over logical operands.
		 *
		 * @return the parsed null-coalescing expressions over logical operands
		 */
		private Node coalesce()
		{
			Node left = logicalOr();
			return match("??") ?
					new Binary("??", left, coalesce()) :
					left;
		}

		/**
		 * Returns the logical or.
		 *
		 * @return the logical or
		 */
		private Node logicalOr()
		{
			Node node = logicalAnd();
			while (match("||"))
				node = new Binary("||", node, logicalAnd());
			return node;
		}

		/**
		 * Returns the logical and.
		 *
		 * @return the logical and
		 */
		private Node logicalAnd()
		{
			Node node = equality();
			while (match("&&"))
				node = new Binary("&&", node, equality());
			return node;
		}

		/**
		 * Returns the equality.
		 *
		 * @return the equality
		 */
		private Node equality()
		{
			Node node = comparison();
			while (this.current.text.equals("==") ||
					this.current.text.equals("!="))
			{
				String operator = this.current.text;
				next();
				node = new Binary(operator, node, comparison());
			}
			return node;
		}

		/**
		 * Returns the comparison.
		 *
		 * @return the comparison
		 */
		private Node comparison()
		{
			Node node = additive();
			while (List.of("<", "<=", ">", ">=").contains(this.current.text))
			{
				String operator = this.current.text;
				next();
				node = new Binary(operator, node, additive());
			}
			return node;
		}

		/**
		 * Returns the additive.
		 *
		 * @return the additive
		 */
		private Node additive()
		{
			Node node = multiplicative();
			while (this.current.text.equals("+")
					|| this.current.text.equals("-"))
			{
				String operator = this.current.text;
				next();
				node = new Binary(operator, node, multiplicative());
			}
			return node;
		}

		/**
		 * Returns the multiplicative.
		 *
		 * @return the multiplicative
		 */
		private Node multiplicative()
		{
			Node node = unary();
			while (this.current.text.equals("*")
					|| this.current.text.equals("/"))
			{
				String operator = this.current.text;
				next();
				node = new Binary(operator, node, unary());
			}
			return node;
		}

		/**
		 * Parses unary operators before primary expressions.
		 *
		 * @return the parsed unary operators before primary expressions
		 */
		private Node unary()
		{
			if (match("-") ||
					match("!"))
			{
				String operator = this.lexer.previous.text;
				return new Unary(operator, unary());
			}
			return primary();
		}

		/**
		 * Returns the primary.
		 *
		 * @return the primary
		 */
		private Node primary()
		{
			if (match("("))
			{
				Node node = expression();
				expect(" )".trim());
				return node;
			}
			if (match("{"))
			{
				List<Node> nodes = statements(TokenType.BRACE_CLOSE);
				expect(TokenType.BRACE_CLOSE, "'}'");
				return new Block(nodes);
			}
			if (this.current.type == TokenType.NUMBER)
			{
				float number = Float.parseFloat(this.current.text);
				next();
				return new Literal(number);
			}
			if (this.current.type == TokenType.IDENTIFIER)
			{
				String name = this.current.text;
				next();
				if (!match("("))
					return new Variable(name);
				List<Node> arguments = new ArrayList<>();
				if (!match(" )".trim()))
				{
					do
						arguments.add(expression());
					while (match(","));
					expect(" )".trim());
				}
				if (normalizeName(name).equals("loop"))
				{
					if (arguments.size() != 2) error("loop requires exactly two arguments");
					return new Loop(arguments.getFirst(), arguments.get(1));
				}
				return new Call(name, arguments);
			}
			error("Expected an expression");
			return new Literal(0f);
		}

		/**
		 * Consumes the next token when its text matches the supplied operator.
		 *
		 * @param text the text
		 * @return whether the requested token was found and consumed
		 */
		private boolean match(String text)
		{
			if (!this.current.text.equals(text)) return false;
			next();
			return true;
		}

		/**
		 * Consumes the next identifier when it matches the supplied keyword.
		 *
		 * @param word the word
		 * @return whether the requested keyword was found and consumed
		 */
		private boolean matchWord(String word)
		{
			if (this.current.type != TokenType.IDENTIFIER ||
					!this.current.text.equalsIgnoreCase(word))
				return false;
			next();
			return true;
		}

		/**
		 * Consumes a required token or throws a source-positioned parse error.
		 *
		 * @param text the text
		 */
		private void expect(String text)
		{
			if (!match(text)) error("Expected '" + text + "'");
		}

		/**
		 * Consumes a required token or throws a source-positioned parse error.
		 *
		 * @param type the type
		 * @param expected the expected
		 */
		private void expect(TokenType type, String expected)
		{
			if (this.current.type != type) error("Expected " + expected);
			next();
		}

		/**
		 * Advances to the next token from the lexer.
		 */
		private void next()
		{
			this.lexer.previous = this.current;
			this.current = this.lexer.next();
		}

		/**
		 * Creates a parse exception containing the current source location.
		 *
		 * @param message the message
		 */
		private void error(String message)
		{
			throw new IllegalArgumentException(message + " at position " + this.current.position);
		}
	}

	/**
	 * Kinds of tokens produced by the Molang lexer.
	 */
	private enum TokenType
	{
		/**
		 * Numeric literal token.
		 */
		NUMBER,
		/**
		 * Namespaced variable or function identifier token.
		 */
		IDENTIFIER,
		/**
		 * Arithmetic, logical or punctuation operator token.
		 */
		OPERATOR,
		/**
		 * Closing brace terminating a statement block.
		 */
		BRACE_CLOSE,
		/**
		 * End-of-source marker.
		 */
		END
	}
	/**
	 * Molang token kind, source text and numeric literal value.
	 *
	 * @param type the type
	 * @param text the text
	 * @param position the position
	 */
	private record Token(TokenType type, String text, int position) { }

	/**
	 * Tokenizes Molang source text into identifiers, numeric literals and operators.
	 */
	private static final class Lexer
	{
		private final String source;
		private int position;
		private Token previous;

		/**
		 * Creates a token cursor over the supplied Molang source text.
		 *
		 * @param source the Molang source text
		 */
		private Lexer(String source) { this.source = source; }

		/**
		 * Reads the next numeric, identifier, operator or end token from the source.
		 *
		 * @return the decoded next numeric, identifier, operator or end token from the source
		 */
		private Token next()
		{
			while (this.position < this.source.length() &&
					Character.isWhitespace(this.source.charAt(this.position)))
				this.position++;
			if (this.position >= this.source.length())
				return new Token(TokenType.END, "", this.position);
			int start = this.position;
			char character = this.source.charAt(this.position);
			if (Character.isDigit(character) ||
					character == '.' &&
					this.position + 1 < this.source.length() &&
					Character.isDigit(this.source.charAt(this.position + 1)))
			{
				this.position++;
				while (this.position < this.source.length() &&
						(Character.isDigit(this.source.charAt(this.position)) ||
						this.source.charAt(this.position) == '.'))
					this.position++;
				if (this.position < this.source.length() &&
						(this.source.charAt(this.position) == 'e' ||
						this.source.charAt(this.position) == 'E'))
				{
					this.position++;
					if (this.position < this.source.length() &&
							(this.source.charAt(this.position) == '+' ||
							this.source.charAt(this.position) == '-'))
						this.position++;
					while (this.position < this.source.length() &&
							Character.isDigit(this.source.charAt(this.position)))
						this.position++;
				}
				return new Token(TokenType.NUMBER, this.source.substring(start, this.position), start);
			}
			if (Character.isLetter(character) ||
					character == '_')
			{
				this.position++;
				while (this.position < this.source.length())
				{
					char next = this.source.charAt(this.position);
					if (!(Character.isLetterOrDigit(next) || next == '_' || next == '.')) break;
					this.position++;
				}
				return new Token(TokenType.IDENTIFIER, this.source.substring(start, this.position), start);
			}
			for (String operator : List.of("??", "&&", "||", "==", "!=", "<=", ">=", "+=", "-=", "*=", "/="))
				if (this.source.startsWith(operator, this.position)) { this.position += operator.length(); return new Token(TokenType.OPERATOR, operator, start); }
			this.position++;
			return switch (character)
			{
				case '}' -> new Token(TokenType.BRACE_CLOSE, "}", start);
				case '+', '-', '*', '/', '<', '>', '!', '=', '?', ':', '(', ')', '{', ';', ',' -> new Token(TokenType.OPERATOR, String.valueOf(character), start);
				default -> throw new IllegalArgumentException("Unexpected Molang character '" + character + "' at position " + start);
			};
		}
	}
}
