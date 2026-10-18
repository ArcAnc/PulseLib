/**
 * @author ArcAnc
 * Created at: 18.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.data.gecko.PExpressionDependency;
import com.arcanc.pulselib.data.gecko.PExpressionEvaluator;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PExpressionEvaluationTest
{
	@Test
	void timeExpressionsUseTheActualContextInEitherCallOrder()
	{
		var evaluator = new PExpressionEvaluator();
		var expression = MolangParser.parse("q.anim_time * 2");
		var first = new MolangParser.Context().query("anim_time", 0.5f);
		var second = new MolangParser.Context().query("anim_time", 1.5f);
		assertEquals(1, evaluator.evaluate(expression, first, 10));
		assertEquals(3, evaluator.evaluate(expression, second, 10));
		assertEquals(3, evaluator.evaluate(expression, second, 30));
		assertEquals(1, evaluator.evaluate(expression, first, 30));
		first.query("anim_time", 2.5f);
		assertEquals(5, evaluator.evaluate(expression, first, 30));
		evaluator.clearTimeCache();
		assertEquals(5, evaluator.evaluate(expression, first, 30));
	}

	@Test
	void subTickTimeIsNeverQuantized()
	{
		var evaluator = new PExpressionEvaluator();
		var expression = MolangParser.parse("q.anim_time");
		var context = new MolangParser.Context();
		for (int frame = 0; frame <= 100; frame++)
		{
			float seconds = 0.5f + frame * 0.0005f;
			context.query("anim_time", seconds);
			assertEquals(seconds, evaluator.evaluate(expression, context, seconds), 0.0000001f);
		}
	}

	@Test
	void customTimeResolversAreInvokedOnEveryEvaluation()
	{
		AtomicInteger calls = new AtomicInteger();
		var context = new MolangParser.Context().queryResolver((name, arguments, current) -> (float)calls.incrementAndGet());
		var evaluator = new PExpressionEvaluator();
		var expression = MolangParser.parse("q.anim_time");
		assertEquals(1, evaluator.evaluate(expression, context, 0));
		assertEquals(2, evaluator.evaluate(expression, context, 0));
	}

	@Test
	void statefulAndInstanceExpressionsKeepTheirContextAndEvaluationOrder()
	{
		var evaluator = new PExpressionEvaluator();
		var expression = MolangParser.parse("v.count = (v.count ?? 0) + 1; return v.count;");
		var first = new MolangParser.Context().randomSeed(1234);
		var second = new MolangParser.Context();
		assertEquals(1, evaluator.evaluate(expression, first, 0));
		assertEquals(2, evaluator.evaluate(expression, first, 0));
		assertEquals(1, evaluator.evaluate(expression, second, 0));
		var instance = MolangParser.parse("this + context.offset + query.offset");
		first.thisValue(1).context("offset", 2).query("offset", 3);
		second.thisValue(4).context("offset", 5).query("offset", 6);
		assertEquals(6, evaluator.evaluate(instance, first, 0));
		assertEquals(15, evaluator.evaluate(instance, second, 0));
		var random = MolangParser.parse("math.random(0, 1)");
		assertNotEquals(evaluator.evaluate(random, first, 0), evaluator.evaluate(random, first, 0));
	}

	@Test
	void dependenciesDoNotFoldContextOrRandomValuesIntoConstants()
	{
		assertEquals(PExpressionDependency.CONSTANT, MolangParser.parse("math.sin(30) + 2").dependency());
		assertEquals(2.5f, MolangParser.evaluate("math.sin(30) + 2", new MolangParser.Context()), 0.00001f);
		assertEquals(PExpressionDependency.TIME_ONLY, MolangParser.parse("q.anim_time").dependency());
		assertEquals(PExpressionDependency.INSTANCE, MolangParser.parse("this + c.value + q.health").dependency());
		assertEquals(PExpressionDependency.STATEFUL, MolangParser.parse("v.value").dependency());
		for (String name : new String[]{"math.random", "math.random_integer", "math.die_roll", "math.die_roll_integer"})
		{
			assertEquals(PExpressionDependency.STATEFUL, MolangParser.parse(name + "(0, 1, 2)").dependency());
			assertEquals(PExpressionDependency.STATEFUL, MolangParser.parse(name).dependency());
		}
	}

	@Test
	void aThousandIndependentContextsAreReevaluatedWithoutRetainedValues()
	{
		var evaluator = new PExpressionEvaluator();
		AtomicInteger calls = new AtomicInteger();
		MolangParser.Expression expression = new MolangParser.Expression()
		{
			@Override
			public PExpressionDependency dependency()
			{
				return PExpressionDependency.TIME_ONLY;
			}

			@Override
			public float evaluate(MolangParser.Context context)
			{
				calls.incrementAndGet();
				return MolangParser.evaluate("q.anim_time", context);
			}
		};
		for (int controller = 0; controller < 1000; controller++)
		{
			var instance = new PAnimationController<PTransitionTestSupport.Dummy>(state ->
					ControllerState.STOP);
			var context = instance.persistentMolangContext().query("anim_time", controller);
			assertEquals(controller, evaluator.evaluate(expression, context, 0));
			context.query("anim_time", controller + 0.25f);
			assertEquals(controller + 0.25f, evaluator.evaluate(expression, context, 0));
		}
		assertEquals(2000, calls.get());
	}

	@Test
	void measureDirectAndEvaluatorCostWithoutATimingThreshold()
	{
		var expression = MolangParser.parse("math.sin(query.anim_time * 90) * 4 + query.anim_time");
		var context = new MolangParser.Context().query("anim_time", 0.25f);
		var evaluator = new PExpressionEvaluator();
		for (int warmup = 0; warmup < 50_000; warmup++)
		{
			expression.evaluate(context);
			evaluator.evaluate(expression, context, 0.25f);
		}
		int samples = 200_000;
		long start = System.nanoTime();
		double direct = 0;
		for (int sample = 0; sample < samples; sample++)
		{
			direct += expression.evaluate(context);
		}
		long directNanos = System.nanoTime() - start;
		start = System.nanoTime();
		double throughEvaluator = 0;
		for (int sample = 0; sample < samples; sample++)
		{
			throughEvaluator += evaluator.evaluate(expression, context, 0.25f);
		}
		long evaluatorNanos = System.nanoTime() - start;
		assertEquals(direct, throughEvaluator);
		System.out.printf("Molang %,d evaluations: direct %.1f ns/op, evaluator %.1f ns/op%n",
				samples, (double)directNanos / samples, (double)evaluatorNanos / samples);
	}
}
