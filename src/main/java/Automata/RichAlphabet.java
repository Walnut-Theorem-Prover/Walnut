package Automata;

import java.util.ArrayList;
import java.util.List;

public class RichAlphabet {
  /* Inputs to an ordinary automaton are n-tuples. Each coordinate has its own alphabet.
   * Let's see this by means of an example:
   * Suppose our automaton has 3-tuples as its input:
   * The field A stores the alphabets of these inputs.
   * For example, we might have A = [[1,2],[0,-1,1],[1,3]]. This means that the first input is over alphabet
   * {1,2} and the second one is over {0,-1,1},...
   * Note: Input alphabets are subsets of integers.
   * -So in total there are 12 = 2*3*2 different inputs (this number is stored in alphabetSize)
   * for this automaton. Here are two example inputs: (2,-1,3),(1,0,3).
   * We can encode these 3-tuples by the following rule:
   * 0 = (1,0,1)
   * 1 = (2,0,1)
   * 2 = (1,-1,1)
   * ...
   * 11 = (2,1,3)
   */
  private List<List<Integer>> A;
  private List<Integer> encoder;

  public RichAlphabet() {
    this.A = new ArrayList<>();
  }

  static int mapToReducedEncodedInput(int n, List<Integer> I, List<Integer> newEncoder,
                                    List<List<Integer>> oldAlphabet,
                                    List<List<Integer>> newAlphabet) {
      if (I.size() <= 1) return n;
      List<Integer> x = decode(oldAlphabet, n);
      for (int i = 1; i < I.size(); i++)
          if (x.get(I.get(i)) != x.get(I.get(0)))
              return -1;
      List<Integer> y = new ArrayList<>();
      for (int i = 0; i < x.size(); i++)
          if (!I.contains(i) || I.indexOf(i) == 0)
              y.add(x.get(i));
      return encode(y, newAlphabet, newEncoder);
  }

  public static int encode(List<Integer> l, List<List<Integer>> A, List<Integer> encoder) {
    int encoding = 0;
    for (int i = 0; i < l.size(); i++) {
      encoding += encoder.get(i) * A.get(i).indexOf(l.get(i));
    }
    return encoding;
  }

  int determineAlphabetSize() {
    int alphabetSize = 1;
    for (List<Integer> x : A) {
      alphabetSize *= x.size();
    }
    return alphabetSize;
  }

  public void setupEncoder() {
    setEncoder(new ArrayList<>());
    getEncoder().add(1);
    for (int i = 0; i < getA().size() - 1; i++) {
      getEncoder().add(getEncoder().get(i) * getA().get(i).size());
    }
  }

  /**
   * Input to dk.brics.automaton.Automata is a char. Input to Automata.Automaton is List<Integer>.
   * Thus, this method transforms a List<Integer> to its corresponding integer.
   * The other application of this function is when we use the transition function d in State. Note that the transtion function
   * maps an integer (encoding of List<Integer>) to a set of states.
   * <p>
   * Example: A = [[0,1],[-1,2,3]] and if
   * l = [0,-1] then we return 0
   * l = [1,-1] then we return 1
   * l = [0,2] then we return 2
   * l = [1,2] then we return 3
   * l = [0,3] then we return 4
   * l = [1,3] then we return 5
   * Second Example: A = [[-2,-1,-3],[0,1],[-1,0,3],[7,8]] and if
   * l = [-2,0,-1,7] then we return 0
   * l = [-1,0,-1,7] then we return 1
   * l = [-3,0,-1,7] then we return 2
   * l = [-2,1,-1,7] then we return 3
   * l = [-1,1,-1,7] then we return 4
   * l = [-3,1,-1,7] then we return 5
   * l = [-2,0,0,7] then we return 6
   * ...
   */
  public int encode(List<Integer> l) {
    if (getEncoder() == null) {
      setupEncoder();
    }
    int encoding = 0;
    for (int i = 0; i < l.size(); i++) {
      encoding += encoder.get(i) * A.get(i).indexOf(l.get(i));
    }
    return encoding;
  }

  public List<Integer> decode(int n) {
    return decode(A, n);
  }

  /**
   * Input to dk.brics.automaton.Automata is a char. Input to Automaton is List<Integer>.
   * Thus, this method transforms an integer to its corresponding List<Integer>
   * Example: A = [[0,1],[-1,2,3]] and if
   * n = 0 then we return [0,-1]
   * n = 1 then we return [1,-1]
   * n = 2 then we return [0,2]
   * n = 3 then we return [1,2]
   * n = 4 then we return [0,3]
   * n = 5 then we return [1,3]
   */
  public static List<Integer> decode(List<List<Integer>> A, int n) {
      List<Integer> l = new ArrayList<>(A.size());
      for (List<Integer> integers : A) {
          l.add(integers.get(n % integers.size()));
          n = n / integers.size();
      }
      return l;
  }

  /**
   * This is used in the encode method.
   * When A = [l1,l2,l3,...,ln] then
   * encoder = [1,|l1|,|l1|*|l2|,...,|l1|*|l2|*...*|ln-1|].
   * It is useful, as mentioned earlier, in the encode method. encode method gets a list x, which represents a viable
   * input to this automaton, and returns a non-negative integer, which is the integer represented by x, in base encoder.
   * Note that encoder is a mixed-radix base. We use the encoded integer, returned by encode(), to store transitions.
   * So we don't store the list x.
   * We can decode, the number returned by encode(), and get x back using decode method.
   */
  public List<Integer> getEncoder() {
    return encoder;
  }

  public void setEncoder(List<Integer> encoder) {
    this.encoder = encoder;
  }

  public List<List<Integer>> getA() {
    return A;
  }

  public void setA(List<List<Integer>> a) {
    A = a;
  }

  /**
   * A wildcard is denoted by null in L. What do we mean by expanding wildcard?
   * Here is an example: suppose that A = [[1,2],[0,-1],[3,4,5]] and L = [1,*,4]. Then the method would return
   * [[1,0,4],[1,-1,4]]. In other words, it'll replace * in the second position with 0 and -1.
   */
  public List<List<Integer>> expandWildcard(List<Integer> L) {
    List<List<Integer>> R = new ArrayList<>();
    R.add(new ArrayList<>(L));
    for (int i = 0; i < L.size(); i++) {
      if (L.get(i) == null) {
        List<List<Integer>> tmp = new ArrayList<>();
        for (int x : A.get(i)) {
          for (List<Integer> tmp2 : R) {
            tmp.add(new ArrayList<>(tmp2));
            tmp.get(tmp.size() - 1).set(i, x);
          }
        }
        R = new ArrayList<>(tmp);
      }
    }
    return R;
  }

  public List<Integer> getPermutedEncoder(List<List<Integer>> permuted_A) {
    List<Integer> permuted_encoder = new ArrayList<>();
    permuted_encoder.add(1);
    for (int i = 0; i < A.size() - 1; i++) {
      permuted_encoder.add(permuted_encoder.get(i) * permuted_A.get(i).size());
    }
    return permuted_encoder;
  }

  public RichAlphabet clone() {
    RichAlphabet r = new RichAlphabet();
    for (int i = 0; i < getA().size(); i++) {
      r.A.add(new ArrayList<>(getA().get(i)));
      if (getEncoder() != null && !getEncoder().isEmpty()) {
        if (r.getEncoder() == null) r.setEncoder(new ArrayList<>());
        r.getEncoder().add(getEncoder().get(i));
      }
    }
    return r;
  }

  public void clear() {
    A = null;
    encoder = null;
  }

  public String toString() {
    return "\nA:" + A + "\nencoder:" + this.encoder;
  }
}
