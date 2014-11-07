package com.twock.ranking;

import java.util.List;

import org.joda.time.LocalDate;

/**
 * @author Chris Pearson
 */
public interface Ranker {
  public static final double MIN_RANKING = 0.0;
  public static final double INITIAL_RANKING = 50.0;
  public static final double MAX_RANKING = 100.0;

  void addMatch(LocalDate date, String team1, String team2, int score1, int score2);

  double getRanking(String team1);

  List<String> getTeams();
}
