package com.twock.test.ranking;

import java.util.*;

import com.twock.ranking.Match;
import org.joda.time.LocalDate;
import org.testng.annotations.Test;

import static com.twock.ranking.MatchUtils.*;
import static org.testng.Assert.assertEquals;

/**
 * @author Chris Pearson
 */
public class TestMatchUtils {
  @Test
  public void testSortedTeams() {
    List<Match> matches = Arrays.asList(
      new Match(new LocalDate(), "c", "b", 10, 9),
      new Match(new LocalDate(), "a", "b", 10, 9),
      new Match(new LocalDate(), "c", "d", 10, 9)
    );
    assertEquals(getSortedTeamList(matches), new ArrayList<String>(Arrays.asList("a", "b", "c", "d")));
    Map<String, List<Match>> groups = getMatchGroups(matches);
    assertEquals(new HashSet<List<Match>>(groups.values()).size(), 1);
    assertEquals(groups.get("a").size(), 3);
  }

  @Test
  public void testSingleGame() {
    List<Match> matches = Arrays.asList(new Match(new LocalDate(), "b", "a", 10, 9));
    assertEquals(getSortedTeamList(matches), new ArrayList<String>(Arrays.asList("a", "b")));
    Map<String, List<Match>> groups = getMatchGroups(matches);
    assertEquals(new HashSet<List<Match>>(groups.values()).size(), 1);
    assertEquals(groups.get("a").size(), 1);
  }

  @Test
  public void testUnlinked() {
    List<Match> matches = Arrays.asList(
      new Match(new LocalDate(), "c", "b", 10, 9),
      new Match(new LocalDate(), "a", "b", 10, 9),
      new Match(new LocalDate(), "e", "f", 10, 9),
      new Match(new LocalDate(), "a", "d", 10, 9),
      new Match(new LocalDate(), "a", "d", 10, 9)
    );
    assertEquals(getSortedTeamList(matches), new ArrayList<String>(Arrays.asList("a", "b", "c", "d", "e", "f")));
    Map<String, List<Match>> groups = getMatchGroups(matches);
    assertEquals(new HashSet<List<Match>>(groups.values()).size(), 2); // a,b,c,d ; e,f
    assertEquals(groups.get("a").size(), 4); // c-b,a-b,a-d,a-d
    assertEquals(groups.get("f").size(), 1); // e-f
  }
}
