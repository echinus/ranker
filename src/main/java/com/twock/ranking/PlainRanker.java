package com.twock.ranking;

import java.util.*;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Chris Pearson
 */
public class PlainRanker implements Ranker {
  private static final String LF = System.getProperty("line.separator");
  private static final Logger log = LoggerFactory.getLogger(PlainRanker.class);
  private static final int CENTRAL_RANK = 50;
  private List<Match> matches = new ArrayList<Match>();
  private Matrix factors;

  @Override
  public void addMatch(LocalDate date, String team1, String team2, int score1, int score2) {
    factors = null;
    matches.add(new Match(date, team1, team2, score1, score2));
  }

  @Override
  public double getRanking(String team) {
    if(factors == null) {
      Map<String, List<Match>> teamMatches = MatchUtils.getMatchGroups(matches);
      Set<List<Match>> matchGroups = new HashSet<List<Match>>(teamMatches.values());
      for(List<Match> matchGroup : matchGroups) {
        List<String> teams = MatchUtils.getSortedTeamList(matchGroup);
        factors = calculateFactors(matchGroup, teams);
        factors.convertToReducedRowEchelonForm(0, teams.size(), 0, factors.getMatrix().length);
        factors.deriveAndEliminateConstants(teams.size(), teams.size() + factors.getMatrix().length - 1);
        // double[][] ks = rearrangeToClearColumns(factors.clone(), teams.size());
      }
    }
    int teamIndex = factors.getHeadings().indexOf(team);
    double[] targetRow = factors.getMatrix()[teamIndex];
    return -targetRow[targetRow.length - 1];
  }

  private Matrix calculateFactors(List<Match> allMatches, List<String> groupTeams) {
    int teamCount = groupTeams.size();
    List<List<Match>> matchesWithMatchingTeams = extractMatchesWithSameTeams(allMatches);
    int matchCount = matchesWithMatchingTeams.size();
    // one linear equation per match, team1 = team2 + goalDiff[positive when team1 wins] + constant1
    // each linear equation = 0 because we have the result as the last value
    int variableCount = teamCount + matchCount + 1; // one variable per team + one constant per match + one numeric total
    double[][] factors = new double[matchCount + 1][];// one per match + one total of variables to average 50
    for(int matchIndex = 0; matchIndex < matchCount; matchIndex++) {
      double[] thisFactor = factors[matchIndex] = new double[variableCount];
      // from above, team2 - team1 + goalDiff[positive when team1 wins] + constant = 0
      List<Match> matches = matchesWithMatchingTeams.get(matchIndex);
      thisFactor[groupTeams.indexOf(matches.get(0).getTeam1())] = -1;
      thisFactor[groupTeams.indexOf(matches.get(0).getTeam2())] = 1;
      // todo: take into account the date the game was played and weight accordingly
      thisFactor[variableCount - 1] = (double)(getTotalScore(matches, 1) - getTotalScore(matches, 2)) / matches.size();
      thisFactor[teamCount + matchIndex] = 1;
    }
    // team1 + team2 + ... + teamn = 50 * n
    // so team1 + team2 + ... + teamn - 50 * n = 0
    double[] lastFactor = factors[matchCount] = new double[variableCount];
    for(int teamIndex = 0; teamIndex < teamCount; teamIndex++) {
      lastFactor[teamIndex] = 1;
    }
    lastFactor[variableCount - 1] = -CENTRAL_RANK * teamCount;
    // log the calculated matrix
    List<String> headings = new ArrayList<String>(variableCount);
    headings.addAll(groupTeams);
    for(int i = 1; i <= matchCount; i++) {
      headings.add("k" + i);
    }
    headings.add("#");
    Matrix result = new Matrix(headings, factors);
    log.trace("Calculated initial matrix:{}{}", LF, result);
    return result;
  }

  private int getTotalScore(List<Match> matches, int teamNumber) {
    int result = 0;
    for(Match match : matches) {
      result += teamNumber == 1 ? match.getTeam1Score() : match.getTeam2Score();
    }
    return result;
  }

  private List<List<Match>> extractMatchesWithSameTeams(List<Match> allMatches) {
    List<List<Match>> result = new ArrayList<List<Match>>();
    List<Match> remaining = new ArrayList<Match>(allMatches);
    while(!remaining.isEmpty()) {
      Match todo = remaining.remove(0);
      List<Match> thisPairing = new ArrayList<Match>();
      thisPairing.add(todo);
      for(Iterator<Match> i = remaining.iterator(); i.hasNext(); ) {
        Match check = i.next();
        if(check.getTeam1().equals(todo.getTeam1()) && check.getTeam2().equals(todo.getTeam2())) {
          i.remove();
          thisPairing.add(check);
        }
      }
      result.add(thisPairing);
    }
    return result;
  }

  @Override
  public List<String> getTeams() {
    return MatchUtils.getSortedTeamList(matches);
  }
}