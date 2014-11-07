package com.twock.ranking;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * @author Chris Pearson
 */
public abstract class MatchUtils {
  private MatchUtils() {
  }

  /**
   * For each team, collate linked teams into match groups.
   *
   * @param matches input list of matches
   * @return map with team name as key, and list of matches for their group as value
   */
  public static Map<String, List<Match>> getMatchGroups(Collection<Match> matches) {
    Map<String, List<Match>> matchGroups = new HashMap<String, List<Match>>();
    // link the matches
    for(Match match : matches) {
      String team1 = match.getTeam1();
      List<Match> matches1 = matchGroups.get(team1);
      String team2 = match.getTeam2();
      List<Match> matches2 = matchGroups.get(team2);
      if(matches1 == null && matches2 == null) {
        // neither team has played a match yet
        List<Match> newMatch = new ArrayList<Match>();
        newMatch.add(match);
        matchGroups.put(team1, newMatch);
        matchGroups.put(team2, newMatch);
      } else if(matches1 == null) {
        // team2 already has a match recorded
        matchGroups.put(team1, matches2);
        matches2.add(match);
      } else if(matches2 == null) {
        // team1 already has a match recorded
        matchGroups.put(team2, matches1);
        matches1.add(match);
      } else if(matches1 == matches2) {
        // team1 and team2 are already part of the same group
        matches1.add(match);
      } else {
        // team1 and team2 are currently part of separate groups
        matches1.addAll(matches2);
        matches1.add(match);
        // make all the teams that were pointing to matches2 point to matches1
        for(Match groupMoveMatch : matches2) {
          matchGroups.put(groupMoveMatch.getTeam1(), matches1);
          matchGroups.put(groupMoveMatch.getTeam2(), matches1);
        }
      }
    }
    return matchGroups;
  }

  public static List<String> getSortedTeamList(Collection<Match> matches) {
    // get a sorted list of all teams
    Set<String> teams = new HashSet<String>();
    for(Match match : matches) {
      teams.add(match.getTeam1());
      teams.add(match.getTeam2());
    }
    List<String> teamList = new ArrayList<String>(teams);
    Collections.sort(teamList);
    return teamList;
  }

}
