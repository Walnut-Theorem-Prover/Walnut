package Main;

import java.util.ArrayList;
import java.util.List;

import Main.EvalComputations.Token.Token;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

public class PredicateTest {
	private static final String PRED_NAME = "my_macro";
	private static final String PRED_CMD_NAME = "#" + PRED_NAME;
	@Test
	void basicTest() {
		Predicate p = new Predicate("blah");
		Assertions.assertEquals(1, p.getPostOrder().size());
		Assertions.assertEquals("blah", p.toString());

		p = new Predicate("?msd_3 (a=1 )");
		Assertions.assertEquals(3, p.getPostOrder().size());
		Assertions.assertEquals("a:1:=_msd_3", p.toString());
	}

	static class PredTest {
		public PredTest(int i, String macro, String pred, String expectedPredicate, String expected){
			this.macro = macro;
			this.pred = pred;
			this.expected_predicate = expectedPredicate;
			this.expected = expected;
			this.i = i;
		}
		public final String macro,pred,expected,expected_predicate;
		public final int i;
	}

	@TestFactory
	List<DynamicTest> runPredicateTests()  {
		Session.setPathsAndNamesIntegrationTests();
		Session.cleanPathsAndNamesIntegrationTest();
		List<PredTest> tests = new ArrayList<>();
		tests.add(new PredTest(
				0,
				"%0",
				PRED_CMD_NAME + "0(a)=1",
				"a=1",
				"a 1 =_msd_2"));
		tests.add(new PredTest(
				1,
				"%0",
				PRED_CMD_NAME + "1(a)=1",
				"a =1",
				"a 1 =_msd_2"));
		tests.add(new PredTest(
				2,
				"%0",
				PRED_CMD_NAME + "2(a)=1",
				" a =1",
				"a 1 =_msd_2"));
		tests.add(new PredTest(
				3,
				"%0=1",
				"?msd_3 (" + PRED_CMD_NAME + "3(a))",
				"?msd_3 (a=1)",
				"a 1 =_msd_3"));
		tests.add(new PredTest(
				4,
				"%0=1",
				"?msd_3 (" + PRED_CMD_NAME + "4(a))",
				"?msd_3 ( a=1)",
				"a 1 =_msd_3"));
		tests.add(new PredTest(
				5,
				"%0=1",
				"?msd_3 (" + PRED_CMD_NAME + "5(a) )",
				"?msd_3 (a=1 )",
				"a 1 =_msd_3"));
		tests.add(new PredTest(
				6,
				"%0=1",
				"?msd_3 (" + PRED_CMD_NAME +"6(a) => " + PRED_CMD_NAME + "6(b))",
				"?msd_3 (a=1 => b=1)",
				"a 1 =_msd_3 b 1 =_msd_3 =>"));
		tests.add(new PredTest(
				7,
				"a+b=2",
				PRED_CMD_NAME + "7()",
				"a+b=2",
				"a b +_msd_2 2 =_msd_2"));
		tests.add(new PredTest(
				8,
				"a+b=2",
				PRED_CMD_NAME +"8() & " + PRED_CMD_NAME + "8()",
				"a+b=2 & a+b=2",
				"a b +_msd_2 2 =_msd_2 a b +_msd_2 2 =_msd_2 &"));
		tests.add(new PredTest(
					9,
					"%0 E%1 %2 = %1 + 1 & %1 = 5",
					PRED_CMD_NAME + "9(?msd_2,a,b)",
					"?msd_2 Ea b = a + 1 & a = 5",
					"a b a 1 +_msd_2 =_msd_2 a 5 =_msd_2 & E"));
		tests.add(new PredTest(
				10,
				"%0 E%1 %2 = %1 + 1 & %1 = 5",
				PRED_CMD_NAME + "10(?msd_3,a,b)",
				"?msd_3 Ea b = a + 1 & a = 5",
				"a b a 1 +_msd_3 =_msd_3 a 5 =_msd_3 & E"));
		tests.add(new PredTest(
				11,
				"%0 E%1 %2 = %1 + 1 &",
				PRED_CMD_NAME + "11(?msd_2,a,b) a = 5",
				"?msd_2 Ea b = a + 1 & a = 5",
				"a b a 1 +_msd_2 =_msd_2 a 5 =_msd_2 & E"));
		tests.add(new PredTest(
				12,
				"E%0 %1 = %0 + 1 &",
				"?msd_fib " + PRED_CMD_NAME + "12(a,b) a = 5",
				"?msd_fib Ea b = a + 1 & a = 5",
				"a b a 1 +_msd_fib =_msd_fib a 5 =_msd_fib & E"));
		tests.add(new PredTest(
				13,
				"E%0 %1 = %0 + 1 &",
				"?msd_fib (" + PRED_CMD_NAME + "13(a,b) a = 5) =>(?lsd_3   " +
						PRED_CMD_NAME + "13(f,g) f = 6)",
				"?msd_fib (Ea b = a + 1 & a = 5) =>(?lsd_3   Ef g = f + 1 & f = 6)",
				"a b a 1 +_msd_fib =_msd_fib a 5 =_msd_fib & E f g f 1 +_lsd_3 =_lsd_3 f 6 =_lsd_3 & E =>"));
		List<DynamicTest> dynamicTests = new ArrayList<>();
		for(int i=0;i!=tests.size();++i){
			PredTest t = tests.get(i);
			dynamicTests.add(DynamicTest.dynamicTest("Predicate test " + i, () -> test(t)));
		}
		return dynamicTests;
	}

	private static String postToString(List<Token> p){
		StringBuilder s = new StringBuilder();
		for(Token t:p){
			s.append(t.toString()).append(" ");
		}
		return s.toString();
	}
	private static void test(PredTest t) {
		Predicate p = null;
		try {
			p = new Predicate(t.pred);
		} catch (RuntimeException e) {
			Assertions.fail(e);
		}
		String s = postToString(p.postOrder);
		Assertions.assertEquals(t.expected_predicate.trim().replace(" ","")
				, p.predicate.trim().replace(" ",""));
		Assertions.assertEquals(t.expected.trim().replace(" ",""),
				s.trim().replace(" ",""));
	}
}
