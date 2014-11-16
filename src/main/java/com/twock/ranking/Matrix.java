package com.twock.ranking;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.*;
import static java.util.Arrays.copyOf;

/**
 * @author Chris Pearson
 */
public class Matrix implements Cloneable {
  private static final double ALMOST_ZERO = 0.000000001;
  private static final boolean FAIL_ON_NOT_HOLD = true;
  private static final Logger log = LoggerFactory.getLogger(Matrix.class);
  private static final String LF = System.getProperty("line.separator");
  private List<String> headings;
  private double[][] matrix;

  public Matrix(List<String> headings, double[][] matrix) {
    this.headings = headings;
    this.matrix = matrix;
  }

  public List<String> getHeadings() {
    return headings;
  }

  public double[][] getMatrix() {
    return matrix;
  }

  @Override
  public String toString() {
    String[][] output = new String[matrix.length + 1][];
    output[0] = headings.toArray(new String[headings.size()]);
    NumberFormat numberFormat = DecimalFormat.getInstance();
    numberFormat.setMaximumFractionDigits(3);
    numberFormat.setMinimumFractionDigits(3);
    for(int i = 0; i < matrix.length; i++) {
      double[] line = matrix[i];
      output[i + 1] = new String[line.length];
      for(int j = 0; j < line.length; j++) {
        output[i + 1][j] = numberFormat.format(line[j]);
      }
    }
    // max field lengths
    int[] lengths = new int[matrix[0].length];
    for(int column = 0; column < matrix[0].length; column++) {
      for(String[] outputLine : output) {
        lengths[column] = Math.max(lengths[column], outputLine[column].length());
      }
    }
    // Now format result
    StringBuilder sb = new StringBuilder();
    for(String[] line : output) {
      sb.append('[');
      for(int i = 0; i < line.length; i++) {
        if(i != 0) {
          sb.append(", ");
        }
        String field = line[i];
        for(int j = 0; j < lengths[i] - field.length(); j++) {
          sb.append(' ');
        }
        sb.append(field);
      }
      sb.append(']').append(LF);
    }
    return sb.toString();
  }

  @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
  @Override
  protected Matrix clone() {
    try {
      Matrix clone = (Matrix)super.clone();
      clone.headings = new ArrayList<>(headings);
      clone.matrix = new double[matrix.length][];
      for(int i = 0; i < matrix.length; i++) {
        clone.matrix[i] = copyOf(matrix[i], matrix[i].length);
      }
      return clone;
    } catch(CloneNotSupportedException e) {
      throw new RuntimeException("Unable to clone " + getClass(), e);
    }
  }

  public int findFirstRowEmptyUntil(int col, int firstRow, int lastRow) {
    for(int i = firstRow; i < lastRow; i++) {
      boolean suitable = true;
      double[] row = matrix[i];
      for(int j = 0; j < col; j++) {
        if(row[j] != 0) {
          suitable = false;
          break;
        }
      }
      if(row[col] == 0) {
        suitable = false;
      }
      if(suitable) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Transform the matrix using the specified rows to achieve 1s in the diagonals of the [firstColumn-lastColumn) range.
   *
   * @return this matrix
   */
  public Matrix convertToReducedRowEchelonForm() {
    log.trace("Before convertToReducedRowEchelonForm():{}{}", LF, this);
    for(int i = 0; i < min(matrix.length, matrix[0].length); i++) {
      int startRow = findFirstRowEmptyUntil(i, 0, matrix.length);
      if (startRow == -1) {
        continue;
      }
      if(startRow > i) {
        swap(startRow, i);
      }
      for(int row = 0; row < matrix.length; row++) {
        if(row == i) {
          if(matrix[row][i] != 0) {
            multiplyRow(row, 1 / matrix[row][i]);
          }
        } else {
          zeroCellBySubtractingRow(row, i, i);
        }
      }
    }
    log.debug("After convertToReducedRowEchelonForm():{}{}", LF, this);
    return this;
  }

  private Matrix multiplyRow(int row, double factor) {
    for(int col = 0; col < matrix[row].length; col++) {
      matrix[row][col] *= factor;
    }
    log.trace("After multiplyRow(row={}, factor={}):{}{}", row, factor, LF, this);
    return this;
  }

  private void zeroCellBySubtractingRow(int rowToChange, int columnToZero, int sourceRow) {
    if(matrix[rowToChange][columnToZero] == 0) {
      return;
    }
    double factor = matrix[rowToChange][columnToZero] / matrix[sourceRow][columnToZero];
    for(int col = 0; col < matrix[rowToChange].length; col++) {
      matrix[rowToChange][col] -= matrix[sourceRow][col] * factor;
    }
    log.trace("After zeroCellBySubtractingRow(rowToChange={}, columnToZero={}, sourceRow={}):{}{}", rowToChange, columnToZero, sourceRow, LF, this);
  }

  public void addRows(int rowToChange, int sourceRow) {
    for(int col = 0; col < matrix[rowToChange].length; col++) {
      matrix[rowToChange][col] += matrix[sourceRow][col];
    }
    log.trace("After addRows(rowToChange={}, sourceRow={}):{}{}", rowToChange, sourceRow, LF, this);
  }

  private Matrix swap(int row1, int row2) {
    double[] temp = matrix[row1];
    matrix[row1] = matrix[row2];
    matrix[row2] = temp;
    log.trace("After swap(row1={}, row2={}):{}{}", row1, row2, LF, this);
    return this;
  }

  public boolean isZeroCells(int firstRow, int lastRow, int firstCol, int lastCol) {
    for(int row = firstRow; row < lastRow; row++) {
      for(int col = firstCol; col < lastCol; col++) {
        double testValue = matrix[row][col];
        if(!isZero(testValue)) {
          return false;
        }
      }
    }
    return true;
  }

  public static boolean isZero(double testValue) {
    return Double.compare(Math.abs(testValue), 0.00000000001) < 0;
  }

  public void checkSolution(double[] calculatedValues) {
    checkSolution(calculatedValues, true);
  }

  public void checkSolution(double[] calculatedValues, boolean lastLine) {
    NumberFormat format = NumberFormat.getNumberInstance();
    format.setMaximumFractionDigits(4);
    format.setMinimumFractionDigits(0);
    boolean holds = true;
    for(int row = 0; row < matrix.length - (lastLine ? 0 : 1); row++) {
      double total = 0;
      StringBuilder sb = new StringBuilder();
      for(int col = 0; col < matrix[row].length - 1; col++) {
        total += matrix[row][col] * calculatedValues[col];
        if(abs(matrix[row][col]) >= ALMOST_ZERO) {
          if(sb.length() > 0) {
            sb.append(' ');
          }
          sb.append(headings.get(col)).append(':');
          sb.append(format.format(matrix[row][col])).append('*');
          sb.append(format.format(calculatedValues[col]));
        }
      }
      total += matrix[row][matrix[row].length - 1];
      if(abs(total) >= ALMOST_ZERO) {
        log.error("Row {} does not hold (total={}): {}", row, total, sb.toString());
        holds = false;
      }
    }
    if(!holds && FAIL_ON_NOT_HOLD) {
      throw new RuntimeException("Matrix does not hold: " + LF + toString());
    }
  }

  public int getMatchCount() {
    return matrix.length - 1;
  }

  public int getTeamCount() {
    return headings.size() - matrix.length;
  }
}
