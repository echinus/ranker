package com.twock.ranking;

import org.joda.time.LocalDate;

/**
 * @author Chris Pearson
 */
public class Match {
  private LocalDate date;
  private String team1;
  private String team2;
  private int team1Score;
  private int team2Score;

  public Match(LocalDate date, String team1, String team2, int team1Score, int team2Score) {
    this.date = date;
    if (team1.compareTo(team2) < 0) {
      this.team1 = team1;
      this.team2 = team2;
      this.team1Score = team1Score;
      this.team2Score = team2Score;
    } else {
      this.team1 = team2;
      this.team2 = team1;
      this.team1Score = team2Score;
      this.team2Score = team1Score;
    }
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public String getTeam1() {
    return team1;
  }

  public void setTeam1(String team1) {
    this.team1 = team1;
  }

  public String getTeam2() {
    return team2;
  }

  public void setTeam2(String team2) {
    this.team2 = team2;
  }

  public int getTeam1Score() {
    return team1Score;
  }

  public void setTeam1Score(int team1Score) {
    this.team1Score = team1Score;
  }

  public int getTeam2Score() {
    return team2Score;
  }

  public void setTeam2Score(int team2Score) {
    this.team2Score = team2Score;
  }
}
