package Automata.FA;

import Automata.RichAlphabet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class RichAutomataTest {
  /*
   * A = [[0,1],[-1,2,3]] and if
   * l = [0,-1] then we return 0
   * l = [1,-1] then we return 1
   * l = [0,2] then we return 2
   * l = [1,2] then we return 3
   * l = [0,3] then we return 4
   * l = [1,3] then we return 5
   *
   * Decode is just the inversion of this.
   */
  @Test
  void testEncodeDecode() {
    RichAlphabet r = new RichAlphabet();

    r.setA(List.of(List.of(0,1), List.of(-1,2,3)));


    Assertions.assertEquals(0, r.encode(List.of(0,-1)));
    Assertions.assertEquals(1, r.encode(List.of(1,-1)));
    Assertions.assertEquals(2, r.encode(List.of(0,2)));
    Assertions.assertEquals(3, r.encode(List.of(1,2)));
    Assertions.assertEquals(4, r.encode(List.of(0,3)));
    Assertions.assertEquals(5, r.encode(List.of(1,3)));

    Assertions.assertEquals(List.of(0,-1), r.decode(0));
    Assertions.assertEquals(List.of(1,-1), r.decode(1));
    Assertions.assertEquals(List.of(0,2), r.decode(2));
    Assertions.assertEquals(List.of(1,2), r.decode(3));
    Assertions.assertEquals(List.of(0,3), r.decode(4));
    Assertions.assertEquals(List.of(1,3), r.decode(5));


    //r.setA(List.of(List.of(1,2), List.of(0,-1,1), List.of(1,3)));

  }

  /*
   *  A = [[-2,-1,-3],[0,1],[-1,0,3],[7,8]] and if
   * l = [-2,0,-1,7] then we return 0
   * l = [-1,0,-1,7] then we return 1
   * l = [-3,0,-1,7] then we return 2
   * l = [-2,1,-1,7] then we return 3
   * l = [-1,1,-1,7] then we return 4
   * l = [-3,1,-1,7] then we return 5
   * l = [-2,0,0,7] then we return 6
   * etc., up to 3 * 2 * 3 * 2 = 36 total values
   *
   * Decode is just the inversion of this.
   */
  @Test
  void testEncodeDecode2() {

    RichAlphabet r = new RichAlphabet();
    r.setA(List.of(List.of(-2,-1,-3), List.of(0,1), List.of(-1,0,3), List.of(7,8)));

    Assertions.assertEquals(0, r.encode(List.of(-2,0,-1,7)));
    Assertions.assertEquals(1, r.encode(List.of(-1,0,-1,7)));
    Assertions.assertEquals(2, r.encode(List.of(-3,0,-1,7)));
    Assertions.assertEquals(3, r.encode(List.of(-2,1,-1,7)));
    Assertions.assertEquals(4, r.encode(List.of(-1,1,-1,7)));
    Assertions.assertEquals(5, r.encode(List.of(-3,1,-1,7)));
    Assertions.assertEquals(6, r.encode(List.of(-2,0,0,7)));

    Assertions.assertEquals(List.of(-2,0,-1,7), r.decode(0));
    Assertions.assertEquals(List.of(-1,0,-1,7), r.decode(1));
    Assertions.assertEquals(List.of(-3,0,-1,7), r.decode(2));
    Assertions.assertEquals(List.of(-2,1,-1,7), r.decode(3));
    Assertions.assertEquals(List.of(-1,1,-1,7), r.decode(4));
    Assertions.assertEquals(List.of(-3,1,-1,7), r.decode(5));
    Assertions.assertEquals(List.of(-2,0,0,7), r.decode(6));
  }


  @Test
  void testExpandWildcard() {
    // A = [[1,2],[0,-1],[3,4,5]] and L = [1,null,4]. Then the method would return
    // [[1,0,4],[1,-1,4]]. In other words, it replaces null in the second position with 0 and -1.
    RichAlphabet r = new RichAlphabet();
    r.setA(List.of(List.of(1,2), List.of(0,-1), List.of(3,4,5)));
    List<Integer> l = new ArrayList<>();
    l.add(1);
    l.add(null);
    l.add(4);
    Assertions.assertEquals(List.of(List.of(1,0,4), List.of(1,-1,4)), r.expandWildcard(l));
  }
}
