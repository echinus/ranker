package com.twock.ranking;

import java.util.List;

import static java.lang.Math.*;

/**
 * The same as the standard ranker, but adds an amount for the win: 0 for a 1 goal difference, just over 1 for a 2 goal
 * difference up to 2 for a 10 goal difference.
 * 10 * ( 1 - 1 / goalDiff) yields:
 * 20/9 * ( 1 - 1 / 1 ) = 0 for a 1 goal diff
 * 20/9 * ( 1 - 1 / 2 ) = 0 for a 1 goal diff
 * 20/9 * ( 1 - 1 / 9 ) = 1.97 for a 9 goal diff
 * 20/9 * ( 1 - 1 / 10 ) = 2 for a 10 goal diff
 *
 * @author Chris Pearson
 */
public class WinBonusRanker extends PlainRanker {
  @Override
  public double calculateRelativeSkill(List<Match> matches) {
    double parent = super.calculateRelativeSkill(matches);
    double sign = signum(parent);
    return parent + sign * 20d / 9d * (1d - 1d / abs(parent));
  }
}
