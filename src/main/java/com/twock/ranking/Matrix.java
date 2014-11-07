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
  private static final Logger log = LoggerFactory.getLogger(Matrix.class);
  private static final String LF = System.getProperty("line.separator");
  private List<String> headings;
  private double[][] matrix;

  public Matrix() {
  }

  public Matrix(List<String> headings, double[][] matrix) {
    this.headings = headings;
    this.matrix = matrix;
  }

  public List<String> getHeadings() {
    return headings;
  }

  public void setHeadings(List<String> headings) {
    this.headings = headings;
  }

  public double[][] getMatrix() {
    return matrix;
  }

  public void setMatrix(double[][] matrix) {
    this.matrix = matrix;
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
      clone.headings = new ArrayList<String>(headings);
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
    throw new RuntimeException("Unable to find a row with non-empty column " + col + " as the first non-zero cell in:" + LF + toString());
  }

  //  public Matrix combineToFirstRow(int firstColumn, int columnCount) {
  //    convertToReducedRowEchelonForm(firstColumn, )
  //  }

  /**
   * Transform the matrix using the specified rows to achieve 1s in the diagonals of the [firstColumn-lastColumn) range.
   *
   * @param firstColumn column index to start from (0 based, includes the specified column)
   * @param lastColumn column index to finish at (0 based, excludes the specified column)
   * @param firstRow row index to start from (0 based, includes the specified row)
   * @param lastRow row index to finish at (0 based, excludes the specified row)
   * @return this matrix
   */
  public Matrix convertToReducedRowEchelonForm(int firstColumn, int lastColumn, int firstRow, int lastRow) {
    if(firstColumn < 0) {
      throw new ArrayIndexOutOfBoundsException("firstColumn of " + firstColumn + " is invalid");
    }
    if(firstRow < 0) {
      throw new ArrayIndexOutOfBoundsException("firstRow of " + firstRow + " is invalid");
    }
    if(lastColumn > matrix[0].length) {
      throw new ArrayIndexOutOfBoundsException("lastColumn of " + lastColumn + " is invalid");
    }
    if(lastRow > matrix.length) {
      throw new ArrayIndexOutOfBoundsException("lastRow of " + lastRow + " is invalid");
    }
    log.trace("Before convertToReducedRowEchelonForm(firstColumn={}, lastColumn={}, firstRow={}, lastRow={}):{}{}", firstColumn, lastColumn, firstRow, lastRow, LF, this);
    for(int i = 0; i < min(lastColumn - firstColumn, lastRow - firstRow); i++) {
      int workRow = firstRow + i;
      int workCol = firstColumn + i;
      int startRow = findFirstRowEmptyUntil(workCol, firstRow, lastRow);
      if(startRow > workRow) {
        swap(startRow, workRow);
      }
      for(int row = firstRow; row < lastRow; row++) {
        if(row == workRow) {
          if(matrix[row][workCol] != 0) {
            multiplyRow(row, 1 / matrix[row][workCol]);
          }
        } else {
          zeroCellBySubtractingRow(row, workCol, workRow);
        }
      }
    }
    log.trace("After convertToReducedRowEchelonForm(firstColumn={}, lastColumn={}, firstRow={}, lastRow={}):{}{}", firstColumn, lastColumn, firstRow, lastRow, LF, this);
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

  public double sumAbs(int row, int firstCol, int lastCol) {
    double result = 0;
    for(int i = firstCol; i < lastCol; i++) {
      result += abs(matrix[row][i]);
    }
    return result;
  }

  public boolean isZeroCells(int row, int firstCol, int lastCol) {
    for(int i = firstCol; i < lastCol; i++) {
      if(Double.compare(Math.abs(matrix[row][i]), 0.00000000001) > 0) {
        return false;
      }
    }
    return true;
  }
}
