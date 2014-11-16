package com.twock.test.ranking;

import com.twock.ranking.PlainRanker;
import com.twock.ranking.Ranker;
import org.joda.time.LocalDate;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Chris Pearson
 */
public class TestRanking {
  @DataProvider(name = "matches")
  public Object[][] matchData() {
    return new Object[][]{
      {"Single game, 10-9 win", new String[]{"T:A-9,B-10"}, "B>A"},
      {"Single game, 10-0 win", new String[]{"T:A-10,B-0"}, "A>B"},
      {"Two games, equal and opposite, same day", new String[]{
        "T:A-10,B-0",
        "T:A-0,B-10"
      }, "A=B,A=50,B=50"},
      {"Two games, equal and opposite, different days", new String[]{
        "T-1:A-10,B-0",
        "T:A-0,B-10"
      }, "B>A"},
      {"Round robin, everyone scores equal", new String[]{
        "T:A-10,B-0",
        "T:B-10,C-0",
        "T:C-10,D-0",
        "T:D-10,A-0"
      }, "A=B,B=C,C=D,D=A"},
      {"Round robin, one person does slightly better", new String[]{
        "T:A-10,B-0",
        "T:B-10,C-0",
        "T:C-10,A-1",
      }, "A>B,A>C"},
      {"Transitive", new String[]{"T:A-10,B-9", "T:B-10,C-9", "T:D-10,C-0"}, "D>C,D>B,D>A,A>B,B>C"},
      {"Test of unlinked games", new String[]{
        "T:A-10,B-5",
        "T:B-10,C-5",
        "T:C-10,D-5",
        "T:B-10,D-2",
        "T:D-10,E-2",
        "T:E-10,F-2",
      }, "A>B,B>C,C>D,D>E,E>F"},
      {"Test of many games", new String[]{
        "T:A-10,B-2",
        "T:A-10,C-6",
        "T:A-10,D-4",
        "T:B-10,C-2",
        "T:B-10,D-2",
        "T:C-10,D-1",
      }, "A>B,B>C,C>D"},
      {"Two games - caused issues", new String[]{
        "T:A-4,B-10",
        "T:C-6,A-10",
      }, "A>C,B>A"}
    };
  }

  @Test(dataProvider = "matches")
  public void testRanking(String scenario, String[] matches, String tests) {
    Ranker ranker = new PlainRanker();
    for(String match : matches) {
      String[] parts = match.split(":", 2);
      LocalDate matchDate = parseDate(parts[0]);
      parts = parts[1].split(",", 2);
      String[] match1 = parts[0].split("-", 2);
      String[] match2 = parts[1].split("-", 2);
      String team1 = match1[0];
      int score1 = Integer.parseInt(match1[1]);
      String team2 = match2[0];
      int score2 = Integer.parseInt(match2[1]);
      ranker.addMatch(matchDate, team1, team2, score1, score2);
    }
    String[] testParts = tests.split(",");
    for(String test : testParts) {
      String team1 = test.substring(0, 1);
      double ranking1 = ranker.getRanking(team1);
      String team2 = test.substring(2);
      double ranking2;
      try {
        ranking2 = Integer.parseInt(team2);
      } catch(NumberFormatException e) {
        ranking2 = ranker.getRanking(team2);
      }

      switch(test.charAt(1)) {
        case '<':
          Assert.assertTrue(ranking1 < ranking2, test + " (" + team1 + "=" + ranking1 + ", " + team2 + "=" + ranking2 + ")");
          break;
        case '>':
          Assert.assertTrue(ranking1 > ranking2, test + " (" + team1 + "=" + ranking1 + ", " + team2 + "=" + ranking2 + ")");
          break;
        case '=':
          Assert.assertEquals(ranking1, ranking2, 0.00000001, test + " (" + team1 + "=" + ranking1 + ", " + team2 + "=" + ranking2 + ")");
          break;
        default:
          throw new RuntimeException("Unhandled comparison operator " + test.charAt(1));
      }
    }
  }

  private static LocalDate parseDate(String str) {
    LocalDate date = new LocalDate();
    if("T".equals(str)) {
      return date;
    } else {
      return date.plusDays(Integer.parseInt(str.substring(1)));
    }
  }
}
