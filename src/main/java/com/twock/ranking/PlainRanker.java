package com.twock.ranking;

import java.util.*;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.twock.ranking.MatchUtils.getSortedTeamList;
import static com.twock.ranking.Matrix.isZero;
import static java.lang.Math.abs;

/**
 * @author Chris Pearson
 */
public class PlainRanker implements Ranker {
  private static final String LF = System.getProperty("line.separator");
  private static final Logger log = LoggerFactory.getLogger(PlainRanker.class);
  private static final int CENTRAL_RANK = 50;
  private List<Match> matches = new ArrayList<>();
  private List<Matrix> factors;

  @Override
  public void addMatch(LocalDate date, String team1, String team2, int score1, int score2) {
    factors = null;
    Match match = new Match(date, team1, team2, score1, score2);
    matches.add(match);
    log.debug("Added new match: {}", match);
  }

  @Override
  public int getGamesPlayed(String team) {
    int result = 0;
    for(Match match : matches) {
      if(match.getTeam1().equals(team) || match.getTeam2().equals(team)) {
        result++;
      }
    }
    return result;
  }

  @Override
  public double getRanking(String team) {
    if(factors == null) {
      Map<String, List<Match>> teamMatches = MatchUtils.getMatchGroups(matches);
      Set<List<Match>> matchGroups = new HashSet<>(teamMatches.values());
      if(matchGroups.size() > 1) {
        List<List<Match>> matchGroupList = new ArrayList<>(matchGroups);
        log.warn("There are {} distinct groups of matches:", matchGroupList.size());
        for(int i = 0; i < matchGroupList.size(); i++) {
          List<Match> matchList = matchGroupList.get(i);
          log.warn("Group {} ({} teams): {}", i + 1, matchList.size(), getSortedTeamList(matchList));
        }
      }
      factors = new ArrayList<>();
      for(List<Match> matchGroup : matchGroups) {
        List<String> teams = getSortedTeamList(matchGroup);
        Matrix matrix = calculateFactors(matchGroup, teams);
        log.debug("Initial factors:{}{}", LF, matrix);
        solve(teams, matrix);
        log.debug("Calculated factors:{}{}", LF, matrix);
        this.factors.add(matrix);
      }
    }
    for(Matrix factor : factors) {
      int teamIndex = factor.getHeadings().indexOf(team);
      if(teamIndex != -1) {
        double[] targetRow = factor.getMatrix()[teamIndex];
        return -targetRow[targetRow.length - 1];
      }
    }
    throw new RuntimeException("Unable to find team " + team + " in any matrices " + factors);
  }

  public void solve(List<String> teams, Matrix matrix) {
    // Step 1: initialise the matrix with a solution (everyone at 50, constants take the slack)
    int lastIndex = matrix.getHeadings().size() - 1;
    double[] solution = new double[lastIndex];
    // matrix has a row count of number of matches, column count of number of teams + matches + 1
    double[][] factors = matrix.getMatrix();
    int matchCount = factors.length - 1;
    int teamCount = lastIndex - matchCount;
    Arrays.fill(solution, 0, teamCount, 50);
    // a = b + 3 + k1 >> a - b - k1 - 3; hence if a=b=50, k1=-3
    recalculateConstants(matrix, solution);
    matrix.checkSolution(solution);
    double cost = calculateCost(matrix, solution);
    /*
     Each row in the matrix is A - B + 3 + k1 = 0 (e.g. where B beat A by three goals)
     Optimisation function is minimise O = k1^2 + k2^2 ... kn^2
     O = (B - A - 3).(B - A - 3) + k2^2 ... kn^2
     O = B^2 + A^2 + 9 - 2AB + 6A - 6B
     dO/dA = 2A - 2B + 6
     dO/dB = 2B - 2A - 6

     For the row B - A + 3 + k1 = 0
     O = (A - B - 3).(A - B - 3) + k2^2 ... kn^2
     O = A^2 + B^2 + 9 - 2AB + 6B - 6A
     dO/dA = 2A - 2B - 6
     dO/dB = 2B - 2A + 6

     (i.e. original factors * 2)
     Or more precisely:
     - 2 x the function x the factor of the derivative variable

     Or when we have A + B - C - D + 8 + k1 = 0
     O = (-A - B + C + D - 8).(-A - B + C + D - 8) + k2^2 ...
     O = (A^2 + AB - AC - AD + 8A)
       + (AB + B^2 - BC - BD + 8B)
       + (-AC - BC + C^2 + CD - 8C)
       + (-AD - BD + CD + D^2 - 8D)
       + (8A + 8B - 8C - 8D + 64)
     O = A^2 + B^2 + C^2 + D^2 + 64
         + 2AB - 2AC - 2AD - 2BC - 2BD + 2CD
         + 16A + 16B - 16C - 16D
     dO/dA = 2A + 2B - 2C - 2D + 16
     dO/dC = 2C - 2A - 2B + 2D - 16
     */
    double magnitude = 1;
    while(cost > 0) {
      double lastCost = cost;
      double[] oldSolution = Arrays.copyOf(solution, solution.length);
      log.trace("Potential solution (cost {}, increment={})={}", cost, magnitude, solution);
      double[] gradient = new double[teamCount];
      for(int row = 0; row < matchCount; row++) {
        for(int team = 0; team < teamCount; team++) {
          gradient[team] += matrix.getMatrix()[row][team] * calculateRowNoConstants(matrix, row, solution) * 2/*derivative of square*/;
        }
      }
      log.trace("Gradient={}", gradient);
      int teamToChange = findBiggestAbsIndex(gradient);
      if(teamToChange == -1) {
        break;
      }
      solution[teamToChange] += gradient[teamToChange] > 0 ? -magnitude : magnitude;
      recalculateConstants(matrix, solution);
      cost = calculateCost(matrix, solution);
      matrix.checkSolution(solution, false);
      if(cost >= lastCost) {
        magnitude *= 0.5;
        solution = oldSolution;
        if(isZero(magnitude)) {
          break;
        }
      }
    }
    // scale variables up to average around 50
    double total = 0;
    for(int team = 0; team < teamCount; team++) {
      total += solution[team];
    }
    double increment = (50 * teamCount - total) / teamCount;
    for(int team = 0; team < teamCount; team++) {
      solution[team] += increment;
    }
    // Now remove constants from matrix
    for(int col = teamCount; col < teamCount + matchCount; col++) {
      log.debug("Final error {}={}", matrix.getHeadings().get(col), solution[col]);
    }
    for(double[] thisRow : factors) {
      for(int col = teamCount; col < teamCount + matchCount; col++) {
        if(!isZero(thisRow[col])) {
          thisRow[thisRow.length - 1] += thisRow[col] * solution[col];
          thisRow[col] = 0;
        }
      }
    }
    matrix.convertToReducedRowEchelonForm();
  }

  private static void recalculateConstants(Matrix matrix, double[] solution) {
    int teamCount = matrix.getTeamCount();
    for(int row = 0; row < matrix.getMatchCount(); row++) {
      double[] thisRow = matrix.getMatrix()[row];
      solution[teamCount + row] = calculateRowNoConstants(matrix, row, solution) / -thisRow[teamCount + row];
    }
  }

  private static int findBiggestAbsIndex(double[] gradient) {
    double biggestAbs = 0;
    int biggestAbsIndex = -1;
    for(int i = 0; i < gradient.length; i++) {
      double v = abs(gradient[i]);
      if(v > biggestAbs) {
        biggestAbs = v;
        biggestAbsIndex = i;
      }
    }
    return biggestAbsIndex;
  }

  private static double calculateRowNoConstants(Matrix matrix, int row, double[] solution) {
    double total = 0;
    double[] matrixRow = matrix.getMatrix()[row];
    for(int col = 0; col < matrix.getTeamCount(); col++) {
      total += matrixRow[col] * solution[col];
    }
    total += matrixRow[matrixRow.length - 1];
    return total;
  }

  private double calculateCost(Matrix matrix, double[] solution) {
    int lastIndex = matrix.getHeadings().size() - 1;
    double[][] factors = matrix.getMatrix();
    int matchCount = factors.length - 1;
    int teamCount = lastIndex - matchCount;
    double sum = 0;
    for(int k = teamCount; k < lastIndex; k++) {
      log.trace("k{}={}", k - teamCount + 1, solution[k]);
      sum += solution[k] * solution[k];
    }
    log.trace("Cost of constants squared = {}{}{}", sum, LF, matrix);
    return sum;
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
      thisFactor[variableCount - 1] = calculateRelativeSkill(matches);
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
    List<String> headings = new ArrayList<>(variableCount);
    headings.addAll(groupTeams);
    for(int i = 1; i <= matchCount; i++) {
      headings.add("k" + i);
    }
    headings.add("#");
    Matrix result = new Matrix(headings, factors);
    log.trace("Calculated initial matrix:{}{}", LF, result);
    return result;
  }

  /**
   * Calculate the relative skill.  In the PlainRanker this is simply goal difference but it doesn't handle cases where
   * people are beaten 10-0 very well.
   *
   * @param matches matches to assess, all between the same two people
   * @return a figure to measure relative skill, positive when team1 has won
   */
  public double calculateRelativeSkill(List<Match> matches) {
    return (double)(getTotalScore(matches, 1) - getTotalScore(matches, 2)) / matches.size();
  }

  private int getTotalScore(List<Match> matches, int teamNumber) {
    int result = 0;
    for(Match match : matches) {
      result += teamNumber == 1 ? match.getTeam1Score() : match.getTeam2Score();
    }
    return result;
  }

  private List<List<Match>> extractMatchesWithSameTeams(List<Match> allMatches) {
    List<List<Match>> result = new ArrayList<>();
    List<Match> remaining = new ArrayList<>(allMatches);
    while(!remaining.isEmpty()) {
      Match todo = remaining.remove(0);
      List<Match> thisPairing = new ArrayList<>();
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
    return getSortedTeamList(matches);
  }
}