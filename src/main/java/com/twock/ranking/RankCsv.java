package com.twock.ranking;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Chris Pearson
 */
public class RankCsv {
  private static final Logger log = LoggerFactory.getLogger(RankCsv.class);

  public static void main(String[] args) throws IOException {
//    final Ranker ranker = new PlainRanker();
    final Ranker ranker = new WeightedGoalDifferenceRanker();
    String inputFile = args.length > 0 ? args[0] : "ranking_input.csv";
    String outputFile = args.length > 1 ? args[1] : "ranking_output.csv";
    log.info("Reading CSV {} and writing output to {}", inputFile, outputFile);
    List<String> csvLines = Files.readAllLines(Paths.get(inputFile), StandardCharsets.UTF_8);
    for(int i = 0; i < csvLines.size(); i++) {
      String csvLine = csvLines.get(i);
      String[] cols = csvLine.split(",");
      if(cols.length < 4) {
        log.warn("Ignoring CSV line {} since it only has {} col(s): {}", i + 1, cols.length, csvLine);
      } else {
        int score1 = -1;
        int score2 = -1;
        try {
          score1 = Integer.parseInt(cols[1]);
        } catch(NumberFormatException e) {
        }
        try {
          score2 = Integer.parseInt(cols[2]);
        } catch(NumberFormatException e) {
        }
        if(score1 < 0 || score1 > 10 || score2 < 0 || score2 > 10) {
          log.warn("Ignoring CSV line {} since it has an invalid/missing score: {}", i + 1, csvLine);
        } else {
          String team1 = cols[0];
          String team2 = cols[3];
          ranker.addMatch(new LocalDate(), team1, team2, score1, score2);
        }
      }
    }
    // now calculate/display output
    List<String> teams = ranker.getTeams();
    Collections.sort(teams, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return Double.compare(ranker.getRanking(o2), ranker.getRanking(o1));
      }
    });
    log.info("Ranking of {} teams complete:", teams.size());
    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), StandardCharsets.UTF_8)) {
      for(int i1 = 0; i1 < teams.size(); i1++) {
        String team = teams.get(i1);
        log.info("Rank #{}: {} ({} - {} games played)", i1 + 1, team, ranker.getRanking(team), ranker.getGamesPlayed(team));
        writer.write(Integer.toString(i1 + 1) + "," + team + "," + ranker.getRanking(team) + "\r\n");
      }
    }
  }
}
