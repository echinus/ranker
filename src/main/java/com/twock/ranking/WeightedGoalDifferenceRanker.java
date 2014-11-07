package com.twock.ranking;

import java.util.List;

import static java.lang.Math.*;

/**
 * The same as the standard ranker, but weights scores between 10-0 and 10-5 with a factor of 10 * (1/2^(10-goalDiff)).
 * 10-0 gets an addon of 10, 10-1 an addon of 5, 10-2 an addon of 2.5, 10-3 1.25, etc.
 *
 * @author Chris Pearson
 */
public class WeightedGoalDifferenceRanker extends PlainRanker {
  @Override
  public double calculateRelativeSkill(List<Match> matches) {
    double parent = super.calculateRelativeSkill(matches);
    return abs(parent) < 5 ? parent : parent + signum(parent) * 10 / Math.pow(2, 10 - abs(parent));
  }
}
