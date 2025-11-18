package Automata;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class RichAlphabet {
  public static final int MISSING_REDUCED_DIMENSION_ELT = -1;
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
  private IntList encoder;

  public RichAlphabet() {
    this.A = new ArrayList<>();
  }

  /**
   * Is r1.getA() a subset of r2.getA() ?
   */
  static boolean isSubsetA(RichAlphabet r1, RichAlphabet r2) {
      if (r1.getA().size() != r2.getA().size()) {
          return false;
      }
      for (int i = 0; i < r1.getA().size(); i++) {
          if (!new HashSet<>(r2.getA().get(i)).containsAll(r1.getA().get(i))) {
              return false;
          }
      }
      return true;
  }

  public boolean isInNewAlphabet(List<Integer> decoded) {
    for (int i = 0; i < decoded.size(); i++) {
      if (!this.A.get(i).contains(decoded.get(i))) {
        return false;
      }
    }
    return true;
  }

  int determineAlphabetSize() {
    int alphabetSize = 1;
    for (List<Integer> x : A) {
      alphabetSize *= x.size();
    }
    return alphabetSize;
  }

  /**
   * Input to dk.brics.automaton.Automata is a char. Input to Automata.Automaton is List<Integer>.
   * Thus, this method transforms a List<Integer> to its corresponding integer.
   * The other application of this function is when we use the transition function d in State. Note that the transition function
   * maps an integer (encoding of List<Integer>) to a set of states.
   * See unit tests for examples.
   */
  public int encode(List<Integer> l) {
    if (encoder == null) {
      setupEncoder();
    }
    return encode(l, A, encoder);
  }

  public void setupEncoder() {
    encoder = determineEncoder(A);
  }

  public static IntList determineEncoder(List<List<Integer>> A) {
    final IntList encoder = new IntArrayList(A.size());
    int encoderVal = 1;
    for (List<Integer> integers : A) {
      encoder.add(encoderVal);
      encoderVal = Math.multiplyExact(encoderVal, integers.size()); // throws on overflow
    }
    return encoder;
  }

  public static int encode(List<Integer> l, List<List<Integer>> A, IntList encoder) {
    int encoding = 0;
    for (int i = 0; i < l.size(); i++) {
      encoding += encoder.getInt(i) * A.get(i).indexOf(l.get(i));
    }
    return encoding;
  }

  public List<Integer> decode(int n) {
    return decode(A, n);
  }

  /**
   * Input to dk.brics.automaton.Automata is a char. Input to Automaton is List<Integer>.
   * Thus, this method transforms an integer to its corresponding List<Integer>
   * See unit tests for examples.
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
   * It is useful, in the encode method. encode method gets a list x, which represents a viable
   * input to this automaton, and returns a non-negative integer, which is the integer represented by x, in base encoder.
   * Note that encoder is a mixed-radix base. We use the encoded integer, returned by encode(), to store transitions.
   * So we don't store the list x.
   * We can decode, the number returned by encode(), and get x back using decode method.
   */
  public IntList getEncoder() {
    return encoder;
  }

  public void setEncoder(IntList encoder) {
    this.encoder = encoder;
  }

  public List<List<Integer>> getA() {
    return A;
  }

  public void setA(List<List<Integer>> a) {
    A = a;
  }

  /**
   * A wildcard is denoted by null in L.
   * See unit tests for examples.
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

  List<Integer> determineReducedDimensionMap(int alphabetSize, List<Integer> I) {
    List<List<Integer>> newA = new ArrayList<>();
    for (int i = 0; i < A.size(); i++)
      if (!I.contains(i) || I.indexOf(i) == 0)
        newA.add(new ArrayList<>(A.get(i)));

    IntList newEncoder = determineEncoder(newA);

    List<Integer> map = new ArrayList<>(alphabetSize);
    for (int n = 0; n < alphabetSize; n++) {
      int newElt = 0;
      if (I.size() <= 1) {
        newElt = n;
      } else {
        List<Integer> x = decode(A, n);
        Integer i0 = x.get(I.get(0));
        for (int i = 1; i < I.size(); i++) {
          if (!(x.get(I.get(i)).equals(i0))) {
            newElt = MISSING_REDUCED_DIMENSION_ELT;
            break;
          }
        }
        if (newElt != MISSING_REDUCED_DIMENSION_ELT) {
          List<Integer> y = new ArrayList<>();
          for (int i = 0; i < x.size(); i++)
            if (!I.contains(i) || I.indexOf(i) == 0)
              y.add(x.get(i));
          newElt = encode(y, newA, newEncoder);
        }
      }
      map.add(newElt);
    }

    this.A = newA;
    this.encoder = null;
    return map;
  }

  public RichAlphabet clone() {
    RichAlphabet r = new RichAlphabet();
    r.A.addAll(A);
    if (encoder != null && !encoder.isEmpty()) {
      r.encoder = new IntArrayList(encoder);
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
